package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.vlstream.test.vlstream.compute.ComputePlatformAuthorization;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Documented merchant adapter. Read access and funds movement have separate, default-off gates. */
@Component
@ConditionalOnProperty(prefix = "vlstream.compute.external-billing", name = "enabled", havingValue = "true")
public class NewApiBillingClient implements ExternalBillingGateway {
    private final ExternalBillingProperties properties;
    private final ObjectMapper json;

    public NewApiBillingClient(ExternalBillingProperties properties, ObjectMapper json) {
        this.properties = properties; this.json = json;
    }

    @Override public boolean supportsAtomicCapture() { return properties.isEnabled() && properties.isAllowWrites(); }
    @Override public Result capture(String originalKey, String immutableBody) {
        JsonNode body = checkedWrite(originalKey, immutableBody);
        exactId(body.get("hold_id")); // This boundary never falls back to an available-balance charge.
        exactId(body.get("user_id"));
        ExactQuota.integer(BillingSnapshot.required(body, "charge_quota"), true);
        if (!"GPU_COMPUTE".equals(text(body, "business_type"))) throw new IllegalArgumentException("Invalid capture business type");
        text(body, "merchant_charge_id"); text(body, "usage_session_id"); text(body, "conversion_version");
        return operationReply(post("/api/v1/external-billing/charges", merchantKey(), originalKey, immutableBody), false);
    }

    @Override public Result createHold(String originalKey, String immutableBody) {
        JsonNode body = checkedWrite(originalKey, immutableBody);
        requireSame(body, FundingContracts.hold(text(body, "merchant_id"), text(body, "merchant_hold_id"),
            exactId(body.get("user_id")), text(body, "hold_quota"), text(body, "expires_at"), text(body, "reason")));
        return fundingReply(post("/api/v1/external-billing/holds", merchantKey(), originalKey, immutableBody), "HOLD", null);
    }

    @Override public Result topupHold(String holdId, String originalKey, String immutableBody) {
        ExactQuota.integer(holdId, true);
        JsonNode body = checkedWrite(originalKey, immutableBody);
        requireSame(body, FundingContracts.topup(text(body, "merchant_id"), text(body, "merchant_topup_id"),
            exactId(body.get("user_id")), text(body, "topup_quota"), text(body, "reason")));
        return fundingReply(post("/api/v1/external-billing/holds/" + holdId + "/topup", merchantKey(), originalKey, immutableBody), "TOPUP", holdId);
    }

    @Override public Result releaseHold(String holdId, String originalKey, String immutableBody) {
        ExactQuota.integer(holdId, true);
        JsonNode body = checkedWrite(originalKey, immutableBody);
        requireSame(body, FundingContracts.release(text(body, "merchant_id"), text(body, "merchant_release_id"),
            exactId(body.get("user_id")), text(body, "release_quota")));
        return fundingReply(post("/api/v1/external-billing/holds/" + holdId + "/release", merchantKey(), originalKey, immutableBody), "RELEASE", holdId);
    }

    @Override public Result refund(String chargeId, String originalKey, String immutableBody) {
        ExactQuota.integer(chargeId, true);
        JsonNode body = checkedWrite(originalKey, immutableBody);
        text(body, "merchant_refund_id");
        ExactQuota.integer(text(body, "original_ledger_entry_id"), true);
        ExactQuota.integer(text(body, "refund_quota"), true);
        text(body, "reason_code");
        if (body.size() != 5) throw new IllegalArgumentException("Unexpected refund request fields");
        return operationReply(post("/api/v1/external-billing/charges/" + chargeId + "/refunds", merchantKey(), originalKey, immutableBody), true);
    }

    @Override public Result holdById(String holdId) {
        ExactQuota.integer(holdId, true);
        return fundingReply(read("/api/v1/external-billing/holds/" + holdId, merchantKey()), "DETAIL", holdId);
    }

    @Override public Result holdByReference(String reference) {
        Result result = fundingReply(read("/api/v1/external-billing/holds/by-reference/" + segment(reference), merchantKey()), "DETAIL", null);
        if (result.status == Status.SUCCEEDED && !reference.equals(result.charge.path("merchant_hold_id").asText()))
            return new Result(Status.CONFLICT, "HOLD_REFERENCE_MISMATCH", null);
        return result;
    }

    @Override public Result holdOperation(String operation, String businessReference) {
        if (!("HOLD".equals(operation) || "TOPUP".equals(operation) || "RELEASE".equals(operation)))
            throw new IllegalArgumentException("Invalid hold operation type");
        Reply reply = read("/api/v1/external-billing/hold-operations/" + operation + "/" + segment(businessReference), merchantKey());
        Result failure = failure(reply);
        if (failure != null) return failure;
        // The endpoint is known; its full success/not-found schema is not. Do not infer mutation success.
        return new Result(Status.UNKNOWN, "HOLD_OPERATION_RESPONSE_UNSPECIFIED", null);
    }

    private Result fundingReply(Reply reply, String kind, String expectedHoldId) {
        Result failure = failure(reply);
        if (failure != null) return failure;
        if (reply.status != 200 || failed(reply.body)) return Result.of(Status.UNKNOWN);
        try {
            JsonNode data = data(reply.body);
            String actualHoldId;
            if ("DETAIL".equals(kind)) actualHoldId = FundingContracts.holdDetail(data).holdId;
            else if ("TOPUP".equals(kind)) actualHoldId = FundingContracts.topupReceipt(data).holdId;
            else if ("RELEASE".equals(kind)) actualHoldId = FundingContracts.releaseResult(data).holdId;
            else actualHoldId = FundingContracts.holdReceipt(data).holdId;
            if (expectedHoldId != null && !expectedHoldId.equals(actualHoldId))
                return new Result(Status.CONFLICT, "HOLD_MISMATCH", null);
            return new Result(Status.SUCCEEDED, "", data);
        } catch (IllegalArgumentException e) { return Result.of(Status.UNKNOWN); }
    }

    private Result operationReply(Reply reply, boolean refund) {
        Result failure = failure(reply);
        if (failure != null) return failure;
        if (reply.status != 200 || failed(reply.body)) return Result.of(Status.UNKNOWN);
        JsonNode body;
        try { body = data(reply.body); } catch (IllegalArgumentException e) { return Result.of(Status.UNKNOWN); }
        String status = body.path("status").asText();
        if ("SUCCEEDED".equals(status)) {
            try {
                exactId(body.get("operation_id")); exactId(body.get(refund ? "refund_id" : "charge_id")); exactId(body.get("ledger_entry_id"));
                ExactQuota.integer(text(body, refund ? "refunded_quota" : "charged_quota"), true);
                ExactQuota.integer(text(body, "available_quota"), false);
                JsonNode version = body.get("balance_version");
                if (version == null || (!version.isTextual() && !version.isIntegralNumber())) return Result.of(Status.UNKNOWN);
                ExactQuota.integer(version.asText(), false);
                return new Result(Status.SUCCEEDED, "", body);
            } catch (IllegalArgumentException e) { return Result.of(Status.UNKNOWN); }
        }
        if ("PROCESSING".equals(status)) return Result.of(Status.PROCESSING);
        if ("REJECTED".equals(status) && !body.path("error_code").asText().isEmpty())
            return new Result(Status.REJECTED, body.path("error_code").asText(), body);
        return Result.of(Status.UNKNOWN);
    }

    private Result failure(Reply reply) {
        String code = reply.body.path("error").asText();
        if (code.isEmpty()) code = reply.body.path("error_code").asText();
        if (reply.status >= 500 || reply.status < 200 || (reply.status >= 300 && reply.status < 400)) return Result.of(Status.UNKNOWN);
        if (reply.status == 409) return new Result(Status.CONFLICT,
            "BUSINESS_REFERENCE_CONFLICT".equals(code) ? code : "IDEMPOTENCY_CONFLICT".equals(code) ? code : "CONFLICT", null);
        if ((reply.status == 400 || reply.status == 403 || reply.status == 422)
            && ("HOLD_MISMATCH".equals(code) || "HOLD_EXPIRED".equals(code) || "INVALID_REQUEST".equals(code)
                || "INSUFFICIENT_HELD_QUOTA".equals(code) || "INSUFFICIENT_AVAILABLE_QUOTA".equals(code)
                || "REFUND_EXCEEDED".equals(code) || "USER_NOT_FOUND".equals(code)))
            return new Result(Status.REJECTED, code, null);
        if (reply.status == 401 || reply.status == 403) return new Result(Status.NOT_AUTHORIZED, "NOT_AUTHORIZED", null);
        return reply.status == 200 ? null : Result.of(Status.UNKNOWN);
    }

    private JsonNode checkedWrite(String key, String body) {
        if (!supportsAtomicCapture()) throw new IllegalStateException("External billing writes disabled");
        if (key == null || !key.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}")) throw new IllegalArgumentException("Invalid billing operation key");
        try {
            JsonNode parsed = json.readTree(body);
            if (parsed == null || !parsed.isObject() || properties.getMerchantId() == null
                || !properties.getMerchantId().equals(text(parsed, "merchant_id")))
                throw new IllegalArgumentException("Billing merchant mismatch");
            return parsed;
        } catch (java.io.IOException e) { throw new IllegalArgumentException("Invalid billing request JSON"); }
    }

    private void requireSame(JsonNode actual, JsonNode expected) {
        try {
            if (!actual.equals(json.readTree(expected.toString()))) throw new IllegalArgumentException("Unexpected funding request fields");
        } catch (java.io.IOException e) { throw new IllegalArgumentException("Invalid funding request fields"); }
    }
    private static String text(JsonNode body, String field) {
        JsonNode value = body.get(field);
        if (value == null || !value.isTextual()) throw new IllegalArgumentException("Billing string field required");
        return value.textValue();
    }
    private static String exactId(JsonNode value) {
        if (value == null || (!value.isTextual() && !value.isIntegralNumber())) throw new IllegalArgumentException("Exact billing id required");
        return ExactQuota.integer(value.asText(), true).toString();
    }
    private static JsonNode data(JsonNode body) {
        if (body == null || !body.isObject()) throw new IllegalArgumentException("Billing object required");
        if (body.has("error") || (body.has("error_code") && (!body.get("error_code").isTextual()
            || (!body.path("error_code").asText().isEmpty() && !"REJECTED".equals(body.path("status").asText())))))
            throw new IllegalArgumentException("Billing error response");
        if (!body.has("success")) return body;
        if (!body.get("success").isBoolean() || !body.get("success").booleanValue() || !body.path("data").isObject())
            throw new IllegalArgumentException("Billing success envelope invalid");
        return data(body.get("data"));
    }
    public JsonNode config() { return read("/api/v1/external-billing/config", merchantKey()).body; }

    /** Caller must pass the backend session's same independently verified SSO proof, never a payer id. */
    public BillingPayer resolveSelf(ComputePlatformAuthorization.Context actor, String verifiedSsoProof) {
        Reply result = read("/api/user/self", verifiedSsoProof);
        if (result.status != 200 || !result.body.path("success").isBoolean() || !result.body.path("success").booleanValue()
            || !result.body.path("data").isObject()) throw new IllegalArgumentException("Authoritative user lookup unavailable");
        return BillingPayer.fromVerifiedSelf(actor, result.body);
    }

    @Override public Result byKey(String key) { return query("/operations/by-key/" + segment(key), false); }
    @Override public Result byReference(String reference) { return query("/charges/by-reference/" + segment(reference), true); }
    @Override public Result ledger(long fromSecond, long toSecond, String cursor) {
        if (fromSecond < 0 || toSecond <= fromSecond) throw new IllegalArgumentException("Invalid ledger window");
        Reply reply = read("/api/v1/external-billing/ledger?from=" + fromSecond + "&to=" + toSecond + "&limit=100" + (cursor == null ? "" : "&cursor=" + segment(cursor)), merchantKey());
        if (reply.status == 401 || reply.status == 403) return Result.of(Status.NOT_AUTHORIZED);
        if (reply.status != 200 || failed(reply.body) || !reply.body.path("entries").isArray() || !reply.body.path("has_more").isBoolean()) return Result.of(Status.UNKNOWN);
        return new Result(Status.SUCCEEDED, "", reply.body);
    }

    @Override public Result balance(String verifiedUserId) {
        ExactQuota.integer(verifiedUserId, true);
        Reply reply = read("/api/v1/external-billing/users/" + verifiedUserId + "/balance", merchantKey());
        if (reply.status == 401 || reply.status == 403) return Result.of(Status.NOT_AUTHORIZED);
        if (reply.status != 200 || failed(reply.body)) return Result.of(Status.UNKNOWN);
        try {
            if (!verifiedUserId.equals(reply.body.path("user_id").asText())) return Result.of(Status.UNKNOWN);
            java.math.BigInteger balance = ExactQuota.integer(BillingSnapshot.required(reply.body, "balance_quota"), false);
            java.math.BigInteger held = ExactQuota.integer(BillingSnapshot.required(reply.body, "held_quota"), false);
            java.math.BigInteger available = ExactQuota.integer(BillingSnapshot.required(reply.body, "available_quota"), false);
            if (!balance.subtract(held).equals(available)) return Result.of(Status.UNKNOWN);
            return new Result(Status.SUCCEEDED, "", reply.body);
        } catch (IllegalArgumentException e) { return Result.of(Status.UNKNOWN); }
    }

    private Result query(String path, boolean chargeDetail) {
        Reply reply = read("/api/v1/external-billing" + path, merchantKey());
        if (reply.status == 401 || reply.status == 403) return Result.of(Status.NOT_AUTHORIZED);
        if (reply.status == 409) return Result.of(Status.CONFLICT);
        String error = reply.body.path("error").asText();
        if (reply.status == 404 && ("NOT_FOUND".equals(error) || "CHARGE_NOT_FOUND".equals(error) || "OPERATION_NOT_FOUND".equals(error))) return Result.of(Status.NOT_FOUND);
        if (reply.status != 200 || failed(reply.body)) return Result.of(Status.UNKNOWN);
        String status = reply.body.path("status").asText();
        if ("SUCCEEDED".equals(status)) return new Result(Status.SUCCEEDED, "", reply.body);
        if (!chargeDetail && "PROCESSING".equals(status)) return Result.of(Status.PROCESSING);
        if (!chargeDetail && "REJECTED".equals(status) && !reply.body.path("error_code").asText().isEmpty())
            return new Result(Status.REJECTED, reply.body.path("error_code").asText(), reply.body);
        return Result.of(Status.UNKNOWN);
    }

    private boolean failed(JsonNode body) { return body.has("success") && (!body.path("success").isBoolean() || !body.path("success").booleanValue()); }
    private String merchantKey() {
        if (properties.getMerchantId() == null || properties.getMerchantId().trim().isEmpty()) throw new IllegalStateException("Billing merchant is not configured");
        return properties.getApiKey();
    }
    private static String segment(String value) {
        if (value == null || value.length() > 200 || value.trim().isEmpty()) throw new IllegalArgumentException("Invalid billing reference");
        try { return URLEncoder.encode(value, "UTF-8").replace("+", "%20"); }
        catch (java.io.UnsupportedEncodingException e) { throw new IllegalStateException("UTF-8 unavailable"); }
    }
    protected static final class Reply {
        final int status; final JsonNode body;
        protected Reply(int status, JsonNode body) { this.status = status; this.body = body; }
    }
    protected Reply read(String path, String bearer) {
        return request("GET", path, bearer, null, null);
    }
    protected Reply post(String path, String bearer, String key, String body) {
        return request("POST", path, bearer, key, body);
    }
    private Reply request(String method, String path, String bearer, String key, String requestBody) {
        if (!properties.isEnabled()) throw new IllegalStateException("External billing disabled");
        if (bearer == null || bearer.trim().isEmpty()) throw new IllegalStateException("Billing identity unavailable");
        HttpURLConnection connection = null;
        try {
            URI base = URI.create(properties.getBaseUrl());
            boolean loopback = "127.0.0.1".equals(base.getHost()) || "localhost".equals(base.getHost());
            if ((!"https".equals(base.getScheme()) && !(loopback && "http".equals(base.getScheme()))) || base.getUserInfo() != null || base.getQuery() != null || base.getFragment() != null)
                throw new IllegalArgumentException("Billing endpoint must be trusted HTTPS");
            String url = base.toString().replaceAll("/+$", "") + path;
            connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setInstanceFollowRedirects(false); connection.setRequestMethod(method);
            connection.setConnectTimeout(5000); connection.setReadTimeout(5000);
            connection.setRequestProperty("Authorization", "Bearer " + bearer); connection.setRequestProperty("Accept", "application/json");
            if (requestBody != null) {
                connection.setRequestProperty("Idempotency-Key", key);
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.setDoOutput(true);
                byte[] bytes = requestBody.getBytes(StandardCharsets.UTF_8);
                connection.setFixedLengthStreamingMode(bytes.length);
                try (java.io.OutputStream output = connection.getOutputStream()) { output.write(bytes); }
            }
            int status = connection.getResponseCode();
            InputStream stream = status < 400 ? connection.getInputStream() : connection.getErrorStream();
            if (stream == null) return new Reply(status, json.createObjectNode());
            try (InputStream input = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096]; int size;
                while ((size = input.read(buffer)) != -1) {
                    if (output.size() + size > 262144) throw new IllegalArgumentException("Billing response too large");
                    output.write(buffer, 0, size);
                }
                JsonNode body = json.readTree(new String(output.toByteArray(), StandardCharsets.UTF_8));
                return new Reply(status, body == null ? json.createObjectNode() : body);
            }
        } catch (Exception e) {
            // Do not expose upstream payloads, tokens, URLs or exception causes.
            return new Reply(0, json.createObjectNode());
        } finally { if (connection != null) connection.disconnect(); }
    }
}
