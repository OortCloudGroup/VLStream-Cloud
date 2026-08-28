/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.flowable.core;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class WFormInfo {
    // formconfiguration
    private WFormConf formConfig ;
    /**
     * form item
     */
    private List<Map<String, Object>> widgetList;
}
