/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.workflow;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.workflow.domain.bo.ReModeJsonBo;
import com.ruoyi.workflow.domain.vo.ReModelJsonVo;
import com.ruoyi.workflow.service.IReModeJsonService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
/**
 * 流程图JSON
 *
 * @author 雷超群
 * @date 2024-11-02
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/work/modeJson")
public class ReModeJsonController extends BaseController {

    private final IReModeJsonService iReModeJsonService;

    /**
     * 查询流程图JSON列表
     */
    @SaCheckPermission("work:modeJson:list")
    @GetMapping("/list")
    public TableDataInfo<ReModelJsonVo> list(ReModeJsonBo bo, PageQuery pageQuery) {
        return iReModeJsonService.queryPageList(bo, pageQuery);
    }

    /**
     * 导出流程图JSON列表
     */
    @SaCheckPermission("work:modeJson:export")
    @Log(title = "流程图JSON", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(ReModeJsonBo bo, HttpServletResponse response) {
        List<ReModelJsonVo> list = iReModeJsonService.queryList(bo);
        ExcelUtil.exportExcel(list, "流程图JSON", ReModelJsonVo.class, response);
    }

    /**
     * 获取流程图JSON详细信息
     *
     * @param modelId 主键
     */
    @SaCheckPermission("work:modeJson:getInfo")
    @GetMapping("/{modelId}")
    public R<ReModelJsonVo> getInfo(@NotNull(message = "主键不能为空")
                                   @PathVariable String modelId) {
        return R.ok(iReModeJsonService.queryById(modelId));
    }

    /**
     * 新增流程图JSON
     */
    @SaCheckPermission("work:modeJson:add")
    @Log(title = "流程图JSON", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping()
    public R<Void> add(@Validated(AddGroup.class) @RequestBody ReModeJsonBo bo) {
        return toAjax(iReModeJsonService.insertByBo(bo));
    }

    /**
     * 修改流程图JSON
     */
    @SaCheckPermission("work:modeJson:edit")
    @Log(title = "流程图JSON", businessType = BusinessType.UPDATE)
    @RepeatSubmit()
    @PutMapping()
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody ReModeJsonBo bo) {
        return toAjax(iReModeJsonService.updateByBo(bo));
    }

    /**
     * 删除流程图JSON
     *
     * @param modelIds 主键串
     */
    @SaCheckPermission("work:modeJson:remove")
    @Log(title = "流程图JSON", businessType = BusinessType.DELETE)
    @DeleteMapping("/{modelIds}")
    public R<Void> remove(@NotEmpty(message = "主键不能为空")
                          @PathVariable String[] modelIds) {
        return toAjax(iReModeJsonService.deleteWithValidByIds(Arrays.asList(modelIds), true));
    }
}
