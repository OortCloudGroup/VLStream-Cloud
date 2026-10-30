package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/** Backend-only corrective refunds. Unknown results never trigger a new key or an assumed refund lookup. */
@Service
public class ComputeRefundIntentService {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final ObjectMapper json;
    private final ExternalBillingProperties properties;
    private final Supplier<ExternalBillingGateway> gateways;
    private final ComputeBillingTrialGuard trial;

    @Autowired
    public ComputeRefundIntentService(JdbcTemplate jdbc, PlatformTransactionManager manager, ObjectMapper json,
                                      ExternalBillingProperties properties, ObjectProvider<ExternalBillingGateway> gateways) {
        this(jdbc, manager, json, properties, (Supplier<ExternalBillingGateway>) gateways::getIfAvailable);
    }

    public ComputeRefundIntentService(JdbcTemplate jdbc, PlatformTransactionManager manager, ObjectMapper json,
                                      ExternalBillingProperties properties, Supplier<ExternalBillingGateway> gateways) {
        this.jdbc = jdbc;
        this.transactions = new TransactionTemplate(manager);
        this.json = json;
        this.properties = properties;
        this.gateways = gateways;
        this.trial = new ComputeBillingTrialGuard(jdbc, json, properties);
    }

    /** Internal audited correction reference, never a browser-selected payer or destination wallet.
     * All prepared or uncertain intents reserve their requested amount; rejection does not silently free it. */
    public Intent prepare(String merchantChargeId, String correctionReference, String refundQuota, String reasonCode) {
        RefundContracts.reference(correctionReference);
        ExactQuota.integer(refundQuota, true);
        String usageId = chargeOwner(merchantChargeId);
        return transactions.execute(tx -> {
            Map<String, Object> session = session(usageId);
            Map<String, Object> charge = charge(merchantChargeId);
            JsonNode original = verifiedOriginal(session, charge);
            String reference = "vls-refund:" + ComputeBillingSettlementService.hash(merchantChargeId + ":" + correctionReference);
            ObjectNode body = RefundContracts.request(text(session, "merchant_id"), reference,
                text(charge, "ledger_entry_id"), refundQuota, reasonCode);
            List<Map<String, Object>> prior = jdbc.queryForList("SELECT * FROM vls_compute_refund_intent WHERE merchant_charge_id=? AND correction_reference=? FOR UPDATE",
                merchantChargeId, correctionReference);
            if (!prior.isEmpty()) {
                Intent existing = checked(session, charge, prior.get(0));
                if (!existing.body.equals(body.toString())) throw new IllegalArgumentException("Immutable corrective-refund request conflict");
                return existing;
            }
            trial.requireCleanupScope(usageId);
            if (trial.protectedUsage(usageId) && !jdbc.queryForList(
                "SELECT merchant_charge_id FROM vls_compute_billing_slice WHERE usage_id=? AND (state IN ('SUBMITTED','UNKNOWN','PROCESSING') OR lease_until_ms>?)",
                usageId, System.currentTimeMillis()).isEmpty())
                throw new IllegalStateException("TRIAL_CAPTURE_RECONCILIATION_REQUIRED");
            // This same session lock is taken before a new capture becomes SUBMITTED. Once a
            // corrective refund exists, the trial guard permanently rejects further capture POSTs.
            // Refund receipts (including held-quota restoration) never reduce gross spend reservations.
            BigInteger reserved = BigInteger.ZERO;
            for (String amount : jdbc.queryForList("SELECT refund_quota FROM vls_compute_refund_intent WHERE merchant_charge_id=?", String.class, merchantChargeId))
                reserved = reserved.add(ExactQuota.integer(amount, true));
            if (reserved.add(ExactQuota.integer(refundQuota, true)).compareTo(ExactQuota.integer(original.path("charge_quota").asText(), true)) > 0)
                throw new IllegalArgumentException("Corrective refunds exceed the original charge");
            String id = UUID.randomUUID().toString();
            String request = body.toString();
            jdbc.update("INSERT INTO vls_compute_refund_intent(refund_intent_id,merchant_charge_id,correction_reference,merchant_refund_id,usage_id,merchant_id,payer_user_id,charge_id,original_ledger_entry_id,original_request_hash,original_hold_id,idempotency_key,request_path,request_json,request_hash,refund_quota,state) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'PREPARED')",
                id, merchantChargeId, correctionReference, reference, usageId, text(session, "merchant_id"), text(session, "payer_user_id"),
                text(charge, "charge_id"), text(charge, "ledger_entry_id"), text(charge, "request_hash"), original.has("hold_id") ? original.path("hold_id").asText() : null,
                "vls-refund-op-" + UUID.randomUUID(),
                path(text(charge, "charge_id")), request, ComputeBillingSettlementService.hash(request), refundQuota);
            return checked(session, charge, row(id));
        });
    }

    public Intent recover(String intentId) {
        String usageId = owner(intentId);
        return transactions.execute(tx -> {
            Map<String, Object> session = session(usageId);
            Map<String, Object> stored = row(intentId);
            return checked(session, charge(text(stored, "merchant_charge_id")), stored);
        });
    }

    /** Explicit original-intent lookup only: no scheduler, writes, new keys or inferred POST receipt.
     * Each creation-time window is bounded to one day and the entire lookup to ten ledger pages.
     * Delayed records outside these windows remain unknown instead of scanning the merchant's history. */
    public Intent reconcile(String intentId) {
        Intent original = recover(intentId);
        if (!properties.isEnabled() || !"UNKNOWN".equals(original.state) || original.receipt != null || original.holdId == null)
            return original;
        if (TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Refund lookup requires a committed intent");
        if (!original.merchantId.equals(properties.getMerchantId())) return original;
        ExternalBillingGateway gateway = gateways.get();
        if (gateway == null) return original;
        try {
            ExternalBillingGateway.Result operation = gateway.byKey(original.key);
            if (operation.status != ExternalBillingGateway.Status.SUCCEEDED || operation.charge == null) return recover(intentId);
            JsonNode op = operation.charge;
            if (!original.key.equals(op.path("idempotency_key").asText()) || !"REFUND".equals(op.path("operation_type").asText())
                || !"SUCCEEDED".equals(op.path("status").asText())) return recover(intentId);
            String refundId = ExactQuota.integer(op.path("refund_id").asText(), true).toString();
            List<long[]> windows = transactions.execute(tx -> {
                Map<String, Object> session = session(original.usageId);
                Map<String, Object> saved = row(intentId);
                Map<String, Object> charge = charge(original.chargeReference);
                Intent current = checked(session, charge, saved);
                requireHash(current, original.requestHash);
                if (!"UNKNOWN".equals(current.state) || current.receipt != null) return null;
                long now = System.currentTimeMillis() / 1000 + 1;
                long first = createdSecond(charge), second = createdSecond(saved);
                List<long[]> result = new ArrayList<>();
                long begin = Math.max(0, Math.min(first, second) - 60);
                if (now <= begin) return null;
                if (now - begin <= 86400) result.add(new long[]{begin, now});
                else {
                    result.add(new long[]{Math.max(0, first - 60), Math.min(now, first + 86340)});
                    if (second != first) result.add(new long[]{Math.max(0, second - 60), Math.min(now, second + 86340)});
                }
                return result;
            });
            if (windows == null) return recover(intentId);
            JsonNode debit = null, credit = null;
            int pages = 0;
            for (long[] window : windows) {
                String cursor = null; Set<String> cursors = new HashSet<>();
                while (true) {
                    if (++pages > 10) return recover(intentId);
                    ExternalBillingGateway.Result page = gateway.ledger(window[0], window[1], cursor);
                    if (page.status != ExternalBillingGateway.Status.SUCCEEDED || page.charge == null) return recover(intentId);
                    JsonNode body = page.charge;
                    if (!body.path("entries").isArray() || body.path("entries").size() > 100 || !body.path("has_more").isBoolean())
                        return recover(intentId);
                    for (JsonNode entry : body.path("entries")) {
                        if (original.originalLedgerId.equals(entry.path("ledger_entry_id").asText())
                            || original.chargeReference.equals(entry.path("merchant_ref").asText())) {
                            requireWindow(entry, window);
                            if (debit != null && !debit.equals(entry)) return reconcileConflict(intentId, original, op, debit, entry);
                            debit = entry.deepCopy();
                        }
                        if (original.reference.equals(entry.path("merchant_ref").asText())
                            || "refund".equals(entry.path("reference_type").asText()) && refundId.equals(entry.path("reference_id").asText())) {
                            requireWindow(entry, window);
                            if (credit != null && !credit.equals(entry)) return reconcileConflict(intentId, original, op, credit, entry);
                            credit = entry.deepCopy();
                        }
                    }
                    if (!body.path("has_more").booleanValue()) break;
                    String next = body.path("next_cursor").asText();
                    if (next.isEmpty() || next.length() > 200 || !cursors.add(next)) return recover(intentId);
                    cursor = next;
                }
            }
            if (debit == null || credit == null) return recover(intentId);
            ObjectNode proof = RefundContracts.reconciled(op, debit, credit);
            return recordReconciled(intentId, original, proof);
        } catch (Exception unavailableOrUnknownSchema) {
            return recover(intentId);
        }
    }

    private Intent recordReconciled(String intentId, Intent original, ObjectNode proof) {
        return transactions.execute(tx -> {
            Map<String, Object> session = session(original.usageId);
            Map<String, Object> stored = row(intentId);
            Map<String, Object> charge = charge(original.chargeReference);
            Intent current = checked(session, charge, stored);
            requireHash(current, original.requestHash);
            if (!current.merchantId.equals(properties.getMerchantId()) || !("UNKNOWN".equals(current.state)
                || "RECEIPT_UNVERIFIED".equals(current.state) || "LEDGER_RECONCILED".equals(current.state)))
                return current;
            boolean compatible = current.receipt == null || ("LEDGER_RECONCILED".equals(current.state)
                ? parse(current.receipt).equals(proof) : crossSchemaEquivalent(proof, parse(current.receipt)));
            if (!matchesProof(current, charge, proof) || !compatible) {
                jdbc.update("UPDATE vls_compute_refund_intent SET state='CONFLICT',error_code='REFUND_LEDGER_CONFLICT',conflicting_receipt_json=? WHERE refund_intent_id=?",
                    proof.toString(), intentId);
            } else if (current.receipt == null) {
                jdbc.update("UPDATE vls_compute_refund_intent SET state='LEDGER_RECONCILED',error_code='NEEDS_HOLD_REQUERY',receipt_json=?,receipt_observed_at_ms=? WHERE refund_intent_id=?",
                    proof.toString(), System.currentTimeMillis(), intentId);
            }
            return checked(session, charge, row(intentId));
        });
    }

    private Intent reconcileConflict(String intentId, Intent original, JsonNode operation, JsonNode first, JsonNode second) {
        ObjectNode conflict = json.createObjectNode(); conflict.put("kind", "REFUND_LEDGER_CONFLICT");
        conflict.set("operation", diagnosticFields(operation)); conflict.set("first", diagnosticFields(first)); conflict.set("second", diagnosticFields(second));
        return transactions.execute(tx -> {
            Map<String, Object> session = session(original.usageId);
            Map<String, Object> stored = row(intentId);
            Map<String, Object> charge = charge(original.chargeReference);
            Intent current = checked(session, charge, stored);
            requireHash(current, original.requestHash);
            if (current.merchantId.equals(properties.getMerchantId()) && ("UNKNOWN".equals(current.state)
                || "RECEIPT_UNVERIFIED".equals(current.state) || "LEDGER_RECONCILED".equals(current.state)))
                jdbc.update("UPDATE vls_compute_refund_intent SET state='CONFLICT',error_code='REFUND_LEDGER_CONFLICT',conflicting_receipt_json=? WHERE refund_intent_id=?", conflict.toString(), intentId);
            return checked(session, charge, row(intentId));
        });
    }

    private static long createdSecond(Map<String, Object> row) {
        Object value = row.get("create_time");
        if (!(value instanceof java.util.Date)) throw new IllegalStateException("Trusted local creation time required");
        long second = ((java.util.Date) value).getTime() / 1000;
        if (second <= 0 || second > System.currentTimeMillis() / 1000 + 1) throw new IllegalStateException("Invalid creation time");
        return second;
    }

    private boolean matchesProof(Intent intent, Map<String, Object> charge, JsonNode proof) {
        JsonNode debit = proof.path("original_ledger"), credit = proof.path("refund_ledger");
        return intent.key.equals(proof.path("operation").path("idempotency_key").asText())
            && (!proof.path("operation").has("merchant_id") || intent.merchantId.equals(proof.path("operation").path("merchant_id").asText()))
            && intent.chargeId.equals(debit.path("reference_id").asText())
            && intent.originalLedgerId.equals(debit.path("ledger_entry_id").asText())
            && intent.chargeReference.equals(debit.path("merchant_ref").asText())
            && intent.payerUserId.equals(debit.path("user_id").asText())
            && ("-" + text(charge, "amount_text")).equals(debit.path("quota_delta").asText())
            && intent.reference.equals(credit.path("merchant_ref").asText())
            && intent.payerUserId.equals(credit.path("user_id").asText())
            && parse(intent.body).path("refund_quota").asText().equals(credit.path("quota_delta").asText());
    }

    private static boolean crossSchemaEquivalent(JsonNode proof, JsonNode postReceipt) {
        return proof.path("operation").path("refund_id").asText().equals(postReceipt.path("refund_id").asText())
            && proof.path("refund_ledger").path("ledger_entry_id").asText().equals(postReceipt.path("ledger_entry_id").asText())
            && proof.path("refund_ledger").path("quota_delta").asText().equals(postReceipt.path("refunded_quota").asText());
    }

    private static void requireWindow(JsonNode entry, long[] window) {
        long created = java.time.Instant.parse(entry.path("created_at").asText()).getEpochSecond();
        if (created < window[0] || created >= window[1]) throw new IllegalArgumentException("Ledger entry outside fixed query window");
    }

    private ObjectNode diagnosticFields(JsonNode value) {
        ObjectNode result = json.createObjectNode();
        for (String field : new String[]{"idempotency_key", "operation_type", "status", "refund_id", "ledger_entry_id", "user_id",
            "reference_id", "related_entry_id", "merchant_ref", "entry_type", "reference_type", "quota_delta", "balance_before", "balance_after", "created_at"}) {
            JsonNode item = value.get(field);
            if (item != null && item.isValueNode() && item.asText().length() <= 200) result.set(field, item);
        }
        return result;
    }

    /** Local receipt time is only a lower bound for the next hold observation, never refund finality.
     * Consumers must query this same hold after this timestamp before preparing a distinct release. */
    public Intent requireHoldRequery(String expectedMerchant, String merchantRefundId) {
        RefundContracts.reference(expectedMerchant);
        RefundContracts.reference(merchantRefundId);
        String id = jdbc.queryForObject("SELECT refund_intent_id FROM vls_compute_refund_intent WHERE merchant_id=? AND merchant_refund_id=?",
            String.class, expectedMerchant, merchantRefundId);
        Intent intent = recover(id);
        if (!("RECEIPT_UNVERIFIED".equals(intent.state) || "LEDGER_RECONCILED".equals(intent.state)) || !"NEEDS_HOLD_REQUERY".equals(intent.errorCode)
            || intent.receipt == null || intent.holdId == null || intent.receiptObservedAtMs <= 0)
            throw new IllegalStateException("Recorded capture-refund receipt requires a fresh matching hold query");
        return intent;
    }

    /** First submission only. No scheduled executor consumes this journal and paid admission stays closed. */
    public void dispatch(String intentId) {
        if (!properties.isEnabled() || !properties.isAllowWrites()) return;
        ExternalBillingGateway gateway = gateways.get();
        if (gateway == null) return;
        if (TransactionSynchronizationManager.isActualTransactionActive())
            throw new IllegalStateException("Refund I/O requires a committed intent");
        String usageId = owner(intentId);
        Intent original = transactions.execute(tx -> {
            Map<String, Object> session = session(usageId);
            Map<String, Object> stored = row(intentId);
            Intent intent = checked(session, charge(text(stored, "merchant_charge_id")), stored);
            if (!"PREPARED".equals(intent.state)) return null;
            trial.requireCleanupScope(usageId);
            // Commit possible submission before the network write. A crash must recover as UNKNOWN.
            jdbc.update("UPDATE vls_compute_refund_intent SET state='UNKNOWN',error_code='REFUND_RESULT_UNCERTAIN' WHERE refund_intent_id=?", intentId);
            return intent;
        });
        if (original == null) return;
        try {
            ExternalBillingGateway.Result result = gateway.refund(original.chargeId, original.key, original.body);
            if (result.status == ExternalBillingGateway.Status.SUCCEEDED && result.charge != null)
                recordReceipt(intentId, original.requestHash, result.charge);
            else if (result.status == ExternalBillingGateway.Status.REJECTED || result.status == ExternalBillingGateway.Status.CONFLICT)
                recordRejected(intentId, original.requestHash, result.errorCode, result.status == ExternalBillingGateway.Status.CONFLICT);
        } catch (Exception e) {
            // Network failure or malformed acceptance leaves the committed UNKNOWN record intact.
        }
    }

    /** A syntactically valid receipt is correlated to this immutable request; it is not a refund audit.
     * Capture refunds require a fresh hold query and a distinct release if funds returned to held quota. */
    public Intent recordReceipt(String intentId, String expectedRequestHash, JsonNode response) {
        ObjectNode evidence = RefundContracts.receipt(response);
        String usageId = owner(intentId);
        return transactions.execute(tx -> {
            Map<String, Object> session = session(usageId);
            Map<String, Object> stored = row(intentId);
            Intent intent = checked(session, charge(text(stored, "merchant_charge_id")), stored);
            requireHash(intent, expectedRequestHash);
            if ("CONFLICT".equals(intent.state)) return intent;
            JsonNode existing = intent.receipt == null ? null : parse(intent.receipt);
            boolean sameReceipt = existing == null || existing.equals(evidence);
            if ("LEDGER_RECONCILED".equals(intent.state)) {
                // GET evidence and POST receipts intentionally have different schemas. Correlate only
                // the independently observed identifiers and amount; keep the original proof clock.
                sameReceipt = crossSchemaEquivalent(existing, evidence);
            }
            if (!evidence.path("refunded_quota").asText().equals(parse(intent.body).path("refund_quota").asText())
                || !sameReceipt) {
                jdbc.update("UPDATE vls_compute_refund_intent SET state='CONFLICT',error_code='REFUND_RECEIPT_CONFLICT',conflicting_receipt_json=? WHERE refund_intent_id=?",
                    evidence.toString(), intentId);
            } else if (intent.receipt == null) {
                jdbc.update("UPDATE vls_compute_refund_intent SET state='RECEIPT_UNVERIFIED',error_code='NEEDS_HOLD_REQUERY',receipt_json=?,receipt_observed_at_ms=? WHERE refund_intent_id=?",
                    evidence.toString(), System.currentTimeMillis(), intentId);
            }
            return checked(session, charge(intent.chargeReference), row(intentId));
        });
    }

    public Intent recordUnknown(String intentId, String expectedRequestHash) {
        String usageId = owner(intentId);
        return transactions.execute(tx -> {
            Map<String, Object> session = session(usageId);
            Map<String, Object> stored = row(intentId);
            Intent intent = checked(session, charge(text(stored, "merchant_charge_id")), stored);
            requireHash(intent, expectedRequestHash);
            if ("PREPARED".equals(intent.state) || "UNKNOWN".equals(intent.state))
                jdbc.update("UPDATE vls_compute_refund_intent SET state='UNKNOWN',error_code='REFUND_RECOVERY_CONTRACT_UNVERIFIED' WHERE refund_intent_id=?", intentId);
            return checked(session, charge(intent.chargeReference), row(intentId));
        });
    }

    private void recordRejected(String intentId, String expectedRequestHash, String error, boolean conflict) {
        String usageId = owner(intentId);
        transactions.execute(tx -> {
            Map<String, Object> session = session(usageId);
            Map<String, Object> stored = row(intentId);
            Intent intent = checked(session, charge(text(stored, "merchant_charge_id")), stored);
            requireHash(intent, expectedRequestHash);
            if ("UNKNOWN".equals(intent.state) && intent.receipt == null)
                jdbc.update("UPDATE vls_compute_refund_intent SET state=?,error_code=? WHERE refund_intent_id=?",
                    conflict ? "CONFLICT" : "REJECTED", safeError(error), intentId);
            return null;
        });
    }

    private Intent checked(Map<String, Object> session, Map<String, Object> charge, Map<String, Object> stored) {
        JsonNode original = verifiedOriginal(session, charge);
        Intent intent = new Intent(stored);
        if (!intent.usageId.equals(text(session, "usage_id")) || !intent.chargeReference.equals(text(charge, "merchant_charge_id"))
            || !intent.merchantId.equals(text(session, "merchant_id")) || !intent.payerUserId.equals(text(session, "payer_user_id"))
            || !intent.chargeId.equals(text(charge, "charge_id")) || !intent.originalLedgerId.equals(text(charge, "ledger_entry_id"))
            || !intent.originalRequestHash.equals(text(charge, "request_hash")) || !intent.path.equals(path(intent.chargeId))
            || !(intent.holdId == null ? !original.has("hold_id") : intent.holdId.equals(original.path("hold_id").asText()))
            || !ComputeBillingSettlementService.hash(intent.body).equals(intent.requestHash) || !intent.key.matches("vls-refund-op-[0-9a-f-]{36}"))
            throw new IllegalStateException("Corrective-refund ownership or snapshot conflict");
        String reference = "vls-refund:" + ComputeBillingSettlementService.hash(intent.chargeReference + ":" + intent.correctionReference);
        JsonNode body = parse(intent.body);
        if (!reference.equals(intent.reference) || !body.path("refund_quota").asText().equals(text(stored, "refund_quota"))
            || !RefundContracts.request(intent.merchantId, reference, intent.originalLedgerId,
                body.path("refund_quota").asText(), body.path("reason_code").asText()).toString().equals(intent.body)
            || ExactQuota.integer(body.path("refund_quota").asText(), true).compareTo(ExactQuota.integer(original.path("charge_quota").asText(), true)) > 0)
            throw new IllegalStateException("Invalid corrective-refund body");
        if (!("PREPARED".equals(intent.state) || "UNKNOWN".equals(intent.state) || "RECEIPT_UNVERIFIED".equals(intent.state)
            || "LEDGER_RECONCILED".equals(intent.state) || "REJECTED".equals(intent.state) || "CONFLICT".equals(intent.state))) throw new IllegalStateException("Invalid corrective-refund state");
        boolean ledgerProof = intent.receipt != null && "REFUND_LEDGER_RECONCILED".equals(parse(intent.receipt).path("kind").asText());
        if (intent.receipt != null && (!(ledgerProof ? RefundContracts.reconciled(parse(intent.receipt)) : RefundContracts.receipt(parse(intent.receipt))).toString().equals(intent.receipt) || intent.receiptObservedAtMs <= 0))
            throw new IllegalStateException("Invalid persisted corrective-refund receipt");
        if (ledgerProof && !matchesProof(intent, charge, parse(intent.receipt))) throw new IllegalStateException("Refund ledger proof ownership conflict");
        if ("LEDGER_RECONCILED".equals(intent.state) && !ledgerProof) throw new IllegalStateException("Refund ledger proof required");
        if (intent.receipt == null && ("RECEIPT_UNVERIFIED".equals(intent.state) || "LEDGER_RECONCILED".equals(intent.state) || intent.receiptObservedAtMs != 0))
            throw new IllegalStateException("Corrective-refund receipt evidence is missing");
        return intent;
    }

    private JsonNode verifiedOriginal(Map<String, Object> session, Map<String, Object> charge) {
        String body = text(charge, "request_json");
        if (!"SETTLED".equals(text(charge, "state")) || !ComputeBillingSettlementService.hash(body).equals(text(charge, "request_hash")))
            throw new IllegalStateException("A verified settled original charge is required");
        JsonNode original = parse(body);
        if (!original.path("charge_quota").isTextual() || !text(session, "usage_id").equals(text(charge, "usage_id")) || !text(session, "usage_id").equals(original.path("usage_session_id").asText())
            || !text(session, "merchant_id").equals(original.path("merchant_id").asText()) || !text(session, "payer_user_id").equals(original.path("user_id").asText())
            || !text(charge, "merchant_charge_id").equals(original.path("merchant_charge_id").asText())
            || !text(charge, "amount_text").equals(original.path("charge_quota").asText()))
            throw new IllegalStateException("Original charge identity or amount conflict");
        ExactQuota.integer(text(charge, "charge_id"), true);
        ExactQuota.integer(text(charge, "ledger_entry_id"), true);
        ExactQuota.integer(original.path("charge_quota").asText(), true);
        if (original.has("hold_id")) ExactQuota.integer(original.path("hold_id").asText(), true);
        return original;
    }

    private Map<String, Object> session(String id) {
        return jdbc.queryForMap("SELECT * FROM vls_compute_billing_session WHERE usage_id=? FOR UPDATE", id);
    }
    private Map<String, Object> charge(String reference) {
        return jdbc.queryForMap("SELECT * FROM vls_compute_billing_slice WHERE merchant_charge_id=? FOR UPDATE", reference);
    }
    private Map<String, Object> row(String id) {
        return jdbc.queryForMap("SELECT * FROM vls_compute_refund_intent WHERE refund_intent_id=? FOR UPDATE", id);
    }
    private String owner(String id) {
        return jdbc.queryForObject("SELECT usage_id FROM vls_compute_refund_intent WHERE refund_intent_id=?", String.class, id);
    }
    private String chargeOwner(String reference) {
        return jdbc.queryForObject("SELECT usage_id FROM vls_compute_billing_slice WHERE merchant_charge_id=?", String.class, reference);
    }
    private JsonNode parse(String value) {
        try { return json.readTree(value); }
        catch (Exception e) { throw new IllegalStateException("Invalid corrective-refund JSON"); }
    }
    private static String path(String chargeId) {
        return "/api/v1/external-billing/charges/" + ExactQuota.integer(chargeId, true) + "/refunds";
    }
    private static String text(Map<String, Object> row, String field) {
        Object value = row.get(field);
        return value == null ? null : String.valueOf(value);
    }
    private static void requireHash(Intent intent, String expected) {
        if (!intent.requestHash.equals(expected)) throw new IllegalArgumentException("Refund response does not match immutable request");
    }
    private static String safeError(String error) {
        return error != null && error.matches("[A-Z][A-Z0-9_]{0,63}") ? error : "REFUND_REJECTED";
    }

    public static final class Intent {
        public final String id, chargeReference, correctionReference, reference, usageId, merchantId, payerUserId;
        public final String chargeId, originalLedgerId, originalRequestHash, holdId, key, path, body, requestHash, state, errorCode;
        public final String receipt, conflictingReceipt;
        public final long receiptObservedAtMs;
        public final boolean safeToRetry = false;
        public final boolean needsHoldRequery;

        private Intent(Map<String, Object> row) {
            id = text(row, "refund_intent_id"); chargeReference = text(row, "merchant_charge_id");
            correctionReference = text(row, "correction_reference"); reference = text(row, "merchant_refund_id");
            usageId = text(row, "usage_id"); merchantId = text(row, "merchant_id"); payerUserId = text(row, "payer_user_id");
            chargeId = text(row, "charge_id"); originalLedgerId = text(row, "original_ledger_entry_id"); originalRequestHash = text(row, "original_request_hash");
            holdId = text(row, "original_hold_id");
            key = text(row, "idempotency_key"); path = text(row, "request_path"); body = text(row, "request_json"); requestHash = text(row, "request_hash");
            state = text(row, "state"); errorCode = text(row, "error_code"); receipt = text(row, "receipt_json"); conflictingReceipt = text(row, "conflicting_receipt_json");
            receiptObservedAtMs = row.get("receipt_observed_at_ms") == null ? 0 : ((Number) row.get("receipt_observed_at_ms")).longValue();
            needsHoldRequery = receipt != null && ("RECEIPT_UNVERIFIED".equals(state) || "LEDGER_RECONCILED".equals(state))
                && "NEEDS_HOLD_REQUERY".equals(errorCode);
        }
    }
}
