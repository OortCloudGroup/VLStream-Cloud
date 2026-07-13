/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * VLS algorithm annotation entity mapped to vls_algorithm_annotation.
 */
@Data
@TableName("vls_algorithm_annotation")
public class AlgorithmAnnotation implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @TableId(value = "id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String tenantId;

    private String annotationName;

    private String annotationType;

    private String datasetPath;

    private Integer totalCount;

    private Integer annotatedCount;

    private String annotationStatus;

    private Integer progress;

    private String annotationRules;

    private String remark;

    @JsonAlias("createdBy")
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
    private Long createdBy;

    /**
     * Accept frontend JSON object or string payloads for annotation rules.
     */
    @JsonSetter("annotationRules")
    public void setAnnotationRules(Object annotationRules) {
        if (annotationRules == null) {
            this.annotationRules = null;
            return;
        }
        if (annotationRules instanceof String) {
            this.annotationRules = (String) annotationRules;
            return;
        }
        try {
            this.annotationRules = OBJECT_MAPPER.writeValueAsString(annotationRules);
        } catch (JsonProcessingException ex) {
            this.annotationRules = String.valueOf(annotationRules);
        }
    }

    /**
     * Preserve the frontend createdBy alias while storing createUser.
     */
    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
        if (this.createUser == null) {
            this.createUser = createdBy;
        }
    }
}
