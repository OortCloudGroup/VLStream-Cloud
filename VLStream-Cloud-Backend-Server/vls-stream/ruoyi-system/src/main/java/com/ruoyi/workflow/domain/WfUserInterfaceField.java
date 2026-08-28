/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * userinterfacefield configuration
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_user_interface_field")
public class WfUserInterfaceField extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * user ID
     */
    private String userId;
    /**
     * interface
     */
    private String interfacePath;
    /**
     * field
     */
    private String fieldCodes; // JSON , ["taskId","taskName",…]
}
