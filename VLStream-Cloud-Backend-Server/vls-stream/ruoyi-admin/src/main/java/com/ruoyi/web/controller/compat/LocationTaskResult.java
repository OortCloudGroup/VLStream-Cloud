/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

/**
 * Response envelope used by the original apaas-location-service task APIs.
 *
 * @param <T> response data type
 */
public final class LocationTaskResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int code;
    private final String msg;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T data;

    /**
     * Create an immutable legacy response envelope.
     */
    private LocationTaskResult(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * Create a successful response without a data field.
     */
    public static LocationTaskResult<Void> success() {
        return new LocationTaskResult<Void>(200, "成功", null);
    }

    /**
     * Create a successful response with data.
     */
    public static <T> LocationTaskResult<T> success(T data) {
        return new LocationTaskResult<T>(200, "成功", data);
    }

    /**
     * Create an error response using a legacy business code and message.
     */
    public static <T> LocationTaskResult<T> error(int code, String msg) {
        return new LocationTaskResult<T>(code, msg, null);
    }

    /**
     * Create the response returned by deprecated Go task endpoints.
     */
    public static LocationTaskResult<Void> deprecated() {
        return new LocationTaskResult<Void>(404, "接口已废弃", null);
    }

    /**
     * Return the legacy business status code.
     */
    public int getCode() {
        return code;
    }

    /**
     * Return the legacy response message.
     */
    public String getMsg() {
        return msg;
    }

    /**
     * Return the optional response data.
     */
    public T getData() {
        return data;
    }
}
