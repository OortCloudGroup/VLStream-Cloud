/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.workflow.TX;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.workflow.domain.vo.WfDefAndDepVo;
import com.ruoyi.workflow.service.IWfProcessService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 提供给腾讯的流程接口
 *
 * @author KonBAI
 * @createTime 2022/3/7 22:07
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/workflow/tx")
public class WfTXController extends BaseController {

    private final IWfProcessService processService;

    /**
     * 根据流程标识，查询流程定义id和最新版的部署id
     */
    @GetMapping(value = "/getDefIdAndDepIdByProcKey/{processKey}")
    public R<WfDefAndDepVo> getDefIdAndDepIdByProcKey(@PathVariable String processKey, @RequestHeader("Authorization")String token) {
        WfDefAndDepVo wfDefAndDepVo = processService.getDefIdAndDepIdByProcKey(processKey,token);
        if(wfDefAndDepVo == null){
            return R.fail("未找到该流程的部署信息");
        }
        return R.ok(wfDefAndDepVo);
    }
}
