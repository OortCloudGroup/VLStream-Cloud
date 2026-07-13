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
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * VLS scene governance entity mapped to vls_scene_governance.
 */
@Data
@TableName("vls_scene_governance")
public class SceneGovernance implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String tenantId;

    private String name;

    private String description;

    private String cronExpression;

    private String location;

    private String cameras;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long createUser;

    private String createDept;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long updateUser;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    private Integer status;

    @TableLogic
    private Integer isDeleted;

    @TableField(exist = false)
    private String algorithm;

    @TableField(exist = false)
    private List<String> algorithmIds;

    @TableField(exist = false)
    private String algorithmName;

    @TableField(exist = false)
    private List<String> cameraIds;

    @TableField(exist = false)
    private String camerasName;

    @TableField(exist = false)
    private String devices;

    @TableField(exist = false)
    private String rules;

    @TableField(exist = false)
    private Date createdAt;

    public Date getCreatedAt() {
        return createTime;
    }

    @JsonSetter("status")
    public void setStatus(Object value) {
        if (value == null) {
            this.status = null;
            return;
        }
        if (value instanceof Number) {
            this.status = ((Number) value).intValue();
            return;
        }
        String text = String.valueOf(value).trim();
        if ("enabled".equalsIgnoreCase(text) || "enable".equalsIgnoreCase(text) || "true".equalsIgnoreCase(text)) {
            this.status = 1;
            return;
        }
        if ("disabled".equalsIgnoreCase(text) || "disable".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text)) {
            this.status = 0;
            return;
        }
        try {
            this.status = Integer.valueOf(text);
        } catch (NumberFormatException ex) {
            this.status = null;
        }
    }

    @JsonSetter("algorithmIds")
    public void setAlgorithmIds(List<String> algorithmIds) {
        this.algorithmIds = normalizeIds(algorithmIds);
        this.algorithm = join(this.algorithmIds);
    }

    @JsonSetter("cameraIds")
    public void setCameraIds(List<String> cameraIds) {
        this.cameraIds = normalizeIds(cameraIds);
        this.cameras = join(this.cameraIds);
    }

    private List<String> normalizeIds(List<String> ids) {
        if (ids == null) {
            return null;
        }
        List<String> normalized = new ArrayList<String>();
        for (Object id : ids) {
            if (id == null) {
                continue;
            }
            String text = String.valueOf(id).trim();
            if (!text.isEmpty() && !normalized.contains(text)) {
                normalized.add(text);
            }
        }
        return normalized;
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (builder.length() > 0) {
                builder.append(',');
            }
            builder.append(value);
        }
        return builder.toString();
    }
}
