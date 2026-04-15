package com.vlstream.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 算法模型统计DTO
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "AlgorithmModelStatisticsDTO", description = "算法模型统计信息")
public class AlgorithmModelStatisticsDTO {

    @ApiModelProperty(value = "模型总数")
    private Long totalCount;

    @ApiModelProperty(value = "草稿状态模型数")
    private Long draftCount;

    @ApiModelProperty(value = "测试状态模型数")
    private Long testingCount;

    @ApiModelProperty(value = "已发布模型数")
    private Long publishedCount;

    @ApiModelProperty(value = "总下载次数")
    private Long totalDownloadCount;

    @ApiModelProperty(value = "总部署次数")
    private Long totalDeployCount;

    @ApiModelProperty(value = "平均准确率")
    private BigDecimal avgAccuracy;

    @ApiModelProperty(value = "最高准确率")
    private BigDecimal maxAccuracy;

    @ApiModelProperty(value = "最低准确率")
    private BigDecimal minAccuracy;

    @ApiModelProperty(value = "ONNX格式模型数")
    private Long onnxFormatCount;

    @ApiModelProperty(value = "PyTorch格式模型数")
    private Long pytorchFormatCount;

    @ApiModelProperty(value = "TensorFlow格式模型数")
    private Long tensorflowFormatCount;

    @ApiModelProperty(value = "本周新增模型数")
    private Long weeklyNewCount;

    @ApiModelProperty(value = "本月新增模型数")
    private Long monthlyNewCount;

    @ApiModelProperty(value = "本年新增模型数")
    private Long yearlyNewCount;

    @ApiModelProperty(value = "最受欢迎的模型名称")
    private String mostPopularModelName;

    @ApiModelProperty(value = "最受欢迎的模型下载次数")
    private Long mostPopularModelDownloadCount;
} 