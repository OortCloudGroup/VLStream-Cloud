package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.vlstream.test.vlstream.compute.billing.BillingPayer;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ExternalBillingProperties;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@Tag("dev")
class ComputeBillingLifecycleTest {
    JdbcTemplate jdbc;ComputeBillingLifecycleService service;ComputePowerProvider provider;ExternalBillingProperties properties;
    ComputePublicInventoryService.Instance claim;ObjectMapper json=new ObjectMapper();BillingPayer payer;
    @BeforeEach void setup() throws Exception {
        DriverManagerDataSource ds=new DriverManagerDataSource("jdbc:h2:mem:billing_lifecycle_"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");jdbc=new JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE vls_compute_billing_session(usage_id VARCHAR(36) PRIMARY KEY,tenant_id VARCHAR(64),opening_user_id VARCHAR(100),payer_user_id VARCHAR(32),instance_ref VARCHAR(200),control_snapshot TEXT,safety_state VARCHAR(32),stop_requested INT,stop_operation_id VARCHAR(36))");
        jdbc.execute("CREATE TABLE vls_compute_node(id BIGINT PRIMARY KEY,tenant_id VARCHAR(64),resource_scope VARCHAR(20),billing_type VARCHAR(20),public_instance_key VARCHAR(100),control_mode VARCHAR(20),provider VARCHAR(20),provider_instance_id VARCHAR(100),provider_region VARCHAR(100),provider_product VARCHAR(40),provider_credential_cipher VARCHAR(100),host VARCHAR(100),port INT,server_group VARCHAR(100),power_state VARCHAR(20),power_operation_id VARCHAR(36),power_operation_action VARCHAR(20),power_operation_started_at TIMESTAMP,power_operation_submitted INT NOT NULL DEFAULT 0,probe_state VARCHAR(20),probe_time TIMESTAMP,lifecycle_revision BIGINT,lifecycle_message VARCHAR(255))");
        jdbc.execute("CREATE TABLE vls_cloud_training_job(id VARCHAR(36) PRIMARY KEY,tenant_id VARCHAR(64),node_id BIGINT,public_claim_id VARCHAR(36),public_owner_subject VARCHAR(100),job_state VARCHAR(20),cancel_requested INT DEFAULT 0,message VARCHAR(255),update_time TIMESTAMP)");
        jdbc.update("INSERT INTO vls_compute_node(id,tenant_id,resource_scope,billing_type,public_instance_key,control_mode,provider,provider_instance_id,provider_region,provider_product,provider_credential_cipher,host,port,power_state,lifecycle_revision) VALUES(1,'tenant-test','PLATFORM','POINTS','public-test','API','aliyun','instance-test','region-test','ECS','TEST_ONLY_CIPHER','test.invalid',22,'RUNNING',0)");
        jdbc.update("INSERT INTO vls_compute_billing_session(usage_id,tenant_id,opening_user_id,payer_user_id,instance_ref,safety_state,stop_requested) VALUES('usage-test','tenant-test','actor-test','123','node-server:1','UNARMED',1)");
        ComputePlatformAuthorization.Context actor=new ComputePlatformAuthorization.Context();actor.setAuthorized(true);actor.setVlsUserId("actor-test");actor.setVlsTenantId("tenant-test");actor.setPlatformUserId("platform-test");
        payer=BillingPayer.fromVerifiedSelf(actor,json.readTree("{\"id\":123}"));
        ComputeNodeService nodes=mock(ComputeNodeService.class);ComputePublicInventoryService inventory=mock(ComputePublicInventoryService.class);
        claim=new ComputePublicInventoryService.Instance();claim.setPublicState("OCCUPIED");claim.setClaimId("claim-test");claim.setOwnerSubject(ModelArtifactObjectStore.hash("trusted-platform-user\nplatform-test"));claim.setGeneration(1L);
        when(nodes.publicInventory()).thenReturn(inventory);when(inventory.requireOwner(any())).thenReturn(claim);when(inventory.read(any(),eq(true))).thenReturn(claim);
        when(nodes.serverRows(any())).thenAnswer(call->jdbc.query("SELECT * FROM vls_compute_node ORDER BY id",new BeanPropertyRowMapper<>(ComputeNode.class)));
        provider=mock(ComputePowerProvider.class);when(provider.supports(any())).thenReturn(true);
        DefaultListableBeanFactory beans=new DefaultListableBeanFactory();beans.registerSingleton("provider",provider);
        properties=new ExternalBillingProperties();properties.setEnabled(true);properties.setSafetyStopEnabled(true);
        service=new ComputeBillingLifecycleService(jdbc,nodes,beans.getBeanProvider(ComputePowerProvider.class),new DataSourceTransactionManager(ds),json,properties);service.arm("usage-test",payer);
    }
    void job(String state,String owner){jdbc.update("INSERT INTO vls_cloud_training_job(id,tenant_id,node_id,public_claim_id,public_owner_subject,job_state) VALUES('job-test','tenant-test',1,'claim-test',?,?)",owner,state);}
    String phase(){return jdbc.queryForObject("SELECT safety_state FROM vls_compute_billing_session",String.class);}
    @Test void ownRunningJobUsesExistingCancellationAndWaitsForTerminalBeforePowerOutbox() {
        job("RUNNING",claim.getOwnerSubject());service.requestSafeStop("usage-test");assertEquals("CANCEL_REQUESTED",phase());
        assertEquals(1,jdbc.queryForObject("SELECT cancel_requested FROM vls_cloud_training_job",Integer.class));assertNull(jdbc.queryForObject("SELECT power_operation_id FROM vls_compute_node",String.class));
        jdbc.update("UPDATE vls_cloud_training_job SET job_state='CANCELLED'");service.requestSafeStop("usage-test");assertEquals("STOP_REQUESTED",phase());
        String operation=jdbc.queryForObject("SELECT power_operation_id FROM vls_compute_node",String.class);assertNotNull(operation);assertEquals("stop",jdbc.queryForObject("SELECT power_operation_action FROM vls_compute_node",String.class));
        service.requestSafeStop("usage-test");assertEquals(operation,jdbc.queryForObject("SELECT power_operation_id FROM vls_compute_node",String.class));
        verify(provider,never()).submit(any(),anyString(),anyString());
    }
    @Test void archivingIsAllowedToFinishBeforePhysicalStop() {
        job("ARCHIVING",claim.getOwnerSubject());service.requestSafeStop("usage-test");assertEquals("CANCEL_REQUESTED",phase());
        assertEquals(0,jdbc.queryForObject("SELECT cancel_requested FROM vls_cloud_training_job",Integer.class));assertNull(jdbc.queryForObject("SELECT power_operation_id FROM vls_compute_node",String.class));
    }
    @Test void foreignOrUnknownJobOwnershipCannotBeCancelledOrPoweredOff() {
        job("RUNNING","other-subject");service.requestSafeStop("usage-test");assertEquals("FOREIGN_JOB_BLOCKED",phase());assertEquals(0,jdbc.queryForObject("SELECT cancel_requested FROM vls_cloud_training_job",Integer.class));assertNull(jdbc.queryForObject("SELECT power_operation_id FROM vls_compute_node",String.class));
    }
    @Test void changedClaimGenerationBlocksProviderAndStopActions() {
        claim.setGeneration(2L);assertThrows(IllegalStateException.class,()->service.observe("usage-test"));assertThrows(IllegalStateException.class,()->service.requestSafeStop("usage-test"));verify(provider,never()).status(any());
    }
    @Test void changedProviderIdentityCannotTargetAnotherInstance() {
        jdbc.update("UPDATE vls_compute_node SET provider_instance_id='other-instance'");assertThrows(IllegalStateException.class,()->service.requestSafeStop("usage-test"));verify(provider,never()).submit(any(),anyString(),anyString());
    }
    @Test void disabledSafetySwitchDoesNotQueueCloudOperation() {
        properties.setSafetyStopEnabled(false);service.requestSafeStop("usage-test");assertNull(jdbc.queryForObject("SELECT power_operation_id FROM vls_compute_node",String.class));
        properties.setEnabled(false);assertThrows(IllegalStateException.class,()->service.observe("usage-test"));verify(provider,never()).status(any());
    }
    @Test void providerObservationRunsOutsideDbTransactionAndDoesNotDeclareStopFromRequest() {
        when(provider.status(any())).thenAnswer(call->{assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());return new ComputePowerProvider.Snapshot("RUNNING","UNKNOWN","TEST_ONLY");});
        service.requestSafeStop("usage-test");assertEquals("RUNNING",service.observe("usage-test").snapshot.power);assertEquals("STOP_REQUESTED",phase());
        verify(provider,never()).submit(any(),anyString(),anyString());
    }
    @Test void unsupportedProviderDoesNotQueueStop() {
        when(provider.supports(any())).thenReturn(false);assertThrows(IllegalStateException.class,()->service.requestSafeStop("usage-test"));assertNull(jdbc.queryForObject("SELECT power_operation_id FROM vls_compute_node",String.class));
    }
    @Test void completeIsolatedBillingPipelineBindsMetersCancelsQueuesStopConfirmsTailAndKeepsReconciliationRecoverable() throws Exception {
        // Replace only this test's own synthetic session table with the actual 048/049 schemas.
        jdbc.execute("DROP TABLE vls_compute_billing_session");
        jdbc.execute("CREATE TABLE vls_compute_usage(id VARCHAR(36) PRIMARY KEY,tenant_id VARCHAR(64),opening_user_id VARCHAR(100),payer_account_ref VARCHAR(100),server_key VARCHAR(200),rate_snapshot TEXT,state VARCHAR(20),billing_start_at TIMESTAMP(3),billing_end_at TIMESTAMP(3))");
        jdbc.execute("CREATE TABLE vls_compute_charge(charge_key VARCHAR(80),usage_id VARCHAR(36))");
        java.nio.file.Path root=java.nio.file.Paths.get(System.getProperty("user.dir")).toAbsolutePath();while(!java.nio.file.Files.exists(root.resolve("BUSINESS_PROCESSES.md")))root=root.getParent();
        for(String migration:new String[]{"V1_2_0_048__compute_billing_slices.sql","V1_2_0_049__compute_billing_coordination.sql"})
            try(java.sql.Connection connection=jdbc.getDataSource().getConnection()){org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,new org.springframework.core.io.FileSystemResource(root.resolve("VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/db/migration/"+migration)));}
        ComputeBillingRate rate=new ComputeBillingRate();rate.setVersion("approved-test-price");rate.setCostSource("VERIFIED_MANUAL");rate.setCurrency("CNY");rate.setCostScope("INSTANCE_COMPUTE");rate.setCostUnit("PER_INSTANCE_HOUR");rate.setCostHourly(java.math.BigDecimal.ONE);
        rate.setValidFrom("2026-01-01T00:00:00Z");rate.setValidUntil("2027-01-01T00:00:00Z");rate.setQuotaUnit("quota");rate.setConversionVersion("conversion-test");rate.setQuotaPerCurrencyUnit(java.math.BigDecimal.ONE);rate.setQuotaScale(0);rate.setQuotaRounding(java.math.RoundingMode.DOWN);rate.setPricePolicy("LOCKED_FOR_RUN");rate.setProviderPriceLockConfirmed(true);rate.setPriceEvidence("TEST_ONLY_EXISTING_APPROVED_PRICE");
        long start=System.currentTimeMillis()-61000;
        jdbc.update("INSERT INTO vls_compute_usage(id,tenant_id,opening_user_id,payer_account_ref,server_key,rate_snapshot,state,billing_start_at) VALUES('usage-test','tenant-test','actor-test','platform-test','node-server:1',?,'RUNNING',?)",json.writeValueAsString(rate),new java.sql.Timestamp(start));
        properties.setMerchantId("merchant-test");
        com.ruoyi.vlstream.test.vlstream.compute.billing.ExternalBillingGateway gateway=new com.ruoyi.vlstream.test.vlstream.compute.billing.ExternalBillingGateway(){
            @Override public Result byKey(String key){return Result.of(Status.NOT_AUTHORIZED);}
            @Override public Result byReference(String ref){return Result.of(Status.NOT_FOUND);}
            @Override public Result balance(String user){return Result.of(Status.NOT_AUTHORIZED);}
            @Override public Result capture(String key,String body){throw new AssertionError("No financial writes permitted in this end-to-end test");}
            @Override public boolean supportsAtomicCapture(){return false;}
        };
        DataSourceTransactionManager manager=new DataSourceTransactionManager(jdbc.getDataSource());
        com.ruoyi.vlstream.test.vlstream.compute.billing.ComputeBillingSettlementService settlements=new com.ruoyi.vlstream.test.vlstream.compute.billing.ComputeBillingSettlementService(jdbc,manager,json,properties,(java.util.function.Supplier<com.ruoyi.vlstream.test.vlstream.compute.billing.ExternalBillingGateway>)()->gateway);
        com.fasterxml.jackson.databind.JsonNode config=json.readTree("{\"settlement_currency\":\"CNY\",\"quota_unit\":\"quota\",\"conversion_version\":\"conversion-test\",\"quota_per_currency_unit\":{\"numerator\":\"500000\",\"denominator\":\"1\"},\"effective_at\":\"2026-01-01T00:00:00Z\",\"valid_until\":\"2030-01-01T00:00:00Z\"}");
        settlements.bindAcceptedUsage("usage-test",payer,config);service.arm("usage-test",payer);
        DefaultListableBeanFactory beans=new DefaultListableBeanFactory();beans.registerSingleton("lifecycle",service);beans.registerSingleton("gateway",gateway);
        com.ruoyi.vlstream.test.vlstream.compute.billing.ComputeBillingReconciliationService reconciliation=new com.ruoyi.vlstream.test.vlstream.compute.billing.ComputeBillingReconciliationService(jdbc,manager,properties,beans.getBeanProvider(com.ruoyi.vlstream.test.vlstream.compute.billing.ExternalBillingGateway.class),json);
        com.ruoyi.vlstream.test.vlstream.compute.billing.ComputeBillingCoordinator coordinator=new com.ruoyi.vlstream.test.vlstream.compute.billing.ComputeBillingCoordinator(jdbc,settlements,properties,beans.getBeanProvider(com.ruoyi.vlstream.test.vlstream.compute.billing.BillingLifecycleGateway.class),reconciliation);
        job("RUNNING",claim.getOwnerSubject());when(provider.status(any())).thenReturn(new ComputePowerProvider.Snapshot("RUNNING","UNKNOWN","TEST_ONLY_RUNNING"));
        coordinator.scan();assertEquals(1,jdbc.queryForObject("SELECT cancel_requested FROM vls_cloud_training_job",Integer.class));assertEquals("CANCEL_REQUESTED",phase());
        assertNull(jdbc.queryForObject("SELECT billing_end_at FROM vls_compute_usage",java.sql.Timestamp.class));
        jdbc.update("UPDATE vls_cloud_training_job SET job_state='CANCELLED'");coordinator.scan();assertEquals("STOP_REQUESTED",phase());assertNotNull(jdbc.queryForObject("SELECT power_operation_id FROM vls_compute_node",String.class));
        when(provider.status(any())).thenReturn(new ComputePowerProvider.Snapshot("STOPPED","UNKNOWN","TEST_ONLY_STOPPED"));coordinator.scan();
        assertEquals("STOP_CONFIRMED",phase());assertNotNull(jdbc.queryForObject("SELECT billing_end_at FROM vls_compute_usage",java.sql.Timestamp.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_billing_slice WHERE is_tail=1",Integer.class));
        assertEquals("RETRY",jdbc.queryForObject("SELECT state FROM vls_compute_billing_reconciliation",String.class));
        assertEquals("OCCUPIED",claim.getPublicState());verify(provider,never()).submit(any(),anyString(),anyString());
        assertThrows(IllegalStateException.class,()->settlements.requirePaidAdmission("usage-test"));
    }
}
