/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.core.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * VIEW对象 sys_user_dept_view_tenant
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
     * 账号ID
     */
    private String userId;
    /**
     * 租户ID
     */
    private String tenantId;
    /**
     * 组织机构编码
     */
    private String deptId;
    /**
     * 组织机构名称
     */
    private String deptName;
    /**
     * 组织机构类型 1:集团 2:公司 3:部门 4:项目 0:未知
     */
    private Long deptType;
    /**
     * 用户排序越小越靠前
     */
    private Long sort;
    /**
     * 最后更新者的标识
     */
    private String updatedBy;
    /**
     * 创建时间
     */
    private Date createdAt;
    /**
     * 更新时间
     */
    private Date updatedAt;
    /**
     * 删除时间戳
     */
    private String deletedAt;

}
