package com.vlstream.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 容器实例创建DTO
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "ContainerInstanceCreateDTO", description = "容器实例创建参数")
public class ContainerInstanceCreateDTO {

    @ApiModelProperty(value = "实例名称", required = true)
    @NotBlank(message = "实例名称不能为空")
    private String instanceName;

    @ApiModelProperty(value = "镜像名称", required = true)
    @NotBlank(message = "镜像名称不能为空")
    private String imageName;

    @ApiModelProperty(value = "镜像标签")
    private String imageTag = "latest";

    @ApiModelProperty(value = "算法ID")
    private Long algorithmId;

    @ApiModelProperty(value = "实例类型", required = true)
    @NotBlank(message = "实例类型不能为空")
    private String instanceType;

    @ApiModelProperty(value = "CPU限制")
    private String cpuLimit;

    @ApiModelProperty(value = "内存限制")
    private String memoryLimit;

    @ApiModelProperty(value = "GPU限制")
    private String gpuLimit;

    @ApiModelProperty(value = "端口配置（JSON格式）")
    private String portConfig;

    @ApiModelProperty(value = "环境变量配置（JSON格式）")
    private String envConfig;

    @ApiModelProperty(value = "存储卷配置（JSON格式）")
    private String volumeConfig;

    @ApiModelProperty(value = "日志路径")
    private String logsPath;
} 