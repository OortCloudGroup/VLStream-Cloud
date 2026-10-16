package com.ruoyi.vlstream.test.vlstream.compute;

import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/** Deployment references only. No private key material or user-controlled file path. */
@Data
@Component
@ConfigurationProperties(prefix = "vlstream.compute.ssh-identities")
public class ComputeSshIdentityProperties {
    private boolean enabled;
    private String environmentId;
    private List<Identity> identities = new ArrayList<>();

    @Data
    public static class Identity {
        private String assetId;
        @ToString.Exclude private String privateKeyFile;
        private String publicKeyFingerprint;
    }
}
