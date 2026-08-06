/*
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
 * 算法仓库表 实体类
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
	 * 算法仓库名称
	 */
	@Schema(description = "算法仓库名称")
	private String name;
	/**
	 * 拥有算法数量
	 */
	@Schema(description = "拥有算法数量")
	private Integer algorithmCount;
	/**
	 * 仓库类型
	 */
	@Schema(description = "仓库类型")
	private AlgorithmRepositoryTypeEnum repositoryType;
	/**
	 * 备注
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
