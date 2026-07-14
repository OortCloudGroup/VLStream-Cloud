/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.flowable.common.constant;

import java.util.HashMap;
import java.util.Map;

public class UnifiedMessageUtil {

    private static final Map<String, String[]> PROCESS_VARIABLE_IDS = new HashMap<>();

    static {
        // 初始化特殊流程的表单项 ID 映射
        PROCESS_VARIABLE_IDS.put("租户审批", new String[]{"input24512", "input100523"});
        PROCESS_VARIABLE_IDS.put("用户审批", new String[]{"input31536", "input65087"});
        PROCESS_VARIABLE_IDS.put("部门审批", new String[]{"input67697", "input109668"});
        PROCESS_VARIABLE_IDS.put("企业主体审批", new String[]{"input34240", "input18636"});
    }

    /**
     * 获取特殊流程的表单项 ID 映射
     */
    public static Map<String, String[]> getProcessVariableIds() {
        return PROCESS_VARIABLE_IDS;
    }
}
