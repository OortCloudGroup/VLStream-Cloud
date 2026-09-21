/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.util.Date;

/**
 * Platform logo view returned to the console.
 */
@Data
public class PlatformLogoVo {

    private Long id;
    private String logoUrl;
    private String description;
    private Boolean active;
    private Boolean systemDefault;
    private String createBy;
    private Date createTime;
    private Date updateTime;
}
