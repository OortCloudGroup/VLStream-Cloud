/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * workflow instance formobject sys_instance_form
 *
 * @author KonBAI
 * @createTime 2022/3/7 22:07
 */
@Data
@TableName("wf_deploy_form")
public class WfDeployForm {
    private static final long serialVersionUID = 1L;

    /**
     * workflow primary key
     */
    @TableId("deploy_id")
    private String deployId;

    /**
     * formKey
     */
    private String formKey;

    private String tenantId;

    private String userId;

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
