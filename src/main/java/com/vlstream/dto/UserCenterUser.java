package com.vlstream.dto;

import lombok.Data;

/**
 * Unified User Center User Information DTO
 */
@Data
public class UserCenterUser {

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
     * Last request time
     */
    private String lastRequestTime;

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