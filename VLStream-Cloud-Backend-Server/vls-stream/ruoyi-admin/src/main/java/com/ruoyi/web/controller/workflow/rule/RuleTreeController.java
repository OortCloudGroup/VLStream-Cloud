/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.workflow.rule;


import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.rule.bo.RuleTreeBo;
import com.ruoyi.rule.service.IRuleListService;
import com.ruoyi.rule.service.IRuleTreeService;
import com.ruoyi.rule.vo.RuleTreeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;

/**
 * 规则树
 *
 * @author 雷超群
 * @date 2024-12-18
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/rule/tree")
public class RuleTreeController extends BaseController {

    private final IRuleTreeService iRuleTreeService;
    @Resource()
    private IRuleListService ruleListService;

    /**
     * 查询规则树列表
     */
    @SaCheckPermission("rule:tree:list")
    @GetMapping("/list")
    public R<List<RuleTreeVo>> list(RuleTreeBo bo) {
        List<RuleTreeVo> list = iRuleTreeService.queryList(bo);
        return R.ok(list);
    }

    /**
     * 导出规则树列表
     */
    @SaCheckPermission("rule:tree:export")
    @Log(title = "规则树", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(RuleTreeBo bo, HttpServletResponse response) {
        List<RuleTreeVo> list = iRuleTreeService.queryList(bo);
        ExcelUtil.exportExcel(list, "规则树", RuleTreeVo.class, response);
    }

    /**
     * 获取规则树详细信息
     *
     * @param id 主键
     */
    @SaCheckPermission("rule:tree:getInfo")
    @GetMapping("/{id}")
    public R<RuleTreeVo> getInfo(@NotNull(message = "主键不能为空")
                                 @PathVariable String id) {
        return R.ok(iRuleTreeService.queryById(id));
    }

    /**
     * 新增规则树
     */
    @SaCheckPermission("rule:tree:add")
    @Log(title = "规则树", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody RuleTreeBo bo) {
        return toAjax(iRuleTreeService.insertByBo(bo));
    }

    /**
     * 修改规则树
     */
    @SaCheckPermission("rule:tree:edit")
    @Log(title = "规则树", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody RuleTreeBo bo) {
        return toAjax(iRuleTreeService.updateByBo(bo));
    }

    /**
     * 删除规则树
     *
     * @param ids 主键串
     */
    @SaCheckPermission("rule:tree:remove")
    @Log(title = "规则树", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable String[] ids, @RequestParam(name = "treeDelFlag", defaultValue = "0") String treeDelFlag) {
        if (treeDelFlag.equals("1")) {
            ruleListService.selectByTreeId(Arrays.asList(ids).toString());
        }
        return toAjax(
            iRuleTreeService.deleteWithValidByIds(Arrays.asList(ids), true)
        );
    }
}
