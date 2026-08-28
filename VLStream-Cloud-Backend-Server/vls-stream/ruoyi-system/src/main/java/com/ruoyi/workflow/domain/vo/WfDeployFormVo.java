/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain.vo;

import lombok.Data;

/**
 * instance and form object
 *
 * @author KonBAI
 * @createTime 2022/7/17 18:29
 */
@Data
public class WfDeployFormVo {

    private static final long serialVersionUID = 1L;

    /**
     * workflow primary key
     */
    private String deployId;

    /**
     * formKey
     */
    private String formKey;

    /**
     * nodeKey
     */
    private String nodeKey;

    /**
     * form
     */
    private String formName;

    /**
     * node
     */
    private String nodeName;

    /**
     * form
     */
    private String content;
}
