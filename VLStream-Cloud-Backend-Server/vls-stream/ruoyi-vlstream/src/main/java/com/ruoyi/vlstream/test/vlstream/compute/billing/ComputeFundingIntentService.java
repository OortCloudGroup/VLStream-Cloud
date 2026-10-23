package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Backend-only immutable funding journal. External I/O is deliberately outside this service.
 * Queries establish resource observations, never the success of an unspecified hold-operation
 * response. A prepared request does not enable paid admission or authorize live funds. */
@Service
public class ComputeFundingIntentService {
    public static final String CONTRACT_BLOCKED = "CONTRACT_BLOCKED";
    public static final String PREPARED = "PREPARED";
    public static final String DETAIL_OBSERVED = "DETAIL_OBSERVED";
    public static final String REJECTED = "REJECTED";
    public static final String UNKNOWN = "UNKNOWN";
    public static final String RECEIPT_UNVERIFIED = "RECEIPT_UNVERIFIED";
    public static final String CONFLICT = "CONFLICT";
    private static final String BASE = "/api/v1/external-billing/holds";
    private static final String CURRENT_CONTRACT = "HOLDS_V3";
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final ObjectMapper json;
    private final Supplier<ComputeRefundIntentService> refunds;

    public ComputeFundingIntentService(JdbcTemplate jdbc, PlatformTransactionManager manager, ObjectMapper json) {
        this(jdbc, manager, json, (Supplier<ComputeRefundIntentService>) () -> null);
    }

    @Autowired
    public ComputeFundingIntentService(JdbcTemplate jdbc, PlatformTransactionManager manager, ObjectMapper json,
                                       ObjectProvider<ComputeRefundIntentService> refunds) {
        this(jdbc, manager, json, (Supplier<ComputeRefundIntentService>) refunds::getIfAvailable);
    }

    public ComputeFundingIntentService(JdbcTemplate jdbc, PlatformTransactionManager manager, ObjectMapper json,
                                       Supplier<ComputeRefundIntentService> refunds) {
        this.jdbc = jdbc;
        this.transactions = new TransactionTemplate(manager);
        this.json = json;
        this.refunds = refunds;
    }

    /** Backend-only preparation against an already verified session. No caller-supplied payer/key.
     * Expiry is stored verbatim; repeating this operation never extends it or creates another hold. */
    public Intent prepareInitialHold(String usageId, String holdQuota, String expiresAt, String reason) {
        return transactions.execute(tx -> {
            Map<String, Object> session = session(usageId);
            String reference = "vls-hold:" + usageId + ":initial";
            ObjectNode body = FundingContracts.hold(text(session, "merchant_id"), reference,
                text(session, "payer_user_id"), holdQuota, expiresAt, reason);
            return prepare(session, "HOLD", reference, null, BASE, body);
        });
    }

    /** Prepare one immutable final release. An existing legacy request is returned byte-for-byte.
     * Oversized release requests are permitted: the upstream contract clips to actual remaining. */
    public Intent prepareFinalRelease(String holdIntentId, String releaseQuota) {
        String usageId = owner(holdIntentId);
        return transactions.execute(tx -> {
            Map<String, Object> session = session(usageId);
            Intent hold = checked(session, row(holdIntentId));
            requireHold(hold);
            String reference = "vls-release:" + usageId + ":final";
            List<Map<String, Object>> prior = byReference(hold.merchantId, reference);
            boolean legacy = !prior.isEmpty() && !CURRENT_CONTRACT.equals(text(prior.get(0), "contract_version"));
            ObjectNode body = legacy ? FundingContracts.release(hold.merchantId, hold.payerUserId, releaseQuota)
                : FundingContracts.release(hold.merchantId, reference, hold.payerUserId, releaseQuota);
            return prepare(session, "RELEASE", reference, hold.id,
                BASE + "/" + hold.holdId + "/release", body);
        });
    }

    /** Each backend-generated topup reference has its own immutable key/body; it never extends expiry. */
    public Intent prepareTopup(String holdIntentId, String merchantTopupId, String quota, String reason, long nowMillis) {
        String usageId = owner(holdIntentId);
        return transactions.execute(tx -> {
            Map<String, Object> session = session(usageId);
            Intent hold = checked(session, row(holdIntentId));
            requireHold(hold);
            ObjectNode body = FundingContracts.topup(hold.merchantId, merchantTopupId, hold.payerUserId, quota, reason);
            List<Map<String, Object>> prior = byReference(hold.merchantId, merchantTopupId);
            if (prior.isEmpty()) {
                Map<String, Object> detail = observation(hold.id);
                if (nowMillis < 0 || nowMillis >= number(detail, "expires_at_ms")
                    || !("ACTIVE".equals(text(detail, "hold_status")) || "PARTIALLY_CAPTURED".equals(text(detail, "hold_status"))))
                    throw new IllegalStateException("Topup requires an unexpired active hold observation; it is not an extension");
            }
            return prepare(session, "TOPUP", merchantTopupId, hold.id, BASE + "/" + hold.holdId + "/topup", body);
        });
    }

    /** Explicit backend recovery step after a refund and a fresh authoritative hold query.
     * It creates an independent release business number instead of replaying final-release. */
    public Intent prepareRefundRelease(String holdIntentId, String refundReference) {
        if (refundReference == null || !refundReference.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}"))
            throw new IllegalArgumentException("Invalid refund recovery reference");
        String usageId = owner(holdIntentId);
        return transactions.execute(tx -> {
            Map<String, Object> session = session(usageId);
            Intent hold = checked(session, row(holdIntentId));
            requireHold(hold);
            ComputeRefundIntentService refundService = refunds.get();
            if (refundService == null) throw new IllegalStateException("A recorded correction refund is required before hold requery recovery");
            ComputeRefundIntentService.Intent refund = refundService.requireHoldRequery(hold.merchantId, refundReference);
            if (!hold.usageId.equals(refund.usageId) || !hold.payerUserId.equals(refund.payerUserId) || !hold.holdId.equals(refund.holdId))
                throw new IllegalArgumentException("Refund does not belong to this held session");
            String reference = "vls-release:refund:" + ComputeBillingSettlementService.hash(hold.id + ":" + refundReference);
            List<Map<String, Object>> existing = byReference(hold.merchantId, reference);
            if (!existing.isEmpty()) return checked(session, existing.get(0));
            Map<String, Object> detail = observation(hold.id);
            if (number(detail, "query_started_at_ms") <= refund.receiptObservedAtMs)
                throw new IllegalStateException("A new authoritative hold query after the refund receipt is required");
            long version = number(detail, "balance_version");
            for (Map<String, Object> prior : jdbc.queryForList("SELECT * FROM vls_compute_funding_intent WHERE parent_intent_id=? AND operation_kind='RELEASE' FOR UPDATE", hold.id)) {
                checked(session, prior);
                if (text(prior, "receipt_json") == null || CONFLICT.equals(text(prior, "state"))
                    || prior.get("hold_balance_version") == null || version <= number(prior, "hold_balance_version"))
                    throw new IllegalStateException("A fresh hold query after prior release/refund is required; unresolved releases cannot be bypassed");
            }
            String remaining = text(detail, "remaining_quota");
            ExactQuota.integer(remaining, true);
            ObjectNode body = FundingContracts.release(hold.merchantId, reference, hold.payerUserId, remaining);
            return prepare(session, "RELEASE", reference, hold.id, BASE + "/" + hold.holdId + "/release", body);
        });
    }

    /** Preserve current hold counters/version from the documented detail query. Ownership is
     * checked against the immutable initial request; mismatches block and clear future capture. */
    public Intent recordHoldDetail(String holdIntentId, String expectedRequestHash, JsonNode response) {
        return recordHoldDetail(holdIntentId, expectedRequestHash, response, 0);
    }

    /** queryStartedAtMs is captured by the backend transport immediately before issuing GET.
     * Manual/imported observations use zero and cannot prove a query happened after a refund. */
    public Intent recordHoldDetail(String holdIntentId, String expectedRequestHash, JsonNode response, long queryStartedAtMs) {
        if (queryStartedAtMs < 0 || queryStartedAtMs > System.currentTimeMillis())
            throw new IllegalArgumentException("Invalid backend hold query start time");
        FundingContracts.HoldDetail detail = FundingContracts.holdDetail(response);
        String usageId = owner(holdIntentId);
        return transactions.execute(tx -> {
            Map<String, Object> session = session(usageId);
            Intent hold = checked(session, row(holdIntentId));
            requireHash(hold, expectedRequestHash);
            if (!"HOLD".equals(hold.operation)) throw new IllegalArgumentException("Hold detail must bind the original hold intent");
            if (CONFLICT.equals(hold.state)) return hold;
            ObjectNode canonical = canonicalDetail(detail);
            String evidence = canonical.toString();
            if (!hold.reference.equals(detail.merchantHoldId) || !hold.payerUserId.equals(detail.userId)
                || !requiredText(parse(hold.body), "expires_at").equals(detail.expiresAt)
                || (hold.holdId != null && !hold.holdId.equals(detail.holdId))) {
                return conflict(session, hold, "HOLD_DETAIL_MISMATCH", evidence);
            }
            List<Map<String, Object>> previous = jdbc.queryForList("SELECT * FROM vls_compute_funding_hold_observation WHERE hold_intent_id=? FOR UPDATE", hold.id);
            long version = ExactQuota.integer(detail.balanceVersion, false).longValueExact();
            if (previous.isEmpty() && !requiredText(parse(hold.body), "hold_quota").equals(detail.holdQuota))
                return conflict(session, hold, "HOLD_INITIAL_AMOUNT_MISMATCH", evidence);
            if (!previous.isEmpty()) {
                Map<String, Object> prior = previous.get(0);
                if (version < number(prior, "balance_version")) return hold;
                if (version == number(prior, "balance_version")) {
                    if (!evidence.equals(text(prior, "detail_json"))) {
                        jdbc.update("UPDATE vls_compute_funding_hold_observation SET conflicting_detail_json=? WHERE hold_intent_id=?", evidence, hold.id);
                        return conflict(session, hold, "HOLD_VERSION_CONFLICT", evidence);
                    }
                    if (queryStartedAtMs > number(prior, "query_started_at_ms"))
                        jdbc.update("UPDATE vls_compute_funding_hold_observation SET query_started_at_ms=?,observed_at_ms=? WHERE hold_intent_id=?",
                            queryStartedAtMs, System.currentTimeMillis(), hold.id);
                    return hold;
                }
                // Refunds can reduce captured and increase remaining. Released and total are monotonic.
                if (ExactQuota.integer(detail.holdQuota, false).compareTo(ExactQuota.integer(text(prior, "hold_quota"), false)) < 0
                    || ExactQuota.integer(detail.releasedQuota, false).compareTo(ExactQuota.integer(text(prior, "released_quota"), false)) < 0)
                    return conflict(session, hold, "HOLD_COUNTER_CONFLICT", evidence);
                jdbc.update("DELETE FROM vls_compute_funding_hold_observation WHERE hold_intent_id=?", hold.id);
            }
            jdbc.update("INSERT INTO vls_compute_funding_hold_observation(hold_intent_id,hold_id,hold_quota,captured_quota,released_quota,remaining_quota,hold_status,balance_version,expires_at_ms,detail_json,detail_hash,query_started_at_ms,observed_at_ms) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                hold.id, detail.holdId, detail.holdQuota, detail.capturedQuota, detail.releasedQuota, detail.remainingQuota,
                detail.holdStatus, version, ExactQuota.integer(detail.expiresAt, true).longValueExact(), evidence,
                ComputeBillingSettlementService.hash(evidence), queryStartedAtMs, System.currentTimeMillis());
            jdbc.update("UPDATE vls_compute_funding_intent SET state=?,hold_id=?,error_code='HOLD_DETAIL_OBSERVED' WHERE intent_id=?", DETAIL_OBSERVED, detail.holdId, hold.id);
            jdbc.update("UPDATE vls_compute_billing_session SET funding_required=1,funding_hold_intent_id=?,funding_hold_id=?,funding_expires_at_ms=? WHERE usage_id=?",
                hold.id, detail.holdId, ExactQuota.integer(detail.expiresAt, true).longValueExact(), usageId);
            return checked(session, row(hold.id));
        });
    }

    /** Resource counters cannot establish whether one particular TOPUP/RELEASE committed.
     * Until the operation-query response is specified, recovery exposes paths and remains closed. */
    public Recovery recoveryPlan(String intentId) {
        Intent intent = recover(intentId);
        String resourcePath = "HOLD".equals(intent.operation) ? BASE + "/by-reference/" + intent.reference
            : BASE + "/" + recover(intent.parentId).holdId;
        return new Recovery(intent, resourcePath, "/api/v1/external-billing/hold-operations/" + intent.operation + "/" + intent.reference);
    }

    /** Persist uncertainty before external I/O. A process crash cannot make another worker send
     * a fresh request: only the first PREPARED caller receives a claim; recovery is query-first. */
    public Intent claimInitialSend(String intentId, long nowMillis) {
        String usageId = owner(intentId);
        return transactions.execute(tx -> {
            Map<String, Object> session = session(usageId);
            Intent intent = checked(session, row(intentId));
            if (!PREPARED.equals(intent.state) || !CURRENT_CONTRACT.equals(intent.contractVersion)) return null;
            if (!"RELEASE".equals(intent.operation) && ("FUNDING_OPERATION_CONFLICT".equals(text(session, "risk_code"))
                || "NOT_AUTHORIZED".equals(text(session, "risk_code")))) return null;
            if ("HOLD".equals(intent.operation)) {
                if (nowMillis >= ExactQuota.integer(requiredText(parse(intent.body), "expires_at"), true).longValueExact()) return null;
            } else if ("TOPUP".equals(intent.operation)) {
                if (nowMillis >= number(observation(intent.parentId), "expires_at_ms")) return null;
            }
            jdbc.update("UPDATE vls_compute_funding_intent SET state=?,error_code='DISPATCH_OUTCOME_PENDING' WHERE intent_id=?", UNKNOWN, intent.id);
            return checked(session, row(intent.id));
        });
    }

    /** Recover the exact persisted original request. This is NOT permission to send or retry it. */
    public Intent recover(String intentId) {
        String usageId = owner(intentId);
        return transactions.execute(tx -> checked(session(usageId), row(intentId)));
    }

    /** Used by an isolated fake or a future verified transport; no transport is wired today.
     * A late timeout cannot erase an earlier receipt. Conflicts stay sticky for manual reconciliation. */
    public Intent recordUnknown(String intentId, String expectedRequestHash) {
        String usageId = owner(intentId);
        return transactions.execute(tx -> {
            Map<String, Object> session = session(usageId);
            Intent original = checked(session, row(intentId));
            requireHash(original, expectedRequestHash);
            if (CONTRACT_BLOCKED.equals(original.state) || PREPARED.equals(original.state) || UNKNOWN.equals(original.state))
                jdbc.update("UPDATE vls_compute_funding_intent SET state=?,error_code='RESULT_UNCERTAIN' WHERE intent_id=?", UNKNOWN, intentId);
            return checked(session, row(intentId));
        });
    }

    public Intent recordFailure(String intentId, String expectedRequestHash, ExternalBillingGateway.Result result) {
        if (result == null || (result.status != ExternalBillingGateway.Status.CONFLICT
            && result.status != ExternalBillingGateway.Status.REJECTED && result.status != ExternalBillingGateway.Status.NOT_AUTHORIZED))
            return recordUnknown(intentId, expectedRequestHash);
        String usageId = owner(intentId);
        return transactions.execute(tx -> {
            Map<String, Object> session = session(usageId);
            Intent intent = checked(session, row(intentId)); requireHash(intent, expectedRequestHash);
            if (!UNKNOWN.equals(intent.state) && !PREPARED.equals(intent.state)) return intent;
            String code = result.errorCode != null && result.errorCode.matches("[A-Z][A-Z0-9_]{0,63}") ? result.errorCode : "FUNDING_REJECTED";
            if (result.status == ExternalBillingGateway.Status.CONFLICT && "HOLD".equals(intent.operation))
                return conflict(session, intent, code, null);
            jdbc.update("UPDATE vls_compute_funding_intent SET state=?,error_code=? WHERE intent_id=?",
                result.status == ExternalBillingGateway.Status.CONFLICT ? CONFLICT : REJECTED, code, intentId);
            jdbc.update("UPDATE vls_compute_billing_session SET risk_code=?,stop_requested=1 WHERE usage_id=?",
                result.status == ExternalBillingGateway.Status.CONFLICT ? "FUNDING_OPERATION_CONFLICT"
                    : result.status == ExternalBillingGateway.Status.NOT_AUTHORIZED ? "NOT_AUTHORIZED" : code, usageId);
            return checked(session, row(intentId));
        });
    }

    /** Record sanitized contract evidence without claiming externally verified success.
     * Repeated equivalent receipts are idempotent; contradictory receipts preserve both observations. */
    public Intent recordReceipt(String intentId, String expectedRequestHash, JsonNode response) {
        String usageId = owner(intentId);
        return transactions.execute(tx -> {
            Map<String, Object> session = session(usageId);
            Intent original = checked(session, row(intentId));
            requireHash(original, expectedRequestHash);
            ObjectNode canonical = json.createObjectNode();
            String receiptHoldId;
            if ("TOPUP".equals(original.operation)) {
                FundingContracts.TopupReceipt receipt = FundingContracts.topupReceipt(response);
                receiptHoldId = receipt.holdId;
                canonical.put("hold_id", receipt.holdId); canonical.put("hold_quota", receipt.holdQuota);
                canonical.put("captured_quota", receipt.capturedQuota); canonical.put("released_quota", receipt.releasedQuota);
                canonical.put("remaining_quota", receipt.remainingQuota); canonical.put("hold_status", receipt.holdStatus);
                canonical.put("balance_version", receipt.balanceVersion);
            } else if ("RELEASE".equals(original.operation) && CURRENT_CONTRACT.equals(original.contractVersion)) {
                FundingContracts.ReleaseReceipt receipt = FundingContracts.releaseResult(response);
                receiptHoldId = receipt.holdId;
                canonical.put("hold_id", receipt.holdId); canonical.put("held_quota", receipt.heldQuota);
                canonical.put("released_quota", receipt.releasedQuota); canonical.put("remaining_quota", receipt.remainingQuota);
                canonical.put("hold_status", receipt.holdStatus);
                if (ExactQuota.integer(receipt.releasedQuota, false).compareTo(ExactQuota.integer(requiredText(parse(original.body), "release_quota"), true)) > 0)
                    throw new IllegalArgumentException("Actual released quota exceeds immutable requested amount");
            } else {
                FundingContracts.Receipt receipt = "HOLD".equals(original.operation)
                    ? FundingContracts.holdReceipt(response) : FundingContracts.releaseReceipt(response);
                receiptHoldId = receipt.holdId;
                if (receipt.holdId != null) canonical.put("hold_id", receipt.holdId);
                if (receipt.availableQuota != null) canonical.put("available_quota", receipt.availableQuota);
                canonical.put("held_quota", receipt.heldQuota);
            }
            String evidence = canonical.toString();
            if (CONFLICT.equals(original.state)) return original;
            if ("HOLD".equals(original.operation) && original.holdId != null && !original.holdId.equals(receiptHoldId))
                return conflict(session, original, "RECEIPT_HOLD_MISMATCH", evidence);
            if (!"HOLD".equals(original.operation) && receiptHoldId != null
                && !receiptHoldId.equals(text(row(original.parentId), "hold_id"))) {
                return operationConflict(session, original, "RECEIPT_HOLD_MISMATCH", evidence);
            }
            if (original.receipt != null && !parse(original.receipt).equals(canonical)) {
                if ("HOLD".equals(original.operation)) return conflict(session, original, "RECEIPT_CONFLICT", evidence);
                return operationConflict(session, original, "RECEIPT_CONFLICT", evidence);
            } else if (original.receipt == null) {
                jdbc.update("UPDATE vls_compute_funding_intent SET state=?,error_code='CONTRACT_UNVERIFIED',hold_id=?,receipt_json=? WHERE intent_id=?",
                    DETAIL_OBSERVED.equals(original.state) ? DETAIL_OBSERVED : RECEIPT_UNVERIFIED,
                    "HOLD".equals(original.operation) ? receiptHoldId : original.holdId, evidence, intentId);
            }
            return checked(session, row(intentId));
        });
    }

    private Intent prepare(Map<String, Object> session, String kind, String reference, String parentId, String path, ObjectNode body) {
        String usageId = text(session, "usage_id");
        List<Map<String, Object>> prior = byReference(text(session, "merchant_id"), reference);
        String request = body.toString();
        if (!prior.isEmpty()) {
            Intent existing = checked(session, prior.get(0));
            if (!reference.equals(existing.reference) || !same(parentId, existing.parentId)
                || !path.equals(existing.path) || !request.equals(existing.body))
                throw new IllegalArgumentException("Immutable funding request conflict");
            return existing;
        }
        String id = UUID.randomUUID().toString();
        Long version = parentId == null ? null : observedVersion(parentId);
        jdbc.update("INSERT INTO vls_compute_funding_intent(intent_id,usage_id,operation_kind,business_reference,merchant_id,payer_user_id,parent_intent_id,idempotency_key,request_path,request_json,request_hash,state,contract_version,hold_balance_version) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            id, usageId, kind, reference, text(session, "merchant_id"), text(session, "payer_user_id"), parentId,
            UUID.randomUUID().toString(), path, request, ComputeBillingSettlementService.hash(request), PREPARED, CURRENT_CONTRACT, version);
        return checked(session, row(id));
    }

    private Intent checked(Map<String, Object> session, Map<String, Object> row) {
        Intent intent = new Intent(row);
        if (!intent.usageId.equals(text(session, "usage_id")) || !intent.merchantId.equals(text(session, "merchant_id"))
            || !intent.payerUserId.equals(text(session, "payer_user_id")) || !intent.key.matches("[0-9a-f-]{36}")
            || !ComputeBillingSettlementService.hash(intent.body).equals(intent.requestHash))
            throw new IllegalStateException("Funding request integrity/ownership conflict");
        JsonNode stored = parse(intent.body);
        ObjectNode expected;
        if ("HOLD".equals(intent.operation)) {
            expected = FundingContracts.hold(intent.merchantId, "vls-hold:" + intent.usageId + ":initial", intent.payerUserId,
                requiredText(stored, "hold_quota"), requiredText(stored, "expires_at"), requiredText(stored, "reason"));
            if (intent.parentId != null || !BASE.equals(intent.path) || !intent.reference.equals(expected.path("merchant_hold_id").asText()))
                throw new IllegalStateException("Invalid hold association");
        } else if ("RELEASE".equals(intent.operation) || "TOPUP".equals(intent.operation)) {
            Map<String, Object> parentRow = row(intent.parentId);
            if (!"HOLD".equals(text(parentRow, "operation_kind"))) throw new IllegalStateException("Release parent must be a hold");
            Intent parent = checked(session, parentRow);
            String action = "TOPUP".equals(intent.operation) ? "topup" : "release";
            if (parent.holdId == null || !intent.path.equals(BASE + "/" + parent.holdId + "/" + action))
                throw new IllegalStateException("Invalid funding operation association");
            if (CONFLICT.equals(parent.state) && !CONFLICT.equals(intent.state)) {
                jdbc.update("UPDATE vls_compute_funding_intent SET state=?,error_code='PARENT_HOLD_CONFLICT' WHERE intent_id=?", CONFLICT, intent.id);
                intent = new Intent(row(intent.id));
            }
            if ("TOPUP".equals(intent.operation)) {
                expected = FundingContracts.topup(intent.merchantId, intent.reference, intent.payerUserId,
                    requiredText(stored, "topup_quota"), requiredText(stored, "reason"));
            } else if (CURRENT_CONTRACT.equals(intent.contractVersion)) {
                expected = FundingContracts.release(intent.merchantId, intent.reference, intent.payerUserId, requiredText(stored, "release_quota"));
            } else {
                if (!intent.reference.equals("vls-release:" + intent.usageId + ":final")) throw new IllegalStateException("Invalid legacy release reference");
                expected = FundingContracts.release(intent.merchantId, intent.payerUserId, requiredText(stored, "release_quota"));
            }
        } else throw new IllegalStateException("Unknown funding operation");
        if (!expected.toString().equals(intent.body) || !(CONTRACT_BLOCKED.equals(intent.state) || PREPARED.equals(intent.state)
            || DETAIL_OBSERVED.equals(intent.state) || UNKNOWN.equals(intent.state) || REJECTED.equals(intent.state)
            || RECEIPT_UNVERIFIED.equals(intent.state) || CONFLICT.equals(intent.state)))
            throw new IllegalStateException("Invalid immutable funding request/state");
        if (intent.receipt != null) {
            JsonNode receipt = parse(intent.receipt);
            if (!"TOPUP".equals(intent.operation)) ExactQuota.integer(requiredText(receipt, "held_quota"), false);
            if ("HOLD".equals(intent.operation)) {
                ExactQuota.integer(requiredText(receipt, "available_quota"), false);
                if (!requiredText(receipt, "hold_id").equals(intent.holdId)) throw new IllegalStateException("Hold receipt binding mismatch");
                ExactQuota.integer(intent.holdId, true);
            } else {
                if (receipt.has("available_quota")) ExactQuota.integer(requiredText(receipt, "available_quota"), false);
                if (receipt.has("hold_id") && !requiredText(receipt, "hold_id").equals(text(row(intent.parentId), "hold_id")))
                    throw new IllegalStateException("Release receipt binding mismatch");
            }
        } else if (RECEIPT_UNVERIFIED.equals(intent.state)) throw new IllegalStateException("Funding receipt missing");
        else if (intent.holdId != null) {
            Map<String, Object> detail = observation(intent.id);
            if (!intent.holdId.equals(text(detail, "hold_id"))) throw new IllegalStateException("Hold detail binding mismatch");
        }
        if (DETAIL_OBSERVED.equals(intent.state)) {
            Map<String, Object> detail = observation(intent.id);
            if (!intent.holdId.equals(text(detail, "hold_id"))) throw new IllegalStateException("Hold detail binding mismatch");
        }
        return intent;
    }

    private void requireHold(Intent hold) {
        if (!"HOLD".equals(hold.operation) || hold.holdId == null || CONFLICT.equals(hold.state))
            throw new IllegalStateException("A non-conflicting observed hold id is required");
    }
    private List<Map<String, Object>> byReference(String merchant, String reference) {
        return jdbc.queryForList("SELECT * FROM vls_compute_funding_intent WHERE merchant_id=? AND business_reference=? FOR UPDATE", merchant, reference);
    }
    private Map<String, Object> observation(String holdIntentId) {
        Map<String, Object> detail = jdbc.queryForMap("SELECT * FROM vls_compute_funding_hold_observation WHERE hold_intent_id=? FOR UPDATE", holdIntentId);
        if (!ComputeBillingSettlementService.hash(text(detail, "detail_json")).equals(text(detail, "detail_hash")))
            throw new IllegalStateException("Hold observation integrity conflict");
        return detail;
    }
    private Long observedVersion(String holdIntentId) {
        List<Map<String, Object>> list = jdbc.queryForList("SELECT balance_version FROM vls_compute_funding_hold_observation WHERE hold_intent_id=? FOR UPDATE", holdIntentId);
        return list.isEmpty() ? null : number(list.get(0), "balance_version");
    }
    private Intent conflict(Map<String, Object> session, Intent hold, String code, String evidence) {
        jdbc.update("UPDATE vls_compute_funding_intent SET state=?,error_code=?,conflicting_receipt_json=? WHERE intent_id=?", CONFLICT, code, evidence, hold.id);
        jdbc.update("UPDATE vls_compute_billing_session SET funding_hold_intent_id=NULL,funding_hold_id=NULL,funding_expires_at_ms=NULL WHERE usage_id=? AND funding_hold_intent_id=?", hold.usageId, hold.id);
        jdbc.update("UPDATE vls_compute_billing_session SET risk_code=?,stop_requested=1 WHERE usage_id=?", code, hold.usageId);
        return checked(session, row(hold.id));
    }
    private Intent operationConflict(Map<String, Object> session, Intent intent, String code, String evidence) {
        jdbc.update("UPDATE vls_compute_funding_intent SET state=?,error_code=?,conflicting_receipt_json=? WHERE intent_id=?",
            CONFLICT, code, evidence, intent.id);
        jdbc.update("UPDATE vls_compute_billing_session SET risk_code='FUNDING_OPERATION_CONFLICT',stop_requested=1 WHERE usage_id=?", intent.usageId);
        return checked(session, row(intent.id));
    }
    private ObjectNode canonicalDetail(FundingContracts.HoldDetail detail) {
        ObjectNode node = json.createObjectNode();
        node.put("hold_id", detail.holdId); node.put("merchant_hold_id", detail.merchantHoldId); node.put("user_id", detail.userId);
        node.put("hold_quota", detail.holdQuota); node.put("captured_quota", detail.capturedQuota); node.put("released_quota", detail.releasedQuota);
        node.put("remaining_quota", detail.remainingQuota); node.put("hold_status", detail.holdStatus); node.put("balance_version", detail.balanceVersion);
        node.put("expires_at", detail.expiresAt); node.put("created_at", detail.createdAt); node.put("updated_at", detail.updatedAt);
        return node;
    }
    private static long number(Map<String, Object> row, String field) {
        return ExactQuota.integer(text(row, field), false).longValueExact();
    }

    private Map<String, Object> session(String id) {
        return jdbc.queryForMap("SELECT * FROM vls_compute_billing_session WHERE usage_id=? FOR UPDATE", id);
    }
    private String owner(String id) {
        return jdbc.queryForObject("SELECT usage_id FROM vls_compute_funding_intent WHERE intent_id=?", String.class, id);
    }
    private Map<String, Object> row(String id) {
        return jdbc.queryForMap("SELECT * FROM vls_compute_funding_intent WHERE intent_id=? FOR UPDATE", id);
    }
    private JsonNode parse(String value) {
        try { return json.readTree(value); }
        catch (Exception e) { throw new IllegalStateException("Invalid funding snapshot JSON"); }
    }
    private static String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual()) throw new IllegalStateException("Funding snapshot requires string " + field);
        return value.textValue();
    }
    private static void requireHash(Intent intent, String expected) {
        if (!intent.requestHash.equals(expected)) throw new IllegalArgumentException("Funding response does not match original request");
    }
    private static String text(Map<String, Object> row, String field) {
        Object value = row.get(field);
        return value == null ? null : String.valueOf(value);
    }
    private static boolean same(String left, String right) { return left == null ? right == null : left.equals(right); }

    /** Immutable view. safeToSend remains false even after a syntactically valid receipt. */
    public static final class Intent {
        public final String id, usageId, operation, reference, merchantId, payerUserId, parentId;
        public final String key, path, body, requestHash, state, holdId, receipt, conflictingReceipt, contractVersion;
        public final boolean safeToSend = false;
        public final String contractBlock = "LIVE_WRITES_REQUIRE_EXPLICIT_DISABLED_BY_DEFAULT_GATE";
        private Intent(Map<String, Object> row) {
            id = text(row, "intent_id"); usageId = text(row, "usage_id"); operation = text(row, "operation_kind");
            reference = text(row, "business_reference"); merchantId = text(row, "merchant_id"); payerUserId = text(row, "payer_user_id");
            parentId = text(row, "parent_intent_id"); key = text(row, "idempotency_key"); path = text(row, "request_path");
            body = text(row, "request_json"); requestHash = text(row, "request_hash"); state = text(row, "state");
            holdId = text(row, "hold_id"); receipt = text(row, "receipt_json"); conflictingReceipt = text(row, "conflicting_receipt_json");
            contractVersion = text(row, "contract_version");
        }
    }

    public static final class Recovery {
        public final Intent intent;
        public final String resourceQueryPath, operationQueryPath;
        public final boolean operationResponseSpecified = false;
        private Recovery(Intent intent, String resourceQueryPath, String operationQueryPath) {
            this.intent = intent; this.resourceQueryPath = resourceQueryPath; this.operationQueryPath = operationQueryPath;
        }
    }
}
