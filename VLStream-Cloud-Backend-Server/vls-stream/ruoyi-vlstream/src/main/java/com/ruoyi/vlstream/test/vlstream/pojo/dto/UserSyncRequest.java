/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.pojo.dto;

import lombok.Data;

/**
 * user DTO
 */
@Data
public class UserSyncRequest {

    /**
     * user ID
     */
    private String userId;

    /**
     * tenant ID
     */
    private String tenantId;

    /**
     *
     */
    private String loginId;

    /**
     * user
     */
    private String userName;

    /**
     *
     */
    private String loginTime;

    /**
     * IP
     */
    private String loginIP;

    /**
     *
     */
    private Integer loginType;

    /**
     *
     */
    private String client;

    /**
     *
     */
    private String accessToken;
}
