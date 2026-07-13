package com.ruoyi.framework.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "token")
@Component
@Data
public class TokenProperties {
    private String tenantType;
    private String singleTenantId;
}
