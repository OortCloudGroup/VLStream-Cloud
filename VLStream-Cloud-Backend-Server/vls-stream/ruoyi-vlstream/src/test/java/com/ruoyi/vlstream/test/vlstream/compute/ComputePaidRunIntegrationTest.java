package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.compute.billing.*;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Actual application journals, lifecycle, metering, power outbox and recovery; only the
 * authenticated actor, supplier and remote wallet are fixtures. Never opens a GPU/network. */
@Tag("dev")
class ComputePaidRunIntegrationTest {
    final ObjectMapper json=new ObjectMapper();
    JdbcTemplate jdbc;ExternalBillingProperties properties;ComputePaidRunService runs;ComputeBillingLedgerService ledger;
    ComputeBillingSettlementService settlements;ComputeBillingLifecycleService lifecycle;ComputeBillingCoordinator coordinator;
    ComputePowerWorker power;ComputePowerProvider supplier;BillingPayer payer;Wallet wallet;ComputePlatformAuthorization.Context actor;
    ComputePublicInventoryService.Instance claim;ComputePublicInventoryService inventory;
    JsonNode config;
    @BeforeEach void setup() throws Exception {
        TenantContextHolder.setTenantId("tenant-test");
        DriverManagerDataSource ds=new DriverManagerDataSource("jdbc:h2:mem:paid_"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");
        jdbc=new JdbcTemplate(ds);DataSourceTransactionManager manager=new DataSourceTransactionManager(ds);
        jdbc.execute("CREATE TABLE vls_compute_usage(id VARCHAR(36) PRIMARY KEY,tenant_id VARCHAR(64),server_key VARCHAR(200),active_server_key VARCHAR(200) UNIQUE,opening_user_id VARCHAR(100),payer_account_ref VARCHAR(200),start_operation_id VARCHAR(36) UNIQUE,rate_snapshot TEXT,state VARCHAR(32),billing_start_at TIMESTAMP(3),billing_end_at TIMESTAMP(3),start_observed_at TIMESTAMP(3),start_evidence VARCHAR(32),stop_observed_at TIMESTAMP(3),stop_evidence VARCHAR(32))");
        jdbc.execute("CREATE TABLE vls_compute_charge(charge_key VARCHAR(80) PRIMARY KEY,usage_id VARCHAR(36))");
        jdbc.execute("CREATE TABLE vls_compute_node(id BIGINT PRIMARY KEY,tenant_id VARCHAR(64),resource_scope VARCHAR(20),billing_type VARCHAR(20),public_instance_key VARCHAR(64),control_mode VARCHAR(20),provider VARCHAR(20),provider_instance_id VARCHAR(100),provider_region VARCHAR(100),provider_product VARCHAR(40),provider_credential_cipher VARCHAR(100),host VARCHAR(100),port INT,server_group VARCHAR(100),power_state VARCHAR(20),power_checked_at TIMESTAMP,capacity_state VARCHAR(20),capacity_source VARCHAR(32),capacity_checked_at TIMESTAMP,provider_gpu_amount INT,provider_locked INT,actual_stopped_mode VARCHAR(40),requested_stopped_mode VARCHAR(40),provider_charge_type VARCHAR(40),power_operation_id VARCHAR(36),power_operation_action VARCHAR(20),power_operation_started_at TIMESTAMP,power_operation_submitted INT DEFAULT 0,probe_state VARCHAR(20),probe_time TIMESTAMP,lifecycle_revision BIGINT DEFAULT 0,lifecycle_message VARCHAR(255),enabled INT DEFAULT 1)");
        jdbc.execute("CREATE TABLE vls_cloud_training_job(id VARCHAR(36) PRIMARY KEY,tenant_id VARCHAR(64),node_id BIGINT,public_claim_id VARCHAR(36),public_owner_subject VARCHAR(100),job_state VARCHAR(20),cancel_requested INT DEFAULT 0,message VARCHAR(255),update_time TIMESTAMP)");
        Path root=Paths.get(System.getProperty("user.dir")).toAbsolutePath();while(root!=null&&!Files.exists(root.resolve("BUSINESS_PROCESSES.md")))root=root.getParent();assertNotNull(root);
        for(String name:new String[]{"048__compute_billing_slices","049__compute_billing_coordination","052__compute_funding_intents","053__compute_funding_hold_recovery","056__compute_paid_run_orchestration"})
            try(java.sql.Connection connection=ds.getConnection()){ScriptUtils.executeSqlScript(connection,new FileSystemResource(root.resolve("VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/db/migration/V1_2_0_"+name+".sql")));}
        jdbc.update("INSERT INTO vls_compute_node(id,tenant_id,resource_scope,billing_type,public_instance_key,control_mode,provider,provider_instance_id,provider_region,provider_product,host,port,power_state,power_checked_at,capacity_source,capacity_checked_at,requested_stopped_mode,actual_stopped_mode,provider_charge_type) VALUES(1,'tenant-test','PLATFORM','POINTS','public-test','API','aliyun','instance-test','region-test','ECS','fixture.invalid',22,'STOPPED',NOW(),'PROVIDER_API',NOW(),'StopCharging','StopCharging','PostPaid')");
        actor=new ComputePlatformAuthorization.Context();actor.setAuthorized(true);actor.setVlsUserId("actor-test");actor.setVlsTenantId("tenant-test");actor.setPlatformUserId("platform-test");payer=BillingPayer.fromVerifiedSelf(actor,json.readTree("{\"id\":13}"));
        ComputeNodeService nodes=mock(ComputeNodeService.class);inventory=mock(ComputePublicInventoryService.class);
        claim=new ComputePublicInventoryService.Instance();claim.setPublicState("OCCUPIED");claim.setClaimId("claim-test");claim.setOwnerSubject(ModelArtifactObjectStore.hash("trusted-platform-user\nplatform-test"));claim.setGeneration(1L);
        when(nodes.publicInventory()).thenReturn(inventory);when(inventory.identity()).thenReturn(actor);when(inventory.state(any(),any())).thenReturn("FREE");
        when(inventory.requireOwner(any())).thenReturn(claim);when(inventory.read(any(),eq(true))).thenReturn(claim);when(inventory.claimPaid(any(),any())).thenReturn("TEST_ONLY_CLAIM");
        when(nodes.lockServer(1L)).thenAnswer(call->node());when(nodes.serverRows(any())).thenAnswer(call->Collections.singletonList(node()));
        supplier=mock(ComputePowerProvider.class);when(supplier.supports(any())).thenReturn(true);
        when(supplier.status(any())).thenAnswer(call->{ComputePowerProvider.Snapshot snapshot=new ComputePowerProvider.Snapshot(wallet.power,"AVAILABLE_HINT","TEST_ONLY");snapshot.stoppedMode="StopCharging";snapshot.chargeType="PostPaid";snapshot.gpuAmount=1;return snapshot;});
        doAnswer(call->{wallet.power="start".equals(call.getArgument(1))?"RUNNING":"STOPPED";return null;}).when(supplier).submit(any(),anyString(),anyString());
        DefaultListableBeanFactory beans=new DefaultListableBeanFactory();beans.registerSingleton("supplier",supplier);
        properties=new ExternalBillingProperties();properties.setEnabled(true);properties.setAllowWrites(true);properties.setPaidAdmissionEnabled(true);properties.setSafetyStopEnabled(true);properties.setMerchantId("vls-test");properties.setMaximumRunSeconds(60);properties.setMaximumHoldQuota("1000");properties.setVerifiedProviderStopSeconds(30);properties.setHoldStopLeadSeconds(300);properties.setSettlementIntervalSeconds(1);
        ComputeBillingRate rate=new ComputeBillingRate();rate.setVersion("fixture-verified-rate");rate.setCostSource("VERIFIED_MANUAL");rate.setCurrency("CNY");rate.setCostScope("INSTANCE_COMPUTE");rate.setCostUnit("PER_INSTANCE_HOUR");rate.setCostHourly(new BigDecimal("1800"));rate.setValidFrom("2026-01-01T00:00:00Z");rate.setValidUntil("2030-01-01T00:00:00Z");rate.setQuotaUnit("quota");rate.setConversionVersion("fixture-conversion");rate.setQuotaPerCurrencyUnit(BigDecimal.ONE);rate.setQuotaScale(0);rate.setQuotaRounding(RoundingMode.DOWN);rate.setPricePolicy("LOCKED_FOR_RUN");rate.setProviderPriceLockConfirmed(true);rate.setPriceEvidence("TEST_FIXTURE_ONLY");properties.getRates().put("1",rate);
        config=json.readTree("{\"settlement_currency\":\"CNY\",\"quota_unit\":\"quota\",\"conversion_version\":\"fixture-conversion\",\"quota_per_currency_unit\":{\"numerator\":\"1\",\"denominator\":\"1\"},\"effective_at\":\"2026-01-01T00:00:00Z\",\"valid_until\":\"2030-01-01T00:00:00Z\"}");
        wallet=new Wallet();ledger=new ComputeBillingLedgerService(jdbc,json);
        settlements=new ComputeBillingSettlementService(jdbc,manager,json,properties,(java.util.function.Supplier<ExternalBillingGateway>)()->wallet);
        ComputeFundingIntentService journal=new ComputeFundingIntentService(jdbc,manager,json);
        ComputeFundingRecoveryService recovery=new ComputeFundingRecoveryService(journal,(java.util.function.Supplier<ExternalBillingGateway>)()->wallet,properties);
        lifecycle=new ComputeBillingLifecycleService(jdbc,nodes,beans.getBeanProvider(ComputePowerProvider.class),manager,json,properties);
        runs=new ComputePaidRunService(jdbc,manager,json,nodes,ledger,settlements,lifecycle,journal,recovery,properties,new ComputeBillingPolicy(),beans.getBeanProvider(ComputePowerProvider.class));
        coordinator=new ComputeBillingCoordinator(jdbc,settlements,properties,beans.getBeanProvider(BillingLifecycleGateway.class),mock(ComputeBillingReconciliationService.class));
        power=new ComputePowerWorker(jdbc);ReflectionTestUtils.setField(power,"transactions",manager);ReflectionTestUtils.setField(power,"billing",ledger);
    }
    @AfterEach void clear(){TenantContextHolder.clear();}
    ComputeNode node(){return jdbc.queryForObject("SELECT * FROM vls_compute_node WHERE id=1",new BeanPropertyRowMapper<>(ComputeNode.class));}
    String accepted(){ComputePaidRunService.View quoted=runs.quote(1L,payer,config);assertEquals("QUOTED",quoted.getState());assertEquals(0,wallet.holdPosts);runs.accept(quoted.getRunId(),payer);return quoted.getRunId();}
    void start(String id){runs.tick(id);assertEquals("FUNDING",runs.get(id,payer).getState());assertNull(node().getPowerOperationId());runs.tick(id);assertEquals("STARTING",runs.get(id,payer).getState());power.poll(node(),supplier);runs.tick(id);assertEquals("RUNNING",runs.get(id,payer).getState());}

    @Test void confirmedFundingPowerMeterTrainingStopArchiveSettlementAndReleaseRecoverTheSameRun() throws Exception {
        String id=accepted();runs.accept(id,payer);assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_usage",Integer.class));
        start(id);assertTrue(runs.trainingAvailable(node()));verify(supplier,times(1)).submit(any(),eq("start"),anyString());
        String usage=runs.get(id,payer).getUsageId();Thread.sleep(1100);coordinator.tick(usage,lifecycle);
        for(String reference:jdbc.queryForList("SELECT merchant_charge_id FROM vls_compute_billing_slice",String.class))settlements.dispatch(reference);
        assertTrue(wallet.captured>0);assertEquals(1,wallet.capturePosts);
        jdbc.update("INSERT INTO vls_cloud_training_job(id,tenant_id,node_id,public_claim_id,public_owner_subject,job_state) VALUES('job-test','tenant-test',1,'claim-test',?,'RUNNING')",claim.getOwnerSubject());
        runs.stop(id,payer);runs.tick(id);assertEquals(1,jdbc.queryForObject("SELECT cancel_requested FROM vls_cloud_training_job",Integer.class));assertNull(node().getPowerOperationId());
        jdbc.update("UPDATE vls_cloud_training_job SET job_state='ARCHIVING'");runs.tick(id);assertNull(node().getPowerOperationId());
        jdbc.update("UPDATE vls_cloud_training_job SET job_state='COMPLETED'");runs.tick(id);assertEquals("stop",node().getPowerOperationAction());
        power.poll(node(),supplier);coordinator.tick(usage,lifecycle);
        for(String reference:jdbc.queryForList("SELECT merchant_charge_id FROM vls_compute_billing_slice",String.class))settlements.dispatch(reference);
        runs.tick(id);runs.tick(id);assertEquals("COMPLETED",runs.get(id,payer).getState());assertEquals(0,wallet.remaining());assertEquals(1,wallet.releasePosts);
        runs.stop(id,payer);runs.tick(id);assertEquals(1,wallet.capturePosts);assertEquals(1,wallet.releasePosts);assertNull(jdbc.queryForObject("SELECT active_instance_key FROM vls_compute_paid_run WHERE run_id=?",String.class,id));
    }
    @Test void cancellationBeforeFirstSendDoesNotFreezeOrStart() {
        String id=accepted();runs.stop(id,payer);runs.tick(id);assertEquals("CANCELLED",runs.get(id,payer).getState());assertEquals(0,wallet.holdPosts);verify(supplier,never()).submit(any(),anyString(),anyString());
    }
    @Test void lostHoldResponseQueriesOriginalResourceAndNeverCreatesAReplacement() {
        wallet.loseHoldReply=true;String id=accepted();start(id);assertEquals(1,wallet.holdPosts);assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_funding_intent WHERE operation_kind='HOLD'",Integer.class));
    }
    @Test void knownStartFailureReleasesTheOriginalHoldWithoutCapture() {
        String id=accepted();runs.tick(id);runs.tick(id);power.recordFailure(node(),new ComputeProviderException(ComputeProviderException.Kind.NO_STOCK));
        runs.tick(id);runs.tick(id);assertEquals("CANCELLED",runs.get(id,payer).getState());assertEquals(0,wallet.remaining());assertEquals(0,wallet.capturePosts);assertEquals(1,wallet.releasePosts);
    }
    @Test void unknownReleaseCannotDeclareCompletionOrSendAnotherRelease() {
        String id=accepted();runs.tick(id);runs.tick(id);power.recordFailure(node(),new ComputeProviderException(ComputeProviderException.Kind.NO_STOCK));wallet.loseReleaseWithoutCommit=true;
        runs.tick(id);runs.tick(id);assertEquals("RELEASING",runs.get(id,payer).getState());assertTrue(wallet.remaining()>0);assertEquals(1,wallet.releasePosts);
    }
    @Test void merchantIdentityChangeInvalidatesQuoteAndBlocksNewTrainingOnTheExistingRun() {
        String id=runs.quote(1L,payer,config).getRunId();properties.setMerchantId("other-fixture-merchant");
        assertThrows(RuntimeException.class,()->runs.accept(id,payer));assertEquals(0,wallet.holdPosts);
        properties.setMerchantId("vls-test");runs.accept(id,payer);start(id);
        properties.setMerchantId("other-fixture-merchant");assertThrows(RuntimeException.class,()->runs.requireTraining(node()));
        assertEquals(0,wallet.capturePosts);
    }
    @Test void disabledUnpricedAndForeignPayerCannotTriggerTransport() throws Exception {
        properties.setPaidAdmissionEnabled(false);assertThrows(RuntimeException.class,()->runs.quote(1L,payer,config));properties.setPaidAdmissionEnabled(true);
        properties.getRates().clear();assertThrows(RuntimeException.class,()->runs.quote(1L,payer,config));assertEquals(0,wallet.holdPosts);
        setup();String id=accepted();BillingPayer other=BillingPayer.fromVerifiedSelf(actor,json.readTree("{\"id\":14}"));
        assertThrows(RuntimeException.class,()->runs.get(id,other));assertThrows(RuntimeException.class,()->runs.stop(id,other));assertThrows(RuntimeException.class,()->runs.accept(id,other));assertEquals(0,wallet.holdPosts);
    }
    private final class Wallet implements ExternalBillingGateway {
        String power="STOPPED",reference,expiry;int total,captured,released,version=1,holdPosts,capturePosts,releasePosts;boolean loseHoldReply,loseReleaseWithoutCommit;JsonNode charge;
        int remaining(){return total-captured-released;}
        ObjectNode detail(){ObjectNode d=json.createObjectNode();d.put("hold_id","1");d.put("merchant_hold_id",reference);d.put("user_id","13");d.put("hold_quota",Integer.toString(total));d.put("captured_quota",Integer.toString(captured));d.put("released_quota",Integer.toString(released));d.put("remaining_quota",Integer.toString(remaining()));d.put("hold_status",remaining()==0?"RELEASED":captured==0?"ACTIVE":"PARTIALLY_CAPTURED");d.put("balance_version",Integer.toString(version));d.put("expires_at",expiry);d.put("created_at","1");d.put("updated_at","1");return d;}
        public Result createHold(String key,String body){assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());try{JsonNode b=json.readTree(body);holdPosts++;total=b.path("hold_quota").asInt();reference=b.path("merchant_hold_id").asText();expiry=b.path("expires_at").asText();if(loseHoldReply)return Result.of(Status.UNKNOWN);ObjectNode receipt=json.createObjectNode();receipt.put("hold_id","1");receipt.put("held_quota",Integer.toString(total));receipt.put("available_quota",Integer.toString(10000-total));return new Result(Status.SUCCEEDED,"",receipt);}catch(Exception invalid){throw new RuntimeException(invalid);}}
        public Result holdByReference(String ref){return new Result(Status.SUCCEEDED,"",detail());}
        public Result holdById(String id){return new Result(Status.SUCCEEDED,"",detail());}
        public Result capture(String key,String body){try{capturePosts++;ObjectNode d=(ObjectNode)json.readTree(body);captured+=d.path("charge_quota").asInt();version++;d.put("status","SUCCEEDED");d.put("charge_id","1");d.put("ledger_entry_id","1");d.put("charged_quota",d.path("charge_quota").asText());charge=d;return new Result(Status.SUCCEEDED,"",d);}catch(Exception invalid){throw new RuntimeException(invalid);}}
        public Result releaseHold(String id,String key,String body){releasePosts++;if(loseReleaseWithoutCommit)return Result.of(Status.UNKNOWN);int amount=remaining();released+=amount;version++;ObjectNode d=detail();d.put("released_quota",Integer.toString(amount));d.put("held_quota","0");return new Result(Status.SUCCEEDED,"",d);}
        public Result byKey(String key){return Result.of(charge==null?Status.NOT_FOUND:Status.SUCCEEDED);}
        public Result byReference(String ref){return charge==null?Result.of(Status.NOT_FOUND):new Result(Status.SUCCEEDED,"",charge);}
        public Result balance(String id){throw new AssertionError("No implicit balance/auto-grant in workflow scheduler");}
        public boolean supportsAtomicCapture(){return true;}
    }
}
