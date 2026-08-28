/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.core.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ruoyi.common.annotation.Sensitive;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.enums.SensitiveStrategy;
import com.ruoyi.common.xss.Xss;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * userobject sys_user
 *
 * @author Lion Li
 */

@Data
@NoArgsConstructor
@TableName("sys_user")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SysUser implements Serializable {

    /**
     * user ID
     */
    @TableId(value = "user_id")
    private String userId;

//    /**
// * userUUID
//     */
//    private String oortUuid;

    /**
     * tenant ID
     */
    private String tenantId;

    /**
     * user ID; is empty, sub user .
     */
    private String platformUserId;

    /**
     * id
     */
    private String jobId;

    /**
     * id
     */
    private String postId;

    /**
     * user ID card number
     */
    private String idcard;


    /**
     * department ID
     */
    private String deptId;

    /**
     * department name
     */
    private String deptName;

    /**
     * departmentUUID
     */
    private String deptCode;

    /**
     * user
     */
    @Xss(message = "用户账号不能包含脚本字符")
    @NotBlank(message = "用户账号不能为空")
    @Size(min = 0, max = 30, message = "用户账号长度不能超过{max}个字符")
    private String userName;

    /**
     * user
     */
    @Xss(message = "用户昵称不能包含脚本字符")
    @Size(min = 0, max = 30, message = "用户昵称长度不能超过{max}个字符")
    private String nickName;

    /**
     * id
     */
    private String loginId;

    /**
     * user (sys_user user)
     */
    private String userType;

    /**
     * user
     */
    @Sensitive(strategy = SensitiveStrategy.EMAIL)
    @Email(message = "邮箱格式不正确")
    @Size(min = 0, max = 50, message = "邮箱长度不能超过{max}个字符")
    private String email;

    /**
     *
     */
//    @Sensitive(strategy = SensitiveStrategy.PHONE)
    private String phonenumber;

    /**
     * user
     */
    private String sex;

    /**
     * user
     */
    private String avatar;

    /**
     *
     */
    @TableField(
        insertStrategy = FieldStrategy.NOT_EMPTY,
        updateStrategy = FieldStrategy.NOT_EMPTY,
        whereStrategy = FieldStrategy.NOT_EMPTY
    )
    private String password;

    @JsonIgnore
    @JsonProperty
    public String getPassword() {
        return password;
    }

    /**
     * Status (0 normal 1 disabled)
     */
    private String status;

    /**
     * Delete (0represents in 2represents Delete )
     */
    @TableLogic
    private String delFlag;

    /**
     * after IP
     */
    private String loginIp;

    /**
     * after
     */
    private String loginDate;

//    /**
// * (1: 0: )
//     */
//    private String oortIspart;

//    /**
// *
//     */
//    private String oortJobname;

    /**
     * departmentinfo
     */
    private String deptInfo;

    /**
     * departmentobject
     */
    @TableField(exist = false)
    private SysDeptView dept;

    /**
     * roleobject
     */
    @TableField(exist = false)
    private List<SysRole> roles;

    /**
     * role
     */
    @TableField(exist = false)
    private String[] roleIds;

    /**
     *
     */
    @TableField(exist = false)
    private Long[] postIds;

    /**
     * data current role ID
     */
    @TableField(exist = false)
    private Long roleId;

    /**
     * value
     */
    @JsonIgnore
    @TableField(exist = false)
    private String searchValue;

    /**
     * creator
     */
    private String createBy;

    /**
     * create time
     */
    private Date createTime;

    /**
     * updater
     */
    private String updateBy;

    /**
     * update time
     */
    private Date updateTime;

    /**
     * parameter
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @TableField(exist = false)
    private Map<String, Object> params = new HashMap<>();

    public SysUser(String userId) {
        this.userId = userId;
    }

    public boolean isAdmin() {
        return UserConstants.ADMIN_ID.equals(this.userId);
    }

}
