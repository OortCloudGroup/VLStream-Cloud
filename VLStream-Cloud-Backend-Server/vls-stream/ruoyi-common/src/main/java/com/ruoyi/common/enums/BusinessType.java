/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.enums;

/**
 * operation
 *
 * @author ruoyi
 */
public enum BusinessType {
    /**
     *
     */
    OTHER,

    /**
     * Add
     */
    INSERT,

    /**
     *
     */
    COPY,

    /**
     * Update
     */
    UPDATE,

    /**
     * Delete
     */
    DELETE,

    /**
     *
     */
    GRANT,

    /**
     * Export
     */
    EXPORT,

    /**
     * Import
     */
    IMPORT,

    /**
     *
     */
    FORCE,

    /**
     * Generate
     */
    GENCODE,

    /**
     * null / empty data
     */
    CLEAN,
}
