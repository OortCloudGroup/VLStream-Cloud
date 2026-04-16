package com.vlstream.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Algorithm Model Query DTO
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "AlgorithmModelQueryDTO", description = "Algorithm model query parameters")
public class AlgorithmModelQueryDTO {

    @ApiModelProperty(value = "Current page number", example = "1")
    private Integer current = 1;

    @ApiModelProperty(value = "Page size", example = "10")
    private Integer size = 10;

    @ApiModelProperty(value = "Model name (fuzzy search)")
    private String modelName;

    @ApiModelProperty(value = "Algorithm ID")
    private Long algorithmId;

    @ApiModelProperty(value = "Training task ID")
    private Long trainingId;

    @ApiModelProperty(value = "Model version")
    private String version;

    @ApiModelProperty(value = "Model format")
    private String modelFormat;

    @ApiModelProperty(value = "Model status")
    private String status;

    @ApiModelProperty(value = "Creator")
    private Long createdBy;

    @ApiModelProperty(value = "Minimum accuracy")
    private BigDecimal minAccuracy;

    @ApiModelProperty(value = "Maximum accuracy")
    private BigDecimal maxAccuracy;

    @ApiModelProperty(value = "Publish time - start range")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime publishTimeBegin;

    @ApiModelProperty(value = "Publish time - end range")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime publishTimeEnd;

    @ApiModelProperty(value = "Creation time - start range")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTimeBegin;

    @ApiModelProperty(value = "Creation time - end range")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTimeEnd;

    @ApiModelProperty(value = "Sort field", example = "created_time")
    private String orderBy = "created_time";

    @ApiModelProperty(value = "Sort direction", example = "desc")
    private String order = "desc";
} 