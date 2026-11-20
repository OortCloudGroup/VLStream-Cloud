package com.ruoyi.vlstream.test.vlstream.compute;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** The execution catalog supplies the environment. A local property is only for legacy supply. */
@Data
@Component
@ConfigurationProperties(prefix = "vlstream.compute.shared-pool")
public class ComputeSharedQueueProperties {
    private String environmentId;
    @Autowired
    private ComputeCatalogProperties catalog;

    public String getEnvironmentId() {
        return catalog != null && catalog.configured() ? catalog.getEnvironmentId() : environmentId;
    }
}
