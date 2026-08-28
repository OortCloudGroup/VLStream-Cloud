/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.exception;

/**
 * full
 *
 * @author ruoyi
 */
public class GlobalException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * prompt / tip
     */
    private String message;

    /**
     * ,
     * <p>
     * and {@link CommonResult#getDetailMessage()}
     */
    private String detailMessage;

    /**
     * null / empty method ,
     */
    public GlobalException() {
    }

    public GlobalException(String message) {
        this.message = message;
    }

    public String getDetailMessage() {
        return detailMessage;
    }

    public GlobalException setDetailMessage(String detailMessage) {
        this.detailMessage = detailMessage;
        return this;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public GlobalException setMessage(String message) {
        this.message = message;
        return this;
    }
}
