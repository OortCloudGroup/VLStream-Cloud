package com.ruoyi.vlstream.test.vlstream.compute;

import com.ruoyi.vlstream.test.vlstream.config.ModelHubProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@RequiredArgsConstructor
@Component
@ConfigurationProperties(prefix = "vlstream.compute.catalog")
public class ComputeCatalogProperties {
    private final ModelHubProperties deployment;
    private String environmentId = "localhost";
    private long refreshMillis = 15000;
    private long maximumAgeMillis = 60000;
    private String snapshotFile = "./data/security/compute-catalog.snapshot";

    public boolean configured() {
        String baseUrl = deployment.getBaseUrl();
        return baseUrl != null && !baseUrl.trim().isEmpty();
    }
}
