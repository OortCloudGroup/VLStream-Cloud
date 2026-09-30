package com.ruoyi.vlstream.test.vlstream.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.vlstream.test.vlstream.config.VlsSshProperties;
import com.ruoyi.vlstream.test.vlstream.data.*;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmCategoryEnum;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import com.ruoyi.vlstream.test.vlstream.service.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@Tag("dev")
class TrainingDatasetReferenceBindingTest {
    private static final String REF = "vls-dataset://11111111-1111-1111-1111-111111111111";
    @Test void metadataEditCannotReplaceTheFrozenInputOfDefaultTraining() throws Exception {
        VlsAlgorithmTrainingController controller = new VlsAlgorithmTrainingController();
        IVlsAlgorithmTrainingService trainings = mock(IVlsAlgorithmTrainingService.class);
        ReflectionTestUtils.setField(controller, "vlsAlgorithmTrainingService", trainings);
        ReflectionTestUtils.setField(controller, "trainingPublicationService", mock(TrainingPublicationService.class));
        AlgorithmTraining original = new AlgorithmTraining(); original.setId(1L); original.setDatasetId(3L);
        original.setConfigParams("{\"datasetArtifactRef\":\"" + REF + "\",\"runtimeDatasetPath\":\"/frozen/dataset.yaml\"}");
        when(trainings.selectAlgorithmTrainingById(1L)).thenReturn(original);
        AlgorithmTraining edited = new AlgorithmTraining(); edited.setId(1L); edited.setDatasetId(3L); edited.setConfigParams("{\"datasetArtifactRef\":\"other\",\"runtimeDatasetPath\":\"/new/dataset.yaml\",\"epochs\":5}");
        ReflectionTestUtils.invokeMethod(controller, "guardCloudUpdate", edited);
        com.fasterxml.jackson.databind.JsonNode config = new ObjectMapper().readTree(edited.getConfigParams());
        assertEquals(REF, config.path("datasetArtifactRef").asText()); assertEquals("/frozen/dataset.yaml", config.path("runtimeDatasetPath").asText());
        assertEquals(5, config.path("epochs").asInt());
        edited.setDatasetId(4L);
        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(controller, "guardCloudUpdate", edited));
    }
    @Test void defaultQueueReceivesRuntimePathAndFreezesArtifactReference() throws Exception {
        VlsAlgorithmTrainingController controller = new VlsAlgorithmTrainingController();
        IVlsAlgorithmTrainingService trainings = mock(IVlsAlgorithmTrainingService.class);
        IVlsAlgorithmAnnotationService annotations = mock(IVlsAlgorithmAnnotationService.class);
        IVlsAlgorithmService algorithms = mock(IVlsAlgorithmService.class);
        TrainingDatasetRuntimeService runtime = mock(TrainingDatasetRuntimeService.class);
        TrainingDatasetPreflight preflight = mock(TrainingDatasetPreflight.class);
        GpuTrainingSchedulerService scheduler = mock(GpuTrainingSchedulerService.class);
        SSHService ssh = mock(SSHService.class);
        ReflectionTestUtils.setField(controller, "vlsAlgorithmTrainingService", trainings);
        ReflectionTestUtils.setField(controller, "algorithmAnnotationService", annotations);
        ReflectionTestUtils.setField(controller, "algorithmService", algorithms);
        ReflectionTestUtils.setField(controller, "trainingDatasetRuntime", runtime);
        ReflectionTestUtils.setField(controller, "trainingDatasetPreflight", preflight);
        ReflectionTestUtils.setField(controller, "gpuTrainingSchedulerService", scheduler);
        ReflectionTestUtils.setField(controller, "dataManagementService", mock(DataManagementService.class));
        ReflectionTestUtils.setField(controller, "trainingPublicationService", mock(TrainingPublicationService.class));
        ReflectionTestUtils.setField(controller, "sshService", ssh); ReflectionTestUtils.setField(controller, "sshProperties", new VlsSshProperties());
        AlgorithmTraining training = new AlgorithmTraining(); training.setId(1L); training.setAlgorithmId(2L);
        when(trainings.selectAlgorithmTrainingById(1L)).thenReturn(training); when(trainings.updateAlgorithmTraining(any())).thenReturn(1);
        AlgorithmAnnotation annotation = new AlgorithmAnnotation(); annotation.setId(3L); annotation.setAnnotationType("object_detection"); annotation.setDatasetPath(REF);
        when(annotations.getById(3L)).thenReturn(annotation);
        Algorithm algorithm = new Algorithm(); algorithm.setCategory(AlgorithmCategoryEnum.detect); when(algorithms.getById(2L)).thenReturn(algorithm);
        String path = "/data/frozen/dataset.yaml"; when(runtime.prepareDefault(3L, REF)).thenReturn(path);
        SSHService.SSHExecutionResult ready = new SSHService.SSHExecutionResult(); ready.setSuccess(true); ready.setOutput("READY");
        when(ssh.executeCommand(anyString(), anyInt(), anyString(), anyString(), anyString())).thenReturn(ready);
        RemoteTrainingService.StartResult result = new RemoteTrainingService.StartResult(); result.setLogPath("/data/run.log");
        when(scheduler.enqueue(anyString(), anyLong(), anyString(), anyString(), anyInt(), anyInt(), anyInt())).thenReturn(result);
        assertTrue(controller.startTraining(1L, 1, 3L, 1, 320, null).isSuccess());
        verify(scheduler).enqueue("detect", 1L, path, "@preset/detect", 1, 1, 320);
        verify(preflight).validate(path, "@preset/detect", "object_detection", 3L);
        ArgumentCaptor<AlgorithmTraining> updates = ArgumentCaptor.forClass(AlgorithmTraining.class);
        verify(trainings, times(2)).updateAlgorithmTraining(updates.capture());
        com.fasterxml.jackson.databind.JsonNode config = new ObjectMapper().readTree(updates.getAllValues().get(0).getConfigParams());
        assertEquals(REF, config.path("datasetArtifactRef").asText()); assertEquals(path, config.path("runtimeDatasetPath").asText());
    }
    @Test void conversionUsesFrozenInputEvenWhenCurrentDatasetHasChanged() throws Exception {
        VlsAlgorithmTrainingController controller = new VlsAlgorithmTrainingController();
        TrainingDatasetRuntimeService runtime = mock(TrainingDatasetRuntimeService.class);
        IVlsAlgorithmAnnotationService annotations = mock(IVlsAlgorithmAnnotationService.class);
        ReflectionTestUtils.setField(controller, "trainingDatasetRuntime", runtime);
        ReflectionTestUtils.setField(controller, "algorithmAnnotationService", annotations);
        AlgorithmTraining training = new AlgorithmTraining(); training.setDatasetId(3L);
        training.setConfigParams("{\"datasetArtifactRef\":\"" + REF + "\",\"runtimeDatasetPath\":\"/frozen/dataset.yaml\"}");
        when(runtime.restoreDefault(3L, REF, "/frozen/dataset.yaml")).thenReturn("/frozen/dataset.yaml");
        assertEquals("/frozen/dataset.yaml", ReflectionTestUtils.<String>invokeMethod(controller, "resolveDatasetPath", training));
        verifyNoInteractions(annotations);
    }
    @Test void olderTrainingCannotBorrowNewMinioVersionFromCurrentProject() {
        VlsAlgorithmTrainingController controller = new VlsAlgorithmTrainingController();
        IVlsAlgorithmAnnotationService annotations = mock(IVlsAlgorithmAnnotationService.class); RemoteTrainingService remote = mock(RemoteTrainingService.class);
        ReflectionTestUtils.setField(controller, "algorithmAnnotationService", annotations); ReflectionTestUtils.setField(controller, "remoteTrainingService", remote);
        AlgorithmTraining training = new AlgorithmTraining(); training.setDatasetId(3L); training.setModelOutputPath("/old/run/weights/best.pt");
        AlgorithmAnnotation project = new AlgorithmAnnotation(); project.setDatasetPath(REF); when(annotations.getById(3L)).thenReturn(project);
        when(remote.originalDatasetPath(training)).thenReturn("/old/dataset.yaml");
        assertEquals("/old/dataset.yaml", ReflectionTestUtils.<String>invokeMethod(controller, "resolveDatasetPath", training));
        verify(remote).originalDatasetPath(training);
    }
}
