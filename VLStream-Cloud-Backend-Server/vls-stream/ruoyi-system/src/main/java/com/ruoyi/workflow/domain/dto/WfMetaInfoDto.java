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
     * creator (username)
     */
    private String createUser;

    /**
     * workflow
     */
    private String description;
    /**
     * form
     */
    private Integer formType;
    /**
     * form
     */
    private String formId;
    /**
     * id
     */
    private String iconId;
    /**
     * whether
     */
    private String showMobile = "0";
}
