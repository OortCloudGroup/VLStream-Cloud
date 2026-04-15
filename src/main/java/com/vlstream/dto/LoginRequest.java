package com.vlstream.dto;

import lombok.Data;

/**
 * 登录请求DTO
 */
@Data
public class LoginRequest {
    
    /**
     * 加密后的用户信息字符串
     */
    private String userInfo;
    
} 