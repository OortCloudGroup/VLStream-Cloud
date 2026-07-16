package com.ruoyi.vlstream.test.vlstream.excel;


import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;


/**
 * 设备信息表 Excel实体类
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
	 * 设备名称
	 */
	@ColumnWidth(20)
	@ExcelProperty("设备名称")
	private String deviceName;
	/**
	 * 设备编号，唯一标识
	 */
	@ColumnWidth(20)
	@ExcelProperty("设备编号，唯一标识")
	private String deviceId;
	/**
	 * 视频流地址 (RTSP/HTTP等)
	 */
	@ColumnWidth(20)
	@ExcelProperty("视频流地址 (RTSP/HTTP等)")
	private String streamUrl;
	/**
	 * 设备图像路径
	 */
	@ColumnWidth(20)
	@ExcelProperty("设备图像路径")
	private String imagePath;
	/**
	 * 设备位置/安装地点
	 */
	@ColumnWidth(20)
	@ExcelProperty("设备位置/安装地点")
	private String position;
	/**
	 * 设备类型 (球机监控、云台、枪机等)
	 */
	@ColumnWidth(20)
	@ExcelProperty("设备类型 (球机监控、云台、枪机等)")
	private String deviceType;
	/**
	 * 设备品牌 (海康威视、大华、宇视等)
	 */
	@ColumnWidth(20)
	@ExcelProperty("设备品牌 (海康威视、大华、宇视等)")
	private String brand;
	/**
	 * 设备型号
	 */
	@ColumnWidth(20)
	@ExcelProperty("设备型号")
	private String model;
	/**
	 * IP地址 (支持IPv4和IPv6)
	 */
	@ColumnWidth(20)
	@ExcelProperty("IP地址 (支持IPv4和IPv6)")
	private String ipAddress;
	/**
	 * 端口号
	 */
	@ColumnWidth(20)
	@ExcelProperty("端口号")
	private Integer port;
	/**
	 * 登录用户名
	 */
	@ColumnWidth(20)
	@ExcelProperty("登录用户名")
	private String username;
	/**
	 * 登录密码
	 */
	@ColumnWidth(20)
	@ExcelProperty("登录密码")
	private String password;
	/**
	 * 设备描述信息
	 */
	@ColumnWidth(20)
	@ExcelProperty("设备描述信息")
	private String description;
	/**
	 * 备注信息
	 */
	@ColumnWidth(20)
	@ExcelProperty("备注信息")
	private String remark;
	/**
	 * 位置描述
	 */
	@ColumnWidth(20)
	@ExcelProperty("位置描述")
	private String location;
	/**
	 * 经度
	 */
	@ColumnWidth(20)
	@ExcelProperty("经度")
	private BigDecimal longitude;
	/**
	 * 纬度
	 */
	@ColumnWidth(20)
	@ExcelProperty("纬度")
	private BigDecimal latitude;
	/**
	 * 厂商
	 */
	@ColumnWidth(20)
	@ExcelProperty("厂商")
	private String manufacturer;
	/**
	 * 视频流路径
	 */
	@ColumnWidth(20)
	@ExcelProperty("视频流路径")
	private String streamPath;
	/**
	 * 高度位置(高空/地面/地下/其他)
	 */
	@ColumnWidth(20)
	@ExcelProperty("高度位置(高空/地面/地下/其他)")
	private String heightPosition;
	/**
	 * 详细地址
	 */
	@ColumnWidth(20)
	@ExcelProperty("详细地址")
	private String address;
	/**
	 * 区划选择(JSON格式)
	 */
	@ColumnWidth(20)
	@ExcelProperty("区划选择(JSON格式)")
	private String region;
	/**
	 * 创建人
	 */
	@ColumnWidth(20)
	@ExcelProperty("创建人")
	private String creator;
	/**
	 * RTSP地址
	 */
	@ColumnWidth(20)
	@ExcelProperty("RTSP地址")
	private String rtspUrl;
	/**
	 * 设备标签
	 */
	@ColumnWidth(20)
	@ExcelProperty("设备标签")
	private String tag;
	/**
	 * 算法id
	 */
	@ColumnWidth(20)
	@ExcelProperty("算法id")
	private String algorithmId;

}
