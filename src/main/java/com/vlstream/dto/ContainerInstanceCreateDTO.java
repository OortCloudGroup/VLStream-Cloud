package com.vlstream.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Container Instance Creation DTO
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ContainerInstanceCreateDTO", description = "Container instance creation parameters")
public class ContainerInstanceCreateDTO {

    @ApiModelProperty(value = "Instance name", required = true)
    @NotBlank(message = "Instance name cannot be empty")
    private String instanceName;

    @ApiModelProperty(value = "Image name", required = true)
    @NotBlank(message = "Image name cannot be empty")
    private String imageName;

    @ApiModelProperty(value = "Image tag")
    private String imageTag = "latest";

    @ApiModelProperty(value = "Algorithm ID")
    private Long algorithmId;

    @ApiModelProperty(value = "Instance type", required = true)
    @NotBlank(message = "Instance type cannot be empty")
    private String instanceType;

    @ApiModelProperty(value = "CPU limit")
    private String cpuLimit;

    @ApiModelProperty(value = "Memory limit")
    private String memoryLimit;

    @ApiModelProperty(value = "GPU limit")
    private String gpuLimit;

    @ApiModelProperty(value = "Port configuration (JSON format)")
    private String portConfig;

    @ApiModelProperty(value = "Environment variable configuration (JSON format)")
    private String envConfig;

    @ApiModelProperty(value = "Volume configuration (JSON format)")
    private String volumeConfig;

    @ApiModelProperty(value = "Logs path")
    private String logsPath;
} 