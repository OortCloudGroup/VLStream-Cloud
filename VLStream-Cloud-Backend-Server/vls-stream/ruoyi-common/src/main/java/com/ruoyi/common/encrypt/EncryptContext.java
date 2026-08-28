/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.encrypt;

import com.ruoyi.common.enums.AlgorithmType;
import com.ruoyi.common.enums.EncodeType;
import lombok.Data;

/**
 * encryptor need to parameter.
 *
 * @author
 * @version 4.6.0
 */
@Data
public class EncryptContext {

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
