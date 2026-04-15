package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 本地用户实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("local_users")
public class LocalUser {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 统一用户中心用户ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private String tenantId;

    /**
     * 登录账号
     */
    @TableField("login_id")
    private String loginId;

    /**
     * 用户姓名
     */
    @TableField("user_name")
    private String userName;

    /**
     * 姓名拼音
     */
    @TableField("user_name_py")
    private String userNamePy;

    /**
     * 姓名首字母
     */
    @TableField("user_name_fpy")
    private String userNameFpy;

    /**
     * 用户头像
     */
    @TableField("photo")
    private String photo;

    /**
     * 用户状态：1正常，2禁用
     */
    @TableField("status")
    private Integer status;

    /**
     * 用户来源：1系统创建，2组织创建，3用户池创建，4注册
     */
    @TableField("form")
    private Integer form;

    /**
     * 最后登录时间
     */
    @TableField("login_time")
    private LocalDateTime loginTime;

    /**
     * 最后登录IP
     */
    @TableField("login_ip")
    private String loginIp;

    /**
     * 用户登录身份：1B/E端用户，2C端用户
     */
    @TableField("login_type")
    private Integer loginType;

    /**
     * 登录客户端类型
     */
    @TableField("client")
    private String client;

    /**
     * 访问令牌
     */
    @TableField("access_token")
    private String accessToken;

    /**
     * 令牌过期时间
     */
    @TableField("token_expire_time")
    private LocalDateTime tokenExpireTime;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
} 