package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.compute.billing.*;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** H2 journals and synthetic authority only. No real funding or GPU requests. */
@Tag("dev")
class ComputeOriginalReleaseTest {
    ConfiguredOfficialRunIntegrationTest fixture;
    ComputePaidRunIntegrationTest f;
    ComputeFundingIntentService journal;
    ComputeOriginalReleaseService service;
    ComputeOriginalReleaseService.Request request;
    ObjectNode evidence;
    String run,usage,parent,amount;
    String expectedParentState="CONFLICT",trustedTenant="tenant-test";
    int reads;
    boolean ownerEntry;
    ComputePlatformAuthorization.Context trustedOwner;
    DataSourceTransactionManager manager;
    DefaultListableBeanFactory beans;

    @BeforeEach void setup() throws Exception {
        fixture=new ConfiguredOfficialRunIntegrationTest();fixture.setup();f=fixture.f;
        trustedOwner=f.actor;
        assertEquals("actor-test",trustedOwner.getVlsUserId());assertEquals("tenant-test",trustedOwner.getVlsTenantId());
        assertEquals("platform-test",trustedOwner.getPlatformUserId());assertEquals("13",f.payer.userId);
        manager=new DataSourceTransactionManager(f.jdbc.getDataSource());beans=new DefaultListableBeanFactory();
        try(java.sql.Connection c=f.jdbc.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(c,new FileSystemResource(Paths.get("../ruoyi-admin/src/main/resources/db/migration/V1_2_0_054__compute_corrective_refund_intents.sql")));
        }
        run=fixture.accepted();fixture.queued(run);
        Map<String,Object> row=f.jdbc.queryForMap("SELECT * FROM vls_compute_paid_run WHERE run_id=?",run);
        usage=String.valueOf(row.get("usage_id"));parent=String.valueOf(row.get("hold_intent_id"));amount=String.valueOf(row.get("reserve_quota"));
        journal=new ComputeFundingIntentService(f.jdbc,manager,f.json);
        ComputeFundingIntentService.Intent hold=journal.recover(parent);
        f.jdbc.update("UPDATE vls_compute_paid_run SET power_start_state='UNKNOWN',error_code='START_OUTCOME_UNKNOWN' WHERE run_id=?",run);
        f.jdbc.update("UPDATE vls_compute_funding_intent SET state='CONFLICT',error_code='HOLD_VERSION_CONFLICT',conflicting_receipt_json='{}' WHERE intent_id=?",parent);
        f.jdbc.update("UPDATE vls_compute_billing_session SET risk_code='HOLD_VERSION_CONFLICT',stop_requested=1 WHERE usage_id=?",usage);
        request=new ComputeOriginalReleaseService.Request();request.idempotencyKey="original-non-uuid-cleanup-key";request.reason="ACCEPT_ORIGINAL_RELEASE";
        request.requestBody=FundingContracts.release(hold.merchantId,"vls-release:"+usage+":final",hold.payerUserId,amount).toString();
        request.requestHash=ModelArtifactObjectStore.hash(request.requestBody);
        request.releaseReceipt=f.json.createObjectNode().put("hold_id",hold.holdId).put("released_quota",amount).put("held_quota","0").put("remaining_quota","0").put("hold_status","RELEASED");
        evidence=f.json.createObjectNode();
        evidence.set("operation",f.json.createObjectNode().put("hold_id",hold.holdId).put("user_id",hold.payerUserId).put("status","SUCCEEDED").put("idempotency_key",request.idempotencyKey));
        evidence.set("hold",f.json.createObjectNode().put("hold_id",hold.holdId).put("merchant_hold_id",hold.reference).put("user_id",hold.payerUserId)
            .put("hold_quota",amount).put("captured_quota","0").put("released_quota",amount).put("remaining_quota","0").put("hold_status","RELEASED")
            .put("expires_at",f.json.readTree(hold.body).path("expires_at").asText()).put("created_at",1790000000L).put("updated_at",1790000001L).put("balance_version",2));
        evidence.set("wallet",f.json.createObjectNode().put("user_id",hold.payerUserId).put("balance_quota","9000").put("available_quota","9000").put("held_quota","0").put("balance_version",23));
        evidence.put("instance_key",String.valueOf(row.get("instance_key"))).put("provider_instance_id",fixture.resource.getInstanceId()).put("power","STOPPED").put("stopped_mode","AUTODL_PRO_PHYSICAL_STOPPED");
        service=service(f.jdbc);
    }
    ComputeOriginalReleaseService service(JdbcTemplate jdbc) {
        return new ComputeOriginalReleaseService(jdbc,manager,f.json,journal,fixture.registry,beans.getBeanProvider(NewApiBillingClient.class),beans.getBeanProvider(ComputePowerProvider.class)) {
            @Override protected String requireAdministrator(){return "1";}
            @Override protected String currentTenant(){return trustedTenant;}
            @Override protected ComputePlatformAuthorization.Context currentOwner(){return trustedOwner;}
            @Override protected ObjectNode readEvidence(Map<String,Object> r,Map<String,Object> h){reads++;assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());return evidence.deepCopy();}
        };
    }
    @AfterEach void clear(){fixture.cleanup();}
    String adopt(){return ownerEntry?service.reconcileOwned(run,f.payer,request):service.reconcile(run,request);}

    @ParameterizedTest @MethodSource("releaseStates")
    void adoptsExactOriginalReleaseAndPreservesUnknownConflictDataOwnershipAndAudit(String parentState,String receiptState,String holdState) {
        useParentState(parentState);
        useReleaseStatuses(receiptState,holdState);
        f.jdbc.update("UPDATE vls_compute_paid_run SET state='STOPPING',power_stop_state='UNKNOWN' WHERE run_id=?",run);
        Map<String,Object> originalHold=f.jdbc.queryForMap("SELECT * FROM vls_compute_funding_intent WHERE intent_id=?",parent);
        Object originalObservations=f.jdbc.queryForList("SELECT * FROM vls_compute_funding_hold_observation WHERE hold_intent_id=?",parent);
        int holdPosts=f.wallet.holdPosts;
        String adopted=service.reconcile(run,request);
        assertEquals("CANCELLED",value("vls_compute_paid_run","state","run_id",run));
        assertNull(value("vls_compute_paid_run","active_instance_key","run_id",run));
        assertEquals("START_FAILED",value("vls_compute_usage","state","id",usage));
        assertNull(value("vls_compute_usage","active_server_key","id",usage));
        assertEquals("UNKNOWN",value("vls_compute_paid_run","power_start_state","run_id",run));
        assertEquals("UNKNOWN",value("vls_compute_paid_run","power_stop_state","run_id",run));
        assertEquals(parentState,journal.recover(parent).state);
        assertEquals(originalHold,f.jdbc.queryForMap("SELECT * FROM vls_compute_funding_intent WHERE intent_id=?",parent));
        assertEquals(originalObservations,f.jdbc.queryForList("SELECT * FROM vls_compute_funding_hold_observation WHERE hold_intent_id=?",parent));
        assertEquals("CONFLICT".equals(parentState)?"HOLD_VERSION_CONFLICT":null,value("vls_compute_billing_session","risk_code","usage_id",usage));
        ComputeFundingIntentService.Intent receipt=journal.recover(adopted);
        assertEquals(request.idempotencyKey,receipt.key);assertEquals(request.requestBody,receipt.body);assertEquals(request.requestHash,receipt.requestHash);
        assertEquals(ComputeFundingIntentService.EXTERNAL_RELEASE_ADOPTED,receipt.state);assertFalse(receipt.safeToSend);
        assertTrue(receipt.receipt.contains("\"administrator\":\"1\""));
        assertEquals(0,f.wallet.releasePosts);assertEquals(0,f.wallet.capturePosts);assertEquals(holdPosts,f.wallet.holdPosts);
        verify(f.supplier,never()).submit(any(),anyString(),anyString());
        assertEquals(1,f.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_public_instance WHERE public_state='OCCUPIED'",Integer.class));
        // PAID mode transport still cannot dispatch an imported external release.
        ExternalBillingGateway transport=mock(ExternalBillingGateway.class);
        assertEquals(receipt.state,new ComputeFundingRecoveryService(journal,()->transport,f.properties).advance(adopted,System.currentTimeMillis()).state);
        verifyNoInteractions(transport);
    }
    @ParameterizedTest @MethodSource("releaseStates")
    void identicalReplayIsIdempotentAndChangedKeyCannotReplaceIt(String parentState,String receiptState,String holdState) {
        useParentState(parentState);
        useReleaseStatuses(receiptState,holdState);
        String first=service.reconcile(run,request);assertEquals(first,service.reconcile(run,request));assertEquals(1,reads);
        request.idempotencyKey="changed-key";assertThrows(IllegalStateException.class,()->service.reconcile(run,request));
        assertEquals(1,f.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_funding_intent WHERE operation_kind='RELEASE'",Integer.class));
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void firstAdoptionAndTerminalReplayShareSessionFirstLockOrderWithActualStop(boolean replay) throws Exception {
        String prior=replay?adopt():null;
        CountDownLatch sessionHeld=new CountDownLatch(1),adoptionFirstLock=new CountDownLatch(1);
        AtomicBoolean first=new AtomicBoolean(true);
        AtomicReference<String> firstSql=new AtomicReference<>();
        JdbcTemplate monitored=new JdbcTemplate(f.jdbc.getDataSource()) {
            @Override public Map<String,Object> queryForMap(String sql,Object... args) {
                boolean initial=sql.endsWith(" FOR UPDATE") && first.compareAndSet(true,false);
                if(initial) {
                    firstSql.set(sql);
                    // A session-first contender must announce before blocking on STOP's lock.
                    if(sql.contains("vls_compute_billing_session"))adoptionFirstLock.countDown();
                }
                Map<String,Object> result=super.queryForMap(sql,args);
                // A regressed run-first contender announces only after owning the conflicting row.
                if(initial)adoptionFirstLock.countDown();
                return result;
            }
        };
        ComputeOriginalReleaseService contender=service(monitored);
        ExecutorService executor=Executors.newFixedThreadPool(2);
        try {
            Future<?> stopping=executor.submit(()->{
                TenantContextHolder.setTenantId("tenant-test");
                try {return new TransactionTemplate(manager).execute(tx->{
                    f.jdbc.queryForMap("SELECT usage_id FROM vls_compute_billing_session WHERE usage_id=? FOR UPDATE",usage);
                    sessionHeld.countDown();await(adoptionFirstLock);
                    return f.runs.stop(run,f.payer);
                });} finally {TenantContextHolder.clear();}
            });
            await(sessionHeld);
            Future<String> adoption=executor.submit(()->{
                TenantContextHolder.setTenantId("tenant-test");
                try {return ownerEntry?contender.reconcileOwned(run,f.payer,request):contender.reconcile(run,request);} finally {TenantContextHolder.clear();}
            });
            String adopted=adoption.get(10,TimeUnit.SECONDS);
            stopping.get(10,TimeUnit.SECONDS);
            if(replay)assertEquals(prior,adopted);else assertNotNull(adopted);
            assertTrue(firstSql.get().contains("vls_compute_billing_session"));
            assertEquals("CANCELLED",value("vls_compute_paid_run","state","run_id",run));
            assertEquals(1,f.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_funding_intent WHERE operation_kind='RELEASE'",Integer.class));
            assertEquals(1,reads);
            assertEquals(0,f.wallet.releasePosts);assertEquals(0,f.wallet.capturePosts);
            verify(f.supplier,never()).submit(any(),anyString(),anyString());
        } finally {executor.shutdownNow();}
    }
    @Test void evidenceExpiresWhileWaitingForTheLastOwnershipLock() {
        AtomicLong now=new AtomicLong(System.currentTimeMillis());
        AtomicBoolean ownershipChecked=new AtomicBoolean();
        JdbcTemplate delayed=new JdbcTemplate(f.jdbc.getDataSource()) {
            @Override public List<Map<String,Object>> queryForList(String sql,Object... args) {
                List<Map<String,Object>> result=super.queryForList(sql,args);
                if(sql.contains("FROM vls_cloud_training_job")&&sql.endsWith(" FOR UPDATE")) {
                    ownershipChecked.set(true);now.addAndGet(30001L);
                }
                return result;
            }
        };
        ComputeOriginalReleaseService contender=service(delayed);
        ReflectionTestUtils.setField(contender,"clock",(java.util.function.LongSupplier)now::get);
        assertEquals("Evidence expired",assertThrows(IllegalStateException.class,()->contender.reconcile(run,request)).getMessage());
        assertTrue(ownershipChecked.get());assertUnchanged();
    }
    @ParameterizedTest @ValueSource(strings={"usage_id","tenant_id"})
    void lockedOrderMappingIsRecheckedAfterAuthorityReads(String field) {
        ComputeOriginalReleaseService changing=spy(service);
        doAnswer(invocation->{
            f.jdbc.update("UPDATE vls_compute_paid_run SET "+field+"='changed' WHERE run_id=?",run);
            return evidence.deepCopy();
        }).when(changing).readEvidence(anyMap(),anyMap());
        String message="usage_id".equals(field)?"Order usage changed":"Order tenant mismatch";
        assertEquals(message,assertThrows(IllegalStateException.class,()->changing.reconcile(run,request)).getMessage());
        assertUnchanged();
    }
    private static void await(CountDownLatch latch) {
        try {assertTrue(latch.await(5,TimeUnit.SECONDS),"Concurrent transaction did not reach its lock gate");}
        catch(InterruptedException interrupted){Thread.currentThread().interrupt();throw new AssertionError(interrupted);}
    }
    @Test void ordinaryFundingStillRejectsNonUuidKeys() {
        f.jdbc.update("UPDATE vls_compute_funding_intent SET idempotency_key='non-uuid' WHERE intent_id=?",parent);
        assertThrows(IllegalStateException.class,()->journal.recover(parent));
    }
    @Test void administratorAndLoginAreRequiredBeforeAnyEvidenceRead() {
        ComputeOriginalReleaseService actual=new ComputeOriginalReleaseService(f.jdbc,manager,f.json,journal,fixture.registry,beans.getBeanProvider(NewApiBillingClient.class),beans.getBeanProvider(ComputePowerProvider.class));
        assertThrows(IllegalStateException.class,()->actual.reconcile(run,request));assertEquals(0,reads);
        assertNotNull(ComputeOriginalReleaseController.class.getAnnotation(cn.dev33.satoken.annotation.SaCheckLogin.class));
    }
    @ParameterizedTest @MethodSource("requestCases")
    void rejectsMismatchedOrActiveOriginalWithoutWrites(String parentState,String change) throws Exception {
        useParentState(parentState);
        switch(change) {
            case "key":request.idempotencyKey="another-key";break;
            case "hash":request.requestHash="bad";break;
            case "malformedKey":request.idempotencyKey="key with spaces";break;
            case "reason":request.reason="unaudited reason";break;
            case "missingReceipt":request.releaseReceipt=null;break;
            case "body":request.requestBody=request.requestBody.replace("vls-test","other");request.requestHash=ModelArtifactObjectStore.hash(request.requestBody);break;
            case "noncanonicalBody":request.requestBody=" "+request.requestBody;request.requestHash=ModelArtifactObjectStore.hash(request.requestBody);break;
            case "amount":((ObjectNode)request.releaseReceipt).put("released_quota","1");break;
            case "hold":((ObjectNode)request.releaseReceipt).put("hold_id","999");break;
            case "tenant":f.jdbc.update("UPDATE vls_compute_paid_run SET tenant_id='other' WHERE run_id=?",run);break;
            case "trustedTenant":trustedTenant="other";break;
            case "missingTenant":trustedTenant=null;break;
            case "merchant":f.jdbc.update("UPDATE vls_compute_funding_intent SET merchant_id='other' WHERE intent_id=?",parent);break;
            case "payer":f.jdbc.update("UPDATE vls_compute_billing_session SET payer_user_id='99' WHERE usage_id=?",usage);break;
            case "lease":f.jdbc.update("UPDATE vls_compute_paid_run SET worker_lease_until_ms=? WHERE run_id=?",System.currentTimeMillis()+60000L,run);break;
            case "running":f.jdbc.update("UPDATE vls_compute_usage SET billing_start_at=NOW(),state='RUNNING' WHERE id=?",usage);break;
            case "slice":f.jdbc.update("INSERT INTO vls_compute_billing_slice(merchant_charge_id,usage_id,slice_sequence,interval_start_ms,interval_end_ms,idempotency_key,request_json,request_hash,amount_text,remainder_numerator,remainder_denominator,state) VALUES('s',?,0,1,2,'k','{}','h','1','0','1','UNKNOWN')",usage);break;
            case "refund":f.jdbc.update("INSERT INTO vls_compute_refund_intent(refund_intent_id,merchant_charge_id,correction_reference,merchant_refund_id,usage_id,merchant_id,payer_user_id,charge_id,original_ledger_entry_id,original_request_hash,idempotency_key,request_path,request_json,request_hash,refund_quota,state) VALUES('r','c','x','mr',?,'vls-test','13','c','l','h','k','/','{}','h','1','UNKNOWN')",usage);break;
            case "job":f.jdbc.update("INSERT INTO vls_cloud_training_job(id,node_id,job_state) VALUES('running',-6000,'RUNNING')");break;
            default:fail("Unknown case");
        }
        assertThrows(RuntimeException.class,()->adopt());assertUnchanged();
    }
    @ParameterizedTest @MethodSource("requestCases")
    void expiredReleaseKeepsEveryOriginalRequestAndOwnershipGuard(String parentState,String change) throws Exception {
        useReleaseStatuses("EXPIRED","EXPIRED");
        rejectsMismatchedOrActiveOriginalWithoutWrites(parentState,change);
    }
    @ParameterizedTest @MethodSource("authorityCases")
    void rejectsNonterminalOrUnboundAuthorityEvidence(String parentState,String field) {
        useParentState(parentState);
        switch(field) {
            case "status":((ObjectNode)evidence.path("operation")).put("status","PROCESSING");break;
            case "failedStatus":((ObjectNode)evidence.path("operation")).put("status","FAILED");break;
            case "key":((ObjectNode)evidence.path("operation")).put("idempotency_key","other");break;
            case "user":((ObjectNode)evidence.path("operation")).put("user_id","999");break;
            case "hold":((ObjectNode)evidence.path("operation")).put("hold_id","999");break;
            case "reference":((ObjectNode)evidence.path("hold")).put("merchant_hold_id","other");break;
            case "holdUser":((ObjectNode)evidence.path("hold")).put("user_id","999");break;
            case "holdAmount":((ObjectNode)evidence.path("hold")).put("hold_quota","1");break;
            case "holdExpiry":((ObjectNode)evidence.path("hold")).put("expires_at","1");break;
            case "holdState":((ObjectNode)evidence.path("hold")).put("hold_status","EXPIRED");break;
            case "remaining":((ObjectNode)evidence.path("hold")).put("remaining_quota","1");break;
            case "captured":((ObjectNode)evidence.path("hold")).put("captured_quota","1");break;
            case "released":((ObjectNode)evidence.path("hold")).put("released_quota","1");break;
            case "wallet":((ObjectNode)evidence.path("wallet")).put("held_quota","1");break;
            case "walletAvailable":((ObjectNode)evidence.path("wallet")).put("available_quota","8999");break;
            case "walletUser":((ObjectNode)evidence.path("wallet")).put("user_id","999");break;
            case "power":evidence.put("power","RUNNING");break;
            case "powerUnknown":evidence.put("power","UNKNOWN");break;
            case "stoppedMode":evidence.put("stopped_mode","STOP_REQUESTED");break;
            case "instance":evidence.put("instance_key","other");break;
            default:fail("Unknown field");
        }
        assertThrows(RuntimeException.class,()->adopt());assertUnchanged();
    }
    @ParameterizedTest @MethodSource("authorityCases")
    void expiredReleaseKeepsEveryAuthorityAndNoConsumptionGuard(String parentState,String field) {
        useReleaseStatuses("EXPIRED","EXPIRED");
        if("holdState".equals(field)) {
            useParentState(parentState);((ObjectNode)evidence.path("hold")).put("hold_status","ACTIVE");
            assertThrows(RuntimeException.class,()->adopt());assertUnchanged();
        } else rejectsNonterminalOrUnboundAuthorityEvidence(parentState,field);
    }
    @ParameterizedTest @ValueSource(strings={"CONFLICT","DETAIL_OBSERVED"})
    void expiredReceiptBeforeOriginalExpiryIsRejectedWithoutEvidenceRead(String parentState) {
        useParentState(parentState);
        ((ObjectNode)request.releaseReceipt).put("hold_status","EXPIRED");
        ((ObjectNode)evidence.path("hold")).put("hold_status","EXPIRED");
        assertThrows(IllegalStateException.class,()->service.reconcile(run,request));assertUnchanged();assertEquals(0,reads);
    }
    @ParameterizedTest @ValueSource(strings={"CONFLICT","DETAIL_OBSERVED"})
    void offModeCannotUseRequestReceiptInsteadOfFreshAuthenticatedReads(String parentState) {
        useParentState(parentState);
        ComputeOriginalReleaseService noClient=new ComputeOriginalReleaseService(f.jdbc,manager,f.json,journal,fixture.registry,beans.getBeanProvider(NewApiBillingClient.class),beans.getBeanProvider(ComputePowerProvider.class)) {
            @Override protected String requireAdministrator(){return "1";}
            @Override protected String currentTenant(){return "tenant-test";}
        };
        assertEquals("Read-only billing client unavailable",assertThrows(IllegalStateException.class,()->noClient.reconcile(run,request)).getMessage());assertUnchanged();
    }
    @ParameterizedTest @MethodSource("releaseStates")
    void realReaderUsesOnlyBoundMerchantGetsAndFreshPhysicalStatus(String parentState,String receiptState,String holdState) {
        useParentState(parentState);
        useReleaseStatuses(receiptState,holdState);
        clearInvocations(f.supplier); // Exclude the quote preflight from this operation's reads.
        NewApiBillingClient client=mock(NewApiBillingClient.class);beans.registerSingleton("billing",client);beans.registerSingleton("power",f.supplier);
        when(client.servesMerchant("vls-test")).thenReturn(true);
        when(client.servesOrigin(anyString(),eq("vls-test"))).thenReturn(true);
        when(client.holdOperation(eq("RELEASE"),anyString())).thenReturn(new ExternalBillingGateway.Result(ExternalBillingGateway.Status.UNKNOWN,"HOLD_OPERATION_RESPONSE_UNSPECIFIED",evidence.path("operation")));
        when(client.holdById(anyString())).thenReturn(new ExternalBillingGateway.Result(ExternalBillingGateway.Status.SUCCEEDED,"",evidence.path("hold")));
        when(client.balance(anyString())).thenReturn(new ExternalBillingGateway.Result(ExternalBillingGateway.Status.SUCCEEDED,"",evidence.path("wallet")));
        ComputeOriginalReleaseService actual=new ComputeOriginalReleaseService(f.jdbc,manager,f.json,journal,fixture.registry,beans.getBeanProvider(NewApiBillingClient.class),beans.getBeanProvider(ComputePowerProvider.class)) {
            @Override protected String requireAdministrator(){return "1";}
            @Override protected String currentTenant(){return "tenant-test";}
        };
        assertNotNull(actual.reconcile(run,request));
        verify(client).servesMerchant("vls-test");verify(client,times(2)).servesOrigin(anyString(),eq("vls-test"));verify(client).holdOperation("RELEASE","vls-release:"+usage+":final");verify(client).holdById(evidence.path("hold").path("hold_id").asText());verify(client).balance("13");verifyNoMoreInteractions(client);
        verify(f.supplier).status(any());verify(f.supplier,never()).submit(any(),anyString(),anyString());
    }
    @ParameterizedTest @ValueSource(strings={"PREPARED","CONTRACT_BLOCKED","UNKNOWN","RECEIPT_UNVERIFIED","OPERATION_CONFIRMED","REJECTED","PENDING","SUBMITTED","FAILED"})
    void unobservedOrUncertainOriginalStateCannotBeAdopted(String state) {
        useParentState(state);
        assertThrows(RuntimeException.class,()->service.reconcile(run,request));assertUnchanged();
    }
    @Test void observedStateRequiresItsOriginalObservation() {
        useParentState("DETAIL_OBSERVED");
        f.jdbc.update("DELETE FROM vls_compute_funding_hold_observation WHERE hold_intent_id=?",parent);
        assertThrows(RuntimeException.class,()->service.reconcile(run,request));assertUnchanged();
    }
    @Test void recoveredAdoptionStillRejectsChangedParentState() {
        useParentState("DETAIL_OBSERVED");String adopted=service.reconcile(run,request);
        f.jdbc.update("UPDATE vls_compute_funding_intent SET state='PREPARED' WHERE intent_id=?",parent);
        assertThrows(IllegalStateException.class,()->journal.recover(adopted));
        assertThrows(IllegalStateException.class,()->service.reconcile(run,request));
        assertEquals(1,f.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_funding_intent WHERE operation_kind='RELEASE'",Integer.class));
    }
    @ParameterizedTest @ValueSource(strings={"CONFLICT","DETAIL_OBSERVED"})
    void lockedParentStateIsRecheckedAfterAuthorityReads(String parentState) {
        useParentState(parentState);
        ComputeOriginalReleaseService changing=spy(service);
        doAnswer(invocation->{f.jdbc.update("UPDATE vls_compute_funding_intent SET state='PREPARED' WHERE intent_id=?",parent);return evidence.deepCopy();}).when(changing).readEvidence(anyMap(),anyMap());
        assertThrows(IllegalStateException.class,()->changing.reconcile(run,request));
        expectedParentState="PREPARED";assertUnchanged();
    }
    @ParameterizedTest @MethodSource("replayCases")
    void conflictingReplayCannotRewriteAdoptedProof(String parentState,String change) throws Exception {
        useParentState(parentState);String adopted=adopt();
        String audit=journal.recover(adopted).receipt;
        switch(change){
            case "key":request.idempotencyKey="replacement-key";break;
            case "body":request.requestBody=request.requestBody.replace("vls-test","other");request.requestHash=ModelArtifactObjectStore.hash(request.requestBody);break;
            case "receipt":((ObjectNode)request.releaseReceipt).put("released_quota","1");break;
            case "tenant":trustedTenant="other";break;
            default:fail("Unknown replay change");
        }
        assertThrows(RuntimeException.class,()->adopt());
        assertEquals(audit,journal.recover(adopted).receipt);assertEquals(1,reads);
        assertEquals("CANCELLED",value("vls_compute_paid_run","state","run_id",run));
        assertEquals(1,f.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_funding_intent WHERE operation_kind='RELEASE'",Integer.class));
        assertEquals(0,f.wallet.releasePosts);assertEquals(0,f.wallet.capturePosts);
    }
    @ParameterizedTest @MethodSource("replayCases")
    void expiredReleaseKeepsReplayIntegrity(String parentState,String change) throws Exception {
        useReleaseStatuses("EXPIRED","EXPIRED");
        conflictingReplayCannotRewriteAdoptedProof(parentState,change);
    }
    @ParameterizedTest @MethodSource("expiredAuditCases")
    void expiredAdoptionRecoveryRequiresTheOriginalExpiryAndHistoricalObservation(String parentState,String change) throws Exception {
        useParentState(parentState);useReleaseStatuses("EXPIRED","EXPIRED");
        String adopted=service.reconcile(run,request);
        ObjectNode audit=(ObjectNode)f.json.readTree(journal.recover(adopted).receipt);
        long expires=Long.parseLong(evidence.path("hold").path("expires_at").asText());
        if("beforeExpiry".equals(change))audit.put("observed_at_ms",expires-1);
        else if("missingObservation".equals(change))audit.remove("observed_at_ms");
        else if("futureObservation".equals(change))audit.put("observed_at_ms",System.currentTimeMillis()+60000L);
        else if("expiryMismatch".equals(change))((ObjectNode)audit.path("authority").path("hold")).put("expires_at",String.valueOf(expires-1));
        else fail("Unknown expired audit change");
        f.jdbc.update("UPDATE vls_compute_funding_intent SET receipt_json=? WHERE intent_id=?",audit.toString(),adopted);
        assertThrows(RuntimeException.class,()->journal.recover(adopted));
        assertEquals("CANCELLED",value("vls_compute_paid_run","state","run_id",run));
        assertEquals(1,f.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_funding_intent WHERE operation_kind='RELEASE'",Integer.class));
        assertEquals(0,f.wallet.releasePosts);assertEquals(0,f.wallet.capturePosts);
    }
    @ParameterizedTest @ValueSource(strings={"CONFLICT","DETAIL_OBSERVED"})
    void loggedInOrdinaryUserCannotAdoptEvenWithWildcardPermissions(String parentState) {
        useParentState(parentState);
        cn.dev33.satoken.context.SaTokenContext previousContext=cn.dev33.satoken.SaManager.getSaTokenContext();
        cn.dev33.satoken.dao.SaTokenDao previousDao=cn.dev33.satoken.SaManager.getSaTokenDao();
        cn.dev33.satoken.stp.StpInterface previousPermissions=cn.dev33.satoken.SaManager.getStpInterface();
        cn.dev33.satoken.dao.SaTokenDaoDefaultImpl memory=new cn.dev33.satoken.dao.SaTokenDaoDefaultImpl();
        try {
            cn.dev33.satoken.SaManager.setSaTokenDao(memory);
            cn.dev33.satoken.SaManager.setSaTokenContext(new cn.dev33.satoken.context.SaTokenContextForThreadLocal());
            cn.dev33.satoken.SaManager.setStpInterface(new cn.dev33.satoken.stp.StpInterface(){
                public java.util.List<String> getPermissionList(Object id,String type){return java.util.Collections.singletonList("*:*:*");}
                public java.util.List<String> getRoleList(Object id,String type){return java.util.Collections.emptyList();}
            });
            org.springframework.mock.web.MockHttpServletRequest http=new org.springframework.mock.web.MockHttpServletRequest();
            cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage.setBox(new cn.dev33.satoken.servlet.model.SaRequestForServlet(http),new cn.dev33.satoken.servlet.model.SaResponseForServlet(new org.springframework.mock.web.MockHttpServletResponse()),new cn.dev33.satoken.servlet.model.SaStorageForServlet(http));
            cn.dev33.satoken.stp.StpUtil.login("ordinary-test-user");
            cn.dev33.satoken.context.SaHolder.getStorage().set(com.ruoyi.common.helper.LoginHelper.USER_KEY,"100");
            ComputeOriginalReleaseService actual=new ComputeOriginalReleaseService(f.jdbc,manager,f.json,journal,fixture.registry,beans.getBeanProvider(NewApiBillingClient.class),beans.getBeanProvider(ComputePowerProvider.class)) {
                @Override protected String currentTenant(){return trustedTenant;}
                @Override protected ObjectNode readEvidence(Map<String,Object> r,Map<String,Object> h){reads++;return evidence.deepCopy();}
            };
            assertEquals("VLS administrator required",assertThrows(IllegalStateException.class,()->actual.reconcile(run,request)).getMessage());
            assertEquals(0,reads);assertUnchanged();
        } finally {
            cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage.clearBox();
            cn.dev33.satoken.SaManager.setStpInterface(previousPermissions);cn.dev33.satoken.SaManager.setSaTokenDao(previousDao);cn.dev33.satoken.SaManager.setSaTokenContext(previousContext);memory.endRefreshThread();
        }
    }
    @ParameterizedTest @ValueSource(strings={"CONFLICT","DETAIL_OBSERVED"})
    void expiredReleaseDoesNotGrantAnOrdinaryUserAdministratorAuthority(String parentState) {
        useReleaseStatuses("EXPIRED","EXPIRED");
        loggedInOrdinaryUserCannotAdoptEvenWithWildcardPermissions(parentState);
    }
    @ParameterizedTest @MethodSource("releaseStates")
    void authenticatedOwnerAdoptsWithoutAdministratorAuthorityOrDispatch(String parentState,String receiptState,String holdState) throws Exception {
        useParentState(parentState);useReleaseStatuses(receiptState,holdState);
        service=spy(service);doThrow(new AssertionError("Owner must not require administrator")).when(service).requireAdministrator();
        f.jdbc.update("UPDATE vls_compute_paid_run SET state='STOPPING',power_stop_state='UNKNOWN' WHERE run_id=?",run);
        Map<String,Object> originalHold=f.jdbc.queryForMap("SELECT * FROM vls_compute_funding_intent WHERE intent_id=?",parent);
        Object observations=f.jdbc.queryForList("SELECT * FROM vls_compute_funding_hold_observation WHERE hold_intent_id=?",parent);
        String adopted=service.reconcileOwned(run,f.payer,request);
        assertEquals(adopted,service.reconcileOwned(run,f.payer,request));assertEquals(1,reads);
        ComputeFundingIntentService.Intent imported=journal.recover(adopted);
        JsonNode audit=f.json.readTree(imported.receipt);
        assertEquals(ComputeFundingIntentService.OWNER_ADOPTED_RELEASE_CONTRACT,imported.contractVersion);
        assertEquals("OWNER",audit.path("actor_type").asText());assertFalse(audit.has("administrator"));
        assertEquals(f.payer.actorId,audit.path("actor_id").asText());assertEquals(f.payer.platformSubject,audit.path("platform_subject").asText());
        assertEquals(f.payer.userId,audit.path("payer_user_id").asText());assertEquals(f.payer.tenantId,audit.path("tenant_id").asText());
        assertEquals("CANCELLED",value("vls_compute_paid_run","state","run_id",run));
        assertEquals("OWNER_ADOPTED_ORIGINAL_RELEASE",value("vls_compute_paid_run","error_code","run_id",run));
        assertNull(value("vls_compute_paid_run","active_instance_key","run_id",run));
        assertNull(value("vls_compute_usage","active_server_key","id",usage));
        assertEquals("START_FAILED",value("vls_compute_usage","state","id",usage));
        assertEquals("UNKNOWN",value("vls_compute_paid_run","power_start_state","run_id",run));
        assertEquals("UNKNOWN",value("vls_compute_paid_run","power_stop_state","run_id",run));
        assertEquals(originalHold,f.jdbc.queryForMap("SELECT * FROM vls_compute_funding_intent WHERE intent_id=?",parent));
        assertEquals(observations,f.jdbc.queryForList("SELECT * FROM vls_compute_funding_hold_observation WHERE hold_intent_id=?",parent));
        assertFalse(imported.safeToSend);ExternalBillingGateway transport=mock(ExternalBillingGateway.class);
        assertEquals(imported.state,new ComputeFundingRecoveryService(journal,()->transport,f.properties).advance(adopted,System.currentTimeMillis()).state);
        verifyNoInteractions(transport);assertEquals(0,f.wallet.releasePosts);assertEquals(0,f.wallet.capturePosts);
        verify(f.supplier,never()).submit(any(),anyString(),anyString());
    }
    @ParameterizedTest @ValueSource(strings={"nullPayer","missingIdentity","unverifiedIdentity","trustedActor","trustedActorTenant","trustedSubject","trustedTenant","missingTenant","actor","subject","payer","tenant"})
    void ownerFourTupleAndCurrentIdentityAreRequiredBeforeEvidence(String change) throws Exception {
        BillingPayer payer=f.payer;
        if("nullPayer".equals(change))payer=null;
        else if("missingIdentity".equals(change))trustedOwner=null;
        else if("unverifiedIdentity".equals(change))trustedOwner.setAuthorized(false);
        else if("trustedActor".equals(change))trustedOwner.setVlsUserId("foreign-actor");
        else if("trustedActorTenant".equals(change))trustedOwner.setVlsTenantId("foreign-tenant");
        else if("trustedSubject".equals(change))trustedOwner.setPlatformUserId("foreign-subject");
        else if("trustedTenant".equals(change))trustedTenant="foreign-tenant";
        else if("missingTenant".equals(change))trustedTenant=null;
        else {
            ComputePlatformAuthorization.Context actor=new ComputePlatformAuthorization.Context();actor.setAuthorized(true);
            actor.setVlsUserId("actor".equals(change)?"foreign-actor":f.payer.actorId);
            actor.setPlatformUserId("subject".equals(change)?"foreign-subject":f.payer.platformSubject);
            actor.setVlsTenantId("tenant".equals(change)?"foreign-tenant":f.payer.tenantId);
            payer=BillingPayer.fromVerifiedSelf(actor,f.json.readTree("payer".equals(change)?"{\"id\":14}":"{\"id\":13}"));
            trustedOwner=actor;if("tenant".equals(change))trustedTenant=actor.getVlsTenantId();
        }
        BillingPayer candidate=payer;
        assertThrows(RuntimeException.class,()->service.reconcileOwned(run,candidate,request));assertEquals(0,reads);assertUnchanged();
    }
    @ParameterizedTest @MethodSource("requestCases")
    void ownerKeepsAllOriginalRequestAndConsumptionGuards(String parentState,String change) throws Exception {
        ownerEntry=true;useReleaseStatuses("EXPIRED","EXPIRED");rejectsMismatchedOrActiveOriginalWithoutWrites(parentState,change);
    }
    @ParameterizedTest @MethodSource("authorityCases")
    void ownerKeepsEveryOriginalAuthorityGuard(String parentState,String change) {
        ownerEntry=true;expiredReleaseKeepsEveryAuthorityAndNoConsumptionGuard(parentState,change);
    }
    @Test void ownerCannotOmitOriginalReleaseProof() {
        assertThrows(RuntimeException.class,()->service.reconcileOwned(run,f.payer,null));assertEquals(0,reads);assertUnchanged();
    }
    @ParameterizedTest @ValueSource(strings={"opening_user_id","platform_subject","payer_user_id","merchant_id","hold_intent_id","quote_snapshot"})
    void lockedOwnerAndOriginalAssociationAreRecheckedAfterEvidence(String field) {
        ComputeOriginalReleaseService changing=spy(service);
        doAnswer(invocation->{f.jdbc.update("UPDATE vls_compute_paid_run SET "+field+"='changed' WHERE run_id=?",run);return evidence.deepCopy();})
            .when(changing).readEvidence(anyMap(),anyMap());
        assertThrows(RuntimeException.class,()->changing.reconcileOwned(run,f.payer,request));assertUnchanged();
    }
    @ParameterizedTest @ValueSource(strings={"opening_user_id","payer_account_ref","last_running_ms","metered_until_ms"})
    void ownerRequiresUnmeteredUsageBoundToItsActorAndSubject(String field) {
        if(field.endsWith("_ms"))f.jdbc.update("UPDATE vls_compute_billing_session SET "+field+"=1 WHERE usage_id=?",usage);
        else f.jdbc.update("UPDATE vls_compute_usage SET "+field+"='changed' WHERE id=?",usage);
        assertThrows(RuntimeException.class,()->service.reconcileOwned(run,f.payer,request));assertUnchanged();
    }
    @ParameterizedTest @MethodSource("replayCases")
    void ownerReplayCannotReplaceTheOriginalProof(String parentState,String change) throws Exception {
        ownerEntry=true;useReleaseStatuses("EXPIRED","EXPIRED");conflictingReplayCannotRewriteAdoptedProof(parentState,change);
    }
    @ParameterizedTest @ValueSource(strings={"actor_id","platform_subject","payer_user_id","tenant_id","run_id","actor_type","administrator","runActor","runSubject","runPayer","runUsage","runRelease","runHold","runMerchant","sessionActor","usageSubject","contract"})
    void ownerRecoveryBindsAuditToTheOriginalRunUsageAndRelease(String change) throws Exception {
        String adopted=service.reconcileOwned(run,f.payer,request);
        ObjectNode audit=(ObjectNode)f.json.readTree(journal.recover(adopted).receipt);
        if(Arrays.asList("actor_id","platform_subject","payer_user_id","tenant_id","run_id","actor_type").contains(change))audit.put(change,"changed");
        else if("administrator".equals(change))audit.put("administrator","1");
        else if("contract".equals(change))f.jdbc.update("UPDATE vls_compute_funding_intent SET contract_version=? WHERE intent_id=?",ComputeFundingIntentService.ADOPTED_RELEASE_CONTRACT,adopted);
        else if("sessionActor".equals(change))f.jdbc.update("UPDATE vls_compute_billing_session SET opening_user_id='changed' WHERE usage_id=?",usage);
        else if("usageSubject".equals(change))f.jdbc.update("UPDATE vls_compute_usage SET payer_account_ref='changed' WHERE id=?",usage);
        else {
            String field="runActor".equals(change)?"opening_user_id":"runSubject".equals(change)?"platform_subject":"runPayer".equals(change)?"payer_user_id":
                "runUsage".equals(change)?"usage_id":"runRelease".equals(change)?"release_intent_id":"runHold".equals(change)?"hold_intent_id":"merchant_id";
            f.jdbc.update("UPDATE vls_compute_paid_run SET "+field+"='changed' WHERE run_id=?",run);
        }
        f.jdbc.update("UPDATE vls_compute_funding_intent SET receipt_json=? WHERE intent_id=?",audit.toString(),adopted);
        assertThrows(RuntimeException.class,()->journal.recover(adopted));assertEquals(0,f.wallet.releasePosts);assertEquals(0,f.wallet.capturePosts);
    }
    @ParameterizedTest @ValueSource(booleans={false,true})
    void ownerAndNormalStopKeepTheSessionFirstLockOrder(boolean replay) throws Exception {
        ownerEntry=true;firstAdoptionAndTerminalReplayShareSessionFirstLockOrderWithActualStop(replay);
    }
    @Test void simultaneousOwnerAndAdministratorImportOnlyOneOriginalRelease() throws Exception {
        CountDownLatch observed=new CountDownLatch(2);
        ComputeOriginalReleaseService concurrent=spy(service);
        doAnswer(invocation->{observed.countDown();await(observed);return evidence.deepCopy();}).when(concurrent).readEvidence(anyMap(),anyMap());
        ExecutorService executor=Executors.newFixedThreadPool(2);
        try {
            Future<String> owner=executor.submit(()->concurrent.reconcileOwned(run,f.payer,request));
            Future<String> admin=executor.submit(()->concurrent.reconcile(run,request));
            assertEquals(owner.get(10,TimeUnit.SECONDS),admin.get(10,TimeUnit.SECONDS));
            assertEquals(1,f.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_funding_intent WHERE operation_kind='RELEASE'",Integer.class));
            assertEquals("CANCELLED",value("vls_compute_paid_run","state","run_id",run));
            assertEquals(0,f.wallet.releasePosts);assertEquals(0,f.wallet.capturePosts);verify(f.supplier,never()).submit(any(),anyString(),anyString());
        } finally {executor.shutdownNow();}
    }
    static Stream<Arguments> releaseStates(){return Stream.of("CONFLICT","DETAIL_OBSERVED").flatMap(parentState->
        Stream.of("RELEASED","EXPIRED").flatMap(receipt->Stream.of("RELEASED","EXPIRED").map(hold->Arguments.of(parentState,receipt,hold))));}
    static Stream<Arguments> expiredAuditCases(){return bothStates("beforeExpiry","missingObservation","futureObservation","expiryMismatch");}
    static Stream<Arguments> requestCases(){return bothStates("key","hash","malformedKey","reason","missingReceipt","body","noncanonicalBody","amount","hold","tenant","trustedTenant","missingTenant","merchant","payer","lease","running","slice","refund","job");}
    static Stream<Arguments> authorityCases(){return bothStates("status","failedStatus","key","user","hold","reference","holdUser","holdAmount","holdExpiry","holdState","remaining","captured","released","wallet","walletAvailable","walletUser","power","powerUnknown","stoppedMode","instance");}
    static Stream<Arguments> replayCases(){return bothStates("key","body","receipt","tenant");}
    static Stream<Arguments> bothStates(String... changes){return Stream.of("CONFLICT","DETAIL_OBSERVED").flatMap(state->Arrays.stream(changes).map(change->Arguments.of(state,change)));}
    void useReleaseStatuses(String receiptState,String holdState) {
        ((ObjectNode)request.releaseReceipt).put("hold_status",receiptState);
        ((ObjectNode)evidence.path("hold")).put("hold_status",holdState);
        if(!"EXPIRED".equals(receiptState)&&!"EXPIRED".equals(holdState))return;
        try {
            long expires=System.currentTimeMillis()-60000L;
            ObjectNode body=(ObjectNode)f.json.readTree(value("vls_compute_funding_intent","request_json","intent_id",parent));
            body.put("expires_at",String.valueOf(expires));
            f.jdbc.update("UPDATE vls_compute_funding_intent SET request_json=?,request_hash=? WHERE intent_id=?",body.toString(),ModelArtifactObjectStore.hash(body.toString()),parent);
            Map<String,Object> stored=f.jdbc.queryForMap("SELECT * FROM vls_compute_funding_hold_observation WHERE hold_intent_id=?",parent);
            ObjectNode originalDetail=(ObjectNode)f.json.readTree(String.valueOf(stored.get("detail_json")));
            originalDetail.put("expires_at",String.valueOf(expires));originalDetail.put("hold_status","EXPIRED");
            f.jdbc.update("UPDATE vls_compute_funding_hold_observation SET expires_at_ms=?,hold_status='EXPIRED',detail_json=?,detail_hash=? WHERE hold_intent_id=?",
                expires,originalDetail.toString(),ModelArtifactObjectStore.hash(originalDetail.toString()),parent);
            f.jdbc.update("UPDATE vls_compute_paid_run SET hold_until_ms=? WHERE run_id=?",expires,run);
            f.jdbc.update("UPDATE vls_compute_billing_session SET funding_expires_at_ms=? WHERE usage_id=?",expires,usage);
            ((ObjectNode)evidence.path("hold")).put("expires_at",String.valueOf(expires));
        } catch(Exception invalidFixture) {throw new AssertionError(invalidFixture);}
    }
    void useParentState(String state){
        expectedParentState=state;
        f.jdbc.update("UPDATE vls_compute_funding_intent SET state=? WHERE intent_id=?",state,parent);
        if("DETAIL_OBSERVED".equals(state)){
            f.jdbc.update("UPDATE vls_compute_funding_intent SET error_code='HOLD_DETAIL_OBSERVED',conflicting_receipt_json=NULL WHERE intent_id=?",parent);
            f.jdbc.update("UPDATE vls_compute_billing_session SET risk_code=NULL WHERE usage_id=?",usage);
            request.idempotencyKey="f9bff8e4-4d1c-4c40-b561-1d9b937655c6";
            ((ObjectNode)evidence.path("operation")).put("idempotency_key",request.idempotencyKey);
        }
    }
    void assertUnchanged(){assertEquals("STARTING",value("vls_compute_paid_run","state","run_id",run));assertEquals(0,f.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_funding_intent WHERE operation_kind='RELEASE'",Integer.class));assertEquals(expectedParentState,value("vls_compute_funding_intent","state","intent_id",parent));}
    String value(String table,String field,String key,String id){return f.jdbc.queryForObject("SELECT "+field+" FROM "+table+" WHERE "+key+"=?",String.class,id);}
}
