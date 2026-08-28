/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.annotation;

import com.ruoyi.common.enums.AlgorithmType;
import com.ruoyi.common.enums.EncodeType;

import java.lang.annotation.*;

/**
 * field
 *
 * @author
 */
@Documented
@Inherited
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface EncryptField {

    /**
     * algorithm
     */
    AlgorithmType algorithm() default AlgorithmType.DEFAULT;

    /**
     * . AES、SM4 need to
     */
    String password() default "";

    /**
     * . RSA、SM2 need to
     */
    String publicKey() default "";

    /**
     * . RSA、SM2 need to
     */
    String privateKey() default "";

    /**
     * . algorithm to BASE64
     */
    EncodeType encode() default EncodeType.DEFAULT;

}
