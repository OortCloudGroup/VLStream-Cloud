package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 算法编排表 实体类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_algorithm_orchestration")
@Schema(description = "VlsAlgorithmOrchestrationEntity对象")
@EqualsAndHashCode(callSuper = true)
public class AlgorithmOrchestration extends TenantEntity {
	private static final long serialVersionUID = 1L;

	/**
	 * 编排名称
	 */
	@Schema(description = "编排名称")
	private String orchestrationName;
	/**
	 * 编排描述
	 */
	@Schema(description = "编排描述")
	private String orchestrationDesc;
	/**
	 * 触发类型：realtime-实时,scheduled-定时,manual-手动
	 */
	@Schema(description = "触发类型：realtime-实时,scheduled-定时,manual-手动")
	private String triggerType;
	/**
	 * 执行模式：serial-串行,parallel-并行
	 */
	@Schema(description = "执行模式：serial-串行,parallel-并行")
	private String executeMode;
	/**
	 * 算法步骤配置
	 */
	@Schema(description = "算法步骤配置")
	private String algorithmSteps;
	/**
	 * 输入配置
	 */
	@Schema(description = "输入配置")
	private String inputConfig;
	/**
	 * 输出配置
	 */
	@Schema(description = "输出配置")
	private String outputConfig;
	/**
	 * 关联设备数量
	 */
	@Schema(description = "关联设备数量")
	private Integer deviceCount;
	/**
	 * 运行次数
	 */
	@Schema(description = "运行次数")
	private Integer runCount;
	/**
	 * 状态：active-活跃,inactive-非活跃,draft-草稿
	 */
	@Schema(description = "状态：active-活跃,inactive-非活跃,draft-草稿")
	private String orchestrationStatus;
	/**
	 * 最后运行时间
	 */
	@Schema(description = "最后运行时间")
	private LocalDateTime lastRunTime;
	/**
	 * 平均运行时间(秒)
	 */
	@Schema(description = "平均运行时间(秒)")
	private Integer avgRunTime;
	/**
	 * 成功率
	 */
	@Schema(description = "成功率")
	private BigDecimal successRate;

}
