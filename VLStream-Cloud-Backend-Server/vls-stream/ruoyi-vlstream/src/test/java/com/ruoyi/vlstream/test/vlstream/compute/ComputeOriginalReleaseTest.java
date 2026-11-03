package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.vlstream.test.vlstream.compute.billing.*;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import java.nio.file.Paths;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ScriptUtils;
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
    int reads;
    DataSourceTransactionManager manager;
    DefaultListableBeanFactory beans;

    @BeforeEach void setup() throws Exception {
        fixture=new ConfiguredOfficialRunIntegrationTest();fixture.setup();f=fixture.f;
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
        service=new ComputeOriginalReleaseService(f.jdbc,manager,f.json,journal,fixture.registry,beans.getBeanProvider(NewApiBillingClient.class),beans.getBeanProvider(ComputePowerProvider.class)) {
            @Override protected String requireAdministrator(){return "1";}
            @Override protected String currentTenant(){return "tenant-test";}
            @Override protected ObjectNode readEvidence(Map<String,Object> r,Map<String,Object> h){reads++;assertFalse(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive());return evidence.deepCopy();}
        };
    }
    @AfterEach void clear(){fixture.cleanup();}

    @Test void adoptsExactOriginalReleaseAndPreservesUnknownConflictDataOwnershipAndAudit() {
        int holdPosts=f.wallet.holdPosts;
        String adopted=service.reconcile(run,request);
        assertEquals("CANCELLED",value("vls_compute_paid_run","state","run_id",run));
        assertNull(value("vls_compute_paid_run","active_instance_key","run_id",run));
        assertEquals("START_FAILED",value("vls_compute_usage","state","id",usage));
        assertNull(value("vls_compute_usage","active_server_key","id",usage));
        assertEquals("UNKNOWN",value("vls_compute_paid_run","power_start_state","run_id",run));
        assertEquals("CONFLICT",journal.recover(parent).state);
        assertEquals("{}",journal.recover(parent).conflictingReceipt);
        assertEquals("HOLD_VERSION_CONFLICT",value("vls_compute_billing_session","risk_code","usage_id",usage));
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
    @Test void identicalReplayIsIdempotentAndChangedKeyCannotReplaceIt() {
        String first=service.reconcile(run,request);assertEquals(first,service.reconcile(run,request));assertEquals(1,reads);
        request.idempotencyKey="changed-key";assertThrows(IllegalStateException.class,()->service.reconcile(run,request));
        assertEquals(1,f.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_funding_intent WHERE operation_kind='RELEASE'",Integer.class));
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
    @ParameterizedTest @ValueSource(strings={"key","hash","body","amount","hold","tenant","payer","lease","running","slice","job"})
    void rejectsMismatchedOrActiveOriginalWithoutWrites(String change) throws Exception {
        switch(change) {
            case "key":request.idempotencyKey="another-key";break;
            case "hash":request.requestHash="bad";break;
            case "body":request.requestBody=request.requestBody.replace("vls-test","other");request.requestHash=ModelArtifactObjectStore.hash(request.requestBody);break;
            case "amount":((ObjectNode)request.releaseReceipt).put("released_quota","1");break;
            case "hold":((ObjectNode)request.releaseReceipt).put("hold_id","999");break;
            case "tenant":f.jdbc.update("UPDATE vls_compute_paid_run SET tenant_id='other' WHERE run_id=?",run);break;
            case "payer":f.jdbc.update("UPDATE vls_compute_billing_session SET payer_user_id='99' WHERE usage_id=?",usage);break;
            case "lease":f.jdbc.update("UPDATE vls_compute_paid_run SET worker_lease_until_ms=? WHERE run_id=?",System.currentTimeMillis()+60000L,run);break;
            case "running":f.jdbc.update("UPDATE vls_compute_usage SET billing_start_at=NOW(),state='RUNNING' WHERE id=?",usage);break;
            case "slice":f.jdbc.update("INSERT INTO vls_compute_billing_slice(merchant_charge_id,usage_id,slice_sequence,interval_start_ms,interval_end_ms,idempotency_key,request_json,request_hash,amount_text,remainder_numerator,remainder_denominator,state) VALUES('s',?,0,1,2,'k','{}','h','1','0','1','UNKNOWN')",usage);break;
            case "job":f.jdbc.update("INSERT INTO vls_cloud_training_job(id,node_id,job_state) VALUES('running',-6000,'RUNNING')");break;
            default:fail("Unknown case");
        }
        assertThrows(RuntimeException.class,()->service.reconcile(run,request));assertUnchanged();
    }
    @ParameterizedTest @ValueSource(strings={"status","key","user","hold","reference","remaining","captured","released","wallet","power","instance"})
    void rejectsNonterminalOrUnboundAuthorityEvidence(String field) {
        switch(field) {
            case "status":((ObjectNode)evidence.path("operation")).put("status","PROCESSING");break;
            case "key":((ObjectNode)evidence.path("operation")).put("idempotency_key","other");break;
            case "user":((ObjectNode)evidence.path("operation")).put("user_id","999");break;
            case "hold":((ObjectNode)evidence.path("operation")).put("hold_id","999");break;
            case "reference":((ObjectNode)evidence.path("hold")).put("merchant_hold_id","other");break;
            case "remaining":((ObjectNode)evidence.path("hold")).put("remaining_quota","1");break;
            case "captured":((ObjectNode)evidence.path("hold")).put("captured_quota","1");break;
            case "released":((ObjectNode)evidence.path("hold")).put("released_quota","1");break;
            case "wallet":((ObjectNode)evidence.path("wallet")).put("held_quota","1");break;
            case "power":evidence.put("power","RUNNING");break;
            case "instance":evidence.put("instance_key","other");break;
            default:fail("Unknown field");
        }
        assertThrows(RuntimeException.class,()->service.reconcile(run,request));assertUnchanged();
    }
    @Test void offModeCannotUseRequestReceiptInsteadOfFreshAuthenticatedReads() {
        ComputeOriginalReleaseService noClient=new ComputeOriginalReleaseService(f.jdbc,manager,f.json,journal,fixture.registry,beans.getBeanProvider(NewApiBillingClient.class),beans.getBeanProvider(ComputePowerProvider.class)) {
            @Override protected String requireAdministrator(){return "1";}
            @Override protected String currentTenant(){return "tenant-test";}
        };
        assertEquals("Read-only billing client unavailable",assertThrows(IllegalStateException.class,()->noClient.reconcile(run,request)).getMessage());assertUnchanged();
    }
    @Test void realReaderUsesOnlyBoundMerchantGetsAndFreshPhysicalStatus() {
        clearInvocations(f.supplier); // Exclude the quote preflight from this operation's reads.
        NewApiBillingClient client=mock(NewApiBillingClient.class);beans.registerSingleton("billing",client);beans.registerSingleton("power",f.supplier);
        when(client.servesMerchant("vls-test")).thenReturn(true);
        when(client.holdOperation(eq("RELEASE"),anyString())).thenReturn(new ExternalBillingGateway.Result(ExternalBillingGateway.Status.UNKNOWN,"HOLD_OPERATION_RESPONSE_UNSPECIFIED",evidence.path("operation")));
        when(client.holdById(anyString())).thenReturn(new ExternalBillingGateway.Result(ExternalBillingGateway.Status.SUCCEEDED,"",evidence.path("hold")));
        when(client.balance(anyString())).thenReturn(new ExternalBillingGateway.Result(ExternalBillingGateway.Status.SUCCEEDED,"",evidence.path("wallet")));
        ComputeOriginalReleaseService actual=new ComputeOriginalReleaseService(f.jdbc,manager,f.json,journal,fixture.registry,beans.getBeanProvider(NewApiBillingClient.class),beans.getBeanProvider(ComputePowerProvider.class)) {
            @Override protected String requireAdministrator(){return "1";}
            @Override protected String currentTenant(){return "tenant-test";}
        };
        assertNotNull(actual.reconcile(run,request));
        verify(client).servesMerchant("vls-test");verify(client).holdOperation("RELEASE","vls-release:"+usage+":final");verify(client).holdById(evidence.path("hold").path("hold_id").asText());verify(client).balance("13");verifyNoMoreInteractions(client);
        verify(f.supplier).status(any());verify(f.supplier,never()).submit(any(),anyString(),anyString());
    }
    void assertUnchanged(){assertEquals("STARTING",value("vls_compute_paid_run","state","run_id",run));assertEquals(0,f.jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_funding_intent WHERE operation_kind='RELEASE'",Integer.class));assertEquals("CONFLICT",value("vls_compute_funding_intent","state","intent_id",parent));}
    String value(String table,String field,String key,String id){return f.jdbc.queryForObject("SELECT "+field+" FROM "+table+" WHERE "+key+"=?",String.class,id);}
}
