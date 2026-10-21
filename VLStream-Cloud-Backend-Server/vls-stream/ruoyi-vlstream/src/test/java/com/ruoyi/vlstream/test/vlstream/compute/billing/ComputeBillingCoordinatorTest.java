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
    @Test void periodicSealingAndRestartNeverCreateDuplicateSlicesOrKeys() throws Exception {
        String id=usage();lifecycle.at=90000;coordinator.tick(id,lifecycle);
        assertEquals(1,f.service.audit(id).size());Map<String,Object> first=f.service.audit(id).get(0);
        due();coordinator=new ComputeBillingCoordinator(f.jdbc,f.service(f.jdbc),f.properties,beans.getBeanProvider(BillingLifecycleGateway.class),reconciler());coordinator.tick(id,lifecycle);
        assertEquals(1,f.service.audit(id).size());assertEquals(first.get("merchant_charge_id"),f.service.audit(id).get(0).get("merchant_charge_id"));
        assertEquals(1,f.gateway.captures.size());assertEquals("SETTLED",f.service.audit(id).get(0).get("state"));
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
