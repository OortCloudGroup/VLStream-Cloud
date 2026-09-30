package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ModelArtifactArchiveDatasetReferenceTest {
    private static final String CONFIG = "{\"datasetArtifactRef\":\"vls-dataset://67f3bd17-aa76-41bb-93e7-c3c1b7ef1ee4\"}";
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final ModelArtifactObjectStore store = mock(ModelArtifactObjectStore.class);
    private final RemoteModelArtifactService remote = mock(RemoteModelArtifactService.class);
    private final ModelClassFileService classes = mock(ModelClassFileService.class);
    private final ModelArtifactArchiveService service = new ModelArtifactArchiveService(jdbc, store, remote, classes);
    private Path temporary;

    @BeforeEach
    void setUp() throws Exception {
        Path project = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (project != null && !Files.isRegularFile(project.resolve("BUSINESS_PROCESSES.md"))) project = project.getParent();
        if (project == null) throw new IllegalStateException("找不到项目根目录，测试文件只能写入项目 codex 目录");
        Path directory = project.resolve("codex/model-archive-reference-tests"); Files.createDirectories(directory);
        temporary = Files.createTempFile(directory, "classes-", ".yaml");
        String yaml = "names: [person]\n";
        when(classes.prepare(any())).thenReturn(new ModelClassFileService.ClassFile("data.yaml", yaml,
            yaml.getBytes(StandardCharsets.UTF_8).length, DigestUtils.sha256Hex(yaml)));
        doAnswer(call -> {
            ModelArtifactObjectStore.Source source = call.getArgument(2);
            source.write(temporary);
            return true;
        }).when(store).archive(endsWith(".vls-classes.yaml"), eq("classes.yaml"), any());
    }

    @AfterEach
    void clean() throws Exception {
        service.close(); TenantContextHolder.clear();
        if (temporary != null) Files.deleteIfExists(temporary);
    }

    @Test
    void currentTrainingPassesFrozenDatasetReferenceIntoClassArchival() throws Exception {
        Map<String, Object> row = row("/runs/current/weights/best.pt");
        row.put("dataset_id", 19L); row.put("config_params", CONFIG);

        service.archiveRow(row);

        ArgumentCaptor<AlgorithmTraining> training = ArgumentCaptor.forClass(AlgorithmTraining.class);
        verify(classes).prepare(training.capture());
        assertEquals(19L, training.getValue().getDatasetId());
        assertEquals(CONFIG, training.getValue().getConfigParams());
        assertEquals("tenant-a", training.getValue().getTenantId());
        assertEquals("names: [person]\n", new String(Files.readAllBytes(temporary), StandardCharsets.UTF_8));
    }

    @Test
    void historicalModelWithoutMatchingTrainingPathDoesNotBorrowCurrentReference() throws Exception {
        Map<String, Object> historical = row("/runs/previous/weights/best.pt");
        // A nonmatching LEFT JOIN preserves the model row while leaving execution metadata null.
        historical.put("dataset_id", null); historical.put("config_params", null);

        service.archiveRow(historical);

        ArgumentCaptor<AlgorithmTraining> training = ArgumentCaptor.forClass(AlgorithmTraining.class);
        verify(classes).prepare(training.capture());
        assertNull(training.getValue().getDatasetId());
        assertNull(training.getValue().getConfigParams());
        assertEquals("/runs/previous/weights/best.pt", training.getValue().getModelOutputPath());
    }

    @Test
    void savedModelQueryJoinsByTenantTrainingAndExactCaseSensitivePtPath() {
        when(jdbc.queryForList(anyString())).thenReturn(Collections.emptyList());

        service.scan();

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForList(sql.capture());
        assertTrue(sql.getValue().contains("t.id training_id,t.dataset_id,t.config_params"));
        assertTrue(sql.getValue().contains("UNION ALL SELECT m.tenant_id,m.training_id,t.dataset_id,t.config_params,m.model_path"));
        assertTrue(sql.getValue().contains("LEFT JOIN vls_algorithm_training t ON t.tenant_id=m.tenant_id AND t.id=m.training_id AND BINARY t.model_output_path=BINARY m.model_path"));
    }

    private Map<String, Object> row(String pt) {
        Map<String, Object> row = new HashMap<>();
        row.put("tenant_id", "tenant-a"); row.put("training_id", 71L); row.put("pt", pt);
        return row;
    }
}
