/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 用户和角色关联 sys_user_role
 *
 * @author Lion Li
 */

@Data
@TableName("sys_user_role")
public class SysUserRoleView {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 角色ID
     */
    @TableId(type = IdType.INPUT)
    private String  roleId;
    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 创建时间
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date createdAt;
    /**
     * 修改时间
     */
    private String updatedAt;

    /**
     * 删除时间
     */
    private Long deletedAt;

    /**
     * 角色名称
     */
    private String roleName;
}
