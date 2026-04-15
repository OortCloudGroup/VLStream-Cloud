package com.vlstream.dto;

import lombok.Data;

/**
 * 用户同步请求DTO
 */
@Data
public class UserSyncRequest {

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 登录账号
     */
    private String loginId;

    /**
     * 用户姓名
     */
    private String userName;

    /**
     * 登录时间
     */
    private String loginTime;

    /**
     * 登录IP
     */
    private String loginIP;

    /**
     * 登录类型
     */
    private Integer loginType;

    /**
     * 客户端类型
     */
    private String client;

    /**
     * 访问令牌
     */
    private String accessToken;
} 