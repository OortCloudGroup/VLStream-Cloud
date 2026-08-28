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
 * algorithm Excel
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsAlgorithmOrchestrationExcel implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("编排名称")
	private String orchestrationName;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("编排描述")
	private String orchestrationDesc;
	/**
	 * : realtime- ,scheduled- ,manual-
	 */
	@ColumnWidth(20)
	@ExcelProperty("触发类型：realtime-实时,scheduled-定时,manual-手动")
	private String triggerType;
	/**
	 * Execute : serial- ,parallel-
	 */
	@ColumnWidth(20)
	@ExcelProperty("执行模式：serial-串行,parallel-并行")
	private String executeMode;
	/**
	 * algorithm configuration
	 */
	@ColumnWidth(20)
	@ExcelProperty("算法步骤配置")
	private String algorithmSteps;
	/**
	 * configuration
	 */
	@ColumnWidth(20)
	@ExcelProperty("输入配置")
	private String inputConfig;
	/**
	 * configuration
	 */
	@ColumnWidth(20)
	@ExcelProperty("输出配置")
	private String outputConfig;
	/**
	 * device
	 */
	@ColumnWidth(20)
	@ExcelProperty("关联设备数量")
	private Integer deviceCount;
	/**
	 *
	 */
	@ColumnWidth(20)
	@ExcelProperty("运行次数")
	private Integer runCount;
	/**
	 * : active- ,inactive- non- ,draft-
	 */
	@ColumnWidth(20)
	@ExcelProperty("状态：active-活跃,inactive-非活跃,draft-草稿")
	private String orchestrationStatus;
	/**
	 * after
	 */
	@ColumnWidth(20)
	@ExcelProperty("最后运行时间")
	private LocalDateTime lastRunTime;
	/**
	 * ( )
	 */
	@ColumnWidth(20)
	@ExcelProperty("平均运行时间(秒)")
	private Integer avgRunTime;
	/**
	 * successfully
	 */
	@ColumnWidth(20)
	@ExcelProperty("成功率")
	private BigDecimal successRate;

}
