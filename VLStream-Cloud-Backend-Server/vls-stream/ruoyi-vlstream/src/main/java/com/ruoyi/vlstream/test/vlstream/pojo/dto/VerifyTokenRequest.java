package com.ruoyi.vlstream.test.vlstream.pojo.dto;

import lombok.Data;

/**
 * 验证Token请求DTO
 */
@Data
public class VerifyTokenRequest {

    /**
     * 访问令牌
     */
    private String accessToken;

}
