/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * algorithm
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
	 *
	 */
	@Schema(description = "编排名称")
	private String orchestrationName;
	/**
	 *
	 */
	@Schema(description = "编排描述")
	private String orchestrationDesc;
	/**
	 * : realtime- ,scheduled- ,manual-
	 */
	@Schema(description = "触发类型：realtime-实时,scheduled-定时,manual-手动")
	private String triggerType;
	/**
	 * Execute : serial- ,parallel-
	 */
	@Schema(description = "执行模式：serial-串行,parallel-并行")
	private String executeMode;
	/**
	 * algorithm configuration
	 */
	@Schema(description = "算法步骤配置")
	private String algorithmSteps;
	/**
	 * configuration
	 */
	@Schema(description = "输入配置")
	private String inputConfig;
	/**
	 * configuration
	 */
	@Schema(description = "输出配置")
	private String outputConfig;
	/**
	 * device
	 */
	@Schema(description = "关联设备数量")
	private Integer deviceCount;
	/**
	 *
	 */
	@Schema(description = "运行次数")
	private Integer runCount;
	/**
	 * : active- ,inactive- non- ,draft-
	 */
	@Schema(description = "状态：active-活跃,inactive-非活跃,draft-草稿")
	private String orchestrationStatus;
	/**
	 * after
	 */
	@Schema(description = "最后运行时间")
	private LocalDateTime lastRunTime;
	/**
	 * ( )
	 */
	@Schema(description = "平均运行时间(秒)")
	private Integer avgRunTime;
	/**
	 * successfully
	 */
	@Schema(description = "成功率")
	private BigDecimal successRate;

}
