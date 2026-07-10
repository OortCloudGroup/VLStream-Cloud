package com.ruoyi.vlstream.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * VLS algorithm training task mapped to vls_algorithm_training.
 */
@Data
@TableName("vls_algorithm_training")
public class AlgorithmTraining implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @TableId(value = "id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String tenantId;

    private String taskName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long algorithmId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long datasetId;

    private String trainStatus;

    private Integer progress;

    private Integer epochCurrent;

    private Integer epochTotal;

    private BigDecimal accuracy;

    private BigDecimal precisionValue;

    private BigDecimal recallValue;

    private BigDecimal mapValue;

    private BigDecimal lossValue;

    private String gpuUsage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

    private String estimatedTime;

    private String modelOutputPath;

    private String onnxModelOutputPath;

    private String rknnModelOutputPath;

    private String int8RknnModelOutputPath;

    private String logPath;

    private String configParams;

    private String errorMessage;

    private String modelPath;

    private String completedAt;

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
    private String algorithmName;

    @TableField(exist = false)
    private String trainType;

    @TableField(exist = false)
    private String targetModel;

    @TableField(exist = false)
    private String datasetName;

    @TableField(exist = false)
    private String createdByName;

    @TableField(exist = false)
    private Long durationMinutes;

    @TableField(exist = false)
    private String trainStatusDesc;

    @TableField(exist = false)
    private String description;

    @TableField(exist = false)
    private Integer batchSize;

    @TableField(exist = false)
    private Integer imgSize;

    @JsonSetter("configParams")
    public void setConfigParams(Object configParams) {
        if (configParams == null) {
            this.configParams = null;
            return;
        }
        if (configParams instanceof String) {
            this.configParams = (String) configParams;
            return;
        }
        try {
            this.configParams = OBJECT_MAPPER.writeValueAsString(configParams);
        } catch (JsonProcessingException ex) {
            this.configParams = String.valueOf(configParams);
        }
    }
}
