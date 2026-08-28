/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.framework.config.properties;

import com.ruoyi.common.enums.AlgorithmType;
import com.ruoyi.common.enums.EncodeType;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * propertyconfiguration
 *
 * @author
 * @version 4.6.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "mybatis-encryptor")
public class EncryptorProperties {

    /**
     *
     */
    private Boolean enable;

    /**
     * algorithm
     */
    private AlgorithmType algorithm;

    /**
     * full
     */
    private String password;

    /**
     *
     */
    private String publicKey;

    /**
     *
     */
    private String privateKey;

    /**
     * , base64/hex
     */
    private EncodeType encode;

}
