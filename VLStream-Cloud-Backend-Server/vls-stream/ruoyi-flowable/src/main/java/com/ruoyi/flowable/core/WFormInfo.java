/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.flowable.core;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class WFormInfo {
    // 表单配置
    private WFormConf formConfig ;
    /**
     * 表单项
     */
    private List<Map<String, Object>> widgetList;
}
