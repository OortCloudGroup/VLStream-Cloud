package com.ruoyi.vlstream.test.vlstream.excel;


import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;


/**
 * 算法模型表 Excel实体类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsAlgorithmModelExcel implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 模型名称
	 */
	@ColumnWidth(20)
	@ExcelProperty("模型名称")
	private String modelName;
	/**
	 * 算法ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("算法ID")
	private Long algorithmId;
	/**
	 * 训练任务ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("训练任务ID")
	private Long trainingId;
	/**
	 * 模型版本
	 */
	@ColumnWidth(20)
	@ExcelProperty("模型版本")
	private Integer version;
	/**
	 * 模型格式：ONNX,PyTorch,TensorFlow
	 */
	@ColumnWidth(20)
	@ExcelProperty("模型格式：ONNX,PyTorch,TensorFlow")
	private String modelFormat;
	/**
	 * 模型大小
	 */
	@ColumnWidth(20)
	@ExcelProperty("模型大小")
	private String modelSize;
	/**
	 * 模型文件路径
	 */
	@ColumnWidth(20)
	@ExcelProperty("模型文件路径")
	private String modelPath;
	/**
	 * 模型准确率
	 */
	@ColumnWidth(20)
	@ExcelProperty("模型准确率")
	private BigDecimal accuracy;
	/**
	 * 模型描述
	 */
	@ColumnWidth(20)
	@ExcelProperty("模型描述")
	private String description;
	/**
	 * 下载次数
	 */
	@ColumnWidth(20)
	@ExcelProperty("下载次数")
	private Integer downloadCount;
	/**
	 * 部署次数
	 */
	@ColumnWidth(20)
	@ExcelProperty("部署次数")
	private Integer deployCount;
	/**
	 * 发布时间
	 */
	@ColumnWidth(20)
	@ExcelProperty("发布时间")
	private LocalDateTime publishTime;

}
