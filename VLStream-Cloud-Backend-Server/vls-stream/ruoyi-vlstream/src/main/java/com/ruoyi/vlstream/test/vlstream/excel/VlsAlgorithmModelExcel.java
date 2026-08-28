/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

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
 * algorithmmodel Excel
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
	 * model
	 */
	@ColumnWidth(20)
	@ExcelProperty("模型名称")
	private String modelName;
	/**
	 * algorithmID
	 */
	@ColumnWidth(20)
	@ExcelProperty("算法ID")
	private Long algorithmId;
	/**
	 * trainingtaskID
	 */
	@ColumnWidth(20)
	@ExcelProperty("训练任务ID")
	private Long trainingId;
	/**
	 * model
	 */
	@ColumnWidth(20)
	@ExcelProperty("模型版本")
	private Integer version;
	/**
	 * model : ONNX,PyTorch,TensorFlow
	 */
	@ColumnWidth(20)
	@ExcelProperty("模型格式：ONNX,PyTorch,TensorFlow")
	private String modelFormat;
	/**
	 * model
	 */
	@ColumnWidth(20)
	@ExcelProperty("模型大小")
	private String modelSize;
	/**
	 * model
	 */
	@ColumnWidth(20)
	@ExcelProperty("模型文件路径")
	private String modelPath;
	/**
	 * model
	 */
	@ColumnWidth(20)
	@ExcelProperty("模型准确率")
	private BigDecimal accuracy;
	/**
	 * model
	 */
	@ColumnWidth(20)
	@ExcelProperty("模型描述")
	private String description;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("下载次数")
	private Integer downloadCount;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("部署次数")
	private Integer deployCount;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("发布时间")
	private LocalDateTime publishTime;

}
