/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 * workflow object wf_category
 *
 * @author KonBAI
 * @date 2022-01-15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_category")
public class WfCategory extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * ID
     */
    @TableId(value = "category_id")
    private Long categoryId;

    /**
     * id
     */
    private String tenantId;
    /**
     * user ID
     */
    private String userId;

    /**
     *
     */
    @NotBlank(message = "分类名称不能为空")
    private String categoryName;
    /**
     *
     */
    @NotBlank(message = "分类编码不能为空")
    private String code;
    /**
     * remark
     */
    private String remark;
    /**
     * Delete (0represents in 2represents Delete )
     */
    @TableLogic
    private String delFlag;

}
