package com.ruoyi.vlstream.test.vlstream.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.oss.core.OssClient;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import com.ruoyi.vlstream.test.vlstream.service.ModelClassFileService;
import com.ruoyi.vlstream.test.vlstream.service.ModelClassSnapshotStore;
import org.junit.jupiter.api.*;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.util.*;
import java.sql.ResultSet;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Cleanup ownership and storage routing, without a database, MinIO or GPU host. */
@Tag("dev")
class DatasetCleanupArtifactTest {
    private static final String TENANT = "tenant-a";
    private static final String REFERENCE = "vls-dataset://3b488e44-72a0-4eeb-b6f1-ec973d849ec0";
    private static final String IDENTITY = "gpu-a:22:trainer:/datasets";
    private JdbcTemplate jdbc;
    private DatasetStorageProvider storage;
    private DatasetRemoteCleanup remote;
    private TrainingDatasetArtifactService artifacts;
    private OssClient client;
    private DatasetCleanupService service;
    private ModelClassFileService classes;
    private ModelClassSnapshotStore snapshots;
    private ResultSet trainingRow;
    private String datasetPath;
    private String manifestJson;
    private String artifactState;
    private List<String> remoteIdentities;
    private List<String> sharedArtifacts;

    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(TENANT);
        datasetPath = REFERENCE;
        remoteIdentities = Collections.emptyList();
        sharedArtifacts = Collections.emptyList();
        jdbc = mock(JdbcTemplate.class, invocation -> {
            Object[] args = invocation.getArguments();
            String sql = args.length > 0 && args[0] instanceof String ? (String) args[0] : "";
            String method = invocation.getMethod().getName();
            if ("queryForObject".equals(method)) {
                if (args[1] == Long.class) return 1L;
                if (args[1] == Integer.class) {
                    if (sql.contains("COUNT(*) FROM vls_training_dataset_artifact") && sql.contains("storage_state IN ('PENDING','BUILDING')"))
                        return Arrays.asList("PENDING", "BUILDING").contains(artifactState) ? 1 : 0;
                    return sql.contains("COUNT(*) FROM vls_dataset_cleanup") && manifestJson != null ? 1 : 0;
                }
            }
            if ("queryForList".equals(method)) {
                if (sql.startsWith("SELECT dataset_path")) {
                    Map<String, Object> project = new LinkedHashMap<>();
                    project.put("dataset_path", datasetPath); project.put("is_deleted", 0);
                    return Collections.singletonList(project);
                }
                if (sql.startsWith("SELECT remote_identity")) return remoteIdentities;
                if (sql.startsWith("SELECT object_key FROM vls_training_dataset_artifact")) return sharedArtifacts;
                return Collections.emptyList();
            }
            if ("query".equals(method)) {
                if (trainingRow != null && sql.contains(" FROM vls_algorithm_training ")) {
                    String columns = sql.substring(0, sql.indexOf(" FROM "));
                    assertTrue(columns.contains("dataset_id"));
                    assertTrue(columns.contains("config_params"));
                    @SuppressWarnings("unchecked") RowMapper<AlgorithmTraining> mapper = (RowMapper<AlgorithmTraining>) args[1];
                    return Collections.singletonList(mapper.mapRow(trainingRow, 0));
                }
                return Collections.emptyList();
            }
            if ("queryForMap".equals(method) && sql.startsWith("SELECT cleanup_state")) {
                Map<String, Object> cleanup = new LinkedHashMap<>();
                cleanup.put("cleanup_state", "PENDING"); cleanup.put("manifest_json", manifestJson);
                return cleanup;
            }
            if ("update".equals(method) && sql.startsWith("INSERT INTO vls_dataset_cleanup")) {
                Object value = args[args.length - 1];
                if (value instanceof Object[]) {
                    Object[] values = (Object[]) value;
                    value = values[values.length - 1];
                }
                manifestJson = (String) value;
                return 1;
            }
            return Answers.RETURNS_DEFAULTS.answer(invocation);
        });
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any())).thenAnswer(invocation -> new SimpleTransactionStatus());
        storage = mock(DatasetStorageProvider.class);
        remote = mock(DatasetRemoteCleanup.class);
        artifacts = mock(TrainingDatasetArtifactService.class);
        client = mock(OssClient.class);
        when(storage.get("fixed-config")).thenReturn(client);
        when(client.getConfigKey()).thenReturn("fixed-config");
        when(client.getBucketName()).thenReturn("datasets");
        when(client.getStorageIdentity()).thenReturn("http://minio/datasets");
        when(remote.identity()).thenReturn(IDENTITY);
        TrainingDatasetArtifact artifact = artifact("training/tenant-a/current.zip");
        when(artifacts.require(1L, REFERENCE)).thenReturn(artifact);
        when(artifacts.listForDataset(1L)).thenReturn(Collections.singletonList(artifact));
        classes = mock(ModelClassFileService.class);
        snapshots = mock(ModelClassSnapshotStore.class);
        service = new DatasetCleanupService(jdbc, transactions, storage, remote, artifacts,
            classes, snapshots, new ObjectMapper());
    }

    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test void objectOnlyDatasetDeletesArtifactsWithoutConsultingGpuHost() throws Exception {
        assertTrue(service.delete(1L));
        verify(artifacts).require(1L, REFERENCE);
        verify(client).delete("training/tenant-a/current.zip");
        verifyNoInteractions(remote);
        DatasetCleanupService.Manifest plan = new ObjectMapper().readValue(manifestJson, DatasetCleanupService.Manifest.class);
        assertFalse(plan.isRemote());
        assertEquals("datasets", plan.getBuckets().get("fixed-config"));
        verify(jdbc).update("DELETE FROM vls_training_dataset_artifact WHERE tenant_id=? AND dataset_id=?", TENANT, 1L);
        verify(jdbc).update("DELETE FROM vls_training_dataset_remote_usage WHERE tenant_id=? AND dataset_id=?", TENANT, 1L);
    }

    @Test void pendingArtifactPreventsFreezingCleanupManifest() {
        artifactState = "PENDING";
        assertArtifactGenerationBlocksCleanup();
    }

    @Test void classSnapshotReceivesOriginalDatasetAndFrozenArtifactReference() throws Exception {
        String config = "{\"datasetArtifactRef\":\"" + REFERENCE + "\"}";
        trainingRow = mock(ResultSet.class);
        when(trainingRow.getLong(1)).thenReturn(50L);
        when(trainingRow.getString(2)).thenReturn(TENANT);
        when(trainingRow.getString(3)).thenReturn("/runs/training-50/weights/best.pt");
        when(trainingRow.getLong("dataset_id")).thenReturn(1L);
        when(trainingRow.getString("config_params")).thenReturn(config);
        ModelClassFileService.ClassFile classFile = new ModelClassFileService.ClassFile("dataset.yaml", "names: [helmet]\n", 16, "sha256");
        when(classes.prepare(any())).thenReturn(classFile);
        assertTrue(service.delete(1L));
        ArgumentCaptor<AlgorithmTraining> original = ArgumentCaptor.forClass(AlgorithmTraining.class);
        verify(classes).prepare(original.capture());
        assertEquals(1L, original.getValue().getDatasetId());
        assertEquals(config, original.getValue().getConfigParams());
        verify(snapshots).save(same(original.getValue()), same(classFile));
        verifyNoInteractions(remote);
    }

    @Test void buildingArtifactPreventsDeletingItsInFlightUpload() {
        artifactState = "BUILDING";
        assertArtifactGenerationBlocksCleanup();
    }

    private void assertArtifactGenerationBlocksCleanup() {
        assertTrue(assertThrows(ServiceException.class, () -> service.delete(1L)).getMessage().contains("训练数据包正在生成"));
        assertNull(manifestJson);
        verify(jdbc).queryForObject("SELECT COUNT(*) FROM vls_training_dataset_artifact WHERE tenant_id=? AND dataset_id=? AND storage_state IN ('PENDING','BUILDING')", Integer.class, TENANT, 1L);
        verifyNoInteractions(artifacts, storage, client, remote);
    }

    @Test void foreignTenantArtifactReferenceIsRejectedBeforeAnyFileOperation() {
        when(artifacts.require(1L, REFERENCE)).thenThrow(new ServiceException("训练数据集不存在或无权访问"));
        assertThrows(ServiceException.class, () -> service.delete(1L));
        assertNull(manifestJson);
        verify(artifacts, never()).listForDataset(anyLong());
        verifyNoInteractions(storage, client, remote);
    }

    @Test void persistedRemoteUsageIsStillCleanedAfterCurrentPointerWasInvalidated() throws Exception {
        datasetPath = null;
        remoteIdentities = Collections.singletonList(IDENTITY);
        assertTrue(service.delete(1L));
        verify(remote).remove(1L);
        verify(artifacts, never()).require(anyLong(), anyString());
    }

    @Test void usageOnAnotherGpuHostStopsBeforeDeletingObjects() throws Exception {
        remoteIdentities = Collections.singletonList("gpu-other:22:trainer:/datasets");
        assertTrue(assertThrows(ServiceException.class, () -> service.delete(1L)).getMessage().contains("服务器配置已变更"));
        assertNull(manifestJson);
        verify(client, never()).delete(anyString());
        verify(remote, never()).remove(anyLong());
    }

    @Test void remoteIdentityIsRecheckedBeforeRetryDeletesAnyMoreObjects() throws Exception {
        remoteIdentities = Collections.singletonList(IDENTITY);
        doThrow(new IllegalStateException("temporarily unavailable")).when(client).delete(anyString());
        assertThrows(ServiceException.class, () -> service.delete(1L));
        assertNotNull(manifestJson);
        when(remote.identity()).thenReturn("gpu-reconfigured:22:trainer:/datasets");
        assertThrows(ServiceException.class, () -> service.delete(1L));
        verify(client, times(1)).delete(anyString());
        verify(remote, never()).remove(anyLong());
    }

    @Test void historicalArtifactsAreIncludedButSharedObjectsArePreserved() throws Exception {
        when(artifacts.listForDataset(1L)).thenReturn(Arrays.asList(artifact("training/tenant-a/current.zip"), artifact("training/shared/old.zip")));
        sharedArtifacts = Collections.singletonList("training/shared/old.zip");
        assertTrue(service.delete(1L));
        DatasetCleanupService.Manifest plan = new ObjectMapper().readValue(manifestJson, DatasetCleanupService.Manifest.class);
        assertEquals(2, plan.getObjects().get("fixed-config").size());
        verify(client).delete("training/tenant-a/current.zip");
        verify(client, never()).delete("training/shared/old.zip");
    }

    @Test void changedArtifactBucketStopsBeforePreparingCleanup() {
        when(client.getBucketName()).thenReturn("another-bucket");
        assertThrows(ServiceException.class, () -> service.delete(1L));
        assertNull(manifestJson);
        verify(client, never()).delete(anyString());
        verifyNoInteractions(remote);
    }

    @Test void arbitraryRemotePathsRemainRejected() {
        datasetPath = "/another-project/dataset.yaml";
        assertThrows(ServiceException.class, () -> service.delete(1L));
        verifyNoInteractions(artifacts, storage, client, remote);
    }

    private TrainingDatasetArtifact artifact(String key) {
        TrainingDatasetArtifact value = new TrainingDatasetArtifact();
        value.setTenantId(TENANT); value.setDatasetId(1L);
        value.setStorageConfig("fixed-config"); value.setStorageBucket("datasets"); value.setObjectKey(key);
        return value;
    }
}
