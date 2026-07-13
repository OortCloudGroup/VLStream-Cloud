/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.listener;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.flowable.core.domain.ProcessQuery;
import com.ruoyi.flowable.factory.FlowServiceFactory;
import com.ruoyi.workflow.domain.bo.WfModelBo;
import com.ruoyi.workflow.domain.vo.WfDeployVo;
import com.ruoyi.workflow.domain.vo.WfModelVo;
import com.ruoyi.workflow.event.ValidationEvent;
import com.ruoyi.workflow.service.IWfDeployService;
import com.ruoyi.workflow.service.IWfModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ValidationEventListener extends FlowServiceFactory {

    private final IWfModelService wfModelService;
    private final IWfDeployService wfDeployService;

    @EventListener
    public void handleValidationEvent(ValidationEvent event) {
        Collection<String> ids = event.getIds();
        for (String id : ids) {
            WfModelBo wfModelBo = new WfModelBo();
            wfModelBo.setWfCategory(id);
            List<WfModelVo> list = wfModelService.list(wfModelBo);
            if (list.size() > 0) {
                throw new RuntimeException("该流程分类下存在流程模型，无法删除");
            }
            ProcessQuery processQuery = new ProcessQuery();
            processQuery.setCategory(id);
            PageQuery pageQuery = new PageQuery();
            pageQuery.setPageNum(1);
            pageQuery.setPageSize(99);
            TableDataInfo<WfDeployVo> wfDeployVoTableDataInfo = wfDeployService.queryPageList(processQuery,pageQuery);
            if (wfDeployVoTableDataInfo.getTotal() > 0) {
                throw new RuntimeException("该流程分类下存在已部署模型，无法删除");
            }
        }
    }
}
