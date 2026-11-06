package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.vlstream.test.vlstream.compute.billing.*;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Actual journals in isolated H2, synthetic provider and gateway. No live funds or credentials. */
@Tag("dev")
class ComputeTrialRefundTest {
    final ObjectMapper json=new ObjectMapper();
    final String run=UUID.randomUUID().toString(),usage=UUID.randomUUID().toString();
    final String charge="vls:"+usage+":0";
    JdbcTemplate jdbc;
    DataSourceTransactionManager manager;
    ExternalBillingProperties properties;
    ExternalBillingGateway gateway;
    ComputeRefundIntentService refunds;
    ComputeFundingIntentService funding;
    ComputeTrialRefundService service;
    ComputeTrialRefundService.Request request;
    ComputeFundingIntentService.Intent hold;
    DefaultListableBeanFactory beans;
    ObjectNode original,proof,snapshot;
    String tenant="tenant",actor="admin-1";
    boolean admin=true,stopped=true,unknownRefund,unknownRelease;
    int captured=1000,released=2000,version=2;
    AtomicInteger refundPosts=new AtomicInteger(),releasePosts=new AtomicInteger(),powerReads=new AtomicInteger();

    @BeforeEach void setup() throws Exception {
        DriverManagerDataSource ds=new DriverManagerDataSource("jdbc:h2:mem:trial_refund_"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");
        jdbc=new JdbcTemplate(ds);manager=new DataSourceTransactionManager(ds);
        jdbc.execute("CREATE TABLE vls_compute_usage(id VARCHAR(36) PRIMARY KEY,tenant_id VARCHAR(64),server_key VARCHAR(200),active_server_key VARCHAR(200),start_operation_id VARCHAR(36),billing_start_at TIMESTAMP,billing_end_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE vls_cloud_training_job(id VARCHAR(36),node_id BIGINT,public_instance_key VARCHAR(64),job_state VARCHAR(32))");
        Path root=Paths.get(System.getProperty("user.dir")).toAbsolutePath();while(root!=null&&!Files.exists(root.resolve("BUSINESS_PROCESSES.md")))root=root.getParent();assertNotNull(root);
        for(String migration:new String[]{"048__compute_billing_slices","049__compute_billing_coordination","052__compute_funding_intents","053__compute_funding_hold_recovery","054__compute_corrective_refund_intents","056__compute_paid_run_orchestration"})
            try(java.sql.Connection c=ds.getConnection()){ScriptUtils.executeSqlScript(c,new FileSystemResource(root.resolve("VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/db/migration/V1_2_0_"+migration+".sql")));}
        properties=new ExternalBillingProperties();properties.setMode(ExternalBillingProperties.Mode.PAID);
        properties.setBaseUrl(ComputeBillingTrialGuard.TARGET);properties.setMerchantId("vls");properties.getTrial().setRunId(run);
        String expiry=Instant.now().plusSeconds(3600).toString();
        properties.setMaximumRunSeconds(600);properties.getTrial().setEnabled(true);properties.getTrial().setMaximumWalletCny("3.47");properties.getTrial().setExpiresAt(expiry);
        snapshot=json.createObjectNode().put("price_snapshot_id","price").put("approved_hourly_price","13.00").put("quota_unit","quota")
            .put("conversion_version","v1").put("numerator","1000").put("denominator","1")
            .put("effective_at",Instant.now().minusSeconds(7200).toString()).put("valid_until",expiry);
        snapshot.putObject("accepted_rate").put("currency","CNY").put("pricePolicy","VLS_FIXED");
        snapshot.putObject("trial_guard").put("version",1).put("run_id",run).put("payer_user_id","13").put("instance_id","pro-7920a22db144")
            .put("instance_key","instance").put("node_id","-6000").put("merchant_id","vls").put("base_url",ComputeBillingTrialGuard.TARGET)
            .put("maximum_cny","3.47").put("maximum_quota","3470").put("reservation_quota","3000").put("expires_at",expiry);
        jdbc.update("INSERT INTO vls_compute_usage(id,tenant_id,server_key,active_server_key,start_operation_id,billing_start_at,billing_end_at) VALUES(?,'tenant','node-server:-6000',NULL,'start',NOW(),NOW())",usage);
        jdbc.update("INSERT INTO vls_compute_billing_session(usage_id,tenant_id,opening_user_id,merchant_id,payer_user_id,instance_ref,quota_unit,accepted_snapshot,metered_until_ms,last_running_ms,safety_state) VALUES(?,'tenant','opener','vls','13','node-server:-6000','quota',?,2,2,'STOP_CONFIRMED')",usage,snapshot.toString());
        jdbc.update("INSERT INTO vls_compute_paid_run(run_id,node_id,instance_key,tenant_id,opening_user_id,platform_subject,payer_user_id,merchant_id,quote_snapshot,reserve_quota,quote_until_ms,hold_until_ms,maximum_run_seconds,usage_id,start_operation_id,state) VALUES(?,-6000,'instance','tenant','opener','verified-original','13','vls',?,'3000',1,2,600,?,'start','COMPLETED')",run,snapshot.toString(),usage);
        gateway=mock(ExternalBillingGateway.class);
        refunds=new ComputeRefundIntentService(jdbc,manager,json,properties,(java.util.function.Supplier<ExternalBillingGateway>)()->gateway);
        funding=new ComputeFundingIntentService(jdbc,manager,json,()->refunds,properties);
        hold=funding.prepareInitialHold(usage,"3000",String.valueOf(System.currentTimeMillis()-100000),"DEV_TRIAL");
        jdbc.update("UPDATE vls_compute_paid_run SET hold_intent_id=? WHERE run_id=?",hold.id,run);
        funding.recordHoldDetail(hold.id,hold.requestHash,detail());hold=funding.recover(hold.id);
        ComputeFundingIntentService.Intent finalRelease=funding.prepareFinalRelease(hold.id,"2000");
        funding.recordReceipt(finalRelease.id,finalRelease.requestHash,json.createObjectNode().put("hold_id","101").put("released_quota","2000")
            .put("held_quota","0").put("remaining_quota","0").put("hold_status","RELEASED"));
        long observedAt=System.currentTimeMillis();funding.recordHoldDetail(hold.id,hold.requestHash,detail(),observedAt);
        assertEquals("OPERATION_CONFIRMED",funding.confirmOperation(finalRelease.id,observedAt).state);
        jdbc.update("UPDATE vls_compute_paid_run SET release_intent_id=? WHERE run_id=?",finalRelease.id,run);
        original=json.createObjectNode().put("merchant_id","vls").put("merchant_charge_id",charge).put("user_id",13)
            .put("usage_session_id",usage).put("hold_id",101).put("charge_quota","1000").put("business_type","GPU_COMPUTE");
        jdbc.update("INSERT INTO vls_compute_billing_slice(merchant_charge_id,usage_id,slice_sequence,interval_start_ms,interval_end_ms,idempotency_key,request_json,request_hash,amount_text,remainder_numerator,remainder_denominator,state,charge_id,ledger_entry_id) VALUES(?,?,0,1,2,'capture-key',?,?,'1000','0','1','SETTLED','201','301')",charge,usage,original.toString(),ModelArtifactObjectStore.hash(original.toString()));
        proof=original.deepCopy().put("charged_quota","1000").put("charge_id","201").put("ledger_entry_id","301").put("status","SUCCEEDED").put("operation_type","CHARGE").put("idempotency_key","capture-key");
        when(gateway.byReference(charge)).thenAnswer(i->{ObjectNode detail=proof.deepCopy();detail.remove(Arrays.asList("merchant_id","hold_id","operation_type","idempotency_key"));return ok(detail);});
        when(gateway.byKey(anyString())).thenAnswer(i->"capture-key".equals(i.getArgument(0))?ok(proof):ExternalBillingGateway.Result.of(ExternalBillingGateway.Status.UNKNOWN));
        when(gateway.holdById("101")).thenAnswer(i->{assertFalse(TransactionSynchronizationManager.isActualTransactionActive());return ok(detail());});
        when(gateway.balance("13")).thenAnswer(i->ok(json.createObjectNode().put("user_id","13").put("balance_quota",String.valueOf(10000-captured))
            .put("held_quota",String.valueOf(remaining())).put("available_quota",String.valueOf(10000-captured-remaining())).put("balance_version",version)));
        when(gateway.refund(anyString(),anyString(),anyString())).thenAnswer(i->{
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());refundPosts.incrementAndGet();
            if(unknownRefund)return ExternalBillingGateway.Result.of(ExternalBillingGateway.Status.UNKNOWN);
            String amount=json.readTree((String)i.getArgument(2)).path("refund_quota").asText();captured-=Integer.parseInt(amount);version++;
            return ok(json.createObjectNode().put("operation_id",String.valueOf(500+refundPosts.get())).put("refund_id",String.valueOf(600+refundPosts.get())).put("ledger_entry_id",String.valueOf(700+refundPosts.get()))
                .put("status","SUCCEEDED").put("refunded_quota",amount).put("available_quota",String.valueOf(10000-captured-remaining())).put("balance_version",version));
        });
        when(gateway.releaseHold(eq("101"),anyString(),anyString())).thenAnswer(i->{
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());releasePosts.incrementAndGet();
            if(unknownRelease)return ExternalBillingGateway.Result.of(ExternalBillingGateway.Status.UNKNOWN);
            int amount=json.readTree((String)i.getArgument(2)).path("release_quota").asInt();released+=amount;version++;
            return ok(json.createObjectNode().put("hold_id","101").put("released_quota",String.valueOf(amount))
                .put("held_quota","0").put("remaining_quota","0").put("hold_status","RELEASED"));
        });
        beans=new DefaultListableBeanFactory();beans.registerSingleton("gateway",gateway);service=service();
        request=new ComputeTrialRefundService.Request();request.merchantChargeId=charge;request.originalRequestHash=ModelArtifactObjectStore.hash(original.toString());request.expectedHoldId="101";request.expectedRefundQuota="1000";
    }
    private ComputeTrialRefundService service() {
        return new ComputeTrialRefundService(jdbc,manager,json,properties,refunds,funding,beans.getBeanProvider(ExternalBillingGateway.class),null,beans.getBeanProvider(ComputePowerProvider.class)) {
            @Override protected String requireAdministrator(){if(!admin)throw new IllegalStateException("VLS administrator required");return actor;}
            @Override protected String currentTenant(){return tenant;}
            @Override protected void verifyShutdown(Map<String,Object> run){powerReads.incrementAndGet();assertFalse(TransactionSynchronizationManager.isActualTransactionActive());if(!stopped)throw new IllegalStateException("Physical shutdown not confirmed");}
        };
    }
    private int remaining(){return 3000-captured-released;}
    private JsonNode detail() {
        return json.createObjectNode().put("hold_id","101").put("merchant_hold_id","vls-hold:"+usage+":initial").put("user_id","13")
            .put("hold_quota","3000").put("captured_quota",String.valueOf(captured)).put("released_quota",String.valueOf(released))
            .put("remaining_quota",String.valueOf(remaining())).put("hold_status",remaining()==0?"RELEASED":"EXPIRED")
            .put("expires_at",hold==null?"1":readExpiry()).put("created_at",1).put("updated_at",2).put("balance_version",version);
    }
    private String readExpiry(){try{return json.readTree(hold.body).path("expires_at").asText();}catch(Exception e){throw new RuntimeException(e);}}
    private ExternalBillingGateway.Result ok(JsonNode value){return new ExternalBillingGateway.Result(ExternalBillingGateway.Status.SUCCEEDED,"",value);}
    private ComputeTrialRefundService.Result finish() throws Exception {
        ComputeTrialRefundService.Result result=service.advance(run,request);
        if("HOLD_REQUERY_REQUIRED".equals(result.outcome)){Thread.sleep(3);result=service.advance(run,request);}
        return result;
    }
    private int count(String table){return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
    private void noRefund(){assertEquals(0,refundPosts.get());assertEquals(0,releasePosts.get());assertEquals(0,count("vls_compute_refund_intent"));}

    @Test void fullCorrectionUsesOriginalChargeAndReleasesRestoredHoldWithoutChangingOrder() throws Exception {
        Map<String,Object> before=jdbc.queryForMap("SELECT * FROM vls_compute_paid_run WHERE run_id=?",run);
        Map<String,Object> captureBefore=jdbc.queryForMap("SELECT * FROM vls_compute_billing_slice WHERE merchant_charge_id=?",charge);
        ComputeTrialRefundService.View view=service.review(run);assertEquals("1000",view.grossQuota);assertEquals("13",view.payerUserId);
        ComputeTrialRefundService.Result result=finish();assertEquals("REFUND_RELEASE_CONFIRMED",result.outcome);
        assertEquals(1,refundPosts.get());assertEquals(1,releasePosts.get());assertEquals(0,remaining());assertEquals(0,captured);
        assertEquals("trial:"+run+":admin:admin-1",refunds.recover(result.refundIntentId).correctionReference);
        assertEquals(before,jdbc.queryForMap("SELECT * FROM vls_compute_paid_run WHERE run_id=?",run));
        assertEquals(captureBefore,jdbc.queryForMap("SELECT * FROM vls_compute_billing_slice WHERE merchant_charge_id=?",charge));
        verify(gateway,never()).createHold(anyString(),anyString());verify(gateway,never()).capture(anyString(),anyString());
    }
    @Test void duplicateAndDifferentAdministratorRecoverSameIntentAndNeverRefundTwice() throws Exception {
        ComputeTrialRefundService.Result first=finish();actor="admin-2";service=service();
        ComputeTrialRefundService.Result again=service.advance(run,request);
        assertEquals(first.refundIntentId,again.refundIntentId);assertEquals(first.releaseIntentId,again.releaseIntentId);
        assertEquals(1,refundPosts.get());assertEquals(1,releasePosts.get());assertEquals(1,count("vls_compute_refund_intent"));
        assertTrue(refunds.recover(again.refundIntentId).correctionReference.endsWith("admin-1"));
    }
    @Test void unknownRefundOnlyQueriesOriginalKeyNeverResubmitsOrReleases() {
        unknownRefund=true;ComputeTrialRefundService.Result first=service.advance(run,request);
        assertEquals("UNKNOWN",first.refundState);service.advance(run,request);
        assertEquals(1,refundPosts.get());assertEquals(0,releasePosts.get());assertEquals(1,count("vls_compute_refund_intent"));
        verify(gateway,atLeastOnce()).byKey(refunds.recover(first.refundIntentId).key);
    }
    @Test void unknownReleaseDoesNotReplayEvenWhenAggregateFundsAreGone() throws Exception {
        unknownRelease=true;ComputeTrialRefundService.Result first=finish();assertEquals("UNKNOWN",first.releaseState);
        released=3000;version++;ComputeTrialRefundService.Result again=service.advance(run,request);
        assertEquals(first.releaseIntentId,again.releaseIntentId);assertEquals("RELEASE_RECONCILIATION_REQUIRED",again.outcome);
        assertEquals(1,refundPosts.get());assertEquals(1,releasePosts.get());
    }
    @Test void stalePreparedReadCannotReplayReleaseClaimedByConcurrentAdministrator() throws Exception {
        ComputeRefundIntentService.Intent refund=refunds.prepare(charge,"trial:"+run+":admin:admin-1","1000","DEV_TRIAL_ACCEPTANCE_REFUND");
        refunds.recordReceipt(refund.id,refund.requestHash,json.createObjectNode().put("operation_id","501").put("refund_id","601").put("ledger_entry_id","701")
            .put("status","SUCCEEDED").put("refunded_quota","1000").put("available_quota","9000").put("balance_version",3));
        captured=0;version=3;Thread.sleep(3);
        funding.recordHoldDetail(hold.id,hold.requestHash,detail(),System.currentTimeMillis());
        ComputeFundingIntentService.Intent release=funding.prepareRefundRelease(hold.id,refund.reference);
        ComputeFundingIntentService actual=funding,paused=spy(funding);
        CountDownLatch claimed=new CountDownLatch(1),proceed=new CountDownLatch(1);
        doAnswer(i->{ComputeFundingIntentService.Intent observed=(ComputeFundingIntentService.Intent)i.callRealMethod();
            if("PREPARED".equals(observed.state)){claimed.countDown();assertTrue(proceed.await(10,TimeUnit.SECONDS));}
            return observed;}).when(paused).recover(release.id);
        funding=paused;ComputeTrialRefundService first=service();funding=actual;ComputeTrialRefundService second=service();
        ExecutorService executor=Executors.newSingleThreadExecutor();unknownRelease=true;
        try {
            Future<ComputeTrialRefundService.Result> pending=executor.submit(()->first.advance(run,request));
            assertTrue(claimed.await(10,TimeUnit.SECONDS));
            assertEquals("UNKNOWN",second.advance(run,request).releaseState);
            proceed.countDown();assertEquals("UNKNOWN",pending.get(10,TimeUnit.SECONDS).releaseState);
            assertEquals(1,releasePosts.get());assertEquals(0,refundPosts.get());
        } finally {proceed.countDown();executor.shutdownNow();}
    }
    @Test void expiredOriginalTrialPermitStillAllowsOnlyItsCleanup() throws Exception {
        snapshot.with("trial_guard").put("expires_at",Instant.now().minusSeconds(60).toString());
        jdbc.update("UPDATE vls_compute_billing_session SET accepted_snapshot=?",snapshot.toString());
        jdbc.update("UPDATE vls_compute_paid_run SET quote_snapshot=?",snapshot.toString());
        properties.getTrial().setEnabled(false);
        assertEquals("REFUND_RELEASE_CONFIRMED",finish().outcome);
    }
    @Test void aggregateGrossBudgetIsNotReducedByRefunds() throws Exception {
        finish();
        ObjectNode extra=original.deepCopy().put("merchant_charge_id",charge+"extra").put("charge_quota","2500");
        jdbc.update("INSERT INTO vls_compute_billing_slice(merchant_charge_id,usage_id,slice_sequence,interval_start_ms,interval_end_ms,idempotency_key,request_json,request_hash,amount_text,remainder_numerator,remainder_denominator,state,charge_id,ledger_entry_id) VALUES(?,?,1,2,3,'extra-key',?,?,'2500','0','1','SETTLED','202','302')",charge+"extra",usage,extra.toString(),ModelArtifactObjectStore.hash(extra.toString()));
        assertThrows(IllegalStateException.class,()->service.review(run));assertEquals(1,refundPosts.get());
    }
    private ComputeTrialRefundService.Request splitCaptures() {
        original.put("charge_quota","400");proof.put("charge_quota","400").put("charged_quota","400");
        jdbc.update("UPDATE vls_compute_billing_slice SET amount_text='400',request_json=?,request_hash=? WHERE merchant_charge_id=?",original.toString(),ModelArtifactObjectStore.hash(original.toString()),charge);
        request.expectedRefundQuota="400";request.originalRequestHash=ModelArtifactObjectStore.hash(original.toString());
        ObjectNode body=original.deepCopy().put("merchant_charge_id",charge+"second").put("charge_quota","600");
        jdbc.update("INSERT INTO vls_compute_billing_slice(merchant_charge_id,usage_id,slice_sequence,interval_start_ms,interval_end_ms,idempotency_key,request_json,request_hash,amount_text,remainder_numerator,remainder_denominator,state,charge_id,ledger_entry_id) VALUES(?,?,1,2,3,'second-key',?,?,'600','0','1','SETTLED','202','302')",charge+"second",usage,body.toString(),ModelArtifactObjectStore.hash(body.toString()));
        ObjectNode secondProof=body.deepCopy().put("charged_quota","600").put("charge_id","202").put("ledger_entry_id","302")
            .put("status","SUCCEEDED").put("operation_type","CHARGE").put("idempotency_key","second-key");
        when(gateway.byReference(charge+"second")).thenReturn(ok(secondProof));when(gateway.byKey("second-key")).thenReturn(ok(secondProof));
        ComputeTrialRefundService.Request second=new ComputeTrialRefundService.Request();second.merchantChargeId=charge+"second";
        second.originalRequestHash=ModelArtifactObjectStore.hash(body.toString());second.expectedHoldId="101";second.expectedRefundQuota="600";return second;
    }
    @Test void multipleCapturesRefundAndReleaseIndividuallyWithoutReusingFinalRelease() throws Exception {
        ComputeTrialRefundService.Request second=splitCaptures();ComputeTrialRefundService.Result first=finish();
        assertEquals("REFUND_RELEASE_CONFIRMED",first.outcome);assertEquals(600,captured);assertEquals(0,remaining());
        request=second;ComputeTrialRefundService.Result last=finish();assertEquals("REFUND_RELEASE_CONFIRMED",last.outcome);
        assertNotEquals(first.releaseIntentId,last.releaseIntentId);assertEquals(0,captured);assertEquals(0,remaining());
        assertEquals(2,refundPosts.get());assertEquals(2,releasePosts.get());assertEquals(4,count("vls_compute_funding_intent"));
    }
    @Test void nextCaptureCannotRefundWhilePriorRefundReleaseIsUnresolved() throws Exception {
        ComputeTrialRefundService.Request second=splitCaptures();unknownRelease=true;assertEquals("UNKNOWN",finish().releaseState);
        request=second;assertThrows(IllegalStateException.class,()->service.advance(run,request));
        assertEquals(1,refundPosts.get());assertEquals(1,releasePosts.get());assertEquals(1,count("vls_compute_refund_intent"));
    }
    @Test void unconfirmedOriginalFinalReleaseBlocksAllNewCorrections() {
        jdbc.update("UPDATE vls_compute_funding_intent SET state='UNKNOWN' WHERE operation_kind='RELEASE'");
        assertThrows(IllegalStateException.class,()->service.advance(run,request));noRefund();
    }
    @ParameterizedTest @ValueSource(strings={"OFF","READ_ONLY"})
    void disabledWriteModeRejectsBeforeAnyJournalOrTransport(String mode) {
        properties.setMode(ExternalBillingProperties.Mode.valueOf(mode));assertThrows(IllegalStateException.class,()->service.advance(run,request));noRefund();assertEquals(0,powerReads.get());
    }
    @Test void normalNonTrialConsumptionIsNeverEligible() {
        jdbc.update("UPDATE vls_compute_billing_session SET accepted_snapshot='{}'");assertThrows(IllegalStateException.class,()->service.advance(run,request));noRefund();
    }
    @Test void unauthorizedAndCrossTenantAreRejectedBeforeExternalReads() {
        admin=false;assertThrows(IllegalStateException.class,()->service.advance(run,request));admin=true;tenant="other";
        assertThrows(IllegalStateException.class,()->service.advance(run,request));tenant=null;assertThrows(IllegalStateException.class,()->service.review(run));noRefund();assertEquals(0,powerReads.get());
    }
    @ParameterizedTest @ValueSource(strings={"STARTING","RUNNING","STOPPING","RELEASING","CANCELLED"})
    void nonCompletedOrdersIncludingOldFailedTrialsAreRejected(String state) {
        jdbc.update("UPDATE vls_compute_paid_run SET state=?",state);assertThrows(IllegalStateException.class,()->service.advance(run,request));noRefund();
    }
    @Test void physicallyRunningOrUnknownProviderCannotRefund() {
        stopped=false;assertThrows(IllegalStateException.class,()->service.advance(run,request));noRefund();
    }
    @ParameterizedTest @ValueSource(strings={"RUNNING","ARCHIVE_PENDING","QUEUED"})
    void activeTrainingAndArchiveBlockRefund(String state) {
        jdbc.update("INSERT INTO vls_cloud_training_job VALUES('job',-6000,'instance',?)",state);assertThrows(IllegalStateException.class,()->service.advance(run,request));noRefund();
    }
    @Test void anotherActiveUsageBlocksRefund() {
        jdbc.update("INSERT INTO vls_compute_usage(id,tenant_id,server_key,active_server_key) VALUES('other','tenant','node-server:-6000','node-server:-6000')");assertThrows(IllegalStateException.class,()->service.advance(run,request));noRefund();
    }
    @ParameterizedTest @ValueSource(strings={"UNKNOWN","SUBMITTED","PROCESSING","BLOCKED"})
    void unsettledCapturesNeverEnterCorrection(String state) {
        jdbc.update("UPDATE vls_compute_billing_slice SET state=?",state);assertThrows(IllegalStateException.class,()->service.advance(run,request));noRefund();
    }
    @Test void runningCaptureLeaseBlocksRefund() {
        jdbc.update("UPDATE vls_compute_billing_slice SET lease_token='worker',lease_until_ms=?",System.currentTimeMillis()+60000);assertThrows(IllegalStateException.class,()->service.advance(run,request));noRefund();
    }
    @Test void changedReviewedAmountHashHoldAndForeignChargeAreRejected() {
        request.expectedRefundQuota="1001";assertThrows(IllegalStateException.class,()->service.advance(run,request));request.expectedRefundQuota="1000";
        request.originalRequestHash="changed";assertThrows(IllegalStateException.class,()->service.advance(run,request));request.originalRequestHash=ModelArtifactObjectStore.hash(original.toString());
        request.expectedHoldId="102";assertThrows(IllegalStateException.class,()->service.advance(run,request));request.expectedHoldId="101";
        request.merchantChargeId="foreign";assertThrows(IllegalStateException.class,()->service.advance(run,request));noRefund();
    }
    @Test void authoritativeOwnershipOrAmountMismatchBlocksBeforePreparation() {
        proof.put("user_id","14");assertThrows(IllegalStateException.class,()->service.advance(run,request));proof.put("user_id","13");
        proof.put("charged_quota","1001");assertThrows(IllegalStateException.class,()->service.advance(run,request));noRefund();
    }
    @Test void immutableBodyTamperingCannotBeRefunded() {
        jdbc.update("UPDATE vls_compute_billing_slice SET request_hash='wrong'");assertThrows(IllegalStateException.class,()->service.advance(run,request));noRefund();
    }
    @Test void inconsistentWalletAndHoldCountersBlockBeforePreparation() {
        captured=999;released=2001;version++;assertThrows(IllegalStateException.class,()->service.advance(run,request));noRefund();
    }
    @Test void conflictingRefundReceiptStaysQuarantinedAndCannotRelease() {
        ComputeRefundIntentService.Intent intent=refunds.prepare(charge,"trial:"+run+":admin:admin-1","1000","DEV_TRIAL_ACCEPTANCE_REFUND");
        refunds.recordReceipt(intent.id,intent.requestHash,json.createObjectNode().put("operation_id","501").put("refund_id","601").put("ledger_entry_id","701").put("status","SUCCEEDED").put("refunded_quota","999").put("available_quota","0").put("balance_version",3));
        assertEquals("CONFLICT",service.advance(run,request).refundState);assertEquals(0,refundPosts.get());assertEquals(0,releasePosts.get());
    }
    @Test void anotherCorrectionCannotBeReplacedWithNewKey() {
        refunds.prepare(charge,"different-correction","1","OVERCHARGED");assertThrows(IllegalStateException.class,()->service.advance(run,request));assertEquals(1,count("vls_compute_refund_intent"));assertEquals(0,refundPosts.get());
    }
    @Test void sessionPayerAndTenantChangesAreNotTrusted() {
        jdbc.update("UPDATE vls_compute_billing_session SET payer_user_id='14'");assertThrows(IllegalStateException.class,()->service.advance(run,request));noRefund();
    }
    @Test void unconfirmedPhysicalBillingEndBlocksRefund() {
        jdbc.update("UPDATE vls_compute_usage SET billing_end_at=NULL");assertThrows(IllegalStateException.class,()->service.advance(run,request));noRefund();
    }
    @Test void completedEndpointRequiresLoginAndServiceAuthorization() {
        assertTrue(ComputeTrialRefundController.class.isAnnotationPresent(cn.dev33.satoken.annotation.SaCheckLogin.class));
        admin=false;assertThrows(IllegalStateException.class,()->new ComputeTrialRefundController(service).review(run));noRefund();
    }
    @Test void realLoginHelperRejectsOrdinaryUserEvenWithWildcardPermissions() {
        cn.dev33.satoken.context.SaTokenContext previousContext=cn.dev33.satoken.SaManager.getSaTokenContext();
        cn.dev33.satoken.dao.SaTokenDao previousDao=cn.dev33.satoken.SaManager.getSaTokenDao();
        cn.dev33.satoken.stp.StpInterface previousPermissions=cn.dev33.satoken.SaManager.getStpInterface();
        cn.dev33.satoken.dao.SaTokenDaoDefaultImpl memory=new cn.dev33.satoken.dao.SaTokenDaoDefaultImpl();
        try {
            cn.dev33.satoken.SaManager.setSaTokenDao(memory);cn.dev33.satoken.SaManager.setSaTokenContext(new cn.dev33.satoken.context.SaTokenContextForThreadLocal());
            cn.dev33.satoken.SaManager.setStpInterface(new cn.dev33.satoken.stp.StpInterface(){
                public List<String> getPermissionList(Object id,String type){return Collections.singletonList("*:*:*");}
                public List<String> getRoleList(Object id,String type){return Collections.emptyList();}
            });
            org.springframework.mock.web.MockHttpServletRequest http=new org.springframework.mock.web.MockHttpServletRequest();
            cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage.setBox(new cn.dev33.satoken.servlet.model.SaRequestForServlet(http),new cn.dev33.satoken.servlet.model.SaResponseForServlet(new org.springframework.mock.web.MockHttpServletResponse()),new cn.dev33.satoken.servlet.model.SaStorageForServlet(http));
            cn.dev33.satoken.stp.StpUtil.login("ordinary-test-user");
            cn.dev33.satoken.context.SaHolder.getStorage().set(com.ruoyi.common.helper.LoginHelper.USER_KEY,"100");
            ComputeTrialRefundService actual=new ComputeTrialRefundService(jdbc,manager,json,properties,refunds,funding,beans.getBeanProvider(ExternalBillingGateway.class),null,beans.getBeanProvider(ComputePowerProvider.class));
            assertEquals("VLS administrator required",assertThrows(IllegalStateException.class,()->actual.advance(run,request)).getMessage());
            noRefund();verifyNoInteractions(gateway);
        } finally {
            cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage.clearBox();cn.dev33.satoken.SaManager.setStpInterface(previousPermissions);
            cn.dev33.satoken.SaManager.setSaTokenDao(previousDao);cn.dev33.satoken.SaManager.setSaTokenContext(previousContext);memory.endRefreshThread();
        }
    }

    private String unallocated(String reason) {
        String reference="vls:"+usage+":1";
        ObjectNode body=original.deepCopy().put("merchant_charge_id",reference).put("charge_quota","3000");
        jdbc.update("INSERT INTO vls_compute_billing_slice(merchant_charge_id,usage_id,slice_sequence,interval_start_ms,interval_end_ms,is_tail,idempotency_key,request_json,request_hash,amount_text,remainder_numerator,remainder_denominator,state,error_code) VALUES(?,?,1,2,3,0,?,?,?,'3000','0','1','BLOCKED',?)",
            reference,usage,"vls-op-"+UUID.randomUUID(),body.toString(),ModelArtifactObjectStore.hash(body.toString()),reason);
        return reference;
    }
    private void savedSnapshot() {
        jdbc.update("UPDATE vls_compute_billing_session SET accepted_snapshot=?",snapshot.toString());
        jdbc.update("UPDATE vls_compute_paid_run SET quote_snapshot=?",snapshot.toString());
    }
    private void holdExpiry(long expiry) throws Exception {
        ObjectNode requestBody=(ObjectNode)json.readTree(hold.body);requestBody.put("expires_at",String.valueOf(expiry));
        ObjectNode observation=(ObjectNode)json.readTree(jdbc.queryForObject("SELECT detail_json FROM vls_compute_funding_hold_observation WHERE hold_intent_id=?",String.class,hold.id));
        observation.put("expires_at",String.valueOf(expiry));
        jdbc.update("UPDATE vls_compute_funding_intent SET request_json=?,request_hash=? WHERE intent_id=?",requestBody.toString(),ModelArtifactObjectStore.hash(requestBody.toString()),hold.id);
        jdbc.update("UPDATE vls_compute_funding_hold_observation SET detail_json=?,detail_hash=?,expires_at_ms=? WHERE hold_intent_id=?",observation.toString(),ModelArtifactObjectStore.hash(observation.toString()),expiry,hold.id);
        jdbc.update("UPDATE vls_compute_paid_run SET hold_until_ms=?",expiry);hold=funding.recover(hold.id);
    }
    @ParameterizedTest @ValueSource(strings={"TRIAL_UNALLOCATED_BUDGET","TRIAL_UNALLOCATED_PERMIT"})
    void onlyActualCapturesRefundWhileUnallocatedMeteringAndAuditStayUnchanged(String reason) throws Exception {
        String tail=unallocated(reason);
        String risk="TRIAL_UNALLOCATED_BUDGET".equals(reason)?"TRIAL_GROSS_CAP_EXCEEDED":"HOLD_EXPIRED";
        jdbc.update("UPDATE vls_compute_billing_session SET risk_code=?",risk);
        // A GET-only dispatcher pass increments this counter without acquiring a POST claim.
        jdbc.update("UPDATE vls_compute_billing_slice SET attempt_count=2 WHERE merchant_charge_id=?",tail);
        Map<String,Object> before=jdbc.queryForMap("SELECT * FROM vls_compute_billing_slice WHERE merchant_charge_id=?",tail);
        ComputeTrialRefundService.View view=service.review(run);assertEquals("1000",view.grossQuota);assertEquals(1,view.charges.size());
        ComputeTrialRefundService.Result first=finish();assertEquals("REFUND_RELEASE_CONFIRMED",first.outcome);
        actor="admin-2";ComputeTrialRefundService.Result again=service().advance(run,request);
        assertEquals(first.refundIntentId,again.refundIntentId);assertEquals(first.releaseIntentId,again.releaseIntentId);
        assertEquals(1,refundPosts.get());assertEquals(1,releasePosts.get());assertEquals(1,count("vls_compute_refund_intent"));
        assertEquals(before,jdbc.queryForMap("SELECT * FROM vls_compute_billing_slice WHERE merchant_charge_id=?",tail));
        assertEquals(risk,jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session",String.class));
        assertEquals("1000",service.review(run).grossQuota);verify(gateway,never()).byReference(tail);
    }
    @ParameterizedTest @ValueSource(strings={"UNKNOWN","SUBMITTED","PROCESSING"})
    void anUnallocatedReasonNeverConvertsAnUncertainOrSubmittedStateIntoNeverSent(String state) {
        String tail=unallocated("TRIAL_UNALLOCATED_BUDGET");jdbc.update("UPDATE vls_compute_billing_slice SET state=? WHERE merchant_charge_id=?",state,tail);
        assertThrows(IllegalStateException.class,()->service.advance(run,request));noRefund();
    }
    @ParameterizedTest @ValueSource(strings={"SUBMISSION_UNCERTAIN","BUSINESS_REFERENCE_CONFLICT","NOT_AUTHORIZED","TRIAL_GROSS_CAP_EXCEEDED","HOLD_EXPIRED"})
    void previouslySentOrOtherBlockedReasonsRemainIneligible(String reason) {
        unallocated(reason);assertThrows(IllegalStateException.class,()->service.advance(run,request));noRefund();
    }
    @ParameterizedTest @ValueSource(strings={"charge","ledger","lease","leaseTime","nullLeaseTime","hash","amount","zeroAmount","payer","merchant","usage","hold","reference"})
    void incompleteNeverSentProofAndOriginalSnapshotConflictsBlockAllRefunds(String change) throws Exception {
        String tail=unallocated("TRIAL_UNALLOCATED_PERMIT");
        if("charge".equals(change))jdbc.update("UPDATE vls_compute_billing_slice SET charge_id='202' WHERE merchant_charge_id=?",tail);
        else if("ledger".equals(change))jdbc.update("UPDATE vls_compute_billing_slice SET ledger_entry_id='302' WHERE merchant_charge_id=?",tail);
        else if("lease".equals(change))jdbc.update("UPDATE vls_compute_billing_slice SET lease_token='old-worker' WHERE merchant_charge_id=?",tail);
        else if("leaseTime".equals(change))jdbc.update("UPDATE vls_compute_billing_slice SET lease_until_ms=1 WHERE merchant_charge_id=?",tail);
        else if("nullLeaseTime".equals(change)){jdbc.execute("ALTER TABLE vls_compute_billing_slice ALTER COLUMN lease_until_ms DROP NOT NULL");jdbc.update("UPDATE vls_compute_billing_slice SET lease_until_ms=NULL WHERE merchant_charge_id=?",tail);}
        else if("hash".equals(change))jdbc.update("UPDATE vls_compute_billing_slice SET request_hash='changed' WHERE merchant_charge_id=?",tail);
        else if("amount".equals(change))jdbc.update("UPDATE vls_compute_billing_slice SET amount_text='3001' WHERE merchant_charge_id=?",tail);
        else {
            ObjectNode body=(ObjectNode)json.readTree(jdbc.queryForObject("SELECT request_json FROM vls_compute_billing_slice WHERE merchant_charge_id=?",String.class,tail));
            if("zeroAmount".equals(change)){body.put("charge_quota","0");jdbc.update("UPDATE vls_compute_billing_slice SET amount_text='0' WHERE merchant_charge_id=?",tail);}
            else body.put("payer".equals(change)?"user_id":"merchant".equals(change)?"merchant_id":"usage".equals(change)?"usage_session_id":"hold".equals(change)?"hold_id":"merchant_charge_id","other");
            jdbc.update("UPDATE vls_compute_billing_slice SET request_json=?,request_hash=? WHERE merchant_charge_id=?",body.toString(),ModelArtifactObjectStore.hash(body.toString()),tail);
        }
        assertThrows(RuntimeException.class,()->service.advance(run,request));noRefund();
    }
    @Test void anObservedAcceptedLedgerEntryContradictsTheUnallocatedMarker() {
        String tail=unallocated("TRIAL_UNALLOCATED_BUDGET");
        jdbc.update("INSERT INTO vls_compute_billing_ledger_seen VALUES('vls','302',?,?,'CAPTURE','-3000','UNMATCHED')",ModelArtifactObjectStore.hash("accepted"),tail);
        assertThrows(IllegalStateException.class,()->service.advance(run,request));noRefund();assertEquals(1,count("vls_compute_billing_ledger_seen"));
    }
    @Test void existingRefundEvidenceForUnallocatedSliceCannotBeIgnoredOrReplaced() {
        String tail=unallocated("TRIAL_UNALLOCATED_PERMIT");ComputeRefundIntentService.Intent intent=refunds.prepare(charge,"existing-correction","1000","OVERCHARGED");
        jdbc.update("UPDATE vls_compute_refund_intent SET merchant_charge_id=? WHERE refund_intent_id=?",tail,intent.id);
        assertThrows(IllegalStateException.class,()->service.advance(run,request));assertEquals(0,refundPosts.get());assertEquals(0,releasePosts.get());assertEquals(1,count("vls_compute_refund_intent"));
    }
    @ParameterizedTest @ValueSource(strings={"TRIAL_UNALLOCATED_BUDGET","TRIAL_UNALLOCATED_PERMIT"})
    void administratorCannotSelectUnallocatedAmountAsARefund(String reason) {
        String tail=unallocated(reason);request.merchantChargeId=tail;request.expectedRefundQuota="3000";
        request.originalRequestHash=jdbc.queryForObject("SELECT request_hash FROM vls_compute_billing_slice WHERE merchant_charge_id=?",String.class,tail);
        assertThrows(IllegalStateException.class,()->service.advance(run,request));noRefund();
    }
    @Test void concurrentTailChangeDuringAuthorityReadIsRejectedByLockedRecheck() {
        String tail=unallocated("TRIAL_UNALLOCATED_BUDGET");
        when(gateway.byReference(charge)).thenAnswer(i->{jdbc.update("UPDATE vls_compute_billing_slice SET state='UNKNOWN',error_code='SUBMISSION_UNCERTAIN' WHERE merchant_charge_id=?",tail);return ok(proof);});
        assertThrows(IllegalStateException.class,()->service.advance(run,request));noRefund();
        assertEquals("UNKNOWN",jdbc.queryForObject("SELECT state FROM vls_compute_billing_slice WHERE merchant_charge_id=?",String.class,tail));
    }
    @ParameterizedTest @ValueSource(strings={"tail","risk","job"})
    void restartedPreparedRefundRechecksEligibilityAtomicallyBeforeAnyPost(String change) {
        String tail=unallocated("TRIAL_UNALLOCATED_PERMIT");
        ComputeRefundIntentService.Intent prepared=refunds.prepare(charge,"trial:"+run+":admin:admin-1","1000","DEV_TRIAL_ACCEPTANCE_REFUND");
        when(gateway.byReference(charge)).thenAnswer(i->{changeEligibility(change,tail);return ok(proof);});
        assertThrows(IllegalStateException.class,()->service.advance(run,request));
        assertEquals("PREPARED",refunds.recover(prepared.id).state);assertEquals(0,refundPosts.get());assertEquals(0,releasePosts.get());assertEquals(1,count("vls_compute_refund_intent"));
    }
    @ParameterizedTest @ValueSource(strings={"tail","risk","job"})
    void restartedPreparedReleaseRechecksEligibilityInItsSubmissionClaimTransaction(String change) throws Exception {
        String tail=unallocated("TRIAL_UNALLOCATED_PERMIT");
        ComputeRefundIntentService.Intent refund=refunds.prepare(charge,"trial:"+run+":admin:admin-1","1000","DEV_TRIAL_ACCEPTANCE_REFUND");
        refunds.recordReceipt(refund.id,refund.requestHash,json.createObjectNode().put("operation_id","501").put("refund_id","601").put("ledger_entry_id","701")
            .put("status","SUCCEEDED").put("refunded_quota","1000").put("available_quota","9000").put("balance_version",3));
        captured=0;version=3;Thread.sleep(3);funding.recordHoldDetail(hold.id,hold.requestHash,detail(),System.currentTimeMillis());
        ComputeFundingIntentService.Intent release=funding.prepareRefundRelease(hold.id,refund.reference);
        when(gateway.byReference(charge)).thenAnswer(i->{changeEligibility(change,tail);return ok(proof);});
        assertThrows(IllegalStateException.class,()->service.advance(run,request));
        assertEquals("PREPARED",funding.recover(release.id).state);assertEquals(0,refundPosts.get());assertEquals(0,releasePosts.get());
    }
    private void changeEligibility(String change,String tail) {
        if("tail".equals(change))jdbc.update("UPDATE vls_compute_billing_slice SET state='UNKNOWN',error_code='SUBMISSION_UNCERTAIN' WHERE merchant_charge_id=?",tail);
        else if("risk".equals(change))jdbc.update("UPDATE vls_compute_billing_session SET risk_code='NOT_AUTHORIZED'");
        else jdbc.update("INSERT INTO vls_cloud_training_job VALUES('late-job',-6000,'instance','RUNNING')");
    }
    @Test void evidenceThatExpiresWhileWaitingForEligibilityLocksCannotClaimSubmission() throws Exception {
        JdbcTemplate actual=jdbc,waiting=spy(jdbc);
        CountDownLatch held=new CountDownLatch(1),waitingForLock=new CountDownLatch(1),release=new CountDownLatch(1);
        doAnswer(i->{waitingForLock.countDown();return i.callRealMethod();})
            .when(waiting).queryForMap(eq("SELECT * FROM vls_compute_billing_session WHERE usage_id=? FOR UPDATE"),eq(usage));
        jdbc=waiting;ComputeTrialRefundService guarded=service();jdbc=actual;
        ExecutorService pool=Executors.newFixedThreadPool(2);
        org.springframework.transaction.support.TransactionTemplate transaction=new org.springframework.transaction.support.TransactionTemplate(manager);
        try {
            Future<?> holder=pool.submit(()->transaction.execute(s->{
                actual.queryForMap("SELECT * FROM vls_compute_billing_session WHERE usage_id=? FOR UPDATE",usage);held.countDown();
                try{assertTrue(release.await(10,TimeUnit.SECONDS));}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}return null;
            }));
            assertTrue(held.await(5,TimeUnit.SECONDS));long observedAt=System.currentTimeMillis()-29500;
            Future<?> claimant=pool.submit(()->transaction.execute(s->{org.springframework.test.util.ReflectionTestUtils.invokeMethod(guarded,"validateClaim",run,tenant,request,observedAt);return null;}));
            assertTrue(waitingForLock.await(5,TimeUnit.SECONDS));Thread.sleep(700);release.countDown();holder.get(5,TimeUnit.SECONDS);
            ExecutionException failure=assertThrows(ExecutionException.class,()->claimant.get(5,TimeUnit.SECONDS));
            assertEquals("Cleanup evidence expired",failure.getCause().getMessage());noRefund();
        } finally {release.countDown();pool.shutdownNow();}
    }
    @Test void completedStopAndRefundReviewUseTheSameSessionThenRunLockOrder() throws Exception {
        JdbcTemplate actual=jdbc,stopJdbc=spy(jdbc),refundJdbc=spy(jdbc);
        CountDownLatch stopHasSession=new CountDownLatch(1),refundWantsSession=new CountDownLatch(1);
        doAnswer(i->{Object value=i.callRealMethod();stopHasSession.countDown();assertTrue(refundWantsSession.await(5,TimeUnit.SECONDS));return value;})
            .when(stopJdbc).queryForMap(eq("SELECT usage_id FROM vls_compute_billing_session WHERE usage_id=? FOR UPDATE"),eq(usage));
        doAnswer(i->{refundWantsSession.countDown();return i.callRealMethod();})
            .when(refundJdbc).queryForMap(eq("SELECT * FROM vls_compute_billing_session WHERE usage_id=? FOR UPDATE"),eq(usage));
        ComputePlatformAuthorization.Context identity=new ComputePlatformAuthorization.Context();identity.setAuthorized(true);
        identity.setVlsUserId("opener");identity.setVlsTenantId("tenant");identity.setPlatformUserId("verified-original");
        BillingPayer payer=BillingPayer.fromVerifiedSelf(identity,json.createObjectNode().put("id",13));
        ComputeNodeService nodes=mock(ComputeNodeService.class);ComputePublicInventoryService inventory=mock(ComputePublicInventoryService.class);
        when(nodes.publicInventory()).thenReturn(inventory);when(inventory.identity()).thenReturn(identity);
        ComputePaidRunService runs=new ComputePaidRunService(stopJdbc,manager,json,nodes,null,null,null,funding,null,properties,new ComputeBillingPolicy(),beans.getBeanProvider(ComputePowerProvider.class));
        jdbc=refundJdbc;ComputeTrialRefundService reviewed=service();jdbc=actual;
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try {
            Future<ComputePaidRunService.View> stopped=pool.submit(()->runs.stop(run,payer));
            assertTrue(stopHasSession.await(5,TimeUnit.SECONDS));
            Future<ComputeTrialRefundService.View> inspection=pool.submit(()->reviewed.review(run));
            assertEquals("COMPLETED",stopped.get(5,TimeUnit.SECONDS).getState());
            assertEquals("1000",inspection.get(5,TimeUnit.SECONDS).grossQuota);noRefund();
            assertEquals("COMPLETED",jdbc.queryForObject("SELECT state FROM vls_compute_paid_run",String.class));
        } finally {refundWantsSession.countDown();pool.shutdownNow();}
    }
    @ParameterizedTest @ValueSource(strings={"NOT_AUTHORIZED","FUNDING_OPERATION_CONFLICT","HOLD_VERSION_CONFLICT","BUSINESS_REFERENCE_CONFLICT","TRIAL_PERMIT_CHANGED"})
    void unrelatedSessionRisksCannotUseAnUnallocatedTailAsPermission(String risk) {
        unallocated("TRIAL_UNALLOCATED_PERMIT");jdbc.update("UPDATE vls_compute_billing_session SET risk_code=?",risk);
        assertThrows(IllegalStateException.class,()->service.advance(run,request));noRefund();
    }
    @Test void budgetRiskRequiresBudgetTailAndRealBudgetExcess() {
        String tail=unallocated("TRIAL_UNALLOCATED_PERMIT");jdbc.update("UPDATE vls_compute_billing_session SET risk_code='TRIAL_GROSS_CAP_EXCEEDED'");
        assertThrows(IllegalStateException.class,()->service.advance(run,request));
        ObjectNode body=original.deepCopy().put("merchant_charge_id",tail).put("charge_quota","1");
        jdbc.update("UPDATE vls_compute_billing_slice SET error_code='TRIAL_UNALLOCATED_BUDGET',amount_text='1',request_json=?,request_hash=? WHERE merchant_charge_id=?",body.toString(),ModelArtifactObjectStore.hash(body.toString()),tail);
        assertThrows(IllegalStateException.class,()->service.advance(run,request));noRefund();
    }
    @ParameterizedTest @ValueSource(strings={"HOLD_EXPIRED","HOLD_EXPIRING"})
    void expiredHoldRisksRequireTheOriginalHoldActuallyExpired(String risk) throws Exception {
        unallocated("TRIAL_UNALLOCATED_PERMIT");jdbc.update("UPDATE vls_compute_billing_session SET risk_code=?",risk);
        holdExpiry(System.currentTimeMillis()+3600000);assertThrows(IllegalStateException.class,()->service.advance(run,request));noRefund();
        holdExpiry(System.currentTimeMillis()-60000);assertEquals("REFUND_RELEASE_CONFIRMED",finish().outcome);
        assertEquals(risk,jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session",String.class));
    }
    @Test void permitExpiryRiskRequiresPermitTailAndOriginalAcceptedExpiry() throws Exception {
        String tail=unallocated("TRIAL_UNALLOCATED_PERMIT");jdbc.update("UPDATE vls_compute_billing_session SET risk_code='TRIAL_PERMIT_EXPIRED'");
        assertThrows(IllegalStateException.class,()->service.advance(run,request));noRefund();
        snapshot.with("trial_guard").put("expires_at",Instant.now().minusSeconds(30).toString());savedSnapshot();
        jdbc.update("UPDATE vls_compute_billing_slice SET error_code='TRIAL_UNALLOCATED_BUDGET' WHERE merchant_charge_id=?",tail);
        assertThrows(IllegalStateException.class,()->service.advance(run,request));noRefund();
        jdbc.update("UPDATE vls_compute_billing_slice SET error_code='TRIAL_UNALLOCATED_PERMIT' WHERE merchant_charge_id=?",tail);
        assertEquals("REFUND_RELEASE_CONFIRMED",finish().outcome);
    }
    @Test void runWindowRiskUsesOnlyOriginalAcceptedStopMarginAndPreservesAudit() throws Exception {
        unallocated("TRIAL_UNALLOCATED_BUDGET");jdbc.update("UPDATE vls_compute_billing_session SET risk_code='RUN_WINDOW_EXPIRED'");
        snapshot.putObject("run_budget").put("stop_budget_seconds",30).put("polling_safety_seconds",10);savedSnapshot();
        holdExpiry(System.currentTimeMillis()+20000); // Original deadline was 20 seconds ago, hold itself is not expired.
        assertEquals("REFUND_RELEASE_CONFIRMED",finish().outcome);
        assertEquals("RUN_WINDOW_EXPIRED",jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session",String.class));
    }
    @ParameterizedTest @ValueSource(strings={"missing","zeroStop","negativePoll","textStop","fractionPoll","overflowStop","futureDeadline","missingHold","wrongHold","negativeDeadline"})
    void absentInvalidOrNotYetReachedOriginalRunWindowCannotAuthorizeRefund(String change) throws Exception {
        unallocated("TRIAL_UNALLOCATED_BUDGET");jdbc.update("UPDATE vls_compute_billing_session SET risk_code='RUN_WINDOW_EXPIRED'");
        ObjectNode budget=snapshot.putObject("run_budget").put("stop_budget_seconds",30).put("polling_safety_seconds",10);
        holdExpiry(System.currentTimeMillis()+20000);
        switch(change) {
            case "missing":snapshot.remove("run_budget");break;
            case "zeroStop":budget.put("stop_budget_seconds",0);break;
            case "negativePoll":budget.put("polling_safety_seconds",-1);break;
            case "textStop":budget.put("stop_budget_seconds","30");break;
            case "fractionPoll":budget.put("polling_safety_seconds",0.5);break;
            case "overflowStop":budget.put("stop_budget_seconds",Long.MAX_VALUE);break;
            case "futureDeadline":holdExpiry(System.currentTimeMillis()+3600000);break;
            case "missingHold":jdbc.execute("ALTER TABLE vls_compute_paid_run ALTER COLUMN hold_until_ms DROP NOT NULL");jdbc.update("UPDATE vls_compute_paid_run SET hold_until_ms=NULL");break;
            case "wrongHold":jdbc.update("UPDATE vls_compute_paid_run SET hold_until_ms=1");break;
            case "negativeDeadline":budget.put("stop_budget_seconds",Integer.MAX_VALUE);budget.put("polling_safety_seconds",Integer.MAX_VALUE);break;
            default:throw new AssertionError(change);
        }
        savedSnapshot();assertThrows(RuntimeException.class,()->service.advance(run,request));noRefund();
    }
    @ParameterizedTest @ValueSource(strings={"TRIAL_GROSS_CAP_EXCEEDED","HOLD_EXPIRED","TRIAL_PERMIT_EXPIRED","HOLD_EXPIRING","RUN_WINDOW_EXPIRED"})
    void thisCompatibilityDoesNotRelaxRisksForOrdersWithoutUnallocatedSlices(String risk) {
        jdbc.update("UPDATE vls_compute_billing_session SET risk_code=?",risk);assertThrows(IllegalStateException.class,()->service.advance(run,request));noRefund();
    }
}
