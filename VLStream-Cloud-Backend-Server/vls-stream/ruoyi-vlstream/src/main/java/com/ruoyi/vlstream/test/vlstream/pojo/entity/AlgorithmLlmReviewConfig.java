/*
 * SPDX-License-Identifier: MIT
 */
package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.math.BigDecimal;

/** Optional LLM review settings owned by one algorithm. */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vls_algorithm_llm_review_config")
public class AlgorithmLlmReviewConfig extends TenantEntity {

	private Long algorithmId;
	private Long providerId;
	private Boolean enabled;
	private String promptTemplate;
	private BigDecimal decisionThreshold;
	private Integer maxRetries;
	private String failureStrategy;
	private String imageMode;
}
