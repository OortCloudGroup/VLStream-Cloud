/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.encrypt.encryptor;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;
import com.ruoyi.common.encrypt.EncryptContext;
import com.ruoyi.common.enums.AlgorithmType;
import com.ruoyi.common.enums.EncodeType;

import java.nio.charset.StandardCharsets;

/**
 * AESalgorithm
 *
 * @author
 * @version 4.6.0
 */
public class AesEncryptor extends AbstractEncryptor {

    private final AES aes;

    public AesEncryptor(EncryptContext context) {
        super(context);
        String password = context.getPassword();
        if (StrUtil.isBlank(password)) {
            throw new IllegalArgumentException("AES没有获得秘钥信息");
        }
        // aesalgorithm need to is 16 、24 、32
        int[] array = {16, 24, 32};
        if (!ArrayUtil.contains(array, password.length())) {
            throw new IllegalArgumentException("AES秘钥长度应该为16位、24位、32位，实际为" + password.length() + "位");
        }
        aes = SecureUtil.aes(context.getPassword().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * current algorithm
     */
    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.AES;
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
            return aes.encryptHex(value);
        } else {
            return aes.encryptBase64(value);
        }
    }

    /**
     *
     *
     * @param value
     */
    @Override
    public String decrypt(String value) {
        return this.aes.decryptStr(value);
    }
}
