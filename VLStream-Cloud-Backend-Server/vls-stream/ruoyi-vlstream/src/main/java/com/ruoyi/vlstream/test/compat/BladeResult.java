/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.compat;

import java.io.Serializable;

/**
 * SpringBlade-compatible response envelope for VLStream frontend calls.
 */
public class BladeResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int SUCCESS_CODE = 200;
    private static final int FAIL_CODE = 500;

    private int code;
    private boolean success;
    private String msg;
    private T data;

    public BladeResult() {
    }

    private BladeResult(int code, boolean success, String msg, T data) {
        this.code = code;
        this.success = success;
        this.msg = msg;
        this.data = data;
    }

    /**
     * Create a SpringBlade-compatible success response with data.
     */
    public static <T> BladeResult<T> success(T data) {
        return new BladeResult<T>(SUCCESS_CODE, true, "操作成功", data);
    }

    /**
     * Create a SpringBlade-compatible empty success response.
     */
    public static BladeResult<Void> success() {
        return new BladeResult<Void>(SUCCESS_CODE, true, "操作成功", null);
    }

    /**
     * Create a SpringBlade-compatible failure response with message.
     */
    public static <T> BladeResult<T> fail(String msg) {
        return new BladeResult<T>(FAIL_CODE, false, msg, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
