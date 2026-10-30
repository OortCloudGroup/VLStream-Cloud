package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AutoDlRuntimeSshTest {
    ConfiguredComputeRegistry registry=mock(ConfiguredComputeRegistry.class);
    AutoDlProPowerProvider provider=mock(AutoDlProPowerProvider.class);
    JdbcTemplate jdbc=mock(JdbcTemplate.class);
    ConfiguredComputeProperties.Resource resource=new ConfiguredComputeProperties.Resource();
    AutoDlRuntimeSsh runtime=new AutoDlRuntimeSsh(registry,provider,jdbc);
    ComputeNode node=new ComputeNode();ObjectNode snapshot=new ObjectMapper().createObjectNode();
    @BeforeEach void setup(){
        node.setId(-6000L);node.setConfigurationCode("official-autodl");node.setPublicInstanceKey("instance-fixture");node.setTenantId("official");node.setProviderInstanceId("pro-123456abcdef");
        when(registry.official(any())).thenReturn(true);when(registry.definition(any())).thenReturn(resource);
        when(jdbc.queryForList(anyString(),anyString(),anyLong())).thenReturn(Collections.singletonList(Collections.singletonMap("run_id","run1")));
        snapshot.put("proxy_host","connect.fixture.seetacloud.com").put("ssh_port",23456).put("root_password","SYNTHETIC_PASSWORD");
        byte[] type="ssh-ed25519".getBytes(StandardCharsets.US_ASCII);ByteBuffer wire=ByteBuffer.allocate(4+type.length+4+32);wire.putInt(type.length).put(type).putInt(32).put(new byte[32]);resource.setHostKey(Base64.getEncoder().encodeToString(wire.array()));
        when(provider.connectionSnapshot(node)).thenReturn(snapshot);
    }
    @AfterEach void cleanup(){runtime.destroy();}
    @Test void passwordContextDoesNotNeedPrivateKeyOrMutateNode() throws Exception {
        runtime.prepare("run1",node);try(AutoDlRuntimeSsh.Binding b=runtime.require(node)){
            assertEquals("connect.fixture.seetacloud.com",b.endpoint(node).getHost());assertEquals(23456,b.port);assertNull(node.getHost());assertNull(node.getPasswordCipher());
            assertFalse(b.toString().contains("SYNTHETIC_PASSWORD"));
        }
    }
    @Test void refusesMissingTrustAndNeverLearnsFirstSeenKey(){resource.setHostKey(null);assertThrows(java.io.IOException.class,()->runtime.prepare("run1",node));assertThrows(java.io.IOException.class,()->runtime.require(node));}
    @Test void refusesForeignEndpointAndInvalidPort(){snapshot.put("proxy_host","127.0.0.1");assertThrows(java.io.IOException.class,()->runtime.prepare("run1",node));snapshot.put("proxy_host","connect.fixture.seetacloud.com").put("ssh_port",65536);assertThrows(java.io.IOException.class,()->runtime.prepare("run1",node));}
    @Test void cannotChangeEndpointInSameOriginalRun() throws Exception {runtime.prepare("run1",node);snapshot.put("ssh_port",34567);assertThrows(java.io.IOException.class,()->runtime.prepare("run1",node));assertThrows(java.io.IOException.class,()->runtime.require(node));snapshot.put("ssh_port",23456);assertThrows(java.io.IOException.class,()->runtime.prepare("run1",node));}
    @Test void cannotReuseAnotherRunOrTenant() throws Exception {runtime.prepare("run1",node);when(jdbc.queryForList(anyString(),anyString(),anyLong())).thenReturn(Collections.singletonList(Collections.singletonMap("run_id","run2")));assertThrows(java.io.IOException.class,()->runtime.require(node));assertThrows(java.io.IOException.class,()->runtime.prepare("run1",node));}
    @Test void verifiedKnownHostsSupportsDynamicPort() throws Exception {
        Path file=Files.createTempFile("vls-public-host-fixture",".txt");try{
            Files.write(file,Collections.singletonList("[connect.fixture.seetacloud.com]:23456 ssh-ed25519 "+resource.getHostKey()),StandardCharsets.US_ASCII);
            resource.setKnownHostsFile(file.toAbsolutePath().toString());resource.setHostKey(null);runtime.prepare("run1",node);
            try(AutoDlRuntimeSsh.Binding b=runtime.require(node)){assertEquals(23456,b.port);}
        }finally{Files.delete(file);}
    }
    @Test void confirmedStopDestroysRuntimeBinding() throws Exception {runtime.prepare("run1",node);runtime.clear(node);assertThrows(java.io.IOException.class,()->runtime.require(node));}
    @Test void providerAuthenticatedTerminalCanSupplyTrustAfterBoot() throws Exception {
        String key=resource.getHostKey();resource.setHostKey(null);AutoDlJupyterHostProof proof=mock(AutoDlJupyterHostProof.class);when(proof.read(snapshot)).thenReturn(key);ReflectionTestUtils.setField(runtime,"hostProof",proof);
        runtime.prepare("run1",node);try(AutoDlRuntimeSsh.Binding b=runtime.require(node)){assertEquals(key,b.key);assertNull(resource.getHostKey());}
        runtime.prepare("run1",node);verify(proof,times(1)).read(snapshot);
    }
    @Test void connectionFailurePastGraceRequestsOriginalSafeStop() throws Exception {
        AutoDlRuntimeSsh unavailable=mock(AutoDlRuntimeSsh.class);when(unavailable.dynamic(node)).thenReturn(true);doThrow(new java.io.IOException("FIXTURE")).when(unavailable).prepare("run1",node);
        ComputePowerProvider power=mock(ComputePowerProvider.class);when(power.supports(node)).thenReturn(true);when(power.status(node)).thenReturn(new ComputePowerProvider.Snapshot("RUNNING","UNKNOWN","FIXTURE"));
        DefaultListableBeanFactory beans=new DefaultListableBeanFactory();beans.registerSingleton("power",power);
        ConfiguredPaidPowerService service=new ConfiguredPaidPowerService(jdbc,registry,beans.getBeanProvider(ComputePowerProvider.class),mock(ComputeBillingLedgerService.class));ReflectionTestUtils.setField(service,"runtimeSsh",unavailable);
        Map<String,Object> usage=new HashMap<>();usage.put("billing_start_at",new java.sql.Timestamp(System.currentTimeMillis()-31000));when(jdbc.queryForMap(anyString(),any())).thenReturn(usage);
        Map<String,Object> run=new HashMap<>();run.put("run_id","run1");run.put("usage_id","usage1");run.put("state","STARTING");run.put("power_start_state","ACCEPTED");run.put("power_stop_state","PREPARED");service.advance(run,node);
        verify(jdbc).update(contains("risk_code='SSH_PREPARATION_FAILED'"),eq("usage1"));verify(power,never()).submit(any(),anyString(),anyString());
    }
}
