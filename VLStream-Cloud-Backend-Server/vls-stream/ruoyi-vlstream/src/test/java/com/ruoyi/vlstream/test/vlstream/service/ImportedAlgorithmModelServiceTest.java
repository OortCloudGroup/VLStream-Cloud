package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmCategoryEnum;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.Algorithm;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ImportedAlgorithmModelServiceTest {
    private VlsAlgorithmMapper algorithms;
    private IVlsAlgorithmModelService models;
    private ModelArtifactObjectStore storage;
    private ImportedAlgorithmModelService service;

    @BeforeEach void setup() {
        algorithms = mock(VlsAlgorithmMapper.class);
        models = mock(IVlsAlgorithmModelService.class);
        storage = mock(ModelArtifactObjectStore.class);
        service = new ImportedAlgorithmModelService(algorithms, models, storage);
        TenantContextHolder.setTenantId("tenant-a");
    }

    @AfterEach void cleanup() { TenantContextHolder.clear(); }

    @Test void importsPairedArtifactsWithoutInventingTrainingTask() throws Exception {
        Algorithm algorithm = new Algorithm();
        algorithm.setId(42L);
        algorithm.setCategory(AlgorithmCategoryEnum.detect);
        when(algorithms.selectOne(any())).thenReturn(algorithm);
        when(storage.archive(anyString(), anyString(), any())).thenReturn(true);
        when(models.save(any(AlgorithmModel.class))).thenReturn(true);

        AlgorithmModel model = service.importModel(42L, "object_detection", "helmet", 1, "", pt(), yaml());

        assertEquals("tenant-a", model.getTenantId());
        assertEquals(Long.valueOf(42), model.getAlgorithmId());
        assertNull(model.getTrainingId());
        assertEquals("object_detection", model.getAnnotationType());
        assertTrue(model.getModelPath().matches("imports/[0-9a-f-]+/weights/model\\.pt"));
        verify(storage).archive(eq(model.getModelPath()), eq("model.pt"), any());
        verify(storage).archive(eq(ModelClassFileService.storagePath(model.getModelPath())), eq("data.yaml"), any());
        verify(models).save(same(model));
    }

    @Test void rejectsMissingTenantAlgorithmBeforeSavingFiles() {
        assertThrows(RuntimeException.class, () -> service.importModel(42L, "object_detection", "helmet", 1, "", pt(), yaml()));
        verifyNoInteractions(storage);
        verify(models, never()).save(any());
    }

    @Test void rejectsInvalidClassNumberingBeforeSavingFiles() {
        Algorithm algorithm = new Algorithm();
        algorithm.setCategory(AlgorithmCategoryEnum.detect);
        when(algorithms.selectOne(any())).thenReturn(algorithm);
        MockMultipartFile wrong = new MockMultipartFile("dataYaml", "data.yaml", "text/yaml",
            "nc: 2\nnames: {0: helmet, 2: person}\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        assertThrows(java.io.IOException.class, () -> service.importModel(42L, "object_detection", "helmet", 1, "", pt(), wrong));
        verifyNoInteractions(storage);
        verify(models, never()).save(any());
    }

    @Test void acceptsAllFourTaskTypesWithMatchingAlgorithms() throws Exception {
        when(storage.archive(anyString(), anyString(), any())).thenReturn(true);
        when(models.save(any(AlgorithmModel.class))).thenReturn(true);
        String[] types = {"image_classification", "object_detection", "instance_segmentation", "semantic_segmentation"};
        AlgorithmCategoryEnum[] categories = {AlgorithmCategoryEnum.classify, AlgorithmCategoryEnum.detect,
            AlgorithmCategoryEnum.segment, AlgorithmCategoryEnum.semanticSeg};
        for (int index = 0; index < types.length; index++) {
            Algorithm algorithm = new Algorithm();
            algorithm.setCategory(categories[index]);
            when(algorithms.selectOne(any())).thenReturn(algorithm);
            MockMultipartFile typedYaml = new MockMultipartFile("dataYaml", "data.yaml", "text/yaml",
                ("nc: 2\nnames: [helmet, person]\nannotation_type: " + types[index] + "\n")
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            AlgorithmModel model = service.importModel(42L, types[index], "model-" + index, 1, "", pt(), typedYaml);
            assertEquals(types[index], model.getAnnotationType());
        }
        verify(models, times(4)).save(any(AlgorithmModel.class));
    }

    @Test void rejectsAlgorithmOrYamlTypeMismatch() {
        Algorithm algorithm = new Algorithm();
        algorithm.setCategory(AlgorithmCategoryEnum.detect);
        when(algorithms.selectOne(any())).thenReturn(algorithm);
        assertThrows(RuntimeException.class, () -> service.importModel(42L, "image_classification", "helmet", 1, "", pt(), yaml()));
        MockMultipartFile wrongType = new MockMultipartFile("dataYaml", "data.yaml", "text/yaml",
            "nc: 2\nnames: [helmet, person]\nannotation_type: image_classification\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThrows(java.io.IOException.class, () -> service.importModel(42L, "object_detection", "helmet", 1, "", pt(), wrongType));
        verifyNoInteractions(storage);
    }

    @Test void importsOnePairedModelFromZip() throws Exception {
        Algorithm algorithm = new Algorithm();
        algorithm.setCategory(AlgorithmCategoryEnum.segment);
        when(algorithms.selectOne(any())).thenReturn(algorithm);
        Path temp = setProjectTempDir();
        when(storage.archive(anyString(), anyString(), any())).thenAnswer(call -> {
            Path copy = Files.createTempFile(temp, "archive-source-", ".part");
            try {
                ModelArtifactObjectStore.Source source = call.getArgument(2);
                source.write(copy);
                if ("best.pt".equals(call.getArgument(1))) assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(copy));
                if ("data.yaml".equals(call.getArgument(1))) {
                    assertTrue(new String(Files.readAllBytes(copy), java.nio.charset.StandardCharsets.UTF_8).contains("helmet"));
                }
            } finally { Files.deleteIfExists(copy); }
            return true;
        });
        when(models.save(any(AlgorithmModel.class))).thenReturn(true);

        AlgorithmModel model = service.importArchive(42L, "instance_segmentation", "segment", 1, "",
            archive("bundle/weights/best.pt", "bundle/data.yaml"));

        assertEquals("instance_segmentation", model.getAnnotationType());
        assertTrue(model.getModelPath().endsWith("/weights/best.pt"));
        verify(storage).archive(eq(model.getModelPath()), eq("best.pt"), any());
        verify(storage).archive(eq(ModelClassFileService.storagePath(model.getModelPath())), eq("data.yaml"), any());
    }

    @Test void rejectsZipTraversalBeforeSaving() throws Exception {
        setProjectTempDir();
        assertThrows(RuntimeException.class, () -> service.importArchive(42L, "object_detection", "bad", 1, "",
            archive("../best.pt", "data.yaml")));
        verifyNoInteractions(storage);
    }

    @Test void rejectsZipWithMoreThanOneModel() throws Exception {
        setProjectTempDir();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (String name : new String[]{"first.pt", "second.pt", "data.yaml"}) {
                zip.putNextEntry(new ZipEntry(name));
                zip.write(new byte[]{1, 2, 3});
                zip.closeEntry();
            }
        }
        MockMultipartFile ambiguous = new MockMultipartFile("archive", "models.zip", "application/zip", bytes.toByteArray());
        assertThrows(RuntimeException.class, () -> service.importArchive(42L, "object_detection", "bad", 1, "", ambiguous));
        verifyNoInteractions(storage);
    }

    private Path setProjectTempDir() throws Exception {
        Path root = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("BUSINESS_PROCESSES.md"))) root = root.getParent();
        assertNotNull(root);
        Path temp = root.resolve("codex/model-import-tests");
        Files.createDirectories(temp);
        ReflectionTestUtils.setField(service, "tempDir", temp.toString());
        return temp;
    }

    private static MockMultipartFile archive(String ptName, String yamlName) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry(ptName));
            zip.write(new byte[]{1, 2, 3});
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(yamlName));
            zip.write("nc: 2\nnames: [helmet, person]\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return new MockMultipartFile("archive", "bundle.zip", "application/zip", bytes.toByteArray());
    }

    private static MockMultipartFile pt() {
        return new MockMultipartFile("file", "model.pt", "application/octet-stream", new byte[]{1, 2, 3});
    }

    private static MockMultipartFile yaml() {
        return new MockMultipartFile("dataYaml", "data.yaml", "text/yaml",
            "nc: 2\nnames: [helmet, person]\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
