package com.ruoyi.workflow.service;

import cn.dev33.satoken.stp.StpUtil;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.workflow.domain.bo.WfModelBo;
import com.ruoyi.workflow.domain.bo.WorkOrderAppBo;
import com.ruoyi.workflow.domain.vo.WfModelVo;
import com.ruoyi.workflow.domain.vo.WorkOrderAppVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Read-only metadata shared by authorized event and work-order pages. */
@Service
@RequiredArgsConstructor
public class WorkOrderPageQueryService {
    private final IWorkOrderAppService apps;
    private final IWfModelService models;

    public List<WorkOrderAppVo> appOptions(WorkOrderAppBo request, PageQuery page) {
        requirePageAccess();
        WorkOrderAppBo query = new WorkOrderAppBo();
        // Never accept a secret as a lookup filter on the page-facing path.
        query.setApplicationName(request.getApplicationName());
        query.setApplicationId(request.getApplicationId());
        List<WorkOrderAppVo> result = new ArrayList<>();
        for (WorkOrderAppVo app : apps.queryPageList(query, page)) {
            WorkOrderAppVo option = new WorkOrderAppVo();
            option.setAppId(app.getAppId());
            option.setApplicationName(app.getApplicationName());
            option.setApplicationId(app.getApplicationId());
            option.setCategoryId(app.getCategoryId());
            result.add(option);
        }
        return result;
    }

    public TableDataInfo<WfModelVo> modelOptions(WfModelBo request, PageQuery page) {
        String tenant = requirePageAccess();
        String category = request.getWorkOrderCategory();
        if (StringUtils.isBlank(category)) {
            return TableDataInfo.build(Collections.emptyList());
        }
        boolean owned = apps.queryPageList(new WorkOrderAppBo(), new PageQuery()).stream()
            .anyMatch(app -> category.equals(app.getAppId()));
        if (!owned) {
            throw new ServiceException("工单分类不存在或无权访问", 403);
        }
        WfModelBo query = new WfModelBo();
        query.setWorkOrderCategory(category);
        query.setTenantId(tenant);
        query.setModelName(request.getModelName());
        query.setModelKey(request.getModelKey());
        // Discard application overrides, all-category flags and workflow-category filters.
        TableDataInfo<WfModelVo> result = models.list(query, page);
        List<WfModelVo> options = new ArrayList<>();
        if (result.getRows() != null) {
            for (WfModelVo model : result.getRows()) {
                if (!category.equals(model.getCategory())) {
                    throw new ServiceException("流程分类不一致，禁止访问", 403);
                }
                WfModelVo option = new WfModelVo();
                option.setModelId(model.getModelId());
                option.setModelName(model.getModelName());
                option.setModelKey(model.getModelKey());
                option.setCategory(model.getCategory());
                option.setVersion(model.getVersion());
                option.setDefinitionId(model.getDefinitionId());
                option.setDeploymentStatus(model.getDeploymentStatus());
                option.setSuspended(model.getSuspended());
                option.setWorkOrderAppAll(true);
                options.add(option);
            }
        }
        result.setRows(options);
        return result;
    }

    private String requirePageAccess() {
        StpUtil.checkPermissionOr("active-safety:events:list", "active-safety:workorder:my",
            "active-safety:workorder:pending", "active-safety:workorder:completed",
            "active-safety:workorder:claimable", "active-safety:settings:secure",
            "active-safety:settings:workorder", "workflow:workorder:selectPageOwnWorkOrderList",
            "workflow:workorder:selectPageTodoWorkOrderList", "workflow:workorder:selectPageFinishedWorkOrderList",
            "workflow:workorder:claimWorkOrderList");
        LoginUser user = LoginHelper.getLoginUser();
        if (user == null || StringUtils.isBlank(user.getTenantId())) {
            throw new ServiceException("缺少可信租户，禁止访问", 403);
        }
        return user.getTenantId();
    }
}
