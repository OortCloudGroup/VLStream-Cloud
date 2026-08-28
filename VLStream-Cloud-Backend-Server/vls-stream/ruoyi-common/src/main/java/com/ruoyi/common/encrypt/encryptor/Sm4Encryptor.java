/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.encrypt.encryptor;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.symmetric.SM4;
import com.ruoyi.common.encrypt.EncryptContext;
import com.ruoyi.common.enums.AlgorithmType;
import com.ruoyi.common.enums.EncodeType;

import java.nio.charset.StandardCharsets;

/**
 * sm4algorithm
 *
 * @author
 * @version 4.6.0
 */
public class Sm4Encryptor extends AbstractEncryptor {

    private final SM4 sm4;

    public Sm4Encryptor(EncryptContext context) {
        super(context);
        String password = context.getPassword();
        if (StrUtil.isBlank(password)) {
            throw new IllegalArgumentException("SM4没有获得秘钥信息");
        }
        // sm4algorithm need to is 16
        if (16 != password.length()) {
            throw new IllegalArgumentException("SM4秘钥长度应该为16位，实际为" + password.length() + "位");
        }
        this.sm4 = SmUtil.sm4(password.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * current algorithm
     */
    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.SM4;
    }

    /**
     *
     *
     * @param value
     * @param encodeType after
     */
    @Override
    public String encrypt(String value, EncodeType encodeType) {
        if (encodeType == EncodeType.HEX) {
            return sm4.encryptHex(value);
        } else {
            return sm4.encryptBase64(value);
        }
    }

    /**
     *
     *
     * @param value
     */
    @Override
    public String decrypt(String value) {
        return this.sm4.decryptStr(value);
    }
}
