package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ComputeFundingIntentService;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ExactQuota;
import com.ruoyi.vlstream.test.vlstream.compute.billing.ExternalBillingGateway;
import com.ruoyi.vlstream.test.vlstream.compute.billing.FundingContracts;
import com.ruoyi.vlstream.test.vlstream.compute.billing.NewApiBillingClient;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Imports an already executed release; this path has no funding or power write transport.
 * Only never-running failed starts are supported. Historical conflicts and UNKNOWN survive. */
@Service
public class ComputeOriginalReleaseService {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final ObjectMapper json;
    private final ComputeFundingIntentService journal;
    private final ConfiguredComputeRegistry configured;
    private final ObjectProvider<NewApiBillingClient> clients;
    private final ObjectProvider<ComputePowerProvider> providers;

    public ComputeOriginalReleaseService(JdbcTemplate jdbc, PlatformTransactionManager manager, ObjectMapper json,
            ComputeFundingIntentService journal, ConfiguredComputeRegistry configured,
            ObjectProvider<NewApiBillingClient> clients, ObjectProvider<ComputePowerProvider> providers) {
        this.jdbc=jdbc; this.transactions=new TransactionTemplate(manager); this.json=json;
        this.journal=journal; this.configured=configured; this.clients=clients; this.providers=providers;
    }

    public static class Request {
        public String idempotencyKey, requestBody, requestHash, reason;
        public JsonNode releaseReceipt;
    }

    public String reconcile(String runId, Request request) {
        String administrator=requireAdministrator(), tenant=currentTenant();
        require(tenant!=null && !tenant.trim().isEmpty(), "Trusted tenant required");
        require(request!=null && request.reason!=null && request.reason.matches("[A-Z][A-Z0-9_]{0,63}"), "Audit reason required");
        require(request.idempotencyKey!=null && request.idempotencyKey.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,79}"), "Original release key required");
        require(request.requestBody!=null && request.requestBody.length()<4096
            && ModelArtifactObjectStore.hash(request.requestBody).equals(request.requestHash), "Original release hash mismatch");
        // Copy mutable request input before external I/O and before entering the write transaction.
        Request original=new Request(); original.idempotencyKey=request.idempotencyKey; original.requestBody=request.requestBody;
        original.requestHash=request.requestHash; original.reason=request.reason;
        original.releaseReceipt=request.releaseReceipt==null?null:request.releaseReceipt.deepCopy();
        Map<String,Object> run=run(runId,false);
        require(tenant.equals(text(run,"tenant_id")), "Order tenant mismatch");
        Map<String,Object> hold=jdbc.queryForMap("SELECT * FROM vls_compute_funding_intent WHERE intent_id=?",text(run,"hold_intent_id"));
        validateRequest(run,hold,original);
        if (text(run,"release_intent_id")!=null) return transactions.execute(tx->replay(run(runId,true),original,tenant));
        require(TransactionSynchronizationManager.isActualTransactionActive()==false,"Evidence reads require no transaction");
        long observedAt=System.currentTimeMillis();
        ObjectNode evidence=readEvidence(run,hold);
        return transactions.execute(tx->{
            Map<String,Object> locked=run(runId,true);
            require(tenant.equals(text(locked,"tenant_id")), "Order tenant mismatch");
            if(text(locked,"release_intent_id")!=null)return replay(locked,original,tenant);
            require(observedAt<=System.currentTimeMillis() && System.currentTimeMillis()-observedAt<=30000L,"Evidence expired");
            Map<String,Object> session=jdbc.queryForMap("SELECT * FROM vls_compute_billing_session WHERE usage_id=? FOR UPDATE",text(locked,"usage_id"));
            Map<String,Object> usage=jdbc.queryForMap("SELECT * FROM vls_compute_usage WHERE id=? FOR UPDATE",text(locked,"usage_id"));
            Map<String,Object> parent=jdbc.queryForMap("SELECT * FROM vls_compute_funding_intent WHERE intent_id=? FOR UPDATE",text(locked,"hold_intent_id"));
            validateRequest(locked,parent,original);
            journal.recover(text(parent,"intent_id")); // Validate immutable HOLD even when quarantined.
            for(String field:new String[]{"tenant_id","opening_user_id","merchant_id","payer_user_id"})
                require(Objects.equals(text(locked,field),text(session,field)),"Session ownership mismatch");
            require(tenant.equals(text(usage,"tenant_id")) && Objects.equals(text(locked,"start_operation_id"),text(usage,"start_operation_id")),"Usage ownership mismatch");
            require(("STARTING".equals(text(locked,"state"))||"CANCELLING".equals(text(locked,"state"))||"STOPPING".equals(text(locked,"state")))
                && number(locked,"worker_lease_until_ms")<System.currentTimeMillis(),"Order is not an idle failed start");
            require(usage.get("billing_start_at")==null && usage.get("billing_end_at")==null
                && ("RESERVED".equals(text(usage,"state"))||"RECONCILE".equals(text(usage,"state"))||"START_FAILED".equals(text(usage,"state"))),"Running usage cannot be cancelled");
            require(ComputeFundingIntentService.CONFLICT.equals(text(parent,"state"))
                || ComputeFundingIntentService.DETAIL_OBSERVED.equals(text(parent,"state")),
                "Original hold must be observed or quarantined");
            require(jdbc.queryForList("SELECT merchant_charge_id FROM vls_compute_billing_slice WHERE usage_id=? FOR UPDATE",text(locked,"usage_id")).isEmpty(),"Billing slices require separate reconciliation");
            require(jdbc.queryForList("SELECT refund_intent_id FROM vls_compute_refund_intent WHERE usage_id=? FOR UPDATE",text(locked,"usage_id")).isEmpty(),"Refund history requires separate reconciliation");
            require(jdbc.queryForList("SELECT id FROM vls_compute_usage WHERE server_key=? AND id<>? AND active_server_key IS NOT NULL FOR UPDATE",text(usage,"server_key"),text(usage,"id")).isEmpty(),"Another usage owns this server");
            require(jdbc.queryForList("SELECT id FROM vls_cloud_training_job WHERE (node_id=? OR public_instance_key=?) AND job_state NOT IN ('COMPLETED','FAILED','CANCELLED') FOR UPDATE",locked.get("node_id"),text(locked,"instance_key")).isEmpty(),"Training or archive ownership is active");
            validateEvidence(locked,parent,original,evidence);
            ObjectNode audit=json.createObjectNode();
            audit.put("administrator",administrator); audit.put("tenant_id",tenant); audit.put("run_id",runId);
            audit.put("reason",original.reason); audit.put("observed_at_ms",observedAt);
            audit.set("release_receipt",original.releaseReceipt); audit.set("authority",evidence);
            String id=UUID.randomUUID().toString(), reference="vls-release:"+text(locked,"usage_id")+":final";
            jdbc.update("INSERT INTO vls_compute_funding_intent(intent_id,usage_id,operation_kind,business_reference,merchant_id,payer_user_id,parent_intent_id,idempotency_key,request_path,request_json,request_hash,state,contract_version,hold_id,receipt_json,error_code) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                id,text(locked,"usage_id"),"RELEASE",reference,text(locked,"merchant_id"),text(locked,"payer_user_id"),text(parent,"intent_id"),original.idempotencyKey,
                "/api/v1/external-billing/holds/"+text(parent,"hold_id")+"/release",original.requestBody,original.requestHash,
                ComputeFundingIntentService.EXTERNAL_RELEASE_ADOPTED,ComputeFundingIntentService.ADOPTED_RELEASE_CONTRACT,text(parent,"hold_id"),audit.toString(),"ADMIN_ADOPTED_ORIGINAL_RELEASE");
            jdbc.update("UPDATE vls_compute_paid_run SET release_intent_id=?,state='CANCELLED',active_instance_key=NULL,error_code='ADMIN_ADOPTED_ORIGINAL_RELEASE',update_time=NOW() WHERE run_id=?",id,runId);
            jdbc.update("UPDATE vls_compute_usage SET state='START_FAILED',active_server_key=NULL WHERE id=?",text(locked,"usage_id"));
            // Preserve risk_code, original HOLD observations, start/stop outbox and data claims.
            jdbc.update("UPDATE vls_compute_billing_session SET stop_requested=1 WHERE usage_id=?",text(locked,"usage_id"));
            journal.recover(id);
            return id;
        });
    }

    protected String requireAdministrator() {
        String user=LoginHelper.getUserId();
        require(user!=null && LoginHelper.isAdmin(user),"VLS administrator required"); return user;
    }
    protected String currentTenant(){return TenantContextHolder.getTenantId();}

    /** All evidence comes from authenticated server-side GETs; request-supplied status is insufficient. */
    protected ObjectNode readEvidence(Map<String,Object> run, Map<String,Object> hold) {
        NewApiBillingClient client=clients.getIfAvailable();
        require(client!=null,"Read-only billing client unavailable");
        require(client.servesMerchant(text(run,"merchant_id")),"Billing merchant mismatch");
        ExternalBillingGateway.Result operation=client.holdOperation("RELEASE","vls-release:"+text(run,"usage_id")+":final");
        ExternalBillingGateway.Result detail=client.holdById(text(hold,"hold_id")), wallet=client.balance(text(run,"payer_user_id"));
        require(operation!=null && operation.charge!=null && (operation.status==ExternalBillingGateway.Status.UNKNOWN||operation.status==ExternalBillingGateway.Status.SUCCEEDED)
            && detail!=null && detail.status==ExternalBillingGateway.Status.SUCCEEDED && wallet!=null && wallet.status==ExternalBillingGateway.Status.SUCCEEDED,"Authoritative release evidence unavailable");
        ComputeNode node=configured.resolve(Long.valueOf(text(run,"node_id"))); configured.verifyOfficial(node);
        require(Objects.equals(node.getPublicInstanceKey(),text(run,"instance_key")),"Configured instance mismatch");
        ComputePowerProvider provider=providers.orderedStream().filter(p->p instanceof AutoDlProPowerProvider || p.supports(node)).findFirst().orElseThrow(()->new IllegalStateException("Provider status unavailable"));
        ComputePowerProvider.Snapshot status=provider instanceof AutoDlProPowerProvider
            ? ((AutoDlProPowerProvider)provider).reconciliationStatus(node) : provider.status(node);
        require(ComputeBillingPolicy.computeStopConfirmed(node,status),"Physical shutdown not confirmed");
        ObjectNode result=json.createObjectNode(); result.set("operation",operation.charge);result.set("hold",detail.charge);result.set("wallet",wallet.charge);
        result.put("instance_key",node.getPublicInstanceKey());result.put("provider_instance_id",node.getProviderInstanceId());
        result.put("power",status.power);result.put("stopped_mode",status.stoppedMode);return result;
    }

    private void validateRequest(Map<String,Object> run,Map<String,Object> hold,Request original) {
        String usage=text(run,"usage_id"),reference="vls-release:"+usage+":final";
        require(usage!=null && usage.equals(text(hold,"usage_id")) && "HOLD".equals(text(hold,"operation_kind"))
            && Objects.equals(text(run,"merchant_id"),text(hold,"merchant_id")) && Objects.equals(text(run,"payer_user_id"),text(hold,"payer_user_id")),"Original hold association mismatch");
        JsonNode body=parse(original.requestBody),initial=parse(text(hold,"request_json"));
        require(FundingContracts.release(text(run,"merchant_id"),reference,text(run,"payer_user_id"),text(run,"reserve_quota")).toString().equals(original.requestBody)
            && text(run,"reserve_quota").equals(initial.path("hold_quota").asText()) && ModelArtifactObjectStore.hash(text(hold,"request_json")).equals(text(hold,"request_hash")),"Original release body mismatch");
        require(body.path("merchant_release_id").asText().equals(reference),"Release reference mismatch");
        FundingContracts.ReleaseReceipt receipt=FundingContracts.releaseResult(original.releaseReceipt);
        require(Objects.equals(text(hold,"hold_id"),receipt.holdId) && text(run,"reserve_quota").equals(receipt.releasedQuota)
            && "0".equals(receipt.heldQuota) && "0".equals(receipt.remainingQuota) && "RELEASED".equals(receipt.holdStatus),"Original release receipt mismatch");
    }
    private void validateEvidence(Map<String,Object> run,Map<String,Object> parent,Request original,JsonNode evidence) {
        FundingContracts.HoldDetail hold=FundingContracts.holdDetail(evidence.path("hold"));JsonNode op=evidence.path("operation"),wallet=evidence.path("wallet");
        require("SUCCEEDED".equals(op.path("status").asText()) && original.idempotencyKey.equals(op.path("idempotency_key").asText())
            && hold.holdId.equals(op.path("hold_id").asText()) && hold.userId.equals(op.path("user_id").asText()),"Original operation proof mismatch");
        require(hold.holdId.equals(text(parent,"hold_id")) && hold.userId.equals(text(run,"payer_user_id")) && hold.merchantHoldId.equals(text(parent,"business_reference"))
            && hold.holdQuota.equals(text(run,"reserve_quota")) && hold.expiresAt.equals(parse(text(parent,"request_json")).path("expires_at").asText())
            && "0".equals(hold.capturedQuota) && hold.holdQuota.equals(hold.releasedQuota) && "0".equals(hold.remainingQuota) && "RELEASED".equals(hold.holdStatus),"Released hold proof mismatch");
        require(hold.userId.equals(wallet.path("user_id").asText()) && "0".equals(wallet.path("held_quota").asText())
            && ExactQuota.integer(wallet.path("balance_quota").asText(),false).equals(ExactQuota.integer(wallet.path("available_quota").asText(),false)),"Wallet proof mismatch");
        require(text(run,"instance_key").equals(evidence.path("instance_key").asText()) && "STOPPED".equals(evidence.path("power").asText())
            && "AUTODL_PRO_PHYSICAL_STOPPED".equals(evidence.path("stopped_mode").asText()),"Physical shutdown proof mismatch");
    }
    private String replay(Map<String,Object> run,Request request,String tenant) {
        ComputeFundingIntentService.Intent prior=journal.recover(text(run,"release_intent_id"));
        require(tenant.equals(text(run,"tenant_id")) && "CANCELLED".equals(text(run,"state")) && ComputeFundingIntentService.EXTERNAL_RELEASE_ADOPTED.equals(prior.state)
            && request.idempotencyKey.equals(prior.key) && request.requestHash.equals(prior.requestHash) && request.requestBody.equals(prior.body)
            && request.releaseReceipt.equals(parse(prior.receipt).path("release_receipt")),"Adoption replay conflict");return prior.id;
    }
    private Map<String,Object> run(String id,boolean lock){return jdbc.queryForMap("SELECT * FROM vls_compute_paid_run WHERE run_id=?"+(lock?" FOR UPDATE":""),id);}
    private JsonNode parse(String value){try{return json.readTree(value);}catch(Exception invalid){throw new IllegalArgumentException("Invalid original evidence JSON");}}
    private static String text(Map<String,Object> row,String field){Object value=row.get(field);return value==null?null:String.valueOf(value);}
    private static long number(Map<String,Object> row,String field){return ExactQuota.integer(text(row,field),false).longValueExact();}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
}
