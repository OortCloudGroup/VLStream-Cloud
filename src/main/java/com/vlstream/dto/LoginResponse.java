package com.vlstream.dto;

import lombok.Data;

/**
 * Login Response DTO
 */
@Data
public class LoginResponse {
    
    private String userId;
    private String tenantId;
    private String loginId;
    private String userName;
    private String loginTime;
    private String LastRequestTime;
    private String loginIP;
    private Integer login_type;
    private String client;
    private String accessToken;
    
} 