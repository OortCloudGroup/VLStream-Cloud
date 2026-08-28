/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;
import org.springblade.core.tool.utils.DateUtil;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

/**
 * instance
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_container_instance")
@Schema(description = "VlsContainerInstanceEntity对象")
@EqualsAndHashCode(callSuper = true)
public class ContainerInstance extends TenantEntity {
	private static final long serialVersionUID = 1L;

	/**
	 * instance
	 */
	@Schema(description = "实例名称")
	private String instanceName;
	/**
	 * ID
	 */
	@Schema(description = "容器ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private String containerId;
	/**
	 *
	 */
	@Schema(description = "镜像名称")
	private String imageName;
	/**
	 * : base- ,app- ,custom-Custom ,url-
	 */
	@Schema(description = "镜像类型：base-基础镜像,app-应用镜像,custom-自定义镜像,url-镜像地址")
	private String imageType;
	/**
	 *
	 */
	@Schema(description = "镜像标签")
	private String imageTag;
	/**
	 * ID
	 */
	@Schema(description = "资源类型ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long resourceTypeId;
	/**
	 * ID
	 */
	@Schema(description = "资源规格ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long resourceSpecId;
	/**
	 * instance
	 */
	@Schema(description = "实例数量")
	private Integer instanceCount;
	/**
	 * algorithmID
	 */
	@Schema(description = "算法ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long algorithmId;
	/**
	 * instance
	 */
	@Schema(description = "实例类型")
	private String instanceType;
	/**
	 * CPU
	 */
	@Schema(description = "CPU限制")
	private String cpuLimit;
	/**
	 *
	 */
	@Schema(description = "内存限制")
	private String memoryLimit;
	/**
	 * GPU
	 */
	@Schema(description = "GPU限制")
	private String gpuLimit;
	/**
	 * configuration
	 */
	@Schema(description = "端口配置")
	private String portConfig;
	/**
	 * variableconfiguration
	 */
	@Schema(description = "环境变量配置")
	private String envConfig;
	/**
	 * configuration
	 */
	@Schema(description = "存储卷配置")
	private String volumeConfig;
	/**
	 * instance : running- in ,stopped- already ,error- ,starting- in ,stopping- in
	 */
	@Schema(description = "实例状态：running-运行中,stopped-已停止,error-错误,starting-启动中,stopping-停止中")
	private String instanceStatus;
	/**
	 * : healthy- ,unhealthy- ,unknown- not
	 */
	@Schema(description = "健康状态：healthy-健康,unhealthy-不健康,unknown-未知")
	private String healthStatus;
	/**
	 *
	 */
	@Schema(description = "启动时间")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date startTime;
	/**
	 *
	 */
	@Schema(description = "停止时间")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date stopTime;
	/**
	 *
	 */
	@Schema(description = "重启次数")
	private Integer restartCount;
	/**
	 * CPU
	 */
	@Schema(description = "CPU使用率")
	private BigDecimal cpuUsage;
	/**
	 *
	 */
	@Schema(description = "内存使用率")
	private BigDecimal memoryUsage;
	/**
	 * GPU
	 */
	@Schema(description = "GPU使用率")
	private BigDecimal gpuUsage;
	/**
	 * log
	 */
	@Schema(description = "日志路径")
	private String logsPath;
	/**
	 * algorithmtrainingtaskID
	 */
	@Schema(description = "关联的算法训练任务ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long trainingTaskId;
	/**
	 * GPUservice and info
	 */
	@Schema(description = "GPU服务器ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long serverId;
	@Schema(description = "GPU服务器地址")
	private String serverIp;
	@Schema(description = "GPU序号")
	private Integer gpuIndex;
	@Schema(description = "GPU UUID")
	private String gpuUuid;
	@Schema(description = "排队时间")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date queueTime;
	@Schema(description = "运行错误信息")
	private String errorMessage;

}
