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
 * 算法编排表 Excel实体类
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
	 * 编排名称
	 */
	@ColumnWidth(20)
	@ExcelProperty("编排名称")
	private String orchestrationName;
	/**
	 * 编排描述
	 */
	@ColumnWidth(20)
	@ExcelProperty("编排描述")
	private String orchestrationDesc;
	/**
	 * 触发类型：realtime-实时,scheduled-定时,manual-手动
	 */
	@ColumnWidth(20)
	@ExcelProperty("触发类型：realtime-实时,scheduled-定时,manual-手动")
	private String triggerType;
	/**
	 * 执行模式：serial-串行,parallel-并行
	 */
	@ColumnWidth(20)
	@ExcelProperty("执行模式：serial-串行,parallel-并行")
	private String executeMode;
	/**
	 * 算法步骤配置
	 */
	@ColumnWidth(20)
	@ExcelProperty("算法步骤配置")
	private String algorithmSteps;
	/**
	 * 输入配置
	 */
	@ColumnWidth(20)
	@ExcelProperty("输入配置")
	private String inputConfig;
	/**
	 * 输出配置
	 */
	@ColumnWidth(20)
	@ExcelProperty("输出配置")
	private String outputConfig;
	/**
	 * 关联设备数量
	 */
	@ColumnWidth(20)
	@ExcelProperty("关联设备数量")
	private Integer deviceCount;
	/**
	 * 运行次数
	 */
	@ColumnWidth(20)
	@ExcelProperty("运行次数")
	private Integer runCount;
	/**
	 * 状态：active-活跃,inactive-非活跃,draft-草稿
	 */
	@ColumnWidth(20)
	@ExcelProperty("状态：active-活跃,inactive-非活跃,draft-草稿")
	private String orchestrationStatus;
	/**
	 * 最后运行时间
	 */
	@ColumnWidth(20)
	@ExcelProperty("最后运行时间")
	private LocalDateTime lastRunTime;
	/**
	 * 平均运行时间(秒)
	 */
	@ColumnWidth(20)
	@ExcelProperty("平均运行时间(秒)")
	private Integer avgRunTime;
	/**
	 * 成功率
	 */
	@ColumnWidth(20)
	@ExcelProperty("成功率")
	private BigDecimal successRate;

}
