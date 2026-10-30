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

/** Durable VLS-side slice/outbox coordinator. No paid-start enablement.
 * All external I/O follows a committed local intent, preserving its original funding association. */
@Service
public class ComputeBillingSettlementService {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final ObjectMapper json;
    private final ExternalBillingProperties properties;
    private final Supplier<ExternalBillingGateway> gateways;
    private final ComputeBillingTrialGuard trial;

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
        if(!properties.isPaidAdmissionEnabled())throw new IllegalStateException("Paid admission disabled");
        bind(usageId,payer,snapshot,true);
    }

    private void bind(String usageId, BillingPayer payer, BillingSnapshot snapshot, boolean reserved) {
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
        Map<String, Object> usage = jdbc.queryForMap("SELECT rate_snapshot,billing_start_at FROM vls_compute_usage WHERE id=?", usageId);
        try {
            com.ruoyi.vlstream.test.vlstream.compute.ComputeBillingRate rate = json.readValue(String.valueOf(usage.get("rate_snapshot")), com.ruoyi.vlstream.test.vlstream.compute.ComputeBillingRate.class);
            Timestamp start = jdbc.queryForObject("SELECT billing_start_at FROM vls_compute_usage WHERE id=?", (rs, index) -> rs.getTimestamp(1), usageId);
            if (start == null) throw new IllegalArgumentException("Confirmed running usage required");
            bind(usageId, payer, BillingSnapshot.fromExistingRate(rate, authoritativeConfig, start.toInstant(), json));
        } catch (java.io.IOException e) { throw new IllegalArgumentException("Invalid accepted usage rate"); }
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
            jdbc.update("INSERT IGNORE INTO vls_compute_account_remainder(payer_user_id,quota_unit,numerator_text,denominator_text) VALUES(?,?,'0','1')", user, unit);
            Map<String, Object> tail = jdbc.queryForMap("SELECT * FROM vls_compute_account_remainder WHERE payer_user_id=? AND quota_unit=? FOR UPDATE", user, unit);
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
            boolean blockingRisk = session.get("risk_code") != null && !"HOLD_EXPIRING".equals(session.get("risk_code"));
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
                next.numerator.toString(), next.denominator.toString(), user, unit);
            jdbc.update("UPDATE vls_compute_billing_session SET metered_until_ms=?,next_sequence=? WHERE usage_id=?", endMillis, Math.addExact(sequence, 1), usageId);
            return reference;
        });
    }

    /** Off means no I/O at all. Current real client remains unable to authorize a new paid task. */
    public void requirePaidAdmission(String usageId) {
        ExternalBillingGateway gateway = properties.isEnabled() ? gateways.get() : null;
        if (gateway == null || !gateway.supportsAtomicCapture()) throw new IllegalStateException("Paid billing requires verified atomic capture; disabled");
        Map<String, Object> session = jdbc.queryForMap("SELECT * FROM vls_compute_billing_session WHERE usage_id=?", usageId);
        if (session.get("risk_code") != null || number(session, "stop_requested") != 0) throw new IllegalStateException("Funding/authorization blocks new work");
        if(!properties.isPaidAdmissionEnabled()||!properties.isSafetyStopEnabled()||session.get("control_snapshot")==null)
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
        if (!properties.isEnabled()) return;
        ExternalBillingGateway gateway = gateways.get(); if (gateway == null) return;
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("External I/O requires a committed intent");
        String lease = UUID.randomUUID().toString();
        Map<String, Object> row = transactions.execute(tx -> {
            Map<String, Object> candidate = jdbc.queryForMap("SELECT * FROM vls_compute_billing_slice WHERE merchant_charge_id=? FOR UPDATE", reference);
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
                if (!properties.isAllowWrites()) throw new IllegalStateException("TRIAL_WRITES_DISABLED");
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
        return jdbc.queryForList("SELECT merchant_charge_id,interval_start_ms,interval_end_ms,is_tail,amount_text,state,charge_id,ledger_entry_id,error_code FROM vls_compute_billing_slice WHERE usage_id=? ORDER BY slice_sequence", usageId);
    }

    /** Local-only continuation when funding proof arrives after an original charge was settled.
     * Never sends or retries a request, and never withdraws an already requested physical stop. */
    public void resumeResolvedSubmissionRisk(String usageId) {
        if (!properties.isEnabled()) return;
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
