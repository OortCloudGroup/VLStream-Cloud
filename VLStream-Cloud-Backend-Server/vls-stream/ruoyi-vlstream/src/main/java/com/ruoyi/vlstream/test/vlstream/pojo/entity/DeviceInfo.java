package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.math.BigDecimal;

/**
 * 设备信息表 实体类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_device_info")
@Schema(description = "VlsDeviceInfoEntity对象")
@EqualsAndHashCode(callSuper = true)
public class DeviceInfo extends TenantEntity {
	private static final long serialVersionUID = 1L;

	/**
	 * 设备名称
	 */
	@Schema(description = "设备名称")
	private String deviceName;
	/**
	 * 设备编号，唯一标识
	 */
	@Schema(description = "设备编号，唯一标识")
	private String deviceId;
	/**
	 * 视频流地址 (RTSP/HTTP等)
	 */
	@Schema(description = "视频流地址 (RTSP/HTTP等)")
	private String streamUrl;
	/**
	 * 设备图像路径
	 */
	@Schema(description = "设备图像路径")
	private String imagePath;
	/**
	 * 设备类型 (球机监控、云台、枪机等)
	 */
	@Schema(description = "设备类型 (球机监控、云台、枪机等)")
	private String deviceType;
	/**
	 * 备注信息
	 */
	@Schema(description = "备注信息")
	private String remark;
	/**
	 * 经度
	 */
	@Schema(description = "经度")
	private BigDecimal longitude;
	/**
	 * 纬度
	 */
	@Schema(description = "纬度")
	private BigDecimal latitude;
	/**
	 * 高度位置(高空/地面/地下/其他)
	 */
	@Schema(description = "高度位置(高空/地面/地下/其他)")
	private String heightPosition;
	/**
	 * 详细地址
	 */
	@Schema(description = "详细地址")
	private String address;
	/**
	 * 区划选择
	 */
	@Schema(description = "区划选择")
	private String region;
	/**
	 * 设备标签
	 */
	@Schema(description = "设备标签")
	private String tag;
	/**
	 * 算法id
	 */
	@Schema(description = "算法id")
	private String algorithmId;
	/**
	 * 推送地址
	 */
	@Schema(description = "推送地址")
	private String pushUrl;
	/**
	 * 是否公开：0-否，1-是
	 */
	@Schema(description = "是否公开：0-否，1-是")
	private Integer isPublic;
}
