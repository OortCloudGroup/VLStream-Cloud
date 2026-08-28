/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.encrypt.encryptor;


import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.SM2;
import com.ruoyi.common.encrypt.EncryptContext;
import com.ruoyi.common.enums.AlgorithmType;
import com.ruoyi.common.enums.EncodeType;
import com.ruoyi.common.utils.StringUtils;

/**
 * sm2algorithm
 *
 * @author
 * @version 4.6.0
 */
public class Sm2Encryptor extends AbstractEncryptor {

    private final SM2 sm2;

    public Sm2Encryptor(EncryptContext context) {
        super(context);
        String privateKey = context.getPrivateKey();
        String publicKey = context.getPublicKey();
        if (StringUtils.isAnyEmpty(privateKey, publicKey)) {
            throw new IllegalArgumentException("SM2公私钥均需要提供，公钥加密，私钥解密。");
        }
        this.sm2 = SmUtil.sm2(Base64.decode(privateKey), Base64.decode(publicKey));
    }

    /**
     * current algorithm
     */
    @Override
    public AlgorithmType algorithm() {
        return AlgorithmType.SM2;
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
            return sm2.encryptHex(value, KeyType.PublicKey);
        } else {
            return sm2.encryptBase64(value, KeyType.PublicKey);
        }
    }

    /**
     *
     *
     * @param value
     */
    @Override
    public String decrypt(String value) {
        return this.sm2.decryptStr(value, KeyType.PrivateKey);
    }
}
