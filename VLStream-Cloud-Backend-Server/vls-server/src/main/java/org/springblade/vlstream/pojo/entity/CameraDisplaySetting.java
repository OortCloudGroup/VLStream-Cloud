package org.springblade.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.io.Serial;

/**
 * Camera display settings table entity class
 */
@Data
@TableName("vls_camera_display_setting")
@Schema(description = "CameraDisplaySetting object")
@EqualsAndHashCode(callSuper = true)
public class CameraDisplaySetting extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Device Primary Key ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long deviceId;

	@Schema(description = "Scene")
	private String scene;

	@Schema(description = "Brightness")
	private Integer brightness;

	@Schema(description = "Contrast")
	private Integer contrast;

	@Schema(description = "Saturation")
	private Integer saturation;

	@Schema(description = "Sharpness")
	private Integer sharpness;

	@Schema(description = "Exposure Mode")
	private String exposureMode;

	@Schema(description = "Maximum Shutter Limit")
	private String maxShutterLimit;

	@Schema(description = "Minimum Shutter Limit")
	private String minShutterLimit;

	@Schema(description = "Gain limit")
	private Integer gainLimit;

	@Schema(description = "Low-light electronic shutter")
	private String lowLightElectronicShutter;

	@Schema(description = "Focus mode")
	private String focusMode;

	@Schema(description = "Minimum Focus Distance")
	private String minFocusDistance;

	@Schema(description = "Day-night switch")
	private String dayNightSwitch;

	@Schema(description = "Sensitivity")
	private Integer sensitivity;

	@Schema(description = "Fill light overexposure prevention")
	private String antiFillLightOverExposure;

	@Schema(description = "Infrared lamp mode")
	private String infraredLampMode;

	@Schema(description = "Brightness limit")
	private Integer brightnessLimit;

	@Schema(description = "Backlight compensation")
	private String backlightCompensation;

	@Schema(description = "Wide Dynamic Range (WDR)")
	private String wideDynamic;

	@Schema(description = "Highlight Suppression (HLC)")
	private String strongLightSuppression;

	@Schema(description = "White balance")
	private String whiteBalance;

	@Schema(description = "Digital noise reduction")
	private String digitalNoiseReduction;

	@Schema(description = "Noise reduction level")
	private Integer noiseReductionLevel;

	@Schema(description = "Defog mode")
	private String defogMode;

	@Schema(description = "Electronic Image Stabilization (EIS)")
	private String electronicStabilization;

	@Schema(description = "Image")
	private String mirrorMode;

	@Schema(description = "PAL(50HZ)")
	private String pal50hz;

	@Schema(description = "Lens initialization")
	private String lensInitialization;

	@Schema(description = "Zoom limit")
	private Integer zoomLimit;

	@Schema(description = "Remarks")
	private String remark;
}
