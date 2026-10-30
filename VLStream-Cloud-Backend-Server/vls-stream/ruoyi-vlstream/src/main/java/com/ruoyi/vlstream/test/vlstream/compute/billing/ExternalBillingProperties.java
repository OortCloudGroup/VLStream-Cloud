package com.ruoyi.vlstream.test.vlstream.compute.billing;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Disabled by default. No endpoint/key defaults; credentials are never included in toString. */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "vlstream.compute.external-billing")
public class ExternalBillingProperties {
    private boolean enabled = false;
    /** Enabling read access alone must never enable funds movement. */
    private boolean allowWrites = false;
    /** Product admission is independent of transport and remains closed until configured. */
    private boolean paidAdmissionEnabled = false;
    private String baseUrl;
    private String merchantId;
    private String apiKey;
    private boolean safetyStopEnabled = false;
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
}
