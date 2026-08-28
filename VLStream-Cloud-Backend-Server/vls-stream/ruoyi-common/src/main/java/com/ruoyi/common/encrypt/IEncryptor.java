/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.encrypt;

import com.ruoyi.common.enums.AlgorithmType;
import com.ruoyi.common.enums.EncodeType;

/**
 *
 *
 * @author
 * @version 4.6.0
 */
public interface IEncryptor {

    /**
     * current algorithm
     */
    AlgorithmType algorithm();

    /**
     *
     *
     * @param value
     * @param encodeType after
     * @return after
     */
    String encrypt(String value, EncodeType encodeType);

    /**
     *
     *
     * @param value
     * @return after
     */
    String decrypt(String value);
}
