/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.core.domain.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * current in will
 *
 * @author ruoyi
 */

@Data
@NoArgsConstructor
public class UserOnlineDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * will
     */
    private String tokenId;

    /**
     * department name
     */
    private String deptName;

    /**
     * username
     */
    private String userName;

    /**
     * IP
     */
    private String ipaddr;

    /**
     *
     */
    private String loginLocation;

    /**
     *
     */
    private String browser;

    /**
     * operation
     */
    private String os;

    /**
     *
     */
    private Long loginTime;

}
