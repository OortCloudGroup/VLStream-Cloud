package com.ruoyi.vlstream.test.vlstream.data;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.oss.core.OssClient;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationImage;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationInstance;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationLabel;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class TrainingDatasetArtifactServiceTest {
    private final ObjectMapper json = new ObjectMapper();
    private JdbcTemplate jdbc;
    private DataMediaStorage media;
    private OssClient storage;
    private TrainingDatasetArtifactService service;
    private TrainingDatasetArtifact row;
    private DatasetVersion version;
    private DatasetVersion savedVersion;
    private DatasetSnapshot snapshot;
    private Map<String, byte[]> source;
    private AtomicReference<byte[]> uploaded;
    private Path temp;

    @BeforeEach @SuppressWarnings({"unchecked", "rawtypes"}) void setup() throws Exception {
        TenantContextHolder.setTenantId("tenant-a");
        jdbc = mock(JdbcTemplate.class); media = mock(DataMediaStorage.class); storage = mock(OssClient.class);
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any(TransactionDefinition.class))).thenReturn(mock(TransactionStatus.class));
        service = spy(new TrainingDatasetArtifactService(jdbc, media, new SampleMediaInspector(), json, transactions));
        doReturn(storage).when(service).client(any());
        Path workspace = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (workspace != null && !Files.exists(workspace.resolve("BUSINESS_PROCESSES.md"))) workspace = workspace.getParent();
        assertNotNull(workspace, "Tests must run inside the project workspace");
        Path artifacts = Paths.get(System.getProperty("dataset.test.dir", workspace.resolve("codex/dataset-minio").toString()));
        Files.createDirectories(artifacts); temp = Files.createTempDirectory(artifacts, "artifact-unit-");
        ReflectionTestUtils.setField(service, "tempDir", temp.toString());
        when(storage.getConfigKey()).thenReturn("minio-original"); when(storage.getBucketName()).thenReturn("datasets-original");
        source = new HashMap<>(); uploaded = new AtomicReference<>();
        when(media.read(anyString())).thenAnswer(call -> new ByteArrayInputStream(source.get(call.getArgument(0))));
        doAnswer(call -> { uploaded.set(Files.readAllBytes(((File) call.getArgument(0)).toPath())); return null; })
            .when(storage).uploadFile(any(File.class), anyString(), eq("application/zip"));
        when(storage.getObjectMetadata(anyString())).thenAnswer(call -> { ObjectMetadata meta = new ObjectMetadata(); meta.setContentLength(uploaded.get().length); return meta; });
        when(storage.getObjectContent(anyString())).thenAnswer(call -> new ByteArrayInputStream(uploaded.get()));
        version = new DatasetVersion(); version.setId(30L); version.setAnnotationId(20L); version.setTenantId("tenant-a");
        when(jdbc.queryForList(contains("FOR UPDATE"), eq(Long.class), eq("tenant-a"), eq(20L))).thenReturn(Collections.singletonList(20L));
        when(jdbc.queryForObject(contains("FROM vls_dataset_cleanup"), eq(Long.class), eq("tenant-a"), eq(20L))).thenReturn(0L);
        savedVersion = new DatasetVersion(); savedVersion.setVersionNumber(3);
        row = new TrainingDatasetArtifact(); row.setId(UUID.randomUUID().toString()); row.setTenantId("tenant-a"); row.setDatasetId(20L); row.setVersionId(30L); row.setState("PENDING");
        when(jdbc.query(anyString(), any(RowMapper.class), any(), any(), any())).thenAnswer(call -> {
            if (((String) call.getArgument(0)).contains("FROM vls_dataset_version")) return Collections.singletonList(savedVersion);
            return Collections.singletonList(row);
        });
        when(jdbc.update(anyString(), org.mockito.ArgumentMatchers.<Object>any())).thenAnswer(call -> {
            String sql = call.getArgument(0);
            if (sql.contains("SET storage_config=")) {
                row.setStorageConfig(call.getArgument(1)); row.setStorageBucket(call.getArgument(2)); row.setObjectKey(call.getArgument(3));
                row.setFileSize(call.getArgument(4)); row.setSha256(call.getArgument(5));
            }
            if (sql.contains("SET storage_state='READY'")) row.setState("READY");
            if (sql.contains("SET storage_state='FAILED'")) row.setState("FAILED");
            return 1;
        });
        prepare("object_detection");
    }

    @AfterEach void cleanup() throws IOException {
        TenantContextHolder.clear();
        if (temp != null) {
            try (java.util.stream.Stream<Path> entries = Files.list(temp)) { assertEquals(0, entries.count(), "Temporary data packages must be removed on completion or failure"); }
            Files.deleteIfExists(temp);
        }
    }

    @ParameterizedTest @ValueSource(strings = {"object_detection", "image_classification", "instance_segmentation", "semantic_segmentation"})
    void archivesCompletePortableNativeTrainingPackages(String type) throws Exception {
        prepare(type);
        TrainingDatasetArtifact artifact = service.ensure(20L, version);
        assertEquals("READY", artifact.getState()); assertNotNull(uploaded.get());
        Map<String, byte[]> files = unzip(uploaded.get());
        String yaml = text(files, "dataset.yaml");
        assertTrue(yaml.contains("annotation_type: " + type)); assertFalse(yaml.contains("path:"));
        assertTrue(yaml.contains("names: [\"class-one\"]"));
        assertTrue(files.containsKey("version.json")); assertTrue(files.containsKey("vls-dataset.json"));
        assertTrue(files.keySet().stream().noneMatch(name -> name.endsWith(".py") || name.startsWith("/") || name.contains("..")));
        String image = "image_classification".equals(type) ? "classification/train/c000000/1.png" : "images/train/1.png";
        assertArrayEquals(source.get("source-1"), files.get(image));
        assertEquals(image, text(files, "coco_subset_20.txt"));
        assertEquals("30", json.readTree(files.get("version.json")).path("versionId").asText());
        assertEquals(type, json.readTree(files.get("vls-dataset.json")).path("annotationType").asText());
        if ("object_detection".equals(type)) assertTrue(text(files, "labels/train/1.txt").startsWith("0 "));
        if ("image_classification".equals(type)) assertTrue(files.containsKey("annotations/1.json"));
        if ("instance_segmentation".equals(type)) assertTrue(text(files, "annotations/1.json").contains("maskData"));
        if ("semantic_segmentation".equals(type)) assertNotNull(ImageIO.read(new ByteArrayInputStream(files.get("masks/1.png"))));
        verify(jdbc).update(contains("SET storage_state='READY'"), eq("tenant-a"), eq(row.getId()), anyString());
        verify(storage).getObjectContent(row.getObjectKey());
        assertEquals("minio-original", artifact.getStorageConfig()); assertEquals("datasets-original", artifact.getStorageBucket());
        assertEquals(sha(uploaded.get()), artifact.getSha256());
    }

    @Test void persistedFrozenSnapshotOverridesCallerSuppliedChanges() throws Exception {
        version.setSnapshotJson("{}");
        service.ensure(20L, version);
        assertTrue(unzip(uploaded.get()).containsKey("images/train/1.png"));
        verify(jdbc).query(contains("FROM vls_dataset_version WHERE tenant_id=? AND annotation_id=? AND id=?"), any(RowMapper.class), eq("tenant-a"), eq(20L), eq(30L));
    }

    @Test void readyVersionIsIdempotentAndDoesNotReadSourceOrUpload() throws Exception {
        row.setState("READY");
        assertSame(row, service.ensure(20L, version));
        verifyNoInteractions(media, storage);
        verify(jdbc, never()).update(anyString(), org.mockito.ArgumentMatchers.<Object>any());
    }

    @Test void changedVersionCannotReplaceExistingArchive() throws Exception {
        row.setSnapshotSha256("unexpected");
        assertThrows(IOException.class, () -> service.ensure(20L, version));
        verifyNoInteractions(media, storage);
    }

    @Test void busyLeaseDoesNotBuildOrOverwritePackage() {
        when(jdbc.update(contains("SET storage_state='BUILDING'"), any(), any(), any())).thenReturn(0);
        assertThrows(IOException.class, () -> service.ensure(20L, version));
        verifyNoInteractions(media, storage);
    }

    @Test void lostLeaseCannotPublishReady() throws Exception {
        when(jdbc.update(contains("SET storage_state='READY'"), any(), any(), any())).thenReturn(0);
        assertThrows(IOException.class, () -> service.ensure(20L, version));
        assertEquals("FAILED", row.getState());
        verify(jdbc).update(contains("SET storage_state='READY'"), eq("tenant-a"), eq(row.getId()), anyString());
    }

    @Test void corruptMinioReadbackIsTrackedAndNeverPublished() {
        doAnswer(call -> { byte[] bytes = uploaded.get().clone(); bytes[bytes.length - 1] ^= 1; return new ByteArrayInputStream(bytes); })
            .when(storage).getObjectContent(anyString());
        assertThrows(IOException.class, () -> service.ensure(20L, version));
        assertEquals("FAILED", row.getState()); assertNotNull(row.getObjectKey());
        verify(jdbc, never()).update(contains("SET storage_state='READY'"), any(), any(), any());
    }

    @Test void sameHashAcrossTrainAndValidationIsRejectedBeforeUpload() throws Exception {
        snapshot.getSamples().get(1).setContentSha256(snapshot.getSamples().get(0).getContentSha256()); freeze();
        assertThrows(ServiceException.class, () -> service.ensure(20L, version));
        verifyNoInteractions(storage, media);
    }

    @Test void decodedDuplicateContentCannotBypassMissingHistoricalHashes() throws Exception {
        snapshot.getSamples().forEach(image -> { image.setContentSha256(null); image.setFileSize(null); });
        source.put("source-2", source.get("source-1")); freeze();
        assertThrows(IOException.class, () -> service.ensure(20L, version));
        assertNull(uploaded.get());
    }

    @Test void unassignedEligibleSampleCannotSilentlyDropOut() throws Exception {
        snapshot.getSamples().get(1).setDatasetSplit("unassigned"); freeze();
        assertThrows(ServiceException.class, () -> service.ensure(20L, version));
        verifyNoInteractions(storage, media);
    }

    @Test void changedSourceDimensionsAreRejected() throws Exception {
        snapshot.getSamples().get(0).setMediaWidth(999); freeze();
        assertThrows(IOException.class, () -> service.ensure(20L, version)); assertNull(uploaded.get());
    }

    @Test void changedSourceChecksumIsRejected() {
        byte[] changed = source.get("source-2"); source.put("source-1", changed);
        assertThrows(IOException.class, () -> service.ensure(20L, version)); assertNull(uploaded.get());
    }

    @Test void over25MiBImageStopsReadingAndDoesNotUpload() {
        when(media.read("source-1")).thenReturn(new InputStream() {
            private long remaining = 25L * 1024 * 1024 + 1;
            @Override public int read() { return remaining-- > 0 ? 1 : -1; }
            @Override public int read(byte[] bytes, int offset, int length) { if (remaining <= 0) return -1; int read = (int) Math.min(length, remaining); Arrays.fill(bytes, offset, offset + read, (byte) 1); remaining -= read; return read; }
        });
        assertThrows(IOException.class, () -> service.ensure(20L, version)); assertNull(uploaded.get());
    }

    @Test void missingOrForeignTenantCannotBuild() {
        version.setTenantId("tenant-b"); assertThrows(ServiceException.class, () -> service.ensure(20L, version));
        version.setTenantId("tenant-a"); version.setAnnotationId(21L); assertThrows(ServiceException.class, () -> service.ensure(20L, version));
        TenantContextHolder.clear(); assertThrows(ServiceException.class, () -> service.require(20L, row.getReference()));
        verifyNoInteractions(jdbc, media, storage);
    }

    @Test void cleaningDatasetCannotAcquireLeaseOrWriteNewObject() {
        when(jdbc.queryForObject(contains("FROM vls_dataset_cleanup"), eq(Long.class), eq("tenant-a"), eq(20L))).thenReturn(1L);
        assertThrows(ServiceException.class, () -> service.ensure(20L, version));
        verifyNoInteractions(media, storage);
        verify(jdbc, never()).update(anyString(), org.mockito.ArgumentMatchers.<Object>any());
    }

    @Test void readyArchiveStillRequiresExistingOwnedDataset() {
        row.setState("READY");
        when(jdbc.queryForList(contains("FOR UPDATE"), eq(Long.class), eq("tenant-a"), eq(20L))).thenReturn(Collections.emptyList());
        assertThrows(ServiceException.class, () -> service.ensure(20L, version));
        verifyNoInteractions(media, storage);
    }

    @Test void arbitraryUrlObjectKeyOrMalformedReferenceCannotReachStorage() {
        for (String ref : Arrays.asList("https://minio/private/file.zip", "datasets/tenant/file.zip", "vls-dataset://1-1-1-1-1", row.getReference() + "/../x"))
            assertThrows(ServiceException.class, () -> service.require(20L, ref));
        verifyNoInteractions(jdbc, storage);
    }

    @Test @SuppressWarnings("unchecked") void requireUsesTenantDatasetAndUuidTogether() {
        when(jdbc.query(contains("AND id=? AND storage_state='READY'"), any(RowMapper.class), eq("tenant-a"), eq(21L), eq(row.getId()))).thenReturn(Collections.emptyList());
        assertThrows(ServiceException.class, () -> service.require(21L, row.getReference()));
        verify(jdbc).query(contains("WHERE tenant_id=? AND dataset_id=? AND id=? AND storage_state='READY'"), any(RowMapper.class), eq("tenant-a"), eq(21L), eq(row.getId()));
    }

    @Test void downloadUsesPersistedLocationAndVerifiesContent() throws Exception {
        service.ensure(20L, version);
        TrainingDatasetArtifact forged = new TrainingDatasetArtifact(); forged.setId(row.getId()); forged.setDatasetId(20L); forged.setObjectKey("another-tenant"); forged.setStorageConfig("different");
        Path file = service.download(forged);
        try { assertArrayEquals(uploaded.get(), Files.readAllBytes(file)); } finally { Files.deleteIfExists(file); }
        verify(service).client("minio-original"); verify(storage, never()).getObjectContent("another-tenant");
    }

    @Test void failedDownloadRemovesPartialFile() throws Exception {
        service.ensure(20L, version);
        when(storage.getObjectContent(row.getObjectKey())).thenReturn(new ByteArrayInputStream(new byte[]{1, 2}));
        assertThrows(IOException.class, () -> service.download(row));
    }

    @Test void changedBucketNeverFallsBackToDefaultStorage() throws Exception {
        service.ensure(20L, version);
        when(storage.getBucketName()).thenReturn("different-bucket");
        clearInvocations(storage);
        assertThrows(IOException.class, () -> service.download(row));
        verify(storage, never()).getObjectContent(anyString());
    }

    @Test @SuppressWarnings({"unchecked", "rawtypes"}) void cleanupIncludesFailedObjectsAndAlwaysFiltersTenantDataset() {
        when(jdbc.query(anyString(), any(RowMapper.class), any(), any())).thenReturn(Collections.singletonList(row));
        assertEquals(1, service.listForDataset(20L).size());
        verify(jdbc).query(contains("WHERE tenant_id=? AND dataset_id=? AND object_key IS NOT NULL"), any(RowMapper.class), eq("tenant-a"), eq(20L));
    }

    private void prepare(String type) throws Exception {
        snapshot = new DatasetSnapshot(); snapshot.setAnnotationType(type);
        AnnotationLabel label = new AnnotationLabel(); label.setId(11L); label.setName("class-one"); snapshot.setLabels(Collections.singletonList(label));
        List<AnnotationImage> samples = new ArrayList<>(); List<AnnotationInstance> instances = new ArrayList<>();
        for (int i = 1; i <= 2; i++) {
            BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB); image.setRGB(0, 0, i * 97);
            ByteArrayOutputStream output = new ByteArrayOutputStream(); ImageIO.write(image, "png", output); byte[] bytes = output.toByteArray();
            source.put("source-" + i, bytes);
            AnnotationImage sample = new AnnotationImage(); sample.setId((long) i); sample.setOriginalName(i + ".png"); sample.setLocalPath("source-" + i);
            sample.setMediaType("image"); sample.setMediaWidth(2); sample.setMediaHeight(2); sample.setFileSize((long) bytes.length); sample.setContentSha256(sha(bytes));
            sample.setDatasetSplit(i == 1 ? "train" : "val"); samples.add(sample);
            SmartAnnotationRequests.Box target = new SmartAnnotationRequests.Box(); target.setLabelId(11L);
            if (!"image_classification".equals(type)) { target.setX(0d); target.setY(0d); target.setWidth(2d); target.setHeight(2d); }
            if (type.endsWith("segmentation")) { BitSet pixels = new BitSet(4); pixels.set(0, 4); target.setMaskData(AnnotationMask.encode(pixels, 2, 2)); }
            AnnotationInstance instance = new AnnotationInstance(); instance.setId((long) i); instance.setImageId((long) i); instance.setLabelId(11L);
            instance.setAnnotationType(AnnotationPayloads.geometry(type)); instance.setAnnotationData(json.writeValueAsString(AnnotationPayloads.content(type, target))); instances.add(instance);
        }
        snapshot.setSamples(samples); snapshot.setInstances(instances); row.setAnnotationType(type); freeze();
    }

    private void freeze() throws Exception {
        savedVersion.setSnapshotJson(json.writeValueAsString(snapshot)); version.setSnapshotJson(savedVersion.getSnapshotJson());
        row.setSnapshotSha256(sha(savedVersion.getSnapshotJson().getBytes(StandardCharsets.UTF_8)));
        row.setDatasetYaml(new TrainingDatasetLayout(snapshot.getAnnotationType(), snapshot.getLabels()).yaml(".").replaceFirst("^path: [^\\n]*\\n", ""));
    }
    private static String sha(byte[] bytes) throws Exception {
        StringBuilder result = new StringBuilder(); for (byte value : MessageDigest.getInstance("SHA-256").digest(bytes)) result.append(String.format(Locale.ROOT, "%02x", value & 255)); return result.toString();
    }
    private static Map<String, byte[]> unzip(byte[] bytes) throws IOException {
        Map<String, byte[]> result = new LinkedHashMap<>();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry; byte[] buffer = new byte[4096];
            while ((entry = input.getNextEntry()) != null) { ByteArrayOutputStream output = new ByteArrayOutputStream(); int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count); result.put(entry.getName(), output.toByteArray()); }
        }
        return result;
    }
    private static String text(Map<String, byte[]> entries, String name) { return new String(entries.get(name), StandardCharsets.UTF_8); }
}
