/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.domain.dto;

import lombok.Data;

/**
 * @author KonBAI
 * @createTime 2022/6/21 9:16
 */
@Data
public class WfMetaInfoDto {

    /**
     * 创建者（username）
     */
    private String createUser;

    /**
     * 流程描述
     */
    private String description;
    /**
     * 表单类型
     */
    private Integer formType;
    /**
     * 表单编号
     */
    private String formId;
    /**
     * 图标id
     */
    private String iconId;
    /**
     * 手机端是否显示
     */
    private String showMobile = "0";
}
