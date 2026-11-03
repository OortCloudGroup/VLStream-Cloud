package com.ruoyi.vlstream.test.vlstream.compute.billing;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** One deployment mode; explicit legacy gates retain their previous semantics. No secret toString. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "vlstream.compute.external-billing")
public class ExternalBillingProperties {
    public enum Mode { OFF, READ_ONLY, PAID }
    private Mode mode;
    /** Deprecated compatibility inputs. Null means absent, not permission. */
    private Boolean enabled;
    /** Enabling read access alone must never enable funds movement. */
    private Boolean allowWrites;
    /** Product admission is independent of transport and remains closed until configured. */
    private Boolean paidAdmissionEnabled;
    private String baseUrl;
    private String merchantId;
    private String apiKey;
    private Boolean safetyStopEnabled;
    private int settlementIntervalSeconds = 60;
    private int holdStopLeadSeconds = 60;
    private int maximumRunSeconds = 0;
    private Integer verifiedProviderStopSeconds;
    private String maximumHoldQuota;
    /** One explicitly configured DEV acceptance run. No default permission or money amount. */
    private Trial trial = new Trial();
    @Getter
    @Setter
    public static class Trial {
        private boolean enabled = false;
        private String runId;
        private String maximumWalletCny;
        private String expiresAt;
    }
    /** Backend-approved, time-limited supplier price evidence, keyed by canonical node id. */
    private java.util.Map<String, com.ruoyi.vlstream.test.vlstream.compute.ComputeBillingRate> rates = new java.util.LinkedHashMap<>();

    public boolean isEnabled() { return mode == null ? Boolean.TRUE.equals(enabled) : mode != Mode.OFF; }
    public boolean isAllowWrites() { return mode == null ? Boolean.TRUE.equals(allowWrites) : mode == Mode.PAID; }
    public boolean isPaidAdmissionEnabled() { return mode == null ? Boolean.TRUE.equals(paidAdmissionEnabled) : mode == Mode.PAID; }
    public boolean isSafetyStopEnabled() { return mode == null ? Boolean.TRUE.equals(safetyStopEnabled) : mode == Mode.PAID; }
    public void setEnabled(Boolean value) { enabled = value; }
    public void setEnabled(boolean value) { enabled = value; }
    public void setAllowWrites(Boolean value) { allowWrites = value; }
    public void setAllowWrites(boolean value) { allowWrites = value; }
    public void setPaidAdmissionEnabled(Boolean value) { paidAdmissionEnabled = value; }
    public void setPaidAdmissionEnabled(boolean value) { paidAdmissionEnabled = value; }
    public void setSafetyStopEnabled(Boolean value) { safetyStopEnabled = value; }
    public void setSafetyStopEnabled(boolean value) { safetyStopEnabled = value; }
    public boolean paidConfigured() { return isEnabled() && isAllowWrites() && isPaidAdmissionEnabled() && isSafetyStopEnabled(); }

    /** Local presence only. Never contacts the wallet or creates an upstream grant. */
    public String unavailableReason() {
        if (!isEnabled()) return "平台计费服务尚未启用，请联系部署管理员";
        if (baseUrl == null || baseUrl.trim().isEmpty()) return "平台计费服务地址未配置";
        if (merchantId == null || merchantId.trim().isEmpty() || apiKey == null || apiKey.trim().isEmpty()) return "平台计费应用身份未配置，请联系部署管理员";
        if (!paidConfigured()) return mode == Mode.READ_ONLY ? "当前仅开放余额查询，尚未启用付费开机" : "平台计费部署配置不完整，请联系部署管理员";
        return null;
    }

    @javax.annotation.PostConstruct
    public void validateDeployment() {
        if (mode == null) {
            if (enabled != null || allowWrites != null || paidAdmissionEnabled != null || safetyStopEnabled != null)
                org.slf4j.LoggerFactory.getLogger(ExternalBillingProperties.class).warn("旧版计费布尔配置仍按原语义生效；请迁移为单一 external-billing.mode，不能混用冲突的旧开关");
            return;
        }
        conflict(enabled, mode != Mode.OFF, "enabled");
        conflict(allowWrites, mode == Mode.PAID, "allow-writes");
        conflict(paidAdmissionEnabled, mode == Mode.PAID, "paid-admission-enabled");
        conflict(safetyStopEnabled, mode == Mode.PAID, "safety-stop-enabled");
        if (mode == Mode.OFF) return;
        try {
            java.net.URI uri = new java.net.URI(baseUrl == null ? "" : baseUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
                throw new IllegalArgumentException();
        } catch (Exception invalid) { throw new IllegalStateException("计费 mode 已启用，但 base-url 不是有效的 HTTPS 服务地址"); }
        if (merchantId == null || merchantId.trim().isEmpty() || apiKey == null || apiKey.trim().isEmpty())
            throw new IllegalStateException("计费 mode 已启用，但 merchant-id 或 api-key 未配置；不输出凭据值");
        if (mode == Mode.PAID) {
            if (verifiedProviderStopSeconds == null || verifiedProviderStopSeconds < 1 || holdStopLeadSeconds < verifiedProviderStopSeconds
                || maximumRunSeconds < 1 || maximumRunSeconds > 86400)
                throw new IllegalStateException("PAID 模式缺少有效运行时限或可靠停机预算，hold-stop-lead-seconds 必须覆盖 verified-provider-stop-seconds");
            try { ExactQuota.integer(maximumHoldQuota, true); }
            catch (RuntimeException invalid) { throw new IllegalStateException("PAID 模式缺少有效的 maximum-hold-quota 上限"); }
        }
    }
    private void conflict(Boolean legacy, boolean expected, String name) {
        if (legacy != null && legacy != expected) throw new IllegalStateException("external-billing.mode 与旧开关 " + name + " 冲突；请移除旧开关，不能静默改变部署语义");
    }
}
