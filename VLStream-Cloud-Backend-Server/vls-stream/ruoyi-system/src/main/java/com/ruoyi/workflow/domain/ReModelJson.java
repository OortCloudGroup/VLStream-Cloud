/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;


import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 流程图JSON对象 re_mode_json
 *
 * @author 雷超群
 * @date 2024-11-02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("re_model_json")
public class ReModelJson extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 与act_re_model 表的关联ID
     */
    @TableId(value = "model_id")
    private String modelId;
    /**
     * 租户id
     */
    private String tenantId;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 流程图JSON
     */
    private String jsonContent;
    /**
     * 0表示未删除,1表示删除
     */
    @TableLogic
    private String delFlag;

}
