package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Durable VLS-side slice/outbox coordinator.
 * All external I/O follows a committed local intent, preserving its original funding association. */
@Service
public class ComputeBillingSettlementService {
    public static final String EXPIRED_UNSENT_TAIL = "HOLD_EXPIRED_NEVER_DEBITED";
    public static final String RELEASED_RUN_WINDOW_TAIL = "RUN_WINDOW_EXPIRED_RELEASED_UNCHARGED";
    public static final String RELEASED_SSH_PREPARATION_TAIL = "SSH_PREPARATION_FAILED_RELEASED_UNCHARGED";
    private static final long TAIL_CLOCK_SKEW_MS = 2000L;
    private static final long TAIL_BIRTH_MARGIN_MS = 5000L;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final ObjectMapper json;
    private final ExternalBillingProperties properties;
    private final Supplier<ExternalBillingGateway> gateways;
    private final ComputeBillingTrialGuard trial;
    @Autowired
    private com.ruoyi.vlstream.test.vlstream.compute.ComputeBillingPolicy policy = new com.ruoyi.vlstream.test.vlstream.compute.ComputeBillingPolicy();

    @Autowired
    public ComputeBillingSettlementService(JdbcTemplate jdbc, PlatformTransactionManager manager, ObjectMapper json,
                                           ExternalBillingProperties properties, ObjectProvider<ExternalBillingGateway> gateways) {
        this(jdbc, manager, json, properties, gateways::getIfAvailable);
    }
    public ComputeBillingSettlementService(JdbcTemplate jdbc, PlatformTransactionManager manager, ObjectMapper json,
                                           ExternalBillingProperties properties, Supplier<ExternalBillingGateway> gateways) {
        this.jdbc = jdbc; this.transactions = new TransactionTemplate(manager); this.json = json;
        this.properties = properties; this.gateways = gateways;
        this.trial = new ComputeBillingTrialGuard(jdbc, json, properties);
    }

    /** Bind only an existing, trusted, started usage. Does not create a grant, hold, wallet or cloud operation. */
    public void bind(String usageId, BillingPayer payer, BillingSnapshot snapshot) {
        bind(usageId,payer,snapshot,false);
    }

    /** Pre-start binding for a committed RESERVED usage; no fabricated running timestamp. */
    public void bindReserved(String usageId, BillingPayer payer, BillingSnapshot snapshot) {
        if(!properties.paidConfigured())throw new IllegalStateException(properties.unavailableReason());
        bind(usageId,payer,snapshot,true);
    }

    private void bind(String usageId, BillingPayer payer, BillingSnapshot snapshot, boolean reserved) {
        if(!BillingLedgerOrigin.matches(snapshot.json,properties.getMerchantId(),properties))
            throw new IllegalArgumentException("BILLING_ORIGIN_MISMATCH");
        transactions.execute(tx -> {
            Map<String, Object> usage = usage(usageId);
            if (!payer.actorId.equals(usage.get("opening_user_id")) || !payer.tenantId.equals(usage.get("tenant_id"))
                || !payer.platformSubject.equals(usage.get("payer_account_ref"))) throw new IllegalArgumentException("Usage payer binding mismatch");
            if (usage.get("billing_start_at") == null && !(reserved&&"RESERVED".equals(usage.get("state")))) throw new IllegalArgumentException("Confirmed running or reserved usage required");
            try {
                if (!json.readTree(String.valueOf(usage.get("rate_snapshot"))).equals(json.readTree(snapshot.json).path("accepted_rate")))
                    throw new IllegalArgumentException("Accepted rate differs from usage snapshot");
            } catch (java.io.IOException e) { throw new IllegalArgumentException("Invalid usage rate snapshot"); }
            String merchant = properties.getMerchantId();
            if (merchant == null || !merchant.matches("[A-Za-z0-9_-]{1,64}")) throw new IllegalArgumentException("Billing merchant required");
            List<Map<String, Object>> prior = jdbc.queryForList("SELECT * FROM vls_compute_billing_session WHERE usage_id=? FOR UPDATE", usageId);
            if (!prior.isEmpty()) {
                Map<String, Object> row = prior.get(0);
                if (!payer.userId.equals(row.get("payer_user_id")) || !snapshot.json.equals(row.get("accepted_snapshot")) || !merchant.equals(row.get("merchant_id")))
                    throw new IllegalArgumentException("Immutable session binding conflict");
                return null;
            }
            long start = usage.get("billing_start_at")==null?0:((Timestamp) usage.get("billing_start_at")).getTime();
            if(start!=0)snapshot.requireInterval(start, Math.addExact(start, 1));
            // A legacy whole-period charge must never coexist with slices for the same usage.
            if (!jdbc.queryForList("SELECT charge_key FROM vls_compute_charge WHERE usage_id=?", usageId).isEmpty())
                throw new IllegalArgumentException("Legacy intent requires reconciliation before slice billing");
            jdbc.update("INSERT INTO vls_compute_billing_session(usage_id,tenant_id,opening_user_id,merchant_id,payer_user_id,instance_ref,quota_unit,accepted_snapshot,metered_until_ms,last_running_ms) VALUES(?,?,?,?,?,?,?,?,?,?)",
                usageId, payer.tenantId, payer.actorId, merchant, payer.userId, usage.get("server_key"), snapshot.quotaUnit, snapshot.json, start, start);
            jdbc.update("UPDATE vls_compute_usage SET billing_mode='SLICES' WHERE id=?", usageId);
            return null;
        });
    }
    /** Application binding entry: no client-supplied payer, price, amount or conversion. */
    public void bindAcceptedUsage(String usageId, BillingPayer payer, JsonNode authoritativeConfig) {
        requireBoundOrigin(usageId);
        String accepted = jdbc.queryForObject("SELECT accepted_snapshot FROM vls_compute_billing_session WHERE usage_id=?", String.class, usageId);
        bind(usageId, payer, BillingSnapshot.read(accepted, json));
    }

    /** Missing historical origin is never inferred from a newly configured endpoint. */
    public void requireBoundOrigin(String usageId) { BillingLedgerOrigin.requireUsage(jdbc, properties, usageId); }

    /** A control-plane SSH failure does not invalidate already confirmed runtime.
     * Only the original, unattempted final slice can regain dispatch under its still-valid hold. */
    public boolean recoverStoppedSshPreparationTail(String usageId) {
        try {
            tailRequire(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive(),"EXPIRED_TAIL_TRANSACTION_ACTIVE");
            ExpiredTail original=expiredTail(usageId,false,false,true);
            tailRequire(properties.hasMerchantConfiguration(),"EXPIRED_TAIL_EVIDENCE_UNAVAILABLE");
            ExternalBillingGateway gateway=gateways.get();tailRequire(gateway!=null,"EXPIRED_TAIL_EVIDENCE_UNAVAILABLE");
            long evidenceAt=System.currentTimeMillis();
            ExternalBillingGateway.Result reference=gateway.byReference(String.valueOf(original.tail.get("merchant_charge_id")));
            ExternalBillingGateway.Result key=gateway.byKey(String.valueOf(original.tail.get("idempotency_key")));
            tailRequire(reference!=null&&key!=null&&reference.status==ExternalBillingGateway.Status.NOT_FOUND
                &&key.status==ExternalBillingGateway.Status.NOT_FOUND,"EXPIRED_TAIL_ORIGINAL_LOOKUP_UNCONFIRMED");
            ExternalBillingGateway.Result observed=gateway.holdById(original.holdId);
            tailRequire(observed!=null&&observed.status==ExternalBillingGateway.Status.SUCCEEDED&&observed.charge!=null,"EXPIRED_TAIL_HOLD_CONFLICT");
            FundingContracts.HoldDetail detail=FundingContracts.holdDetail(observed.charge);
            verifyActiveSshTailHold(original,detail);
            return Boolean.TRUE.equals(transactions.execute(tx->{
                ExpiredTail current=expiredTail(usageId,true,false,true);
                tailRequire(original.fingerprint.equals(current.fingerprint),"EXPIRED_TAIL_CONCURRENT_CHANGE");
                long now=System.currentTimeMillis();
                tailRequire(now>=evidenceAt&&now-evidenceAt<=30000L,"EXPIRED_TAIL_EVIDENCE_STALE");
                verifyActiveSshTailHold(current,detail);
                lockTailRemainder(current,usageId);
                // Keep the original reason as audit evidence. PREPARED alone restores the
                // ordinary dispatcher, which rechecks this same reference/key and hold.
                tailRequire(jdbc.update("UPDATE vls_compute_billing_slice SET state='PREPARED' WHERE merchant_charge_id=? AND state='BLOCKED' AND error_code='SSH_PREPARATION_FAILED' AND attempt_count=0 AND lease_token IS NULL AND lease_until_ms=0 AND charge_id IS NULL AND ledger_entry_id IS NULL",
                    current.tail.get("merchant_charge_id"))==1,"EXPIRED_TAIL_CONCURRENT_CHANGE");
                return true;
            }));
        } catch(ExpiredTailBlocked blocked) {expiredTailReason(usageId,blocked.reason);return false;}
        catch(RuntimeException unavailable) {expiredTailReason(usageId,"EXPIRED_TAIL_EVIDENCE_UNAVAILABLE");return false;}
    }

    private void verifyActiveSshTailHold(ExpiredTail value,FundingContracts.HoldDetail detail) {
        tailRequire(value.expires>System.currentTimeMillis()&&("ACTIVE".equals(detail.holdStatus)||"PARTIALLY_CAPTURED".equals(detail.holdStatus))
            &&"0".equals(detail.releasedQuota),"EXPIRED_TAIL_HOLD_CONFLICT");
        verifyTailHoldObservation(value,detail);
        tailRequire(ExactQuota.integer(detail.remainingQuota,false).compareTo(ExactQuota.integer(String.valueOf(value.tail.get("amount_text")),true))>=0,"EXPIRED_TAIL_HOLD_CONFLICT");
        long now=System.currentTimeMillis(),observed=number(value.observation,"observed_at_ms");
        tailRequire(observed<=now&&now-observed<=30000L,"EXPIRED_TAIL_EVIDENCE_STALE");
    }

    /** Only an expired tail first made dispatchable after expiry may be waived.
     * It remains BLOCKED with its original amount/body/key. No debit or refund is issued. */
    public boolean confirmExpiredUnsentTail(String usageId) {
        try {
            tailRequire(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive(),"EXPIRED_TAIL_TRANSACTION_ACTIVE");
            ExpiredTail original=expiredTail(usageId,false);
            if(EXPIRED_UNSENT_TAIL.equals(original.tail.get("error_code")))return true;
            tailRequire(properties.hasMerchantConfiguration(),"EXPIRED_TAIL_EVIDENCE_UNAVAILABLE");
            ExternalBillingGateway gateway=gateways.get();
            tailRequire(gateway!=null,"EXPIRED_TAIL_EVIDENCE_UNAVAILABLE");
            long evidenceAt=System.currentTimeMillis();
            ExternalBillingGateway.Result reference=gateway.byReference(String.valueOf(original.tail.get("merchant_charge_id")));
            ExternalBillingGateway.Result key=gateway.byKey(String.valueOf(original.tail.get("idempotency_key")));
            tailRequire(reference!=null&&key!=null&&reference.status==ExternalBillingGateway.Status.NOT_FOUND
                &&key.status==ExternalBillingGateway.Status.NOT_FOUND,"EXPIRED_TAIL_ORIGINAL_LOOKUP_UNCONFIRMED");
            ExternalBillingGateway.Result observed=gateway.holdById(original.holdId);
            tailRequire(observed!=null&&observed.status==ExternalBillingGateway.Status.SUCCEEDED&&observed.charge!=null,"EXPIRED_TAIL_HOLD_CONFLICT");
            FundingContracts.HoldDetail detail=FundingContracts.holdDetail(observed.charge);
            verifyExpiredTailHold(original,detail);
            return Boolean.TRUE.equals(transactions.execute(tx->{
                ExpiredTail current=expiredTail(usageId,true);
                if(EXPIRED_UNSENT_TAIL.equals(current.tail.get("error_code")))return true;
                tailRequire(original.fingerprint.equals(current.fingerprint),"EXPIRED_TAIL_CONCURRENT_CHANGE");
                long now=System.currentTimeMillis();
                tailRequire(now>=evidenceAt&&now-evidenceAt<=30000L,"EXPIRED_TAIL_EVIDENCE_STALE");
                verifyExpiredTailHold(current,detail);
                String remainderUnit=lockTailRemainder(current,usageId);
                jdbc.update("UPDATE vls_compute_account_remainder SET numerator_text=?,denominator_text=? WHERE payer_user_id=? AND quota_unit=?",
                    current.before.numerator.toString(),current.before.denominator.toString(),current.payer,remainderUnit);
                tailRequire(jdbc.update("UPDATE vls_compute_billing_slice SET error_code=? WHERE merchant_charge_id=? AND state='BLOCKED' AND error_code='HOLD_EXPIRED' AND lease_token IS NULL AND lease_until_ms=0 AND charge_id IS NULL AND ledger_entry_id IS NULL",
                    EXPIRED_UNSENT_TAIL,current.tail.get("merchant_charge_id"))==1,"EXPIRED_TAIL_CONCURRENT_CHANGE");
                jdbc.update("UPDATE vls_compute_paid_run SET error_code='EXPIRED_TAIL_NEVER_DEBITED' WHERE usage_id=?",usageId);
                return true;
            }));
        } catch(ExpiredTailBlocked blocked) {
            expiredTailReason(usageId,blocked.reason);return false;
        } catch(RuntimeException unavailable) {
            expiredTailReason(usageId,"EXPIRED_TAIL_EVIDENCE_UNAVAILABLE");return false;
        }
    }

    /** Expiry prevents a new debit. This only permits the original final RELEASE;
     * the tail stays blocked until the release transaction forms a hold-lock barrier. */
    public boolean allowExpiredRunWindowTailRelease(String usageId) {
        return allowExpiredStoppedTailRelease(usageId,false);
    }

    public boolean allowExpiredSshPreparationTailRelease(String usageId) {
        return allowExpiredStoppedTailRelease(usageId,true);
    }

    private boolean allowExpiredStoppedTailRelease(String usageId,boolean sshPreparation) {
        try {
            tailRequire(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive(),"EXPIRED_TAIL_TRANSACTION_ACTIVE");
            ExpiredTail original=expiredTail(usageId,false,true,sshPreparation);
            if(releasedTailReason(sshPreparation).equals(original.tail.get("error_code")))return true;
            tailRequire(properties.hasMerchantConfiguration(),"EXPIRED_TAIL_EVIDENCE_UNAVAILABLE");
            ExternalBillingGateway gateway=gateways.get();tailRequire(gateway!=null,"EXPIRED_TAIL_EVIDENCE_UNAVAILABLE");
            long evidenceAt=System.currentTimeMillis();
            FundingContracts.HoldDetail detail=null;
            if(original.release==null) {
                ExternalBillingGateway.Result observed=gateway.holdById(original.holdId);
                tailRequire(observed!=null&&observed.status==ExternalBillingGateway.Status.SUCCEEDED&&observed.charge!=null,"EXPIRED_TAIL_HOLD_CONFLICT");
                detail=FundingContracts.holdDetail(observed.charge);verifyExpiredTailHold(original,detail);
            }
            final FundingContracts.HoldDetail beforeRelease=detail;
            return Boolean.TRUE.equals(transactions.execute(tx->{
                ExpiredTail current=expiredTail(usageId,true,true,sshPreparation);
                tailRequire(original.fingerprint.equals(current.fingerprint),"EXPIRED_TAIL_CONCURRENT_CHANGE");
                tailRequire(System.currentTimeMillis()>=evidenceAt&&System.currentTimeMillis()-evidenceAt<=30000L,"EXPIRED_TAIL_EVIDENCE_STALE");
                if(beforeRelease!=null)verifyExpiredTailHold(current,beforeRelease);
                lockTailRemainder(current,usageId);return true;
            }));
        } catch(ExpiredTailBlocked blocked) {expiredTailReason(usageId,blocked.reason);return false;}
        catch(RuntimeException unavailable) {expiredTailReason(usageId,"EXPIRED_TAIL_EVIDENCE_UNAVAILABLE");return false;}
    }

    /** A confirmed original RELEASE serializes after any old capture on this same hold.
     * Absence lookups only become conclusive after that barrier and exact counter checks. */
    public boolean confirmReleasedRunWindowTail(String usageId) {
        return confirmReleasedStoppedTail(usageId,false);
    }

    public boolean confirmReleasedSshPreparationTail(String usageId) {
        return confirmReleasedStoppedTail(usageId,true);
    }

    private boolean confirmReleasedStoppedTail(String usageId,boolean sshPreparation) {
        try {
            tailRequire(!org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive(),"EXPIRED_TAIL_TRANSACTION_ACTIVE");
            ExpiredTail original=expiredTail(usageId,false,true,sshPreparation);
            if(releasedTailReason(sshPreparation).equals(original.tail.get("error_code")))return true;
            verifyRunWindowReleaseReceipt(original);
            tailRequire(properties.hasMerchantConfiguration(),"EXPIRED_TAIL_EVIDENCE_UNAVAILABLE");
            ExternalBillingGateway gateway=gateways.get();tailRequire(gateway!=null,"EXPIRED_TAIL_EVIDENCE_UNAVAILABLE");
            long evidenceAt=System.currentTimeMillis();
            ExternalBillingGateway.Result observed=gateway.holdById(original.holdId);
            tailRequire(observed!=null&&observed.status==ExternalBillingGateway.Status.SUCCEEDED&&observed.charge!=null,"EXPIRED_TAIL_HOLD_CONFLICT");
            FundingContracts.HoldDetail detail=FundingContracts.holdDetail(observed.charge);verifyReleasedTailHold(original,detail);
            for(Map<String,Object> slice:original.slices)if("SETTLED".equals(slice.get("state"))) {
                ExternalBillingGateway.Result capture=gateway.byReference(String.valueOf(slice.get("merchant_charge_id")));
                tailRequire(capture!=null&&capture.status==ExternalBillingGateway.Status.SUCCEEDED
                    &&matchesReleasedCapture(jsonTree(String.valueOf(slice.get("request_json"))),capture.charge)
                    &&String.valueOf(slice.get("charge_id")).equals(capture.charge.path("charge_id").asText())
                    &&String.valueOf(slice.get("ledger_entry_id")).equals(capture.charge.path("ledger_entry_id").asText()),"EXPIRED_TAIL_CAPTURE_CONFLICT");
                tailRequire(capture.charge.path("refunded_quota").isTextual()&&"0".equals(capture.charge.path("refunded_quota").asText()),"EXPIRED_TAIL_CAPTURE_REFUNDED");
                if(!capture.charge.has("merchant_id")||!capture.charge.has("hold_id")) {
                    ExternalBillingGateway.Result operation=gateway.byKey(String.valueOf(slice.get("idempotency_key")));
                    tailRequire(operation!=null&&operation.status==ExternalBillingGateway.Status.SUCCEEDED
                        &&matchesCaptureOperation(slice,jsonTree(String.valueOf(slice.get("request_json"))),capture.charge,operation.charge),"EXPIRED_TAIL_CAPTURE_CONFLICT");
                }
            }
            ExternalBillingGateway.Result reference=gateway.byReference(String.valueOf(original.tail.get("merchant_charge_id")));
            ExternalBillingGateway.Result key=gateway.byKey(String.valueOf(original.tail.get("idempotency_key")));
            tailRequire(reference!=null&&key!=null&&reference.status==ExternalBillingGateway.Status.NOT_FOUND
                &&key.status==ExternalBillingGateway.Status.NOT_FOUND,"EXPIRED_TAIL_ORIGINAL_LOOKUP_UNCONFIRMED");
            return Boolean.TRUE.equals(transactions.execute(tx->{
                ExpiredTail current=expiredTail(usageId,true,true,sshPreparation);
                if(releasedTailReason(sshPreparation).equals(current.tail.get("error_code")))return true;
                tailRequire(original.fingerprint.equals(current.fingerprint),"EXPIRED_TAIL_CONCURRENT_CHANGE");
                tailRequire(System.currentTimeMillis()>=evidenceAt&&System.currentTimeMillis()-evidenceAt<=30000L,"EXPIRED_TAIL_EVIDENCE_STALE");
                verifyRunWindowReleaseReceipt(current);verifyReleasedTailHold(current,detail);
                String unit=lockTailRemainder(current,usageId);
                jdbc.update("UPDATE vls_compute_account_remainder SET numerator_text=?,denominator_text=? WHERE payer_user_id=? AND quota_unit=?",
                    current.before.numerator.toString(),current.before.denominator.toString(),current.payer,unit);
                tailRequire(jdbc.update("UPDATE vls_compute_billing_slice SET error_code=? WHERE merchant_charge_id=? AND state='BLOCKED' AND error_code=? AND attempt_count=0 AND lease_token IS NULL AND lease_until_ms=0 AND charge_id IS NULL AND ledger_entry_id IS NULL",
                    releasedTailReason(sshPreparation),current.tail.get("merchant_charge_id"),sshPreparation?"SSH_PREPARATION_FAILED":"RUN_WINDOW_EXPIRED")==1,"EXPIRED_TAIL_CONCURRENT_CHANGE");
                jdbc.update("UPDATE vls_compute_paid_run SET error_code=? WHERE usage_id=?",sshPreparation?"SSH_PREPARATION_TAIL_WAIVED_AFTER_RELEASE":"RUN_WINDOW_TAIL_WAIVED_AFTER_RELEASE",usageId);return true;
            }));
        } catch(ExpiredTailBlocked blocked) {expiredTailReason(usageId,blocked.reason);return false;}
        catch(RuntimeException unavailable) {expiredTailReason(usageId,"EXPIRED_TAIL_EVIDENCE_UNAVAILABLE");return false;}
    }

    private static String releasedTailReason(boolean sshPreparation) {
        return sshPreparation?RELEASED_SSH_PREPARATION_TAIL:RELEASED_RUN_WINDOW_TAIL;
    }

    private String lockTailRemainder(ExpiredTail current,String usageId) {
        String unit="origin:"+hash(BillingLedgerOrigin.namespace(properties)+"\n"+current.snapshot.quotaUnit);
        Map<String,Object> account=jdbc.queryForMap("SELECT * FROM vls_compute_account_remainder WHERE payer_user_id=? AND quota_unit=? FOR UPDATE",current.payer,unit);
        tailRequire(fraction(account,"numerator_text","denominator_text").toString().equals(current.after.toString()),"EXPIRED_TAIL_REMAINDER_CHANGED");
        // After this account lock, no other account writer can commit a successor until this decision commits.
        for(Map<String,Object> successor:datedRows("SELECT s.*,u.accepted_snapshot,u.merchant_id FROM vls_compute_billing_slice s JOIN vls_compute_billing_session u ON u.usage_id=s.usage_id WHERE u.payer_user_id=? AND u.quota_unit=? AND u.merchant_id=? AND s.merchant_charge_id<>?",
            current.payer,current.snapshot.quotaUnit,properties.getMerchantId(),current.tail.get("merchant_charge_id"))) {
            if(!BillingLedgerOrigin.matches(String.valueOf(successor.get("accepted_snapshot")),properties.getMerchantId(),properties))continue;
            if(usageId.equals(successor.get("usage_id"))&&number(successor,"slice_sequence")<number(current.tail,"slice_sequence"))continue;
            tailRequire(successor.get("create_time") instanceof Timestamp&&((Timestamp)successor.get("create_time")).getTime()<current.createdAt,"EXPIRED_TAIL_SUCCESSOR_EXISTS");
        }
        return unit;
    }

    private ExpiredTail expiredTail(String id,boolean lock) {
        return expiredTail(id,lock,false);
    }
    private ExpiredTail expiredTail(String id,boolean lock,boolean releaseBarrier) {
        return expiredTail(id,lock,releaseBarrier,false);
    }
    private ExpiredTail expiredTail(String id,boolean lock,boolean releaseBarrier,boolean sshPreparation) {
        String suffix=lock?" FOR UPDATE":"";
        Map<String,Object> usage=usage(id);
        Map<String,Object> session=jdbc.queryForMap("SELECT * FROM vls_compute_billing_session WHERE usage_id=?"+suffix,id);
        tailRequire(BillingLedgerOrigin.matches(String.valueOf(session.get("accepted_snapshot")),String.valueOf(session.get("merchant_id")),properties),"EXPIRED_TAIL_ORIGIN_MISMATCH");
        java.util.List<Map<String,Object>> runs=datedRows("SELECT * FROM vls_compute_paid_run WHERE usage_id=?"+suffix,id);
        tailRequire(runs.size()==1,"EXPIRED_TAIL_RUN_NOT_UNIQUE");Map<String,Object> run=runs.get(0);
        tailRequire("STOPPING".equals(run.get("state"))||"RELEASING".equals(run.get("state")),"EXPIRED_TAIL_RUN_STATE");
        tailRequire(run.get("active_instance_key")!=null&&run.get("active_instance_key").equals(run.get("instance_key")),"EXPIRED_TAIL_RUN_CLAIM_MISMATCH");
        for(String field:new String[]{"tenant_id","opening_user_id","merchant_id","payer_user_id"})
            tailRequire(java.util.Objects.equals(run.get(field),session.get(field)),"EXPIRED_TAIL_OWNERSHIP_CONFLICT");
        tailRequire(java.util.Objects.equals(usage.get("tenant_id"),session.get("tenant_id"))
            &&java.util.Objects.equals(usage.get("opening_user_id"),session.get("opening_user_id"))
            &&java.util.Objects.equals(usage.get("server_key"),session.get("instance_ref"))
            &&(usage.get("active_server_key")==null||java.util.Objects.equals(usage.get("active_server_key"),usage.get("server_key")))
            &&("node-server:"+run.get("node_id")).equals(session.get("instance_ref"))
            &&java.util.Objects.equals(usage.get("payer_account_ref"),run.get("platform_subject"))
            &&java.util.Objects.equals(usage.get("start_operation_id"),run.get("start_operation_id")),"EXPIRED_TAIL_OWNERSHIP_CONFLICT");
        tailRequire(jsonTree(String.valueOf(run.get("quote_snapshot"))).equals(jsonTree(String.valueOf(session.get("accepted_snapshot")))),"EXPIRED_TAIL_OWNERSHIP_CONFLICT");
        tailRequire("STOPPED".equals(usage.get("state")),"EXPIRED_TAIL_USAGE_NOT_STOPPED");
        tailRequire(usage.get("billing_start_at") instanceof Timestamp&&usage.get("billing_end_at") instanceof Timestamp,"EXPIRED_TAIL_USAGE_TIME_MISSING");
        tailRequire("STOP_CONFIRMED".equals(session.get("safety_state"))&&number(session,"physical_stop_observed_ms")>0,"EXPIRED_TAIL_STOP_UNCONFIRMED");
        long ended=((Timestamp)usage.get("billing_end_at")).getTime();
        tailRequire(number(session,"metered_until_ms")==ended&&number(session,"last_running_ms")==ended,"EXPIRED_TAIL_METERING_MISMATCH");
        BillingSnapshot snapshot=BillingSnapshot.read(String.valueOf(session.get("accepted_snapshot")),json);
        java.util.List<Map<String,Object>> slices=datedRows("SELECT * FROM vls_compute_billing_slice WHERE usage_id=? ORDER BY slice_sequence"+suffix,id);
        Map<String,Object> tail=null;BigInteger captured=BigInteger.ZERO;long sequence=0,cursor=((Timestamp)usage.get("billing_start_at")).getTime();
        for(Map<String,Object> slice:slices) {
            tailRequire(number(slice,"slice_sequence")==sequence++,"EXPIRED_TAIL_SLICE_SEQUENCE_GAP");
            tailRequire(number(slice,"interval_start_ms")==cursor&&number(slice,"interval_end_ms")>cursor,"EXPIRED_TAIL_SLICE_INTERVAL_GAP");
            cursor=number(slice,"interval_end_ms");
            verifyTailSlice(slice,session,snapshot);
            String state=String.valueOf(slice.get("state"));
            if("SETTLED".equals(state)) {
                tailRequire(slice.get("charge_id")!=null&&slice.get("ledger_entry_id")!=null,"EXPIRED_TAIL_SETTLED_RECEIPT_MISSING");
                captured=captured.add(ExactQuota.integer(String.valueOf(slice.get("amount_text")),true));
            } else if("NO_DEBIT".equals(state))tailRequire("0".equals(slice.get("amount_text")),"EXPIRED_TAIL_NO_DEBIT_NONZERO");
            else {
                tailRequire(tail==null,"EXPIRED_TAIL_MULTIPLE_UNSETTLED");
                tailRequire("BLOCKED".equals(state),"EXPIRED_TAIL_UNSETTLED_SLICE_STATE");
                tailRequire(sshPreparation?("SSH_PREPARATION_FAILED".equals(slice.get("error_code"))||(releaseBarrier&&RELEASED_SSH_PREPARATION_TAIL.equals(slice.get("error_code")))):releaseBarrier?("RUN_WINDOW_EXPIRED".equals(slice.get("error_code"))||RELEASED_RUN_WINDOW_TAIL.equals(slice.get("error_code")))
                    :("HOLD_EXPIRED".equals(slice.get("error_code"))||EXPIRED_UNSENT_TAIL.equals(slice.get("error_code"))),"EXPIRED_TAIL_UNSETTLED_SLICE_REASON");
                tailRequire(number(slice,"is_tail")==1&&number(slice,"interval_end_ms")==ended,"EXPIRED_TAIL_NOT_FINAL_TAIL");
                tailRequire(slice.get("charge_id")==null&&slice.get("ledger_entry_id")==null,"EXPIRED_TAIL_DEBIT_EVIDENCE");
                tail=slice;
            }
        }
        tailRequire(tail!=null,"EXPIRED_TAIL_MISSING");
        tailRequire(number(tail,"slice_sequence")==sequence-1,"EXPIRED_TAIL_NOT_LAST_SLICE");
        tailRequire(number(session,"next_sequence")==sequence,"EXPIRED_TAIL_SEQUENCE_CURSOR_MISMATCH");
        tailRequire(cursor==ended,"EXPIRED_TAIL_INTERVAL_END_MISMATCH");
        tailRequire(number(tail,"attempt_count")==0,"EXPIRED_TAIL_SEND_EVIDENCE");
        tailRequire(session.get("funding_hold_intent_id")!=null&&session.get("funding_hold_id")!=null
            &&session.get("funding_hold_intent_id").equals(run.get("hold_intent_id")),"EXPIRED_TAIL_HOLD_CONFLICT");
        Map<String,Object> hold=jdbc.queryForMap("SELECT * FROM vls_compute_funding_intent WHERE intent_id=?"+suffix,run.get("hold_intent_id"));
        Map<String,Object> observation=jdbc.queryForMap("SELECT * FROM vls_compute_funding_hold_observation WHERE hold_intent_id=?"+suffix,run.get("hold_intent_id"));
        JsonNode holdBody=jsonTree(String.valueOf(hold.get("request_json")));
        String payer=String.valueOf(session.get("payer_user_id")),merchant=String.valueOf(session.get("merchant_id"));
        String reference="vls-hold:"+id+":initial",holdId=String.valueOf(session.get("funding_hold_id"));
        long expires=number(session,"funding_expires_at_ms");
        tailRequire("HOLD".equals(hold.get("operation_kind"))&&"HOLDS_V3".equals(hold.get("contract_version"))
            &&"DETAIL_OBSERVED".equals(hold.get("state"))&&hold.get("parent_intent_id")==null&&id.equals(hold.get("usage_id"))
            &&merchant.equals(hold.get("merchant_id"))&&payer.equals(hold.get("payer_user_id"))&&holdId.equals(hold.get("hold_id"))
            &&reference.equals(hold.get("business_reference"))&&"/api/v1/external-billing/holds".equals(hold.get("request_path"))
            &&hash(String.valueOf(hold.get("request_json"))).equals(hold.get("request_hash")),"EXPIRED_TAIL_HOLD_CONFLICT");
        tailRequire(FundingContracts.hold(merchant,reference,payer,String.valueOf(run.get("reserve_quota")),String.valueOf(expires),BillingSnapshot.required(holdBody,"reason")).toString().equals(hold.get("request_json"))
            &&expires==number(run,"hold_until_ms"),"EXPIRED_TAIL_HOLD_CONFLICT");
        tailRequire(run.get("create_time") instanceof Timestamp&&tail.get("create_time") instanceof Timestamp,"EXPIRED_TAIL_CLOCK_UNVERIFIED");
        long runCreated=((Timestamp)run.get("create_time")).getTime(),quoted=Math.subtractExact(number(run,"quote_until_ms"),120000L),created=((Timestamp)tail.get("create_time")).getTime();
        if(sshPreparation) {
            tailRequire("SSH_PREPARATION_FAILED".equals(session.get("risk_code")),"EXPIRED_TAIL_FINANCIAL_RISK");
            tailRequire(created<expires,"EXPIRED_TAIL_NOT_HISTORICAL_RUN_WINDOW");
            tailRequire(releaseBarrier?expires<=System.currentTimeMillis():expires>System.currentTimeMillis(),"EXPIRED_TAIL_HOLD_CONFLICT");
        } else if(releaseBarrier) {
            tailRequire(expires<=System.currentTimeMillis()&&created<expires,"EXPIRED_TAIL_NOT_HISTORICAL_RUN_WINDOW");
            tailRequire(session.get("risk_code")==null||"RUN_WINDOW_EXPIRED".equals(session.get("risk_code"))||"HOLD_EXPIRING".equals(session.get("risk_code")),"EXPIRED_TAIL_FINANCIAL_RISK");
        } else {
            long databaseNow=verifiedDatabaseNow();
            tailRequire(runCreated>=quoted-TAIL_CLOCK_SKEW_MS&&runCreated<=quoted+TAIL_CLOCK_SKEW_MS
                &&runCreated<=((Timestamp)usage.get("billing_start_at")).getTime()+TAIL_CLOCK_SKEW_MS
                &&created<=databaseNow+TAIL_CLOCK_SKEW_MS,"EXPIRED_TAIL_CLOCK_UNVERIFIED");
            tailRequire(created>Math.addExact(expires,TAIL_BIRTH_MARGIN_MS),"EXPIRED_TAIL_CREATED_BEFORE_EXPIRY");
        }
        ExactQuota after=fraction(tail,"remainder_numerator","remainder_denominator");
        ExactQuota cost=ExactQuota.compute(snapshot.approvedHourlyPrice,Math.subtractExact(number(tail,"interval_end_ms"),number(tail,"interval_start_ms")),snapshot.numerator,snapshot.denominator);
        ExactQuota allocated=new ExactQuota(ExactQuota.integer(String.valueOf(tail.get("amount_text")),true),BigInteger.ONE).add(after);
        ExactQuota before=new ExactQuota(allocated.numerator.multiply(cost.denominator).subtract(cost.numerator.multiply(allocated.denominator)),allocated.denominator.multiply(cost.denominator));
        tailRequire(before.numerator.compareTo(before.denominator)<0,"EXPIRED_TAIL_REMAINDER_CHANGED");
        ExpiredTail value=new ExpiredTail();value.run=run;value.session=session;value.usage=usage;value.tail=tail;value.hold=hold;value.observation=observation;
        value.snapshot=snapshot;value.payer=payer;value.holdId=holdId;value.expires=expires;value.createdAt=created;value.captured=captured;value.after=after;value.before=before;
        value.slices=slices;
        java.util.List<Map<String,Object>> evidence=new java.util.ArrayList<>(java.util.Arrays.asList(run,session,usage,hold,observation));
        if(releaseBarrier) {
            tailRequire(jdbc.queryForList("SELECT refund_intent_id FROM vls_compute_refund_intent WHERE usage_id=?"+suffix,id).isEmpty(),"EXPIRED_TAIL_REFUND_EXISTS");
            java.util.List<Map<String,Object>> children=jdbc.queryForList("SELECT * FROM vls_compute_funding_intent WHERE usage_id=? AND intent_id<>?"+suffix,id,run.get("hold_intent_id"));
            tailRequire(children.size()==(run.get("release_intent_id")==null?0:1),"EXPIRED_TAIL_HOLD_CONFLICT");
            if(!children.isEmpty()) {
                value.release=children.get(0);verifyRunWindowReleaseRequest(value);evidence.add(value.release);
            }
        } else if(!EXPIRED_UNSENT_TAIL.equals(tail.get("error_code"))) {
            tailRequire(run.get("release_intent_id")==null,"EXPIRED_TAIL_HOLD_CONFLICT");
            tailRequire(jdbc.queryForList("SELECT intent_id FROM vls_compute_funding_intent WHERE usage_id=? AND intent_id<>?"+suffix,id,run.get("hold_intent_id")).isEmpty()
                &&jdbc.queryForList("SELECT refund_intent_id FROM vls_compute_refund_intent WHERE usage_id=?"+suffix,id).isEmpty(),"EXPIRED_TAIL_HOLD_CONFLICT");
        }
        value.fingerprint=tailFingerprint(evidence,slices);
        return value;
    }

    private void verifyRunWindowReleaseRequest(ExpiredTail value) {
        Map<String,Object> release=value.release;String reference="vls-release:"+value.session.get("usage_id")+":final";
        String amount=ExactQuota.integer(String.valueOf(value.run.get("reserve_quota")),true).subtract(value.captured).toString();
        tailRequire(release!=null&&java.util.Objects.equals(value.run.get("release_intent_id"),release.get("intent_id"))
            &&"RELEASE".equals(release.get("operation_kind"))&&"HOLDS_V3".equals(release.get("contract_version"))
            &&java.util.Objects.equals(value.hold.get("intent_id"),release.get("parent_intent_id"))
            &&java.util.Objects.equals(value.session.get("usage_id"),release.get("usage_id"))
            &&java.util.Objects.equals(value.session.get("merchant_id"),release.get("merchant_id"))&&value.payer.equals(release.get("payer_user_id"))
            &&reference.equals(release.get("business_reference"))&&("/api/v1/external-billing/holds/"+value.holdId+"/release").equals(release.get("request_path"))
            &&hash(String.valueOf(release.get("request_json"))).equals(release.get("request_hash"))
            &&FundingContracts.release(String.valueOf(value.session.get("merchant_id")),reference,value.payer,amount).toString().equals(release.get("request_json"))
            &&release.get("conflicting_receipt_json")==null&&!"CONFLICT".equals(release.get("state"))&&!"REJECTED".equals(release.get("state")),"EXPIRED_TAIL_RELEASE_CONFLICT");
    }
    private void verifyRunWindowReleaseReceipt(ExpiredTail value) {
        verifyRunWindowReleaseRequest(value);
        tailRequire("OPERATION_CONFIRMED".equals(value.release.get("state"))&&value.release.get("receipt_json")!=null,"EXPIRED_TAIL_RELEASE_UNCONFIRMED");
        FundingContracts.ReleaseReceipt receipt=FundingContracts.releaseResult(jsonTree(String.valueOf(value.release.get("receipt_json"))));
        String amount=ExactQuota.integer(String.valueOf(value.run.get("reserve_quota")),true).subtract(value.captured).toString();
        // The upstream may derive EXPIRED after a fully successful original release.
        // Its receipt and exact released/remaining counters, never this status alone, prove the barrier.
        tailRequire(value.holdId.equals(receipt.holdId)&&amount.equals(receipt.releasedQuota)&&"0".equals(receipt.remainingQuota)
            &&("RELEASED".equals(receipt.holdStatus)||"EXPIRED".equals(receipt.holdStatus)),"EXPIRED_TAIL_RELEASE_CONFLICT");
    }
    private void verifyReleasedTailHold(ExpiredTail value,FundingContracts.HoldDetail detail) {
        tailRequire(("RELEASED".equals(detail.holdStatus)||"EXPIRED".equals(detail.holdStatus))&&"0".equals(detail.remainingQuota)
            &&ExactQuota.integer(String.valueOf(value.run.get("reserve_quota")),true).subtract(value.captured).toString().equals(detail.releasedQuota),"EXPIRED_TAIL_HOLD_CONFLICT");
        verifyTailHoldObservation(value,detail);
    }
    private boolean matchesReleasedCapture(JsonNode request,JsonNode charge) {
        if(charge==null||!"SUCCEEDED".equals(charge.path("status").asText())||!charge.path("charged_quota").isTextual()
            ||!request.path("charge_quota").asText().equals(charge.path("charged_quota").asText()))return false;
        for(String field:new String[]{"merchant_charge_id","user_id","usage_session_id"})
            if(!request.path(field).asText().equals(charge.path(field).asText()))return false;
        for(String field:new String[]{"merchant_id","hold_id","metering_ref","price_snapshot_id","conversion_version"})
            if(charge.has(field)&&!request.path(field).asText().equals(charge.path(field).asText()))return false;
        return true;
    }

    private void verifyTailSlice(Map<String,Object> slice,Map<String,Object> session,BillingSnapshot snapshot) {
        tailRequire(slice.get("lease_token")==null&&number(slice,"lease_until_ms")==0,"EXPIRED_TAIL_LEASE_ACTIVE");
        long begin=number(slice,"interval_start_ms"),end=number(slice,"interval_end_ms");snapshot.requireInterval(begin,end);
        String id=String.valueOf(session.get("usage_id")),reference="vls:"+id+":"+number(slice,"slice_sequence"),body=String.valueOf(slice.get("request_json"));
        tailRequire(reference.equals(slice.get("merchant_charge_id"))&&String.valueOf(slice.get("idempotency_key")).matches("vls-op-[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
            &&hash(body).equals(slice.get("request_hash")),"EXPIRED_TAIL_REQUEST_CONFLICT");
        ObjectNode expected=json.createObjectNode();expected.put("merchant_id",String.valueOf(session.get("merchant_id"))).put("merchant_charge_id",reference);
        expected.put("user_id",ExactQuota.integer(String.valueOf(session.get("payer_user_id")),true));expected.put("business_type","GPU_COMPUTE").put("usage_session_id",id);
        expected.put("charge_quota",String.valueOf(slice.get("amount_text")));
        expected.put("metering_ref","sha256:"+hash(session.get("instance_ref")+":"+id+":"+begin+":"+end+":"+snapshot.json));
        expected.put("price_snapshot_id",snapshot.priceId).put("conversion_version",snapshot.conversionVersion);
        expected.put("hold_id",ExactQuota.integer(String.valueOf(session.get("funding_hold_id")),true));
        tailRequire(expected.toString().equals(body),"EXPIRED_TAIL_REQUEST_CONFLICT");
    }
    private void verifyExpiredTailHold(ExpiredTail value,FundingContracts.HoldDetail detail) {
        tailRequire("EXPIRED".equals(detail.holdStatus)&&"0".equals(detail.releasedQuota),"EXPIRED_TAIL_HOLD_CONFLICT");
        verifyTailHoldObservation(value,detail);
    }
    private void verifyTailHoldObservation(ExpiredTail value,FundingContracts.HoldDetail detail) {
        Map<String,Object> observation=value.observation;
        tailRequire(detail.holdId.equals(value.holdId)&&detail.userId.equals(value.payer)
            &&detail.merchantHoldId.equals("vls-hold:"+value.session.get("usage_id")+":initial")&&detail.expiresAt.equals(String.valueOf(value.expires))
            &&detail.holdQuota.equals(String.valueOf(value.run.get("reserve_quota")))&&detail.capturedQuota.equals(value.captured.toString()),"EXPIRED_TAIL_HOLD_CONFLICT");
        for(String[] field:new String[][]{{"hold_id",detail.holdId},{"hold_quota",detail.holdQuota},{"captured_quota",detail.capturedQuota},
            {"released_quota",detail.releasedQuota},{"remaining_quota",detail.remainingQuota},{"hold_status",detail.holdStatus},
            {"balance_version",detail.balanceVersion},{"expires_at_ms",detail.expiresAt}})
            tailRequire(field[1].equals(String.valueOf(observation.get(field[0]))),"EXPIRED_TAIL_HOLD_CONFLICT");
        tailRequire(observation.get("conflicting_detail_json")==null&&hash(String.valueOf(observation.get("detail_json"))).equals(observation.get("detail_hash")),"EXPIRED_TAIL_HOLD_CONFLICT");
        FundingContracts.HoldDetail stored=FundingContracts.holdDetail(jsonTree(String.valueOf(observation.get("detail_json"))));
        tailRequire(stored.holdId.equals(detail.holdId)&&stored.merchantHoldId.equals(detail.merchantHoldId)&&stored.userId.equals(detail.userId)
            &&stored.holdQuota.equals(detail.holdQuota)&&stored.capturedQuota.equals(detail.capturedQuota)&&stored.releasedQuota.equals(detail.releasedQuota)
            &&stored.remainingQuota.equals(detail.remainingQuota)&&stored.holdStatus.equals(detail.holdStatus)&&stored.balanceVersion.equals(detail.balanceVersion)
            &&stored.expiresAt.equals(detail.expiresAt),"EXPIRED_TAIL_HOLD_CONFLICT");
    }
    private long verifiedDatabaseNow() {
        long before=System.currentTimeMillis();Timestamp now=jdbc.queryForObject("SELECT NOW(3)",(rs,index)->rs.getTimestamp(1));long after=System.currentTimeMillis();
        tailRequire(now!=null&&after>=before&&after-before<=TAIL_CLOCK_SKEW_MS&&now.getTime()>=before-TAIL_CLOCK_SKEW_MS&&now.getTime()<=after+TAIL_CLOCK_SKEW_MS,"EXPIRED_TAIL_CLOCK_UNVERIFIED");
        return now.getTime();
    }
    private java.util.List<Map<String,Object>> datedRows(String sql,Object... parameters) {
        return jdbc.query(sql,(rs,index)->{Map<String,Object> row=new org.springframework.jdbc.core.ColumnMapRowMapper().mapRow(rs,index);
            row.put("create_time",rs.getTimestamp("create_time"));return row;},parameters);
    }
    private ExactQuota fraction(Map<String,Object> row,String numerator,String denominator) {
        ExactQuota value=new ExactQuota(new BigInteger(String.valueOf(row.get(numerator))),new BigInteger(String.valueOf(row.get(denominator))));
        tailRequire(value.numerator.compareTo(value.denominator)<0,"EXPIRED_TAIL_REMAINDER_CHANGED");return value;
    }
    private String tailFingerprint(java.util.List<Map<String,Object>> rows,java.util.List<Map<String,Object>> slices) {
        java.util.List<java.util.Map<String,String>> values=new java.util.ArrayList<>();
        java.util.List<Map<String,Object>> combined=new java.util.ArrayList<>(rows);combined.addAll(slices);
        for(Map<String,Object> row:combined) {
            java.util.Map<String,String> value=new java.util.TreeMap<>();row.forEach((key,entry)->value.put(key,entry==null?null:String.valueOf(entry)));values.add(value);
        }
        try{return hash(json.writeValueAsString(values));}catch(java.io.IOException invalid){throw new ExpiredTailBlocked("EXPIRED_TAIL_EVIDENCE_UNAVAILABLE");}
    }
    private JsonNode jsonTree(String value) {
        try{return json.readTree(value);}catch(java.io.IOException invalid){throw new ExpiredTailBlocked("EXPIRED_TAIL_REQUEST_CONFLICT");}
    }
    private void expiredTailReason(String usage,String reason) {
        jdbc.update("UPDATE vls_compute_paid_run SET error_code=? WHERE usage_id=? AND state IN ('STOPPING','RELEASING') AND active_instance_key IS NOT NULL",reason,usage);
    }
    private static void tailRequire(boolean condition,String reason) {if(!condition)throw new ExpiredTailBlocked(reason);}
    private static final class ExpiredTailBlocked extends RuntimeException {
        final String reason;ExpiredTailBlocked(String reason){super(reason);this.reason=reason;}
    }
    private static final class ExpiredTail {
        Map<String,Object> run,session,usage,tail,hold,observation,release;java.util.List<Map<String,Object>> slices;BillingSnapshot snapshot;
        String payer,holdId,fingerprint;long expires,createdAt;BigInteger captured;ExactQuota before,after;
    }

    /** No exact provider event is available here: charge only through the last proven running sample.
     * Retain the later stopped observation independently rather than inventing an actual transition time. */
    public void confirmPhysicalStop(String usageId, java.time.Instant observedAt) {
        confirmPhysicalStop(usageId,observedAt,null);
    }
    public void confirmPhysicalStop(String usageId,java.time.Instant observedAt,java.time.Instant providerEvent) {
        if (observedAt == null || observedAt.toEpochMilli() > System.currentTimeMillis()) throw new IllegalArgumentException("Invalid stopped observation");
        transactions.execute(tx -> {
            Map<String, Object> usage = usage(usageId);
            Map<String, Object> session = jdbc.queryForMap("SELECT * FROM vls_compute_billing_session WHERE usage_id=? FOR UPDATE", usageId);
            long end = number(session, "last_running_ms");
            // A short run may stop before the first periodic running observation.
            // Its immutable confirmed start, not epoch zero or the polling time,
            // is the first unsettled boundary.
            Timestamp confirmedStart=(Timestamp)usage.get("billing_start_at");
            if(end==0&&number(session,"metered_until_ms")==0) {
                if(confirmedStart==null)throw new IllegalStateException("Confirmed start required");
                end=confirmedStart.getTime();
                jdbc.update("UPDATE vls_compute_billing_session SET metered_until_ms=?,last_running_ms=? WHERE usage_id=?",end,end,usageId);
            }
            if (observedAt.toEpochMilli() < end) throw new IllegalArgumentException("Stopped observation precedes proven runtime");
            if(providerEvent!=null) {
                Timestamp started=(Timestamp)usage.get("billing_start_at");
                if(started==null||providerEvent.isBefore(started.toInstant())||providerEvent.isAfter(observedAt)||providerEvent.toEpochMilli()<end)
                    throw new IllegalStateException("Provider stop event conflicts with proven runtime");
                end=providerEvent.toEpochMilli();
            }
            Timestamp priorEnd=(Timestamp)usage.get("billing_end_at");
            if(priorEnd!=null)end="PROVIDER_EVENT".equals(usage.get("stop_evidence"))?priorEnd.getTime():Math.min(end,priorEnd.getTime());
            if(end>observedAt.toEpochMilli()||end<number(session,"metered_until_ms"))throw new IllegalStateException("Runtime end conflicts with sealed intervals");
            jdbc.update("UPDATE vls_compute_usage SET state='STOPPED',billing_end_at=? WHERE id=?",new Timestamp(end),usageId);
            if(providerEvent!=null)jdbc.update("UPDATE vls_compute_usage SET stop_observed_at=?,stop_evidence='PROVIDER_EVENT' WHERE id=?",Timestamp.from(observedAt),usageId);
            jdbc.update("UPDATE vls_compute_billing_session SET last_running_ms=?,physical_stop_observed_ms=COALESCE(physical_stop_observed_ms,?),safety_state='STOP_CONFIRMED' WHERE usage_id=?",end,observedAt.toEpochMilli(),usageId);
            jdbc.update("UPDATE vls_compute_billing_slice SET is_tail=1 WHERE usage_id=? AND interval_end_ms=?", usageId, end);
            return null;
        });
    }

    /** Internal provider-observation hook; a scheduler's wall clock alone is not runtime evidence. */
    public void recordRunningObservation(String usageId, java.time.Instant observedAt) {
        if (observedAt == null || observedAt.toEpochMilli() > System.currentTimeMillis()) throw new IllegalArgumentException("Invalid runtime observation");
        transactions.execute(tx -> {
            Map<String, Object> usage = usage(usageId);
            if (usage.get("billing_end_at") != null || !("RUNNING".equals(usage.get("state")) || "RECONCILE".equals(usage.get("state"))))
                throw new IllegalArgumentException("Running observation cannot extend stopped/unconfirmed usage");
            Timestamp started=(Timestamp)usage.get("billing_start_at");
            if(started==null||observedAt.isBefore(started.toInstant()))throw new IllegalArgumentException("Confirmed start required");
            jdbc.update("UPDATE vls_compute_billing_session SET metered_until_ms=?,last_running_ms=? WHERE usage_id=? AND metered_until_ms=0 AND last_running_ms=0",started.getTime(),started.getTime(),usageId);
            jdbc.update("UPDATE vls_compute_billing_session SET last_running_ms=? WHERE usage_id=? AND last_running_ms<?", observedAt.toEpochMilli(), usageId, observedAt.toEpochMilli());
            return null;
        });
    }

    /** Seal contiguous [cursor,end) once. Keep measuring even after authorization/funding failure. */
    public String prepareInterval(String usageId, long endMillis) {
        requireBoundOrigin(usageId);
        return transactions.execute(tx -> {
            Map<String, Object> usage = usage(usageId);
            Map<String, Object> session = jdbc.queryForMap("SELECT * FROM vls_compute_billing_session WHERE usage_id=? FOR UPDATE", usageId);
            long start = number(session, "metered_until_ms");
            if (endMillis <= start) {
                List<String> existing = jdbc.queryForList("SELECT merchant_charge_id FROM vls_compute_billing_slice WHERE usage_id=? AND interval_end_ms=?", String.class, usageId, endMillis);
                if (!existing.isEmpty()) return existing.get(0);
                throw new IllegalArgumentException("Metering cursor cannot move backwards");
            }
            Timestamp stopped = (Timestamp) usage.get("billing_end_at");
            long confirmedUntil = stopped == null ? number(session, "last_running_ms") : stopped.getTime();
            if (endMillis > System.currentTimeMillis() || endMillis > confirmedUntil)
                throw new IllegalArgumentException("Cannot bill beyond observed runtime");
            BillingSnapshot snapshot = BillingSnapshot.read(String.valueOf(session.get("accepted_snapshot")), json);
            snapshot.requireInterval(start, endMillis);
            String user = String.valueOf(session.get("payer_user_id")), unit = snapshot.quotaUnit;
            String remainderUnit = "origin:" + hash(BillingLedgerOrigin.namespace(properties) + "\n" + unit);
            jdbc.update("INSERT IGNORE INTO vls_compute_account_remainder(payer_user_id,quota_unit,numerator_text,denominator_text) VALUES(?,?,'0','1')", user, remainderUnit);
            Map<String, Object> tail = jdbc.queryForMap("SELECT * FROM vls_compute_account_remainder WHERE payer_user_id=? AND quota_unit=? FOR UPDATE", user, remainderUnit);
            ExactQuota remainder = new ExactQuota(new BigInteger(String.valueOf(tail.get("numerator_text"))), new BigInteger(String.valueOf(tail.get("denominator_text"))));
            if (remainder.numerator.compareTo(remainder.denominator) >= 0) throw new IllegalStateException("Invalid persisted quota remainder");
            ExactQuota total = ExactQuota.compute(snapshot.approvedHourlyPrice, Math.subtractExact(endMillis, start), snapshot.numerator, snapshot.denominator).add(remainder);
            String amount = total.floor(); ExactQuota next = total.remainder();
            long sequence = number(session, "next_sequence");
            String reference = "vls:" + usageId + ":" + sequence;
            String key = "vls-op-" + UUID.randomUUID();
            ObjectNode body = json.createObjectNode();
            body.put("merchant_id", String.valueOf(session.get("merchant_id"))); body.put("merchant_charge_id", reference);
            body.put("user_id", new BigInteger(user)); body.put("business_type", "GPU_COMPUTE"); body.put("usage_session_id", usageId);
            body.put("charge_quota", amount);
            body.put("metering_ref", "sha256:" + hash(session.get("instance_ref") + ":" + usageId + ":" + start + ":" + endMillis + ":" + snapshot.json));
            body.put("price_snapshot_id", snapshot.priceId); body.put("conversion_version", snapshot.conversionVersion);
            // Funding may only be bound by the backend's verified hold-detail journal. Never rewrite
            // a previously sealed interval when funding is bound, topped up, or recovered later.
            if (session.get("funding_hold_id") != null) {
                if (session.get("funding_hold_intent_id") == null || session.get("funding_expires_at_ms") == null)
                    throw new IllegalStateException("Incomplete persisted capture binding");
                body.put("hold_id", ExactQuota.integer(String.valueOf(session.get("funding_hold_id")), true));
            }
            String request = body.toString(); boolean isTail = stopped != null && stopped.getTime() == endMillis;
            boolean missingFunding = session.get("funding_required") != null && number(session, "funding_required") != 0 && session.get("funding_hold_id") == null;
            boolean expiredFunding = session.get("funding_expires_at_ms") != null && number(session, "funding_expires_at_ms") <= System.currentTimeMillis();
            // An advance expiry warning requests shutdown, but a still-valid hold may settle the
            // actual stop tail. Authorization/conflict risks and actual expiry remain fail-closed.
            boolean stoppingSshFailure="SSH_PREPARATION_FAILED".equals(session.get("risk_code"))
                &&number(session,"stop_requested")==1&&session.get("control_snapshot")!=null;
            boolean blockingRisk = session.get("risk_code") != null && !"HOLD_EXPIRING".equals(session.get("risk_code"))&&!"RUN_WINDOW_EXPIRED".equals(session.get("risk_code"))&&!stoppingSshFailure;
            String state = "0".equals(amount) ? "NO_DEBIT" : blockingRisk || missingFunding || expiredFunding ? "BLOCKED" : "PREPARED";
            String trialError = null;
            String trialDenial = null;
            if (!"0".equals(amount)) {
                try { trial.requireCaptureBudget(usageId, ExactQuota.integer(amount, true)); }
                catch (IllegalStateException denied) {
                    trialDenial = trialError(denied);
                    if (trialDenial == null) throw denied;
                    // Metering continues, but this new interval never acquires spend permission.
                    // Only new INSERTs receive these markers. Existing UNKNOWN/SUBMITTED rows
                    // are never relabeled, so their reservations and lookup obligations survive.
                    trialError = "TRIAL_GROSS_CAP_EXCEEDED".equals(trialDenial)
                        ? "TRIAL_UNALLOCATED_BUDGET" : "TRIAL_UNALLOCATED_PERMIT";
                    state = "BLOCKED";
                }
            }
            if (trialError == null && !"0".equals(amount) && expiredFunding && trial.protectedUsage(usageId)) {
                // This is a newly sealed interval, not an attempted debit. Preserve the true
                // meter and expiry reason without inventing an unresolved capture obligation.
                trialError = "TRIAL_UNALLOCATED_PERMIT";
                trialDenial = "HOLD_EXPIRED";
                state = "BLOCKED";
            }
            jdbc.update("INSERT INTO vls_compute_billing_slice(merchant_charge_id,usage_id,slice_sequence,interval_start_ms,interval_end_ms,is_tail,idempotency_key,request_json,request_hash,amount_text,remainder_numerator,remainder_denominator,state,error_code) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                reference, usageId, sequence, start, endMillis, isTail, key, request, hash(request), amount, next.numerator.toString(), next.denominator.toString(), state,
                trialError != null ? trialError : "BLOCKED".equals(state) && blockingRisk ? session.get("risk_code") : null);
            if (trialError != null)
                jdbc.update("UPDATE vls_compute_billing_session SET stop_requested=1,risk_code=COALESCE(risk_code,?) WHERE usage_id=?", trialDenial, usageId);
            if (trialError == null && !"0".equals(amount) && (missingFunding || expiredFunding)) {
                String error = missingFunding ? "HOLD_BINDING_UNAVAILABLE" : "HOLD_EXPIRED";
                jdbc.update("UPDATE vls_compute_billing_slice SET error_code=? WHERE merchant_charge_id=?", error, reference);
                jdbc.update("UPDATE vls_compute_billing_session SET stop_requested=1,risk_code=COALESCE(risk_code,?) WHERE usage_id=?", error, usageId);
            }
            jdbc.update("UPDATE vls_compute_account_remainder SET numerator_text=?,denominator_text=? WHERE payer_user_id=? AND quota_unit=?",
                next.numerator.toString(), next.denominator.toString(), user, remainderUnit);
            jdbc.update("UPDATE vls_compute_billing_session SET metered_until_ms=?,next_sequence=? WHERE usage_id=?", endMillis, Math.addExact(sequence, 1), usageId);
            return reference;
        });
    }

    /** New work requires current configuration, armed original control and fresh held funds. */
    public void requirePaidAdmission(String usageId) {
        if (!properties.paidConfigured()) throw new IllegalStateException(properties.unavailableReason());
        requireBoundOrigin(usageId);
        if (policy == null) throw new IllegalStateException("平台计费缺配置：billing-policy 停机预算");
        int stopBudget = policy.checkedStopBudgetSeconds(properties.getVerifiedProviderStopSeconds());
        if (properties.getHoldStopLeadSeconds() < stopBudget)
            throw new IllegalStateException("余额到期停机提前量未覆盖配置的停机预算");
        ExternalBillingGateway gateway = gateways.get();
        if (gateway == null || !gateway.supportsAtomicCapture()) throw new IllegalStateException("Paid billing requires verified atomic capture configuration");
        Map<String, Object> session = jdbc.queryForMap("SELECT * FROM vls_compute_billing_session WHERE usage_id=?", usageId);
        if (session.get("risk_code") != null || number(session, "stop_requested") != 0) throw new IllegalStateException("Funding/authorization blocks new work");
        if(!properties.getMerchantId().equals(session.get("merchant_id"))||session.get("control_snapshot")==null)
            throw new IllegalStateException("Paid resource admission requires armed funding and safety-stop integration");
        if(session.get("funding_hold_intent_id")==null||session.get("funding_hold_id")==null||session.get("funding_expires_at_ms")==null
            ||number(session,"funding_expires_at_ms")<=System.currentTimeMillis()+properties.getHoldStopLeadSeconds()*1000L)
            throw new IllegalStateException("Unexpired verified hold required");
        List<Map<String,Object>> observations=jdbc.queryForList("SELECT * FROM vls_compute_funding_hold_observation WHERE hold_intent_id=?",session.get("funding_hold_intent_id"));
        if(observations.size()!=1||!String.valueOf(session.get("funding_hold_id")).equals(String.valueOf(observations.get(0).get("hold_id")))
            ||ExactQuota.integer(String.valueOf(observations.get(0).get("remaining_quota")),false).signum()<=0)
            throw new IllegalStateException("Verified available held quota required");
        if(System.currentTimeMillis()-number(observations.get(0),"observed_at_ms")>30000)
            throw new IllegalStateException("Fresh hold observation required");
    }

    /** Explicit coordinator seam; not registered with any periodic executor in this stage. */
    public void dispatch(String reference) {
        if (!properties.hasMerchantConfiguration()) return;
        ExternalBillingGateway gateway = gateways.get(); if (gateway == null) return;
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("External I/O requires a committed intent");
        String lease = UUID.randomUUID().toString();
        Map<String, Object> row = transactions.execute(tx -> {
            Map<String, Object> candidate = jdbc.queryForMap("SELECT * FROM vls_compute_billing_slice WHERE merchant_charge_id=? FOR UPDATE", reference);
            String merchant = jdbc.queryForObject("SELECT merchant_id FROM vls_compute_billing_session WHERE usage_id=?", String.class, candidate.get("usage_id"));
            if (!properties.getMerchantId().equals(merchant)) return null;
            if (!BillingLedgerOrigin.matchesUsage(jdbc, properties, String.valueOf(candidate.get("usage_id")))) return null;
            String state = String.valueOf(candidate.get("state"));
            if ("SETTLED".equals(state) || "NO_DEBIT".equals(state) || "BLOCKED".equals(state) || number(candidate, "lease_until_ms") > System.currentTimeMillis()) return null;
            jdbc.update("UPDATE vls_compute_billing_slice SET lease_token=?,lease_until_ms=? WHERE merchant_charge_id=?", lease, System.currentTimeMillis() + 30000, reference);
            return candidate;
        });
        if (row == null) return;
        try {
            String body = String.valueOf(row.get("request_json"));
            if (!hash(body).equals(row.get("request_hash"))) { finish(row, lease, "BLOCKED", "SNAPSHOT_CONFLICT", null, true); return; }
            JsonNode request = json.readTree(body);
            ExternalBillingGateway.Result charge = gateway.byReference(reference);
            if (charge.status == ExternalBillingGateway.Status.SUCCEEDED) {
                if (!matchesReference(request, charge.charge)) {
                    finish(row, lease, "BLOCKED", "BUSINESS_REFERENCE_CONFLICT", charge.charge, true); return;
                }
                if (request.has("hold_id") && !charge.charge.has("hold_id")) {
                    // The deployed reference detail omits hold/merchant identity. Only the original
                    // key's successful operation, joined to this exact charge/ledger, can supply it.
                    ExternalBillingGateway.Result proof = gateway.byKey(String.valueOf(row.get("idempotency_key")));
                    if (proof.status == ExternalBillingGateway.Status.SUCCEEDED) {
                        if (!matchesCaptureOperation(row, request, charge.charge, proof.charge)) {
                            finish(row, lease, "BLOCKED", "BUSINESS_REFERENCE_CONFLICT", charge.charge, true); return;
                        }
                    } else {
                        String error = proof.status == ExternalBillingGateway.Status.NOT_AUTHORIZED ? "NOT_AUTHORIZED"
                            : proof.status == ExternalBillingGateway.Status.CONFLICT ? "BUSINESS_REFERENCE_CONFLICT" : "SUBMISSION_UNCERTAIN";
                        finish(row, lease, proof.status == ExternalBillingGateway.Status.CONFLICT ? "BLOCKED" : "UNKNOWN", error, charge.charge, true);
                        return; // A known charge never authorizes another POST, even if the key lookup is absent.
                    }
                }
                finish(row, lease, "SETTLED", null, charge.charge, false); return;
            }
            ExternalBillingGateway.Result operation = gateway.byKey(String.valueOf(row.get("idempotency_key")));
            if (charge.status == ExternalBillingGateway.Status.CONFLICT || operation.status == ExternalBillingGateway.Status.CONFLICT) {
                finish(row, lease, "BLOCKED", "BUSINESS_REFERENCE_CONFLICT", null, true); return;
            }
            if (charge.status == ExternalBillingGateway.Status.NOT_AUTHORIZED || operation.status == ExternalBillingGateway.Status.NOT_AUTHORIZED) {
                finish(row, lease, "UNKNOWN", "NOT_AUTHORIZED", null, true); return;
            }
            if (operation.status == ExternalBillingGateway.Status.PROCESSING) { finish(row, lease, "PROCESSING", null, null, false); return; }
            if (charge.status != ExternalBillingGateway.Status.NOT_FOUND) { finish(row, lease, "UNKNOWN", "LOOKUP_UNAVAILABLE", null, true); return; }
            boolean retry = operation.status == ExternalBillingGateway.Status.NOT_FOUND;
            if (operation.status == ExternalBillingGateway.Status.REJECTED) {
                if ("INSUFFICIENT_HELD_QUOTA".equals(operation.errorCode)) {
                    if (!request.has("hold_id")) { finish(row, lease, "BLOCKED", "HOLD_MISMATCH", null, true); return; }
                    ExternalBillingGateway.Result hold = gateway.holdById(request.path("hold_id").asText());
                    if (hold.status == ExternalBillingGateway.Status.NOT_AUTHORIZED) { finish(row, lease, "UNKNOWN", "NOT_AUTHORIZED", null, true); return; }
                    if (hold.status != ExternalBillingGateway.Status.SUCCEEDED || hold.charge == null) { finish(row, lease, "WAITING_FUNDS", operation.errorCode, null, true); return; }
                    FundingContracts.HoldDetail detail = FundingContracts.holdDetail(hold.charge);
                    if (!matchesHold(request, detail)) { finish(row, lease, "BLOCKED", "HOLD_MISMATCH", null, true); return; }
                    long expires = ExactQuota.integer(detail.expiresAt, true).longValueExact();
                    if (expires <= System.currentTimeMillis() || "EXPIRED".equals(detail.holdStatus)) {
                        if (trial.protectedUsage(String.valueOf(row.get("usage_id"))))
                            denyTrialSubmission(row, lease, "HOLD_EXPIRED", false);
                        else finish(row, lease, "BLOCKED", "HOLD_EXPIRED", null, true);
                        return;
                    }
                    if (ExactQuota.integer(detail.remainingQuota, false)
                        .compareTo(ExactQuota.integer(String.valueOf(row.get("amount_text")), true)) < 0) { finish(row, lease, "WAITING_FUNDS", operation.errorCode, null, true); return; }
                } else if ("CONCURRENT_BALANCE".equals(operation.errorCode)) {
                    // v2.2 explicitly permits the original key/body after this rejected operation.
                    // The reference was confirmed absent above; all original funding guards below still apply.
                } else if ("INSUFFICIENT_AVAILABLE_QUOTA".equals(operation.errorCode) && !request.has("hold_id")) {
                    // Historical ordinary-charge snapshots retain their original recovery semantics.
                    ExternalBillingGateway.Result balance = gateway.balance(request.path("user_id").asText());
                    if (balance.status == ExternalBillingGateway.Status.NOT_AUTHORIZED) { finish(row, lease, "UNKNOWN", "NOT_AUTHORIZED", null, true); return; }
                    if (balance.status != ExternalBillingGateway.Status.SUCCEEDED || balance.charge == null) { finish(row, lease, "WAITING_FUNDS", operation.errorCode, null, true); return; }
                    BigInteger available = ExactQuota.integer(BillingSnapshot.required(balance.charge, "available_quota"), false);
                    if (available.compareTo(ExactQuota.integer(String.valueOf(row.get("amount_text")), true)) < 0) { finish(row, lease, "WAITING_FUNDS", operation.errorCode, null, true); return; }
                } else { finish(row, lease, "BLOCKED", operation.errorCode, null, true); return; }
                retry = true;
            }
            if (!retry) { finish(row, lease, "UNKNOWN", "LOOKUP_UNRESOLVED", null, true); return; }
            if (!gateway.supportsAtomicCapture()) { finish(row, lease, "BLOCKED", "ATOMIC_CAPTURE_UNAVAILABLE", null, true); return; }
            Map<String, Object> session = jdbc.queryForMap("SELECT * FROM vls_compute_billing_session WHERE usage_id=?", row.get("usage_id"));
            if ("NOT_AUTHORIZED".equals(session.get("risk_code"))) { finish(row, lease, "BLOCKED", "NOT_AUTHORIZED", null, true); return; }
            if ("FUNDING_OPERATION_CONFLICT".equals(session.get("risk_code"))) {
                // Preserve the trusted parent hold for read recovery and restricted cleanup, while
                // refusing another debit after contradictory topup/release evidence.
                finish(row, lease, "BLOCKED", "FUNDING_OPERATION_CONFLICT", null, true); return;
            }
            if (session.get("funding_required") != null && number(session, "funding_required") != 0 && !request.has("hold_id")) {
                finish(row, lease, "BLOCKED", "HISTORICAL_FUNDING_UNBOUND", null, true); return;
            }
            if (request.has("hold_id")) {
                if (session.get("funding_hold_intent_id") == null || !request.path("hold_id").asText().equals(String.valueOf(session.get("funding_hold_id")))) {
                    finish(row, lease, "BLOCKED", "HOLD_MISMATCH", null, true); return;
                }
                if (session.get("funding_expires_at_ms") == null || number(session, "funding_expires_at_ms") <= System.currentTimeMillis()) {
                    if (trial.protectedUsage(String.valueOf(row.get("usage_id"))))
                        denyTrialSubmission(row, lease, "HOLD_EXPIRED", operation.status == ExternalBillingGateway.Status.NOT_FOUND);
                    else finish(row, lease, "BLOCKED", "HOLD_EXPIRED", null, true);
                    return;
                }
            }
            // Original-reference/key recovery above remains available after a trial permit changes.
            // Only a new POST needs fresh conversion and a locked gross-budget/refund-phase check.
            boolean protectedTrial = trial.protectedUsage(String.valueOf(row.get("usage_id")));
            JsonNode freshConfig = null;
            if (protectedTrial) {
                try { freshConfig = gateway.config(); }
                catch (RuntimeException unavailable) {
                    denyTrialSubmission(row, lease, "TRIAL_CONFIG_UNAVAILABLE", operation.status == ExternalBillingGateway.Status.NOT_FOUND); return;
                }
            }
            try {
                if (!claimCapture(row, lease, request, freshConfig, protectedTrial)) return;
            } catch (IllegalStateException denied) {
                String error = trialError(denied);
                if (error == null) throw denied;
                denyTrialSubmission(row, lease, error, operation.status == ExternalBillingGateway.Status.NOT_FOUND); return;
            }
            ExternalBillingGateway.Result result = gateway.capture(String.valueOf(row.get("idempotency_key")), body);
            if (result.status == ExternalBillingGateway.Status.SUCCEEDED) {
                // POST acceptance is followed by detail lookup; never settle on an incomplete POST body.
                finish(row, lease, "UNKNOWN", "SUBMISSION_UNCERTAIN".equals(row.get("error_code")) ? "SUBMISSION_UNCERTAIN" : null, null, false);
            } else if (result.status == ExternalBillingGateway.Status.REJECTED && "CONCURRENT_BALANCE".equals(result.errorCode)) {
                // Do not turn a transient, definite rejection into a permanent authorization/funding risk.
                // A later dispatch must query the original reference/key before retrying; never loop POST here.
                finish(row, lease, "UNKNOWN", "SUBMISSION_UNCERTAIN".equals(row.get("error_code")) ? "SUBMISSION_UNCERTAIN" : result.errorCode, null, false);
            } else if (result.status == ExternalBillingGateway.Status.REJECTED
                && ((request.has("hold_id") && "INSUFFICIENT_HELD_QUOTA".equals(result.errorCode))
                || (!request.has("hold_id") && "INSUFFICIENT_AVAILABLE_QUOTA".equals(result.errorCode)))) {
                finish(row, lease, "WAITING_FUNDS", result.errorCode, null, true);
            } else if (result.status == ExternalBillingGateway.Status.NOT_AUTHORIZED) {
                finish(row, lease, "UNKNOWN", "NOT_AUTHORIZED", null, true);
            } else if (result.status == ExternalBillingGateway.Status.REJECTED || result.status == ExternalBillingGateway.Status.CONFLICT) {
                // Explicit errors (including expired conversion versions) cannot change the saved rate/body.
                finish(row, lease, "BLOCKED", result.errorCode, null, true);
            } else { finish(row, lease, "UNKNOWN", "SUBMISSION_UNCERTAIN", null, true); }
        } catch (Exception e) { finish(row, lease, "UNKNOWN", "SUBMISSION_UNCERTAIN", null, true); }
    }

    /** Session lock serializes the durable submission claim with corrective-refund preparation.
     * No external I/O is performed while any usage/session/slice lock is held. */
    private boolean claimCapture(Map<String, Object> original, String lease, JsonNode request,
                                 JsonNode freshConfig, boolean protectedTrial) {
        return Boolean.TRUE.equals(transactions.execute(tx -> {
            String usageId = String.valueOf(original.get("usage_id"));
            usage(usageId);
            Map<String, Object> session = jdbc.queryForMap("SELECT * FROM vls_compute_billing_session WHERE usage_id=? FOR UPDATE", usageId);
            Map<String, Object> current = jdbc.queryForMap("SELECT * FROM vls_compute_billing_slice WHERE merchant_charge_id=? FOR UPDATE", original.get("merchant_charge_id"));
            if (!lease.equals(current.get("lease_token")) || number(current, "lease_until_ms") <= System.currentTimeMillis()
                || "SETTLED".equals(current.get("state")) || "BLOCKED".equals(current.get("state")) || "NO_DEBIT".equals(current.get("state"))) return false;
            if (!original.get("request_json").equals(current.get("request_json"))
                || !original.get("request_hash").equals(current.get("request_hash"))
                || !original.get("idempotency_key").equals(current.get("idempotency_key")))
                throw new IllegalStateException("TRIAL_ORIGINAL_REQUEST_CHANGED");
            if (protectedTrial) {
                trial.requireNewUsage(usageId, freshConfig);
                trial.requireCaptureBudget(usageId, BigInteger.ZERO);
                if (!request.has("hold_id") || session.get("funding_hold_intent_id") == null
                    || !request.path("hold_id").asText().equals(String.valueOf(session.get("funding_hold_id")))
                    || !request.path("user_id").asText().equals(String.valueOf(session.get("payer_user_id")))
                    || !request.path("merchant_id").asText().equals(String.valueOf(session.get("merchant_id")))
                    || !request.path("usage_session_id").asText().equals(usageId)
                    || !request.path("charge_quota").asText().equals(String.valueOf(current.get("amount_text"))))
                    throw new IllegalStateException("TRIAL_CAPTURE_BINDING_MISMATCH");
            }
            // Persist possible submission before the network call. An expired lease or restart
            // must query this exact key/body, and cannot return its gross reservation to the cap.
            return jdbc.update("UPDATE vls_compute_billing_slice SET state='SUBMITTED' WHERE merchant_charge_id=? AND lease_token=?",
                original.get("merchant_charge_id"), lease) == 1;
        }));
    }

    private static String trialError(IllegalStateException failure) {
        String error = failure.getMessage();
        return error != null && error.matches("TRIAL_[A-Z0-9_]{1,56}") ? error : null;
    }

    /** A PREPARED row has never committed a submission claim. Recheck this fact under the
     * original lease before recording never-sent proof; previously attempted rows stay UNKNOWN. */
    private void denyTrialSubmission(Map<String, Object> original, String lease, String reason, boolean originalOperationAbsent) {
        transactions.execute(tx -> {
            String usageId = String.valueOf(original.get("usage_id"));
            usage(usageId);
            jdbc.queryForMap("SELECT * FROM vls_compute_billing_session WHERE usage_id=? FOR UPDATE", usageId);
            Map<String, Object> current = jdbc.queryForMap("SELECT * FROM vls_compute_billing_slice WHERE merchant_charge_id=? FOR UPDATE", original.get("merchant_charge_id"));
            if (!lease.equals(current.get("lease_token"))) return null;
            boolean neverSubmitted = originalOperationAbsent && "PREPARED".equals(original.get("state")) && "PREPARED".equals(current.get("state"))
                && current.get("charge_id") == null && current.get("ledger_entry_id") == null
                && original.get("idempotency_key").equals(current.get("idempotency_key"))
                && original.get("request_json").equals(current.get("request_json"))
                && original.get("request_hash").equals(current.get("request_hash"));
            String error = neverSubmitted ? "TRIAL_GROSS_CAP_EXCEEDED".equals(reason)
                ? "TRIAL_UNALLOCATED_BUDGET" : "TRIAL_UNALLOCATED_PERMIT"
                : "SUBMISSION_UNCERTAIN".equals(original.get("error_code")) ? "SUBMISSION_UNCERTAIN" : reason;
            jdbc.update("UPDATE vls_compute_billing_slice SET state=?,error_code=?,lease_token=NULL,lease_until_ms=0 WHERE merchant_charge_id=? AND lease_token=?",
                neverSubmitted ? "BLOCKED" : "UNKNOWN", error, original.get("merchant_charge_id"), lease);
            jdbc.update("UPDATE vls_compute_billing_session SET stop_requested=1,risk_code=COALESCE(risk_code,?) WHERE usage_id=?", reason, usageId);
            return null;
        });
    }

    /** Read-only local tail/reconciliation view. Provider and external ledger evidence remain separate. */
    public List<Map<String, Object>> audit(String usageId) {
        return jdbc.query("SELECT merchant_charge_id,slice_sequence,interval_start_ms,interval_end_ms,is_tail,amount_text,state,charge_id,ledger_entry_id,error_code,create_time,CASE WHEN lease_token IS NULL THEN 0 ELSE 1 END AS lease_present,lease_until_ms,attempt_count FROM vls_compute_billing_slice WHERE usage_id=? ORDER BY slice_sequence",(rs,index)->{
            Map<String,Object> row=new org.springframework.jdbc.core.ColumnMapRowMapper().mapRow(rs,index);
            java.sql.Timestamp created=rs.getTimestamp("create_time");row.remove("create_time");row.put("create_time_ms",created==null?null:created.getTime());return row;
        },usageId);
    }

    /** Local-only continuation when funding proof arrives after an original charge was settled.
     * Never sends or retries a request, and never withdraws an already requested physical stop. */
    public void resumeResolvedSubmissionRisk(String usageId) {
        if (!properties.hasMerchantConfiguration()) return;
        transactions.execute(tx -> {
            if (!jdbc.queryForList("SELECT merchant_charge_id FROM vls_compute_billing_slice WHERE usage_id=? AND state='SETTLED'", usageId).isEmpty())
                clearResolvedSubmissionRisk(usageId);
            return null;
        });
    }

    private void finish(Map<String, Object> row, String lease, String state, String error, JsonNode charge, boolean stop) {
        transactions.execute(tx -> {
            int changed = jdbc.update("UPDATE vls_compute_billing_slice SET state=?,error_code=?,charge_id=?,ledger_entry_id=?,lease_token=NULL,lease_until_ms=0 WHERE merchant_charge_id=? AND lease_token=?",
                state, error, charge == null ? null : charge.path("charge_id").asText(null), charge == null ? null : charge.path("ledger_entry_id").asText(null), row.get("merchant_charge_id"), lease);
            if (changed == 1 && stop) jdbc.update("UPDATE vls_compute_billing_session SET stop_requested=1,risk_code=CASE WHEN risk_code='NOT_AUTHORIZED' OR (?='SUBMISSION_UNCERTAIN' AND risk_code IS NOT NULL) THEN risk_code ELSE ? END WHERE usage_id=?", error, error, row.get("usage_id"));
            if (changed == 1 && "SETTLED".equals(state) && "SUBMISSION_UNCERTAIN".equals(row.get("error_code")))
                clearResolvedSubmissionRisk(String.valueOf(row.get("usage_id")));
            return null;
        });
    }
    /** A verified original charge resolves only its own transport uncertainty, never a stop request.
     * Other unresolved intents, authorization risks and historical unlabelled blocks remain closed. */
    private void clearResolvedSubmissionRisk(String usageId) {
        Map<String, Object> session = jdbc.queryForMap("SELECT * FROM vls_compute_billing_session WHERE usage_id=? FOR UPDATE", usageId);
        if (!"SUBMISSION_UNCERTAIN".equals(session.get("risk_code"))) return;
        if (!jdbc.queryForList("SELECT merchant_charge_id FROM vls_compute_billing_slice WHERE usage_id=? AND state NOT IN ('SETTLED','NO_DEBIT') AND (state<>'BLOCKED' OR error_code IS NULL OR error_code<>'SUBMISSION_UNCERTAIN')", usageId).isEmpty()) return;
        if (session.get("funding_required") != null && number(session, "funding_required") != 0
            && !jdbc.queryForList("SELECT intent_id FROM vls_compute_funding_intent WHERE usage_id=? AND NOT ((operation_kind='HOLD' AND state='DETAIL_OBSERVED') OR (operation_kind IN ('TOPUP','RELEASE') AND state='OPERATION_CONFIRMED' AND receipt_json IS NOT NULL))", usageId).isEmpty()) return;
        if (jdbc.update("UPDATE vls_compute_billing_session SET risk_code=NULL WHERE usage_id=? AND risk_code='SUBMISSION_UNCERTAIN'", usageId) != 1) return;
        // Only newly recorded blocks carry this provenance. The immutable request, key and amount stay intact.
        jdbc.update("UPDATE vls_compute_billing_slice SET state='PREPARED',error_code=NULL WHERE usage_id=? AND state='BLOCKED' AND error_code='SUBMISSION_UNCERTAIN'", usageId);
    }
    private boolean matchesReference(JsonNode request, JsonNode charge) {
        if (charge == null || !"SUCCEEDED".equals(charge.path("status").asText()) || charge.path("charge_id").asText().isEmpty() || charge.path("ledger_entry_id").asText().isEmpty()) return false;
        if (!charge.path("charged_quota").isTextual()) return false;
        for (String field : new String[]{"merchant_charge_id", "user_id", "usage_session_id", "metering_ref", "price_snapshot_id", "conversion_version"})
            if (!request.path(field).asText().equals(charge.path(field).asText())) return false;
        if (charge.has("merchant_id") && !request.path("merchant_id").asText().equals(charge.path("merchant_id").asText())) return false;
        if (request.has("hold_id") && charge.has("hold_id") && !request.path("hold_id").asText().equals(charge.path("hold_id").asText())) return false;
        return request.path("charge_quota").asText().equals(charge.path("charged_quota").asText());
    }
    private boolean matchesCaptureOperation(Map<String, Object> row, JsonNode request, JsonNode charge, JsonNode operation) {
        if (operation == null || !"SUCCEEDED".equals(operation.path("status").asText())
            || !"CHARGE".equals(operation.path("operation_type").asText())
            || !String.valueOf(row.get("idempotency_key")).equals(operation.path("idempotency_key").asText())) return false;
        for (String field : new String[]{"merchant_id", "merchant_charge_id", "user_id", "usage_session_id", "hold_id"})
            if (!request.path(field).asText().equals(operation.path(field).asText())) return false;
        for (String field : new String[]{"charge_id", "ledger_entry_id"})
            if (!charge.path(field).asText().equals(operation.path(field).asText())) return false;
        return operation.path("charged_quota").isTextual()
            && request.path("charge_quota").asText().equals(operation.path("charged_quota").asText());
    }
    private boolean matchesHold(JsonNode request, FundingContracts.HoldDetail hold) {
        Map<String, Object> session = jdbc.queryForMap("SELECT * FROM vls_compute_billing_session WHERE usage_id=?", request.path("usage_session_id").asText());
        String status = hold.holdStatus;
        return session.get("funding_expires_at_ms") != null
            && String.valueOf(session.get("funding_expires_at_ms")).equals(hold.expiresAt)
            && ("ACTIVE".equals(status) || "PARTIALLY_CAPTURED".equals(status) || "EXPIRED".equals(status))
            && request.path("hold_id").asText().equals(hold.holdId)
            && request.path("user_id").asText().equals(hold.userId)
            && ("vls-hold:" + request.path("usage_session_id").asText() + ":initial").equals(hold.merchantHoldId);
    }
    private Map<String, Object> usage(String id) {
        return jdbc.queryForObject("SELECT * FROM vls_compute_usage WHERE id=? FOR UPDATE", (rs, index) -> {
            Map<String, Object> row = new org.springframework.jdbc.core.ColumnMapRowMapper().mapRow(rs, index);
            row.put("billing_start_at", rs.getTimestamp("billing_start_at")); row.put("billing_end_at", rs.getTimestamp("billing_end_at")); return row;
        }, id);
    }
    private static long number(Map<String, Object> row, String key) { return ((Number) row.get(key)).longValue(); }
    static String hash(String text) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(); for (byte value : bytes) result.append(String.format("%02x", value & 255)); return result.toString();
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 unavailable"); }
    }
}
