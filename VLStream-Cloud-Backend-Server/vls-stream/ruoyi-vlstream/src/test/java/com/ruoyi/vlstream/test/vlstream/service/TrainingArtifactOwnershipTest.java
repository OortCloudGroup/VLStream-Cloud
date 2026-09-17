package com.ruoyi.vlstream.test.vlstream.service;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import com.ruoyi.vlstream.test.vlstream.config.VlsSshProperties;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
@Tag("dev")
class TrainingArtifactOwnershipTest {
    @Test void checksExactRunAndNeverSearchesLatestDirectory() {
        RemoteTrainingService service=new RemoteTrainingService();
        IVlsAlgorithmTrainingService tasks=mock(IVlsAlgorithmTrainingService.class);SSHService ssh=mock(SSHService.class);
        ReflectionTestUtils.setField(service,"algorithmTrainingService",tasks);ReflectionTestUtils.setField(service,"sshService",ssh);ReflectionTestUtils.setField(service,"sshProperties",new VlsSshProperties());
        AlgorithmTraining task=new AlgorithmTraining();task.setId(1L);String run="/work/runs/vls/task_1_0123456789abcdef0123456789abcdef";
        task.setConfigParams("{\"runDirectory\":\""+run+"\"}");when(tasks.getById(1L)).thenReturn(task);when(tasks.updateAlgorithmTraining(any())).thenReturn(1);
        RemoteServers server=new RemoteServers();server.setWorkDir("/work");server.setServerIp("host");
        SSHService.SSHExecutionResult result=new SSHService.SSHExecutionResult();result.setSuccess(true);result.setOutput("MODEL_READY");
        when(ssh.executeCommand(any(),anyInt(),any(),any(),any())).thenReturn(result);
        assertEquals(run+"/weights/best.pt",service.processTrainingResult(1L,server,"detect","unsafe $(name)"));
        ArgumentCaptor<String> command=ArgumentCaptor.forClass(String.class);verify(ssh).executeCommand(any(),anyInt(),any(),any(),command.capture());assertFalse(command.getValue().contains("find "));assertTrue(command.getValue().contains(run));
        result.setOutput("missing");assertNull(service.processTrainingResult(1L,server,"detect","name"));
    }
    @Test void legacyWithoutRecordedArtifactDoesNotGuess() {
        RemoteTrainingService service=new RemoteTrainingService();IVlsAlgorithmTrainingService tasks=mock(IVlsAlgorithmTrainingService.class);SSHService ssh=mock(SSHService.class);
        ReflectionTestUtils.setField(service,"algorithmTrainingService",tasks);ReflectionTestUtils.setField(service,"sshService",ssh);
        when(tasks.getById(1L)).thenReturn(new AlgorithmTraining());assertNull(service.processTrainingResult(1L,new RemoteServers(),"detect","name"));verifyNoInteractions(ssh);
    }
}
