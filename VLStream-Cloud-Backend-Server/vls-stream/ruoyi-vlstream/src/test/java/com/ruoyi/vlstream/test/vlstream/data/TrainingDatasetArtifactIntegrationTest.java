package com.ruoyi.vlstream.test.vlstream.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.oss.core.OssClient;
import com.ruoyi.oss.properties.OssProperties;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationImage;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationInstance;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationLabel;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/** Never uses application properties, the default OSS factory, or an existing business database. */
@Tag("dev")
@EnabledIfEnvironmentVariable(named = "VLS_DATASET_ARTIFACT_TEST_JDBC", matches = "jdbc:mysql://127\\.0\\.0\\.1:33329/dataset_artifact_test.*")
class TrainingDatasetArtifactIntegrationTest {
    private static JdbcTemplate jdbc;
    private static DataSourceTransactionManager transactions;
    private static OssClient objects;
    private static Path workspace;
    private static String bucket;
    private final ObjectMapper json = new ObjectMapper();
    private final Map<String, byte[]> source = new ConcurrentHashMap<>();
    private TrainingDatasetArtifactService service;
    private DatasetVersion version;
    private Path temporary;
    private volatile CountDownLatch sourceEntered;
    private volatile CountDownLatch releaseSource;
    private final AtomicBoolean holdOnce = new AtomicBoolean();

    @BeforeAll static void isolatedServices() throws Exception {
        String jdbcUrl = System.getenv("VLS_DATASET_ARTIFACT_TEST_JDBC");
        String endpoint = System.getenv("VLS_DATASET_ARTIFACT_TEST_MINIO");
        Assumptions.assumeTrue("http://127.0.0.1:9002".equals(endpoint), "Dedicated loopback MinIO must be explicitly enabled");
        if (jdbcUrl == null || !jdbcUrl.matches("jdbc:mysql://127\\.0\\.0\\.1:33329/dataset_artifact_test.*")) throw new IllegalStateException("Dedicated dataset artifact test database required");
        DriverManagerDataSource database = new DriverManagerDataSource(jdbcUrl, "root", "autodl-test-only");
        jdbc = new JdbcTemplate(database); transactions = new DataSourceTransactionManager(database);
        workspace = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (workspace != null && !Files.exists(workspace.resolve("BUSINESS_PROCESSES.md"))) workspace = workspace.getParent();
        assertNotNull(workspace, "Project workspace must be discoverable");
        for (String table : Arrays.asList("vls_training_dataset_artifact", "vls_training_dataset_remote_usage", "vls_dataset_cleanup", "vls_dataset_version", "vls_algorithm_annotation")) jdbc.execute("DROP TABLE IF EXISTS " + table);
        jdbc.execute("CREATE TABLE vls_algorithm_annotation(id BIGINT PRIMARY KEY,tenant_id VARCHAR(64),dataset_path TEXT,is_deleted INT NOT NULL DEFAULT 0)");
        jdbc.execute("CREATE TABLE vls_dataset_version(id BIGINT PRIMARY KEY,tenant_id VARCHAR(64),annotation_id BIGINT,version_number INT,version_name VARCHAR(100),snapshot_json LONGTEXT,is_deleted INT NOT NULL DEFAULT 0)");
        jdbc.execute("CREATE TABLE vls_dataset_cleanup(tenant_id VARCHAR(64),annotation_id BIGINT,cleanup_state VARCHAR(20),PRIMARY KEY(tenant_id,annotation_id))");
        // Exercise both historical seed branches before applying the actual migration.
        jdbc.update("INSERT INTO vls_algorithm_annotation(id,tenant_id,dataset_path) VALUES(901,'legacy-a','/data/datasets/version/dataset.yaml'),(902,'legacy-a',NULL),(903,'new-tenant',NULL)");
        jdbc.update("INSERT INTO vls_dataset_version(id,tenant_id,annotation_id,version_number,version_name,snapshot_json) VALUES(999,'legacy-a',902,1,'训练生成快照','{}')");
        Path migration = workspace.resolve("VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/db/migration/V1_2_0_026__training_dataset_object_storage.sql");
        try (Connection connection = database.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new EncodedResource(new FileSystemResource(migration.toFile()), StandardCharsets.UTF_8));
            ScriptUtils.executeSqlScript(connection, new EncodedResource(new FileSystemResource(migration.toFile()), StandardCharsets.UTF_8));
        }
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM vls_training_dataset_remote_usage", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM vls_training_dataset_remote_usage WHERE dataset_id=903", Integer.class));
        bucket = "vls-dataset-artifact-test-" + UUID.randomUUID().toString().replace("-", "");
        OssProperties properties = new OssProperties(); properties.setEndpoint("127.0.0.1:9002"); properties.setRegion("us-east-1");
        properties.setBucketName(bucket); properties.setAccessKey("dataset-test-key"); properties.setSecretKey("dataset-test-secret-2026");
        properties.setIsHttps("N"); properties.setAccessPolicy("0");
        objects = new OssClient("dataset-artifact-integration", properties);
    }

    @BeforeEach void fixture() throws Exception {
        for (String table : Arrays.asList("vls_training_dataset_artifact", "vls_training_dataset_remote_usage", "vls_dataset_cleanup", "vls_dataset_version", "vls_algorithm_annotation")) jdbc.update("DELETE FROM " + table);
        TenantContextHolder.setTenantId("tenant-a");
        Path directory = Paths.get(System.getProperty("dataset.test.dir", workspace.resolve("codex/dataset-minio").toString()));
        Files.createDirectories(directory); temporary = Files.createTempDirectory(directory, "artifact-integration-");
        DataMediaStorage media = new DataMediaStorage() {
            @Override public InputStream read(String key) {
                if (holdOnce.compareAndSet(true, false)) {
                    sourceEntered.countDown();
                    try { if (!releaseSource.await(15, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out awaiting concurrency assertion"); }
                    catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw new IllegalStateException(ex); }
                }
                byte[] bytes = source.get(key);
                if (bytes == null) throw new IllegalStateException("Unknown synthetic image");
                return new ByteArrayInputStream(bytes);
            }
        };
        service = new TrainingDatasetArtifactService(jdbc, media, new SampleMediaInspector(), json, transactions) {
            @Override protected OssClient client(String key) {
                if (key != null && !"dataset-artifact-integration".equals(key)) throw new IllegalArgumentException("Unexpected storage configuration");
                return objects;
            }
        };
        ReflectionTestUtils.setField(service, "tempDir", temporary.toString());
        prepareVersion("object_detection");
    }

    @AfterEach void cleanup() throws Exception {
        if (releaseSource != null) releaseSource.countDown();
        TenantContextHolder.clear();
        if (jdbc != null && objects != null) {
            for (String key : jdbc.queryForList("SELECT object_key FROM vls_training_dataset_artifact WHERE object_key IS NOT NULL", String.class)) objects.delete(key);
        }
        if (temporary != null) {
            try (java.util.stream.Stream<Path> paths = Files.list(temporary)) { assertEquals(0, paths.count(), "Service must remove temporary packages on success and failure"); }
            Files.deleteIfExists(temporary);
        }
    }

    @ParameterizedTest @ValueSource(strings = {"object_detection", "image_classification", "instance_segmentation", "semantic_segmentation"})
    void realPackageRoundTripPreservesFrozenNativeDataset(String type) throws Exception {
        prepareVersion(type);
        TrainingDatasetArtifact artifact = service.ensure(20L, version);
        assertEquals("READY", artifact.getState()); assertEquals(bucket, artifact.getStorageBucket());
        assertEquals("dataset-artifact-integration", artifact.getStorageConfig());
        assertEquals(type, artifact.getAnnotationType()); assertFalse(artifact.getDatasetYaml().contains("path:"));
        assertEquals(1, jdbc.queryForObject("SELECT attempts FROM vls_training_dataset_artifact WHERE id=?", Integer.class, artifact.getId()));
        Path downloaded = service.download(artifact);
        try {
            assertEquals(artifact.getFileSize().longValue(), Files.size(downloaded));
            assertEquals(artifact.getSha256(), DatasetFileIO.sha256(downloaded));
            Map<String, byte[]> entries = unzip(downloaded);
            assertEquals(artifact.getDatasetYaml(), new String(entries.get("dataset.yaml"), StandardCharsets.UTF_8));
            assertEquals("30", json.readTree(entries.get("version.json")).path("versionId").asText());
            assertEquals(2, json.readTree(entries.get("vls-dataset.json")).path("samples").size());
            assertFalse(entries.keySet().stream().anyMatch(name -> name.startsWith("/") || name.endsWith(".py") || name.contains("..")));
            String train = "image_classification".equals(type) ? "classification/train/c000000/1.png" : "images/train/1.png";
            assertArrayEquals(source.get("sample-1"), entries.get(train));
            assertEquals(train, new String(entries.get("coco_subset_20.txt"), StandardCharsets.UTF_8));
            if ("object_detection".equals(type)) assertTrue(entries.containsKey("labels/train/1.txt"));
            if ("instance_segmentation".equals(type)) assertTrue(new String(entries.get("annotations/1.json"), StandardCharsets.UTF_8).contains("maskData"));
            if ("semantic_segmentation".equals(type)) assertNotNull(ImageIO.read(new ByteArrayInputStream(entries.get("masks/1.png"))));
        } finally { Files.deleteIfExists(downloaded); }
        source.clear(); // READY is independent of source availability and never rebuilds.
        assertEquals(artifact.getId(), service.ensure(20L, version).getId());
        assertEquals(1, jdbc.queryForObject("SELECT attempts FROM vls_training_dataset_artifact WHERE id=?", Integer.class, artifact.getId()));
    }

    @Test void tenantAndDatasetMismatchCannotResolveOrDownload() throws Exception {
        TrainingDatasetArtifact artifact = service.ensure(20L, version);
        assertThrows(ServiceException.class, () -> service.require(21L, artifact.getReference()));
        TenantContextHolder.setTenantId("tenant-b");
        assertThrows(ServiceException.class, () -> service.require(20L, artifact.getReference()));
        assertThrows(ServiceException.class, () -> service.requireReference(artifact.getReference()));
        assertThrows(ServiceException.class, () -> service.download(artifact));
        assertThrows(ServiceException.class, () -> service.ensure(20L, version));
    }

    @Test void alteredObjectOfSameSizeFailsSha256DownloadValidation() throws Exception {
        TrainingDatasetArtifact artifact = service.ensure(20L, version);
        Path downloaded = service.download(artifact);
        byte[] corrupt;
        try { corrupt = Files.readAllBytes(downloaded); } finally { Files.deleteIfExists(downloaded); }
        corrupt[corrupt.length - 1] ^= 1;
        objects.upload(corrupt, artifact.getObjectKey(), "application/zip");
        assertThrows(IOException.class, () -> service.download(artifact));
    }

    @Test void committedLeaseIsVisibleWhileSourceIoHoldsNoDatasetRowLock() throws Exception {
        sourceEntered = new CountDownLatch(1); releaseSource = new CountDownLatch(1); holdOnce.set(true);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        Future<TrainingDatasetArtifact> building = workers.submit(() -> {
            TenantContextHolder.setTenantId("tenant-a");
            try { return service.ensure(20L, version); } finally { TenantContextHolder.clear(); }
        });
        try {
            assertTrue(sourceEntered.await(10, TimeUnit.SECONDS), "Builder must reach source IO");
            assertEquals("BUILDING", jdbc.queryForObject("SELECT storage_state FROM vls_training_dataset_artifact WHERE tenant_id='tenant-a' AND dataset_id=20", String.class));
            Future<Boolean> cleanupGuard = workers.submit(() -> new TransactionTemplate(transactions).execute(status -> {
                jdbc.queryForObject("SELECT id FROM vls_algorithm_annotation WHERE tenant_id='tenant-a' AND id=20 FOR UPDATE", Long.class);
                return jdbc.queryForObject("SELECT COUNT(*) FROM vls_training_dataset_artifact WHERE tenant_id='tenant-a' AND dataset_id=20 AND storage_state IN ('PENDING','BUILDING')", Integer.class) > 0;
            }));
            assertTrue(cleanupGuard.get(3, TimeUnit.SECONDS), "Cleanup can lock the project and sees the committed active archive lease");
            assertThrows(IOException.class, () -> service.ensure(20L, version), "A concurrent second builder must not take over the live lease");
            releaseSource.countDown();
            assertEquals("READY", building.get(15, TimeUnit.SECONDS).getState());
        } finally {
            releaseSource.countDown();
            try { building.get(15, TimeUnit.SECONDS); } finally { workers.shutdownNow(); workers.awaitTermination(5, TimeUnit.SECONDS); }
        }
    }

    @Test void cleanupIntentBlocksBothNewArchiveAndReadyReuse() throws Exception {
        jdbc.update("INSERT INTO vls_dataset_cleanup(tenant_id,annotation_id,cleanup_state) VALUES('tenant-a',20,'PENDING')");
        assertThrows(ServiceException.class, () -> service.ensure(20L, version));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM vls_training_dataset_artifact", Integer.class));
        jdbc.update("DELETE FROM vls_dataset_cleanup");
        service.ensure(20L, version);
        jdbc.update("INSERT INTO vls_dataset_cleanup(tenant_id,annotation_id,cleanup_state) VALUES('tenant-a',20,'PENDING')");
        assertThrows(ServiceException.class, () -> service.ensure(20L, version));
    }

    private void prepareVersion(String type) throws Exception {
        DatasetSnapshot snapshot = new DatasetSnapshot(); snapshot.setAnnotationType(type);
        AnnotationLabel label = new AnnotationLabel(); label.setId(11L); label.setName("synthetic"); snapshot.setLabels(Collections.singletonList(label));
        List<AnnotationImage> samples = new ArrayList<>(); List<AnnotationInstance> instances = new ArrayList<>();
        for (int index = 1; index <= 2; index++) {
            BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB); image.setRGB(0, 0, index * 97);
            ByteArrayOutputStream encoded = new ByteArrayOutputStream(); ImageIO.write(image, "png", encoded); byte[] bytes = encoded.toByteArray(); source.put("sample-" + index, bytes);
            AnnotationImage sample = new AnnotationImage(); sample.setId((long) index); sample.setOriginalName(index + ".png"); sample.setLocalPath("sample-" + index);
            sample.setMediaType("image"); sample.setMediaWidth(2); sample.setMediaHeight(2); sample.setFileSize((long) bytes.length); sample.setContentSha256(sha(bytes));
            sample.setDatasetSplit(index == 1 ? "train" : "val"); samples.add(sample);
            SmartAnnotationRequests.Box target = new SmartAnnotationRequests.Box(); target.setLabelId(11L);
            if (!"image_classification".equals(type)) { target.setX(0d); target.setY(0d); target.setWidth(2d); target.setHeight(2d); }
            if (type.endsWith("segmentation")) { BitSet pixels = new BitSet(4); pixels.set(0, 4); target.setMaskData(AnnotationMask.encode(pixels, 2, 2)); }
            AnnotationInstance instance = new AnnotationInstance(); instance.setId((long) index); instance.setImageId((long) index); instance.setLabelId(11L);
            instance.setAnnotationType(AnnotationPayloads.geometry(type)); instance.setAnnotationData(json.writeValueAsString(AnnotationPayloads.content(type, target))); instances.add(instance);
        }
        snapshot.setSamples(samples); snapshot.setInstances(instances);
        version = new DatasetVersion(); version.setId(30L); version.setTenantId("tenant-a"); version.setAnnotationId(20L); version.setVersionNumber(1); version.setSnapshotJson(json.writeValueAsString(snapshot));
        jdbc.update("INSERT IGNORE INTO vls_algorithm_annotation(id,tenant_id) VALUES(20,'tenant-a')");
        jdbc.update("DELETE FROM vls_dataset_version WHERE id=30");
        jdbc.update("INSERT INTO vls_dataset_version(id,tenant_id,annotation_id,version_number,version_name,snapshot_json) VALUES(30,'tenant-a',20,1,'integration',?)", version.getSnapshotJson());
    }

    private static String sha(byte[] bytes) throws Exception {
        StringBuilder value = new StringBuilder(); for (byte item : MessageDigest.getInstance("SHA-256").digest(bytes)) value.append(String.format(Locale.ROOT, "%02x", item & 255)); return value.toString();
    }
    private static Map<String, byte[]> unzip(Path file) throws IOException {
        Map<String, byte[]> result = new LinkedHashMap<>();
        try (ZipInputStream input = new ZipInputStream(Files.newInputStream(file))) {
            ZipEntry entry; byte[] buffer = new byte[4096];
            while ((entry = input.getNextEntry()) != null) { ByteArrayOutputStream output = new ByteArrayOutputStream(); int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count); result.put(entry.getName(), output.toByteArray()); }
        }
        return result;
    }
}
