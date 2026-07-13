package com.ruoyi.web.controller.workflow;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.interceptor.AuthorizationInterceptor;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.system.service.impl.SysDictTypeServiceImpl;
import com.ruoyi.workflow.domain.bo.WfSavePdfBo;
import com.ruoyi.workflow.domain.bo.WfTaskBo;
import com.ruoyi.workflow.service.IWfProcessService;
import com.ruoyi.workflow.service.IWfTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * 工作流任务管理
 *
 * @author KonBAI
 * @createTime 2022/3/10 00:12
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/task")
public class WfTaskController {

    private final IWfTaskService flowTaskService;
    private final SysDictTypeServiceImpl sysDictTypeService;
    private final IWfProcessService processService;

    /**
     * 取消流程
     */
    @PostMapping(value = "/stopProcess")
    @SaCheckPermission("workflow:process:stopProcess")
    public R stopProcess(@RequestBody WfTaskBo bo) {
        flowTaskService.stopProcess(bo);
        return R.ok();
    }

    /**
     * 撤回流程
     */
    @PostMapping(value = "/revokeProcess")
    @SaCheckPermission("workflow:process:revokeProcess")
    public R revokeProcess(@RequestBody WfTaskBo bo) {
        flowTaskService.revokeProcess(bo);
        return R.ok();
    }

    /**
     * 获取流程变量
     *
     * @param taskId 流程任务Id
     */
    @GetMapping(value = "/processVariables/{taskId}")
    @SaCheckPermission("workflow:process:processVariables")
    public R processVariables(@PathVariable(value = "taskId") String taskId) {
        return R.ok(flowTaskService.getProcessVariables(taskId));
    }

    /**
     * 审批任务
     */
    @PostMapping(value = "/complete")
    @SaCheckPermission("workflow:process:complete")
    public R<String> complete(@RequestBody WfTaskBo bo) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        flowTaskService.complete(bo);
        String taskId = processService.getTaskId(bo.getProcInsId(), sysUser);
        return R.ok("操作成功", "TaskId:" + taskId);
    }

    /**
     * 拒绝任务
     */
    @PostMapping(value = "/reject")
    @SaCheckPermission("workflow:process:taskReject")
    public R taskReject(@RequestBody WfTaskBo taskBo) {
        SysUser user = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        flowTaskService.taskReject(taskBo, user);
        return R.ok();
    }

    /**
     * 退回任务
     */
    @PostMapping(value = "/return")
    @SaCheckPermission("workflow:process:taskReturn")
    public R taskReturn(@RequestBody WfTaskBo bo) {
        SysUser sysUser = RedisUtils.getCacheObject(AuthorizationInterceptor.getToken());
        flowTaskService.taskReturn(bo, sysUser);
        return R.ok();
    }

    /**
     * 获取所有可回退的节点
     */
    @PostMapping(value = "/returnList")
    @SaCheckPermission("workflow:process:findReturnTaskList")
    public R findReturnTaskList(@RequestBody WfTaskBo bo) {
        return R.ok(flowTaskService.findReturnTaskList(bo));
    }

    /**
     * 删除任务
     */
    @DeleteMapping(value = "/delete")
    @SaCheckPermission("workflow:process:delete")
    public R delete(@RequestBody WfTaskBo bo) {
        flowTaskService.deleteTask(bo);
        return R.ok();
    }

    /**
     * 认领/签收任务
     */
    @PostMapping(value = "/claim")
    @SaCheckPermission("workflow:process:claim")
    public R claim(@RequestBody WfTaskBo bo) {
        flowTaskService.claim(bo);
        return R.ok();
    }

    /**
     * 取消认领/签收任务
     */
    @PostMapping(value = "/unClaim")
    @SaCheckPermission("workflow:process:unClaim")
    public R unClaim(@RequestBody WfTaskBo bo) {
        flowTaskService.unClaim(bo);
        return R.ok();
    }

    /**
     * 委派任务
     */
    @PostMapping(value = "/delegate")
    @SaCheckPermission("workflow:process:delegate")
    public R delegate(@RequestBody WfTaskBo bo) {
        if (ObjectUtil.hasNull(bo.getTaskId(), bo.getUserId())) {
            return R.fail("参数错误！");
        }
        flowTaskService.delegateTask(bo);
        return R.ok();
    }

    /**
     * 转办任务
     */
    @PostMapping(value = "/transfer")
    @SaCheckPermission("workflow:process:transfer")
    public R transfer(@RequestBody WfTaskBo bo) {
        if (ObjectUtil.hasNull(bo.getTaskId(), bo.getUserId())) {
            return R.fail("参数错误！");
        }
        flowTaskService.transferTask(bo);
        return R.ok();
    }

    /**
     * 生成流程图
     *
     * @param processId 任务ID
     */

    @GetMapping("/diagram/{processId}")
    @SaCheckPermission("workflow:process:genProcessDiagram")
    public void genProcessDiagram(HttpServletResponse response, @PathVariable("processId") String processId
    ) {
        InputStream inputStream = flowTaskService.diagram(processId);
        OutputStream os = null;
        BufferedImage image = null;
        try {
            image = ImageIO.read(inputStream);
            response.setContentType("image/png");
            os = response.getOutputStream();
            if (image != null) {
                ImageIO.write(image, "png", os);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null) {
                    os.flush();
                    os.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 页面上传pdf，保存pdf链接
     */
    @PostMapping(value = "/savePdf")
    @SaCheckPermission("workflow:process:savePdf")
    public R savePDF(@RequestBody WfSavePdfBo bo) {
        flowTaskService.savePDF(bo);
        return R.ok();
    }

    /**
     * 获取流程最新节点的审批人信息
     */
    @SaCheckPermission("workflow:process:getApproverIds")
    @GetMapping(value = "/getApproverIds/{procInstId}")
    public R<List<String>> getApproverIds(@PathVariable String procInstId) {
        return R.ok(flowTaskService.getApproverIds(procInstId));
    }

    /**
     * 多实例加签接口
     */
    @SaCheckPermission("workflow:process:addSignTask")
    @PostMapping("/addSignTask")
    public R<Void> addSignTask(@RequestBody WfTaskBo bo) {
        flowTaskService.addSignTask(bo);
        return R.ok();
    }

    /**
     * 多实例减签接口
     */
    @SaCheckPermission("workflow:process:subSignTask")
    @PostMapping("/subSignTask")
    public R<Void> subSignTask(@RequestBody WfTaskBo bo) {
        flowTaskService.subSignTask(bo);
        return R.ok();
    }

}
