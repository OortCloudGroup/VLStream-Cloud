package org.springblade.vlstream.pojo.dto;

import lombok.Data;

/**
 * Unified user center user information DTO
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
     * Login account
     */
    private String loginId;

    /**
     * User's name
     */
    private String userName;

    /**
     * Login time
     */
    private String loginTime;

    /**
     * Last Request Time
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
     * Access Token
     */
    private String accessToken;
}
