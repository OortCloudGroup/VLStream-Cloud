/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;


import com.ruoyi.common.core.domain.BaseEntity;

/**
 * workflow JSONobject re_mode_json
 *
 * @author
 * @date 2024-11-02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("re_model_json")
public class ReModelJson extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * and act_re_model ID
     */
    @TableId(value = "model_id")
    private String modelId;
    /**
     * id
     */
    private String tenantId;
    /**
     * user ID
     */
    private String userId;
    /**
     * workflow JSON
     */
    private String jsonContent;
    /**
     * 0 not Delete ,1 Delete
     */
    @TableLogic
    private String delFlag;

}
