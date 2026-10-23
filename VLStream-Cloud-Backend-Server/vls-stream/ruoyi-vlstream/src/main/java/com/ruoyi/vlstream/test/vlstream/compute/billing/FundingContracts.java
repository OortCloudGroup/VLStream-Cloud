package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigInteger;

/** Documented funding wire fields only. A receipt never establishes usable funding or capture support. */
public final class FundingContracts {
    private static final ObjectMapper JSON = new ObjectMapper();

    private FundingContracts() { }

    /** The service must derive merchant and payer from its immutable bound session, never controller input. */
    public static ObjectNode hold(String merchantId, String merchantHoldId, String persistedPayerUserId,
                                  String holdQuota, String expiresAt, String reason) {
        ObjectNode body = JSON.createObjectNode();
        body.put("merchant_id", reference(merchantId));
        body.put("merchant_hold_id", reference(merchantHoldId));
        body.put("user_id", integer(persistedPayerUserId, true));
        body.put("hold_quota", positiveText(holdQuota));
        // Preserve the supplied epoch-millisecond string. Retries must use the original persisted body.
        body.put("expires_at", positiveText(expiresAt));
        body.put("reason", reason(reason));
        return body;
    }

    /** Does not establish release idempotency, expiry behavior or authorization after revocation. */
    public static ObjectNode release(String merchantId, String persistedPayerUserId, String releaseQuota) {
        ObjectNode body = JSON.createObjectNode();
        body.put("merchant_id", reference(merchantId));
        body.put("user_id", integer(persistedPayerUserId, true));
        body.put("release_quota", positiveText(releaseQuota));
        return body;
    }

    /** New releases have a durable business reference; the three-argument overload is legacy evidence only. */
    public static ObjectNode release(String merchantId, String merchantReleaseId, String persistedPayerUserId,
                                     String releaseQuota) {
        ObjectNode body = JSON.createObjectNode();
        body.put("merchant_id", reference(merchantId));
        body.put("merchant_release_id", reference(merchantReleaseId));
        body.put("user_id", integer(persistedPayerUserId, true));
        body.put("release_quota", positiveText(releaseQuota));
        return body;
    }

    /** A topup adds quota to the same hold. It never changes expires_at. */
    public static ObjectNode topup(String merchantId, String merchantTopupId, String persistedPayerUserId,
                                   String topupQuota, String reason) {
        ObjectNode body = JSON.createObjectNode();
        body.put("merchant_id", reference(merchantId));
        body.put("merchant_topup_id", reference(merchantTopupId));
        body.put("user_id", integer(persistedPayerUserId, true));
        body.put("topup_quota", positiveText(topupQuota));
        body.put("reason", reason(reason));
        return body;
    }

    public static HoldDetail holdDetail(JsonNode response) { return new HoldDetail(documentedData(response)); }
    public static TopupReceipt topupReceipt(JsonNode response) { return new TopupReceipt(documentedData(response)); }
    public static ReleaseReceipt releaseResult(JsonNode response) { return new ReleaseReceipt(documentedData(response)); }

    public static class TopupReceipt {
        public final String holdId, holdQuota, capturedQuota, releasedQuota, remainingQuota, holdStatus, balanceVersion;
        private TopupReceipt(JsonNode data) {
            holdId = holdId(data.get("hold_id"), false);
            holdQuota = quota(data.get("hold_quota"), false);
            capturedQuota = quota(data.get("captured_quota"), false);
            releasedQuota = quota(data.get("released_quota"), false);
            remainingQuota = quota(data.get("remaining_quota"), false);
            holdStatus = status(data);
            balanceVersion = exactValue(data.get("balance_version"), false);
            if (!integer(holdQuota, false).subtract(integer(capturedQuota, false))
                .subtract(integer(releasedQuota, false)).equals(integer(remainingQuota, false)))
                throw new IllegalArgumentException("Hold quota arithmetic mismatch");
        }
    }

    public static final class HoldDetail extends TopupReceipt {
        public final String merchantHoldId, userId, expiresAt, createdAt, updatedAt;
        private HoldDetail(JsonNode data) {
            super(data);
            merchantHoldId = reference(requiredText(data, "merchant_hold_id"));
            userId = exactValue(data.get("user_id"), true);
            // expiry is milliseconds; creation/update are seconds. No conversion or inferred extension.
            expiresAt = exactValue(data.get("expires_at"), true);
            createdAt = exactValue(data.get("created_at"), false);
            updatedAt = exactValue(data.get("updated_at"), false);
            if (integer(updatedAt, false).compareTo(integer(createdAt, false)) < 0)
                throw new IllegalArgumentException("Hold timestamps out of order");
        }
    }

    /** releasedQuota is the actual amount of this operation, never the cumulative hold total. */
    public static final class ReleaseReceipt {
        public final String holdId, heldQuota, releasedQuota, remainingQuota, holdStatus;
        private ReleaseReceipt(JsonNode data) {
            holdId = holdId(data.get("hold_id"), false);
            heldQuota = quota(data.get("held_quota"), false);
            releasedQuota = quota(data.get("released_quota"), false);
            remainingQuota = quota(data.get("remaining_quota"), false);
            holdStatus = status(data);
        }
    }

    private static String status(JsonNode data) {
        String value = requiredText(data, "hold_status");
        if (!("ACTIVE".equals(value) || "PARTIALLY_CAPTURED".equals(value) || "CAPTURED".equals(value)
            || "RELEASED".equals(value) || "EXPIRED".equals(value)))
            throw new IllegalArgumentException("Unknown hold status");
        return value;
    }

    private static String requiredText(JsonNode data, String field) {
        JsonNode value = data.get(field);
        if (value == null || !value.isTextual()) throw new IllegalArgumentException("Funding string field required");
        return value.textValue();
    }

    private static String exactValue(JsonNode value, boolean positive) {
        if (value == null || (!value.isTextual() && !value.isIntegralNumber()))
            throw new IllegalArgumentException("Exact funding integer required");
        return integer(value.asText(), positive).toString();
    }

    private static JsonNode documentedData(JsonNode response) {
        if (response == null || !response.isObject()) throw new IllegalArgumentException("Funding object required");
        if (response.has("error") || response.has("error_code")) throw new IllegalArgumentException("Funding error response");
        if (response.has("success")) return documentedData(successfulData(response));
        return response;
    }

    public static Receipt holdReceipt(JsonNode response) {
        JsonNode data = documentedData(response);
        return new Receipt(holdId(data.get("hold_id"), false), quota(data.get("available_quota"), false),
            quota(data.get("held_quota"), false));
    }

    /** Only held_quota is required by the release response contract. No funding state is inferred. */
    public static Receipt releaseReceipt(JsonNode response) {
        JsonNode data = successfulData(response);
        return new Receipt(holdId(data.get("hold_id"), true), quota(data.get("available_quota"), true),
            quota(data.get("held_quota"), false));
    }

    public static final class Receipt {
        public final String holdId;
        public final String availableQuota;
        public final String heldQuota;

        private Receipt(String holdId, String availableQuota, String heldQuota) {
            this.holdId = holdId;
            this.availableQuota = availableQuota;
            this.heldQuota = heldQuota;
        }
    }

    private static JsonNode successfulData(JsonNode response) {
        JsonNode success = response == null ? null : response.get("success");
        JsonNode data = response == null ? null : response.get("data");
        if (success == null || !success.isBoolean() || !success.booleanValue() || data == null || !data.isObject())
            throw new IllegalArgumentException("Successful funding response with object data required");
        return data;
    }

    private static String holdId(JsonNode value, boolean optional) {
        if (value == null || value.isNull()) {
            if (optional) return null;
            throw new IllegalArgumentException("Funding hold id required");
        }
        if (!value.isTextual() && !value.isIntegralNumber())
            throw new IllegalArgumentException("Funding hold id must be an exact integer");
        return integer(value.asText(), true).toString();
    }

    private static String quota(JsonNode value, boolean optional) {
        if (value == null || value.isNull()) {
            if (optional) return null;
            throw new IllegalArgumentException("Funding quota required");
        }
        if (!value.isTextual()) throw new IllegalArgumentException("Funding quota must be an integer string");
        return integer(value.textValue(), false).toString();
    }

    private static String positiveText(String value) {
        integer(value, true);
        return value;
    }

    private static BigInteger integer(String value, boolean positive) {
        return ExactQuota.integer(value, positive);
    }

    private static String reference(String value) {
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}"))
            throw new IllegalArgumentException("Invalid funding merchant or business reference");
        return value;
    }

    private static String reason(String value) {
        if (value == null || value.length() > 255 || value.trim().isEmpty())
            throw new IllegalArgumentException("Invalid funding reason");
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) throw new IllegalArgumentException("Invalid funding reason");
        }
        return value;
    }
}
