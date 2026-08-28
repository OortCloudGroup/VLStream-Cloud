/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.demo.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.core.validate.QueryGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.demo.domain.bo.TestTreeBo;
import com.ruoyi.demo.domain.vo.TestTreeVo;
import com.ruoyi.demo.service.ITestTreeService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;

/**
 * Controller
 *
 * @author Lion Li
 * @date 2021-07-26
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/tree")
public class TestTreeController extends BaseController {

    private final ITestTreeService iTestTreeService;

    /**
     * Query list
     */
    @SaCheckPermission("demo:tree:list")
    @GetMapping("/list")
    public R<List<TestTreeVo>> list(@Validated(QueryGroup.class) TestTreeBo bo) {
        List<TestTreeVo> list = iTestTreeService.queryList(bo);
        return R.ok(list);
    }

    /**
     * Export
     */
    @SaCheckPermission("demo:tree:export")
    @Log(title = "测试树表", businessType = BusinessType.EXPORT)
    @GetMapping("/export")
    public void export(@Validated TestTreeBo bo, HttpServletResponse response) {
        List<TestTreeVo> list = iTestTreeService.queryList(bo);
        ExcelUtil.exportExcel(list, "测试树表", TestTreeVo.class, response);
    }

    /**
     * Get info
     *
     * @param id ID
     */
    @SaCheckPermission("demo:tree:query")
    @GetMapping("/{id}")
    public R<TestTreeVo> getInfo(@NotNull(message = "主键不能为空")
                                 @PathVariable("id") Long id) {
        return R.ok(iTestTreeService.queryById(id));
    }

    /**
     * Add
     */
    @SaCheckPermission("demo:tree:add")
    @Log(title = "测试树表", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody TestTreeBo bo) {
        return toAjax(iTestTreeService.insertByBo(bo));
    }

    /**
     * Update
     */
    @SaCheckPermission("demo:tree:edit")
    @Log(title = "测试树表", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody TestTreeBo bo) {
        return toAjax(iTestTreeService.updateByBo(bo));
    }

    /**
     * Delete
     *
     * @param ids ID
     */
    @SaCheckPermission("demo:tree:remove")
    @Log(title = "测试树表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable Long[] ids) {
        return toAjax(iTestTreeService.deleteWithValidByIds(Arrays.asList(ids), true));
    }
}
