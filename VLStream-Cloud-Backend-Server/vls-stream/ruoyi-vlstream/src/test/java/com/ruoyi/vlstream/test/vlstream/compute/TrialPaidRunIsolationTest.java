package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.compute.billing.BillingPayer;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ComputeBillingTrialGuard;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ComputeFundingIntentService;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ComputeFundingRecoveryService;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ComputeRefundIntentService;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ExternalBillingGateway;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real local journals and original power outbox. Every remote boundary is a fixture. */
@Tag("dev")
class TrialPaidRunIsolationTest {
    private static final String RUN = "a8c089f1-ad59-43e9-a210-de7b7dc57b10";
    private ConfiguredOfficialRunIntegrationTest official;
    private ComputePaidRunIntegrationTest f;
    private ExternalBillingGateway gateway;
    private ComputeBillingTrialGuard guard;

    @BeforeEach void setup() throws Exception {
        official=new ConfiguredOfficialRunIntegrationTest();official.setup();f=official.f;
        official.resource.setInstanceId("pro-7920a22db144");official.registry.initialize();
        f.properties.setBaseUrl(ComputeBillingTrialGuard.TARGET);f.properties.setMerchantId("vls");
        f.properties.setMaximumRunSeconds(600);f.properties.setMaximumHoldQuota("4000");
        f.properties.getTrial().setEnabled(true);f.properties.getTrial().setRunId(RUN);
        f.properties.getTrial().setMaximumWalletCny("3.47");
        f.properties.getTrial().setExpiresAt(Instant.now().plusSeconds(3600).toString());
        // Explicit synthetic conversion, deliberately different from the historical DEV value.
        ((ObjectNode)f.config.path("quota_per_currency_unit")).put("numerator","1000");
        Path root=Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while(root!=null&&!Files.exists(root.resolve("ruoyi-admin/src/main/resources/db/migration/V1_2_0_054__compute_corrective_refund_intents.sql")))root=root.getParent();
        assertNotNull(root);
        try(java.sql.Connection connection=f.jdbc.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(connection,new FileSystemResource(root.resolve("ruoyi-admin/src/main/resources/db/migration/V1_2_0_054__compute_corrective_refund_intents.sql").toFile()));
        }
        gateway=mock(ExternalBillingGateway.class,invocation->{
            if("config".equals(invocation.getMethod().getName()))return f.config;
            return invocation.getMethod().invoke(f.wallet,invocation.getArguments());
        });
        guard=new ComputeBillingTrialGuard(f.jdbc,f.json,f.properties);
        DataSourceTransactionManager manager=new DataSourceTransactionManager(f.jdbc.getDataSource());
        ComputeFundingIntentService funding=new ComputeFundingIntentService(f.jdbc,manager,f.json,(Supplier<ComputeRefundIntentService>)()->null,f.properties);
        ComputeFundingRecoveryService recovery=new ComputeFundingRecoveryService(funding,(Supplier<ExternalBillingGateway>)()->gateway,f.properties);
        ReflectionTestUtils.setField(f.runs,"funding",funding);ReflectionTestUtils.setField(f.runs,"recovery",recovery);
        ReflectionTestUtils.setField(f.settlements,"gateways",(Supplier<ExternalBillingGateway>)()->gateway);
        installGuard();
        clearInvocations(f.supplier,gateway);
    }
    private void installGuard() {
        DefaultListableBeanFactory beans=new DefaultListableBeanFactory();beans.registerSingleton("wallet",gateway);
        ReflectionTestUtils.setField(f.runs,"trialGuard",guard);
        ReflectionTestUtils.setField(f.runs,"externalGateways",beans.getBeanProvider(ExternalBillingGateway.class));
        ReflectionTestUtils.setField(official.power,"trialGuard",guard);
        ReflectionTestUtils.setField(official.power,"externalGateways",beans.getBeanProvider(ExternalBillingGateway.class));
    }
    @AfterEach void cleanup(){official.cleanup();}
    private ComputePaidRunService.View quote(){return f.runs.quote(-6000L,f.payer,f.config);}
    private String accepted(){String id=quote().getRunId();f.runs.accept(id,f.payer);return id;}
    private int count(String table){return f.jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
    private void changedConversion(){((ObjectNode)f.config).put("conversion_version","changed-fixture-version");}
    private void assertNoPowerSubmission(){verify(f.supplier,never()).submit(any(),anyString(),anyString());}

    @Test void foreignPayerIsRejectedBeforeSupplierReadOrOrderCreation() throws Exception {
        BillingPayer foreign=BillingPayer.fromVerifiedSelf(f.actor,f.json.readTree("{\"id\":14}"));
        assertThrows(RuntimeException.class,()->f.runs.quote(-6000L,foreign,f.config));
        verifyNoInteractions(f.supplier,gateway);assertEquals(0,count("vls_compute_paid_run"));assertEquals(0,f.wallet.holdPosts);
    }
    @Test void differentOfficialInstanceIsRejectedBeforeSupplierRead() {
        official.resource.setInstanceId("pro-111111abcdef");official.registry.initialize();
        assertThrows(RuntimeException.class,this::quote);
        verifyNoInteractions(f.supplier,gateway);assertEquals(0,count("vls_compute_paid_run"));
    }
    @Test void knownDevTargetCannotFallBackToUnrestrictedModeWhenPermitIsDisabled() {
        f.properties.getTrial().setEnabled(false);
        assertThrows(RuntimeException.class,this::quote);
        verifyNoInteractions(f.supplier,gateway);assertEquals(0,count("vls_compute_paid_run"));
    }
    @Test void tickAndScanDoNotLeaseOrRecoverAnyRunOutsideTheFixedTrialSelection() {
        String id=accepted(),other="56ebbec2-3631-4481-a994-a7c0e14ed81c";
        f.jdbc.update("UPDATE vls_compute_paid_run SET run_id=? WHERE run_id=?",other,id);
        ComputeFundingRecoveryService excluded=mock(ComputeFundingRecoveryService.class);
        ReflectionTestUtils.setField(f.runs,"recovery",excluded);
        clearInvocations(f.supplier,gateway);
        f.runs.tick(other);f.runs.scan();
        verifyNoInteractions(excluded,f.supplier,gateway);
        assertEquals("FUNDING",f.runs.get(other,f.payer).getState());
        assertEquals(0L,f.jdbc.queryForObject("SELECT worker_lease_until_ms FROM vls_compute_paid_run WHERE run_id=?",Long.class,other));
        assertNull(f.jdbc.queryForObject("SELECT error_code FROM vls_compute_paid_run WHERE run_id=?",String.class,other));
        assertEquals("PREPARED",f.jdbc.queryForObject("SELECT state FROM vls_compute_funding_intent WHERE operation_kind='HOLD'",String.class));
        assertEquals(0,f.wallet.holdPosts);assertEquals(0,f.wallet.releasePosts);
    }
    @Test void concurrentQuotesAndFreshServiceInstanceRecoverExactlyOnePersistentQuote() throws Exception {
        ExecutorService workers=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        try {
            List<Future<String>> results=new ArrayList<>();
            for(int i=0;i<2;i++)results.add(workers.submit(()->{TenantContextHolder.setTenantId("tenant-test");try{start.await();return quote().getRunId();}finally{TenantContextHolder.clear();}}));
            start.countDown();for(Future<String> result:results)assertEquals(RUN,result.get(10,TimeUnit.SECONDS));
        } finally {workers.shutdownNow();}
        String snapshot=f.jdbc.queryForObject("SELECT quote_snapshot FROM vls_compute_paid_run WHERE run_id=?",String.class,RUN);
        ReflectionTestUtils.setField(f.runs,"trialGuard",new ComputeBillingTrialGuard(f.jdbc,f.json,f.properties));
        changedConversion();clearInvocations(f.supplier);
        assertEquals(RUN,quote().getRunId());assertEquals(snapshot,f.jdbc.queryForObject("SELECT quote_snapshot FROM vls_compute_paid_run WHERE run_id=?",String.class,RUN));
        verifyNoInteractions(f.supplier);assertEquals(1,count("vls_compute_paid_run"));assertEquals(0,count("vls_compute_usage"));
    }
    @Test void terminalOrExpiredFixedQuoteCannotAllocateAnotherRunOrHold() {
        assertEquals(RUN,quote().getRunId());f.runs.stop(RUN,f.payer);
        assertEquals("CANCELLED",quote().getState());f.runs.accept(RUN,f.payer);
        assertEquals(1,count("vls_compute_paid_run"));assertEquals(0,count("vls_compute_funding_intent"));
        assertNoPowerSubmission();assertEquals(0,f.wallet.holdPosts);
    }
    @Test void expiredQuoteAcceptanceFailsUntilTheSameUnacceptedOrderIsRequoted() {
        quote();f.jdbc.update("UPDATE vls_compute_paid_run SET quote_until_ms=? WHERE run_id=?",System.currentTimeMillis()-1,RUN);
        assertThrows(RuntimeException.class,()->f.runs.accept(RUN,f.payer));
        String powerKey=f.jdbc.queryForObject("SELECT start_operation_id FROM vls_compute_paid_run WHERE run_id=?",String.class,RUN);
        assertEquals(RUN,quote().getRunId());
        assertEquals(powerKey,f.jdbc.queryForObject("SELECT start_operation_id FROM vls_compute_paid_run WHERE run_id=?",String.class,RUN));
        assertTrue(f.jdbc.queryForObject("SELECT quote_until_ms FROM vls_compute_paid_run WHERE run_id=?",Long.class,RUN)>System.currentTimeMillis());
        assertEquals(1,count("vls_compute_paid_run"));assertEquals(0,count("vls_compute_usage"));assertEquals(0,count("vls_compute_funding_intent"));
        assertEquals(0,f.wallet.holdPosts);assertNoPowerSubmission();
        f.runs.accept(RUN,f.payer);assertEquals(1,count("vls_compute_usage"));assertEquals(1,count("vls_compute_funding_intent"));
    }
    @Test void expiredUnacceptedQuoteRefreshRechecksCurrentConversionAndCannotBypassTheCap() {
        quote();f.jdbc.update("UPDATE vls_compute_paid_run SET quote_until_ms=? WHERE run_id=?",System.currentTimeMillis()-1,RUN);
        String snapshot=f.jdbc.queryForObject("SELECT quote_snapshot FROM vls_compute_paid_run WHERE run_id=?",String.class,RUN);
        ((ObjectNode)f.config.path("quota_per_currency_unit")).put("numerator","1000000");
        assertThrows(RuntimeException.class,this::quote);
        assertEquals(snapshot,f.jdbc.queryForObject("SELECT quote_snapshot FROM vls_compute_paid_run WHERE run_id=?",String.class,RUN));
        assertEquals(1,count("vls_compute_paid_run"));assertEquals(0,count("vls_compute_usage"));assertEquals(0,f.wallet.holdPosts);
        assertNoPowerSubmission();
    }
    @Test void conversionChangeBetweenQuoteAndAcceptRollsBackUsageClaimAndHold() {
        quote();changedConversion();assertThrows(RuntimeException.class,()->f.runs.accept(RUN,f.payer));
        assertEquals("QUOTED",f.runs.get(RUN,f.payer).getState());assertEquals(0,count("vls_compute_usage"));
        assertEquals(0,count("vls_compute_billing_session"));assertEquals(0,count("vls_compute_funding_intent"));
        assertEquals(0,f.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_public_instance WHERE public_state='OCCUPIED'",Integer.class));
        assertEquals(0,f.wallet.holdPosts);assertNoPowerSubmission();
    }
    @Test void repeatedAcceptanceRecoversSameUsageAndHoldEvenWhenNewConfigCannotBeRead() {
        String id=accepted(),usage=f.runs.get(id,f.payer).getUsageId();
        doThrow(new IllegalStateException("fixture configuration unavailable")).when(gateway).config();
        f.runs.accept(id,f.payer);
        assertEquals(usage,f.runs.get(id,f.payer).getUsageId());assertEquals(1,count("vls_compute_usage"));assertEquals(1,count("vls_compute_funding_intent"));
        assertEquals(0,f.wallet.holdPosts);assertNoPowerSubmission();
    }
    @Test void conversionChangeAfterFundingBlocksStartAndCleansTheOriginalHold() {
        String id=accepted();f.runs.tick(id);assertEquals(1,f.wallet.holdPosts);changedConversion();
        for(int i=0;i<6;i++)f.runs.tick(id);
        assertNoPowerSubmission();assertEquals("CANCELLED",f.runs.get(id,f.payer).getState());
        assertEquals(0,f.wallet.remaining());assertEquals(1,f.wallet.releaseEffects);assertEquals(1,f.wallet.holdPosts);
    }
    @Test void freshConfigIsRecheckedAtActualPowerSendAfterOutboxWasQueued() {
        String id=accepted();official.queued(id);changedConversion();
        for(int i=0;i<6;i++)f.runs.tick(id);
        assertNoPowerSubmission();assertEquals("CANCELLED",f.runs.get(id,f.payer).getState());
        assertEquals(0,f.wallet.remaining());assertEquals(1,f.wallet.releaseEffects);
    }
    @Test void missingGuardAfterRestartFailsClosedForMarkedPreparedStartButAllowsCancellation() {
        String id=accepted();official.queued(id);official.installPower();
        // Directly constructed replacement intentionally lacks dependency injection.
        for(int i=0;i<6;i++)f.runs.tick(id);
        assertNoPowerSubmission();assertEquals("CANCELLED",f.runs.get(id,f.payer).getState());assertEquals(0,f.wallet.remaining());
    }
    @Test void invalidPermitDoesNotPreventOriginalQueriesOrStopSubmission() {
        String id=accepted();official.running(id);String usage=f.runs.get(id,f.payer).getUsageId();
        f.properties.getTrial().setEnabled(false);doThrow(new IllegalStateException("fixture configuration unavailable")).when(gateway).config();
        assertThrows(RuntimeException.class,()->f.runs.requireTraining(official.node()));
        assertEquals(id,f.runs.get(id,f.payer).getRunId());assertTrue(f.runs.list(f.payer).stream().anyMatch(run->id.equals(run.getRunId())));
        f.runs.stop(id,f.payer);for(int i=0;i<3;i++)f.runs.tick(id);
        verify(f.supplier,times(1)).submit(any(),eq("stop"),anyString());
        assertNotNull(f.jdbc.queryForObject("SELECT billing_end_at FROM vls_compute_usage WHERE id=?",java.sql.Timestamp.class,usage));
        assertEquals(1,f.wallet.holdPosts);assertEquals(0,f.wallet.capturePosts);
    }
    @Test void expiredPermitBeforePowerSendStillAllowsOriginalHoldRelease() {
        String id=accepted();official.queued(id);f.properties.getTrial().setExpiresAt(Instant.now().minusSeconds(1).toString());
        f.runs.stop(id,f.payer);for(int i=0;i<6;i++)f.runs.tick(id);
        assertEquals("CANCELLED",f.runs.get(id,f.payer).getState());assertEquals(0,f.wallet.remaining());
        assertEquals(1,f.wallet.releaseEffects);assertNoPowerSubmission();
    }
    @Test void submittedUnknownStartOnlyObservesOriginalOperationAfterPermitExpires() {
        doThrow(new ComputeProviderException(ComputeProviderException.Kind.UNKNOWN)).when(f.supplier).submit(any(),eq("start"),anyString());
        String id=accepted();official.queued(id);f.runs.tick(id);
        String operation=f.jdbc.queryForObject("SELECT start_operation_id FROM vls_compute_paid_run WHERE run_id=?",String.class,id);
        f.properties.getTrial().setExpiresAt(Instant.now().minusSeconds(1).toString());
        clearInvocations(f.supplier);f.runs.tick(id);f.runs.tick(id);
        verify(f.supplier,atLeastOnce()).status(any());verify(f.supplier,never()).submit(any(),anyString(),anyString());
        assertEquals(operation,f.jdbc.queryForObject("SELECT start_operation_id FROM vls_compute_paid_run WHERE run_id=?",String.class,id));
        assertEquals("UNKNOWN",f.jdbc.queryForObject("SELECT power_start_state FROM vls_compute_paid_run WHERE run_id=?",String.class,id));
        assertTrue(f.wallet.remaining()>0);assertEquals(0,f.wallet.releaseEffects);
    }

    /** Shift only the test clock evidence, then use real metering/dispatch and fake supplier stop. */
    private long elapsedFixture(String usage,long seconds) {
        long start=System.currentTimeMillis()-seconds*1000;
        f.jdbc.update("UPDATE vls_compute_usage SET billing_start_at=? WHERE id=?",new java.sql.Timestamp(start),usage);
        f.jdbc.update("UPDATE vls_compute_billing_session SET metered_until_ms=?,last_running_ms=? WHERE usage_id=?",start,start,usage);
        official.started=Instant.ofEpochMilli(start);return start;
    }
    private String budgetOverrun(String id) {
        official.running(id);String usage=f.runs.get(id,f.payer).getUsageId();long start=elapsedFixture(usage,1100);
        f.settlements.recordRunningObservation(usage,Instant.ofEpochMilli(start+100000));
        String allocated=f.settlements.prepareInterval(usage,start+100000);f.settlements.dispatch(allocated);
        assertEquals("UNKNOWN",f.jdbc.queryForObject("SELECT state FROM vls_compute_billing_slice WHERE merchant_charge_id=?",String.class,allocated));
        assertEquals(1,f.wallet.capturePosts);
        f.settlements.dispatch(allocated); // Confirm by the original reference; never another debit.
        assertEquals("SETTLED",f.jdbc.queryForObject("SELECT state FROM vls_compute_billing_slice WHERE merchant_charge_id=?",String.class,allocated));
        assertEquals(1,f.wallet.capturePosts);
        assertTrue(f.wallet.captured>0);assertTrue(f.wallet.remaining()>0);
        long observed=System.currentTimeMillis();f.settlements.recordRunningObservation(usage,Instant.ofEpochMilli(observed));
        String blocked=f.settlements.prepareInterval(usage,observed);
        assertEquals("TRIAL_UNALLOCATED_BUDGET",f.jdbc.queryForObject("SELECT error_code FROM vls_compute_billing_slice WHERE merchant_charge_id=?",String.class,blocked));
        assertEquals(1,f.wallet.capturePosts);return blocked;
    }
    private void confirmedStopAndTail(String id) {
        f.runs.stop(id,f.payer);f.runs.tick(id);f.runs.tick(id);
        String usage=f.runs.get(id,f.payer).getUsageId();f.coordinator.tick(usage,f.lifecycle);
        assertEquals("STOP_CONFIRMED",f.jdbc.queryForObject("SELECT safety_state FROM vls_compute_billing_session WHERE usage_id=?",String.class,usage));
        assertEquals(f.jdbc.queryForObject("SELECT last_running_ms FROM vls_compute_billing_session WHERE usage_id=?",Long.class,usage),
            f.jdbc.queryForObject("SELECT metered_until_ms FROM vls_compute_billing_session WHERE usage_id=?",Long.class,usage));
    }
    @Test void capRejectedUnsentMeteringDoesNotPreventConfirmedStopReleasingOriginalRemainder() {
        String id=accepted(),blocked=budgetOverrun(id);int captured=f.wallet.captured,remaining=f.wallet.remaining();
        String amount=f.jdbc.queryForObject("SELECT amount_text FROM vls_compute_billing_slice WHERE merchant_charge_id=?",String.class,blocked);
        confirmedStopAndTail(id);for(int i=0;i<5;i++)f.runs.tick(id);
        assertEquals("COMPLETED",f.runs.get(id,f.payer).getState());assertEquals(0,f.wallet.remaining());
        assertEquals(captured,f.wallet.captured);assertEquals(remaining,f.wallet.released);assertEquals(1,f.wallet.releaseEffects);
        assertEquals("BLOCKED",f.jdbc.queryForObject("SELECT state FROM vls_compute_billing_slice WHERE merchant_charge_id=?",String.class,blocked));
        assertEquals("TRIAL_UNALLOCATED_BUDGET",f.jdbc.queryForObject("SELECT error_code FROM vls_compute_billing_slice WHERE merchant_charge_id=?",String.class,blocked));
        assertEquals(amount,f.jdbc.queryForObject("SELECT amount_text FROM vls_compute_billing_slice WHERE merchant_charge_id=?",String.class,blocked));
        assertEquals(1,f.wallet.capturePosts);assertEquals(1,f.wallet.holdPosts);
    }
    @Test void unknownOrResidualSendEvidenceStillBlocksReleaseEvenWithUnallocatedErrorText() {
        String id=accepted(),blocked=budgetOverrun(id);confirmedStopAndTail(id);
        String[] unsafe={"state='UNKNOWN'","state='SUBMITTED'","charge_id='uncertain-charge'","ledger_entry_id='uncertain-ledger'","lease_token='uncertain-send'","lease_until_ms=1","error_code=NULL","error_code='TRIAL_OTHER_ERROR'"};
        for(String condition:unsafe) {
            f.jdbc.update("UPDATE vls_compute_billing_slice SET state='BLOCKED',error_code='TRIAL_UNALLOCATED_BUDGET',charge_id=NULL,ledger_entry_id=NULL,lease_token=NULL,lease_until_ms=0 WHERE merchant_charge_id=?",blocked);
            f.jdbc.update("UPDATE vls_compute_billing_slice SET "+condition+" WHERE merchant_charge_id=?",blocked);
            f.runs.tick(id);
            assertNotEquals("COMPLETED",f.runs.get(id,f.payer).getState(),condition);
            assertEquals(0,f.wallet.releasePosts,condition);assertTrue(f.wallet.remaining()>0,condition);
        }
        assertEquals(1,f.wallet.capturePosts);assertEquals(1,f.wallet.holdPosts);
    }
    @Test void expiredPermitTailRemainsUnsentButOriginalHoldCanBeReleasedAfterConfirmedStop() {
        String id=accepted();official.running(id);String usage=f.runs.get(id,f.payer).getUsageId();elapsedFixture(usage,10);
        f.properties.getTrial().setExpiresAt(Instant.now().minusSeconds(1).toString());
        confirmedStopAndTail(id);
        assertTrue(f.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_billing_slice WHERE usage_id=? AND state='BLOCKED' AND error_code='TRIAL_UNALLOCATED_PERMIT'",Integer.class,usage)>0);
        for(int i=0;i<5;i++)f.runs.tick(id);
        assertEquals("COMPLETED",f.runs.get(id,f.payer).getState());assertEquals(0,f.wallet.remaining());
        assertEquals(0,f.wallet.capturePosts);assertEquals(1,f.wallet.releaseEffects);assertEquals(f.wallet.total,f.wallet.released);
        assertTrue(f.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_billing_slice WHERE usage_id=? AND state='BLOCKED' AND error_code='TRIAL_UNALLOCATED_PERMIT' AND charge_id IS NULL AND ledger_entry_id IS NULL",Integer.class,usage)>0);
    }
    @Test void rejectedUnsentHoldAutomaticallyCancelsAtRunDeadlineWithoutAnyWalletPost() {
        String id=accepted();f.properties.getTrial().setEnabled(false);f.runs.tick(id);
        assertEquals("PREPARED",f.jdbc.queryForObject("SELECT state FROM vls_compute_funding_intent WHERE operation_kind='HOLD'",String.class));
        f.jdbc.update("UPDATE vls_compute_paid_run SET hold_until_ms=? WHERE run_id=?",System.currentTimeMillis()+360000L-1,id);
        f.runs.tick(id);f.runs.tick(id);
        assertEquals("CANCELLED",f.runs.get(id,f.payer).getState());
        assertEquals("CANCELLED_BEFORE_SEND",f.jdbc.queryForObject("SELECT error_code FROM vls_compute_funding_intent WHERE operation_kind='HOLD'",String.class));
        assertNull(f.jdbc.queryForObject("SELECT active_instance_key FROM vls_compute_paid_run WHERE run_id=?",String.class,id));
        assertNull(f.jdbc.queryForObject("SELECT active_server_key FROM vls_compute_usage",String.class));
        assertEquals(0,f.wallet.holdPosts);assertEquals(0,f.wallet.releasePosts);assertNoPowerSubmission();
    }
    @Test void unknownHoldAtRunDeadlineIsQueriedAndNeverCancelledAsUnsent() {
        String id=accepted();String usage=f.runs.get(id,f.payer).getUsageId();
        f.jdbc.update("UPDATE vls_compute_funding_intent SET state='UNKNOWN',error_code='DISPATCH_OUTCOME_PENDING' WHERE operation_kind='HOLD'");
        doReturn(ExternalBillingGateway.Result.of(ExternalBillingGateway.Status.UNKNOWN)).when(gateway).holdOperation(anyString(),anyString());
        doReturn(ExternalBillingGateway.Result.of(ExternalBillingGateway.Status.UNKNOWN)).when(gateway).holdByReference(anyString());
        f.properties.getTrial().setEnabled(false);
        f.jdbc.update("UPDATE vls_compute_paid_run SET hold_until_ms=? WHERE run_id=?",System.currentTimeMillis()+360000L-1,id);
        f.runs.tick(id);f.runs.tick(id);
        assertEquals("CANCELLING",f.runs.get(id,f.payer).getState());
        assertEquals("UNKNOWN",f.jdbc.queryForObject("SELECT state FROM vls_compute_funding_intent WHERE operation_kind='HOLD'",String.class));
        assertNotNull(f.jdbc.queryForObject("SELECT active_instance_key FROM vls_compute_paid_run WHERE run_id=?",String.class,id));
        assertNotNull(f.jdbc.queryForObject("SELECT active_server_key FROM vls_compute_usage WHERE id=?",String.class,usage));
        verify(gateway,atLeastOnce()).holdByReference(anyString());
        assertEquals(0,f.wallet.holdPosts);assertEquals(0,f.wallet.releasePosts);assertNoPowerSubmission();
    }
}
