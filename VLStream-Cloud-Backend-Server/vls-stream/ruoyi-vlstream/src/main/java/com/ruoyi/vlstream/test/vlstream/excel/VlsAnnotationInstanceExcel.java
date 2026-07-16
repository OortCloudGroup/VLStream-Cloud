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
import java.math.BigDecimal;


/**
 * 标注实例实体类 Excel实体类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsAnnotationInstanceExcel implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 关联的标注项目ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("关联的标注项目ID")
	private Long annotationId;
	/**
	 * 标签ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("标签ID")
	private Long labelId;
	/**
	 * 图片id
	 */
	@ColumnWidth(20)
	@ExcelProperty("图片id")
	private Long imageId;
	/**
	 * 标注类型：rect-矩形,circle-圆形,polygon-多边形
	 */
	@ColumnWidth(20)
	@ExcelProperty("标注类型：rect-矩形,circle-圆形,polygon-多边形")
	private String annotationType;
	/**
	 * 标注坐标数据(JSON格式)
	 */
	@ColumnWidth(20)
	@ExcelProperty("标注坐标数据(JSON格式)")
	private String annotationData;
	/**
	 * 置信度
	 */
	@ColumnWidth(20)
	@ExcelProperty("置信度")
	private BigDecimal confidence;
	/**
	 * 是否已验证
	 */
	@ColumnWidth(20)
	@ExcelProperty("是否已验证")
	private Byte verified;

}
