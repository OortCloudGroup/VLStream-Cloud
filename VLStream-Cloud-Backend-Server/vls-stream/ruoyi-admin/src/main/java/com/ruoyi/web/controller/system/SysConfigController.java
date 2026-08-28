/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.web.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.service.ISysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * parameterconfiguration infooperationProcess
 *
 * @author Lion Li
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/system/config")
public class SysConfigController extends BaseController {

    private final ISysConfigService configService;

    /**
     * Get parameterconfiguration
     */
    @SaCheckPermission("system:config:list")
    @GetMapping("/list")
    public TableDataInfo<SysConfig> list(SysConfig config, PageQuery pageQuery) {
        return configService.selectPageConfigList(config, pageQuery);
    }

    /**
     * Export parameterconfiguration
     */
    @Log(title = "参数管理", businessType = BusinessType.EXPORT)
    @SaCheckPermission("system:config:export")
    @PostMapping("/export")
    public void export(SysConfig config, HttpServletResponse response) {
        List<SysConfig> list = configService.selectConfigList(config);
        ExcelUtil.exportExcel(list, "参数数据", SysConfig.class, response);
    }

    /**
     * parameter Get info
     *
     * @param configId parameterID
     */
    @SaCheckPermission("system:config:query")
    @GetMapping(value = "/{configId}")
    public R<SysConfig> getInfo(@PathVariable Long configId) {
        return R.ok(configService.selectConfigById(configId));
    }

    /**
     * parameter keyQuery parameter value
     *
     * @param configKey parameterKey
     */
    @GetMapping(value = "/configKey/{configKey}")
    public R<Void> getConfigKey(@PathVariable String configKey) {
        return R.ok(configService.selectConfigByKey(configKey));
    }

    /**
     * Add parameterconfiguration
     */
    @SaCheckPermission("system:config:add")
    @Log(title = "参数管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated @RequestBody SysConfig config) {
        if (!configService.checkConfigKeyUnique(config)) {
            return R.fail("新增参数'" + config.getConfigName() + "'失败，参数键名已存在");
        }
        configService.insertConfig(config);
        return R.ok();
    }

    /**
     * Update parameterconfiguration
     */
    @SaCheckPermission("system:config:edit")
    @Log(title = "参数管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated @RequestBody SysConfig config) {
        if (!configService.checkConfigKeyUnique(config)) {
            return R.fail("修改参数'" + config.getConfigName() + "'失败，参数键名已存在");
        }
        configService.updateConfig(config);
        return R.ok();
    }

    /**
     * parameter keyUpdate parameterconfiguration
     */
    @SaCheckPermission("system:config:edit")
    @Log(title = "参数管理", businessType = BusinessType.UPDATE)
    @PutMapping("/updateByKey")
    public R<Void> updateByKey(@RequestBody SysConfig config) {
        configService.updateConfig(config);
        return R.ok();
    }

    /**
     * Delete parameterconfiguration
     *
     * @param configIds parameterID
     */
    @SaCheckPermission("system:config:remove")
    @Log(title = "参数管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{configIds}")
    public R<Void> remove(@PathVariable Long[] configIds) {
        configService.deleteConfigByIds(configIds);
        return R.ok();
    }

    /**
     * new parameter
     */
    @SaCheckPermission("system:config:remove")
    @Log(title = "参数管理", businessType = BusinessType.CLEAN)
    @DeleteMapping("/refreshCache")
    public R<Void> refreshCache() {
        configService.resetConfigCache();
        return R.ok();
    }
}
