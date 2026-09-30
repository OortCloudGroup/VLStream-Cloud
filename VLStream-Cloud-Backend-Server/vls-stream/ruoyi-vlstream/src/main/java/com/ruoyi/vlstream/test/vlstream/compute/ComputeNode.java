package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.ToString;

@Data
public class ComputeNode {
    @JsonSerialize(using = ToStringSerializer.class) private Long id;
    @JsonIgnore private String tenantId;
    private String name;
    private String provider;
    private String host;
    private int port;
    private String username;
    @JsonIgnore @ToString.Exclude private String passwordCipher;
    @JsonIgnore @ToString.Exclude private String hostKey;
    private String pythonPath;
    private String workDir;
    private int gpuIndex;
    private boolean enabled;
    private String probeState;
    private String probeJson;
    private java.util.Date probeTime;
    public boolean isPasswordConfigured() { return passwordCipher != null && !passwordCipher.isEmpty(); }
}
