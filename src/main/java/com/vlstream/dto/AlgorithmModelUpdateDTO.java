package com.vlstream.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.validation.constraints.Pattern;
import java.math.BigDecimal;

/**
 * Algorithm Model Update DTO
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "AlgorithmModelUpdateDTO", description = "Algorithm model update parameters")
public class AlgorithmModelUpdateDTO {

    @ApiModelProperty(value = "Model ID", required = true, example = "1")
    @NotNull(message = "Model ID cannot be empty")
    private Long id;

    @ApiModelProperty(value = "Model name", example = "Object Detection Model v1.0")
    @Size(max = 100, message = "Model name length cannot exceed 100 characters")
    private String modelName;

    @ApiModelProperty(value = "Model version", example = "1")
    private Integer version;

    @ApiModelProperty(value = "Model format", example = "ONNX")
    @Pattern(regexp = "^(onnx|pt)$", message = "Model format can only be onnx or pt")
    private String modelFormat;

    @ApiModelProperty(value = "Model size", example = "100MB")
    @Size(max = 20, message = "Model size description length cannot exceed 20 characters")
    private String modelSize;

    @ApiModelProperty(value = "Model file path", example = "/models/detection/v1.0/model.onnx")
    @Size(max = 500, message = "Model file path length cannot exceed 500 characters")
    private String modelPath;

    @ApiModelProperty(value = "Model accuracy", example = "0.95")
    private BigDecimal accuracy;

    @ApiModelProperty(value = "Model description", example = "Object detection model based on YOLOv5")
    private String description;

    @ApiModelProperty(value = "Status", example = "draft")
    @Pattern(regexp = "^(draft|testing|published)$", message = "Status can only be draft, testing, or published")
    private String status;
} 