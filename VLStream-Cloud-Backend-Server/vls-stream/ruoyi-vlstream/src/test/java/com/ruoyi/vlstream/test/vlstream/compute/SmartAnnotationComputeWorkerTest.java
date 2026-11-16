package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.vlstream.test.vlstream.data.*;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationLabel;
import com.ruoyi.vlstream.test.vlstream.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@Tag("dev")
class SmartAnnotationComputeWorkerTest {
    CloudTrainingWorker worker; CloudTrainingJob job; ComputeNode node; ComputeSsh.Connection connection;
    ComputeSharedQueueService shared; SmartAnnotationWorker importer; SmartAnnotationComputeService annotations;
    JdbcTemplate jdbc; ObjectMapper json=new ObjectMapper(); byte[] predictions; JsonNode success; ModelArtifactObjectStore objects;
    @BeforeEach @SuppressWarnings("unchecked") void setup() throws Exception {
        jdbc=mock(JdbcTemplate.class);ComputeNodeService nodes=mock(ComputeNodeService.class);
        ComputeSsh ssh=mock(ComputeSsh.class);connection=mock(ComputeSsh.Connection.class);
        PlatformTransactionManager tx=mock(PlatformTransactionManager.class);when(tx.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        DataManagementService data=mock(DataManagementService.class);objects=mock(ModelArtifactObjectStore.class);
        worker=new CloudTrainingWorker(jdbc,tx,nodes,ssh,mock(CloudTrainingDatasetWriter.class),data,objects,mock(RemoteModelArtifactService.class),json);
        shared=mock(ComputeSharedQueueService.class);annotations=mock(SmartAnnotationComputeService.class);importer=mock(SmartAnnotationWorker.class);
        ObjectProvider<SmartAnnotationWorker> provider=mock(ObjectProvider.class);when(provider.getObject()).thenReturn(importer);
        ReflectionTestUtils.setField(worker,"sharedQueue",shared);ReflectionTestUtils.setField(worker,"annotations",annotations);
        ReflectionTestUtils.setField(worker,"annotationWorker",provider);
        Path artifacts=Paths.get(System.getProperty("vls.test.artifacts","codex/smart-compute-worker"));Files.createDirectories(artifacts);
        ReflectionTestUtils.setField(worker,"tempDir",artifacts.toAbsolutePath().toString());
        node=new ComputeNode();node.setId(10L);node.setEnabled(true);node.setPythonPath("/fixture/python");node.setWorkDir("/fixture");
        job=new CloudTrainingJob();job.setId("job");job.setTenantId("tenant");job.setNodeId(10L);job.setSmartRoundId(2L);job.setContainerRecordId(3L);
        job.setRunDir("/fixture/job");job.setJobState("RUNNING");job.setAnnotationType("object_detection");job.setSnapshotJson("snapshot");job.setOptionsJson("{}");
        when(nodes.get(10L)).thenReturn(node);when(nodes.isCompanyShared(node)).thenReturn(true);when(shared.isSharedJob(job)).thenReturn(true);
        when(shared.pythonCommand(node)).thenReturn("python");when(ssh.open(node)).thenReturn(connection);
        when(connection.execute(contains("test -s"),eq(20))).thenReturn("READY");
        when(jdbc.queryForObject(contains("cancel_requested"),eq(Boolean.class),any(),any())).thenReturn(false);
        DatasetSnapshot snapshot=new DatasetSnapshot();snapshot.setAnnotationType("object_detection");AnnotationLabel label=new AnnotationLabel();label.setId(1L);label.setName("car");snapshot.setLabels(Collections.singletonList(label));
        when(data.readSnapshot("snapshot")).thenReturn(snapshot);
        String yaml=new TrainingDatasetLayout("object_detection",snapshot.getLabels()).yaml("/fixture/job/dataset");
        when(objects.archive(anyString(),anyString(),any())).thenReturn(true);
        when(objects.find(job.modelKey())).thenReturn(new ModelArtifactObjectStore.StoredArtifact("fixture","pt","best.pt",10,repeatA()));
        when(objects.find(ModelClassFileService.storagePath(job.modelKey()))).thenReturn(new ModelArtifactObjectStore.StoredArtifact("fixture","classes","data.yaml",yaml.getBytes(StandardCharsets.UTF_8).length,ModelArtifactObjectStore.hash(yaml)));
        predictions=("{\"annotationType\":\"object_detection\",\"modelSha256\":\""+repeatA()+"\",\"predictions\":[]}").getBytes(StandardCharsets.UTF_8);
        success=json.readTree("{\"state\":\"SUCCEEDED\",\"sha256\":\""+repeatA()+"\",\"size\":10,\"predictionsSha256\":\""+ModelArtifactObjectStore.hash(new String(predictions,StandardCharsets.UTF_8))+"\",\"predictionsSize\":"+predictions.length+"}");
        when(connection.execute(contains(" status "),eq(45))).thenReturn("VLS_CLOUD="+success);
        doAnswer(call->{Files.write(call.getArgument(1),predictions);return null;}).when(connection).downloadBounded(eq("/fixture/job/results.json"),any(Path.class),eq(64L*1024*1024));
    }
    private String repeatA(){return String.join("",Collections.nCopies(64,"a"));}
    @Test void importsVerifiedResultsBeforeReleaseThenPublishesReview() throws Exception {
        ReflectionTestUtils.invokeMethod(worker,"process",job);
        org.mockito.InOrder order=inOrder(importer,shared,annotations);
        order.verify(importer).importComputeResults(eq(2L),any(),eq(job.modelKey()),eq(repeatA()));
        order.verify(shared).release(eq(connection),eq(node),eq(job),any(),anyString());
        order.verify(annotations).finished(job,"COMPLETED","PT 和类别文件已校验并回存平台");
        verify(jdbc,never()).update(contains("UPDATE vls_algorithm_training"),any(),any(),any(),any(),any(),any(),any());
    }
    @Test void corruptPredictionsRetainLeaseAndNeverPublishReview() throws Exception {
        predictions="corrupt".getBytes(StandardCharsets.UTF_8);
        assertThrows(Exception.class,()->ReflectionTestUtils.invokeMethod(worker,"process",job));
        verifyNoInteractions(importer);verify(shared,never()).release(any(),any(),any(),any(),any());
        verify(annotations,never()).finished(any(),any(),any());
    }
    @Test void importFailureRetainsLeaseForIdempotentRetry() throws Exception {
        doThrow(new com.ruoyi.common.exception.ServiceException("import failed")).when(importer).importComputeResults(anyLong(),any(),anyString(),anyString());
        assertThrows(Exception.class,()->ReflectionTestUtils.invokeMethod(worker,"process",job));
        verify(shared,never()).release(any(),any(),any(),any(),any());
    }
    @Test void queuedCancellationReleasesTicketWithoutStartingProcess() throws Exception {
        job.setJobState("QUEUED");job.setCancelRequested(true);
        ReflectionTestUtils.invokeMethod(worker,"process",job);
        verify(shared).release(eq(connection),eq(node),eq(job),argThat(result->"CANCELLED".equals(result.path("state").asText())),isNull());
        verify(connection,never()).execute(anyString(),anyInt());verifyNoInteractions(importer);
        verify(annotations).finished(job,"CANCELLED","训练已取消");
    }
}
