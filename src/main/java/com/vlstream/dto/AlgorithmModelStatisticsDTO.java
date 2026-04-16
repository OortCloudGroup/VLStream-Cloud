package com.vlstream.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Algorithm Model Statistics DTO
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "AlgorithmModelStatisticsDTO", description = "Algorithm model statistics information")
public class AlgorithmModelStatisticsDTO {

    @ApiModelProperty(value = "Total model count")
    private Long totalCount;

    @ApiModelProperty(value = "Draft status model count")
    private Long draftCount;

    @ApiModelProperty(value = "Testing status model count")
    private Long testingCount;

    @ApiModelProperty(value = "Published model count")
    private Long publishedCount;

    @ApiModelProperty(value = "Total download count")
    private Long totalDownloadCount;

    @ApiModelProperty(value = "Total deployment count")
    private Long totalDeployCount;

    @ApiModelProperty(value = "Average accuracy")
    private BigDecimal avgAccuracy;

    @ApiModelProperty(value = "Maximum accuracy")
    private BigDecimal maxAccuracy;

    @ApiModelProperty(value = "Minimum accuracy")
    private BigDecimal minAccuracy;

    @ApiModelProperty(value = "ONNX format model count")
    private Long onnxFormatCount;

    @ApiModelProperty(value = "PyTorch format model count")
    private Long pytorchFormatCount;

    @ApiModelProperty(value = "TensorFlow format model count")
    private Long tensorflowFormatCount;

    @ApiModelProperty(value = "Weekly new model count")
    private Long weeklyNewCount;

    @ApiModelProperty(value = "Monthly new model count")
    private Long monthlyNewCount;

    @ApiModelProperty(value = "Yearly new model count")
    private Long yearlyNewCount;

    @ApiModelProperty(value = "Most popular model name")
    private String mostPopularModelName;

    @ApiModelProperty(value = "Most popular model download count")
    private Long mostPopularModelDownloadCount;
} 