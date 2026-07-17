/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.workflow.domain.vo.WorkOrderAppVo;
import com.ruoyi.workflow.service.IWorkOrderAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Compatibility endpoint for the legacy process category selector used by the model editor.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/processCategory")
public class ProcessCategoryCompatController {

    private static final String DEFAULT_APP_ID = "000000-event-management-app";
    private static final String DEFAULT_APP_NAME = "事件管理";
    private final IWorkOrderAppService workOrderAppService;
    @Value("${vls.tenant.id:000000}")
    private String singleTenantId = "000000";

    @GetMapping("/list")
    public TableDataInfo<Map<String, Object>> list(String appId) {
        return TableDataInfo.build(Collections.singletonList(buildFixedCategory(appId)));
    }

    @GetMapping("/ModelAndCategoryInfoList")
    public TableDataInfo<Map<String, Object>> modelAndCategoryInfoList(String appId) {
        return TableDataInfo.build(Collections.singletonList(buildFixedCategory(appId)));
    }

    @PostMapping
    public R<Void> add(@RequestBody(required = false) Map<String, Object> ignored) {
        return R.ok("单租户固定流程分类无需新增");
    }

    @PutMapping
    public R<Void> edit(@RequestBody(required = false) Map<String, Object> ignored) {
        return R.ok("单租户固定流程分类无需修改");
    }

    @PutMapping("/updateSortOrderBatch")
    public R<Void> updateSortOrderBatch(@RequestBody(required = false) Object ignored) {
        return R.ok("单租户固定流程分类无需排序");
    }

    @DeleteMapping("/{processCategoryIds}")
    public R<Void> remove(@PathVariable String processCategoryIds) {
        return R.ok("单租户固定流程分类无需删除");
    }

    private Map<String, Object> buildFixedCategory(String requestedAppId) {
        String effectiveAppId = StringUtils.isBlank(requestedAppId) ? DEFAULT_APP_ID : requestedAppId;
        WorkOrderAppVo app = queryApp(effectiveAppId);
        if (app == null && !DEFAULT_APP_ID.equals(effectiveAppId)) {
            app = queryApp(DEFAULT_APP_ID);
        }

        String appId = app != null && StringUtils.isNotBlank(app.getAppId()) ? app.getAppId() : effectiveAppId;
        String appName = app != null && StringUtils.isNotBlank(app.getApplicationName())
            ? app.getApplicationName()
            : DEFAULT_APP_NAME;

        Map<String, Object> row = new LinkedHashMap<String, Object>();
        row.put("processCategoryId", appId);
        row.put("processCategoryName", appName);
        row.put("appId", appId);
        row.put("categoryId", appId);
        row.put("categoryName", appName);
        row.put("id", appId);
        row.put("name", appName);
        row.put("parentId", "0");
        row.put("sortOrder", 0);
        row.put("sort", 0);
        row.put("tenantId", singleTenantId);
        row.put("children", Collections.emptyList());
        row.put("modelList", Collections.emptyList());
        return row;
    }

    private WorkOrderAppVo queryApp(String appId) {
        if (StringUtils.isBlank(appId)) {
            return null;
        }
        return workOrderAppService.queryById(appId);
    }
}
