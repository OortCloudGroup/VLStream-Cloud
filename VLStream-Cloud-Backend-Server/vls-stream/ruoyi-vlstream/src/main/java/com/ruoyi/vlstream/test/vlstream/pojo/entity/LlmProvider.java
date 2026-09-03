/*
 * SPDX-License-Identifier: MIT
 */
package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

/** Tenant-scoped OpenAI-compatible vision provider. */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("vls_llm_provider")
public class LlmProvider extends TenantEntity {

	private String name;
	private String baseUrl;
	private String modelName;
	@JsonIgnore
	private String apiKeyCiphertext;
	private Integer timeoutSeconds;
	private Boolean enabled;

	@TableField(exist = false)
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String apiKey;

	@TableField(exist = false)
	private Boolean apiKeyConfigured;
}
