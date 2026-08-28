/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.oss.enumd;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * minio configuration
 *
 * @author Lion Li
 */
@Getter
@AllArgsConstructor
public enum PolicyType {

    /**
     * only
     */
    READ("read-only"),

    /**
     * only
     */
    WRITE("write-only"),

    /**
     *
     */
    READ_WRITE("read-write");

    /**
     *
     */
    private final String type;

}
