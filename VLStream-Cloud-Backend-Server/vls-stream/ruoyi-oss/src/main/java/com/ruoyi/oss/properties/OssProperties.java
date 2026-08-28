/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.oss.properties;

import lombok.Data;

/**
 * OSSobject configurationproperty
 *
 * @author Lion Li
 */
@Data
public class OssProperties {

    /**
     *
     */
    private String endpoint;

    /**
     * Custom
     */
    private String domain;

    /**
     * before
     */
    private String prefix;

    /**
     * ACCESS_KEY
     */
    private String accessKey;

    /**
     * SECRET_KEY
     */
    private String secretKey;

    /**
     * null / empty
     */
    private String bucketName;

    /**
     *
     */
    private String region;

    /**
     * whether https (Y= is ,N= )
     */
    private String isHttps;

    /**
     * (0private 1public 2custom)
     */
    private String accessPolicy;

}
