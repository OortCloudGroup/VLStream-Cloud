package com.ruoyi.vlstream.test.vlstream.pojo.dto;

import lombok.Data;

/**
 * 登录响应DTO
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
