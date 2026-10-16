package com.ruoyi.vlstream.test.vlstream.service;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.oss.core.OssClient;
import java.io.File;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Optional offline fixture replay. OSS and JDBC are substitutes, not live MinIO/DB. */
@Tag("dev")
@EnabledIfSystemProperty(named = "autodl.pro.fixture", matches = ".+")
class AutoDlRealArtifactArchiveReplayTest {
    @TempDir Path temp;
    private Path fixture;
    private JsonNode summary;
    private ModelArtifactObjectStore store;
    private JdbcTemplate jdbc;
    private OssClient storage;
    private final Map<String, Path> objects = new HashMap<>();

    @BeforeEach void setup() throws Exception {
        fixture = Paths.get(System.getProperty("autodl.pro.fixture"));
        summary = new ObjectMapper().readTree(fixture.resolve("evidence/summary.json").toFile());
        jdbc = mock(JdbcTemplate.class, call -> {
            if (call.getMethod().getName().equals("update")) return 1;
            return RETURNS_DEFAULTS.answer(call);
        });
        storage = mock(OssClient.class);
        store = spy(new ModelArtifactObjectStore(jdbc));
        doReturn(storage).when(store).client(any());
        doReturn(null).when(store).find(anyString(), anyString());
        ReflectionTestUtils.setField(store, "tempDir", temp.resolve("staging").toString());
        TenantContextHolder.setTenantId("offline-replay-tenant");
        when(storage.getConfigKey()).thenReturn("file-backed-test-double");
        doAnswer(call -> {
            String key = call.getArgument(1);
            Path object = Files.createTempFile(temp, "object-", ".bin");
            Files.copy(((File) call.getArgument(0)).toPath(), object, StandardCopyOption.REPLACE_EXISTING);
            objects.put(key, object);
            return null;
        }).when(storage).uploadFile(any(File.class), anyString(), anyString());
        when(storage.getObjectMetadata(anyString())).thenAnswer(call -> {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(Files.size(objects.get(call.getArgument(0))));
            return metadata;
        });
        when(storage.getObjectContent(anyString())).thenAnswer(call -> Files.newInputStream(objects.get(call.getArgument(0))));
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @ParameterizedTest
    @ValueSource(strings = {"image_classification", "object_detection", "instance_segmentation", "semantic_segmentation"})
    void realPtAndClassesPassSourceAndObjectReadbackChecks(String type) throws Exception {
        JsonNode task = task(type);
        replay(task, "pt", "best.pt", task.path("ptSha256").asText(), task.path("ptBytes").asLong());
        replay(task, "classes", "classes.yaml", task.path("classesSha256").asText(), -1);
        assertEquals(2, objects.size());
        assertEquals("offline-replay-tenant", TenantContextHolder.getTenantId());
    }

    @Test void corruptedReadbackOfRealPtCannotPublishReady() throws Exception {
        doAnswer(call -> new FilterInputStream(Files.newInputStream(objects.get(call.getArgument(0)))) {
            boolean first = true;
            @Override public int read(byte[] buffer, int offset, int length) throws java.io.IOException {
                int count = super.read(buffer, offset, length);
                if (first && count > 0) { buffer[offset] ^= 1; first = false; }
                return count;
            }
        }).when(storage).getObjectContent(anyString());
        JsonNode task = task("image_classification");
        assertThrows(java.io.IOException.class, () -> store.archive(task.path("pt").asText(), "best.pt", local -> copy(task, "pt", local)));
        assertEquals(0, readyWrites());
        assertTrue(mockingDetails(jdbc).getInvocations().stream().anyMatch(call -> call.getArgument(0).toString().contains("storage_state='FAILED'")));
    }

    @Test void wrongObjectSizeOfRealPtCannotPublishReady() throws Exception {
        ObjectMetadata wrong = new ObjectMetadata(); wrong.setContentLength(1);
        doReturn(wrong).when(storage).getObjectMetadata(anyString());
        JsonNode task = task("image_classification");
        assertThrows(java.io.IOException.class, () -> store.archive(task.path("pt").asText(), "best.pt", local -> copy(task, "pt", local)));
        assertEquals(0, readyWrites());
    }

    private void replay(JsonNode task, String field, String fileName, String expectedSha, long expectedSize) throws Exception {
        Path source = source(task, field);
        if (expectedSize >= 0) assertEquals(expectedSize, Files.size(source));
        try (InputStream input = Files.newInputStream(source)) { assertEquals(expectedSha, ModelArtifactObjectStore.digest(input)); }
        assertTrue(store.archive(task.path(field).asText(), fileName, local -> copy(task, field, local)));
        assertTrue(mockingDetails(jdbc).getInvocations().stream().anyMatch(call -> {
            Object[] args = call.getArguments();
            return args.length > 5 && args[0].toString().contains("storage_state='READY'") && expectedSha.equals(args[5]);
        }));
    }
    private long readyWrites() {
        return mockingDetails(jdbc).getInvocations().stream().filter(call -> call.getArgument(0).toString().contains("storage_state='READY'")).count();
    }
    private JsonNode task(String type) {
        for (JsonNode task : summary.path("tasks")) if (type.equals(task.path("annotationType").asText())) return task;
        throw new AssertionError("Missing real GPU fixture: " + type);
    }
    private Path source(JsonNode task, String field) {
        String path = task.path(field).asText();
        assertTrue(path.startsWith("/root/vls-pro/runs/"));
        return fixture.resolve(path.substring("/root/vls-pro/".length()));
    }
    private void copy(JsonNode task, String field, Path target) throws java.io.IOException {
        Files.copy(source(task, field), target, StandardCopyOption.REPLACE_EXISTING);
    }
}
