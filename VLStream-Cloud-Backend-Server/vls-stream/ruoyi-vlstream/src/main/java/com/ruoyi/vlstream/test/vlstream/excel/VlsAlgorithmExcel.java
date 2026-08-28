/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
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
 * algorithm Excel
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
	 * algorithm ID
	 */
	@ColumnWidth(20)
	@ExcelProperty("所属算法仓库ID")
	private Long repositoryId;
	/**
	 * algorithm
	 */
	@ColumnWidth(20)
	@ExcelProperty("算法名称")
	private String name;
	/**
	 * algorithm ( algorithm、instance algorithm、 algorithm、 algorithm、 algorithm etc.)
	 */
	@ColumnWidth(20)
	@ExcelProperty("算法分类（目标检测算法、实例分割算法、图像分类算法、关键点检测算法、旋转目标检测算法等）")
	private String category;
	/**
	 * algorithm
	 */
	@ColumnWidth(20)
	@ExcelProperty("算法描述")
	private String description;
	/**
	 * algorithm URL
	 */
	@ColumnWidth(20)
	@ExcelProperty("算法图片URL")
	private String imageUrl;
	/**
	 * algorithm
	 */
	@ColumnWidth(20)
	@ExcelProperty("算法版本")
	private String version;
	/**
	 * model
	 */
	@ColumnWidth(20)
	@ExcelProperty("模型格式")
	private String modelFormat;
	/**
	 * model
	 */
	@ColumnWidth(20)
	@ExcelProperty("模型文件路径")
	private String modelFilePath;
	/**
	 * algorithmconfigurationparameter (JSON )
	 */
	@ColumnWidth(20)
	@ExcelProperty("算法配置参数（JSON格式）")
	private String configParams;
	/**
	 * (image、video etc.)
	 */
	@ColumnWidth(20)
	@ExcelProperty("输入格式（image、video等）")
	private String inputFormat;
	/**
	 * (bbox、mask、keypoint etc.)
	 */
	@ColumnWidth(20)
	@ExcelProperty("输出格式（bbox、mask、keypoint等）")
	private String outputFormat;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("准确率")
	private BigDecimal accuracy;
	/**
	 * Process (FPS)
	 */
	@ColumnWidth(20)
	@ExcelProperty("处理速度（FPS）")
	private Integer processingSpeed;
	/**
	 * (MB)
	 */
	@ColumnWidth(20)
	@ExcelProperty("内存使用量（MB）")
	private Integer memoryUsage;
	/**
	 * whether need to GPU: 0- , 1- is
	 */
	@ColumnWidth(20)
	@ExcelProperty("是否需要GPU：0-否，1-是")
	private Byte gpuRequired;
	/**
	 * : ready- then , deploying- in , deployed- already , failed-failed
	 */
	@ColumnWidth(20)
	@ExcelProperty("部署状态：ready-就绪, deploying-部署中, deployed-已部署, failed-失败")
	private String deployStatus;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("部署次数")
	private Integer deployCount;
	/**
	 * after
	 */
	@ColumnWidth(20)
	@ExcelProperty("最后部署时间")
	private LocalDateTime lastDeployTime;

}
