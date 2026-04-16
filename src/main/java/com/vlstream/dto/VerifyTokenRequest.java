package com.vlstream.dto;

import lombok.Data;

/**
 * Verify Token Request DTO
 */
@Data
public class VerifyTokenRequest {
    
    /**
     * Access token
     */
    private String accessToken;
    
} 