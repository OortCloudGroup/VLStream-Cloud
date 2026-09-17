package com.ruoyi.vlstream.test.vlstream.service;
import com.ruoyi.vlstream.test.vlstream.config.VlsSshProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsRemoteServersMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.RemoteServers;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
@Tag("dev")
class TrainingDatasetPreflightTest {
    @Test void acceptsOnlySuccessfulStructuredValidationAndQuotesPaths() {
        SSHService ssh=mock(SSHService.class); VlsRemoteServersMapper servers=mock(VlsRemoteServersMapper.class);
        RemoteServers server=new RemoteServers();server.setCondaEnv("yolo");when(servers.selectActiveServer()).thenReturn(server);
        SSHService.SSHExecutionResult result=new SSHService.SSHExecutionResult();result.setSuccess(true);result.setOutput("VLS_DATASET_CHECK={\"valid\":true}");
        when(ssh.executeCommand(anyString(),anyInt(),anyString(),anyString(),anyString())).thenReturn(result);
        TrainingDatasetPreflight service=new TrainingDatasetPreflight(ssh,new VlsSshProperties(),servers);
        service.validate("/data/a'$(bad)/dataset.yaml","/models/base.pt");
        ArgumentCaptor<String> command=ArgumentCaptor.forClass(String.class);verify(ssh).executeCommand(anyString(),anyInt(),anyString(),anyString(),command.capture());
        assertTrue(command.getValue().startsWith("timeout 180 "));assertTrue(command.getValue().contains("'/data/a'\"'\"'$(bad)/dataset.yaml'"));
        result.setOutput("VLS_DATASET_CHECK={\"valid\":false,\"message\":\"重复图片\"}");
        assertTrue(assertThrows(RuntimeException.class,()->service.validate("/d/dataset.yaml","/m.pt")).getMessage().contains("重复图片"));
        result.setOutput("READY");assertThrows(RuntimeException.class,()->service.validate("/d/dataset.yaml","/m.pt"));
    }
}
