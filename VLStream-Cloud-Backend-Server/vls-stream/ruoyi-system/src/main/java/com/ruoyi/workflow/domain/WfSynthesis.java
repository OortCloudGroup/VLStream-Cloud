/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TreeEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 综合通用流程对象 wf_synthesis
 *
 * @author 雷超群
 * @date 2025-01-04
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_synthesis")
public class WfSynthesis extends TreeEntity<WfSynthesis> {

    private static final long serialVersionUID=1L;

    /**
     * 主键ID
     */
    @TableId(value = "synthesis_id")
    private String synthesisId;
    /**
     * 分类名称
     */
    private String categoryName;
    /**
     * 租户id
     */
    private String tenantId;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 删除标记，0表示未删除，1表示删除
     */
    @TableLogic
    private String delFlag;

}
