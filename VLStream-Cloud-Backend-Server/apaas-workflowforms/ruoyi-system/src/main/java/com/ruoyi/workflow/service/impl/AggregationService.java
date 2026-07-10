package com.ruoyi.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.workflow.domain.WfApp;
import com.ruoyi.workflow.domain.WfFormApp;
import com.ruoyi.workflow.domain.WorkOrderApp;
import com.ruoyi.workflow.service.IAggregationService;
import com.ruoyi.workflow.service.IWfAppService;
import com.ruoyi.workflow.service.IWfFormAppService;
import com.ruoyi.workflow.service.IWorkOrderAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AggregationService implements IAggregationService {
    private final IWfFormAppService formAppService;
    private final IWfAppService appService;
    private final IWorkOrderAppService workOrderAppService;

    @Override
    public Map<String, Object> getFormAndAppId(String applicationId, String formType) {
        Map<String, Object> map = null;
        try {
            map = new HashMap<>();
            if (formType.equals("0")) {//流程
                map.put("WfApp", appService.
                    getOne(new LambdaQueryWrapper<>(WfApp.class).
                        eq(WfApp::getApplicationId, applicationId)));
            } else {
                map.put("WorkOrderApp", workOrderAppService.
                    getOne(new LambdaQueryWrapper<>(WorkOrderApp.class).
                        eq(WorkOrderApp::getApplicationId, applicationId)));
            }
            LambdaQueryWrapper<WfFormApp> lambdaQueryWrapper = new LambdaQueryWrapper();
            lambdaQueryWrapper.eq(WfFormApp::getApplicationId, applicationId);
            lambdaQueryWrapper.eq(WfFormApp::getType, formType);
            map.put("categoryId", formAppService.getOne(lambdaQueryWrapper));
        } catch (Exception e) {
            throw new RuntimeException("获取表单或流程应用异常，请检测是否创建对应应用");
        }
        return map;
    }


}
