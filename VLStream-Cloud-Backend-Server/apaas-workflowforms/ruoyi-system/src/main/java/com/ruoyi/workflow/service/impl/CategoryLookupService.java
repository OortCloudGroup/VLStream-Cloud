package com.ruoyi.workflow.service.impl;

import com.ruoyi.workflow.domain.WfApp;
import com.ruoyi.workflow.domain.WfSynthesis;
import com.ruoyi.workflow.domain.WorkOrderApp;
import com.ruoyi.workflow.domain.WorkOrderSynthesis;
import com.ruoyi.workflow.service.IWfAppService;
import com.ruoyi.workflow.service.IWfSynthesisService;
import com.ruoyi.workflow.service.IWorkOrderAppService;
import com.ruoyi.workflow.service.IWorkOrderSynthesisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CategoryLookupService {
    private final IWfAppService wfAppService;
    private final IWfSynthesisService wfSynthesisService;
    private final IWorkOrderAppService workOrderAppService;
    private final IWorkOrderSynthesisService workOrderSynthesisService;

    public String queryCategoryName(String categoryId, String categoryType) {
        switch (categoryType) {
            case "wfAppAll":
                return wfAppService.list().stream().filter(item -> item.getAppId().equals(categoryId)).map(WfApp::getApplicationName).findFirst().orElse(null);
            case "wfSynthesisAll":
                return wfSynthesisService.list().stream().filter(item -> item.getSynthesisId().equals(categoryId)).map(WfSynthesis::getCategoryName).findFirst().orElse(null);
            case "WorkOrderAppAll":
                return workOrderAppService.list().stream().filter(item -> item.getAppId().equals(categoryId)).map(WorkOrderApp::getApplicationName).findFirst().orElse(null);
            case "WorkOrderSynthesisAll":
                return workOrderSynthesisService.list().stream().filter(item -> item.getSynthesisId().equals(categoryId)).map(WorkOrderSynthesis::getCategoryName).findFirst().orElse(null);
            default:
               throw new RuntimeException("未知的分类类型");
        }
    }
}
