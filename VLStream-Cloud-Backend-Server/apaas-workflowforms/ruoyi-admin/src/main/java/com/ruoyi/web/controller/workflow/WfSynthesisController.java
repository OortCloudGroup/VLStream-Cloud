package com.ruoyi.web.controller.workflow;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.workflow.domain.bo.WfSynthesisBo;
import com.ruoyi.workflow.domain.vo.WfSynthesisVo;
import com.ruoyi.workflow.service.IWfSynthesisService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;

/**
 * 综合通用流程
 *
 * @author 雷超群
 * @date 2025-01-04
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/wf/synthesis")
public class WfSynthesisController extends BaseController {

    private final IWfSynthesisService iWfSynthesisService;

    /**
     * 查询综合通用流程列表
     */
    @SaCheckPermission("wf:synthesis:list")
    @GetMapping("/list")
    public R<List<WfSynthesisVo>> list(WfSynthesisBo bo) {
        List<WfSynthesisVo> list = iWfSynthesisService.queryList(bo);
        return R.ok(list);
    }

    /**
     * 查询全部综合通用流程
     */
    @SaCheckPermission("wf:synthesis:queryListAll")
    @GetMapping("/listAll")
    R<List<WfSynthesisVo>> queryListAll(String categoryName) {
        return R.ok(iWfSynthesisService.queryListAll(categoryName));
    }

    /**
     * 导出综合通用流程列表
     */
    @SaCheckPermission("wf:synthesis:export")
    @Log(title = "综合通用流程", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(WfSynthesisBo bo, HttpServletResponse response) {
        List<WfSynthesisVo> list = iWfSynthesisService.queryList(bo);
        ExcelUtil.exportExcel(list, "综合通用流程", WfSynthesisVo.class, response);
    }

    /**
     * 获取综合通用流程详细信息
     *
     * @param synthesisId 主键
     */
    @SaCheckPermission("wf:synthesis:getInfo")
    @GetMapping("/{synthesisId}")
    public R<WfSynthesisVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable String synthesisId) {
        return R.ok(iWfSynthesisService.queryById(synthesisId));
    }

    /**
     * 新增综合通用流程
     */
    @SaCheckPermission("wf:synthesis:add")
    @Log(title = "综合通用流程", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody WfSynthesisBo bo) {
        return toAjax(iWfSynthesisService.insertByBo(bo));
    }

    /**
     * 修改综合通用流程
     */
    @SaCheckPermission("wf:synthesis:edit")
    @Log(title = "综合通用流程", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody WfSynthesisBo bo) {
        return toAjax(iWfSynthesisService.updateByBo(bo));
    }

    /**
     * 删除综合通用流程
     *
     * @param synthesisIds 主键串
     */
    @SaCheckPermission("wf:synthesis:remove")
    @Log(title = "综合通用流程", businessType = BusinessType.DELETE)
    @DeleteMapping("/{synthesisIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空") @PathVariable String[] synthesisIds) {
        return toAjax(iWfSynthesisService.deleteWithValidByIds(Arrays.asList(synthesisIds), true));
    }
}
