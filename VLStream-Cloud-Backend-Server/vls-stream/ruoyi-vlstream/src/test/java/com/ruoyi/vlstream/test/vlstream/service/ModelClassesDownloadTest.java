package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmModel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.FileNotFoundException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModelClassesDownloadTest {
    @Test void downloadsClassesBoundToTrainingAndSavedModelPt() throws Exception {
        ModelFileDownloadService service = new ModelFileDownloadService();
        IVlsAlgorithmTrainingService trainings = mock(IVlsAlgorithmTrainingService.class);
        IVlsAlgorithmModelService models = mock(IVlsAlgorithmModelService.class);
        RemoteModelArtifactService artifacts = mock(RemoteModelArtifactService.class);
        ReflectionTestUtils.setField(service, "algorithmTrainingService", trainings);
        ReflectionTestUtils.setField(service, "algorithmModelService", models);
        ReflectionTestUtils.setField(service, "artifactService", artifacts);
        AlgorithmTraining training = new AlgorithmTraining(); training.setModelOutputPath("cloud-training/1/run/weights/best.pt");
        AlgorithmModel model = new AlgorithmModel(); model.setModelPath("cloud-training/1/older/weights/best.pt");
        when(trainings.getById(1L)).thenReturn(training); when(models.getById(2L)).thenReturn(model);
        String run = ModelClassFileService.storagePath(training.getModelOutputPath());
        String saved = ModelClassFileService.storagePath(model.getModelPath());
        when(artifacts.inspect(run)).thenReturn(new RemoteModelArtifactService.ArtifactMetadata("data.yaml", 3, "run-hash"));
        when(artifacts.inspect(saved)).thenReturn(new RemoteModelArtifactService.ArtifactMetadata("data.yaml", 3, "saved-hash"));
        MockHttpServletResponse first = new MockHttpServletResponse(), second = new MockHttpServletResponse();
        service.downloadTrainingModel(1L, "classes", first); service.downloadModel(2L, "classes", second);
        assertEquals("run-hash", first.getHeader("X-Model-SHA256"));
        assertEquals("saved-hash", second.getHeader("X-Model-SHA256"));
        verify(artifacts).stream(eq(run), any()); verify(artifacts).stream(eq(saved), any());
    }

    @Test void missingTrainingCannotResolveAnArtifactPath() {
        ModelFileDownloadService service = new ModelFileDownloadService();
        IVlsAlgorithmTrainingService trainings = mock(IVlsAlgorithmTrainingService.class);
        RemoteModelArtifactService artifacts = mock(RemoteModelArtifactService.class);
        ReflectionTestUtils.setField(service, "algorithmTrainingService", trainings);
        ReflectionTestUtils.setField(service, "artifactService", artifacts);
        assertThrows(FileNotFoundException.class, () -> service.downloadTrainingModel(99L, "classes", new MockHttpServletResponse()));
        verifyNoInteractions(artifacts);
    }
}
