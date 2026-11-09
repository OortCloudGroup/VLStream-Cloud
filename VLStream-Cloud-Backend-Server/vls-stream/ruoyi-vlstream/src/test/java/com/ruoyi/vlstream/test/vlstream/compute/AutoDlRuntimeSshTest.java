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

@Tag("dev")
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
    @Test void anotherBackendRestoresItsOwnBindingForTheSameAuthorizedRunningOriginal() throws Exception {
        String key=resource.getHostKey();resource.setHostKey(null);node.setPowerState("RUNNING");
        AutoDlJupyterHostProof proof=mock(AutoDlJupyterHostProof.class);when(proof.read(snapshot)).thenAnswer(call->{
            assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());return key;
        });
        ReflectionTestUtils.setField(runtime,"hostProof",proof);runtime.prepare("run1",node);
        AutoDlRuntimeSsh second=new AutoDlRuntimeSsh(registry,provider,jdbc);ReflectionTestUtils.setField(second,"hostProof",proof);
        try {
            try(AutoDlRuntimeSsh.Binding local=second.require(node)){assertEquals("run1",local.run);assertEquals(key,local.key);assertEquals(23456,local.port);}
            verify(provider,times(2)).connectionSnapshot(node);verify(proof,times(2)).read(snapshot);
            assertNull(resource.getHostKey());assertNull(node.getHostKey());assertNull(node.getHost());assertNull(node.getPasswordCipher());
        } finally {second.destroy();}
    }
    @Test void missingLocalBindingNeverPerformsProviderIoInsideATransaction() {
        node.setPowerState("RUNNING");
        org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            assertThrows(java.io.IOException.class,()->runtime.require(node));
            assertThrows(java.io.IOException.class,()->runtime.prepare("run1",node));
            verifyNoInteractions(provider);
        } finally {org.springframework.transaction.support.TransactionSynchronizationManager.setActualTransactionActive(false);}
    }
    @Test void unavailableOrAmbiguousOriginalCannotRestoreAnyConnection() {
        node.setPowerState("STOPPED");assertThrows(java.io.IOException.class,()->runtime.require(node));
        node.setPowerState("RUNNING");when(jdbc.queryForList(anyString(),anyString(),anyLong())).thenReturn(Collections.emptyList());
        assertThrows(java.io.IOException.class,()->runtime.require(node));
        when(jdbc.queryForList(anyString(),anyString(),anyLong())).thenReturn(Arrays.asList(Collections.singletonMap("run_id","run1"),Collections.singletonMap("run_id","run2")));
        assertThrows(java.io.IOException.class,()->runtime.require(node));verifyNoInteractions(provider);
    }
    @Test void restorationStillRequiresAuthenticatedHostTrustAndDoesNotLearnTheServerKey() {
        node.setPowerState("RUNNING");resource.setHostKey(null);
        assertThrows(java.io.IOException.class,()->runtime.require(node));
        assertNull(resource.getHostKey());assertNull(node.getHostKey());assertNull(node.getHost());
    }
    @Test void snapshotFailurePreservesItsStageAndSafeCauseTypeWithoutMessage() {
        when(provider.connectionSnapshot(node)).thenThrow(new IllegalStateException("SYNTHETIC_PASSWORD https://secret.invalid/token"));
        AutoDlRuntimeSsh.PreparationException failure=assertThrows(AutoDlRuntimeSsh.PreparationException.class,()->runtime.prepare("run1",node));
        assertEquals("SNAPSHOT_REQUEST",AutoDlRuntimeSsh.safeDiagnostic(failure));assertEquals("STATE",AutoDlRuntimeSsh.safeFailureType(failure));
        assertEquals("SNAPSHOT_REQUEST",failure.getMessage());assertSanitized(failure);
    }
    @Test void endpointPasswordTrustAndOriginalRunHaveDistinctFixedStages() {
        snapshot.put("ssh_port","23456");assertStage("SSH_ENDPOINT",()->runtime.prepare("run1",node));
        snapshot.put("ssh_port",23456).put("root_password","");assertStage("SSH_PASSWORD",()->runtime.prepare("run1",node));
        snapshot.put("root_password","SYNTHETIC_PASSWORD");resource.setHostKey(null);assertStage("HOST_TRUST",()->runtime.prepare("run1",node));
        assertStage("ACTIVE_RUN",()->runtime.prepare("another-run",node));
        assertStage("SNAPSHOT_SHAPE",()->AutoDlRuntimeSsh.parse("run1",node,null,resource));
    }
    @Test void endpointConflictRemainsQuarantinedWithItsOriginalStage() throws Exception {
        runtime.prepare("run1",node);snapshot.put("ssh_port",34567);assertStage("ENDPOINT_CONFLICT",()->runtime.prepare("run1",node));
        runtime.clear(node);snapshot.put("ssh_port",23456);assertStage("ENDPOINT_CONFLICT",()->runtime.prepare("run1",node));
    }
    @Test void nestedHostProofAndAsyncWrappersCannotEraseTheOriginalStage() throws Exception {
        resource.setHostKey(null);AutoDlJupyterHostProof proof=mock(AutoDlJupyterHostProof.class);
        AutoDlRuntimeSsh.PreparationException original=AutoDlRuntimeSsh.failure(AutoDlRuntimeSsh.Stage.JUPYTER_WEBSOCKET,
            new javax.websocket.DeploymentException("SYNTHETIC_PASSWORD https://secret.invalid/token"));
        when(proof.read(snapshot)).thenThrow(original);ReflectionTestUtils.setField(runtime,"hostProof",proof);
        assertSame(original,assertThrows(AutoDlRuntimeSsh.PreparationException.class,()->runtime.prepare("run1",node)));
        Throwable wrapped=new java.util.concurrent.ExecutionException(new java.io.IOException("SYNTHETIC_PASSWORD",original));
        assertSame(original,AutoDlRuntimeSsh.failure(AutoDlRuntimeSsh.Stage.JUPYTER_PROOF_WAIT,wrapped));
        assertEquals("JUPYTER_WEBSOCKET",AutoDlRuntimeSsh.safeDiagnostic(wrapped));assertEquals("WEBSOCKET",AutoDlRuntimeSsh.safeFailureType(wrapped));
        assertEquals("JUPYTER_WEBSOCKET",original.getMessage());assertSanitized(original);
    }
    @Test void arbitraryMessagesAndClassNamesNeverBecomeDiagnostics() {
        Throwable[] failures={new java.net.SocketTimeoutException("TOKEN"),new javax.net.ssl.SSLException("PASSWORD"),
            new java.net.UnknownHostException("secret.invalid"),new java.net.ConnectException("secret.invalid:123"),
            new java.util.concurrent.TimeoutException("TOKEN"),new java.io.IOException("PASSWORD"),new RuntimeException("TOKEN")};
        String[] types={"TIMEOUT","TLS","DNS","CONNECT","TIMEOUT","IO","OTHER"};
        for(int i=0;i<failures.length;i++){assertEquals("UNKNOWN",AutoDlRuntimeSsh.safeDiagnostic(failures[i]));assertEquals(types[i],AutoDlRuntimeSsh.safeFailureType(failures[i]));}
        assertEquals("UNKNOWN",AutoDlRuntimeSsh.safeDiagnostic(null));assertEquals("OTHER",AutoDlRuntimeSsh.safeFailureType(null));
    }
    private void assertStage(String expected,org.junit.jupiter.api.function.Executable action) {
        AutoDlRuntimeSsh.PreparationException failure=assertThrows(AutoDlRuntimeSsh.PreparationException.class,action);
        assertEquals(expected,AutoDlRuntimeSsh.safeDiagnostic(failure));assertEquals(expected,failure.getMessage());
    }
    private void assertSanitized(Throwable failure) {
        assertNull(failure.getCause());java.io.StringWriter text=new java.io.StringWriter();failure.printStackTrace(new java.io.PrintWriter(text));
        for(String secret:new String[]{"SYNTHETIC_PASSWORD","SYNTHETIC_TOKEN","secret.invalid","https://","wss://"})assertFalse(text.toString().contains(secret));
    }
    @Test void connectionFailurePastGraceRequestsOriginalSafeStop() throws Exception {
        AutoDlRuntimeSsh unavailable=mock(AutoDlRuntimeSsh.class);when(unavailable.dynamic(node)).thenReturn(true);doThrow(new java.io.IOException("FIXTURE")).when(unavailable).prepare("run1",node);
        ComputePowerProvider power=mock(ComputePowerProvider.class);when(power.supports(node)).thenReturn(true);when(power.status(node)).thenReturn(new ComputePowerProvider.Snapshot("RUNNING","UNKNOWN","FIXTURE"));
        DefaultListableBeanFactory beans=new DefaultListableBeanFactory();beans.registerSingleton("power",power);
        ConfiguredPaidPowerService service=new ConfiguredPaidPowerService(jdbc,registry,beans.getBeanProvider(ComputePowerProvider.class),mock(ComputeBillingLedgerService.class),mock(com.ruoyi.vlstream.test.vlstream.compute.billing.ComputeBillingSettlementService.class));ReflectionTestUtils.setField(service,"runtimeSsh",unavailable);
        Map<String,Object> usage=new HashMap<>();usage.put("billing_start_at",new java.sql.Timestamp(System.currentTimeMillis()-31000));when(jdbc.queryForMap(anyString(),any())).thenReturn(usage);
        when(jdbc.queryForObject(anyString(),org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<Map<String,Object>>>any(),anyString())).thenReturn(usage);
        Map<String,Object> run=new HashMap<>();run.put("run_id","run1");run.put("usage_id","usage1");run.put("state","STARTING");run.put("power_start_state","ACCEPTED");run.put("power_stop_state","PREPARED");service.advance(run,node);
        verify(jdbc).update(contains("risk_code='SSH_PREPARATION_FAILED'"),eq("usage1"));verify(power,never()).submit(any(),anyString(),anyString());
    }
}
