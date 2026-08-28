/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.flowable.common.enums;

import com.ruoyi.common.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author konbai
 * @since 2023/3/9 00:45
 */
@Getter
@AllArgsConstructor
public enum ProcessStatus {

    /**
     * in (approval in )
     */
    RUNNING("running"),
    /**
     * already
     */
    TERMINATED("terminated"),
    /**
     * already
     */
    COMPLETED("completed"),
    /**
     * already
     */
    CANCELED("canceled");

    private final String status;

    public static ProcessStatus getProcessStatus(String str) {
        if (StringUtils.isNotBlank(str)) {
            for (ProcessStatus value : values()) {
                if (StringUtils.equalsIgnoreCase(str, value.getStatus())) {
                    return value;
                }
            }
        }
        return null;
    }
}
