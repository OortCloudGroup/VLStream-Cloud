package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigInteger;
import java.time.Instant;

/** Documented corrective-refund fields. A receipt cannot prove where captured funds became available. */
public final class RefundContracts {
    private static final ObjectMapper JSON = new ObjectMapper();

    private RefundContracts() { }

    public static ObjectNode request(String merchantId, String merchantRefundId, String originalLedgerId,
                                     String refundQuota, String reasonCode) {
        ObjectNode body = JSON.createObjectNode();
        body.put("merchant_id", reference(merchantId));
        body.put("merchant_refund_id", reference(merchantRefundId));
        body.put("original_ledger_entry_id", ExactQuota.integer(originalLedgerId, true).toString());
        body.put("refund_quota", ExactQuota.integer(refundQuota, true).toString());
        if (reasonCode == null || !reasonCode.matches("[A-Z][A-Z0-9_]{0,63}"))
            throw new IllegalArgumentException("Invalid corrective-refund reason code");
        body.put("reason_code", reasonCode);
        return body;
    }

    public static ObjectNode receipt(JsonNode response) {
        JsonNode data = response;
        rejectError(response);
        if (response != null && response.has("success")) {
            if (!response.get("success").isBoolean() || !response.get("success").booleanValue())
                throw new IllegalArgumentException("Successful refund response required");
            data = response.get("data");
        }
        if (data == null || !data.isObject() || !"SUCCEEDED".equals(data.path("status").asText()))
            throw new IllegalArgumentException("Succeeded refund data required");
        rejectError(data);
        ObjectNode canonical = JSON.createObjectNode();
        for (String field : new String[]{"operation_id", "refund_id", "ledger_entry_id"})
            canonical.put(field, exact(data.get(field), true, false));
        canonical.put("status", "SUCCEEDED");
        canonical.put("refunded_quota", exact(data.get("refunded_quota"), true, true));
        canonical.put("available_quota", exact(data.get("available_quota"), false, true));
        canonical.put("balance_version", exact(data.get("balance_version"), false, false));
        return canonical;
    }

    /** Read-only proof has its own schema: missing POST fields must never be fabricated. */
    public static ObjectNode reconciled(JsonNode operation, JsonNode original, JsonNode refund) {
        rejectError(operation);
        if (operation == null || !operation.isObject() || !"SUCCEEDED".equals(operation.path("status").asText())
            || !"REFUND".equals(operation.path("operation_type").asText()))
            throw new IllegalArgumentException("Succeeded refund operation required");
        ObjectNode op = JSON.createObjectNode();
        op.put("idempotency_key", reference(operation.path("idempotency_key").asText()));
        op.put("operation_type", "REFUND"); op.put("status", "SUCCEEDED");
        op.put("refund_id", exact(operation.get("refund_id"), true, false));
        if (operation.has("merchant_id")) op.put("merchant_id", reference(operation.path("merchant_id").asText()));
        if (operation.has("user_id")) op.put("user_id", exact(operation.get("user_id"), true, false));
        ObjectNode debit = ledger(original, false);
        ObjectNode credit = ledger(refund, true);
        if (!op.path("refund_id").asText().equals(credit.path("reference_id").asText())
            || !debit.path("ledger_entry_id").asText().equals(credit.path("related_entry_id").asText())
            || !debit.path("user_id").asText().equals(credit.path("user_id").asText())
            || op.has("user_id") && !op.path("user_id").asText().equals(debit.path("user_id").asText())
            || debit.path("ledger_entry_id").asText().equals(credit.path("ledger_entry_id").asText())
            || !"0".equals(debit.path("related_entry_id").asText())
            || Instant.parse(credit.path("created_at").asText()).isBefore(Instant.parse(debit.path("created_at").asText()))
            || ExactQuota.integer(credit.path("quota_delta").asText(), true).compareTo(
                ExactQuota.integer(debit.path("quota_delta").asText().substring(1), true)) > 0)
            throw new IllegalArgumentException("Refund ledger association conflict");
        ObjectNode proof = JSON.createObjectNode();
        proof.put("version", 1); proof.put("kind", "REFUND_LEDGER_RECONCILED");
        proof.set("operation", op); proof.set("original_ledger", debit); proof.set("refund_ledger", credit);
        return proof;
    }

    public static ObjectNode reconciled(JsonNode proof) {
        if (proof == null || !proof.isObject() || proof.size() != 5 || !proof.path("version").isInt()
            || proof.path("version").intValue() != 1 || !"REFUND_LEDGER_RECONCILED".equals(proof.path("kind").asText()))
            throw new IllegalArgumentException("Unsupported refund evidence schema");
        ObjectNode canonical = reconciled(proof.get("operation"), proof.get("original_ledger"), proof.get("refund_ledger"));
        if (!canonical.equals(proof)) throw new IllegalArgumentException("Noncanonical refund evidence");
        return canonical;
    }

    private static ObjectNode ledger(JsonNode entry, boolean refund) {
        rejectError(entry);
        if (entry == null || !entry.isObject() || !(refund ? "CAPTURE_REFUND" : "CAPTURE").equals(entry.path("entry_type").asText())
            || !(refund ? "refund" : "charge").equals(entry.path("reference_type").asText()))
            throw new IllegalArgumentException("Capture ledger type required");
        ObjectNode canonical = JSON.createObjectNode();
        for (String field : new String[]{"ledger_entry_id", "user_id", "reference_id"})
            canonical.put(field, exact(entry.get(field), true, false));
        canonical.put("related_entry_id", exact(entry.get("related_entry_id"), refund, false));
        canonical.put("merchant_ref", reference(entry.path("merchant_ref").asText()));
        canonical.put("entry_type", refund ? "CAPTURE_REFUND" : "CAPTURE");
        canonical.put("reference_type", refund ? "refund" : "charge");
        String delta = entry.path("quota_delta").asText();
        if (!entry.path("quota_delta").isTextual() || (!refund && !delta.startsWith("-")))
            throw new IllegalArgumentException("Exact ledger delta required");
        BigInteger amount = ExactQuota.integer(refund ? delta : delta.substring(1), true);
        canonical.put("quota_delta", refund ? amount.toString() : "-" + amount);
        BigInteger before = ExactQuota.integer(exact(entry.get("balance_before"), false, true), false);
        BigInteger after = ExactQuota.integer(exact(entry.get("balance_after"), false, true), false);
        if (!before.add(refund ? amount : amount.negate()).equals(after))
            throw new IllegalArgumentException("Ledger balance equation conflict");
        canonical.put("balance_before", before.toString()); canonical.put("balance_after", after.toString());
        String created = entry.path("created_at").asText();
        try { Instant.parse(created); } catch (RuntimeException e) { throw new IllegalArgumentException("Ledger timestamp required"); }
        canonical.put("created_at", created);
        return canonical;
    }

    static String reference(String value) {
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}"))
            throw new IllegalArgumentException("Invalid corrective-refund reference");
        return value;
    }

    private static void rejectError(JsonNode node) {
        if (node == null) return;
        for (String field : new String[]{"error", "error_code"}) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull() && (!value.isTextual() || !value.textValue().isEmpty()))
                throw new IllegalArgumentException("Contradictory refund error response");
        }
    }

    private static String exact(JsonNode value, boolean positive, boolean stringOnly) {
        if (value == null || !(value.isTextual() || !stringOnly && value.isIntegralNumber()))
            throw new IllegalArgumentException("Exact refund integer required");
        return ExactQuota.integer(value.asText(), positive).toString();
    }
}
