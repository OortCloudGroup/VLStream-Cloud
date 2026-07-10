package com.ruoyi.web.controller.workflow;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.bean.BeanUtil;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.CopyGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.workflow.convert.ProcessModel;
import com.ruoyi.workflow.domain.WfCategory;
import com.ruoyi.workflow.domain.bo.InitBo;
import com.ruoyi.workflow.domain.bo.WfModelBo;
import com.ruoyi.workflow.domain.vo.WfCategoryVo;
import com.ruoyi.workflow.domain.vo.WfModelExportVo;
import com.ruoyi.workflow.domain.vo.WfModelVo;
import com.ruoyi.workflow.service.IReModeJsonService;
import com.ruoyi.workflow.service.IWfCategoryService;
import com.ruoyi.workflow.service.IWfModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.repository.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 工作流流程模型管理
 *
 * @author KonBAI
 * @createTime 2022/6/21 9:09
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/model")
public class WfModelController extends BaseController {

    private final IWfModelService modelService;
    private final IWfCategoryService categoryService;
    private final IReModeJsonService reModeJsonService;


    /**
     * 根据模型 ID 级联删除模型、流程图信息、所有模型版本、部署及其运行实例与历史数据
     */
    @DeleteMapping("/deleteModelCascade")
    @SaCheckPermission("workflow:model:deleteModelCascade")
    public R<Void> deleteModelCascade(String modelId,boolean isWorkOrder) {
        modelService.deleteModelCascade(modelId, isWorkOrder);
        return R.ok();
    }

    /**
     * 流程数据初始化
     */
    @PostMapping("/initStart")
    @SaCheckPermission("workflow:model:initStart")
    public R<String > initStart(@RequestBody InitBo initBo) {
        return R.ok(modelService.initStart(initBo));
    }

    /**
     * 事件管理流程数据初始化
     */
    @PostMapping("/eventManagementInitStart")
    @SaCheckPermission("workflow:model:eventManagementInitStart")
    public R<Boolean> eventManagementInitStart(@RequestBody InitBo initBo) {
        return R.ok(modelService.eventManagementInitStart(initBo));
    }

    /**
     * 展示需要初始化的信息
     */
    @PostMapping("/initShow")
    @SaCheckPermission("workflow:model:initShow")
    public R<List<List<String>>> initShow() {
        return R.ok(modelService.initShow());
    }

    /**
     * 查询流程模型列表
     *
     * @param modelBo   流程模型对象
     * @param pageQuery 分页参数
     */
    @SaCheckPermission("workflow:model:list")
    @GetMapping("/list")
    public TableDataInfo<WfModelVo> list(WfModelBo modelBo, PageQuery pageQuery) {
        return modelService.list(modelBo, pageQuery);
    }

    /**
     * 查询历史流程模型列表
     *
     * @param modelBo   流程模型对象
     * @param pageQuery 分页参数
     */
    @SaCheckPermission("workflow:model:historyList")
    @GetMapping("/historyList")
    public TableDataInfo<WfModelVo> historyList(WfModelBo modelBo, PageQuery pageQuery) {
        return modelService.historyList(modelBo, pageQuery);
    }

    /**
     * 获取流程模型详细信息
     *
     * @param modelId 模型主键
     */
    @SaCheckPermission("workflow:model:getInfo")
    @GetMapping(value = "/{modelId}")
    public R<WfModelVo> getInfo(@NotNull(message = "主键不能为空") @PathVariable("modelId") String modelId,
                                String applicationId) {
        return R.ok(modelService.getModel(modelId,applicationId));
    }

    /**
     * 获取流程表单详细信息
     *
     * @param modelId 模型主键
     */
    @SaCheckPermission("workflow:model:getBpmnXml")
    @GetMapping(value = "/bpmnXml/{modelId}")
    public R<String> getBpmnXml(@NotNull(message = "主键不能为空") @PathVariable("modelId") String modelId) {
        return R.ok("操作成功", modelService.queryBpmnXmlById(modelId));
    }

    /**
     * 新增流程模型
     */
    @SaCheckPermission("workflow:model:add")
    @Log(title = "流程模型", businessType = BusinessType.INSERT)
    @PostMapping
    public R<String > add(@Validated(AddGroup.class) @RequestBody WfModelBo modelBo) {
        return R.ok(modelService.insertModel(modelBo));
    }

    /**
     * 复制流程模型
     */
    @Log(title = "流程模型", businessType = BusinessType.COPY)
    @SaCheckPermission("workflow:model:copyModel")
    @PostMapping(value = "/copyModel")
    public R<Void> copyModel(@Validated(CopyGroup.class) @RequestBody WfModelBo modelBo) {
        modelService.copyModel(modelBo);
        return R.ok();
    }

    /**
     * 修改流程模型
     */
    @SaCheckPermission("workflow:model:edit")
    @Log(title = "流程模型", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody WfModelBo modelBo) {
        modelService.updateModel(modelBo);
        return R.ok();
    }


    /**
     * 保存流程模型
     */
    @SaCheckPermission("workflow:model:save")
//    @Log(title = "保存流程模型", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/save")
    public R<String> save(@RequestBody ProcessModel processModel, @RequestBody WfModelBo modelBo) {
        try {
            Model model = modelService.saveModel(modelBo, null,processModel);
            return R.ok(model.getId());
        } catch (Exception e) {
            // 处理异常
            return R.fail("保存失败：" + e.getMessage());
        }
    }


    /**
     * 设为最新流程模型
     *
     * @param modelId
     * @return
     */
    @SaCheckPermission("workflow:model:latest")
    @Log(title = "设为最新流程模型", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/latest")
    public R<?> latest(@RequestParam String modelId) {
        modelService.latestModel(modelId);
        return R.ok();
    }

    /**
     * 删除流程模型
     *
     * @param modelIds 流程模型主键串
     */
    @SaCheckPermission("workflow:model:remove")
    @Log(title = "删除流程模型", businessType = BusinessType.DELETE)
    @DeleteMapping()
    public R<String> remove( @NotEmpty(message = "主键不能为空") String[] modelIds) {
        modelService.deleteByIds(Arrays.asList(modelIds));
        return R.ok();
    }

    /**
     * 批量删除流程模型
     *
     * @param modelKey 流程模型key
     */
    @SaCheckPermission("workflow:model:batchRemove")
    @Log(title = "批量删除流程模型", businessType = BusinessType.DELETE)
    @DeleteMapping("/batchRemove")
    public R<String> batchRemove(@NotEmpty(message = "主键不能为空") String modelKey) {
        modelService.batchRemove(modelKey);
        return R.ok();
    }

    /**
     * 部署流程模型
     *
     * @param modelId 流程模型主键
     */
    @SaCheckPermission("workflow:model:deploy")
    @Log(title = "部署流程模型", businessType = BusinessType.INSERT)
    @RepeatSubmit()
    @PostMapping("/deploy")
    public R<Void> deployModel(@RequestParam String modelId) {
        return toAjax(modelService.deployModel(modelId));
    }

    /**
     * 导出流程模型数据
     */
    @Log(title = "导出流程模型数据", businessType = BusinessType.EXPORT)
    @SaCheckPermission("workflow:model:export")
    @PostMapping("/export")
    public void export(WfModelBo modelBo, HttpServletResponse response) {
        List<WfModelVo> list = modelService.list(modelBo);
        List<WfModelExportVo> listVo = BeanUtil.copyToList(list, WfModelExportVo.class);
        List<WfCategoryVo> categoryVos = categoryService.queryList(new WfCategory());
        Map<String, String> categoryMap = categoryVos.stream()
            .collect(Collectors.toMap(WfCategoryVo::getCode, WfCategoryVo::getCategoryName));
        for (WfModelExportVo exportVo : listVo) {
            exportVo.setCategoryName(categoryMap.get(exportVo.getCategory()));
        }
        ExcelUtil.exportExcel(listVo, "流程模型数据", WfModelExportVo.class, response);
    }
}
