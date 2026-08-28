/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.domain;

import lombok.Data;

/**
 * current in will
 *
 * @author Lion Li
 */

@Data
public class SysUserOnline {

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
