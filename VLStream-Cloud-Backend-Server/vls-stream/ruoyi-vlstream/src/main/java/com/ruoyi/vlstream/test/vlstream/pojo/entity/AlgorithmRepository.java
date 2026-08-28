/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmRepositoryTypeEnum;
import com.ruoyi.vlstream.test.vlstream.deserialize.AlgorithmRepositoryStatusDeserializer;


/**
 * algorithm
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_algorithm_repository")
@Schema(description = "VlsAlgorithmRepositoryEntity对象")
@EqualsAndHashCode(callSuper = true)
public class AlgorithmRepository extends TenantEntity {
	private static final long serialVersionUID = 1L;

	/**
	 * algorithm
	 */
	@Schema(description = "算法仓库名称")
	private String name;
	/**
	 * algorithm
	 */
	@Schema(description = "拥有算法数量")
	private Integer algorithmCount;
	/**
	 *
	 */
	@Schema(description = "仓库类型")
	private AlgorithmRepositoryTypeEnum repositoryType;
	/**
	 * remark
	 */
	@Schema(description = "备注")
	private String remark;

	/**
	 * Accepts both the persisted numeric status and the legacy enabled/disabled
	 * values sent by the algorithm repository management page.
	 */
	@Override
	@JsonDeserialize(using = AlgorithmRepositoryStatusDeserializer.class)
	public void setStatus(Integer status) {
		super.setStatus(status);
	}

}
