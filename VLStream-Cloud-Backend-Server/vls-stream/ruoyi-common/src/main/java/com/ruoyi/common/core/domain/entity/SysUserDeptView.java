/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.core.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * VIEWobject sys_user_dept_view_tenant
 *
 * @author ruoyi
 * @date 2024-10-29
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_dept_view")
public class SysUserDeptView extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * ID
     */
    private String userId;
    /**
     * tenant ID
     */
    private String tenantId;
    /**
     *
     */
    private String deptId;
    /**
     *
     */
    private String deptName;
    /**
     * 1: 2: 3:department 4: item 0: not
     */
    private Long deptType;
    /**
     * user before
     */
    private Long sort;
    /**
     * afterupdater
     */
    private String updatedBy;
    /**
     * create time
     */
    private Date createdAt;
    /**
     * update time
     */
    private Date updatedAt;
    /**
     * Delete
     */
    private String deletedAt;

}
