/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import lombok.Getter;

@Getter
public class TunnelApiException extends RuntimeException {
    private final int status;

    public TunnelApiException(int status, String message) {
        super(message);
        this.status = status;
    }
}
