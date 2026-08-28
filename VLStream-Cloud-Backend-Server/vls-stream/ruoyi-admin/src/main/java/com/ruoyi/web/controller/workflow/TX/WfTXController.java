/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
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
 * workflowinterface
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
     * workflow , Query workflow definitionid and new id
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
