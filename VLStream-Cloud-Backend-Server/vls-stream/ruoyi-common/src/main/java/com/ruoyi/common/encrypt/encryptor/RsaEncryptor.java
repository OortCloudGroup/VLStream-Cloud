/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.encrypt.encryptor;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import com.ruoyi.common.encrypt.EncryptContext;
import com.ruoyi.common.enums.AlgorithmType;
import com.ruoyi.common.enums.EncodeType;
import com.ruoyi.common.utils.StringUtils;


/**
 * RSAalgorithm
 *
 * @author
 * @version 4.6.0
 */
public class RsaEncryptor extends AbstractEncryptor {

    private final RSA rsa;

    public RsaEncryptor(EncryptContext context) {
        super(context);
        String privateKey = context.getPrivateKey();
        String publicKey = context.getPublicKey();
        if (StringUtils.isAnyEmpty(privateKey, publicKey)) {
            throw new IllegalArgumentException("RSA公私钥均需要提供，公钥加密，私钥解密。");
        }
        this.rsa = SecureUtil.rsa(Base64.decode(privateKey), Base64.decode(publicKey));
    }

    /**
     * current algorithm
     */
    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.RSA;
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
            return rsa.encryptHex(value, KeyType.PublicKey);
        } else {
            return rsa.encryptBase64(value, KeyType.PublicKey);
        }
    }

    /**
     *
     *
     * @param value
     */
    @Override
    public String decrypt(String value) {
        return this.rsa.decryptStr(value, KeyType.PrivateKey);
    }
}
