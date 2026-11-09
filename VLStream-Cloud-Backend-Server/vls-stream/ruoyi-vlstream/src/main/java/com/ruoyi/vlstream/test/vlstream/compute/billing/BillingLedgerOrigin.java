package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

/** Immutable ledger identity accepted with a new quote/session; never inferred for history. */
public final class BillingLedgerOrigin {
    public static final String FIELD = "billing_origin";
    private static final ObjectMapper JSON = new ObjectMapper();

    private BillingLedgerOrigin() { }

    public static String normalizedBaseUrl(String value) {
        try {
            URI uri = new URI(value);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
                || uri.getPort() == 0 || uri.getPort() < -1 || uri.getPort() > 65535)
                throw new IllegalArgumentException("Invalid billing origin");
            String path = uri.getRawPath() == null ? "" : uri.getRawPath();
            if (path.indexOf('%') >= 0 || path.indexOf('\\') >= 0)
                throw new IllegalArgumentException("Ambiguous billing origin path");
            for (String segment : path.split("/"))
                if (".".equals(segment) || "..".equals(segment))
                    throw new IllegalArgumentException("Ambiguous billing origin path");
            path = path.replaceAll("/+$", "");
            return new URI("https", null, uri.getHost().toLowerCase(Locale.ROOT),
                uri.getPort() == 443 ? -1 : uri.getPort(), path, null, null).toASCIIString();
        } catch (Exception invalid) {
            throw new IllegalArgumentException("Invalid billing origin", invalid);
        }
    }

    /** Only called while creating a new quote or a new session, never during recovery. */
    public static void bind(ObjectNode snapshot, ExternalBillingProperties properties) {
        String merchant = properties.getMerchantId();
        if (merchant == null || !merchant.matches("[A-Za-z0-9_-]{1,64}"))
            throw new IllegalArgumentException("Billing merchant required");
        ObjectNode origin = JSON.createObjectNode();
        origin.put("version", 1).put("base_url", normalizedBaseUrl(properties.getBaseUrl()))
            .put("merchant_id", merchant);
        if (snapshot.has(FIELD) && !origin.equals(snapshot.get(FIELD)))
            throw new IllegalStateException("BILLING_ORIGIN_MISMATCH");
        snapshot.set(FIELD, origin);
    }

    public static boolean matches(String snapshot, String merchant, ExternalBillingProperties properties) {
        try {
            JsonNode origin = JSON.readTree(snapshot).path(FIELD);
            return merchant != null && merchant.equals(properties.getMerchantId())
                && origin.isObject() && origin.size() == 3
                && origin.path("version").isIntegralNumber() && origin.path("version").canConvertToInt()
                && origin.path("version").intValue() == 1
                && origin.path("merchant_id").isTextual() && merchant.equals(origin.path("merchant_id").textValue())
                && origin.path("base_url").isTextual()
                && normalizedBaseUrl(properties.getBaseUrl()).equals(origin.path("base_url").textValue());
        } catch (Exception invalid) {
            return false;
        }
    }

    public static boolean matchesUsage(JdbcTemplate jdbc, ExternalBillingProperties properties, String usageId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT accepted_snapshot,merchant_id FROM vls_compute_billing_session WHERE usage_id=?", usageId);
        return rows.size() == 1 && matches(String.valueOf(rows.get(0).get("accepted_snapshot")),
            String.valueOf(rows.get(0).get("merchant_id")), properties);
    }

    public static void requireUsage(JdbcTemplate jdbc, ExternalBillingProperties properties, String usageId) {
        if (!matchesUsage(jdbc, properties, usageId)) throw new IllegalStateException("BILLING_ORIGIN_MISMATCH");
    }

    /** Internal local cursor/deduplication key, never an upstream merchant identity. */
    public static String namespace(ExternalBillingProperties properties) {
        return ComputeBillingSettlementService.hash("billing-origin-v1\n"
            + normalizedBaseUrl(properties.getBaseUrl()) + "\n" + properties.getMerchantId());
    }
}
