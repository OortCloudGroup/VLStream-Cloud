package com.ruoyi.vlstream.test.vlstream.compute;

import lombok.Data;
import lombok.ToString;
import javax.validation.constraints.*;

public final class ComputeRequests {
    private ComputeRequests() { }

    @Data
    public static class Node {
        @NotBlank @Size(max = 100) private String name;
        @Pattern(regexp = "autodl|aliyun|self") private String provider = "autodl";
        @Size(max = 100) private String serverGroup;
        @Size(max = 100) private String gpuModel;
        @NotBlank @Size(max = 253) private String host;
        @Min(1) @Max(65535) private int port = 22;
        @NotBlank @Pattern(regexp = "[a-zA-Z_][a-zA-Z0-9_-]{0,63}") private String username = "root";
        @NotNull @Pattern(regexp = "PASSWORD|DEPLOYMENT_KEY") private String authMode = "PASSWORD";
        @Size(max = 1000) @ToString.Exclude private String password;
        @NotBlank @Size(max = 500) private String pythonPath = "/root/autodl-tmp/vlstream/envs/vls-standard/bin/python";
        @NotBlank @Size(max = 500) private String workDir = "/root/autodl-tmp/vlstream";
        @Min(0) @Max(15) private int gpuIndex;
        private boolean enabled = true;
    }

    @Data
    public static class Start {
        @Pattern(regexp = "preset|algorithm") private String modelSource;
        @NotNull private Long nodeId;
        @NotNull private Long datasetId;
        private Integer epochs = 10;
        private Integer batchSize = 16;
        private Integer imgSize = 640;
        @Size(max = 1000) private String extraParams;
    }

    @Data
    public static class Offer {
        @Pattern(regexp = "INSTANCE_EXCLUSIVE|SHARED_GPU_QUEUE") private String schedulingMode;
        private boolean published;
        @NotBlank @Pattern(regexp = "FREE|POINTS") private String billingType;
        @DecimalMin("0.01") private java.math.BigDecimal pointsPerHour;
    }
    @Data
    public static class Observation {
        @NotBlank @Pattern(regexp = "UNKNOWN|RUNNING|STOPPED") private String powerState;
        @NotBlank @Pattern(regexp = "UNKNOWN|AVAILABLE_HINT|UNAVAILABLE") private String capacityState;
        @NotBlank @Size(max = 400) private String note;
    }
    @Data
    public static class PublicIdentity {
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_.:-]{1,128}") private String accountScope;
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,100}") private String instanceId;
    }
    @Data
    public static class ProviderSettings {
        @NotBlank @Pattern(regexp = "MANUAL|API") private String controlMode;
        @Pattern(regexp = "AUTODL_PRO|ECS") private String providerProduct;
        @Size(max = 100) private String providerInstanceId;
        @Size(max = 100) private String providerRegion;
        @Pattern(regexp = "KeepCharging|StopCharging") private String requestedStoppedMode;
        @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
        @Size(max = 2000) @ToString.Exclude private String token;
        @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
        @Size(max = 200) @ToString.Exclude private String accessKeyId;
        @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
        @Size(max = 2000) @ToString.Exclude private String accessKeySecret;
        private boolean clearCredentials;
    }
}
