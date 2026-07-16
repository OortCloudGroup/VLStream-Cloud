/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.dto;

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
