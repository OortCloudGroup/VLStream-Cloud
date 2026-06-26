package org.springblade.vlstream.pojo.dto;

import lombok.Data;

/**
 * Verify Token request DTO
 */
@Data
public class VerifyTokenRequest {

    /**
     * Access Token
     */
    private String accessToken;

}
