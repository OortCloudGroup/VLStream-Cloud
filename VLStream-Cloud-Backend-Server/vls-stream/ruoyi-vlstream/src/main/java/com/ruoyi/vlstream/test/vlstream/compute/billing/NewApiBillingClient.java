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

/** Read/query adapter for the documented API. Real writes stay blocked until atomic capture exists. */
@Component
@ConditionalOnProperty(prefix = "vlstream.compute.external-billing", name = "enabled", havingValue = "true")
public class NewApiBillingClient implements ExternalBillingGateway {
    private final ExternalBillingProperties properties;
    private final ObjectMapper json;

    public NewApiBillingClient(ExternalBillingProperties properties, ObjectMapper json) {
        this.properties = properties; this.json = json;
    }

    @Override public boolean supportsAtomicCapture() { return false; }
    @Override public Result capture(String originalKey, String immutableBody) {
        // Never replace this with release+charge: another consumer could spend the released funds.
        throw new IllegalStateException("Atomic capture is not specified/implemented; paid billing is blocked");
    }
    public JsonNode config() { return read("/api/v1/external-billing/config", merchantKey()).body; }

    /** Caller must pass the backend session's same independently verified SSO proof, never a payer id. */
    public BillingPayer resolveSelf(ComputePlatformAuthorization.Context actor, String verifiedSsoProof) {
        Reply result = read("/api/user/self", verifiedSsoProof);
        if (result.status != 200) throw new IllegalArgumentException("Authoritative user lookup unavailable");
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

    private boolean failed(JsonNode body) { return body.has("success") && !body.path("success").asBoolean(); }
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
            connection.setInstanceFollowRedirects(false); connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000); connection.setReadTimeout(5000);
            connection.setRequestProperty("Authorization", "Bearer " + bearer); connection.setRequestProperty("Accept", "application/json");
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
