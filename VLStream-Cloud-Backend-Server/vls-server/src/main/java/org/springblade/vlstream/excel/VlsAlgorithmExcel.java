package org.springblade.vlstream.excel;


import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * Algorithm table Excel entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsAlgorithmExcel implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Algorithm repository ID to which it belongs
	 */
	@ColumnWidth(20)
	@ExcelProperty("Algorithm repository ID to which it belongs")
	private Long repositoryId;
	/**
	 * Algorithm name
	 */
	@ColumnWidth(20)
	@ExcelProperty("Algorithm name")
	private String name;
	/**
	 * Algorithm category (object detection, instance segmentation, image classification, keypoint detection, rotated object detection, etc.)
	 */
	@ColumnWidth(20)
	@ExcelProperty("Algorithm category (object detection, instance segmentation, image classification, keypoint detection, rotated object detection, etc.)")
	private String category;
	/**
	 * Algorithm description
	 */
	@ColumnWidth(20)
	@ExcelProperty("Algorithm description")
	private String description;
	/**
	 * Algorithm image URL
	 */
	@ColumnWidth(20)
	@ExcelProperty("Algorithm image URL")
	private String imageUrl;
	/**
	 * Algorithm version
	 */
	@ColumnWidth(20)
	@ExcelProperty("Algorithm version")
	private String version;
	/**
	 * Model format
	 */
	@ColumnWidth(20)
	@ExcelProperty("Model format")
	private String modelFormat;
	/**
	 * Model file path
	 */
	@ColumnWidth(20)
	@ExcelProperty("Model file path")
	private String modelFilePath;
	/**
	 * Algorithm configuration parameters (JSON format)
	 */
	@ColumnWidth(20)
	@ExcelProperty("Algorithm configuration parameters (JSON format)")
	private String configParams;
	/**
	 * Input format (image, video, etc.)
	 */
	@ColumnWidth(20)
	@ExcelProperty("Input format (image, video, etc.)")
	private String inputFormat;
	/**
	 * Output format (bbox, mask, keypoint, etc.)
	 */
	@ColumnWidth(20)
	@ExcelProperty("Output format (bbox, mask, keypoint, etc.)")
	private String outputFormat;
	/**
	 * Accuracy
	 */
	@ColumnWidth(20)
	@ExcelProperty("Accuracy")
	private BigDecimal accuracy;
	/**
	 * Processing speed (FPS)
	 */
	@ColumnWidth(20)
	@ExcelProperty("Processing speed (FPS)")
	private Integer processingSpeed;
	/**
	 * Memory usage (MB)
	 */
	@ColumnWidth(20)
	@ExcelProperty("Memory usage (MB)")
	private Integer memoryUsage;
	/**
	 * Whether GPU is required: 0-No, 1-Yes
	 */
	@ColumnWidth(20)
	@ExcelProperty("Whether GPU is required: 0-No, 1-Yes")
	private Byte gpuRequired;
	/**
	 * Deployment status: ready-ready, deploying-deploying, deployed-deployed, failed-failed
	 */
	@ColumnWidth(20)
	@ExcelProperty("Deployment status: ready-ready, deploying-deploying, deployed-deployed, failed-failed")
	private String deployStatus;
	/**
	 * Deployment count
	 */
	@ColumnWidth(20)
	@ExcelProperty("Deployment count")
	private Integer deployCount;
	/**
	 * Last Deployment Time
	 */
	@ColumnWidth(20)
	@ExcelProperty("Last Deployment Time")
	private LocalDateTime lastDeployTime;

}
