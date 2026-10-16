package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.ToString;

@Data
public class ComputeNode {
    private String schedulingMode = "INSTANCE_EXCLUSIVE";
    @JsonSerialize(using = ToStringSerializer.class) private Long id;
    @JsonIgnore private String tenantId;
    private String name;
    private String provider;
    private String resourceScope;
    private String serverGroup;
    private String gpuModel;
    private String billingType;
    private java.math.BigDecimal pointsPerHour;
    private String powerState;
    private String capacityState;
    private String capacitySource;
    private java.util.Date capacityCheckedAt;
    private java.util.Date powerCheckedAt;
    private String providerInstanceId;
    @JsonIgnore private String publicInstanceKey;
    @JsonIgnore @ToString.Exclude private String managementOwnerSubject;
    private String managementOwnership;
    private String controlMode;
    private String providerProduct;
    private String providerRegion;
    private String requestedStoppedMode;
    private String actualStoppedMode;
    private String providerChargeType;
    /** Purchased/requested specification, never free GPU stock. */
    private Integer providerGpuAmount;
    private Boolean providerLocked;
    private long lifecycleRevision;
    private String providerCostJson;
    @JsonIgnore @ToString.Exclude private String providerCredentialCipher;
    private String lifecycleMessage;
    private String powerOperationId;
    private String powerOperationAction;
    private java.util.Date powerOperationStartedAt;
    private boolean powerOperationSubmitted;
    private String host;
    private int port;
    private String username;
    private String authMode = "PASSWORD";
    @JsonIgnore @ToString.Exclude private String passwordCipher;
    @JsonIgnore @ToString.Exclude private String hostKey;
    private String pythonPath;
    private String workDir;
    private int gpuIndex;
    private boolean enabled;
    private String probeState;
    private String probeJson;
    private java.util.Date probeTime;
    private java.util.Date probeRequestedAt;
    public boolean isPasswordConfigured() { return passwordCipher != null && !passwordCipher.isEmpty(); }
    public boolean isProviderCredentialConfigured() { return providerCredentialCipher != null && !providerCredentialCipher.isEmpty(); }
    public boolean isSelfServiceNode() {
        return "PRIVATE".equals(resourceScope) && publicInstanceKey == null && "INSTANCE_EXCLUSIVE".equals(schedulingMode);
    }
}
