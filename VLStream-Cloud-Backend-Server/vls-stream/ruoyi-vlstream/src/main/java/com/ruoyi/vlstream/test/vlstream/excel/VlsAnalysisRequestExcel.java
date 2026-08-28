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
import java.time.LocalDateTime;


/**
 * can Excel
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
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("分析名称")
	private String analysisName;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("分析类型")
	private String analysisType;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("场景名称")
	private String sceneName;
	/**
	 * deviceID ,
	 */
	@ColumnWidth(20)
	@ExcelProperty("设备ID列表，逗号分隔")
	private String deviceIds;
	/**
	 * deviceinfo
	 */
	@ColumnWidth(20)
	@ExcelProperty("设备信息")
	private String deviceInfo;
	/**
	 * algorithmID ,
	 */
	@ColumnWidth(20)
	@ExcelProperty("算法ID列表，逗号分隔")
	private String algorithmIds;
	/**
	 * info
	 */
	@ColumnWidth(20)
	@ExcelProperty("区域信息")
	private String regionInfo;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("时间范围")
	private String timeRange;
	/**
	 * snapshotinfo
	 */
	@ColumnWidth(20)
	@ExcelProperty("截图信息")
	private String screenshots;
	/**
	 * : pending- Process ,processing-Process in ,completed- already ,failed-Process failed
	 */
	@ColumnWidth(20)
	@ExcelProperty("请求状态：pending-待处理,processing-处理中,completed-已完成,failed-处理失败")
	private String requestStatus;
	/**
	 * Process
	 */
	@ColumnWidth(20)
	@ExcelProperty("处理进度百分比")
	private Integer progress;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("结果文件路径")
	private String resultPath;
	/**
	 * startProcess
	 */
	@ColumnWidth(20)
	@ExcelProperty("开始处理时间")
	private LocalDateTime startTime;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("完成时间")
	private LocalDateTime completeTime;
	/**
	 * info
	 */
	@ColumnWidth(20)
	@ExcelProperty("错误信息")
	private String errorMessage;
	/**
	 * info
	 */
	@ColumnWidth(20)
	@ExcelProperty("描述信息")
	private String description;

}
