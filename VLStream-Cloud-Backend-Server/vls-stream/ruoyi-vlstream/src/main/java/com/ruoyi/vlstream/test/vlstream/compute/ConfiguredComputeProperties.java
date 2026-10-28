package com.ruoyi.vlstream.test.vlstream.compute;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Company supply is deployment configuration; user-owned nodes remain in the database. */
@Data
@Component
@ConfigurationProperties(prefix = "vlstream.compute.company-resources")
public class ConfiguredComputeProperties {
    /** Explicit cutover only after deployment asset/environment/legacy-ID preflight. */
    private boolean enabled;
    private List<Resource> resources = new ArrayList<>();

    @Data
    public static class Resource {
        private String code;
        /** Reserved negative namespace, immutable after first use. */
        private Long nodeId;
        private List<Long> legacyNodeIds = new ArrayList<>();
        private String assetId;
        /** Reference an already-approved asset, never a new network allowlist entry. */
        private String assetHost;
        private String name;
        private String gpuModel;
        private String pythonPath;
        private String workDir;
        private int gpuIndex;
        private boolean enabled = true;
        private boolean published = true;
        /** A physical GPU retains the existing serial queue and remote lease. */
        private int maxConcurrentTasks = 1;
    }
}
