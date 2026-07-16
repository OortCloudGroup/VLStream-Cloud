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
 * 智能分析请求表 Excel实体类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsAnalysisRequestExcel implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * 分析名称
	 */
	@ColumnWidth(20)
	@ExcelProperty("分析名称")
	private String analysisName;
	/**
	 * 分析类型
	 */
	@ColumnWidth(20)
	@ExcelProperty("分析类型")
	private String analysisType;
	/**
	 * 场景名称
	 */
	@ColumnWidth(20)
	@ExcelProperty("场景名称")
	private String sceneName;
	/**
	 * 设备ID列表，逗号分隔
	 */
	@ColumnWidth(20)
	@ExcelProperty("设备ID列表，逗号分隔")
	private String deviceIds;
	/**
	 * 设备信息
	 */
	@ColumnWidth(20)
	@ExcelProperty("设备信息")
	private String deviceInfo;
	/**
	 * 算法ID列表，逗号分隔
	 */
	@ColumnWidth(20)
	@ExcelProperty("算法ID列表，逗号分隔")
	private String algorithmIds;
	/**
	 * 区域信息
	 */
	@ColumnWidth(20)
	@ExcelProperty("区域信息")
	private String regionInfo;
	/**
	 * 时间范围
	 */
	@ColumnWidth(20)
	@ExcelProperty("时间范围")
	private String timeRange;
	/**
	 * 截图信息
	 */
	@ColumnWidth(20)
	@ExcelProperty("截图信息")
	private String screenshots;
	/**
	 * 请求状态：pending-待处理,processing-处理中,completed-已完成,failed-处理失败
	 */
	@ColumnWidth(20)
	@ExcelProperty("请求状态：pending-待处理,processing-处理中,completed-已完成,failed-处理失败")
	private String requestStatus;
	/**
	 * 处理进度百分比
	 */
	@ColumnWidth(20)
	@ExcelProperty("处理进度百分比")
	private Integer progress;
	/**
	 * 结果文件路径
	 */
	@ColumnWidth(20)
	@ExcelProperty("结果文件路径")
	private String resultPath;
	/**
	 * 开始处理时间
	 */
	@ColumnWidth(20)
	@ExcelProperty("开始处理时间")
	private LocalDateTime startTime;
	/**
	 * 完成时间
	 */
	@ColumnWidth(20)
	@ExcelProperty("完成时间")
	private LocalDateTime completeTime;
	/**
	 * 错误信息
	 */
	@ColumnWidth(20)
	@ExcelProperty("错误信息")
	private String errorMessage;
	/**
	 * 描述信息
	 */
	@ColumnWidth(20)
	@ExcelProperty("描述信息")
	private String description;

}
