package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.vlstream.test.vlstream.data.DataManagementService;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import com.ruoyi.vlstream.test.vlstream.service.RemoteModelArtifactService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class CloudTrainingArchiveOnlyTest {
    CloudTrainingWorker worker; ComputePaidArchiveRecoveryService archives; ComputeSsh ssh;
    ComputeSsh.Connection connection; JdbcTemplate jdbc; CloudTrainingJob job; ComputeNode node;

    @BeforeEach void setup() throws Exception {
        jdbc=mock(JdbcTemplate.class);ComputeNodeService nodes=mock(ComputeNodeService.class);ssh=mock(ComputeSsh.class);
        archives=mock(ComputePaidArchiveRecoveryService.class);connection=mock(ComputeSsh.Connection.class);
        worker=new CloudTrainingWorker(jdbc,mock(PlatformTransactionManager.class),nodes,ssh,mock(CloudTrainingDatasetWriter.class),
            mock(DataManagementService.class),mock(ModelArtifactObjectStore.class),mock(RemoteModelArtifactService.class),new ObjectMapper());
        ReflectionTestUtils.setField(worker,"paidArchives",archives);
        node=new ComputeNode();node.setId(-6000L);node.setEnabled(true);node.setPublicInstanceKey("instance");node.setPythonPath("/fixture/python");
        job=new CloudTrainingJob();job.setId("job");job.setTenantId("tenant");job.setNodeId(node.getId());job.setRunDir("/fixture/original/job");
        job.setPublicInstanceKey("instance");job.setPublicClaimId("claim");job.setPublicOwnerSubject("owner");job.setJobState("ARCHIVE_PENDING");job.setOptionsJson("{}");
        ComputePublicInventoryService inventory=mock(ComputePublicInventoryService.class);ComputePublicInventoryService.Instance claim=new ComputePublicInventoryService.Instance();
        claim.setPublicState("OCCUPIED");claim.setClaimId("claim");claim.setOwnerSubject("owner");
        when(nodes.get(node.getId())).thenReturn(node);when(nodes.officialConfigured(node)).thenReturn(true);when(nodes.publicInventory()).thenReturn(inventory);
        when(inventory.read(node,false)).thenReturn(claim);when(nodes.configurationSnapshot(node)).thenReturn(Collections.singletonMap("targetFingerprint","target"));
        when(archives.hasCheckpoint(job)).thenReturn(true);when(ssh.open(node)).thenReturn(connection);
        when(jdbc.queryForObject(anyString(),eq(Boolean.class),eq("tenant"),eq("job"))).thenReturn(false);
    }

    @Test void pausedAttemptDoesNotReconnectOrRestartTraining() {
        when(archives.mayArchive(eq(job),eq(node),eq("target"),anyLong())).thenReturn(false);
        ReflectionTestUtils.invokeMethod(worker,"process",job);
        verifyNoInteractions(ssh);verify(jdbc,never()).update(startsWith("UPDATE vls_cloud_training_job SET job_state"),any(Object[].class));
    }

    @Test void lostRemoteTerminalStateCannotConvertArchiveRecoveryIntoANewTrainingRun() throws Exception {
        when(archives.mayArchive(eq(job),eq(node),eq("target"),anyLong())).thenReturn(true);
        when(connection.execute(startsWith("test -s "),anyInt())).thenReturn("READY");
        when(connection.execute(contains(" status "),anyInt())).thenReturn("VLS_CLOUD={\"state\":\"NOT_STARTED\"}");
        assertThrows(Exception.class,()->ReflectionTestUtils.invokeMethod(worker,"process",job));
        verify(connection,never()).execute(startsWith("nohup "),anyInt());
        verify(jdbc,never()).update(startsWith("UPDATE vls_cloud_training_job SET job_state"),any(Object[].class));
    }

    @Test void completionRequiresReadyObjectsMatchingTheOriginalResultAfterUpload() {
        ModelArtifactObjectStore store=(ModelArtifactObjectStore)ReflectionTestUtils.getField(worker,"objects");
        String sha=ModelArtifactObjectStore.hash("original");
        assertDoesNotThrow(()->ReflectionTestUtils.invokeMethod(worker,"requireOriginalStoredArtifact","path",sha,4L,false));
        assertThrows(Exception.class,()->ReflectionTestUtils.invokeMethod(worker,"requireOriginalStoredArtifact","path",sha,4L,true));
        when(store.find("path")).thenReturn(new ModelArtifactObjectStore.StoredArtifact("fixture","key","best.pt",4L,ModelArtifactObjectStore.hash("different")));
        assertThrows(Exception.class,()->ReflectionTestUtils.invokeMethod(worker,"requireOriginalStoredArtifact","path",sha,4L,true));
        when(store.find("path")).thenReturn(new ModelArtifactObjectStore.StoredArtifact("fixture","key","best.pt",4L,sha));
        assertDoesNotThrow(()->ReflectionTestUtils.invokeMethod(worker,"requireOriginalStoredArtifact","path",sha,4L,true));
    }
}
