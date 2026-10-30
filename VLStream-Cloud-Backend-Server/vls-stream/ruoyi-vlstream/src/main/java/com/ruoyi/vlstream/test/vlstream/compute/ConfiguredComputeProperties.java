package com.ruoyi.vlstream.test.vlstream.compute;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Self-owned supply is deployment configuration; cloud providers retain database management. */
@Data
@Component
@ConfigurationProperties(prefix = "vlstream.compute.company-resources")
public class ConfiguredComputeProperties {
    private List<Resource> resources = new ArrayList<>();

    @Data
    public static class Resource {
        private String code;
        private String provider = "self";
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
        /** Official cloud supply is still deployment data, never a managed node row. */
        private String instanceId;
        private String accountScope;
        private String tenantId;
        private String host;
        private int port = 22;
        private String username = "root";
        private String hostKey;
        /** Existing administrator-verified host bindings; never written by first-use discovery. */
        private String knownHostsFile;
        private String publicKeyFingerprint;
        @com.fasterxml.jackson.annotation.JsonIgnore
        @lombok.ToString.Exclude
        private String privateKeyFile;
        @com.fasterxml.jackson.annotation.JsonIgnore
        @lombok.ToString.Exclude
        private String providerApiToken;
        private FixedPrice fixedPrice;
    }

    @Data
    public static class FixedPrice {
        private String currency = "CNY";
        /** No numeric default: absent deployment price must never start paid compute. */
        private java.math.BigDecimal perInstanceHour;
    }
}
