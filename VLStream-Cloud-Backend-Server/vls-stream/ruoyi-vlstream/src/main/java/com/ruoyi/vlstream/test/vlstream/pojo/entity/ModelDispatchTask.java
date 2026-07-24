/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.util.Date;

/**
 * Persistent state for one model deployment request sent to one device.
 */
@Data
@TableName("vls_model_dispatch_task")
@EqualsAndHashCode(callSuper = true)
public class ModelDispatchTask extends TenantEntity {

	private static final long serialVersionUID = 1L;

	private String requestId;
	private Long deviceRowId;
	private String deviceId;
	private Long algorithmId;
	private Long trainingId;
	private String modelType;
	@JsonIgnore
	private String remotePath;
	private String fileName;
	private Long fileSize;
	private String sha256;
	private String dispatchStatus;
	private String mqttTopic;
	private Long downloadExpiresAt;
	private Date publishedAt;
	private Date downloadStartedAt;
	private Date downloadCompletedAt;
	private Date deployedAt;
	private Date lastReplyAt;
	private String failureReason;
	private String replyPayload;
	private Integer retryCount;
}
