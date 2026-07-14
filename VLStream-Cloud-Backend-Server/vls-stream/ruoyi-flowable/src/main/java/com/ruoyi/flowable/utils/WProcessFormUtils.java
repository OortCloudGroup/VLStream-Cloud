/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.flowable.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import com.ruoyi.flowable.core.WFormInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * 流程表单工具类（新填充表单工具类）
 *
 * @author KonBAI
 * @createTime 2022/8/7 17:09
 */
public class WProcessFormUtils {

    private static final Logger log = LoggerFactory.getLogger(WProcessFormUtils.class);
    private static final String CONFIG = "options";
    private static final String MODEL = "id";

    /**
     * 填充表单项内容
     *
     * @param formInfo 表单配置信息
     * @param data     表单内容
     */
    public static void fillFormData(WFormInfo formInfo, Map<String, Object> data) {
        // 增加空值检查
        if (formInfo == null) {
            log.warn("WFormInfo为null，无法填充表单数据");
            return;
        }
        
        if (data == null || data.isEmpty()) {
            log.debug("表单数据为空，跳过填充");
            return;
        }
        
        List<Map<String, Object>> widgetList = formInfo.getWidgetList();
        if (CollUtil.isEmpty(widgetList)) {
            log.debug("widgetList为空，跳过填充");
            return;
        }
        
        for (Map<String, Object> field : widgetList) {
            if (field != null) {
                recursiveFillField(field, data);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void recursiveFillField(final Map<String, Object> field, final Map<String, Object> data) {
        if (field == null || data == null) {
            return;
        }
        
        if (!field.containsKey(CONFIG)) {
            return;
        }
        
        // 处理嵌套的widgetList
        if (field.containsKey("widgetList")) {
            Object widgetListObj = field.get("widgetList");
            if (widgetListObj instanceof List) {
                List<Map<String, Object>> childrens = (List<Map<String, Object>>) widgetListObj;
                if (CollUtil.isNotEmpty(childrens)) {
                    for (Map<String, Object> children : childrens) {
                        if (children != null) {
                            recursiveFillField(children, data);
                        }
                    }
                }
            }
        }
        
        // 处理cols
        if (field.containsKey("cols")) {
            Object colsObj = field.get("cols");
            if (colsObj instanceof List) {
                List<Map<String, Object>> childrens = (List<Map<String, Object>>) colsObj;
                if (CollUtil.isNotEmpty(childrens)) {
                    for (Map<String, Object> children : childrens) {
                        if (children != null) {
                            recursiveFillField(children, data);
                        }
                    }
                }
            }
        }
        
        // 填充字段值
        String modelKey = Convert.toStr(field.get(MODEL));
        if (modelKey != null && !modelKey.isEmpty()) {
            Object value = data.get(modelKey);
            if (value != null) {
                Object configObj = field.get(CONFIG);
                if (configObj instanceof Map) {
                    Map<String, Object> optionsMap = (Map<String, Object>) configObj;
                    optionsMap.put("defaultValue", value);
                } else {
                    log.warn("字段的config不是Map类型，无法设置defaultValue，modelKey: {}", modelKey);
                }
            }
        }
    }
}
