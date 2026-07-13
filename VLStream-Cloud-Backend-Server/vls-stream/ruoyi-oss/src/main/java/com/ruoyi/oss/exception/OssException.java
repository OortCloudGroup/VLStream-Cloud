/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.oss.exception;

/**
 * OSS异常类
 *
 * @author Lion Li
 */
public class OssException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OssException(String msg) {
        super(msg);
    }

}
