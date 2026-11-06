package com.ruoyi.vlstream.test.vlstream.cloudai;

import com.ruoyi.vlstream.test.vlstream.compute.*;
import com.ruoyi.vlstream.test.vlstream.service.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class CloudAiExecutionScopeTest {
    @Test void singleAndMultiWorkersUseDifferentDurableClaimBoundaries() {
        for (String scope : new String[]{"localhost:single", "localhost:multi"}) {
            JdbcTemplate jdbc = mock(JdbcTemplate.class);
            CloudAiService service = mock(CloudAiService.class);
            when(service.executionScope()).thenReturn(scope);
            when(jdbc.queryForList(anyString(), eq(String.class), eq(scope), anyLong()))
                .thenReturn(Collections.singletonList("run"));
            CloudAiWorker worker = new CloudAiWorker(jdbc, service, mock(CloudAiSources.class),
                mock(CloudAiEvents.class), mock(CloudAiEvidenceStore.class), mock(ModelArtifactObjectStore.class),
                mock(ComputeNodeService.class), mock(ConfiguredComputeRegistry.class),
                mock(ComputeSharedQueueService.class), mock(ComputeSsh.class));
            worker.scan();
            verify(jdbc).queryForList(contains("JSON_UNQUOTE(JSON_EXTRACT(snapshot_json,'$.executionScope'))=?"),
                eq(String.class), eq(scope), anyLong());
            verify(jdbc).update(contains("id=? AND JSON_UNQUOTE(JSON_EXTRACT(snapshot_json,'$.executionScope'))=?"),
                anyString(), anyLong(), eq("run"), eq(scope), anyLong());
            worker.shutdown();
        }
    }

    @Test void schedulerFiltersBeforeTakingTheRowLock() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        CloudAiService service = mock(CloudAiService.class);
        when(service.executionScope()).thenReturn("localhost:single");
        CloudAiScheduler scheduler = new CloudAiScheduler(jdbc, mock(TransactionTemplate.class), service);
        scheduler.occurrence("other-mode-task", 10L);
        verify(jdbc).queryForList(contains("JSON_UNQUOTE(JSON_EXTRACT(schedule_json,'$.executionScope'))=?"),
            eq("other-mode-task"), eq("localhost:single"), eq(10L));
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }

    @Test void executionIdentityIsServerOwnedAndRejectsMissingEnvironment() {
        CloudAiService service = new CloudAiService(null,null,null,null,null,null,null);
        ReflectionTestUtils.setField(service,"executionEnvironment","localhost");
        ReflectionTestUtils.setField(service,"executionMode","single");
        assertEquals("localhost:single",service.executionScope());
        ReflectionTestUtils.setField(service,"executionMode","multi");
        assertEquals("localhost:multi",service.executionScope());
        ReflectionTestUtils.setField(service,"executionEnvironment","");
        assertThrows(RuntimeException.class,service::executionScope);
    }
}
