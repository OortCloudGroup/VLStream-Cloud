package com.ruoyi.vlstream.test.vlstream.compute;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/** Deployment-owned public asset identity. Empty by default; never contains a credential. */
@Data
@Component
@ConfigurationProperties(prefix = "vlstream.compute.trusted-self")
public class ComputeTrustedSelfAssetsProperties {
    private List<Asset> assets = new ArrayList<>();

    @Data
    public static class Asset {
        private String assetId;
        private String tenantId;
        private String host;
        private int port = 22;
        private String username;
        /** Base64 SSH host public key, verified out of band; not an SSH private key. */
        private String hostKey;
        /** Explicit company stock, distinct from private/paid whole-instance resources. */
        private boolean companyPool;
    }
}
