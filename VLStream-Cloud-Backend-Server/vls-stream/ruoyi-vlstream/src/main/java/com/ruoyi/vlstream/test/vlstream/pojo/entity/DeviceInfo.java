/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.math.BigDecimal;

/**
 * deviceinfo
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
	 * device
	 */
	@Schema(description = "设备名称")
	private String deviceName;
	/**
	 * device ,
	 */
	@Schema(description = "设备编号，唯一标识")
	private String deviceId;
	/**
	 * (RTSP/HTTP etc.)
	 */
	@Schema(description = "视频流地址 (RTSP/HTTP等)")
	private String streamUrl;
	/**
	 * device
	 */
	@Schema(description = "设备图像路径")
	private String imagePath;
	/**
	 * device ( 、 、 etc.)
	 */
	@Schema(description = "设备类型 (球机监控、云台、枪机等)")
	private String deviceType;
	/**
	 * remarkinfo
	 */
	@Schema(description = "备注信息")
	private String remark;
	/**
	 *
	 */
	@Schema(description = "经度")
	private BigDecimal longitude;
	/**
	 *
	 */
	@Schema(description = "纬度")
	private BigDecimal latitude;
	/**
	 * ( null / empty / / / )
	 */
	@Schema(description = "高度位置(高空/地面/地下/其他)")
	private String heightPosition;
	/**
	 *
	 */
	@Schema(description = "详细地址")
	private String address;
	/**
	 *
	 */
	@Schema(description = "区划选择")
	private String region;
	/**
	 * device
	 */
	@Schema(description = "设备标签")
	private String tag;
	/**
	 * algorithmid
	 */
	@Schema(description = "算法id")
	private String algorithmId;
	/**
	 * Push
	 */
	@Schema(description = "推送地址")
	private String pushUrl;
	/**
	 * whether : 0- , 1- is
	 */
	@Schema(description = "是否公开：0-否，1-是")
	private Integer isPublic;
}
