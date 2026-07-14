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
 * 用户对象 sys_user
 *
 * @author Lion Li
 */

@Data
@NoArgsConstructor
@TableName("sys_user")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SysUser implements Serializable {

    /**
     * 用户ID
     */
    @TableId(value = "user_id")
    private String userId;

//    /**
//     * 用户UUID
//     */
//    private String oortUuid;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 岗位id
     */
    private String jobId;

    /**
     * 职位id
     */
    private String postId;

    /**
     * 用户身份证号
     */
    private String idcard;


    /**
     * 部门ID
     */
    private String deptId;

    /**
     * 部门名称
     */
    private String deptName;

    /**
     * 部门UUID
     */
    private String deptCode;

    /**
     * 用户账号
     */
    @Xss(message = "用户账号不能包含脚本字符")
    @NotBlank(message = "用户账号不能为空")
    @Size(min = 0, max = 30, message = "用户账号长度不能超过{max}个字符")
    private String userName;

    /**
     * 用户昵称
     */
    @Xss(message = "用户昵称不能包含脚本字符")
    @Size(min = 0, max = 30, message = "用户昵称长度不能超过{max}个字符")
    private String nickName;

    /**
     * 登录id
     */
    private String loginId;

    /**
     * 用户类型（sys_user系统用户）
     */
    private String userType;

    /**
     * 用户邮箱
     */
    @Sensitive(strategy = SensitiveStrategy.EMAIL)
    @Email(message = "邮箱格式不正确")
    @Size(min = 0, max = 50, message = "邮箱长度不能超过{max}个字符")
    private String email;

    /**
     * 手机号码
     */
//    @Sensitive(strategy = SensitiveStrategy.PHONE)
    private String phonenumber;

    /**
     * 用户性别
     */
    private String sex;

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 密码
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
     * 帐号状态（0正常 1停用）
     */
    private String status;

    /**
     * 删除标志（0代表存在 2代表删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 最后登录IP
     */
    private String loginIp;

    /**
     * 最后登录时间
     */
    private String loginDate;

//    /**
//     * 职位类型（1:兼职 0:正职）
//     */
//    private String oortIspart;

//    /**
//     * 职位名称
//     */
//    private String oortJobname;

    /**
     * 部门信息
     */
    private String deptInfo;

    /**
     * 部门对象
     */
    @TableField(exist = false)
    private SysDeptView dept;

    /**
     * 角色对象
     */
    @TableField(exist = false)
    private List<SysRole> roles;

    /**
     * 角色组
     */
    @TableField(exist = false)
    private String[] roleIds;

    /**
     * 岗位组
     */
    @TableField(exist = false)
    private Long[] postIds;

    /**
     * 数据权限 当前角色ID
     */
    @TableField(exist = false)
    private Long roleId;

    /**
     * 搜索值
     */
    @JsonIgnore
    @TableField(exist = false)
    private String searchValue;

    /**
     * 创建者
     */
    private String createBy;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新者
     */
    private String updateBy;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 请求参数
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
