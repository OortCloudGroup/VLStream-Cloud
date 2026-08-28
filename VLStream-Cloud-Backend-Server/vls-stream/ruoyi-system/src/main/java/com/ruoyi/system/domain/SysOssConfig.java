/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * object configurationobject sys_oss_config
 *
 * @author Lion Li
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_oss_config")
public class SysOssConfig extends BaseEntity {

    /**
     * main
     */
    @TableId(value = "oss_config_id")
    private Long ossConfigId;

    /**
     * configurationkey
     */
    private String configKey;

    /**
     * accessKey
     */
    private String accessKey;

    /**
     *
     */
    private String secretKey;

    /**
     *
     */
    private String bucketName;

    /**
     * before
     */
    private String prefix;

    /**
     *
     */
    private String endpoint;

    /**
     * Custom
     */
    private String domain;

    /**
     * whether https (0 1 is )
     */
    private String isHttps;

    /**
     *
     */
    private String region;

    /**
     * whether (0= is ,1= )
     */
    private String status;

    /**
     * field
     */
    private String ext1;

    /**
     * remark
     */
    private String remark;

    /**
     * (0private 1public 2custom)
     */
    private String accessPolicy;
}
