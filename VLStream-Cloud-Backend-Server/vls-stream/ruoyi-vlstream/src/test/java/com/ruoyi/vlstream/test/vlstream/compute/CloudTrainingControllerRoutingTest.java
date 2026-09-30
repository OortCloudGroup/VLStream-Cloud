package com.ruoyi.vlstream.test.vlstream.compute;

import com.ruoyi.vlstream.test.vlstream.controller.VlsAlgorithmTrainingController;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAlgorithmTrainingService;
import com.ruoyi.vlstream.test.vlstream.service.RemoteTrainingService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class CloudTrainingControllerRoutingTest {
    @Test void genericContainerEndpointsCannotRewriteTheCloudQueueLedger() {
        com.ruoyi.vlstream.test.vlstream.service.IVlsContainerInstanceService containers = mock(com.ruoyi.vlstream.test.vlstream.service.IVlsContainerInstanceService.class);
        com.ruoyi.vlstream.test.vlstream.controller.VlsContainerInstanceController controller = new com.ruoyi.vlstream.test.vlstream.controller.VlsContainerInstanceController(containers, mock(com.ruoyi.vlstream.test.vlstream.service.GpuTrainingSchedulerService.class));
        com.ruoyi.vlstream.test.vlstream.pojo.entity.ContainerInstance saved = new com.ruoyi.vlstream.test.vlstream.pojo.entity.ContainerInstance();
        saved.setId(7L); saved.setInstanceType("cloud_training"); when(containers.getById(7L)).thenReturn(saved);
        com.ruoyi.vlstream.test.vlstream.pojo.entity.ContainerInstance update = new com.ruoyi.vlstream.test.vlstream.pojo.entity.ContainerInstance();
        update.setId(7L); update.setInstanceType("training");
        assertThrows(RuntimeException.class, () -> controller.update(update));
        assertThrows(RuntimeException.class, () -> controller.submit(update));
        assertThrows(RuntimeException.class, () -> controller.remove("7"));
        saved.setId(null); assertThrows(RuntimeException.class, () -> controller.save(saved));
        verify(containers, never()).updateById(any()); verify(containers, never()).save(any());
    }
    @Test void cloudLogsStatusStopAndConversionNeverUseDefaultGpu() {
        VlsAlgorithmTrainingController controller = new VlsAlgorithmTrainingController();
        IVlsAlgorithmTrainingService tasks = mock(IVlsAlgorithmTrainingService.class);
        CloudTrainingService cloud = mock(CloudTrainingService.class);
        RemoteTrainingService local = mock(RemoteTrainingService.class);
        ReflectionTestUtils.setField(controller, "vlsAlgorithmTrainingService", tasks);
        ReflectionTestUtils.setField(controller, "cloudTrainingService", cloud);
        ReflectionTestUtils.setField(controller, "remoteTrainingService", local);
        AlgorithmTraining task = new AlgorithmTraining(); task.setId(1L); task.setConfigParams("{\"cloudJobId\":\"test\"}");
        when(tasks.selectAlgorithmTrainingById(1L)).thenReturn(task);
        RemoteTrainingService.LogResult logs = new RemoteTrainingService.LogResult(); logs.setServerManaged(true);
        when(cloud.logs(task)).thenReturn(logs);
        RemoteTrainingService.TrainingProgress progress = new RemoteTrainingService.TrainingProgress();
        when(cloud.progress(task)).thenReturn(progress);
        assertSame(logs, controller.getTrainingLogs(1L, "/caller-supplied-other-host-path", 100).getData());
        assertSame(progress, controller.getTrainingStatus(1L, "/caller-supplied-other-host-path").getData());
        assertEquals("stop_requested", controller.stopTraining(1L).getData());
        verify(cloud).stop(task);
        assertTrue(controller.convertModel(1L).isSuccess());
        verifyNoInteractions(local);
    }
}
