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
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * VLS algorithm model entity mapped to vls_algorithm_model.
 */
@Data
@TableName("vls_algorithm_model")
public class AlgorithmModel implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String tenantId;

    private String modelName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long algorithmId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long trainingId;

    private Integer version;

    private String modelFormat;

    private String modelSize;

    private String modelPath;

    private String onnxModelPath;

    private String rknnModelPath;

    private String int8RknnModelOutputPath;

    private BigDecimal accuracy;

    private String description;

    private Integer downloadCount;

    private Integer deployCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date publishTime;

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
    private String statusName;

    @TableField(exist = false)
    private String modelDownloadPath;
}
