package com.ruoyi.vlstream.test.vlstream.compute.billing;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Merchant connectivity and bounded paid-admission policy. No secret toString. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "vlstream.compute.external-billing")
public class ExternalBillingProperties {
    private String baseUrl;
    private String merchantId;
    private String apiKey;
    private int settlementIntervalSeconds = 60;
    private int holdStopLeadSeconds = 60;
    private int maximumRunSeconds = 0;
    /** Optional observed stop-duration evidence. It is not a supplier SLA or a stop guarantee. */
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

    /** The target self endpoint requires a valid HTTPS destination and verified user proof. */
    public boolean hasIdentityConfiguration() { return identityConfigurationReason() == null; }

    /** Local presence only; this grants no user payment authority and sends no request. */
    public boolean hasMerchantConfiguration() { return merchantConfigurationReason() == null; }

    /** New orders need explicit bounds. Original-operation recovery uses its persisted evidence. */
    public boolean paidConfigured() { return unavailableReason() == null; }

    public String identityConfigurationReason() {
        if (baseUrl == null || baseUrl.trim().isEmpty()) return "平台计费缺配置：base-url 服务地址";
        try {
            java.net.URI uri = new java.net.URI(baseUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null || uri.getPort() == 0 || uri.getPort() < -1 || uri.getPort() > 65535)
                return "平台计费配置无效：base-url 必须是有效的 HTTPS 服务地址";
        } catch (Exception invalid) { return "平台计费配置无效：base-url 必须是有效的 HTTPS 服务地址"; }
        return null;
    }

    public String merchantConfigurationReason() {
        String identity = identityConfigurationReason();
        if (identity != null) return identity;
        if (merchantId == null || merchantId.trim().isEmpty()) return "平台计费缺配置：merchant-id 商户身份";
        if (apiKey == null || apiKey.trim().isEmpty()) return "平台计费缺配置：api-key 商户凭据";
        if (!merchantId.matches("[A-Za-z0-9_-]{1,64}")) return "平台计费配置无效：merchant-id 商户身份格式";
        if (!cleanIdentity(apiKey)) return "平台计费配置无效：api-key 商户凭据格式";
        return null;
    }

    /** Local presence only. Never contacts the wallet or creates an upstream grant. */
    public String unavailableReason() {
        String merchant = merchantConfigurationReason();
        if (merchant != null) return merchant;
        if (maximumRunSeconds < 1 || maximumRunSeconds > 86400) return "平台计费缺少有效配置：maximum-run-seconds 运行时限";
        if (holdStopLeadSeconds < 1 || holdStopLeadSeconds > 3600)
            return "平台计费配置无效：hold-stop-lead-seconds 停机提前量必须在 1 到 3600 秒之间";
        if (verifiedProviderStopSeconds != null) {
            if (verifiedProviderStopSeconds < 1)
                return "平台计费配置无效：verified-provider-stop-seconds 停机观测证据须为正数";
            if (holdStopLeadSeconds < verifiedProviderStopSeconds)
                return "平台计费配置无效：hold-stop-lead-seconds 必须覆盖已配置的停机观测时长";
        }
        if (settlementIntervalSeconds < 1 || settlementIntervalSeconds > 3600)
            return "平台计费配置无效：settlement-interval-seconds 结算周期必须在 1 到 3600 秒之间";
        try { ExactQuota.integer(maximumHoldQuota, true); }
        catch (RuntimeException invalid) { return "平台计费缺少有效配置：maximum-hold-quota 资金上限"; }
        return null;
    }

    @javax.annotation.PostConstruct
    public void validateDeployment() {
        String reason = unavailableReason();
        if (reason != null) org.slf4j.LoggerFactory.getLogger(ExternalBillingProperties.class).warn("{}", reason);
    }
    private static boolean cleanIdentity(String value) {
        if (!value.equals(value.trim())) return false;
        for (int index = 0; index < value.length(); index++) if (Character.isISOControl(value.charAt(index))) return false;
        return true;
    }
}
