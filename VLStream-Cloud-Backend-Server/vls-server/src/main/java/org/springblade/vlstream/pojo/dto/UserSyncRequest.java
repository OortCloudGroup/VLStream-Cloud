package org.springblade.vlstream.pojo.dto;

import lombok.Data;

/**
 * User synchronization request DTO
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
