package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.ruoyi.common.exception.ServiceException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Real quote persistence and acceptance with fixture wallet/provider; no live money or GPU. */
@Tag("dev")
class ComputePaidQuoteSnapshotTest {
    ComputePaidRunIntegrationTest fixture;
    ComputeNodeService nodes;
    Map<String,Object> snapshot;
    @BeforeEach void setup() throws Exception {
        fixture=new ComputePaidRunIntegrationTest();fixture.setup();
        nodes=(ComputeNodeService)ReflectionTestUtils.getField(fixture.runs,"nodes");
        when(nodes.lockServer(1L)).thenAnswer(call->{
            ComputeNode node=fixture.node();node.setPlatformCatalogManaged(true);node.setConfigurationRevision(1L);return node;
        });
        snapshot=new LinkedHashMap<>();snapshot.put("code","official-autodl");snapshot.put("nodeId","1");
        snapshot.put("targetFingerprint","fixture-stable-target");snapshot.put("executionRevision",1L);snapshot.put("executionEnvironment","localhost");
        when(nodes.configurationSnapshot(any())).thenAnswer(call->new LinkedHashMap<>(snapshot));
    }
    @AfterEach void clear(){fixture.clear();}
    @ParameterizedTest @ValueSource(longs={1L,2147483648L,9007199254740993L})
    void persistedUnchangedExecutionRevisionDoesNotRejectAcceptance(long revision) throws Exception {
        snapshot.put("executionRevision",revision);
        String id=fixture.runs.quote(1L,fixture.payer,fixture.config).getRunId();
        JsonNode saved=fixture.json.readTree(fixture.jdbc.queryForObject("SELECT quote_snapshot FROM vls_compute_paid_run WHERE run_id=?",String.class,id)).path("configuredResource");
        assertEquals(revision,saved.path("executionRevision").longValue());
        fixture.runs.accept(id,fixture.payer);
        assertEquals("FUNDING",fixture.runs.get(id,fixture.payer).getState());
        assertNotNull(fixture.runs.get(id,fixture.payer).getUsageId());
        assertEquals(0,fixture.wallet.holdPosts);verify(fixture.supplier,never()).submit(any(),anyString(),anyString());
    }
    @ParameterizedTest @ValueSource(strings={"code","nodeId","targetFingerprint","executionRevision","executionEnvironment"})
    void genuineExecutionChangeStillRejectsBeforeClaimOrFunding(String field) {
        String id=fixture.runs.quote(1L,fixture.payer,fixture.config).getRunId();
        snapshot.put(field,"executionRevision".equals(field)?2L:"changed");
        assertEquals("算力执行配置已变化，请重新确认报价",assertThrows(ServiceException.class,()->fixture.runs.accept(id,fixture.payer)).getMessage());
        assertEquals("QUOTED",fixture.runs.get(id,fixture.payer).getState());assertNull(fixture.runs.get(id,fixture.payer).getUsageId());
        verify(fixture.inventory,never()).claimPaid(any(),any());assertEquals(0,fixture.wallet.holdPosts);
    }
    @Test void textualRevisionCannotBeTreatedAsTheSameNumber() {
        String id=fixture.runs.quote(1L,fixture.payer,fixture.config).getRunId();snapshot.put("executionRevision","1");
        assertThrows(ServiceException.class,()->fixture.runs.accept(id,fixture.payer));
        verify(fixture.inventory,never()).claimPaid(any(),any());
    }
}
