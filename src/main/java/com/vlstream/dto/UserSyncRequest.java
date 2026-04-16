package com.vlstream.dto;

import lombok.Data;

/**
 * User Sync Request DTO
 */
@Data
public class UserSyncRequest {

    /**
     * User ID
     */
    private String userId;

    /**
     * Tenant ID
     */
    private String tenantId;

    /**
     * Login ID
     */
    private String loginId;

    /**
     * User name
     */
    private String userName;

    /**
     * Login time
     */
    private String loginTime;

    /**
     * Login IP
     */
    private String loginIP;

    /**
     * Login type
     */
    private Integer loginType;

    /**
     * Client type
     */
    private String client;

    /**
     * Access token
     */
    private String accessToken;
} 