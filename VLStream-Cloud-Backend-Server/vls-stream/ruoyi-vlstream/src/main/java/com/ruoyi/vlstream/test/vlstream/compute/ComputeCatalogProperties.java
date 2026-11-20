package com.ruoyi.vlstream.test.vlstream.compute;

import com.ruoyi.vlstream.test.vlstream.config.ModelHubProperties;
import lombok.AccessLevel;
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
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private volatile String assignedEnvironmentId;
    private long refreshMillis = 15000;
    private long maximumAgeMillis = 60000;
    private String snapshotFile = "./data/security/compute-catalog.snapshot";

    public String getEnvironmentId() { return assignedEnvironmentId; }

    void assignEnvironment(String environmentId) { assignedEnvironmentId = environmentId; }

    public boolean configured() {
        String baseUrl = deployment.getBaseUrl();
        return baseUrl != null && !baseUrl.trim().isEmpty();
    }
}
