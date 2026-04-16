package com.vlstream.dto;

import lombok.Data;

/**
 * Login Request DTO
 */
@Data
public class LoginRequest {
    
    /**
     * Encrypted user information string
     */
    private String userInfo;
    
} 