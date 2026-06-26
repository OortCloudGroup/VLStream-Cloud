package org.springblade.vlstream.pojo.entity;

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

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Container instance table entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_container_instance")
@Schema(description = "VlsContainerInstanceEntity object")
@EqualsAndHashCode(callSuper = true)
public class ContainerInstance extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Instance name
	 */
	@Schema(description = "Instance name")
	private String instanceName;
	/**
	 * Container ID
	 */
	@Schema(description = "Container ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private String containerId;
	/**
	 * Image name
	 */
	@Schema(description = "Image name")
	private String imageName;
	/**
	 * Image type: base-base image, app-app image, custom-custom image, url-image URL
	 */
	@Schema(description = "Image type: base-base image, app-app image, custom-custom image, url-image URL")
	private String imageType;
	/**
	 * Image tag
	 */
	@Schema(description = "Image tag")
	private String imageTag;
	/**
	 * Resource Type ID
	 */
	@Schema(description = "Resource Type ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long resourceTypeId;
	/**
	 * Resource Specification ID
	 */
	@Schema(description = "Resource Specification ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long resourceSpecId;
	/**
	 * Instance quantity
	 */
	@Schema(description = "Instance quantity")
	private Integer instanceCount;
	/**
	 * Algorithm ID
	 */
	@Schema(description = "Algorithm ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long algorithmId;
	/**
	 * Instance type
	 */
	@Schema(description = "Instance type")
	private String instanceType;
	/**
	 * CPU limit
	 */
	@Schema(description = "CPU limit")
	private String cpuLimit;
	/**
	 * Memory limit
	 */
	@Schema(description = "Memory limit")
	private String memoryLimit;
	/**
	 * GPU limit
	 */
	@Schema(description = "GPU limit")
	private String gpuLimit;
	/**
	 * Port configuration
	 */
	@Schema(description = "Port configuration")
	private String portConfig;
	/**
	 * Environment variable configuration
	 */
	@Schema(description = "Environment variable configuration")
	private String envConfig;
	/**
	 * Storage volume configuration
	 */
	@Schema(description = "Storage volume configuration")
	private String volumeConfig;
	/**
	 * Instance status: running-Running, stopped-Stopped, error-Error, starting-Starting, stopping-Stopping
	 */
	@Schema(description = "Instance status: running-Running, stopped-Stopped, error-Error, starting-Starting, stopping-Stopping")
	private String instanceStatus;
	/**
	 * Health status: healthy-healthy, unhealthy-unhealthy, unknown-unknown
	 */
	@Schema(description = "Health status: healthy-healthy, unhealthy-unhealthy, unknown-unknown")
	private String healthStatus;
	/**
	 * Start time
	 */
	@Schema(description = "Start time")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date startTime;
	/**
	 * Stop time
	 */
	@Schema(description = "Stop time")
	@DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME)
	@JsonFormat(pattern = DateUtil.PATTERN_DATETIME)
	private Date stopTime;
	/**
	 * Restart count
	 */
	@Schema(description = "Restart count")
	private Integer restartCount;
	/**
	 * CPU usage
	 */
	@Schema(description = "CPU usage")
	private BigDecimal cpuUsage;
	/**
	 * Memory usage rate
	 */
	@Schema(description = "Memory usage rate")
	private BigDecimal memoryUsage;
	/**
	 * GPU usage
	 */
	@Schema(description = "GPU usage")
	private BigDecimal gpuUsage;
	/**
	 * Log path
	 */
	@Schema(description = "Log path")
	private String logsPath;

}
