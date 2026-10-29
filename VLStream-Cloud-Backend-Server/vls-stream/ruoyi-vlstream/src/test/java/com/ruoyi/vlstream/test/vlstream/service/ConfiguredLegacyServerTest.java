package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.config.VlsSshProperties;
import com.ruoyi.vlstream.test.vlstream.config.VlsTrainingContainerProperties;
import com.ruoyi.vlstream.test.vlstream.compute.ComputeTrustedSelfAssetsProperties;
import com.ruoyi.vlstream.test.vlstream.compute.ComputeTrustedSelfPolicy;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsContainerInstanceMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ContainerInstance;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.RemoteServers;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** No real SSH/GPU. Deletes a fixture legacy row and exercises the actual YAML consumers. */
@Tag("dev")
public class ConfiguredLegacyServerTest {
    private VlsSshProperties yaml() {
        VlsSshProperties yaml=new VlsSshProperties();yaml.setHost("executor.invalid");yaml.setUsername("fixture");
        yaml.setPassword("fixture-only");yaml.setWorkDir("/fixture");yaml.setCondaEnv("fixture");
        yaml.setLegacyServerIds(Collections.singletonList(7L));return yaml;
    }
    private ContainerInstance record(Long serverId) {
        ContainerInstance row=new ContainerInstance();row.setId(10L);row.setTrainingTaskId(20L);row.setCreateUser("101");
        row.setTenantId("tenant-a");row.setInstanceType("training");row.setInstanceName("vls-training-20");
        row.setInstanceStatus("running");row.setServerId(serverId);row.setServerIp("executor.invalid");row.setLogsPath("/fixture/logs/task.log");return row;
    }
    private SSHService.SSHExecutionResult result(String value,boolean success) {
        SSHService.SSHExecutionResult result=new SSHService.SSHExecutionResult();result.setSuccess(success);result.setOutput(value);result.setErrorMsg(success?"":"Auth fail");return result;
    }
    @Test void deletedLegacyRowsNeverSupplyRuntimeOrGetRecreated() {
        JdbcTemplate jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:legacy_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1","sa",""));
        jdbc.execute("CREATE TABLE vls_remote_servers(id BIGINT, server_ip VARCHAR(100))");
        jdbc.update("INSERT INTO vls_remote_servers VALUES(7,'old.invalid')");jdbc.update("DELETE FROM vls_remote_servers");
        VlsSshProperties yaml=yaml();assertEquals(-1L,yaml.requireServer().getId());assertEquals("executor.invalid",yaml.requireServer().getServerIp());
        SSHService ssh=mock(SSHService.class);when(ssh.executeCommand(anyString(),anyInt(),anyString(),anyString(),anyString())).thenReturn(result("VLS_DATASET_CHECK={\"valid\":true}",true));
        new TrainingDatasetPreflight(ssh,yaml).validate("/fixture/dataset.yaml","/fixture/model.pt");
        verify(ssh).executeCommand(eq("executor.invalid"),eq(22),eq("fixture"),eq("fixture-only"),contains("/envs/fixture/bin/python"));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_remote_servers",Integer.class));
        assertTrue(java.util.Arrays.stream(RemoteTrainingService.class.getDeclaredMethods()).noneMatch(method -> method.getName().equals("initDefaultServer")));
    }
    @Test void missingCredentialsAndInvalidAliasesFailBeforeTransport() {
        VlsSshProperties yaml=yaml();yaml.setPassword(null);assertThrows(ServiceException.class,yaml::requireServer);
        yaml.setPassword("fixture-only");yaml.setLegacyServerIds(java.util.Arrays.asList(7L,7L));assertThrows(ServiceException.class,yaml::requireServer);
        yaml.setLegacyServerIds(Collections.singletonList(-173L));assertThrows(ServiceException.class,yaml::requireServer);
    }
    @Test void aliasesAndNewFrozenTargetsCannotSwitchExecutorAfterRowDeletion() {
        VlsSshProperties yaml=yaml();ContainerInstance legacy=record(7L);yaml.requireReference(legacy);
        legacy.setServerId(8L);assertThrows(ServiceException.class,()->yaml.requireReference(legacy));
        ContainerInstance current=record(-1L);current.setEnvConfig("{\"executionFingerprint\":\""+yaml.executionFingerprint()+"\"}");yaml.requireReference(current);
        yaml.setPort(2222);assertThrows(ServiceException.class,()->yaml.requireReference(current));
        yaml.setPort(22);yaml.setWorkDir("/changed");assertThrows(ServiceException.class,()->yaml.requireReference(current));
    }
    @Test void authenticationFailureDoesNotRetryWithADatabaseOrAlternateTarget() {
        VlsSshProperties yaml=yaml();SSHService ssh=mock(SSHService.class);
        when(ssh.executeCommand(anyString(),anyInt(),anyString(),anyString(),anyString())).thenReturn(result("",false));
        RemoteTrainingService remote=new RemoteTrainingService();ReflectionTestUtils.setField(remote,"sshProperties",yaml);ReflectionTestUtils.setField(remote,"sshService",ssh);
        RemoteServers configured=yaml.requireServer();
        SSHService.SSHExecutionResult outcome=ReflectionTestUtils.invokeMethod(remote,"executeWithFallback",configured,"true");assertFalse(outcome.isSuccess());
        verify(ssh,times(1)).executeCommand(eq("executor.invalid"),eq(22),eq("fixture"),eq("fixture-only"),eq("true"));
        configured.setServerIp("old.invalid");assertThrows(ServiceException.class,()->ReflectionTestUtils.invokeMethod(remote,"executeWithFallback",configured,"true"));
        verifyNoMoreInteractions(ssh);
    }
    @Test void legacyHistoryLogsUseExplicitAliasAndPreserveRecordIdentity() {
        VlsSshProperties yaml=yaml();ContainerInstance legacy=record(7L);IVlsContainerInstanceService records=mock(IVlsContainerInstanceService.class);
        when(records.getById(10L)).thenReturn(legacy);SSHService ssh=mock(SSHService.class);
        when(ssh.executeCommand(anyString(),anyInt(),anyString(),anyString(),anyString())).thenReturn(result("saved logs",true));
        GpuTrainingSchedulerService scheduler=new GpuTrainingSchedulerService();ReflectionTestUtils.setField(scheduler,"sshProperties",yaml);ReflectionTestUtils.setField(scheduler,"sshService",ssh);ReflectionTestUtils.setField(scheduler,"containerInstanceService",records);
        assertEquals("saved logs",scheduler.getContainerLogs(10L,100));assertEquals(7L,legacy.getServerId());
        legacy.setServerId(8L);assertThrows(ServiceException.class,()->scheduler.getContainerLogs(10L,100));verify(ssh,times(1)).executeCommand(anyString(),anyInt(),anyString(),anyString(),anyString());
    }
    @Test void unmappedActiveLegacyTaskHoldsQueueWithoutClaimingExitOrDispatchingAnother() {
        String previous=TenantContextHolder.getTenantId();
        try {
            GpuTrainingSchedulerService scheduler=new GpuTrainingSchedulerService();VlsContainerInstanceMapper mapper=mock(VlsContainerInstanceMapper.class);SSHService ssh=mock(SSHService.class);
            when(mapper.selectActiveTrainingForScheduler()).thenReturn(Collections.singletonList(record(8L)));
            ReflectionTestUtils.setField(scheduler,"containerInstanceMapper",mapper);ReflectionTestUtils.setField(scheduler,"containerInstanceService",mock(IVlsContainerInstanceService.class));ReflectionTestUtils.setField(scheduler,"sshProperties",yaml());ReflectionTestUtils.setField(scheduler,"sshService",ssh);
            ReflectionTestUtils.invokeMethod(scheduler,"scheduleSafely");verify(mapper,never()).selectNextQueuedTrainingForScheduler();verifyNoInteractions(ssh);
        } finally {if(previous==null)TenantContextHolder.clear();else TenantContextHolder.setTenantId(previous);}
    }
    @Test void legacyEntryCannotBypassCompanySharedLeaseAfterDatabaseRowsAreGone() {
        VlsSshProperties yaml=yaml();yaml.setHost("192.168.88.173");ComputeTrustedSelfAssetsProperties assets=new ComputeTrustedSelfAssetsProperties();ComputeTrustedSelfAssetsProperties.Asset asset=new ComputeTrustedSelfAssetsProperties.Asset();
        asset.setHost(yaml.getHost());asset.setPort(yaml.getPort());asset.setCompanyPool(true);assets.getAssets().add(asset);
        GpuTrainingSchedulerService scheduler=new GpuTrainingSchedulerService();ReflectionTestUtils.setField(scheduler,"sshProperties",yaml);ReflectionTestUtils.setField(scheduler,"trustedSelfPolicy",new ComputeTrustedSelfPolicy(assets));
        assertThrows(IllegalStateException.class,()->ReflectionTestUtils.invokeMethod(scheduler,"requireServer"));
    }
}
