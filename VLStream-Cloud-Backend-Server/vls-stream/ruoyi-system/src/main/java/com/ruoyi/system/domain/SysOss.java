/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * OSSobject object
 *
 * @author Lion Li
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_oss")
public class SysOss extends BaseEntity {

    /**
     * object primary key
     */
    @TableId(value = "oss_id")
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
