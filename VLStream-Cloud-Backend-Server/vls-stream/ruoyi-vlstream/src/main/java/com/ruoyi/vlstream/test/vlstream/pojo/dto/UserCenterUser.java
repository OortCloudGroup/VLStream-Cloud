/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.pojo.dto;

import lombok.Data;

/**
 * user in userinfoDTO
 */
@Data
public class UserCenterUser {

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
     * after
     */
    private String lastRequestTime;

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
