package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.data.*;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmCategoryEnum;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmTrainingStatusEnum;
import com.ruoyi.vlstream.test.vlstream.mapper.*;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import com.ruoyi.vlstream.test.vlstream.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@Tag("dev")
@EnabledIfEnvironmentVariable(named = "VLS_AUTODL_TEST_JDBC", matches = "jdbc:mysql://127\\.0\\.0\\.1:33329/autodl_contract_test.*")
class CloudComputeIntegrationTest {
    static JdbcTemplate jdbc;
    static DataSourceTransactionManager transactions;
    static Path root;
    ObjectMapper json = new ObjectMapper();
    ComputeSsh ssh;
    ComputeSsh.Connection connection;
    ComputeNodeService nodes;
    CloudTrainingService training;
    CloudTrainingWorker worker;
    ModelArtifactObjectStore storage;
    Long nodeId;
    AlgorithmTraining task;

    @BeforeAll static void database() throws Exception {
        DriverManagerDataSource source = new DriverManagerDataSource(System.getenv("VLS_AUTODL_TEST_JDBC"), "root", "autodl-test-only");
        jdbc = new JdbcTemplate(source); transactions = new DataSourceTransactionManager(source);
        root = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (!Files.exists(root.resolve("BUSINESS_PROCESSES.md"))) root = root.getParent();
        for (String table : Arrays.asList("vls_cloud_training_job", "vls_compute_node", "vls_training_publication", "vls_algorithm_training", "vls_container_instance", "vls_dataset_conversion_guard")) jdbc.execute("DROP TABLE IF EXISTS " + table);
        jdbc.execute("CREATE TABLE vls_algorithm_training(id BIGINT PRIMARY KEY,tenant_id VARCHAR(64),is_deleted INT DEFAULT 0,dataset_id BIGINT,train_status VARCHAR(20),progress INT,epoch_total INT,epoch_current INT,config_params TEXT,log_path TEXT,error_message TEXT,model_output_path TEXT,onnx_model_output_path TEXT,om_model_output_path TEXT,rknn_model_output_path TEXT,int8_rknn_model_output_path TEXT,onnx_conversion_status VARCHAR(20),om_conversion_status VARCHAR(20),start_time DATETIME,end_time DATETIME,update_time DATETIME)");
        jdbc.execute("CREATE TABLE vls_container_instance(id BIGINT PRIMARY KEY,tenant_id VARCHAR(64),training_task_id BIGINT,instance_status VARCHAR(20),error_message TEXT,stop_time DATETIME)");
        jdbc.execute("CREATE TABLE vls_dataset_conversion_guard(tenant_id VARCHAR(64),training_id BIGINT)");
        for (String file : Arrays.asList("V1_2_0_018__training_auto_publish.sql", "V1_2_0_025__autodl_ssh_compute.sql")) {
            String sql = new String(Files.readAllBytes(root.resolve("VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/db/migration/" + file)), StandardCharsets.UTF_8);
            for (String statement : sql.split(";")) if (!statement.trim().isEmpty()) jdbc.execute(statement);
        }
    }
    @BeforeEach void setup() throws Exception {
        for (String table : Arrays.asList("vls_cloud_training_job", "vls_compute_node", "vls_training_publication", "vls_algorithm_training", "vls_container_instance")) jdbc.update("DELETE FROM " + table);
        jdbc.update("INSERT INTO vls_algorithm_training(id,tenant_id,train_status) VALUES(1,'tenant-a','pending')");
        TenantContextHolder.setTenantId("tenant-a");
        ComputeCredentialCipher cipher = mock(ComputeCredentialCipher.class); when(cipher.encrypt(anyString())).thenReturn("encrypted-test-secret");
        ssh = mock(ComputeSsh.class); connection = mock(ComputeSsh.Connection.class); when(ssh.open(any())).thenReturn(connection);
        nodes = new ComputeNodeService(jdbc, cipher, ssh, json);
        nodeId = nodes.save(null, request()).getId();
        jdbc.update("UPDATE vls_compute_node SET probe_state='READY',probe_json='{\"contractVersion\":1,\"ready\":true}',host_key='test-key' WHERE id=?", nodeId);
        task = new AlgorithmTraining(); task.setId(1L); task.setTenantId("tenant-a"); task.setAlgorithmId(2L); task.setTaskName("cloud-test"); task.setTrainStatus(AlgorithmTrainingStatusEnum.pending); task.setEpochTotal(1);
        IVlsAlgorithmTrainingService trainings = mock(IVlsAlgorithmTrainingService.class); when(trainings.selectAlgorithmTrainingById(1L)).thenReturn(task);
        VlsAlgorithmMapper algorithms = mock(VlsAlgorithmMapper.class); Algorithm algorithm = new Algorithm(); algorithm.setCategory(AlgorithmCategoryEnum.detect); algorithm.setTenantId("tenant-a"); when(algorithms.selectById(2L)).thenReturn(algorithm);
        DataManagementService data = mock(DataManagementService.class);
        AlgorithmAnnotation project = new AlgorithmAnnotation(); project.setId(3L); project.setAnnotationType("object_detection"); when(data.project(3L)).thenReturn(project);
        DatasetSnapshot snapshot = CloudComputeContractTest.snapshot(); when(data.snapshot(3L)).thenReturn(snapshot); when(data.readSnapshot(anyString())).thenReturn(snapshot);
        DatasetVersion version = new DatasetVersion(); version.setId(4L); version.setSnapshotJson(json.writeValueAsString(snapshot)); when(data.saveVersion(eq(3L), any(DataRequests.Version.class))).thenReturn(version);
        TrainingPublicationService publication = new TrainingPublicationService(jdbc, transactions, mock(VlsAlgorithmTrainingMapper.class), mock(VlsAlgorithmModelMapper.class), mock(IVlsAlgorithmModelService.class));
        IVlsContainerInstanceService containers = mock(IVlsContainerInstanceService.class);
        when(containers.save(any())).thenAnswer(call -> { ContainerInstance instance = call.getArgument(0); instance.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId()); jdbc.update("INSERT INTO vls_container_instance(id,tenant_id,training_task_id,instance_status) VALUES(?,?,?,?)", instance.getId(), instance.getTenantId(), instance.getTrainingTaskId(), instance.getInstanceStatus()); return true; });
        training = new CloudTrainingService(jdbc, nodes, data, publication, trainings, containers, algorithms, json);
        storage = mock(ModelArtifactObjectStore.class);
        CloudTrainingDatasetWriter writer = mock(CloudTrainingDatasetWriter.class);
        when(writer.ensureArtifact(any())).thenReturn("vls-dataset://11111111-1111-1111-1111-111111111111");
        worker = new CloudTrainingWorker(jdbc, transactions, nodes, ssh, writer, data, storage, mock(RemoteModelArtifactService.class), json);
        ReflectionTestUtils.setField(worker, "tempDir", root.resolve("codex/autodl/tmp").toString());
    }
    @AfterEach void cleanup() { worker.shutdown(); TenantContextHolder.clear(); }
    ComputeRequests.Node request() {
        ComputeRequests.Node request = new ComputeRequests.Node(); request.setName("test-instance"); request.setHost("test.autodl.com"); request.setPassword("test-only"); return request;
    }
    String start() {
        ComputeRequests.Start request = new ComputeRequests.Start(); request.setNodeId(nodeId); request.setDatasetId(3L); request.setEpochs(1);
        new TransactionTemplate(transactions).execute(tx -> training.start(1L, request));
        task.setConfigParams(jdbc.queryForObject("SELECT config_params FROM vls_algorithm_training WHERE id=1", String.class));
        return training.current(task).getId();
    }
    @Test void migrationQueueFreezesSnapshotWithoutDefaultGpuPathAndRejectsDuplicateStart() {
        String job = start();
        assertEquals("QUEUED", training.current(task).getJobState());
        assertEquals("not_required", jdbc.queryForObject("SELECT onnx_conversion_status FROM vls_algorithm_training WHERE id=1", String.class));
        assertTrue(training.current(task).getRunDir().endsWith(job));
        assertThrows(RuntimeException.class, this::start);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM vls_cloud_training_job", Integer.class));
        verifyNoInteractions(ssh);
    }
    @Test void tenantCannotReadChangeProbeOrStartAnotherTenantsInstanceOrJob() {
        start(); TenantContextHolder.setTenantId("tenant-b");
        assertTrue(nodes.list().isEmpty());
        assertThrows(RuntimeException.class, () -> nodes.get(nodeId));
        assertThrows(RuntimeException.class, () -> nodes.probe(nodeId));
        assertThrows(RuntimeException.class, () -> nodes.save(nodeId, request()));
        assertThrows(RuntimeException.class, () -> nodes.delete(nodeId));
        assertThrows(RuntimeException.class, () -> training.current(task));
        assertThrows(RuntimeException.class, this::start);
        verifyNoInteractions(ssh);
    }
    @Test void legacyProbeRequiresARecheckAndStructuredProbeIsParsedWithoutDroppingTheJsonBrace() throws Exception {
        jdbc.update("UPDATE vls_compute_node SET probe_json='{}' WHERE id=?", nodeId);
        assertThrows(RuntimeException.class, () -> nodes.requireReady(nodeId));
        when(connection.hostKey()).thenReturn("pinned-key");
        when(connection.execute(anyString(), eq(180))).thenReturn("startup message\nVLS_PROBE={\"contractVersion\":1,\"ready\":true,\"checks\":[]}");
        assertEquals("READY", nodes.probe(nodeId).getProbeState());
        assertDoesNotThrow(() -> nodes.requireReady(nodeId));
        when(connection.execute(anyString(), eq(180))).thenReturn("VLS_PROBE={\"contractVersion\":1,\"ready\":false,\"message\":\"missing preset\"}");
        assertEquals("ERROR", nodes.probe(nodeId).getProbeState());
        assertThrows(RuntimeException.class, () -> nodes.requireReady(nodeId));
    }
    @Test void queuedCancellationDoesNotRequireAnOnlineInstanceAndActiveNodesCannotBeEdited() {
        start(); assertThrows(RuntimeException.class, () -> nodes.save(nodeId, request())); assertThrows(RuntimeException.class, () -> nodes.delete(nodeId));
        assertThrows(RuntimeException.class, () -> training.assertInactive(task));
        training.stop(task); worker.tick(nodeId);
        assertEquals("CANCELLED", training.current(task).getJobState()); verifyNoInteractions(ssh);
        assertDoesNotThrow(() -> nodes.delete(nodeId));
    }
    @Test void downloadedBytesMustMatchTheRemoteHashBeforeCompletion() throws Exception {
        start();
        when(connection.execute(startsWith("test -s "), anyInt())).thenReturn("READY");
        when(connection.execute(contains(" status "), anyInt())).thenReturn("VLS_CLOUD={\"state\":\"SUCCEEDED\",\"sha256\":\"" + ModelArtifactObjectStore.hash("data") + "\",\"size\":4}");
        doAnswer(call -> { Files.write(call.getArgument(1), "evil".getBytes(StandardCharsets.UTF_8)); return null; }).when(connection).download(anyString(), any(Path.class));
        Path tempRoot = root.resolve("codex/autodl/tmp"); Files.createDirectories(tempRoot);
        when(storage.archive(anyString(), anyString(), any())).thenAnswer(call -> {
            Path file = Files.createTempFile(tempRoot, "verify-", ".part");
            try { ((ModelArtifactObjectStore.Source) call.getArgument(2)).write(file); return true; } finally { Files.deleteIfExists(file); }
        });
        worker.tick(nodeId);
        assertEquals("ARCHIVING", training.current(task).getJobState());
        assertNull(jdbc.queryForObject("SELECT model_output_path FROM vls_algorithm_training WHERE id=1", String.class));
        doAnswer(call -> { Files.write(call.getArgument(1), "data".getBytes(StandardCharsets.UTF_8)); return null; }).when(connection).download(anyString(), any(Path.class));
        worker.tick(nodeId);
        assertEquals("COMPLETED", training.current(task).getJobState());
        AlgorithmTraining update = new AlgorithmTraining(); update.setConfigParams("{}");
        training.preserveExecutionConfig(task, update);
        assertTrue(CloudTrainingService.isCloud(update));
    }
    @Test void disconnectKeepsJobActiveRatherThanClaimingTrainingStopped() throws Exception {
        start(); when(ssh.open(any())).thenThrow(new IOException("offline")); worker.tick(nodeId);
        assertEquals("PREPARING", training.current(task).getJobState());
        assertNotNull(training.current(task).getMessage()); assertThrows(RuntimeException.class, () -> nodes.delete(nodeId));
    }
    @Test void completionWaitsForBothVerifiedArtifactsAndLogsRemainServerAuthoritative() throws Exception {
        start();
        when(connection.execute(startsWith("test -s "), anyInt())).thenReturn("READY");
        when(connection.execute(contains(" status "), anyInt())).thenReturn("VLS_CLOUD={\"state\":\"SUCCEEDED\",\"logs\":\"Training complete\",\"epoch\":1,\"sha256\":\"" + String.join("", Collections.nCopies(64, "a")) + "\",\"size\":4}");
        when(storage.archive(anyString(), anyString(), any())).thenReturn(false);
        worker.tick(nodeId); assertEquals("ARCHIVING", training.current(task).getJobState());
        assertFalse(training.logs(task).isCompleted()); assertTrue(training.logs(task).isServerManaged());
        assertNull(jdbc.queryForObject("SELECT model_output_path FROM vls_algorithm_training WHERE id=1", String.class));
        when(storage.archive(anyString(), anyString(), any())).thenReturn(true);
        worker.tick(nodeId); assertEquals("COMPLETED", training.current(task).getJobState());
        assertEquals(training.current(task).modelKey(), jdbc.queryForObject("SELECT model_output_path FROM vls_algorithm_training WHERE id=1", String.class));
        verify(storage, atLeastOnce()).archive(endsWith(".vls-classes.yaml"), eq("data.yaml"), any());
        verify(storage).archive(endsWith("/weights/best.pt"), eq("best.pt"), any());
    }
}
