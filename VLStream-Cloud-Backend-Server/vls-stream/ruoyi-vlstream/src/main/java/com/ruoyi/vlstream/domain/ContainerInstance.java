/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * VLS container instance mapped to vls_container_instance.
 */
@Data
@TableName("vls_container_instance")
public class ContainerInstance implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String tenantId;

    private String instanceName;

    private String containerId;

    private String imageName;

    private String imageType;

    private String imageTag;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long resourceTypeId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long resourceSpecId;

    private Integer instanceCount;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long algorithmId;

    private String instanceType;

    private String cpuLimit;

    private String memoryLimit;

    private String gpuLimit;

    private String portConfig;

    private String envConfig;

    private String volumeConfig;

    private String instanceStatus;

    private String healthStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date stopTime;

    private Integer restartCount;

    private BigDecimal cpuUsage;

    private BigDecimal memoryUsage;

    private BigDecimal gpuUsage;

    private String logsPath;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long createUser;

    private String createDept;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long updateUser;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    @JsonIgnore
    @TableField("status")
    private Integer recordStatus;

    @TableLogic
    private Integer isDeleted;

    @TableField(exist = false)
    private String name;

    @TableField(exist = false)
    private String image;

    @TableField(exist = false)
    private String status;

    @TableField(exist = false)
    private String portMappings;

    @TableField(exist = false)
    private String envVariables;

    @TableField(exist = false)
    private String envVars;

    @TableField(exist = false)
    private String port;

    @TableField(exist = false)
    private String description;

    @TableField(exist = false)
    private String algorithmName;

    public String getName() {
        return firstText(name, instanceName);
    }

    @JsonSetter("name")
    public void setName(String name) {
        this.name = name;
        if (hasText(name)) {
            this.instanceName = name.trim();
        }
    }

    public String getImage() {
        if (hasText(image)) {
            return image;
        }
        if (!hasText(imageName)) {
            return null;
        }
        if (!hasText(imageTag)) {
            return imageName;
        }
        return imageName + ":" + imageTag;
    }

    @JsonSetter("image")
    public void setImage(String image) {
        this.image = image;
        parseImage(image);
    }

    public String getStatus() {
        return firstText(status, instanceStatus);
    }

    @JsonSetter("status")
    public void setStatus(String status) {
        this.status = status;
        if (hasText(status)) {
            this.instanceStatus = status.trim();
        }
    }

    public String getPortMappings() {
        return firstText(portMappings, portConfig);
    }

    @JsonSetter("portMappings")
    public void setPortMappings(String portMappings) {
        this.portMappings = portMappings;
        this.portConfig = portMappings;
    }

    public String getEnvVariables() {
        return firstText(envVariables, envConfig);
    }

    @JsonSetter("envVariables")
    public void setEnvVariables(String envVariables) {
        this.envVariables = envVariables;
        this.envConfig = envVariables;
    }

    @JsonSetter("envVars")
    public void setEnvVars(String envVars) {
        this.envVars = envVars;
        this.envConfig = envVars;
    }

    @JsonSetter("port")
    public void setPort(String port) {
        this.port = port;
        if (hasText(port)) {
            this.portConfig = "{\"port\":\"" + port.trim() + "\"}";
        }
    }

    @JsonSetter("cpu")
    public void setCpu(String cpu) {
        this.cpuLimit = cpu;
    }

    @JsonSetter("memory")
    public void setMemory(String memory) {
        this.memoryLimit = memory;
    }

    @JsonSetter("gpu")
    public void setGpu(String gpu) {
        this.gpuLimit = gpu;
    }

    private void parseImage(String value) {
        if (!hasText(value)) {
            return;
        }
        String trimmed = value.trim();
        int slash = trimmed.lastIndexOf('/');
        int colon = trimmed.lastIndexOf(':');
        if (colon > slash) {
            this.imageName = trimmed.substring(0, colon);
            this.imageTag = trimmed.substring(colon + 1);
        } else {
            this.imageName = trimmed;
        }
    }

    private String firstText(String first, String second) {
        return hasText(first) ? first : second;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
