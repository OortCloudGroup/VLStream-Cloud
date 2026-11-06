package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.compute.billing.*;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import java.math.BigInteger;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Administrator-only original-capture correction for the configured DEV acceptance run.
 * No order states are rewritten. Existing journals own every refund/release, including UNKNOWN.
 * The immutable correction reference records the approving administrator; tenant, payer and
 * merchant remain bound to the original run/session. No scheduler calls this service. */
@Service
public class ComputeTrialRefundService {
    private static final String REASON = "DEV_TRIAL_ACCEPTANCE_REFUND";
    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final ObjectMapper json;
    private final ExternalBillingProperties properties;
    private final ComputeBillingTrialGuard trial;
    private final ComputeRefundIntentService refunds;
    private final ComputeFundingIntentService funding;
    private final ObjectProvider<ExternalBillingGateway> gateways;
    private final ConfiguredComputeRegistry configured;
    private final ObjectProvider<ComputePowerProvider> providers;

    public ComputeTrialRefundService(JdbcTemplate jdbc, PlatformTransactionManager manager, ObjectMapper json,
            ExternalBillingProperties properties, ComputeRefundIntentService refunds,
            ComputeFundingIntentService funding,
            ObjectProvider<ExternalBillingGateway> gateways, ConfiguredComputeRegistry configured,
            ObjectProvider<ComputePowerProvider> providers) {
        this.jdbc=jdbc; this.tx=new TransactionTemplate(manager); this.json=json; this.properties=properties;
        this.trial=new ComputeBillingTrialGuard(jdbc,json,properties); this.refunds=refunds; this.funding=funding;
        this.gateways=gateways; this.configured=configured; this.providers=providers;
    }

    /** Review bindings only, never a caller-selected beneficiary, amount or idempotency key. */
    public static class Request {
        public String merchantChargeId, originalRequestHash, expectedHoldId, expectedRefundQuota;
    }
    public static class Charge {
        public String merchantChargeId, originalRequestHash, refundQuota;
    }
    public static class View {
        public String runId, usageId, holdId, merchantId, payerUserId, grossQuota;
        public List<Charge> charges=new ArrayList<>();
    }
    public static class Result {
        public String refundIntentId, refundState, releaseIntentId, releaseState, outcome;
    }
    private static class Context {
        Map<String,Object> run, session, usage;
        List<Map<String,Object>> slices;
        ComputeFundingIntentService.Intent hold;
        BigInteger gross;
    }
    private static class Observation {
        FundingContracts.HoldDetail hold;
        long started;
    }

    public View review(String runId) {
        requireAdministrator(); String tenant=trustedTenant();
        require(properties.isEnabled(),"Billing reads are disabled");
        Context context=tx.execute(s->locked(runId,tenant));
        verifyShutdown(context.run);
        View view=new View(); view.runId=runId; view.usageId=text(context.run,"usage_id");
        view.holdId=context.hold.holdId; view.merchantId=context.hold.merchantId;
        view.payerUserId=context.hold.payerUserId; view.grossQuota=context.gross.toString();
        for(Map<String,Object> slice:context.slices) if("SETTLED".equals(text(slice,"state"))) {
            Charge charge=new Charge(); charge.merchantChargeId=text(slice,"merchant_charge_id");
            charge.originalRequestHash=text(slice,"request_hash"); charge.refundQuota=text(slice,"amount_text");
            view.charges.add(charge);
        }
        return view;
    }

    public Result advance(String runId, Request request) {
        String actor=requireAdministrator(), tenant=trustedTenant();
        require(actor.matches("[A-Za-z0-9_-]{1,64}"),"Auditable administrator identifier required");
        require(properties.isEnabled() && properties.isAllowWrites(),"Paid billing writes are disabled");
        require(!TransactionSynchronizationManager.isActualTransactionActive(),"Cleanup transport requires committed intents");
        require(request!=null,"Reviewed capture required");
        // Do not retain caller-mutable fields across evidence I/O.
        Request expected=new Request(); expected.merchantChargeId=request.merchantChargeId;
        expected.originalRequestHash=request.originalRequestHash; expected.expectedHoldId=request.expectedHoldId;
        expected.expectedRefundQuota=request.expectedRefundQuota;
        Context context=tx.execute(s->locked(runId,tenant));
        Map<String,Object> selected=selected(context,expected);
        long evidenceAt=System.currentTimeMillis();
        verifyShutdown(context.run);
        ExternalBillingGateway gateway=gateways.getIfAvailable(); require(gateway!=null,"Billing gateway unavailable");
        verifyCapture(gateway,context,selected);
        ComputeRefundIntentService.Intent original=existing(context,selected);
        if(original==null) {
            Observation before=observe(gateway,context);
            require("0".equals(before.hold.remainingQuota),"Prior refund release must complete first");
            verifyCounters(context,before.hold);
            original=tx.execute(s->{
                Context current=locked(runId,tenant); Map<String,Object> charge=selected(current,expected);
                requireFresh(evidenceAt);
                ComputeRefundIntentService.Intent prior=existing(current,charge);
                if(prior!=null)return prior;
                // No competing correction may reserve a second charge between the observation and this lock.
                require(noUnfinishedCorrections(current),"Another correction requires recovery first");
                return refunds.prepare(text(charge,"merchant_charge_id"),auditPrefix(runId)+actor,
                    text(charge,"amount_text"),REASON);
            });
        }
        if("PREPARED".equals(original.state)) {
            Observation beforeSend=observe(gateway,context); verifyCounters(context,beforeSend.hold);
            require("0".equals(beforeSend.hold.remainingQuota),"Settled hold evidence required before refund submission");
            refunds.dispatch(original.id,()->validateClaim(runId,tenant,expected,evidenceAt));
        }
        original=refunds.recover(original.id);
        if("UNKNOWN".equals(original.state)) original=refunds.reconcile(original.id);
        Result result=new Result(); result.refundIntentId=original.id; result.refundState=original.state;
        if(!original.needsHoldRequery) { result.outcome="REFUND_RECONCILIATION_REQUIRED"; return result; }
        Observation after=observe(gateway,context);
        verifyCounters(context,after.hold);
        String releaseRef="vls-release:refund:"+ModelArtifactObjectStore.hash(context.hold.id+":"+original.reference);
        List<String> releases=jdbc.queryForList("SELECT intent_id FROM vls_compute_funding_intent WHERE merchant_id=? AND business_reference=?",
            String.class,context.hold.merchantId,releaseRef);
        ComputeFundingIntentService.Intent release=releases.isEmpty()?null:funding.recover(releases.get(0));
        if(release==null && "0".equals(after.hold.remainingQuota)) {
            // A refund may become immediately available. Require the authoritative counters and wallet,
            // not an assumed synthetic release receipt; preserve the original refund evidence unchanged.
            result.outcome="REFUND_AVAILABLE_CONFIRMED"; return result;
        }
        if(release==null) {
            require(after.hold.remainingQuota.equals(expected.expectedRefundQuota),"Restored hold amount differs from this correction");
            if(after.started<=original.receiptObservedAtMs) {result.outcome="HOLD_REQUERY_REQUIRED";return result;}
            final String refundReference=original.reference;
            release=tx.execute(s->{locked(runId,tenant);return funding.prepareRefundRelease(context.hold.id,refundReference);});
        }
        require("RELEASE".equals(release.operation) && context.hold.id.equals(release.parentId)
            && expected.expectedRefundQuota.equals(parse(release.body).path("release_quota").asText()),"Correction release mismatch");
        // Never replay UNKNOWN through the general recovery coordinator. Without the original POST
        // receipt this entry remains pending, even if aggregate counters have already reached zero.
        if(ComputeFundingIntentService.PREPARED.equals(release.state)) release=sendReleaseOnce(gateway,context,release,tenant,expected,evidenceAt);
        if(ComputeFundingIntentService.RECEIPT_UNVERIFIED.equals(release.state)) {
            Observation confirmation=observe(gateway,context);
            release=funding.confirmOperation(release.id,confirmation.started);
        }
        result.releaseIntentId=release.id; result.releaseState=release.state;
        if(!ComputeFundingIntentService.OPERATION_CONFIRMED.equals(release.state)) {
            result.outcome="RELEASE_RECONCILIATION_REQUIRED"; return result;
        }
        Observation completed=observe(gateway,context); verifyCounters(context,completed.hold);
        require("0".equals(completed.hold.remainingQuota),"Refund hold release is incomplete");
        result.outcome="REFUND_RELEASE_CONFIRMED"; return result;
    }

    private ComputeFundingIntentService.Intent sendReleaseOnce(ExternalBillingGateway gateway,Context context,
            ComputeFundingIntentService.Intent prepared,String tenant,Request expected,long evidenceAt) {
        // The database claim, not an earlier Java state read, decides whether any POST is allowed.
        // A concurrent sender or process crash leaves UNKNOWN, for which claimInitialSend returns null.
        ComputeFundingIntentService.Intent claim=tx.execute(s->{
            validateClaim(text(context.run,"run_id"),tenant,expected,evidenceAt);
            return funding.claimInitialSend(prepared.id,System.currentTimeMillis());
        });
        if(claim==null)return funding.recover(prepared.id);
        try {
            ExternalBillingGateway.Result result=gateway.releaseHold(context.hold.holdId,claim.key,claim.body);
            if(result!=null && result.status==ExternalBillingGateway.Status.SUCCEEDED && result.charge!=null)
                return funding.recordReceipt(claim.id,claim.requestHash,result.charge);
            return funding.recordFailure(claim.id,claim.requestHash,result==null
                ? ExternalBillingGateway.Result.of(ExternalBillingGateway.Status.UNKNOWN):result);
        } catch(RuntimeException uncertain) {return funding.recordUnknown(claim.id,claim.requestHash);}
    }

    private void validateClaim(String runId,String tenant,Request expected,long evidenceAt) {
        selected(locked(runId,tenant),expected);
        // A lock wait is part of evidence age. Recheck after all eligibility locks are held.
        requireFresh(evidenceAt);
    }
    private static void requireFresh(long observed) {
        long now=System.currentTimeMillis();
        require(observed<=now && now-observed<=30000L,"Cleanup evidence expired");
    }

    private Context locked(String runId,String tenant) {
        require(runId!=null && runId.matches("[0-9a-f-]{36}"),"Run identifier required");
        Map<String,Object> observed=jdbc.queryForMap("SELECT usage_id,tenant_id FROM vls_compute_paid_run WHERE run_id=?",runId);
        require(tenant.equals(text(observed,"tenant_id")),"Order tenant mismatch");
        String usage=text(observed,"usage_id");require(usage!=null,"Completed usage required");
        // Match paid-run stop/lifecycle: session before run, then usage and slices. Read the
        // association without a lock first, and verify it again after both locks are acquired.
        Context c=new Context();
        c.session=jdbc.queryForMap("SELECT * FROM vls_compute_billing_session WHERE usage_id=? FOR UPDATE",usage);
        c.run=jdbc.queryForMap("SELECT * FROM vls_compute_paid_run WHERE run_id=? FOR UPDATE",runId);
        require(usage.equals(text(c.run,"usage_id")),"Order usage association changed");
        require(tenant.equals(text(c.run,"tenant_id")),"Order tenant mismatch");
        require("COMPLETED".equals(text(c.run,"state")) && c.run.get("active_instance_key")==null
            && number(c.run,"worker_lease_until_ms")<System.currentTimeMillis(),"Only an idle completed paid run can be corrected");
        c.usage=jdbc.queryForMap("SELECT * FROM vls_compute_usage WHERE id=? FOR UPDATE",usage);
        for(String field:new String[]{"tenant_id","opening_user_id","merchant_id","payer_user_id"})
            require(Objects.equals(text(c.run,field),text(c.session,field)),"Session ownership mismatch");
        require(tenant.equals(text(c.usage,"tenant_id")) && Objects.equals(text(c.run,"start_operation_id"),text(c.usage,"start_operation_id")),"Usage ownership mismatch");
        require("STOP_CONFIRMED".equals(text(c.session,"safety_state"))
            && c.usage.get("billing_start_at")!=null && c.usage.get("billing_end_at")!=null && c.usage.get("active_server_key")==null
            && number(c.session,"metered_until_ms")>=number(c.session,"last_running_ms"),"Usage is not safely stopped and settled");
        JsonNode snapshot=parse(text(c.session,"accepted_snapshot"));
        require(snapshot.has(ComputeBillingTrialGuard.MARKER) && ComputeBillingTrialGuard.TARGET.equals(properties.getBaseUrl()),"Only the original DEV acceptance trial is eligible");
        trial.requireCleanupScope(usage);
        require(jdbc.queryForList("SELECT id FROM vls_compute_usage WHERE server_key=? AND active_server_key IS NOT NULL FOR UPDATE",text(c.usage,"server_key")).isEmpty(),"Another usage owns this instance");
        require(jdbc.queryForList("SELECT id FROM vls_cloud_training_job WHERE (node_id=? OR public_instance_key=?) AND job_state NOT IN ('COMPLETED','FAILED','CANCELLED') FOR UPDATE",c.run.get("node_id"),text(c.run,"instance_key")).isEmpty(),"Training or archive work is active");
        c.hold=funding.recover(text(c.run,"hold_intent_id"));
        require("HOLD".equals(c.hold.operation) && ComputeFundingIntentService.DETAIL_OBSERVED.equals(c.hold.state)
            && usage.equals(c.hold.usageId) && properties.getMerchantId().equals(c.hold.merchantId)
            && text(c.run,"merchant_id").equals(c.hold.merchantId) && text(c.run,"payer_user_id").equals(c.hold.payerUserId)
            && text(c.run,"reserve_quota").equals(parse(c.hold.body).path("hold_quota").asText()),"Original hold ownership mismatch");
        if(c.run.get("release_intent_id")!=null) {
            ComputeFundingIntentService.Intent finalRelease=funding.recover(text(c.run,"release_intent_id"));
            require(c.hold.id.equals(finalRelease.parentId) && "RELEASE".equals(finalRelease.operation)
                && ComputeFundingIntentService.OPERATION_CONFIRMED.equals(finalRelease.state),"Original final release is unresolved");
        }
        c.slices=jdbc.queryForList("SELECT * FROM vls_compute_billing_slice WHERE usage_id=? ORDER BY slice_sequence FOR UPDATE",usage);
        c.gross=BigInteger.ZERO;
        BigInteger unallocatedBudget=BigInteger.ZERO;
        boolean unallocatedPermit=false;
        for(Map<String,Object> slice:c.slices) {
            require(number(slice,"lease_until_ms")==0 && slice.get("lease_token")==null,"Capture worker is active");
            if("NO_DEBIT".equals(text(slice,"state"))) {
                require("0".equals(text(slice,"amount_text")) && slice.get("charge_id")==null && slice.get("ledger_entry_id")==null,"Invalid zero-debit slice");
                continue;
            }
            // This is exactly the never-sent predicate already accepted by paid-run completion.
            // attempt_count counts dispatcher calls (including GET-only checks), not POSTs.
            // is_tail is also not required: several sealed intervals may remain unallocated.
            boolean unallocated="BLOCKED".equals(text(slice,"state"))
                && ("TRIAL_UNALLOCATED_BUDGET".equals(text(slice,"error_code")) || "TRIAL_UNALLOCATED_PERMIT".equals(text(slice,"error_code")))
                && slice.get("charge_id")==null && slice.get("ledger_entry_id")==null && slice.get("lease_token")==null
                && slice.get("lease_until_ms")!=null && number(slice,"lease_until_ms")==0;
            require("SETTLED".equals(text(slice,"state")) || unallocated,"Every original capture must be settled or provably unallocated");
            JsonNode body=parse(text(slice,"request_json"));
            require(ModelArtifactObjectStore.hash(text(slice,"request_json")).equals(text(slice,"request_hash"))
                && usage.equals(body.path("usage_session_id").asText()) && c.hold.holdId.equals(body.path("hold_id").asText())
                && c.hold.merchantId.equals(body.path("merchant_id").asText()) && c.hold.payerUserId.equals(body.path("user_id").asText())
                && text(slice,"merchant_charge_id").equals(body.path("merchant_charge_id").asText())
                && text(slice,"amount_text").equals(body.path("charge_quota").asText()),"Original capture binding mismatch");
            BigInteger amount=ExactQuota.integer(text(slice,"amount_text"),true);
            if(unallocated) {
                // A later accepted ledger/refund observation contradicts the never-sent marker.
                // Do not invent a refund, erase evidence or silently treat that slice as zero.
                require(jdbc.queryForList("SELECT ledger_entry_id FROM vls_compute_billing_ledger_seen WHERE merchant_ref=? FOR UPDATE",text(slice,"merchant_charge_id")).isEmpty()
                    && jdbc.queryForList("SELECT refund_intent_id FROM vls_compute_refund_intent WHERE merchant_charge_id=? FOR UPDATE",text(slice,"merchant_charge_id")).isEmpty(),
                    "Unallocated slice has accepted financial evidence");
                if("TRIAL_UNALLOCATED_BUDGET".equals(text(slice,"error_code")))unallocatedBudget=unallocatedBudget.add(amount);
                else unallocatedPermit=true;
                continue;
            }
            ExactQuota.integer(text(slice,"charge_id"),true); ExactQuota.integer(text(slice,"ledger_entry_id"),true);
            c.gross=c.gross.add(amount);
        }
        BigInteger cap=ExactQuota.integer(text(c.run,"reserve_quota"),true)
            .min(ExactQuota.integer(snapshot.path(ComputeBillingTrialGuard.MARKER).path("maximum_quota").asText(),true));
        require(c.gross.signum()>0 && c.gross.compareTo(cap)<=0,"Trial gross capture exceeds its authorized cap");
        String risk=text(c.session,"risk_code");
        boolean holdExpired=ExactQuota.integer(parse(c.hold.body).path("expires_at").asText(),true)
            .compareTo(BigInteger.valueOf(System.currentTimeMillis()))<=0;
        require(risk==null
            || "TRIAL_GROSS_CAP_EXCEEDED".equals(risk) && unallocatedBudget.signum()>0 && c.gross.add(unallocatedBudget).compareTo(cap)>0
            || "HOLD_EXPIRED".equals(risk) && unallocatedPermit && holdExpired
            || "TRIAL_PERMIT_EXPIRED".equals(risk) && unallocatedPermit
                && Instant.parse(snapshot.path(ComputeBillingTrialGuard.MARKER).path("expires_at").asText()).toEpochMilli()<=System.currentTimeMillis()
            || "HOLD_EXPIRING".equals(risk) && (unallocatedBudget.signum()>0 || unallocatedPermit) && holdExpired
            || "RUN_WINDOW_EXPIRED".equals(risk) && (unallocatedBudget.signum()>0 || unallocatedPermit) && originalRunWindowExpired(c),
            "Unresolved session risk requires separate reconciliation");
        return c;
    }

    private boolean originalRunWindowExpired(Context c) {
        JsonNode budget=parse(text(c.run,"quote_snapshot")).path("run_budget");
        JsonNode stop=budget.path("stop_budget_seconds"),poll=budget.path("polling_safety_seconds");
        require(stop.isIntegralNumber() && stop.canConvertToInt() && stop.asInt()>0
            && poll.isIntegralNumber() && poll.canConvertToInt() && poll.asInt()>0,"Original accepted stop budget required");
        long until=ExactQuota.integer(text(c.run,"hold_until_ms"),true).longValueExact();
        require(until==ExactQuota.integer(parse(c.hold.body).path("expires_at").asText(),true).longValueExact(),"Original run and hold expiry mismatch");
        long deadline=Math.subtractExact(until,Math.multiplyExact(Math.addExact(stop.longValue(),poll.longValue()),1000L));
        return deadline>0 && deadline<=System.currentTimeMillis();
    }

    private Map<String,Object> selected(Context c,Request expected) {
        require(c.hold.holdId.equals(expected.expectedHoldId),"Reviewed hold mismatch");
        for(Map<String,Object> row:c.slices) if(Objects.equals(expected.merchantChargeId,text(row,"merchant_charge_id"))) {
            require("SETTLED".equals(text(row,"state")) && Objects.equals(expected.originalRequestHash,text(row,"request_hash"))
                && Objects.equals(expected.expectedRefundQuota,text(row,"amount_text")),"Reviewed original capture mismatch");
            return row;
        }
        throw new IllegalStateException("Capture does not belong to this run");
    }
    private ComputeRefundIntentService.Intent existing(Context c,Map<String,Object> charge) {
        List<String> ids=jdbc.queryForList("SELECT refund_intent_id FROM vls_compute_refund_intent WHERE merchant_charge_id=?",String.class,text(charge,"merchant_charge_id"));
        require(ids.size()<=1,"Multiple corrections require separate reconciliation");
        if(ids.isEmpty())return null;
        ComputeRefundIntentService.Intent intent=refunds.recover(ids.get(0));
        require(intent.correctionReference.startsWith(auditPrefix(text(c.run,"run_id")))
            && intent.correctionReference.substring(auditPrefix(text(c.run,"run_id")).length()).matches("[A-Za-z0-9_-]{1,64}")
            && REASON.equals(parse(intent.body).path("reason_code").asText())
            && text(charge,"amount_text").equals(parse(intent.body).path("refund_quota").asText()),"Another correction owns the original capture");
        return intent;
    }
    private boolean noUnfinishedCorrections(Context c) {
        for(Map<String,Object> charge:c.slices) {
            if(!"SETTLED".equals(text(charge,"state")))continue;
            ComputeRefundIntentService.Intent intent=existing(c,charge); if(intent==null)continue;
            if(!intent.needsHoldRequery)return false;
            String reference="vls-release:refund:"+ModelArtifactObjectStore.hash(c.hold.id+":"+intent.reference);
            for(String id:jdbc.queryForList("SELECT intent_id FROM vls_compute_funding_intent WHERE merchant_id=? AND business_reference=?",String.class,c.hold.merchantId,reference))
                if(!ComputeFundingIntentService.OPERATION_CONFIRMED.equals(funding.recover(id).state))return false;
        }
        return true;
    }
    private void verifyCapture(ExternalBillingGateway gateway,Context c,Map<String,Object> slice) {
        JsonNode detail=success(gateway.byReference(text(slice,"merchant_charge_id"))), operation=success(gateway.byKey(text(slice,"idempotency_key")));
        JsonNode body=parse(text(slice,"request_json"));
        require("SUCCEEDED".equals(detail.path("status").asText()) && "SUCCEEDED".equals(operation.path("status").asText())
            && "CHARGE".equals(operation.path("operation_type").asText())
            && text(slice,"idempotency_key").equals(operation.path("idempotency_key").asText()),"Original capture authority unavailable");
        for(String field:new String[]{"merchant_id","merchant_charge_id","user_id","usage_session_id","hold_id"}) {
            require(body.path(field).asText().equals(operation.path(field).asText()),"Original capture operation mismatch");
            if(detail.has(field))require(body.path(field).asText().equals(detail.path(field).asText()),"Original capture detail mismatch");
        }
        for(String field:new String[]{"charge_id","ledger_entry_id"})
            require(text(slice,field).equals(detail.path(field).asText()) && text(slice,field).equals(operation.path(field).asText()),"Original capture receipt mismatch");
        require(text(slice,"amount_text").equals(detail.path("charged_quota").asText())
            && text(slice,"amount_text").equals(operation.path("charged_quota").asText()),"Original capture amount mismatch");
    }
    private Observation observe(ExternalBillingGateway gateway,Context c) {
        Observation o=new Observation(); o.started=System.currentTimeMillis(); JsonNode raw=success(gateway.holdById(c.hold.holdId));
        o.hold=FundingContracts.holdDetail(raw);
        ComputeFundingIntentService.Intent observed=funding.recordHoldDetail(c.hold.id,c.hold.requestHash,raw,o.started);
        require(ComputeFundingIntentService.DETAIL_OBSERVED.equals(observed.state),"Original hold observation conflict");
        JsonNode wallet=success(gateway.balance(c.hold.payerUserId));
        require(c.hold.payerUserId.equals(wallet.path("user_id").asText())
            && o.hold.remainingQuota.equals(wallet.path("held_quota").asText()),"Wallet held balance mismatch");
        BigInteger balance=ExactQuota.integer(wallet.path("balance_quota").asText(),false);
        require(balance.subtract(ExactQuota.integer(wallet.path("held_quota").asText(),false))
            .equals(ExactQuota.integer(wallet.path("available_quota").asText(),false)),"Wallet balance equation mismatch");
        return o;
    }
    private void verifyCounters(Context c,FundingContracts.HoldDetail hold) {
        BigInteger refunded=BigInteger.ZERO;
        for(Map<String,Object> charge:c.slices) {
            if(!"SETTLED".equals(text(charge,"state")))continue;
            ComputeRefundIntentService.Intent prior=existing(c,charge);
            if(prior!=null && prior.needsHoldRequery)refunded=refunded.add(ExactQuota.integer(parse(prior.body).path("refund_quota").asText(),true));
        }
        require(text(c.run,"reserve_quota").equals(hold.holdQuota)
            && c.gross.subtract(refunded).equals(ExactQuota.integer(hold.capturedQuota,false)),"Authoritative capture/refund counters mismatch");
    }
    protected String requireAdministrator() {
        String user=LoginHelper.getUserId(); require(user!=null && LoginHelper.isAdmin(user),"VLS administrator required"); return user;
    }
    protected String currentTenant(){return TenantContextHolder.getTenantId();}
    protected void verifyShutdown(Map<String,Object> run) {
        ComputeNode node=configured.resolve(Long.valueOf(text(run,"node_id"))); configured.verifyOfficial(node);
        require(Objects.equals(node.getPublicInstanceKey(),text(run,"instance_key")),"Configured instance mismatch");
        ComputePowerProvider provider=providers.orderedStream().filter(p->p instanceof AutoDlProPowerProvider || p.supports(node))
            .findFirst().orElseThrow(()->new IllegalStateException("Provider status unavailable"));
        ComputePowerProvider.Snapshot status=provider instanceof AutoDlProPowerProvider
            ? ((AutoDlProPowerProvider)provider).reconciliationStatus(node) : provider.status(node);
        require(ComputeBillingPolicy.computeStopConfirmed(node,status),"Physical shutdown not confirmed");
    }
    private String trustedTenant(){String tenant=currentTenant();require(tenant!=null && !tenant.trim().isEmpty(),"Trusted tenant required");return tenant;}
    private static String auditPrefix(String run){return "trial:"+run+":admin:";}
    private static JsonNode success(ExternalBillingGateway.Result result){require(result!=null && result.status==ExternalBillingGateway.Status.SUCCEEDED && result.charge!=null,"Authoritative billing evidence unavailable");return result.charge;}
    private JsonNode parse(String value){try{return json.readTree(value);}catch(Exception e){throw new IllegalStateException("Invalid persisted billing evidence");}}
    private static String text(Map<String,Object> row,String name){Object value=row.get(name);return value==null?null:String.valueOf(value);}
    private static long number(Map<String,Object> row,String name){Object value=row.get(name);return value==null?0:((Number)value).longValue();}
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
}
