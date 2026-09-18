package com.ruoyi.vlstream.test.vlstream.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "vls.model-hub")
public class ModelHubProperties {
    private String baseUrl;
    private String guestToken;
    private String appId;
    private String secretKey;
}
