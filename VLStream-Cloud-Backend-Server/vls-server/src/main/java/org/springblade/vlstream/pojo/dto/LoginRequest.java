package org.springblade.vlstream.pojo.dto;

import lombok.Data;

/**
 * Login request DTO
 */
@Data
public class LoginRequest {

    /**
     * Encrypted user information string
     */
    private String userInfo;

}
