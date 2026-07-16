/*
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


/**
 * 算法标注数据表 Excel实体类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsAlgorithmAnnotationExcel implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 标注名称
	 */
	@ColumnWidth(20)
	@ExcelProperty("标注名称")
	private String annotationName;
	/**
	 * 标注类型：object_detection-物体检测,image_classification-图像分类,instance_segmentation-实例分割,semantic_segmentation-语义分割
	 */
	@ColumnWidth(20)
	@ExcelProperty("标注类型：object_detection-物体检测,image_classification-图像分类,instance_segmentation-实例分割,semantic_segmentation-语义分割")
	private String annotationType;
	/**
	 * 数据集路径
	 */
	@ColumnWidth(20)
	@ExcelProperty("数据集路径")
	private String datasetPath;
	/**
	 * 总数量
	 */
	@ColumnWidth(20)
	@ExcelProperty("总数量")
	private Integer totalCount;
	/**
	 * 已标注数量
	 */
	@ColumnWidth(20)
	@ExcelProperty("已标注数量")
	private Integer annotatedCount;
	/**
	 * 标注状态：none-未标注,partial-部分标注,completed-完成标注
	 */
	@ColumnWidth(20)
	@ExcelProperty("标注状态：none-未标注,partial-部分标注,completed-完成标注")
	private String annotationStatus;
	/**
	 * 标注进度百分比
	 */
	@ColumnWidth(20)
	@ExcelProperty("标注进度百分比")
	private Integer progress;
	/**
	 * 标注规则
	 */
	@ColumnWidth(20)
	@ExcelProperty("标注规则")
	private String annotationRules;
	/**
	 * 备注
	 */
	@ColumnWidth(20)
	@ExcelProperty("备注")
	private String remark;

}
