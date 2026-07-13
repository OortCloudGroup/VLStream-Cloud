/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.workflow;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.interceptor.AuthorizationInterceptor;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.flowable.core.domain.ProcessQuery;
import com.ruoyi.system.service.ISysDictTypeService;
import com.ruoyi.workflow.domain.WfAttachment;
import com.ruoyi.workflow.domain.bo.ProcessStartBo;
import com.ruoyi.workflow.domain.bo.WfCopyBo;
import com.ruoyi.workflow.domain.vo.*;
import com.ruoyi.workflow.service.IWfCopyService;
import com.ruoyi.workflow.service.IWfProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流流程管理
 *
 * @author KonBAI
 * @createTime 2022/3/24 18:54
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/process")
@Validated
public class WfProcessController extends BaseController {

    private final IWfProcessService processService;
    private final IWfCopyService copyService;
    private final ISysDictTypeService dictTypeService;
    private final HistoryService historyService;

    /**
     * 查询可发起流程列表
     *
     * @param pageQuery 分页参数
     */
    @GetMapping(value = "/list")
    @SaCheckPermission("workflow:process:startProcessList")
    public TableDataInfo<WfDefinitionVo> startProcessList(ProcessQuery processQuery, PageQuery pageQuery) {
        return processService.selectPageStartProcessList(processQuery, pageQuery);
    }

    /**
     * 我拥有的流程
     */
    @SaCheckPermission("workflow:process:ownProcessList")
    @GetMapping(value = "/ownList")
    public TableDataInfo<Object> ownProcessList(ProcessQuery processQuery, PageQuery pageQuery) {
        return processService.selectPageOwnProcessList(processQuery, pageQuery);
    }

    /**
     * 查询所有流程
     */
    @SaCheckPermission("workflow:process:allProcessList")
    @GetMapping(value = "/allList")
    public TableDataInfo<WfTaskVo> allProcessList(ProcessQuery processQuery, PageQuery pageQuery,
            @RequestHeader("Authorization") String token) {
        return processService.selectPageAllProcessList(processQuery, pageQuery, token);
    }

    /**
     * 获取待办列表
     */
    @SaCheckPermission("workflow:process:todoProcessList")
    @GetMapping(value = "/todoList")
    public TableDataInfo<WfTaskVo> todoProcessList(ProcessQuery processQuery, PageQuery pageQuery) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        return processService.selectPageTodoProcessList(processQuery, pageQuery, sysUser);
    }

    /**
     * 获取待签列表
     *
     * @param processQuery 流程业务对象
     * @param pageQuery    分页参数
     */
    @SaCheckPermission("workflow:process:claimProcessList")
    @GetMapping(value = "/claimList")
    public TableDataInfo<WfTaskVo> claimProcessList(ProcessQuery processQuery, PageQuery pageQuery) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        return processService.selectPageClaimProcessList(processQuery, pageQuery, sysUser);
    }

    /**
     * 获取已办列表
     *
     * @param pageQuery 分页参数
     */
    @SaCheckPermission("workflow:process:finishedProcessList")
    @GetMapping(value = "/finishedList")
    public TableDataInfo<WfTaskVo> finishedProcessList(ProcessQuery processQuery, PageQuery pageQuery) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        return processService.selectPageFinishedProcessList(processQuery, pageQuery, sysUser);
    }

    /**
     * 获取抄送列表
     *
     * @param copyBo    流程抄送对象
     * @param pageQuery 分页参数
     */
    @SaCheckPermission("workflow:process:copyProcessList")
    @GetMapping(value = "/copyList")
    public TableDataInfo<WfCopyVo> copyProcessList(WfCopyBo copyBo, PageQuery pageQuery) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        copyBo.setUserId(getUserId());
        return copyService.selectPageList(copyBo, pageQuery, sysUser);
    }

    /**
     * 导出可发起流程列表
     */
    @SaCheckPermission("workflow:process:startExport")
    @Log(title = "导出可发起流程", businessType = BusinessType.EXPORT)
    @PostMapping("/startExport")
    public void startExport(@Validated ProcessQuery processQuery, HttpServletResponse response) {
        List<WfDefinitionVo> list = processService.selectStartProcessList(processQuery);
        ExcelUtil.exportExcel(list, "可发起流程", WfDefinitionVo.class, response);
    }

    /**
     * 导出我拥有流程列表
     */
    @SaCheckPermission("workflow:process:ownExport")
    @Log(title = "我拥有流程", businessType = BusinessType.EXPORT)
    @PostMapping("/ownExport")
    public void ownExport(@Validated ProcessQuery processQuery, HttpServletResponse response,
            @RequestHeader("Authorization") String token) {
        List<WfTaskVo> list = processService.selectOwnProcessList(processQuery, true, token);
        List<WfOwnTaskExportVo> listVo = BeanUtil.copyToList(list, WfOwnTaskExportVo.class);
        for (WfOwnTaskExportVo exportVo : listVo) {
            exportVo.setStatus(ObjectUtil.isNull(exportVo.getFinishTime()) ? "进行中" : "已完成");
        }
        ExcelUtil.exportExcel(listVo, "我拥有流程", WfOwnTaskExportVo.class, response);
    }

    /**
     * 导出所有流程列表
     */
    @SaCheckPermission("workflow:process:allExport")
    @Log(title = "所有流程", businessType = BusinessType.EXPORT)
    @PostMapping("/allExport")
    public void allExport(@Validated ProcessQuery processQuery, HttpServletResponse response,
            @RequestHeader("Authorization") String token) {
        List<WfTaskVo> list = processService.selectOwnProcessList(processQuery, false, token);
        List<WfOwnTaskExportVo> listVo = BeanUtil.copyToList(list, WfOwnTaskExportVo.class);
        for (WfOwnTaskExportVo exportVo : listVo) {
            exportVo.setStatus(ObjectUtil.isNull(exportVo.getFinishTime()) ? "进行中" : "已完成");
        }
        ExcelUtil.exportExcel(listVo, "我拥有流程", WfOwnTaskExportVo.class, response);
    }

    /**
     * 导出待办流程列表
     */
    @SaCheckPermission("workflow:process:todoExport")
    @Log(title = "待办流程", businessType = BusinessType.EXPORT)
    @PostMapping("/todoExport")
    public void todoExport(@Validated ProcessQuery processQuery, HttpServletResponse response,
            @RequestHeader("Authorization") String token) {
        List<WfTaskVo> list = processService.selectTodoProcessList(processQuery, token);
        List<WfTodoTaskExportVo> listVo = BeanUtil.copyToList(list, WfTodoTaskExportVo.class);
        ExcelUtil.exportExcel(listVo, "待办流程", WfTodoTaskExportVo.class, response);
    }

    /**
     * 导出待签流程列表
     */
    @SaCheckPermission("workflow:process:claimExport")
    @Log(title = "待签流程", businessType = BusinessType.EXPORT)
    @PostMapping("/claimExport")
    public void claimExport(@Validated ProcessQuery processQuery, HttpServletResponse response) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        List<WfTaskVo> list = processService.selectClaimProcessList(processQuery, sysUser);
        List<WfClaimTaskExportVo> listVo = BeanUtil.copyToList(list, WfClaimTaskExportVo.class);
        ExcelUtil.exportExcel(listVo, "待签流程", WfClaimTaskExportVo.class, response);
    }

    /**
     * 导出已办流程列表
     */
    @SaCheckPermission("workflow:process:finishedExport")
    @Log(title = "已办流程", businessType = BusinessType.EXPORT)
    @PostMapping("/finishedExport")
    public void finishedExport(@Validated ProcessQuery processQuery, HttpServletResponse response,
            @RequestHeader("Authorization") String token) {
        List<WfTaskVo> list = processService.selectFinishedProcessList(processQuery, token);
        List<WfFinishedTaskExportVo> listVo = BeanUtil.copyToList(list, WfFinishedTaskExportVo.class);
        ExcelUtil.exportExcel(listVo, "已办流程", WfFinishedTaskExportVo.class, response);
    }

    /**
     * 导出抄送流程列表
     */
    @SaCheckPermission("workflow:process:copyExport")
    @Log(title = "抄送流程", businessType = BusinessType.EXPORT)
    @PostMapping("/copyExport")
    public void copyExport(WfCopyBo copyBo, HttpServletResponse response,
            @RequestHeader("Authorization") String token) {
        copyBo.setUserId(getUserId());
        List<WfCopyVo> list = copyService.selectList(copyBo);
        ExcelUtil.exportExcel(list, "抄送流程", WfCopyVo.class, response);
    }

    /**
     * 查询流程部署关联表单信息
     *
     * @param definitionId 流程定义id
     * @param deployId     流程部署id
     */
    @GetMapping("/getProcessForm")
    @SaCheckPermission("workflow:process:getForm")
    public R<?> getForm(@RequestParam(value = "definitionId") String definitionId,
            @RequestParam(value = "deployId") String deployId,
            @RequestParam(value = "procInsId", required = false) String procInsId) {
        return R.ok(processService.selectFormContent(definitionId, deployId, procInsId));
    }

    /**
     * 根据流程定义id启动流程实例
     */
    @SaCheckPermission("workflow:process:start")
    @PostMapping("/start")
    public R<Map<String, Object>> start(@RequestBody ProcessStartBo processStartBo) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        String processInstanceId = processService.startProcessByDefId(processStartBo, sysUser);
        Map<String, Object> result = new HashMap<>();
        result.put("ProcessInstanceId", processInstanceId);
        result.put("TaskId", processService.getTaskId(processInstanceId, sysUser));
        result.put("workOrder", processStartBo.getWorkOrder());
        return R.ok("流程启动成功", result);
    }

    /**
     * 删除流程实例
     *
     * @param instanceIds 流程实例ID串
     */
    @DeleteMapping("/instance/{instanceIds}")
    @SaCheckPermission("workflow:process:delete")
    public R<Void> delete(@PathVariable String[] instanceIds) {
        processService.deleteProcessByIds(instanceIds);
        return R.ok();
    }

    /**
     * 读取xml文件
     *
     * @param processDefId 流程定义ID
     */
    @SaCheckPermission("workflow:process:getBpmnXml")
    @GetMapping("/bpmnXml/{processDefId}")
    public R<String> getBpmnXml(@PathVariable(value = "processDefId") String processDefId) {
        return R.ok(null, processService.queryBpmnXmlById(processDefId));
    }

    /**
     * 返回流程json
     *
     * @param processDefId 流程定义ID
     */
    @SaCheckPermission("workflow:process:getBpmnJson")
    @GetMapping("/bpmnJson/{processDefId}")
    public R<String> getBpmnJson(@PathVariable(value = "processDefId") String processDefId) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        return R.ok(null, processService.queryBpmnJsonById(processDefId, sysUser));
    }

    /**
     * 查询流程详情信息
     *
     * @param procInsId          流程实例ID
     * @param taskId             任务ID
     * @param includeApproverIds 是否需要返回所有审批人ID列表
     */
    @SaCheckPermission("workflow:process:detail")
    @GetMapping("/detail")
    public R detail(String procInsId, String taskId,
            @RequestParam(value = "includeApproverIds", required = false, defaultValue = "false") Boolean includeApproverIds) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        return R.ok(processService.queryProcessDetail(procInsId, taskId, sysUser, includeApproverIds));
    }

    /**
     * 根据流程模板id获取所有用户节点相关联的用户信息
     *
     * @param processDefKey 流程定义ID
     */
    @SaCheckPermission("workflow:process:getAllUserInfo")
    @GetMapping("/getAllUserInfo/{processDefKey}")
    public R getAllUserInfo(@PathVariable(value = "processDefKey") String processDefKey) {
        return R.ok(processService.getAllUserInfo(processDefKey));
    }

    /**
     * 获取流程所有状态值
     */
    @SaCheckPermission("workflow:process:getAllProcessStatus")
    @GetMapping("/getAllProcessStatus")
    public R getAllProcessStatus() {
        List<SysDictData> data = dictTypeService.selectDictDataByType("wf_process_status");
        if (ObjectUtil.isNull(data)) {
            data = new ArrayList<>();
        }
        return R.ok(data);
    }

    // /**
    // * 获取当前流程所有待办候选人或者候选组
    // */
    // @GetMapping("/getAllCandidate")
    // public R getAllCandidate(String procInsId) {
    // processService.getAllCandidate(String procInsId);
    // return R.ok(data);
    // }

    /**
     * 根据流程实例id获取附件pdf信息
     */
    @SaCheckPermission("workflow:process:getPDF")
    @PostMapping(value = "/getPDF/{procInstId}")
    public R<List<WfAttachment>> getPDF(@PathVariable String procInstId) {
        return R.ok(processService.getPDF(procInstId));
    }

    /**
     * 根据流程实例id获取最新的已完成的用户节点信息
     */
    @SaCheckPermission("workflow:process:getLatestHisProInsInfo")
    @GetMapping(value = "/getLatestHisProInsInfo/{procInstId}")
    public R<WfLastHisTaskInfoVo> getLatestHisProInsInfo(@PathVariable String procInstId) {
        return R.ok(processService.getLatestHisTaskInfo(procInstId));
    }

    /**
     * 获取历史任务信息列表
     *
     * @param historicProcIns
     * @return
     */
    @SaCheckPermission("workflow:process:historyProcNodeList")
    @GetMapping(value = "/historyProcNodeList")
    public R<List<WfProcNodeVo>> historyProcNodeList(String historicProcIns) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        return R.ok(processService.historyProcNodeList(historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(historicProcIns).processInstanceTenantId(sysUser.getTenantId()).singleResult(),
                sysUser));
    }

}
