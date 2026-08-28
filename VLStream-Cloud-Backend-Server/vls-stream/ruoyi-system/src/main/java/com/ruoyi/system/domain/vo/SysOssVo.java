/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.util.Date;

/**
 * OSSobject object sys_oss
 *
 * @author Lion Li
 */
@Data
public class SysOssVo {

    private static final long serialVersionUID = 1L;

    /**
     * object primary key
     */
    private Long ossId;

    /**
     *
     */
    private String fileName;

    /**
     *
     */
    private String originalName;

    /**
     * after
     */
    private String fileSuffix;

    /**
     * URL
     */
    private String url;

    /**
     * create time
     */
    private Date createTime;

    /**
     *
     */
    private String createBy;

    /**
     * service
     */
    private String service;


}
