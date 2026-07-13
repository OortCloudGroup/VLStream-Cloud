/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
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
