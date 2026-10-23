package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
