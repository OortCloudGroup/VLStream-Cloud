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

/**
 * form object wf_form_app
 *
 * @author
 * @date 2025-04-26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wf_form_app")
public class WfFormApp extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * form id
     */
    @TableId(value = "category_id")
    private String categoryId;
    /**
     * ID
     */
    private String applicationId;
    /**
     *
     */
    private String applicationName;
    /**
     *
     */
    private String applicationSecret;
    /**
     * tenant ID
     */
    private String tenantId;
    /**
     * user ID
     */
    private String userId;
    /**
     * id
     */
    private String parentId;
    /**
     * form
     */
    private String categoryName;
    /**
     *
     */
    private String code;
    /**
     * remark
     */
    private String remark;
    /**
     * Delete (0represents in 1represents Delete )
     */
    @TableLogic
    private String delFlag;
    /**
     * 0 , 1
     */
    private String appFlag;
    /**
     *
     */
    private String images;
    /**
     * 0workflow 1work order
     */
    private String type;
}
