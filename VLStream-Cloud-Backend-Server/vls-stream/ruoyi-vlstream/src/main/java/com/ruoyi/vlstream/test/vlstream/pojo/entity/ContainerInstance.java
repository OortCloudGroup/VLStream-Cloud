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
 * 容器实例表 实体类
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
	 * 实例名称
	 */
	@Schema(description = "实例名称")
	private String instanceName;
	/**
	 * 容器ID
	 */
	@Schema(description = "容器ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private String containerId;
	/**
	 * 镜像名称
	 */
	@Schema(description = "镜像名称")
	private String imageName;
	/**
	 * 镜像类型：base-基础镜像,app-应用镜像,custom-自定义镜像,url-镜像地址
	 */
	@Schema(description = "镜像类型：base-基础镜像,app-应用镜像,custom-自定义镜像,url-镜像地址")
	private String imageType;
	/**
	 * 镜像标签
	 */
	@Schema(description = "镜像标签")
	private String imageTag;
	/**
	 * 资源类型ID
	 */
	@Schema(description = "资源类型ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long resourceTypeId;
	/**
	 * 资源规格ID
	 */
	@Schema(description = "资源规格ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long resourceSpecId;
	/**
	 * 实例数量
	 */
	@Schema(description = "实例数量")
	private Integer instanceCount;
	/**
	 * 算法ID
	 */
	@Schema(description = "算法ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long algorithmId;
	/**
	 * 实例类型
	 */
	@Schema(description = "实例类型")
	private String instanceType;
	/**
	 * CPU限制
	 */
	@Schema(description = "CPU限制")
	private String cpuLimit;
	/**
	 * 内存限制
	 */
	@Schema(description = "内存限制")
	private String memoryLimit;
	/**
	 * GPU限制
	 */
	@Schema(description = "GPU限制")
	private String gpuLimit;
	/**
	 * 端口配置
	 */
	@Schema(description = "端口配置")
	private String portConfig;
	/**
	 * 环境变量配置
	 */
	@Schema(description = "环境变量配置")
	private String envConfig;
	/**
	 * 存储卷配置
	 */
	@Schema(description = "存储卷配置")
	private String volumeConfig;
	/**
	 * 实例状态：running-运行中,stopped-已停止,error-错误,starting-启动中,stopping-停止中
	 */
	@Schema(description = "实例状态：running-运行中,stopped-已停止,error-错误,starting-启动中,stopping-停止中")
	private String instanceStatus;
	/**
	 * 健康状态：healthy-健康,unhealthy-不健康,unknown-未知
	 */
	@Schema(description = "健康状态：healthy-健康,unhealthy-不健康,unknown-未知")
	private String healthStatus;
	/**
	 * 启动时间
	 */
	@Schema(description = "启动时间")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date startTime;
	/**
	 * 停止时间
	 */
	@Schema(description = "停止时间")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date stopTime;
	/**
	 * 重启次数
	 */
	@Schema(description = "重启次数")
	private Integer restartCount;
	/**
	 * CPU使用率
	 */
	@Schema(description = "CPU使用率")
	private BigDecimal cpuUsage;
	/**
	 * 内存使用率
	 */
	@Schema(description = "内存使用率")
	private BigDecimal memoryUsage;
	/**
	 * GPU使用率
	 */
	@Schema(description = "GPU使用率")
	private BigDecimal gpuUsage;
	/**
	 * 日志路径
	 */
	@Schema(description = "日志路径")
	private String logsPath;

}
