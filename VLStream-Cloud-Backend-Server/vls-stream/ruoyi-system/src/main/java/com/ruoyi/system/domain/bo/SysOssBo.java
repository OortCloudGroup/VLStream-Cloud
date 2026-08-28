/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * OSSobject Query object sys_oss
 *
 * @author Lion Li
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysOssBo extends BaseEntity {

    /**
     * ossId
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
     * service
     */
    private String service;

}
