/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.flowable.utils;

import cn.hutool.core.convert.Convert;
import com.ruoyi.flowable.core.FormConf;

import java.util.List;
import java.util.Map;

/**
 * workflowform
 *
 * @author KonBAI
 * @createTime 2022/8/7 17:09
 */
public class ProcessFormUtils {

    private static final String CONFIG = "__config__";
    private static final String MODEL = "__vModel__";

    /**
     * fill form item
     *
     * @param formConf formconfigurationinfo
     * @param data form
     */
    public static void fillFormData(FormConf formConf, Map<String, Object> data) {
        for (Map<String, Object> field : formConf.getFields()) {
            recursiveFillField(field, data);
        }
    }

    @SuppressWarnings("unchecked")
    private static void recursiveFillField(final Map<String, Object> field, final Map<String, Object> data) {
        if (!field.containsKey(CONFIG)) {
            return;
        }
        Map<String, Object> configMap = (Map<String, Object>) field.get(CONFIG);
        if (configMap.containsKey("children")) {
            List<Map<String, Object>> childrens = (List<Map<String, Object>>) configMap.get("children");
            for (Map<String, Object> children : childrens) {
                recursiveFillField(children, data);
            }
        }
        String modelKey = Convert.toStr(field.get(MODEL));
        Object value = data.get(modelKey);
        if (value != null) {
            configMap.put("defaultValue", value);
        }
    }
}
