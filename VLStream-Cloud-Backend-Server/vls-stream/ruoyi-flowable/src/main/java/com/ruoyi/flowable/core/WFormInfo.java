/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
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
