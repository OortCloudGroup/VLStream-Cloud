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
 * Algorithm model table Excel entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsAlgorithmModelExcel implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Model name
	 */
	@ColumnWidth(20)
	@ExcelProperty("Model name")
	private String modelName;
	/**
	 * Algorithm ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("Algorithm ID")
	private Long algorithmId;
	/**
	 * Training Task ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("Training Task ID")
	private Long trainingId;
	/**
	 * Model version
	 */
	@ColumnWidth(20)
	@ExcelProperty("Model version")
	private Integer version;
	/**
	 * Model format: ONNX, PyTorch, TensorFlow
	 */
	@ColumnWidth(20)
	@ExcelProperty("Model format: ONNX, PyTorch, TensorFlow")
	private String modelFormat;
	/**
	 * Model size
	 */
	@ColumnWidth(20)
	@ExcelProperty("Model size")
	private String modelSize;
	/**
	 * Model file path
	 */
	@ColumnWidth(20)
	@ExcelProperty("Model file path")
	private String modelPath;
	/**
	 * Model accuracy
	 */
	@ColumnWidth(20)
	@ExcelProperty("Model accuracy")
	private BigDecimal accuracy;
	/**
	 * Model description
	 */
	@ColumnWidth(20)
	@ExcelProperty("Model description")
	private String description;
	/**
	 * Download count
	 */
	@ColumnWidth(20)
	@ExcelProperty("Download count")
	private Integer downloadCount;
	/**
	 * Deployment count
	 */
	@ColumnWidth(20)
	@ExcelProperty("Deployment count")
	private Integer deployCount;
	/**
	 * Publish time
	 */
	@ColumnWidth(20)
	@ExcelProperty("Publish time")
	private LocalDateTime publishTime;

}
