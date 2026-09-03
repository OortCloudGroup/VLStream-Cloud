/*
 * SPDX-License-Identifier: MIT
 */
package com.ruoyi.vlstream.test.vlstream.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Runtime controls for optional algorithm-level vision review. */
@Data
@Component
@ConfigurationProperties(prefix = "vlstream.llm-review")
public class VlsLlmReviewProperties {

	/**
	 * Built-in key for local and out-of-the-box deployments. Production can override it
	 * with VLSTREAM_LLM_REVIEW_ENCRYPTION_KEY without exposing this setting to end users.
	 */
	public static final String DEFAULT_ENCRYPTION_KEY = "VLStream-Llm-Key-2026-Default-01";

	private Boolean enabled = Boolean.FALSE;
	private String encryptionKey = DEFAULT_ENCRYPTION_KEY;
	private Integer scanIntervalMillis = 3000;
	private Integer batchSize = 10;
	private Long staleLockMillis = 120000L;
	private Long firstRetryMillis = 10000L;
	private Long maxRetryMillis = 60000L;
	private Integer maxImageBytes = 10485760;
}
