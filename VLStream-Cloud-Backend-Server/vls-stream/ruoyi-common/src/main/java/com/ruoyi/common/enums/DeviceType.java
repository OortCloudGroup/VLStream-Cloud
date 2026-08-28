/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * device
 * user
 *
 * @author Lion Li
 */
@Getter
@AllArgsConstructor
public enum DeviceType {

    /**
     * pc
     */
    PC("pc"),

    /**
     * app
     */
    APP("app"),

    /**
     *
     */
    XCX("xcx");

    private final String device;
}
