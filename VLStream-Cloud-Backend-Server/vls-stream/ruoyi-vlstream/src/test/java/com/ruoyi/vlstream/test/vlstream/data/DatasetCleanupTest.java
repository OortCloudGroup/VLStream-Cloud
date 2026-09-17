package com.ruoyi.vlstream.test.vlstream.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.oss.core.OssClient;
import com.ruoyi.vlstream.test.vlstream.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
@EnabledIfEnvironmentVariable(named="VLS_CLEANUP_TEST_JDBC", matches="jdbc:mysql://127\\.0\\.0\\.1:33318/annotation_cleanup_test.*")
class DatasetCleanupTest {
    static JdbcTemplate jdbc;
    static DriverManagerDataSource source;
    DatasetStorageProvider storage;
    DatasetRemoteCleanup remote;
    ModelClassFileService classes;
    OssClient client;
    DatasetCleanupService service;

    @BeforeAll static void schema() throws Exception {
        source = new DriverManagerDataSource(System.getenv("VLS_CLEANUP_TEST_JDBC"),"root","cleanup-test-only");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("DROP TABLE IF EXISTS vls_dataset_conversion_guard");
        jdbc.execute("DROP TABLE IF EXISTS sys_oss");
        jdbc.execute("CREATE TABLE sys_oss(service VARCHAR(64),file_name TEXT)");
        // Only this disposable, loopback-only schema is accepted by the test annotation.
        for (String table : Arrays.asList("vls_model_class_snapshot","vls_dataset_cleanup","vls_algorithm_annotation","vls_algorithm_training","vls_container_instance","vls_annotation_image","vls_annotation_label","vls_annotation_instance","vls_dataset_version","vls_dataset_import_job","vls_dataset_upload_part","vls_dataset_frame_origin")) jdbc.execute("DROP TABLE IF EXISTS " + table);
        jdbc.execute("CREATE TABLE vls_algorithm_annotation(id BIGINT PRIMARY KEY,tenant_id VARCHAR(64),dataset_path TEXT,is_deleted INT DEFAULT 0,total_count INT,annotated_count INT,progress INT)");
        jdbc.execute("CREATE TABLE vls_algorithm_training(id BIGINT PRIMARY KEY,tenant_id VARCHAR(64),dataset_id BIGINT,train_status VARCHAR(20),onnx_conversion_status VARCHAR(20),om_conversion_status VARCHAR(20),model_output_path TEXT,onnx_model_output_path TEXT,om_model_output_path TEXT,rknn_model_output_path TEXT)");
        jdbc.execute("ALTER TABLE vls_algorithm_training ADD COLUMN int8_rknn_model_output_path TEXT");
        jdbc.execute("CREATE TABLE vls_container_instance(id BIGINT PRIMARY KEY,tenant_id VARCHAR(64),training_task_id BIGINT,instance_status VARCHAR(20),env_config TEXT)");
        for (String table : Arrays.asList("vls_annotation_image","vls_annotation_label","vls_annotation_instance")) jdbc.execute("CREATE TABLE " + table + "(id BIGINT PRIMARY KEY,tenant_id VARCHAR(64),annotation_id BIGINT,local_path TEXT)");
        jdbc.execute("CREATE TABLE vls_dataset_version(id BIGINT PRIMARY KEY,tenant_id VARCHAR(64),annotation_id BIGINT,snapshot_json LONGTEXT)");
        jdbc.execute("CREATE TABLE vls_dataset_import_job(id BIGINT PRIMARY KEY,tenant_id VARCHAR(64),dataset_id BIGINT,job_state VARCHAR(20),storage_config VARCHAR(64),storage_bucket VARCHAR(64),object_key TEXT,multipart_id TEXT)");
        jdbc.execute("CREATE TABLE vls_dataset_upload_part(job_id BIGINT)");
        jdbc.execute("CREATE TABLE vls_dataset_frame_origin(tenant_id VARCHAR(64),dataset_id BIGINT)");
        Path root=Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (!Files.exists(root.resolve("BUSINESS_PROCESSES.md"))) root=root.getParent();
        String migration=new String(Files.readAllBytes(root.resolve("VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/db/migration/V1_2_0_017__dataset_cleanup_and_model_classes.sql")),StandardCharsets.UTF_8).replaceAll("(?m)^--.*$","");
        for (String sql:migration.split(";")) if (!sql.trim().isEmpty()) jdbc.execute(sql);
    }

    @BeforeEach void setup() {
        jdbc.update("DELETE FROM vls_dataset_conversion_guard");
        jdbc.update("DELETE FROM sys_oss");
        for (String table : Arrays.asList("vls_model_class_snapshot","vls_dataset_cleanup","vls_algorithm_annotation","vls_algorithm_training","vls_container_instance","vls_annotation_image","vls_annotation_label","vls_annotation_instance","vls_dataset_version","vls_dataset_import_job","vls_dataset_upload_part","vls_dataset_frame_origin")) jdbc.update("DELETE FROM " + table);
        TenantContextHolder.setTenantId("tenant-a");
        storage=mock(DatasetStorageProvider.class); remote=mock(DatasetRemoteCleanup.class); classes=mock(ModelClassFileService.class); client=mock(OssClient.class);
        when(storage.current()).thenReturn(client); when(storage.get("test")).thenReturn(client);
        when(client.getBucketName()).thenReturn("images"); when(client.getConfigKey()).thenReturn("test"); when(client.getStorageIdentity()).thenReturn("http://test/images");
        service=new DatasetCleanupService(jdbc,new DataSourceTransactionManager(source),storage,remote,classes,new ModelClassSnapshotStore(jdbc),new ObjectMapper());
        jdbc.update("INSERT INTO vls_algorithm_annotation(id,tenant_id) VALUES(1,'tenant-a'),(2,'tenant-b')");
        jdbc.update("INSERT INTO vls_annotation_image(id,tenant_id,annotation_id,local_path) VALUES(10,'tenant-a',1,'own.png'),(11,'tenant-a',1,'shared.png'),(12,'tenant-b',2,'shared.png')");
        jdbc.update("INSERT INTO sys_oss VALUES('test','own.png'),('test','shared.png')");
    }
    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    @Test void deletesOwnedDataPreservingSharedObjectsTrainingAndModelPaths() throws Exception {
        jdbc.update("INSERT INTO vls_algorithm_training(id,tenant_id,dataset_id,train_status,model_output_path,om_model_output_path) VALUES(50,'tenant-a',1,'completed','/runs/train/weights/best.pt','/runs/train/weights/best.om')");
        String yaml="nc: 1\nnames: [helmet]\n";
        when(classes.prepare(any())).thenReturn(new ModelClassFileService.ClassFile("dataset.yaml",yaml,yaml.length(),org.apache.commons.codec.digest.DigestUtils.sha256Hex(yaml)));
        assertTrue(service.delete(1L));
        verify(client).delete("own.png"); verify(client,never()).delete("shared.png"); verify(remote).remove(1L);
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_annotation_image WHERE annotation_id=1",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM vls_annotation_image WHERE annotation_id=2",Integer.class));
        assertEquals("/runs/train/weights/best.om",jdbc.queryForObject("SELECT om_model_output_path FROM vls_algorithm_training WHERE id=50",String.class));
        assertEquals(yaml,jdbc.queryForObject("SELECT content FROM vls_model_class_snapshot WHERE training_id=50",String.class));
        assertEquals("COMPLETED",state());
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM sys_oss",Integer.class));
        service.delete(1L); verify(client,times(1)).delete("own.png");
    }

    @Test void activeTrainingPreventsEveryFileMutation() {
        jdbc.update("INSERT INTO vls_algorithm_training(id,tenant_id,dataset_id,train_status) VALUES(50,'tenant-a',1,'training')");
        assertThrows(RuntimeException.class,()->service.delete(1L)); verifyNoInteractions(client,remote);
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_dataset_cleanup",Integer.class));
    }

    @Test void rknnCalibrationStillProtectsDatasetAfterOnnxAndOmFinish() {
        jdbc.update("INSERT INTO vls_dataset_conversion_guard(tenant_id,training_id,dataset_id) VALUES('tenant-a',50,1)");
        assertThrows(RuntimeException.class,()->service.delete(1L)); verifyNoInteractions(client,remote);
    }

    @Test void unqueuedDraftIsPreservedWithoutBlockingDeletion() {
        jdbc.update("INSERT INTO vls_algorithm_training(id,tenant_id,dataset_id,train_status) VALUES(50,'tenant-a',1,'pending')");
        assertTrue(service.delete(1L));
        assertEquals("pending",jdbc.queryForObject("SELECT train_status FROM vls_algorithm_training WHERE id=50",String.class));
    }

    @Test void queuedContainerProtectsCompletedTrainingRecord() {
        jdbc.update("INSERT INTO vls_algorithm_training(id,tenant_id,dataset_id,train_status) VALUES(50,'tenant-a',1,'completed')");
        jdbc.update("INSERT INTO vls_container_instance VALUES(51,'tenant-a',50,'queued','{}')");
        assertThrows(RuntimeException.class,()->service.delete(1L)); verifyNoInteractions(client,remote);
    }

    @Test void storageFailurePersistsIntentAndRetryCompletes() {
        doThrow(new IllegalStateException("offline")).doNothing().when(client).delete("own.png");
        assertThrows(RuntimeException.class,()->service.delete(1L)); assertEquals("FAILED",state());
        assertEquals(0,jdbc.queryForObject("SELECT is_deleted FROM vls_algorithm_annotation WHERE id=1",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_algorithm_annotation p WHERE id=1 AND NOT EXISTS (SELECT 1 FROM vls_dataset_cleanup c WHERE c.tenant_id=p.tenant_id AND c.annotation_id=p.id)",Integer.class));
        assertTrue(service.delete(1L)); assertEquals("COMPLETED",state());
    }

    @Test void changedStorageEndpointStopsRetry() {
        doThrow(new IllegalStateException("offline")).when(client).delete("own.png");
        assertThrows(RuntimeException.class,()->service.delete(1L));
        when(client.getStorageIdentity()).thenReturn("http://other/images");
        assertThrows(RuntimeException.class,()->service.delete(1L)); verify(client,times(1)).delete("own.png");
    }

    @Test void foreignTenantCannotDelete() {
        assertThrows(RuntimeException.class,()->service.delete(2L)); verifyNoInteractions(storage,remote);
    }

    @Test void missingModelMetadataStopsBeforeDeletingFiles() throws Exception {
        jdbc.update("INSERT INTO vls_algorithm_training(id,tenant_id,dataset_id,train_status,model_output_path) VALUES(50,'tenant-a',1,'completed','/model.pt')");
        when(classes.prepare(any())).thenThrow(new java.io.IOException("missing original classes"));
        assertThrows(RuntimeException.class,()->service.delete(1L)); verifyNoInteractions(client,remote);
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_dataset_cleanup",Integer.class));
    }

    @Test void remoteFailurePreservesDurableModelMetadataForRetry() throws Exception {
        jdbc.update("UPDATE vls_algorithm_annotation SET dataset_path=? WHERE id=1",DatasetRemoteCleanup.root(1L)+"/dataset.yaml");
        doThrow(new IllegalStateException("offline")).doNothing().when(remote).remove(1L);
        assertThrows(RuntimeException.class,()->service.delete(1L)); assertEquals("FAILED",state());
        assertTrue(service.delete(1L)); verify(remote,times(2)).remove(1L);
    }

    private String state() { return jdbc.queryForObject("SELECT cleanup_state FROM vls_dataset_cleanup WHERE annotation_id=1",String.class); }
}
