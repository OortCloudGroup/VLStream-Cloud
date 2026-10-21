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
    private String baseUrl;
    private String merchantId;
    private String apiKey;
    private boolean safetyStopEnabled = false;
    private int settlementIntervalSeconds = 60;
}
