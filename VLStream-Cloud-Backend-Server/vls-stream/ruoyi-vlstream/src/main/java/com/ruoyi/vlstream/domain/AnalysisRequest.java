package com.ruoyi.vlstream.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/** Persisted intelligent-analysis request and its processing state. */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vls_analysis_request")
public class AnalysisRequest extends VlsTenantEntity {

    private static final long serialVersionUID = 1L;

    private String analysisName;
    private String analysisType;
    private String deviceIds;
    private String regionInfo;
    private String timeRange;
    private String images;
    private String requestStatus;
    private Integer progress;
    private String resultPath;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date completeTime;

    private String errorMessage;
    private String description;

    @TableField(exist = false)
    private String cameraName;
}
