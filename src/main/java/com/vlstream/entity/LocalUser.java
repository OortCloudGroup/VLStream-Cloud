package com.vlstream.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Local User Entity Class
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("local_users")
public class LocalUser {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * Unified user center user ID
     */
    @TableField("user_id")
    private String userId;

    /**
     * Tenant ID
     */
    @TableField("tenant_id")
    private String tenantId;

    /**
     * Login account
     */
    @TableField("login_id")
    private String loginId;

    /**
     * User name
     */
    @TableField("user_name")
    private String userName;

    /**
     * User name pinyin
     */
    @TableField("user_name_py")
    private String userNamePy;

    /**
     * User name first letter pinyin
     */
    @TableField("user_name_fpy")
    private String userNameFpy;

    /**
     * User avatar
     */
    @TableField("photo")
    private String photo;

    /**
     * User status: 1-Normal, 2-Disabled
     */
    @TableField("status")
    private Integer status;

    /**
     * User source: 1-System created, 2-Organization created, 3-User pool created, 4-Registered
     */
    @TableField("form")
    private Integer form;

    /**
     * Last login time
     */
    @TableField("login_time")
    private LocalDateTime loginTime;

    /**
     * Last login IP
     */
    @TableField("login_ip")
    private String loginIp;

    /**
     * User login identity: 1-B/E end user, 2-C end user
     */
    @TableField("login_type")
    private Integer loginType;

    /**
     * Login client type
     */
    @TableField("client")
    private String client;

    /**
     * Access token
     */
    @TableField("access_token")
    private String accessToken;

    /**
     * Token expiration time
     */
    @TableField("token_expire_time")
    private LocalDateTime tokenExpireTime;

    /**
     * Creation time
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * Update time
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
} 