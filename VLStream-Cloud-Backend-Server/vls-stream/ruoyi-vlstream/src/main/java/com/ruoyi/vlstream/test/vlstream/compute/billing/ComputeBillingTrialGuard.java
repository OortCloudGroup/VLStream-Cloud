package com.ruoyi.vlstream.test.vlstream.compute.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.vlstream.test.vlstream.compute.ComputeNode;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Narrow, opt-in acceptance permit for the existing DEV wallet and official instance.
 * The paid-run primary key and existing immutable journals are the durable permit/usage record.
 * This class never authorizes cleanup, contacts a provider, or changes an existing request. */
@Service
public class ComputeBillingTrialGuard {
    public static final String TARGET = "https://workup-dev.myoumuamua.com:6433/bus/apaas-newapi";
    public static final String MARKER = "trial_guard";
    private static final String INSTANCE = "pro-7920a22db144";
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final ExternalBillingProperties properties;

    public ComputeBillingTrialGuard(JdbcTemplate jdbc, ObjectMapper json, ExternalBillingProperties properties) {
        this.jdbc = jdbc; this.json = json; this.properties = properties;
    }

    public boolean requested() { return requested(properties); }
    /** Scheduler selection is independent of expiry: the selected original must still stop. */
    public boolean selectsRun(String runId) {
        return !requested() || properties.getTrial() != null && runId != null && runId.equals(properties.getTrial().getRunId());
    }
    public boolean selectsUsage(String usageId) {
        if (!requested()) return true;
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT run_id,payer_user_id,merchant_id,node_id FROM vls_compute_paid_run WHERE usage_id=?", usageId);
        return rows.size() == 1 && selectsRun(text(rows.get(0),"run_id"))
            && "13".equals(text(rows.get(0),"payer_user_id")) && "vls".equals(text(rows.get(0),"merchant_id"))
            && "-6000".equals(text(rows.get(0),"node_id"));
    }
    private static boolean requested(ExternalBillingProperties p) {
        ExternalBillingProperties.Trial t = p.getTrial();
        // The known DEV target cannot fall back to unrestricted writes when a permit is absent.
        return devHost(p) || t != null && (t.isEnabled() || t.getRunId() != null);
    }
    private static boolean devHost(ExternalBillingProperties p) {
        try {
            String host = java.net.URI.create(p.getBaseUrl()).getHost();
            return host != null && "workup-dev.myoumuamua.com".equalsIgnoreCase(host.replaceAll("\\.+$", ""));
        }
        catch (RuntimeException invalid) { return false; }
    }
    private static String base(ExternalBillingProperties p) {
        String value = p.getBaseUrl();
        if (value == null) return null;
        try {
            java.net.URI uri = java.net.URI.create(value);
            if ("https".equalsIgnoreCase(uri.getScheme()) && "workup-dev.myoumuamua.com".equalsIgnoreCase(uri.getHost())
                && uri.getPort() == 6433 && uri.getRawUserInfo() == null && uri.getRawQuery() == null && uri.getRawFragment() == null
                && "/bus/apaas-newapi".equals(uri.getRawPath().replaceAll("/+$", ""))) return TARGET;
        } catch (RuntimeException invalid) { /* Invalid or different endpoints never validate a trial permit. */ }
        return value;
    }
    private static ExternalBillingProperties.Trial active(ExternalBillingProperties p) {
        ExternalBillingProperties.Trial t = p.getTrial();
        if (t == null || !t.isEnabled() || !TARGET.equals(base(p)) || !"vls".equals(p.getMerchantId()))
            throw violation("TRIAL_NOT_ENABLED");
        if (t.getRunId() == null || !t.getRunId().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
            throw violation("TRIAL_RUN_ID_REQUIRED");
        capCny(t);
        if (expiry(t) <= System.currentTimeMillis()) throw violation("TRIAL_PERMIT_EXPIRED");
        return t;
    }
    private static BigDecimal capCny(ExternalBillingProperties.Trial t) {
        String value = t.getMaximumWalletCny();
        if (value == null || !value.matches("(?:0|[1-9][0-9]*)\\.[0-9]{2}")) throw violation("TRIAL_CNY_CAP_INVALID");
        BigDecimal cap = new BigDecimal(value);
        if (cap.signum() <= 0 || cap.compareTo(new BigDecimal("3.47")) > 0) throw violation("TRIAL_CNY_CAP_INVALID");
        return cap;
    }
    private static long expiry(ExternalBillingProperties.Trial t) {
        try { return Instant.parse(t.getExpiresAt()).toEpochMilli(); }
        catch (RuntimeException invalid) { throw violation("TRIAL_PERMIT_EXPIRY_REQUIRED"); }
    }
    public static void requirePayerScope(ExternalBillingProperties p, BillingPayer payer) {
        if (!requested(p)) return;
        active(p);
        // ID is taken only from the verified /api/user/self flow, never a browser parameter/name.
        if (payer == null || !"13".equals(payer.userId)) throw violation("TRIAL_PAYER_MISMATCH");
    }
    public void requirePayer(BillingPayer payer) { requirePayerScope(properties, payer); }
    public String requestedRunId() { return requested() ? active(properties).getRunId() : null; }
    public void requireNode(ComputeNode node) {
        if (!requested()) return;
        active(properties);
        if (node == null || !Long.valueOf(-6000L).equals(node.getId()) || !node.isConfigurationManaged()
            || !"official-autodl".equals(node.getConfigurationCode()) || !"autodl".equals(node.getProvider())
            || !"AUTODL_PRO".equals(node.getProviderProduct()) || !INSTANCE.equals(node.getProviderInstanceId()))
            throw violation("TRIAL_INSTANCE_MISMATCH");
    }

    /** Ceiling reservation must fit a FLOOR of the approved CNY cap at this current conversion. */
    public String quoteRunId(ComputeNode node, BillingPayer payer, ObjectNode snapshot, long reserveMillis, BigInteger hold) {
        if (!requested()) return null;
        requirePayer(payer); requireNode(node);
        ExternalBillingProperties.Trial t = active(properties);
        BillingSnapshot price = BillingSnapshot.read(snapshot.toString(), json);
        if (properties.getMaximumRunSeconds() <= 0 || properties.getMaximumRunSeconds() > 600
            || reserveMillis <= 0 || reserveMillis > 960000 || expiry(t) - System.currentTimeMillis() < reserveMillis
            || price.approvedHourlyPrice.compareTo(new BigDecimal("13.00")) != 0
            || !"CNY".equals(snapshot.path("accepted_rate").path("currency").asText())
            || !"VLS_FIXED".equals(snapshot.path("accepted_rate").path("pricePolicy").asText()))
            throw violation("TRIAL_QUOTE_SCOPE_MISMATCH");
        BigInteger cap = capQuota(capCny(t), price);
        if (hold == null || hold.signum() <= 0 || hold.compareTo(cap) > 0) throw violation("TRIAL_CNY_CAP_EXCEEDED");
        ObjectNode permit = snapshot.putObject(MARKER);
        permit.put("version", 1).put("run_id", t.getRunId()).put("payer_user_id", "13")
            .put("instance_id", INSTANCE).put("instance_key", node.getPublicInstanceKey()).put("node_id", "-6000")
            .put("merchant_id", "vls").put("base_url", TARGET).put("maximum_cny", capCny(t).toPlainString())
            .put("maximum_quota", cap.toString()).put("reservation_quota", hold.toString())
            .put("expires_at", t.getExpiresAt());
        return t.getRunId();
    }
    private static BigInteger capQuota(BigDecimal cny, BillingSnapshot price) {
        BigInteger n = cny.unscaledValue().multiply(ExactQuota.integer(price.numerator, true));
        BigInteger d = BigInteger.TEN.pow(cny.scale()).multiply(ExactQuota.integer(price.denominator, true));
        BigInteger result = n.divide(d);
        ExactQuota.integer(result.toString(), true);
        return result;
    }
    private JsonNode snapshot(Map<String,Object> session) {
        try { return json.readTree(String.valueOf(session.get("accepted_snapshot"))); }
        catch (Exception invalid) { throw violation("TRIAL_SNAPSHOT_INVALID"); }
    }
    private Map<String,Object> session(String usageId) {
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT * FROM vls_compute_billing_session WHERE usage_id=?", usageId);
        if (rows.size() != 1) throw violation("TRIAL_SESSION_MISSING");
        return rows.get(0);
    }
    public boolean protectedUsage(String usageId) {
        Map<String,Object> s = session(usageId);
        return requested() || snapshot(s).has(MARKER);
    }
    /** Original cleanup keeps identity/run scope, but never requires a live spend permit or rate. */
    public void requireCleanupScope(String usageId) { stored(usageId, false); }
    private JsonNode stored(String usageId) { return stored(usageId, true); }
    private JsonNode stored(String usageId, boolean newSpend) {
        Map<String,Object> s = session(usageId); JsonNode saved = snapshot(s);
        if (!requested() && !saved.has(MARKER)) return null;
        JsonNode permit = saved.path(MARKER);
        ExternalBillingProperties.Trial t = newSpend ? active(properties) : cleanupPermit(permit);
        BillingSnapshot price = BillingSnapshot.read(saved.toString(), json);
        if (!permit.isObject() || permit.path("version").asInt() != 1
            || !t.getRunId().equals(permit.path("run_id").asText())
            || !t.getExpiresAt().equals(permit.path("expires_at").asText())
            || !capCny(t).toPlainString().equals(permit.path("maximum_cny").asText())
            || !capQuota(capCny(t), price).toString().equals(permit.path("maximum_quota").asText())
            || !"13".equals(permit.path("payer_user_id").asText()) || !"13".equals(text(s,"payer_user_id"))
            || !"vls".equals(text(s,"merchant_id")) || !"vls".equals(permit.path("merchant_id").asText())
            || !TARGET.equals(permit.path("base_url").asText()) || !INSTANCE.equals(permit.path("instance_id").asText())
            || !"-6000".equals(permit.path("node_id").asText()) || !"node-server:-6000".equals(text(s,"instance_ref")))
            throw violation("TRIAL_PERMIT_CHANGED");
        List<Map<String,Object>> runs = jdbc.queryForList("SELECT * FROM vls_compute_paid_run WHERE run_id=?", t.getRunId());
        if (runs.size() != 1) throw violation("TRIAL_ORIGINAL_RUN_MISSING");
        Map<String,Object> run = runs.get(0);
        if (!usageId.equals(text(run,"usage_id")) || !"-6000".equals(text(run,"node_id"))
            || !permit.path("instance_key").asText().equals(text(run,"instance_key"))
            || !permit.path("reservation_quota").asText().equals(text(run,"reserve_quota"))
            || !text(s,"tenant_id").equals(text(run,"tenant_id")) || !text(s,"opening_user_id").equals(text(run,"opening_user_id"))
            || !"13".equals(text(run,"payer_user_id")) || !"vls".equals(text(run,"merchant_id")))
            throw violation("TRIAL_ORIGINAL_RUN_MISMATCH");
        try { if (!saved.equals(json.readTree(text(run,"quote_snapshot")))) throw violation("TRIAL_SNAPSHOT_CONFLICT"); }
        catch (java.io.IOException invalid) { throw violation("TRIAL_SNAPSHOT_INVALID"); }
        if (ExactQuota.integer(permit.path("reservation_quota").asText(), true).compareTo(capQuota(capCny(t), price)) > 0)
            throw violation("TRIAL_CNY_CAP_EXCEEDED");
        return saved;
    }
    private ExternalBillingProperties.Trial cleanupPermit(JsonNode permit) {
        ExternalBillingProperties.Trial configured = properties.getTrial();
        if (!TARGET.equals(base(properties)) || !"vls".equals(properties.getMerchantId())
            || configured == null || configured.getRunId() == null
            || !configured.getRunId().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
            || !configured.getRunId().equals(permit.path("run_id").asText()))
            throw violation("TRIAL_CLEANUP_SCOPE_MISMATCH");
        ExternalBillingProperties.Trial original = new ExternalBillingProperties.Trial();
        original.setRunId(configured.getRunId());
        original.setMaximumWalletCny(permit.path("maximum_cny").asText());
        original.setExpiresAt(permit.path("expires_at").asText());
        capCny(original); expiry(original); // Validate the stored values, not whether they remain current.
        return original;
    }
    public void requireNewUsage(String usageId, JsonNode currentConfig) {
        JsonNode saved = stored(usageId); if (saved == null) return;
        JsonNode actual = currentConfig != null && currentConfig.has("success")
            ? currentConfig.path("success").asBoolean(false) ? currentConfig.path("data") : null : currentConfig;
        if (actual == null || !actual.isObject() || !"CNY".equals(actual.path("settlement_currency").asText()))
            throw violation("TRIAL_CONVERSION_UNCONFIRMED");
        for (String key : new String[]{"conversion_version","quota_unit","effective_at","valid_until"})
            if (!actual.path(key).isTextual() || !actual.path(key).asText().equals(saved.path(key).asText()))
                throw violation("TRIAL_CONVERSION_CHANGED");
        for (String key : new String[]{"numerator","denominator"})
            if (!actual.path("quota_per_currency_unit").path(key).isTextual()
                || !actual.path("quota_per_currency_unit").path(key).asText().equals(saved.path(key).asText()))
                throw violation("TRIAL_CONVERSION_CHANGED");
        long now = System.currentTimeMillis(); BillingSnapshot.read(saved.toString(), json).requireInterval(now, Math.addExact(now, 1));
    }

    /** Caller holds the existing usage/session locks. Unknown and refunded slices still reserve gross budget. */
    public void requireCaptureBudget(String usageId, BigInteger additional) {
        JsonNode saved = stored(usageId); if (saved == null) return;
        if (!TransactionSynchronizationManager.isActualTransactionActive()) throw violation("TRIAL_BUDGET_LOCK_REQUIRED");
        jdbc.queryForMap("SELECT usage_id FROM vls_compute_billing_session WHERE usage_id=? FOR UPDATE", usageId);
        BigInteger total = additional;
        if (total == null || total.signum() < 0) throw violation("TRIAL_BUDGET_INVALID");
        // Only this preparation-only marker proves a slice was never allocated/sent. A later
        // UNKNOWN, rejected or refunded request must never free its original gross reservation.
        for (String amount : jdbc.queryForList("SELECT amount_text FROM vls_compute_billing_slice WHERE usage_id=? AND NOT(state='BLOCKED' AND COALESCE(error_code,'') IN ('TRIAL_UNALLOCATED_BUDGET','TRIAL_UNALLOCATED_PERMIT') AND charge_id IS NULL AND ledger_entry_id IS NULL AND lease_token IS NULL AND lease_until_ms=0)", String.class, usageId))
            total = total.add(ExactQuota.integer(amount, false));
        BigInteger cap = ExactQuota.integer(saved.path(MARKER).path("maximum_quota").asText(), true);
        BigInteger reservation = ExactQuota.integer(saved.path(MARKER).path("reservation_quota").asText(), true);
        if (total.compareTo(cap.min(reservation)) > 0) throw violation("TRIAL_GROSS_CAP_EXCEEDED");
        if (!jdbc.queryForList("SELECT refund_intent_id FROM vls_compute_refund_intent WHERE usage_id=?", usageId).isEmpty())
            throw violation("TRIAL_REFUND_PHASE_NO_NEW_CAPTURE");
    }

    public void requireFundingPreparation(String usageId, String operation, String amount) {
        if ("RELEASE".equals(operation)) { requireCleanupScope(usageId); return; }
        JsonNode saved = stored(usageId); if (saved == null) return;
        if (!"HOLD".equals(operation)) throw violation("TRIAL_TOPUP_FORBIDDEN");
        if (!saved.path(MARKER).path("reservation_quota").asText().equals(amount)) throw violation("TRIAL_HOLD_AMOUNT_MISMATCH");
        if (jdbc.queryForList("SELECT intent_id FROM vls_compute_funding_intent WHERE usage_id=? AND operation_kind='HOLD'", usageId).size() > 1)
            throw violation("TRIAL_MULTIPLE_HOLDS");
    }
    public void requireNewFunding(String usageId, String operation, String amount, JsonNode freshConfig) {
        if ("RELEASE".equals(operation)) { requireCleanupScope(usageId); return; }
        if (!protectedUsage(usageId)) return;
        requireNewUsage(usageId, freshConfig); requireFundingPreparation(usageId, operation, amount);
        JsonNode saved = stored(usageId);
        List<Map<String,Object>> holds = jdbc.queryForList("SELECT intent_id FROM vls_compute_funding_intent WHERE usage_id=? AND operation_kind='HOLD'", usageId);
        String original = jdbc.queryForObject("SELECT hold_intent_id FROM vls_compute_paid_run WHERE run_id=?", String.class, saved.path(MARKER).path("run_id").asText());
        if (holds.size() != 1 || original == null || !original.equals(text(holds.get(0),"intent_id"))) throw violation("TRIAL_HOLD_BINDING_MISMATCH");
    }
    private static String text(Map<String,Object> row, String key) { Object value=row.get(key); return value==null?null:String.valueOf(value); }
    private static IllegalStateException violation(String code) { return new IllegalStateException(code); }
}
