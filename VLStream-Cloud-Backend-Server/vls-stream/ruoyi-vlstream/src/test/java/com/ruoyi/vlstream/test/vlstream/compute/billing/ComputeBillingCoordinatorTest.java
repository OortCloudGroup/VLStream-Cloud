package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.vlstream.test.vlstream.compute.ComputePowerProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import java.time.Instant;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ComputeBillingCoordinatorTest {
    ComputeBillingSettlementTest f;
    ComputeBillingCoordinator coordinator;
    ComputeBillingReconciliationService reconciliation;
    Lifecycle lifecycle;
    LedgerGateway gateway;
    DefaultListableBeanFactory beans;
    @BeforeEach void setup() throws Exception {
        f=new ComputeBillingSettlementTest();f.database();
        String migration=System.getProperty("billing.migration");
        if(migration==null){java.nio.file.Path root=java.nio.file.Paths.get(System.getProperty("user.dir")).toAbsolutePath();while(!java.nio.file.Files.exists(root.resolve("BUSINESS_PROCESSES.md")))root=root.getParent();migration=root.resolve("VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/db/migration/V1_2_0_048__compute_billing_slices.sql").toString();}
        try(java.sql.Connection connection=f.jdbc.getDataSource().getConnection()){ScriptUtils.executeSqlScript(connection,new FileSystemResource(migration.replace("048__compute_billing_slices","049__compute_billing_coordination")));}
        lifecycle=new Lifecycle();gateway=new LedgerGateway();beans=new DefaultListableBeanFactory();beans.registerSingleton("lifecycle",lifecycle);beans.registerSingleton("gateway",gateway);
        reconciliation=reconciler();coordinator=new ComputeBillingCoordinator(f.jdbc,f.service,f.properties,beans.getBeanProvider(BillingLifecycleGateway.class),reconciliation);
        f.properties.setSafetyStopEnabled(true);
    }
    ComputeBillingReconciliationService reconciler(){return new ComputeBillingReconciliationService(f.jdbc,f.manager,f.properties,beans.getBeanProvider(ExternalBillingGateway.class),f.json);}
    String usage() throws Exception{return f.usage("tenant-test","1","500000","1");}
    void due(){f.jdbc.update("UPDATE vls_compute_billing_slice SET next_attempt_ms=0");}
    @Test void disabledSchedulerPerformsNoProviderOrLedgerCalls() throws Exception {
        usage();f.properties.setEnabled(false);coordinator.scan();assertEquals(0,lifecycle.observations);assertEquals(0,gateway.ledgerCalls);
    }
    @Test void devTrialTickIgnoresAnotherRunBeforeAnyLifecycleSettlementOrRiskChange() throws Exception {
        String selected=usage(),other=usage();
        String run="91de8320-9030-4263-8d5f-8602dcc9482e";
        f.jdbc.execute("CREATE TABLE vls_compute_paid_run(run_id VARCHAR(36) PRIMARY KEY,usage_id VARCHAR(36),payer_user_id VARCHAR(100),merchant_id VARCHAR(100),node_id BIGINT)");
        f.jdbc.update("INSERT INTO vls_compute_paid_run VALUES(?,?,?,?,?)",run,selected,"13","vls",-6000L);
        // Even the same payer and official node cannot make a different run part of this permit.
        f.jdbc.update("INSERT INTO vls_compute_paid_run VALUES(?,?,?,?,?)","40c8025d-d00a-4a19-aa83-a8f2fbe39f6f",other,"13","vls",-6000L);
        f.jdbc.update("UPDATE vls_compute_billing_session SET stop_requested=1,risk_code='EXISTING_OTHER_RUN_RISK',next_poll_ms=0 WHERE usage_id=?",other);
        String checkpoint="SELECT risk_code,stop_requested,safety_state,metered_until_ms,last_running_ms,next_poll_ms,physical_stop_observed_ms FROM vls_compute_billing_session WHERE usage_id=?";
        Map<String,Object> before=f.jdbc.queryForMap(checkpoint,other);
        f.properties.setBaseUrl(ComputeBillingTrialGuard.TARGET);f.properties.setMerchantId("vls");
        f.properties.setAllowWrites(true);f.properties.setPaidAdmissionEnabled(true);
        f.properties.getTrial().setEnabled(true);f.properties.getTrial().setRunId(run);
        f.properties.getTrial().setMaximumWalletCny("3.47");f.properties.getTrial().setExpiresAt(Instant.now().plusSeconds(3600).toString());
        ComputeBillingSettlementService settlements=org.mockito.Mockito.mock(ComputeBillingSettlementService.class);
        ComputeBillingReconciliationService reconcile=org.mockito.Mockito.mock(ComputeBillingReconciliationService.class);
        coordinator=new ComputeBillingCoordinator(f.jdbc,settlements,f.properties,beans.getBeanProvider(BillingLifecycleGateway.class),reconcile);
        lifecycle.power="UNKNOWN";lifecycle.at=60000;

        coordinator.tick(other,lifecycle);

        org.mockito.Mockito.verifyNoInteractions(settlements,reconcile);
        assertEquals(0,lifecycle.observations);assertEquals(0,lifecycle.stops);
        assertEquals(0,gateway.ledgerCalls);assertEquals(0,f.gateway.queries);assertTrue(f.gateway.captures.isEmpty());
        assertEquals(before,f.jdbc.queryForMap(checkpoint,other));
        assertTrue(f.service.audit(other).isEmpty());
    }
    @Test void scanPastMoreThanOnePageOfNeverStartedHistoryStillMetersActiveUsage() throws Exception {
        for(int i=0;i<101;i++) {
            String failed=usage();
            f.jdbc.update("UPDATE vls_compute_usage SET state='START_FAILED',billing_start_at=NULL WHERE id=?",failed);
            f.jdbc.update("UPDATE vls_compute_billing_session SET control_snapshot='TEST_ONLY_CONTROL',safety_state='ARMED',metered_until_ms=0,last_running_ms=0,next_poll_ms=0 WHERE usage_id=?",failed);
        }
        String active=usage();
        f.jdbc.update("UPDATE vls_compute_billing_session SET control_snapshot='TEST_ONLY_CONTROL',safety_state='ARMED',next_poll_ms=1 WHERE usage_id=?",active);
        lifecycle.at=60000;

        coordinator.scan();

        assertEquals(1,lifecycle.observations);
        assertEquals(f.start+60000,f.jdbc.queryForObject("SELECT last_running_ms FROM vls_compute_billing_session WHERE usage_id=?",Long.class,active));
        assertEquals(1,f.service.audit(active).size());
        assertEquals(0,f.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_billing_slice WHERE usage_id<>?",Integer.class,active));
        assertEquals(101,f.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_billing_session s JOIN vls_compute_usage u ON u.id=s.usage_id WHERE u.state='START_FAILED' AND u.billing_start_at IS NULL AND s.safety_state='ARMED' AND s.next_poll_ms=0",Integer.class));
    }
    @Test void scanPreservesUnknownStartAndItsStopRequestUntilRunningIsConfirmed() throws Exception {
        String pending=usage();
        f.jdbc.update("UPDATE vls_compute_usage SET state='RESERVED',billing_start_at=NULL WHERE id=?",pending);
        f.jdbc.update("UPDATE vls_compute_billing_session SET control_snapshot='TEST_ONLY_CONTROL',safety_state='ARMED',stop_requested=1,risk_code='START_OUTCOME_UNKNOWN',metered_until_ms=0,last_running_ms=0,next_poll_ms=0 WHERE usage_id=?",pending);
        lifecycle.at=60000;

        coordinator.scan();

        assertEquals(0,lifecycle.observations);assertEquals(0,lifecycle.stops);
        assertTrue(f.service.audit(pending).isEmpty());
        assertNull(f.jdbc.queryForObject("SELECT billing_start_at FROM vls_compute_usage WHERE id=?",java.sql.Timestamp.class,pending));
        assertEquals("RESERVED",f.jdbc.queryForObject("SELECT state FROM vls_compute_usage WHERE id=?",String.class,pending));
        assertEquals("START_OUTCOME_UNKNOWN",f.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session WHERE usage_id=?",String.class,pending));
        assertEquals(1,f.jdbc.queryForObject("SELECT stop_requested FROM vls_compute_billing_session WHERE usage_id=?",Integer.class,pending));
        assertEquals(0L,f.jdbc.queryForObject("SELECT next_poll_ms FROM vls_compute_billing_session WHERE usage_id=?",Long.class,pending));

        // The paid-run worker later establishes running evidence for the same original usage.
        f.jdbc.update("UPDATE vls_compute_usage SET state='RUNNING',billing_start_at=? WHERE id=?",new java.sql.Timestamp(f.start),pending);
        coordinator.scan();

        assertEquals(1,lifecycle.observations);assertEquals(1,lifecycle.stops);
        assertEquals("STOP_REQUESTED",f.jdbc.queryForObject("SELECT safety_state FROM vls_compute_billing_session WHERE usage_id=?",String.class,pending));
        assertEquals(1,f.jdbc.queryForObject("SELECT stop_requested FROM vls_compute_billing_session WHERE usage_id=?",Integer.class,pending));
    }
    @Test void periodicSealingAndRestartNeverCreateDuplicateSlicesOrKeys() throws Exception {
        String id=usage();lifecycle.at=90000;coordinator.tick(id,lifecycle);
        assertEquals(1,f.service.audit(id).size());Map<String,Object> first=f.service.audit(id).get(0);
        due();coordinator=new ComputeBillingCoordinator(f.jdbc,f.service(f.jdbc),f.properties,beans.getBeanProvider(BillingLifecycleGateway.class),reconciler());coordinator.tick(id,lifecycle);
        assertEquals(1,f.service.audit(id).size());assertEquals(first.get("merchant_charge_id"),f.service.audit(id).get(0).get("merchant_charge_id"));
        assertEquals(1,f.gateway.captures.size());assertEquals("SETTLED",f.service.audit(id).get(0).get("state"));
    }
    @Test void scanResumesStoppedUsageWhoseOriginalChargeResolvedBeforeItsBlockedTail() throws Exception {
        String id=usage(),ref=f.interval(id,60000);
        f.gateway.loseResponse=true;f.service.dispatch(ref);
        String tail=f.interval(id,90000);Map<String,Object> original=f.row(tail);
        f.service.confirmPhysicalStop(id,Instant.ofEpochMilli(f.start+100000));
        assertEquals("BLOCKED",f.state(tail));
        // Persisted checkpoint after original charge proof but before a separate recovery prerequisite was ready.
        f.jdbc.update("UPDATE vls_compute_billing_slice SET state='SETTLED',error_code=NULL,charge_id='charge-test',ledger_entry_id='ledger-test' WHERE merchant_charge_id=?",ref);
        f.jdbc.update("UPDATE vls_compute_billing_session SET control_snapshot='TEST_ONLY_CONTROL' WHERE usage_id=?",id);
        assertEquals("STOP_CONFIRMED",f.jdbc.queryForObject("SELECT safety_state FROM vls_compute_billing_session WHERE usage_id=?",String.class,id));
        f.gateway.loseResponse=false;lifecycle.power="STOPPED";lifecycle.at=100000;

        coordinator.scan();
        assertEquals(1,lifecycle.observations);assertEquals("UNKNOWN",f.state(tail));
        assertNull(f.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session WHERE usage_id=?",String.class,id));
        assertEquals(1,f.jdbc.queryForObject("SELECT stop_requested FROM vls_compute_billing_session WHERE usage_id=?",Integer.class,id));
        for(String field:new String[]{"request_json","request_hash","idempotency_key","amount_text"})assertEquals(original.get(field),f.row(tail).get(field));
        due();coordinator.scan();
        assertEquals("SETTLED",f.state(tail));assertEquals(2,f.gateway.captures.size());
        int observations=lifecycle.observations;coordinator.scan();assertEquals(observations,lifecycle.observations);
    }
    @Test void authorizationFailureRequestsStopButKeepsMeteringUntilStoppedObservation() throws Exception {
        String id=usage();f.gateway.operation=ExternalBillingGateway.Result.of(ExternalBillingGateway.Status.NOT_AUTHORIZED);
        lifecycle.at=60000;coordinator.tick(id,lifecycle);assertEquals(1,lifecycle.stops);
        assertNull(f.jdbc.queryForObject("SELECT billing_end_at FROM vls_compute_usage WHERE id=?",java.sql.Timestamp.class,id));
        lifecycle.at=90000;coordinator.tick(id,lifecycle);
        lifecycle.power="STOPPED";lifecycle.at=100000;coordinator.tick(id,lifecycle);
        assertEquals(f.start+90000,f.jdbc.queryForObject("SELECT billing_end_at FROM vls_compute_usage WHERE id=?",java.sql.Timestamp.class,id).getTime());
        assertEquals(f.start+100000,f.jdbc.queryForObject("SELECT physical_stop_observed_ms FROM vls_compute_billing_session",Long.class));
        assertEquals("STOP_CONFIRMED",f.jdbc.queryForObject("SELECT safety_state FROM vls_compute_billing_session",String.class));
        assertEquals(2,f.service.audit(id).size());assertEquals("8334",f.service.audit(id).get(1).get("amount_text"));assertEquals(0,f.gateway.captures.size());
    }
    @Test void stoppingRequestIsNotConfirmationAndUnknownProviderCannotEndBilling() throws Exception {
        String id=usage();lifecycle.power="UNKNOWN";lifecycle.at=60000;coordinator.tick(id,lifecycle);
        assertEquals(1,lifecycle.stops);assertEquals(0,f.service.audit(id).size());assertNull(f.jdbc.queryForObject("SELECT billing_end_at FROM vls_compute_usage WHERE id=?",java.sql.Timestamp.class,id));
    }
    @Test void durableBackoffSkipsPrematureRetriesAndProlongedProcessingRequestsSafetyStop() throws Exception {
        String id=usage();f.gateway.operation=ExternalBillingGateway.Result.of(ExternalBillingGateway.Status.PROCESSING);lifecycle.at=60000;
        coordinator.tick(id,lifecycle);int queries=f.gateway.queries;coordinator.tick(id,lifecycle);assertEquals(queries,f.gateway.queries);
        f.jdbc.update("UPDATE vls_compute_billing_slice SET attempt_count=4,next_attempt_ms=0");coordinator.tick(id,lifecycle);
        assertEquals(1,lifecycle.stops);assertEquals("SETTLEMENT_CONFIRMATION_EXPIRED",f.jdbc.queryForObject("SELECT risk_code FROM vls_compute_billing_session",String.class));
    }
    @Test void ledgerCursorAndDedupSurviveReconcilerRestart() throws Exception {
        String id=usage(),ref=f.interval(id,60000);f.service.dispatch(ref);f.service.dispatch(ref);
        gateway.page=page(ref,true,"page-2");reconciliation.tick();assertEquals("page-2",f.jdbc.queryForObject("SELECT page_cursor FROM vls_compute_billing_reconciliation",String.class));
        gateway.page=page(ref,false,"");reconciler().tick();
        assertEquals(1,f.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_billing_ledger_seen",Integer.class));
        assertEquals("MATCHED",f.jdbc.queryForObject("SELECT match_state FROM vls_compute_billing_ledger_seen",String.class));
        assertEquals("DONE",f.jdbc.queryForObject("SELECT state FROM vls_compute_billing_reconciliation",String.class));
    }
    @Test void unavailableLedgerDoesNotAdvanceWindowOrCursor() throws Exception {
        f.interval(usage(),60000);reconciliation.tick();
        assertEquals("RETRY",f.jdbc.queryForObject("SELECT state FROM vls_compute_billing_reconciliation",String.class));
        assertNull(f.jdbc.queryForObject("SELECT page_cursor FROM vls_compute_billing_reconciliation",String.class));assertEquals(0,f.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_billing_ledger_seen",Integer.class));
    }
    @Test void changedImmutableLedgerEntryFreezesCursorInsteadOfSilentlyOverwriting() throws Exception {
        String ref=f.interval(usage(),60000);gateway.page=page(ref,true,"page-2");reconciliation.tick();
        gateway.page=page(ref,true,"page-3");((ObjectNode)gateway.page.path("entries").get(0)).put("quota_delta","-1");reconciler().tick();
        assertEquals("CONFLICT",f.jdbc.queryForObject("SELECT state FROM vls_compute_billing_reconciliation",String.class));
        assertEquals("page-2",f.jdbc.queryForObject("SELECT page_cursor FROM vls_compute_billing_reconciliation",String.class));
        assertEquals("-16666",f.jdbc.queryForObject("SELECT quota_delta FROM vls_compute_billing_ledger_seen",String.class));
    }
    @Test void malformedPageRollsBackAllImportedEntriesAndCursor() throws Exception {
        String ref=f.interval(usage(),60000);ObjectNode page=(ObjectNode)page(ref,true,"page-2");((com.fasterxml.jackson.databind.node.ArrayNode)page.path("entries")).add(f.json.createObjectNode());gateway.page=page;
        reconciliation.tick();assertEquals(0,f.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_billing_ledger_seen",Integer.class));assertNull(f.jdbc.queryForObject("SELECT page_cursor FROM vls_compute_billing_reconciliation",String.class));
    }
    JsonNode page(String ref,boolean more,String cursor) throws Exception {
        return f.json.readTree("{\"entries\":[{\"ledger_entry_id\":\"ledger-test\",\"merchant_ref\":\""+ref+"\",\"entry_type\":\"CHARGE\",\"quota_delta\":\"-16666\",\"user_id\":123,\"reference_id\":\"charge-test\",\"balance_before\":\"1000000\",\"balance_after\":\"983334\",\"created_at\":\"2026-01-01T00:01:00Z\"}],\"has_more\":"+more+",\"next_cursor\":\""+cursor+"\"}");
    }
    class Lifecycle implements BillingLifecycleGateway {
        long at;String power="RUNNING";int observations,stops;
        @Override public Observation observe(String id){observations++;return new Observation(new ComputePowerProvider.Snapshot(power,"UNKNOWN","TEST_ONLY_PROVIDER"),Instant.ofEpochMilli(f.start+at));}
        @Override public void requestSafeStop(String id){stops++;f.jdbc.update("UPDATE vls_compute_billing_session SET safety_state='STOP_REQUESTED' WHERE usage_id=? AND safety_state<>'STOP_CONFIRMED'",id);}
    }
    class LedgerGateway implements ExternalBillingGateway {
        JsonNode page;int ledgerCalls;
        @Override public Result byKey(String key){return f.gateway.byKey(key);}
        @Override public Result byReference(String ref){return f.gateway.byReference(ref);}
        @Override public Result balance(String user){return f.gateway.balance(user);}
        @Override public Result capture(String key,String body){return f.gateway.capture(key,body);}
        @Override public boolean supportsAtomicCapture(){return f.gateway.supportsAtomicCapture();}
        @Override public Result ledger(long from,long to,String cursor){ledgerCalls++;return page==null?Result.of(Status.UNKNOWN):new Result(Status.SUCCEEDED,"",page);}
    }
}
