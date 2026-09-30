package com.ruoyi.vlstream.test.vlstream.data;

import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmAnnotation;
import com.ruoyi.vlstream.test.vlstream.service.impl.VlsAlgorithmAnnotationServiceImpl;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class TrainingDatasetMinioDownloadTest {
    @Test void minioExportWorksWithoutAnySshConfigurationAndDeletesOnlyItsTemporaryZip() throws Exception {
        Path root = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (!Files.exists(root.resolve("BUSINESS_PROCESSES.md"))) root = root.getParent();
        Path directory = root.resolve("codex/dataset-minio"); Files.createDirectories(directory);
        Path zip = Files.createTempFile(directory, "download-test-", ".zip"); byte[] bytes = new byte[]{80, 75, 3, 4, 1}; Files.write(zip, bytes);
        VlsAlgorithmAnnotationServiceImpl service = mock(VlsAlgorithmAnnotationServiceImpl.class, CALLS_REAL_METHODS);
        TrainingDatasetArtifactService artifacts = mock(TrainingDatasetArtifactService.class);
        ReflectionTestUtils.setField(service, "trainingDatasetArtifacts", artifacts);
        TrainingDatasetArtifact artifact = new TrainingDatasetArtifact(); artifact.setId("11111111-1111-1111-1111-111111111111"); artifact.setVersionId(2L); artifact.setSha256("fixture-sha");
        AlgorithmAnnotation annotation = new AlgorithmAnnotation(); annotation.setDatasetPath(artifact.getReference());
        doReturn(annotation).when(service).getById(1L); when(artifacts.require(1L, artifact.getReference())).thenReturn(artifact); when(artifacts.download(artifact)).thenReturn(zip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        service.downloadAnnotationDataset(1L, response);
        assertEquals(200, response.getStatus()); assertArrayEquals(bytes, response.getContentAsByteArray());
        assertEquals("fixture-sha", response.getHeader("X-Dataset-SHA256")); assertFalse(Files.exists(zip));
        verify(artifacts).require(1L, artifact.getReference());
    }
}
