package com.ruoyi.vlstream.test.vlstream.compute;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Deployment namespace only. Missing identity keeps company shared admission closed. */
@Data
@Component
@ConfigurationProperties(prefix = "vlstream.compute.shared-pool")
public class ComputeSharedQueueProperties {
    private String environmentId;
}
