package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.vlstream.test.vlstream.data.*;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationLabel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ContainerInstance;
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
    SharedTrainingWorkspaceCleanup cleanup;
    JdbcTemplate jdbc; ObjectMapper json=new ObjectMapper(); byte[] predictions; JsonNode success; ModelArtifactObjectStore objects;
    @BeforeEach @SuppressWarnings("unchecked") void setup() throws Exception {
        jdbc=mock(JdbcTemplate.class);ComputeNodeService nodes=mock(ComputeNodeService.class);
        ComputeSsh ssh=mock(ComputeSsh.class);connection=mock(ComputeSsh.Connection.class);
        PlatformTransactionManager tx=mock(PlatformTransactionManager.class);when(tx.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        DataManagementService data=mock(DataManagementService.class);objects=mock(ModelArtifactObjectStore.class);
        worker=new CloudTrainingWorker(jdbc,tx,nodes,ssh,mock(CloudTrainingDatasetWriter.class),data,objects,mock(RemoteModelArtifactService.class),json);
        shared=mock(ComputeSharedQueueService.class);annotations=mock(SmartAnnotationComputeService.class);importer=mock(SmartAnnotationWorker.class);
        cleanup = mock(SharedTrainingWorkspaceCleanup.class);
        ObjectProvider<SmartAnnotationWorker> provider=mock(ObjectProvider.class);when(provider.getObject()).thenReturn(importer);
        ReflectionTestUtils.setField(worker,"sharedQueue",shared);ReflectionTestUtils.setField(worker,"annotations",annotations);
        ReflectionTestUtils.setField(worker,"annotationWorker",provider);
        ReflectionTestUtils.setField(worker, "workspaceCleanup", cleanup);
        Path artifacts=Paths.get(System.getProperty("vls.test.artifacts","codex/smart-compute-worker"));Files.createDirectories(artifacts);
        ReflectionTestUtils.setField(worker,"tempDir",artifacts.toAbsolutePath().toString());
        node=new ComputeNode();node.setId(10L);node.setEnabled(true);node.setPythonPath("/fixture/python");node.setWorkDir("/fixture");
        job=new CloudTrainingJob();job.setId("job");job.setTenantId("tenant");job.setNodeId(10L);job.setSmartRoundId(2L);job.setContainerRecordId(3L);
        job.setRunDir("/fixture/job");job.setJobState("RUNNING");job.setAnnotationType("object_detection");job.setSnapshotJson("snapshot");job.setOptionsJson("{}");
        when(nodes.get(10L)).thenReturn(node);when(nodes.isCompanyShared(node)).thenReturn(true);when(shared.isSharedJob(job)).thenReturn(true);
        when(shared.pythonCommand(node)).thenReturn("python");when(ssh.open(node)).thenReturn(connection);
        when(shared.release(eq(connection), eq(node), eq(job), any(), nullable(String.class))).thenAnswer(call -> {
            JsonNode result = call.getArgument(3);
            return json.createObjectNode().put("state", "SUCCEEDED".equals(result.path("state").asText())
                ? "ARCHIVED" : result.path("state").asText());
        });
        when(connection.execute(contains("test -s"),eq(20))).thenReturn("READY");
        when(jdbc.queryForObject(contains("cancel_requested"),eq(Boolean.class),any(),any())).thenReturn(false);
        DatasetSnapshot snapshot=new DatasetSnapshot();snapshot.setAnnotationType("object_detection");AnnotationLabel label=new AnnotationLabel();label.setId(1L);label.setName("car");snapshot.setLabels(Collections.singletonList(label));
        when(data.readSnapshot("snapshot")).thenReturn(snapshot);
        String yaml=new TrainingDatasetLayout("object_detection",snapshot.getLabels()).yaml("/fixture/job/dataset");
        when(objects.archive(anyString(),anyString(),any())).thenReturn(true);
        when(objects.find(job.modelKey())).thenReturn(new ModelArtifactObjectStore.StoredArtifact("fixture","pt","best.pt",10,repeatA()));
        when(objects.find(ModelClassFileService.storagePath(job.modelKey()))).thenReturn(new ModelArtifactObjectStore.StoredArtifact("fixture","classes","data.yaml",yaml.getBytes(StandardCharsets.UTF_8).length,ModelArtifactObjectStore.hash(yaml)));
        stubPredictionArtifact(null);
        doAnswer(call->{Files.write(call.getArgument(1),predictions);return null;}).when(connection).downloadBounded(eq("/fixture/job/results.json"),any(Path.class),eq(64L*1024*1024));
    }
    private String repeatA(){return String.join("",Collections.nCopies(64,"a"));}

    private void stubPredictionArtifact(JsonNode autoConfirm) throws Exception {
        ObjectNode artifact = json.createObjectNode().put("annotationType", "object_detection").put("modelSha256", repeatA());
        artifact.putArray("predictions");
        if (autoConfirm != null) artifact.set("autoConfirm", autoConfirm);
        String contents = json.writeValueAsString(artifact);
        predictions = contents.getBytes(StandardCharsets.UTF_8);
        success = json.createObjectNode().put("state", "SUCCEEDED").put("sha256", repeatA()).put("size", 10)
            .put("predictionsSha256", ModelArtifactObjectStore.hash(contents)).put("predictionsSize", predictions.length);
        when(connection.execute(contains(" status "), eq(45))).thenReturn("VLS_CLOUD=" + success);
    }

    private void assertPublishesAfterImport(boolean autoConfirm) throws Exception {
        ReflectionTestUtils.invokeMethod(worker,"process",job);
        org.mockito.InOrder order=inOrder(importer,shared,annotations,cleanup);
        order.verify(importer).importComputeResults(eq(2L),any(),eq(job.modelKey()),eq(repeatA()),eq(autoConfirm));
        order.verify(shared).release(eq(connection),eq(node),eq(job),any(),anyString());
        order.verify(annotations).finished(job,"COMPLETED","PT 和类别文件已校验并回存平台",autoConfirm);
        order.verify(cleanup).enqueue(eq(job), argThat(ack -> "ARCHIVED".equals(ack.path("state").asText())), isNull());
    }

    @Test void importsVerifiedResultsBeforeReleaseThenPublishesReview() throws Exception {
        assertPublishesAfterImport(false);
        verify(jdbc,never()).update(contains("UPDATE vls_algorithm_training"),any(),any(),any(),any(),any(),any(),any());
    }

    @Test void deploymentReleaseIsDeferredUntilTheDurableCleanupRequestIsDelivered() throws Exception {
        node.setPlatformCatalogManaged(true);
        ComputeExecutionCoordinator coordinator = mock(ComputeExecutionCoordinator.class);
        ReflectionTestUtils.setField(worker, "executionCoordinator", coordinator);

        ReflectionTestUtils.invokeMethod(worker, "process", job);

        org.mockito.InOrder order = inOrder(importer, shared, annotations, cleanup);
        order.verify(importer).importComputeResults(eq(2L), any(), eq(job.modelKey()), eq(repeatA()), eq(false));
        order.verify(shared).release(eq(connection), eq(node), eq(job), any(), anyString());
        order.verify(annotations).finished(job, "COMPLETED", "PT 和类别文件已校验并回存平台", false);
        order.verify(cleanup).enqueue(eq(job), argThat(ack -> "ARCHIVED".equals(ack.path("state").asText())), eq(10L));
        verifyNoInteractions(coordinator);
    }

    @Test void verifiedOptInEnablesAutomaticConfirmationAfterRelease() throws Exception {
        job.setOptionsJson("{\"autoConfirm\":true}");
        stubPredictionArtifact(json.getNodeFactory().booleanNode(true));
        assertPublishesAfterImport(true);
    }

    @Test void resultMarkerAloneCannotEnableAutomaticConfirmation() throws Exception {
        stubPredictionArtifact(json.getNodeFactory().booleanNode(true));
        assertPublishesAfterImport(false);
    }

    @Test void optedInJobWithOldRunnerResultPublishesManualReview() throws Exception {
        job.setOptionsJson("{\"autoConfirm\":true}");
        assertPublishesAfterImport(false);
    }

    @Test void newNodeJobsFreezeAutomaticConfirmationAndThreshold() throws Exception {
        ComputeNodeService nodes = mock(ComputeNodeService.class);
        when(nodes.requireReady(10L)).thenReturn(node);
        IVlsContainerInstanceService containers = mock(IVlsContainerInstanceService.class);
        when(containers.saveTaskRecord(any())).thenAnswer(call -> {
            call.<ContainerInstance>getArgument(0).setId(3L);
            return true;
        });
        SmartAnnotationTask task = new SmartAnnotationTask();
        task.setTenantId("tenant");
        task.setCreateUser("7");
        task.setDatasetId(20L);
        task.setComputeNodeId(10L);
        task.setAnnotationType("object_detection");
        task.setModelPath("@preset/fixture");
        task.setConfidence(.37);
        SmartAnnotationRound round = new SmartAnnotationRound();
        round.setId(2L);
        round.setVersionId(30L);
        new SmartAnnotationComputeService(jdbc, nodes, shared, containers, json, objects).enqueue(task, round, new DatasetSnapshot(), task.getCreateUser());

        org.mockito.ArgumentCaptor<String> options = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(jdbc).update(contains("INSERT INTO vls_cloud_training_job"), any(), eq("tenant"), eq(2L), eq(10L), eq(3L), eq(20L),
            eq(30L), eq("object_detection"), any(), eq("@preset/fixture"), options.capture(), any(), isNull(), isNull(), isNull());
        JsonNode frozen = json.readTree(options.getValue());
        assertTrue(frozen.path("autoConfirm").isBoolean() && frozen.path("autoConfirm").booleanValue());
        assertEquals(.37, frozen.path("confidence").doubleValue());
    }

    @Test void corruptPredictionsRetainLeaseAndNeverPublishReview() throws Exception {
        predictions="corrupt".getBytes(StandardCharsets.UTF_8);
        assertThrows(Exception.class,()->ReflectionTestUtils.invokeMethod(worker,"process",job));
        verifyNoInteractions(importer);verify(shared,never()).release(any(),any(),any(),any(),any());
        verify(annotations,never()).finished(any(),any(),any(),anyBoolean());
        verifyNoInteractions(cleanup);
    }
    @Test void importFailureRetainsLeaseForIdempotentRetry() throws Exception {
        node.setPlatformCatalogManaged(true);
        ComputeExecutionCoordinator coordinator = mock(ComputeExecutionCoordinator.class);
        ReflectionTestUtils.setField(worker, "executionCoordinator", coordinator);
        doThrow(new com.ruoyi.common.exception.ServiceException("import failed")).when(importer).importComputeResults(anyLong(),any(),anyString(),anyString(),anyBoolean());
        assertThrows(Exception.class,()->ReflectionTestUtils.invokeMethod(worker,"process",job));
        verify(shared,never()).release(any(),any(),any(),any(),any());
        verifyNoInteractions(coordinator);
        verifyNoInteractions(cleanup);
    }

    @Test void regularSharedTrainingDoesNotCleanAnArchiveWithMismatchedBytes() throws Exception {
        job.setSmartRoundId(null);
        job.setTrainingId(2L);
        when(objects.find(job.modelKey())).thenReturn(new ModelArtifactObjectStore.StoredArtifact("fixture", "pt", "best.pt", 11, repeatA()));
        assertThrows(Exception.class, () -> ReflectionTestUtils.invokeMethod(worker, "process", job));
        verify(shared, never()).release(any(), any(), any(), any(), any());
        verifyNoInteractions(cleanup);
    }

    @Test void queuedCancellationReleasesTicketWithoutStartingProcess() throws Exception {
        job.setJobState("QUEUED");job.setCancelRequested(true);
        ReflectionTestUtils.invokeMethod(worker,"process",job);
        verify(shared).release(eq(connection),eq(node),eq(job),argThat(result->"CANCELLED".equals(result.path("state").asText())),isNull());
        verify(connection,never()).execute(anyString(),anyInt());verifyNoInteractions(importer);
        verify(annotations).finished(job,"CANCELLED","训练已取消",false);
        verify(cleanup).enqueue(eq(job), argThat(ack -> "CANCELLED".equals(ack.path("state").asText())), isNull());
    }
}
