package com.ruoyi.vlstream.test.vlstream.compute;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "vlstream.compute.catalog")
public class ComputeCatalogProperties {
    private String baseUrl;
    private String appId;
    @ToString.Exclude private String secretKey;
    @ToString.Exclude private String accessToken;
    private String environmentId = "localhost";
    private long refreshMillis = 15000;
    private long maximumAgeMillis = 60000;
    private String snapshotFile = "./data/security/compute-catalog.snapshot";

    public boolean configured() { return baseUrl != null && !baseUrl.trim().isEmpty(); }
}
