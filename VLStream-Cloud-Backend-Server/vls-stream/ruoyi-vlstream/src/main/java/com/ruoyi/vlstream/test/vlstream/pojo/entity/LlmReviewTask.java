/*
 * SPDX-License-Identifier: MIT
 */
package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/** Durable review task created from one validated MQTT struct event. */
@Data
@TableName("vls_llm_review_task")
public class LlmReviewTask {

	@TableId(type = IdType.ASSIGN_ID)
	private Long id;
	private String tenantId;
	private String sourceMessageId;
	private String deviceEventId;
	private String deviceId;
	private Long algorithmId;
	private Long providerId;
	private String mediaId;
	private String eventPayloadJson;
	private String configSnapshotJson;
	private String reviewStatus;
	private Integer attemptCount;
	private Date nextRetryTime;
	private String lockedBy;
	private Date lockedAt;
	private String decision;
	private BigDecimal confidence;
	private String reason;
	private String rawResponse;
	private String lastError;
	private String formalEventId;
	private String reviewedBy;
	private Date reviewedAt;
	private Date createTime;
	private Date updateTime;
}
