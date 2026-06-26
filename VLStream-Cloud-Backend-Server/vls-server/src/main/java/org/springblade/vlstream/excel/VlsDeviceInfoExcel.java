package org.springblade.vlstream.excel;


import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;


/**
 * Device Information Table Excel Entity Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsDeviceInfoExcel implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Device Name
	 */
	@ColumnWidth(20)
	@ExcelProperty("Device Name")
	private String deviceName;
	/**
	 * Device number, unique identifier
	 */
	@ColumnWidth(20)
	@ExcelProperty("Device number, unique identifier")
	private String deviceId;
	/**
	 * Video Stream Address (RTSP/HTTP, etc.)
	 */
	@ColumnWidth(20)
	@ExcelProperty("Video Stream Address (RTSP/HTTP, etc.)")
	private String streamUrl;
	/**
	 * Device Image Path
	 */
	@ColumnWidth(20)
	@ExcelProperty("Device Image Path")
	private String imagePath;
	/**
	 * Device location/installation site
	 */
	@ColumnWidth(20)
	@ExcelProperty("Device location/installation site")
	private String position;
	/**
	 * Device Type (Dome camera, PTZ, Box camera, etc.)
	 */
	@ColumnWidth(20)
	@ExcelProperty("Device Type (Dome camera, PTZ, Box camera, etc.)")
	private String deviceType;
	/**
	 * Device Brand (Hikvision, Dahua, Uniview, etc.)
	 */
	@ColumnWidth(20)
	@ExcelProperty("Device Brand (Hikvision, Dahua, Uniview, etc.)")
	private String brand;
	/**
	 * Device Model
	 */
	@ColumnWidth(20)
	@ExcelProperty("Device Model")
	private String model;
	/**
	 * IP address (supports IPv4 and IPv6)
	 */
	@ColumnWidth(20)
	@ExcelProperty("IP address (supports IPv4 and IPv6)")
	private String ipAddress;
	/**
	 * Port number
	 */
	@ColumnWidth(20)
	@ExcelProperty("Port number")
	private Integer port;
	/**
	 * Login username
	 */
	@ColumnWidth(20)
	@ExcelProperty("Login username")
	private String username;
	/**
	 * Login password
	 */
	@ColumnWidth(20)
	@ExcelProperty("Login password")
	private String password;
	/**
	 * Device Description
	 */
	@ColumnWidth(20)
	@ExcelProperty("Device Description")
	private String description;
	/**
	 * Remarks information
	 */
	@ColumnWidth(20)
	@ExcelProperty("Remarks information")
	private String remark;
	/**
	 * Location description
	 */
	@ColumnWidth(20)
	@ExcelProperty("Location description")
	private String location;
	/**
	 * Longitude
	 */
	@ColumnWidth(20)
	@ExcelProperty("Longitude")
	private BigDecimal longitude;
	/**
	 * Latitude
	 */
	@ColumnWidth(20)
	@ExcelProperty("Latitude")
	private BigDecimal latitude;
	/**
	 * Manufacturer
	 */
	@ColumnWidth(20)
	@ExcelProperty("Manufacturer")
	private String manufacturer;
	/**
	 * Video Stream Path
	 */
	@ColumnWidth(20)
	@ExcelProperty("Video Stream Path")
	private String streamPath;
	/**
	 * Height position (high altitude/ground/underground/other)
	 */
	@ColumnWidth(20)
	@ExcelProperty("Height position (high altitude/ground/underground/other)")
	private String heightPosition;
	/**
	 * Detail Address
	 */
	@ColumnWidth(20)
	@ExcelProperty("Detail Address")
	private String address;
	/**
	 * Division selection (JSON format)
	 */
	@ColumnWidth(20)
	@ExcelProperty("Division selection (JSON format)")
	private String region;
	/**
	 * Created by
	 */
	@ColumnWidth(20)
	@ExcelProperty("Created by")
	private String creator;
	/**
	 * RTSP address
	 */
	@ColumnWidth(20)
	@ExcelProperty("RTSP address")
	private String rtspUrl;
	/**
	 * Device Tag
	 */
	@ColumnWidth(20)
	@ExcelProperty("Device Tag")
	private String tag;
	/**
	 * Algorithm ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("Algorithm ID")
	private String algorithmId;

}
