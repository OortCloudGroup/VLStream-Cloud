/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
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
 * user and role sys_user_role
 *
 * @author Lion Li
 */

@Data
@TableName("sys_user_role")
public class SysUserRoleView {

    /**
     * user ID
     */
    private String userId;

    /**
     * role ID
     */
    @TableId(type = IdType.INPUT)
    private String  roleId;
    /**
     * tenant ID
     */
    private String tenantId;

    /**
     * create time
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date createdAt;
    /**
     * Update
     */
    private String updatedAt;

    /**
     * Delete
     */
    private Long deletedAt;

    /**
     * role name
     */
    private String roleName;
}
