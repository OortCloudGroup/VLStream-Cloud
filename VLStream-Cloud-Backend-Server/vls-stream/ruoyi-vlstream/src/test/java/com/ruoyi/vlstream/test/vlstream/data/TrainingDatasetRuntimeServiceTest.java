package com.ruoyi.vlstream.test.vlstream.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.compute.ComputeSsh;
import com.ruoyi.vlstream.test.vlstream.config.VlsSshProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class TrainingDatasetRuntimeServiceTest {
    private static final String ID = "67f3bd17-aa76-41bb-93e7-c3c1b7ef1ee4";
    private static final String ROOT = "/work/tenant/task/dataset";
    private static final String SHA = String.join("", Collections.nCopies(64, "a"));
    private static final String YAML = "path: .\ntrain: images/train\nval: images/val\nnc: 1\nnames: [person]\n";
    private static final byte[] IMAGE = {21, 33, 57, 79, 91, 112, 19};

    private final TrainingDatasetArtifactService artifacts = mock(TrainingDatasetArtifactService.class);
    private final DatasetRemoteCleanup cleanup = mock(DatasetRemoteCleanup.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ComputeSsh.Connection connection = mock(ComputeSsh.Connection.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final TrainingDatasetArtifact artifact = new TrainingDatasetArtifact();
    private final Map<String, byte[]> uploaded = new LinkedHashMap<>();
    private final TrainingDatasetRuntimeService service = new TrainingDatasetRuntimeService(artifacts, new VlsSshProperties(), cleanup, jdbc) {
        @Override protected ComputeSsh.Connection openDefault() { return connection; }
    };
    private Path temporary;

    @BeforeEach
    void setUp() throws Exception {
        artifact.setId(ID); artifact.setTenantId("tenant-a"); artifact.setDatasetId(19L);
        artifact.setDatasetYaml(YAML); artifact.setSha256(SHA); artifact.setState("READY");
        when(artifacts.require(19L, artifact.getReference())).thenReturn(artifact);
        TenantContextHolder.setTenantId("tenant-a");
        when(cleanup.identity()).thenReturn("default-compute-identity");
        when(jdbc.queryForList(contains("vls_training_dataset_remote_usage"), eq(String.class), eq("tenant-a"), eq(19L)))
            .thenReturn(Collections.singletonList("default-compute-identity"));
        ReflectionTestUtils.setField(service, "transactionManager", transactions);
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(connection.execute(anyString(), anyInt())).thenAnswer(call -> {
            String command = call.getArgument(0);
            if (command.startsWith("if [ -e ")) return "MISSING";
            if (command.startsWith("test -r ")) return "READY";
            return "";
        });
        doAnswer(call -> {
            uploaded.put(call.getArgument(0), bytes(call.getArgument(1)));
            return null;
        }).when(connection).put(anyString(), any(InputStream.class));
        doAnswer(call -> {
            uploaded.put(call.getArgument(0), ((String) call.getArgument(1)).getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(connection).putText(anyString(), anyString());
        Path project = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (project != null && !Files.isRegularFile(project.resolve("BUSINESS_PROCESSES.md"))) project = project.getParent();
        if (project == null) throw new IllegalStateException("找不到项目根目录，测试文件只能写入项目 codex 目录");
        Path base = project.resolve("codex/dataset-runtime-tests"); Files.createDirectories(base);
        temporary = Files.createTempDirectory(base, "run-");
    }

    @AfterEach
    void clean() throws Exception {
        TenantContextHolder.clear();
        if (temporary != null) {
            try (DirectoryStream<Path> files = Files.newDirectoryStream(temporary)) {
                for (Path file : files) Files.deleteIfExists(file);
            }
            Files.deleteIfExists(temporary);
        }
    }

    @Test
    void portableYamlAndCalibrationBecomeRuntimePathsWhileImagesStayUnchanged() throws Exception {
        Path zip = zip(baseEntries());
        when(artifacts.download(artifact)).thenReturn(zip);

        assertEquals(ROOT + "/dataset.yaml", service.upload(artifact, ROOT, connection));

        Map<?, ?> runtime = new Yaml().load(text(ROOT + "/dataset.yaml"));
        assertEquals(ROOT, runtime.get("path"));
        assertEquals("images/train", runtime.get("train"));
        assertEquals(ROOT + "/images/train/1.jpg\n", text(ROOT + "/coco_subset_20.txt"));
        assertArrayEquals(IMAGE, uploaded.get(ROOT + "/images/train/1.jpg"));
        assertEquals(artifact.getReference(), new ObjectMapper().readTree(text(ROOT + "/vls-artifact.json")).path("reference").asText());
        assertFalse(Files.exists(zip));
        try (DirectoryStream<Path> files = Files.newDirectoryStream(temporary)) { assertFalse(files.iterator().hasNext()); }
    }

    @Test
    void unsafeArchivePathsAndScriptsAreRejectedBeforeAnyRemoteOperation() throws Exception {
        for (String unsafe : Arrays.asList("../escape.txt", "/absolute.txt", "a\\b.txt", "a//b.txt", "a/./b.txt", "C:bad.txt", "run_training.py")) {
            Map<String, byte[]> entries = baseEntries(); entries.put(unsafe, new byte[]{1});
            Path zip = zip(entries); when(artifacts.download(artifact)).thenReturn(zip);
            assertThrows(IOException.class, () -> service.upload(artifact, ROOT, connection), unsafe);
            assertFalse(Files.exists(zip));
        }
        verifyNoInteractions(connection);
    }

    @Test
    void duplicatePathsAreRejectedBeforeAnyRemoteOperation() throws Exception {
        Map<String, byte[]> entries = baseEntries(); entries.put("a.txt", new byte[]{1}); entries.put("b.txt", new byte[]{2});
        Path zip = zip(entries);
        byte[] content = Files.readAllBytes(zip);
        replace(content, "b.txt".getBytes(StandardCharsets.UTF_8), "a.txt".getBytes(StandardCharsets.UTF_8));
        Files.write(zip, content); when(artifacts.download(artifact)).thenReturn(zip);

        assertThrows(IOException.class, () -> service.upload(artifact, ROOT, connection));
        verifyNoInteractions(connection);
        assertFalse(Files.exists(zip));
    }

    @Test
    void crcCorruptionIsFoundBeforeRemoteWrites() throws Exception {
        Path zip = zip(baseEntries()); byte[] content = Files.readAllBytes(zip);
        byte[] broken = IMAGE.clone(); broken[0]++;
        replace(content, IMAGE, broken);
        Files.write(zip, content); when(artifacts.download(artifact)).thenReturn(zip);

        assertThrows(IOException.class, () -> service.upload(artifact, ROOT, connection));
        verifyNoInteractions(connection);
        assertFalse(Files.exists(zip));
    }

    @Test
    void expansionLimitIsEnforcedBeforeRemoteWrites() throws Exception {
        ReflectionTestUtils.setField(service, "maxExpandedBytes", 4L);
        Path zip = zip(baseEntries()); when(artifacts.download(artifact)).thenReturn(zip);

        assertThrows(IOException.class, () -> service.upload(artifact, ROOT, connection));
        verifyNoInteractions(connection);
        assertFalse(Files.exists(zip));
    }

    @Test
    void invalidCalibrationAndFrozenYamlMismatchNeverUpload() throws Exception {
        Map<String, byte[]> entries = baseEntries();
        entries.put("coco_subset_20.txt", "../other/image.jpg".getBytes(StandardCharsets.UTF_8));
        when(artifacts.download(artifact)).thenReturn(zip(entries));
        assertThrows(IOException.class, () -> service.upload(artifact, ROOT, connection));
        when(artifacts.download(artifact)).thenReturn(zip(baseEntries()));
        artifact.setDatasetYaml(YAML.replace("person", "changed"));
        assertThrows(IOException.class, () -> service.upload(artifact, ROOT, connection));
        verifyNoInteractions(connection);
    }

    @Test
    void defaultUsageCommitsIndependentlyBeforeDirectoryCreationAndMarkerFollowsScripts() throws Exception {
        when(artifacts.download(artifact)).thenReturn(zip(baseEntries()));

        String runtime = service.prepareDefault(19L, artifact.getReference());

        assertTrue(runtime.startsWith(DatasetRemoteCleanup.root(19L) + "/artifact_" + ID + "_"));
        String root = runtime.substring(0, runtime.length() - "/dataset.yaml".length());
        InOrder order = inOrder(transactions, connection);
        order.verify(transactions).commit(any());
        order.verify(connection).mkdir(root);
        order.verify(connection).put(eq(root + "/four_task_runtime.py"), any());
        order.verify(connection).put(eq(root + "/run_training.py"), any());
        order.verify(connection).putText(eq(root + "/vls-artifact.json"), anyString());
        verify(transactions).getTransaction(argThat(definition -> definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        verify(jdbc).update(contains("INSERT IGNORE INTO vls_training_dataset_remote_usage"), eq("tenant-a"), eq(19L), eq("default-compute-identity"));
    }

    @Test
    void matchingDefaultRuntimeReusesOnlyTheSameIdentityAndCompleteScripts() throws Exception {
        String runtime = runtimePath();
        when(connection.execute(startsWith("if [ -e "), anyInt())).thenReturn(marker());

        assertEquals(runtime, service.restoreDefault(19L, artifact.getReference(), runtime));

        verify(artifacts, never()).download(any());
        verify(connection, never()).mkdir(anyString());
        verify(connection, never()).put(anyString(), any());
        verify(connection).execute(argThat(command -> command.startsWith("test -r ") && command.contains("four_task_runtime.py") && command.contains("run_training.py")), eq(30));
        verify(transactions).commit(any());
    }

    @Test
    void changedDefaultHostIdentityRollsBackWithoutRemoteOperations() throws Exception {
        when(jdbc.queryForList(contains("vls_training_dataset_remote_usage"), eq(String.class), eq("tenant-a"), eq(19L)))
            .thenReturn(Collections.singletonList("old-compute-identity"));

        assertThrows(IOException.class, () -> service.restoreDefault(19L, artifact.getReference(), runtimePath()));

        verify(transactions).rollback(any());
        verify(transactions, never()).commit(any());
        verify(connection, never()).execute(anyString(), anyInt());
        verify(connection, never()).mkdir(anyString());
        verify(artifacts, never()).download(any());
    }

    @Test
    void wrongArtifactPathOrMarkerCannotBeReusedOrOverwritten() throws Exception {
        assertThrows(IOException.class, () -> service.restoreDefault(19L, artifact.getReference(), runtimePath().replace(ID, "77f3bd17-aa76-41bb-93e7-c3c1b7ef1ee4")));
        verifyNoInteractions(connection, transactions);
        when(connection.execute(startsWith("if [ -e "), anyInt())).thenReturn(marker().replace(SHA, String.join("", Collections.nCopies(64, "b"))));

        assertThrows(IOException.class, () -> service.restoreDefault(19L, artifact.getReference(), runtimePath()));

        verify(connection, never()).mkdir(anyString());
        verify(connection, never()).put(anyString(), any());
        verify(artifacts, never()).download(any());
    }

    @Test
    void interruptedDefaultMaterializationRestoresTheSameRuntimePath() throws Exception {
        when(artifacts.download(artifact)).thenReturn(zip(baseEntries()));

        assertEquals(runtimePath(), service.restoreDefault(19L, artifact.getReference(), runtimePath()));

        verify(connection).put(eq(runtimePath().replace("dataset.yaml", "run_training.py")), any());
        assertTrue(uploaded.containsKey(runtimePath().replace("dataset.yaml", "vls-artifact.json")));
    }

    private Map<String, byte[]> baseEntries() {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("images/train/1.jpg", IMAGE);
        entries.put("dataset.yaml", YAML.getBytes(StandardCharsets.UTF_8));
        entries.put("coco_subset_20.txt", "images/train/1.jpg\n".getBytes(StandardCharsets.UTF_8));
        return entries;
    }

    private Path zip(Map<String, byte[]> entries) throws IOException {
        Path file = Files.createTempFile(temporary, "artifact-", ".zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(file))) {
            for (Map.Entry<String, byte[]> source : entries.entrySet()) {
                ZipEntry entry = new ZipEntry(source.getKey());
                entry.setMethod(ZipEntry.STORED); entry.setSize(source.getValue().length);
                CRC32 crc = new CRC32(); crc.update(source.getValue()); entry.setCrc(crc.getValue());
                output.putNextEntry(entry); output.write(source.getValue()); output.closeEntry();
            }
        }
        return file;
    }

    private static void replace(byte[] content, byte[] source, byte[] target) {
        int replacements = 0;
        for (int i = 0; i <= content.length - source.length; i++) {
            boolean match = true;
            for (int j = 0; j < source.length; j++) if (content[i + j] != source[j]) { match = false; break; }
            if (match) { System.arraycopy(target, 0, content, i, target.length); replacements++; i += source.length - 1; }
        }
        assertTrue(replacements > 0);
    }

    private static byte[] bytes(InputStream input) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int count;
        while ((count = input.read(buffer)) != -1) result.write(buffer, 0, count);
        return result.toByteArray();
    }

    private String text(String path) { return new String(uploaded.get(path), StandardCharsets.UTF_8); }
    private String runtimePath() { return DatasetRemoteCleanup.root(19L) + "/artifact_" + ID + "_" + ID + "/dataset.yaml"; }
    private String marker() { return "{\"reference\":\"" + artifact.getReference() + "\",\"sha256\":\"" + SHA + "\"}"; }
}
