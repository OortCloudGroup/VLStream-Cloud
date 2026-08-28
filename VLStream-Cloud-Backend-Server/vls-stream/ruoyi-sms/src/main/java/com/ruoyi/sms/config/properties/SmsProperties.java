/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.sms.config.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SMS configurationproperty
 *
 * @author Lion Li
 * @version 4.2.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "sms")
public class SmsProperties {

    private Boolean enabled;

    /**
     * configurationnode
     * dysmsapi.aliyuncs.com
     * sms.tencentcloudapi.com
     */
    private String endpoint;

    /**
     * key
     */
    private String accessKeyId;

    /**
     *
     */
    private String accessKeySecret;

    /*
     *
     */
    private String signName;

    /**
     * ID ( )
     */
    private String sdkAppId;

}
