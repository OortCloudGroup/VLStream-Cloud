package org.springblade.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * Device Information Table Entity Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_device_info")
@Schema(description = "VlsDeviceInfoEntity object")
@EqualsAndHashCode(callSuper = true)
public class DeviceInfo extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Device Name
	 */
	@Schema(description = "Device Name")
	private String deviceName;
	/**
	 * Device number, unique identifier
	 */
	@Schema(description = "Device number, unique identifier")
	private String deviceId;
	/**
	 * Video Stream Address (RTSP/HTTP, etc.)
	 */
	@Schema(description = "Video Stream Address (RTSP/HTTP, etc.)")
	private String streamUrl;
	/**
	 * Device Image Path
	 */
	@Schema(description = "Device Image Path")
	private String imagePath;
	/**
	 * Device Type (Dome camera, PTZ, Box camera, etc.)
	 */
	@Schema(description = "Device Type (Dome camera, PTZ, Box camera, etc.)")
	private String deviceType;
	/**
	 * Remarks information
	 */
	@Schema(description = "Remarks information")
	private String remark;
	/**
	 * Longitude
	 */
	@Schema(description = "Longitude")
	private BigDecimal longitude;
	/**
	 * Latitude
	 */
	@Schema(description = "Latitude")
	private BigDecimal latitude;
	/**
	 * Height position (high altitude/ground/underground/other)
	 */
	@Schema(description = "Height position (high altitude/ground/underground/other)")
	private String heightPosition;
	/**
	 * Detail Address
	 */
	@Schema(description = "Detail Address")
	private String address;
	/**
	 * Division selection
	 */
	@Schema(description = "Division selection")
	private String region;
	/**
	 * Device Tag
	 */
	@Schema(description = "Device Tag")
	private String tag;
	/**
	 * Algorithm ID
	 */
	@Schema(description = "Algorithm ID")
	private String algorithmId;
	/**
	 * Push address
	 */
	@Schema(description = "Push address")
	private String pushUrl;
	/**
	 * Whether public: 0-No, 1-Yes
	 */
	@Schema(description = "Whether public: 0-No, 1-Yes")
	private Integer isPublic;
}
