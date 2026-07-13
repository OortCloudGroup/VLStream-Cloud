/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户接口字段显示配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_user_interface_field")
public class WfUserInterfaceField extends BaseEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 用户id
     */
    private String userId;
    /**
     * 接口路径
     */
    private String interfacePath;
    /**
     * 字段列表
     */
    private String fieldCodes; // JSON 字符串，存 ["taskId","taskName",…]
}
