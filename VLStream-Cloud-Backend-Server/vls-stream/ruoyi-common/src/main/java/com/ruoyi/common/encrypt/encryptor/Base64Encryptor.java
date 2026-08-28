/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.encrypt.encryptor;

import cn.hutool.core.codec.Base64;
import com.ruoyi.common.encrypt.EncryptContext;
import com.ruoyi.common.enums.AlgorithmType;
import com.ruoyi.common.enums.EncodeType;

/**
 * Base64algorithm
 *
 * @author
 * @version 4.6.0
 */
public class Base64Encryptor extends AbstractEncryptor {

    public Base64Encryptor(EncryptContext context) {
        super(context);
    }

    /**
     * current algorithm
     */
    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.BASE64;
    }

    /**
     *
     *
     * @param value
     * @param encodeType after
     */
    @Override
    public String encrypt(String value, EncodeType encodeType) {
        return Base64.encode(value);
    }

    /**
     *
     *
     * @param value
     */
    @Override
    public String decrypt(String value) {
        return Base64.decodeStr(value);
    }
}
