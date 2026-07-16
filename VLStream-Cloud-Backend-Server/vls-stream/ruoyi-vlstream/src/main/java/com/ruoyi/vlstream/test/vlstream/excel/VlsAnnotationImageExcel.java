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
import java.time.LocalDateTime;


/**
 * 标注图片信息表 Excel实体类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsAnnotationImageExcel implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 标注项目ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("标注项目ID")
	private Long annotationId;
	/**
	 * 图片名称
	 */
	@ColumnWidth(20)
	@ExcelProperty("图片名称")
	private String imageName;
	/**
	 * 原始文件名
	 */
	@ColumnWidth(20)
	@ExcelProperty("原始文件名")
	private String originalName;
	/**
	 * 本地存储路径
	 */
	@ColumnWidth(20)
	@ExcelProperty("本地存储路径")
	private String localPath;
	/**
	 * 文件大小（字节）
	 */
	@ColumnWidth(20)
	@ExcelProperty("文件大小（字节）")
	private Long fileSize;
	/**
	 * 最后修改时间
	 */
	@ColumnWidth(20)
	@ExcelProperty("最后修改时间")
	private LocalDateTime lastModified;
	/**
	 * 是否为导入的图片：0-否，1-是
	 */
	@ColumnWidth(20)
	@ExcelProperty("是否为导入的图片：0-否，1-是")
	private Byte isImported;
	/**
	 * 导入时间
	 */
	@ColumnWidth(20)
	@ExcelProperty("导入时间")
	private LocalDateTime importTime;

}
