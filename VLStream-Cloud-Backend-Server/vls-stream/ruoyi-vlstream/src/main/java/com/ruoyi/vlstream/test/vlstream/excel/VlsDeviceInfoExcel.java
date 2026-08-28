/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.excel;


import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;


/**
 * deviceinfo Excel
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsDeviceInfoExcel implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * device
	 */
	@ColumnWidth(20)
	@ExcelProperty("设备名称")
	private String deviceName;
	/**
	 * device ,
	 */
	@ColumnWidth(20)
	@ExcelProperty("设备编号，唯一标识")
	private String deviceId;
	/**
	 * (RTSP/HTTP etc.)
	 */
	@ColumnWidth(20)
	@ExcelProperty("视频流地址 (RTSP/HTTP等)")
	private String streamUrl;
	/**
	 * device
	 */
	@ColumnWidth(20)
	@ExcelProperty("设备图像路径")
	private String imagePath;
	/**
	 * device /
	 */
	@ColumnWidth(20)
	@ExcelProperty("设备位置/安装地点")
	private String position;
	/**
	 * device ( 、 、 etc.)
	 */
	@ColumnWidth(20)
	@ExcelProperty("设备类型 (球机监控、云台、枪机等)")
	private String deviceType;
	/**
	 * device ( 、 、 etc.)
	 */
	@ColumnWidth(20)
	@ExcelProperty("设备品牌 (海康威视、大华、宇视等)")
	private String brand;
	/**
	 * device
	 */
	@ColumnWidth(20)
	@ExcelProperty("设备型号")
	private String model;
	/**
	 * IP ( IPv4 and IPv6)
	 */
	@ColumnWidth(20)
	@ExcelProperty("IP地址 (支持IPv4和IPv6)")
	private String ipAddress;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("端口号")
	private Integer port;
	/**
	 * user
	 */
	@ColumnWidth(20)
	@ExcelProperty("登录用户名")
	private String username;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("登录密码")
	private String password;
	/**
	 * device info
	 */
	@ColumnWidth(20)
	@ExcelProperty("设备描述信息")
	private String description;
	/**
	 * remarkinfo
	 */
	@ColumnWidth(20)
	@ExcelProperty("备注信息")
	private String remark;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("位置描述")
	private String location;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("经度")
	private BigDecimal longitude;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("纬度")
	private BigDecimal latitude;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("厂商")
	private String manufacturer;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("视频流路径")
	private String streamPath;
	/**
	 * ( null / empty / / / )
	 */
	@ColumnWidth(20)
	@ExcelProperty("高度位置(高空/地面/地下/其他)")
	private String heightPosition;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("详细地址")
	private String address;
	/**
	 * (JSON )
	 */
	@ColumnWidth(20)
	@ExcelProperty("区划选择(JSON格式)")
	private String region;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("创建人")
	private String creator;
	/**
	 * RTSP
	 */
	@ColumnWidth(20)
	@ExcelProperty("RTSP地址")
	private String rtspUrl;
	/**
	 * device
	 */
	@ColumnWidth(20)
	@ExcelProperty("设备标签")
	private String tag;
	/**
	 * algorithmid
	 */
	@ColumnWidth(20)
	@ExcelProperty("算法id")
	private String algorithmId;

}
