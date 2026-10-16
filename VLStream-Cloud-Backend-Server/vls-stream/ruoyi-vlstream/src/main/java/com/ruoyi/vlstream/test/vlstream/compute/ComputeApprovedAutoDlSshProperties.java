package com.ruoyi.vlstream.test.vlstream.compute;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Administrator-owned references only; never accepts private material through a node API. */
@Data
@Component
@ConfigurationProperties(prefix = "vlstream.compute.autodl-local-ssh")
public class ComputeApprovedAutoDlSshProperties {
    private boolean enabled;
    private String environmentId;
    private List<Binding> bindings = new ArrayList<>();

    @Data
    public static class Binding {
        private String id;
        private String tenantId;
        @ToString.Exclude private String managementOwnerSubject;
        private String instanceId;
        private String host;
        private int port;
        private String username;
        private String hostKey;
        private String publicKeyFingerprint;
        @ToString.Exclude private String privateKeyFile;
    }
}
