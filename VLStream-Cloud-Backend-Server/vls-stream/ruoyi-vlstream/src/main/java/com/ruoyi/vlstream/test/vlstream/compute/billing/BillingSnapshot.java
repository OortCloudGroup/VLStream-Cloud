package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.vlstream.test.vlstream.compute.ComputeBillingRate;
import java.math.BigDecimal;
import java.time.Instant;

/** Immutable historical price and exact conversion. Preserves the already-approved price policy. */
public final class BillingSnapshot {
    public final String json;
    public final String priceId;
    public final String conversionVersion;
    public final String quotaUnit;
    public final BigDecimal approvedHourlyPrice;
    public final String numerator;
    public final String denominator;
    public final long effectiveAt;
    public final long validUntil;

    private BillingSnapshot(ObjectNode snapshot) {
        json = snapshot.toString(); priceId = required(snapshot, "price_snapshot_id");
        conversionVersion = required(snapshot, "conversion_version"); quotaUnit = required(snapshot, "quota_unit");
        approvedHourlyPrice = new BigDecimal(required(snapshot, "approved_hourly_price"));
        numerator = required(snapshot, "numerator"); denominator = required(snapshot, "denominator");
        ExactQuota.compute(approvedHourlyPrice, 0, numerator, denominator);
        effectiveAt = Instant.parse(required(snapshot, "effective_at")).toEpochMilli();
        validUntil = Instant.parse(required(snapshot, "valid_until")).toEpochMilli();
        if (validUntil <= effectiveAt) throw new IllegalArgumentException("Invalid conversion validity");
    }

    public static BillingSnapshot fromExistingRate(ComputeBillingRate rate, JsonNode config, Instant at, ObjectMapper mapper) {
        rate.validate(at);
        if (!"LOCKED_FOR_RUN".equals(rate.getPricePolicy())) throw new IllegalArgumentException("Segmented supplier pricing not admitted yet");
        if (!rate.getCurrency().equals(required(config, "settlement_currency")) || !rate.getQuotaUnit().equals(required(config, "quota_unit"))
            || !rate.getConversionVersion().equals(required(config, "conversion_version"))) throw new IllegalArgumentException("Price/conversion snapshot mismatch");
        ObjectNode snapshot = mapper.createObjectNode();
        snapshot.put("price_snapshot_id", rate.getVersion());
        snapshot.put("approved_hourly_price", rate.getCostHourly().multiply(rate.getSaleMultiplier()).toPlainString());
        snapshot.put("quota_unit", rate.getQuotaUnit()); snapshot.put("conversion_version", rate.getConversionVersion());
        snapshot.put("numerator", required(config.path("quota_per_currency_unit"), "numerator"));
        snapshot.put("denominator", required(config.path("quota_per_currency_unit"), "denominator"));
        snapshot.put("effective_at", required(config, "effective_at")); snapshot.put("valid_until", required(config, "valid_until"));
        snapshot.set("accepted_rate", mapper.valueToTree(rate));
        BillingSnapshot result = new BillingSnapshot(snapshot);
        result.requireInterval(at.toEpochMilli(), Math.addExact(at.toEpochMilli(), 1)); return result;
    }

    public static BillingSnapshot read(String json, ObjectMapper mapper) {
        try { return new BillingSnapshot((ObjectNode) mapper.readTree(json)); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid persisted billing snapshot"); }
    }
    public void requireInterval(long from, long to) {
        if (from < effectiveAt || to <= from || to > validUntil) throw new IllegalArgumentException("Interval outside accepted conversion snapshot");
    }
    static String required(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || !value.isTextual() || value.asText().trim().isEmpty()) throw new IllegalArgumentException("Missing billing field: " + field);
        return value.asText();
    }
}
