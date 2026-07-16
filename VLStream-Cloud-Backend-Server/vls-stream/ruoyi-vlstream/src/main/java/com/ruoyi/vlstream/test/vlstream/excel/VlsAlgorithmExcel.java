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
import java.time.LocalDateTime;


/**
 * 算法表 Excel实体类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsAlgorithmExcel implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 所属算法仓库ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("所属算法仓库ID")
	private Long repositoryId;
	/**
	 * 算法名称
	 */
	@ColumnWidth(20)
	@ExcelProperty("算法名称")
	private String name;
	/**
	 * 算法分类（目标检测算法、实例分割算法、图像分类算法、关键点检测算法、旋转目标检测算法等）
	 */
	@ColumnWidth(20)
	@ExcelProperty("算法分类（目标检测算法、实例分割算法、图像分类算法、关键点检测算法、旋转目标检测算法等）")
	private String category;
	/**
	 * 算法描述
	 */
	@ColumnWidth(20)
	@ExcelProperty("算法描述")
	private String description;
	/**
	 * 算法图片URL
	 */
	@ColumnWidth(20)
	@ExcelProperty("算法图片URL")
	private String imageUrl;
	/**
	 * 算法版本
	 */
	@ColumnWidth(20)
	@ExcelProperty("算法版本")
	private String version;
	/**
	 * 模型格式
	 */
	@ColumnWidth(20)
	@ExcelProperty("模型格式")
	private String modelFormat;
	/**
	 * 模型文件路径
	 */
	@ColumnWidth(20)
	@ExcelProperty("模型文件路径")
	private String modelFilePath;
	/**
	 * 算法配置参数（JSON格式）
	 */
	@ColumnWidth(20)
	@ExcelProperty("算法配置参数（JSON格式）")
	private String configParams;
	/**
	 * 输入格式（image、video等）
	 */
	@ColumnWidth(20)
	@ExcelProperty("输入格式（image、video等）")
	private String inputFormat;
	/**
	 * 输出格式（bbox、mask、keypoint等）
	 */
	@ColumnWidth(20)
	@ExcelProperty("输出格式（bbox、mask、keypoint等）")
	private String outputFormat;
	/**
	 * 准确率
	 */
	@ColumnWidth(20)
	@ExcelProperty("准确率")
	private BigDecimal accuracy;
	/**
	 * 处理速度（FPS）
	 */
	@ColumnWidth(20)
	@ExcelProperty("处理速度（FPS）")
	private Integer processingSpeed;
	/**
	 * 内存使用量（MB）
	 */
	@ColumnWidth(20)
	@ExcelProperty("内存使用量（MB）")
	private Integer memoryUsage;
	/**
	 * 是否需要GPU：0-否，1-是
	 */
	@ColumnWidth(20)
	@ExcelProperty("是否需要GPU：0-否，1-是")
	private Byte gpuRequired;
	/**
	 * 部署状态：ready-就绪, deploying-部署中, deployed-已部署, failed-失败
	 */
	@ColumnWidth(20)
	@ExcelProperty("部署状态：ready-就绪, deploying-部署中, deployed-已部署, failed-失败")
	private String deployStatus;
	/**
	 * 部署次数
	 */
	@ColumnWidth(20)
	@ExcelProperty("部署次数")
	private Integer deployCount;
	/**
	 * 最后部署时间
	 */
	@ColumnWidth(20)
	@ExcelProperty("最后部署时间")
	private LocalDateTime lastDeployTime;

}
