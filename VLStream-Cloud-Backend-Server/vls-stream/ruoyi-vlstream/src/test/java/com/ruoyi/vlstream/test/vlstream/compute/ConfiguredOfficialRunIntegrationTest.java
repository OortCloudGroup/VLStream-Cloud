package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.vlstream.test.vlstream.compute.billing.*;
import com.ruoyi.vlstream.test.vlstream.data.*;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmCategoryEnum;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmTrainingStatusEnum;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import com.ruoyi.vlstream.test.vlstream.service.*;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** No compute-node rows, real application journals/ownership, H2 and explicit supplier/wallet fixtures. */
@Tag("dev")
class ConfiguredOfficialRunIntegrationTest {
    ComputePaidRunIntegrationTest f;
    JdbcTemplate jdbc;
    ConfiguredComputeProperties properties;
    ConfiguredComputeProperties.Resource resource;
    ConfiguredComputeRegistry registry;
    ComputeNodeService nodes;
    ConfiguredPaidPowerService power;
    Instant started,stopped;

    @BeforeEach void setup() throws Exception {
        f=new ComputePaidRunIntegrationTest();f.setup();jdbc=f.jdbc;
        jdbc.update("DELETE FROM vls_compute_node");
        jdbc.execute("CREATE TABLE vls_compute_public_instance(instance_key VARCHAR(64) PRIMARY KEY,provider VARCHAR(20),account_scope VARCHAR(128),provider_instance_id VARCHAR(100),public_state VARCHAR(20) DEFAULT 'FREE',owner_subject VARCHAR(64),claim_id VARCHAR(36),generation BIGINT DEFAULT 0,workspace_dirty BOOLEAN DEFAULT FALSE)");
        jdbc.execute("ALTER TABLE vls_cloud_training_job ADD COLUMN public_instance_key VARCHAR(64)");
        Path root=Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while(root!=null&&!Files.exists(root.resolve("ruoyi-admin/src/main/resources/db/migration/V1_2_0_057__configured_official_power_outbox.sql")))root=root.getParent();
        assertNotNull(root);
        try(java.sql.Connection connection=jdbc.getDataSource().getConnection()){ScriptUtils.executeSqlScript(connection,new FileSystemResource(root.resolve("ruoyi-admin/src/main/resources/db/migration/V1_2_0_057__configured_official_power_outbox.sql").toFile()));}
        properties=new ConfiguredComputeProperties();resource=new ConfiguredComputeProperties.Resource();
        resource.setCode("official-autodl");resource.setProvider("autodl");resource.setNodeId(-6000L);resource.setName("Official fixture");resource.setGpuModel("RTX 6000D");
        resource.setInstanceId("pro-123456abcdef");resource.setAccountScope("official-fixture");resource.setTenantId("official-platform");
        resource.setHost("fixture.invalid");resource.setPort(22);resource.setUsername("root");resource.setPythonPath("/fixture/python");resource.setWorkDir("/fixture/vls-official/localhost");
        byte[] type="ssh-ed25519".getBytes(StandardCharsets.US_ASCII);ByteBuffer host=ByteBuffer.allocate(4+type.length+4+32);host.putInt(type.length).put(type).putInt(32).put(new byte[32]);resource.setHostKey(Base64.getEncoder().encodeToString(host.array()));
        resource.setPrivateKeyFile("C:/fixture/official_ed25519");resource.setPublicKeyFingerprint("SHA256:"+Base64.getEncoder().withoutPadding().encodeToString(new byte[32]));resource.setProviderApiToken("SYNTHETIC_OFFICIAL_API");
        ConfiguredComputeProperties.FixedPrice price=new ConfiguredComputeProperties.FixedPrice();price.setPerInstanceHour(new BigDecimal("13.00"));resource.setFixedPrice(price);properties.getResources().add(resource);
        registry=new ConfiguredComputeRegistry(properties,new ComputeTrustedSelfAssetsProperties(),f.json,jdbc);registry.initialize();
        nodes=new ComputeNodeService(jdbc,mock(ComputeCredentialCipher.class),mock(ComputeSsh.class),f.json);ReflectionTestUtils.setField(nodes,"configured",registry);
        ComputePublicInventoryService inventory=new ComputePublicInventoryService(jdbc);ComputePlatformAuthorization auth=mock(ComputePlatformAuthorization.class);when(auth.requireIdentity()).thenReturn(f.actor);
        ReflectionTestUtils.setField(inventory,"authorization",auth);ReflectionTestUtils.setField(nodes,"publicInventory",inventory);
        ComputeNodeOwnershipService ownership=mock(ComputeNodeOwnershipService.class);when(ownership.identity()).thenReturn(f.actor);ReflectionTestUtils.setField(nodes,"ownership",ownership);
        ReflectionTestUtils.setField(f.runs,"nodes",nodes);ReflectionTestUtils.setField(f.lifecycle,"nodes",nodes);
        f.properties.setMaximumHoldQuota("1000000");((ObjectNode)f.config.path("quota_per_currency_unit")).put("numerator","500000");
        when(f.supplier.status(any())).thenAnswer(call->{ComputePowerProvider.Snapshot value=new ComputePowerProvider.Snapshot(f.wallet.power,"AVAILABLE_HINT","FIXTURE_ONLY");value.chargeType="payg";value.gpuAmount=1;value.providerStartedAt=started;value.providerStoppedAt=stopped;if("STOPPED".equals(value.power))value.stoppedMode="AUTODL_PRO_PHYSICAL_STOPPED";return value;});
        doAnswer(call->{assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());if("start".equals(call.getArgument(1))){started=Instant.now();f.wallet.power="RUNNING";}else{stopped=Instant.now();f.wallet.power="STOPPED";}return null;}).when(f.supplier).submit(any(),anyString(),anyString());
        installPower();
    }
    void installPower(){DefaultListableBeanFactory beans=new DefaultListableBeanFactory();beans.registerSingleton("supplier",f.supplier);power=new ConfiguredPaidPowerService(jdbc,registry,beans.getBeanProvider(ComputePowerProvider.class),f.ledger);ReflectionTestUtils.setField(f.runs,"configuredPower",power);ReflectionTestUtils.setField(f.lifecycle,"configuredPower",power);}
    @AfterEach void cleanup(){f.clear();}
    ComputeNode node(){return nodes.get(-6000L);}
    String accepted(){ComputePaidRunService.View quote=f.runs.quote(-6000L,f.payer,f.config);assertEquals("13.00",quote.getHourlyPrice());f.runs.accept(quote.getRunId(),f.payer);return quote.getRunId();}
    void queued(String id){f.runs.tick(id);f.runs.tick(id);assertEquals("STARTING",f.runs.get(id,f.payer).getState());}
    void running(String id){queued(id);f.runs.tick(id);assertEquals("RUNNING",f.runs.get(id,f.payer).getState());}
    void settle(String id){String usage=f.runs.get(id,f.payer).getUsageId();f.coordinator.tick(usage,f.lifecycle);for(String ref:jdbc.queryForList("SELECT merchant_charge_id FROM vls_compute_billing_slice",String.class))f.settlements.dispatch(ref);}
    void finish(String id){f.runs.stop(id,f.payer);f.runs.tick(id);f.runs.tick(id);settle(id);for(int i=0;i<5;i++)f.runs.tick(id);}

    @Test void yamlOnlyOfficialOrderCapturesAndReleasesWithoutRestoringAnyNodeRow() throws Exception {
        String id=accepted();assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_node",Integer.class));
        running(id);Thread.sleep(1100);settle(id);finish(id);
        assertEquals("COMPLETED",f.runs.get(id,f.payer).getState(),()->"order="+jdbc.queryForList("SELECT state,error_code,power_start_state,power_stop_state FROM vls_compute_paid_run")
            +" session="+jdbc.queryForList("SELECT safety_state,risk_code,metered_until_ms,last_running_ms,physical_stop_observed_ms,stop_requested FROM vls_compute_billing_session")
            +" usage="+jdbc.queryForList("SELECT state,billing_start_at,billing_end_at,start_evidence,stop_evidence FROM vls_compute_usage")
            +" slices="+jdbc.queryForList("SELECT state,error_code,interval_start_ms,interval_end_ms FROM vls_compute_billing_slice")
            +" hold="+f.wallet.remaining());assertTrue(f.wallet.captured>0);assertEquals(0,f.wallet.remaining());
        assertEquals(1,f.wallet.holdPosts);assertEquals(1,f.wallet.releasePosts);verify(f.supplier,times(1)).submit(any(),eq("start"),anyString());verify(f.supplier,times(1)).submit(any(),eq("stop"),anyString());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_node",Integer.class));
        assertEquals("PROVIDER_EVENT",jdbc.queryForObject("SELECT start_evidence FROM vls_compute_usage",String.class));assertEquals("PROVIDER_EVENT",jdbc.queryForObject("SELECT stop_evidence FROM vls_compute_usage",String.class));
        assertTrue(f.wallet.capturePosts>=2);assertEquals(f.wallet.capturePosts,f.wallet.chargesByReference.size());
    }
    @Test void restartAfterAmbiguousStartQueriesOriginalWithoutResendOrEarlyRelease() {
        doThrow(new ComputeProviderException(ComputeProviderException.Kind.UNKNOWN)).when(f.supplier).submit(any(),eq("start"),anyString());
        String id=accepted();queued(id);f.runs.tick(id);String operation=jdbc.queryForObject("SELECT start_operation_id FROM vls_compute_paid_run",String.class);
        registry.initialize();installPower();f.runs.tick(id);f.runs.tick(id);
        assertEquals("STARTING",f.runs.get(id,f.payer).getState());assertTrue(f.wallet.remaining()>0);assertEquals(0,f.wallet.releasePosts);
        verify(f.supplier,times(1)).submit(any(),eq("start"),eq(operation));assertEquals("UNKNOWN",jdbc.queryForObject("SELECT power_start_state FROM vls_compute_paid_run",String.class));
    }
    @Test void crashAfterSubmissionMarkerBeforeHttpIsStillAmbiguousAndCannotSendAgain() {
        String id=accepted();queued(id);jdbc.update("UPDATE vls_compute_paid_run SET power_start_state='SUBMITTED',power_start_attempt_ms=?",System.currentTimeMillis());
        registry.initialize();installPower();f.runs.tick(id);f.runs.tick(id);
        verify(f.supplier,never()).submit(any(),anyString(),anyString());assertTrue(f.wallet.remaining()>0);assertEquals(0,f.wallet.releasePosts);
    }
    @Test void cancellingPreparedOfficialStartReleasesOnlyItsOriginalHold() {
        String id=accepted();queued(id);f.runs.stop(id,f.payer);f.runs.tick(id);f.runs.tick(id);
        assertEquals("CANCELLED",f.runs.get(id,f.payer).getState());assertEquals(0,f.wallet.remaining());verify(f.supplier,never()).submit(any(),anyString(),anyString());
        assertEquals("CANCELLED",jdbc.queryForObject("SELECT power_start_state FROM vls_compute_paid_run",String.class));
    }
    @Test void changingConfiguredPriceDoesNotRepriceTheActiveOrderOrInvalidateItsControlIdentity() {
        String id=accepted();running(id);resource.getFixedPrice().setPerInstanceHour(new BigDecimal("17.00"));registry.initialize();f.runs.tick(id);
        assertEquals("13.00",f.runs.get(id,f.payer).getHourlyPrice());assertEquals("17.00",node().getUserHourlyPrice());assertEquals("RUNNING",f.runs.get(id,f.payer).getState());
    }
    @Test void missingOfficialPriceDoesNotFallBackToGlobalLegacyOrSupplierRates() {
        resource.getFixedPrice().setPerInstanceHour(null);registry.initialize();assertThrows(RuntimeException.class,()->f.runs.quote(-6000L,f.payer,f.config));
        assertEquals(0,f.wallet.holdPosts);assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_public_instance",Integer.class));
    }
    @Test void configurationSecretsAreNotSerializedOrUsedAsDatabaseSupply() throws Exception {
        String publicValue=f.json.writeValueAsString(resource);assertFalse(publicValue.contains("SYNTHETIC_OFFICIAL_API"));assertFalse(publicValue.contains("official_ed25519"));
        assertEquals("13.00",node().getUserHourlyPrice());assertTrue(node().isConfigurationManaged());assertFalse(nodes.canManage(node()));
        assertThrows(RuntimeException.class,()->registry.requireMutable(-6000L));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_node",Integer.class));
    }
    @Test void fixtureLookupAndReplayAreScopedToTheirOriginalBusinessReferenceAndKey() {
        String first="{\"merchant_charge_id\":\"fixture-first\",\"charge_quota\":\"2\"}",second="{\"merchant_charge_id\":\"fixture-tail\",\"charge_quota\":\"3\"}";
        assertEquals(ExternalBillingGateway.Status.SUCCEEDED,f.wallet.capture("fixture-key-1",first).status);
        assertEquals(ExternalBillingGateway.Status.NOT_FOUND,f.wallet.byReference("fixture-tail").status);
        assertEquals(ExternalBillingGateway.Status.SUCCEEDED,f.wallet.capture("fixture-key-2",second).status);
        assertEquals("fixture-first",f.wallet.byReference("fixture-first").charge.path("merchant_charge_id").asText());
        assertNotEquals(f.wallet.byKey("fixture-key-1").charge.path("ledger_entry_id"),f.wallet.byKey("fixture-key-2").charge.path("ledger_entry_id"));
        f.wallet.capture("fixture-key-1",first);assertEquals(5,f.wallet.captured);assertEquals(2,f.wallet.capturePosts);
        assertEquals(ExternalBillingGateway.Status.CONFLICT,f.wallet.capture("fixture-key-1",second).status);assertEquals(5,f.wallet.captured);
    }
    @Test void ambiguousStopSurvivesWorkerRestartWithoutResendingOrReleasingUntilConfirmed() {
        String id=accepted();running(id);
        doThrow(new ComputeProviderException(ComputeProviderException.Kind.UNKNOWN)).when(f.supplier).submit(any(),eq("stop"),anyString());
        f.runs.stop(id,f.payer);f.runs.tick(id);f.runs.tick(id);
        String operation=jdbc.queryForObject("SELECT stop_operation_id FROM vls_compute_billing_session",String.class);
        assertEquals("UNKNOWN",jdbc.queryForObject("SELECT power_stop_state FROM vls_compute_paid_run",String.class));
        registry.initialize();installPower();f.wallet.power="UNKNOWN";f.runs.tick(id);f.runs.tick(id);
        assertEquals("STOPPING",f.runs.get(id,f.payer).getState());assertTrue(f.wallet.remaining()>0);assertEquals(0,f.wallet.releasePosts);
        verify(f.supplier,times(1)).submit(any(),eq("stop"),eq(operation));
        stopped=Instant.now();f.wallet.power="STOPPED";f.runs.tick(id);settle(id);for(int i=0;i<5;i++)f.runs.tick(id);
        assertEquals("COMPLETED",f.runs.get(id,f.payer).getState(),()->"order="+jdbc.queryForList("SELECT state,error_code,power_stop_state FROM vls_compute_paid_run")+" session="+jdbc.queryForList("SELECT safety_state,risk_code,metered_until_ms,last_running_ms,physical_stop_observed_ms FROM vls_compute_billing_session")+" slices="+jdbc.queryForList("SELECT state,error_code,interval_start_ms,interval_end_ms FROM vls_compute_billing_slice"));assertEquals(0,f.wallet.remaining());
        verify(f.supplier,times(1)).submit(any(),eq("stop"),eq(operation));
    }
    @Test void lateHealthProofCannotMakeStoppedOrPendingOfficialResourceTrainable() {
        String id=accepted();running(id);ComputeNode snapshot=node();
        registry.requestedProbe(snapshot);assertEquals("CHECKING",node().getProbeState());
        registry.recordPower(snapshot,new ComputePowerProvider.Snapshot("STOPPED","UNKNOWN","FIXTURE_ONLY"));
        assertFalse(registry.recordProbe(snapshot,"READY","{\"contractVersion\":1,\"ready\":true}",snapshot.getHostKey()));
        assertEquals("STOPPED",node().getPowerState());assertFalse(ComputeAvailability.ready(node(),System.currentTimeMillis()));
        registry.recordPower(snapshot,new ComputePowerProvider.Snapshot("RUNNING","UNKNOWN","FIXTURE_ONLY"));registry.pending(snapshot,"original-stop","stop",true);
        assertFalse(registry.recordProbe(snapshot,"READY","{\"contractVersion\":1,\"ready\":true}",snapshot.getHostKey()));
    }
    @Test void officialTrainingEnqueuesFrozenTargetAndStopWaitsForItsArchiveBeforePowerOff() throws Exception {
        String id=accepted();running(id);
        DefaultListableBeanFactory beans=new DefaultListableBeanFactory();beans.registerSingleton("paid",f.runs);ReflectionTestUtils.setField(nodes,"paidRuns",beans.getBeanProvider(ComputePaidRunService.class));
        ComputeNode snapshot=node();assertTrue(registry.recordProbe(snapshot,"READY","{\"contractVersion\":1,\"ready\":true}",snapshot.getHostKey()));
        for(String column:new String[]{"training_id BIGINT","container_record_id BIGINT","dataset_id BIGINT","version_id BIGINT","annotation_type VARCHAR(40)","snapshot_json TEXT","base_model TEXT","options_json TEXT","run_dir TEXT"})jdbc.execute("ALTER TABLE vls_cloud_training_job ADD COLUMN "+column);
        jdbc.execute("ALTER TABLE vls_cloud_training_job ALTER COLUMN job_state SET DEFAULT 'QUEUED'");
        jdbc.execute("CREATE TABLE vls_algorithm_training(id BIGINT PRIMARY KEY,tenant_id VARCHAR(64),dataset_id BIGINT,train_status VARCHAR(32),progress INT,epoch_total INT,epoch_current INT,config_params TEXT,log_path TEXT,error_message TEXT,onnx_conversion_status VARCHAR(32),om_conversion_status VARCHAR(32),update_time TIMESTAMP)");
        jdbc.update("INSERT INTO vls_algorithm_training(id,tenant_id,train_status) VALUES(1,'tenant-test','pending')");
        AlgorithmTraining task=new AlgorithmTraining();task.setId(1L);task.setTenantId("tenant-test");task.setCreateUser("actor-test");task.setAlgorithmId(2L);task.setTaskName("Official fixture");task.setTrainStatus(AlgorithmTrainingStatusEnum.pending);
        IVlsAlgorithmTrainingService trainings=mock(IVlsAlgorithmTrainingService.class);when(trainings.selectAlgorithmTrainingById(1L)).thenReturn(task);
        VlsAlgorithmMapper algorithms=mock(VlsAlgorithmMapper.class);Algorithm algorithm=new Algorithm();algorithm.setTenantId("tenant-test");algorithm.setCategory(AlgorithmCategoryEnum.detect);when(algorithms.selectById(2L)).thenReturn(algorithm);
        DataManagementService data=mock(DataManagementService.class);AlgorithmAnnotation project=new AlgorithmAnnotation();project.setAnnotationType("object_detection");when(data.project(3L)).thenReturn(project);
        DatasetSnapshot dataset=CloudComputeContractTest.snapshot();when(data.snapshot(3L)).thenReturn(dataset);DatasetVersion version=new DatasetVersion();version.setId(4L);version.setSnapshotJson(f.json.writeValueAsString(dataset));when(data.saveVersion(eq(3L),any())).thenReturn(version);
        IVlsContainerInstanceService containers=mock(IVlsContainerInstanceService.class);when(containers.saveTaskRecord(any())).thenAnswer(call->{ContainerInstance instance=call.getArgument(0);instance.setId(5L);return true;});
        CloudTrainingService training=new CloudTrainingService(jdbc,nodes,data,mock(TrainingPublicationService.class),trainings,containers,algorithms,f.json);
        ComputeRequests.Start request=new ComputeRequests.Start();request.setNodeId(-6000L);request.setDatasetId(3L);request.setModelSource("preset");request.setEpochs(1);
        training.start(1L,request);
        CloudTrainingJob job=jdbc.queryForObject("SELECT * FROM vls_cloud_training_job",new BeanPropertyRowMapper<>(CloudTrainingJob.class));
        assertEquals(-6000L,job.getNodeId());assertEquals("@preset/detect",job.getBaseModel());assertEquals(node().getPublicInstanceKey(),job.getPublicInstanceKey());registry.verifyJob(job,node());
        assertFalse(job.getOptionsJson().contains("SYNTHETIC_OFFICIAL_API"));assertFalse(job.getOptionsJson().contains("official_ed25519"));
        f.runs.stop(id,f.payer);f.runs.tick(id);assertEquals(1,jdbc.queryForObject("SELECT cancel_requested FROM vls_cloud_training_job",Integer.class));verify(f.supplier,never()).submit(any(),eq("stop"),anyString());
        jdbc.update("UPDATE vls_cloud_training_job SET job_state='ARCHIVING'");f.runs.tick(id);verify(f.supplier,never()).submit(any(),eq("stop"),anyString());
        jdbc.update("UPDATE vls_cloud_training_job SET job_state='COMPLETED'");f.runs.tick(id);f.runs.tick(id);settle(id);for(int i=0;i<5;i++)f.runs.tick(id);
        assertEquals("COMPLETED",f.runs.get(id,f.payer).getState());assertEquals(0,f.wallet.remaining());verify(f.supplier,times(1)).submit(any(),eq("stop"),anyString());assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_node",Integer.class));
    }
}
