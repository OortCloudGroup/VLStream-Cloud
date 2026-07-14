/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.workflow.domain.WfApp;
import com.ruoyi.workflow.domain.WfFormApp;
import com.ruoyi.workflow.domain.WorkOrderApp;
import com.ruoyi.workflow.domain.bo.WorkOrderAppBo;
import com.ruoyi.workflow.domain.vo.WorkOrderAppVo;
import com.ruoyi.workflow.mapper.WfFormAppMapper;
import com.ruoyi.workflow.mapper.WfAppMapper;
import com.ruoyi.workflow.mapper.WorkOrderAppMapper;
import com.ruoyi.workflow.service.IWorkOrderAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ruoyi.workflow.service.impl.WfAppServiceImpl.generateUuid;

/**
 * 应用工单分类Service业务层处理
 *
 * @author 雷超群
 * @date 2025-01-04
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class WorkOrderAppServiceImpl extends ServiceImpl<WorkOrderAppMapper, WorkOrderApp> implements IWorkOrderAppService {

    private static final String WORK_ORDER_FORM_TYPE = "1";

    private final WorkOrderAppMapper baseMapper;
    private final ValidateService validateService;
    private final WfAppMapper wfAppMapper;
    private final WfFormAppMapper wfFormAppMapper;


    /**
     * 查询应用工单分类
     */
    @Override
    public WorkOrderAppVo queryById(String appId) {
        WorkOrderAppVo workOrderAppVo = baseMapper.selectVoById(appId);
        attachFormCategory(workOrderAppVo);
        return workOrderAppVo;
    }

    /**
     * 查询应用工单分类列表
     */
    @Override
    public List<WorkOrderAppVo> queryPageList(WorkOrderAppBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<WorkOrderApp> lqw = buildQueryWrapper(bo);
        List<WorkOrderAppVo> workOrderAppVos = baseMapper.selectVoList(lqw);
        attachFormCategories(workOrderAppVos);
        return workOrderAppVos;
    }


    /**
     * 查询应用工单分类列表
     */
    @Override
    public List<WorkOrderAppVo> queryList(WorkOrderAppBo bo) {
        LambdaQueryWrapper<WorkOrderApp> lqw = buildQueryWrapper(bo);
        List<WorkOrderAppVo> workOrderAppVos = baseMapper.selectVoList(lqw);
        attachFormCategories(workOrderAppVos);
        return workOrderAppVos;
    }

    private void attachFormCategory(WorkOrderAppVo workOrderAppVo) {
        if (workOrderAppVo == null || StringUtils.isBlank(workOrderAppVo.getApplicationId())) {
            return;
        }
        WfFormApp formApp = wfFormAppMapper.selectOne(Wrappers.lambdaQuery(WfFormApp.class)
            .eq(WfFormApp::getApplicationId, workOrderAppVo.getApplicationId())
            .eq(WfFormApp::getType, WORK_ORDER_FORM_TYPE)
            .eq(WfFormApp::getDelFlag, "0")
            .last("LIMIT 1"));
        if (formApp != null) {
            workOrderAppVo.setCategoryId(formApp.getCategoryId());
        }
    }

    private void attachFormCategories(List<WorkOrderAppVo> workOrderAppVos) {
        if (workOrderAppVos == null || workOrderAppVos.isEmpty()) {
            return;
        }
        Map<String, String> categoryByApplicationId = new HashMap<>();
        for (WorkOrderAppVo workOrderAppVo : workOrderAppVos) {
            if (workOrderAppVo != null && StringUtils.isNotBlank(workOrderAppVo.getApplicationId())) {
                categoryByApplicationId.put(workOrderAppVo.getApplicationId(), null);
            }
        }
        if (categoryByApplicationId.isEmpty()) {
            return;
        }
        List<WfFormApp> formApps = wfFormAppMapper.selectList(Wrappers.lambdaQuery(WfFormApp.class)
            .in(WfFormApp::getApplicationId, categoryByApplicationId.keySet())
            .eq(WfFormApp::getType, WORK_ORDER_FORM_TYPE)
            .eq(WfFormApp::getDelFlag, "0"));
        for (WfFormApp formApp : formApps) {
            categoryByApplicationId.putIfAbsent(formApp.getApplicationId(), formApp.getCategoryId());
            if (categoryByApplicationId.get(formApp.getApplicationId()) == null) {
                categoryByApplicationId.put(formApp.getApplicationId(), formApp.getCategoryId());
            }
        }
        for (WorkOrderAppVo workOrderAppVo : workOrderAppVos) {
            if (workOrderAppVo != null) {
                workOrderAppVo.setCategoryId(categoryByApplicationId.get(workOrderAppVo.getApplicationId()));
            }
        }
    }

    private LambdaQueryWrapper<WorkOrderApp> buildQueryWrapper(WorkOrderAppBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<WorkOrderApp> lqw = Wrappers.lambdaQuery();
        lqw.like(StringUtils.isNotBlank(bo.getApplicationName()), WorkOrderApp::getApplicationName, bo.getApplicationName());
        lqw.eq(StringUtils.isNotBlank(bo.getApplicationId()), WorkOrderApp::getApplicationId, bo.getApplicationId());
        lqw.eq(StringUtils.isNotBlank(bo.getApplicationSecret()), WorkOrderApp::getApplicationSecret, bo.getApplicationSecret());
        return lqw;
    }

    /**
     * 新增应用工单分类
     */
    @Override
    public Boolean insertByBo(WorkOrderAppBo bo) {
        LambdaQueryWrapper<WorkOrderApp> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WorkOrderApp::getApplicationId, bo.getApplicationId());
        if (baseMapper.selectCount(queryWrapper) > 0) {
            throw new RuntimeException("应用已存在");
        }
        WorkOrderApp add = BeanUtil.toBean(bo, WorkOrderApp.class);
        if ("1".equals(bo.getAppFlag())) {
            if (!com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotBlank(bo.getApplicationId())) {
                add.setApplicationId(generateUuid());
            }
            if (!com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotBlank(bo.getApplicationSecret())) {
                add.setApplicationSecret(generateUuid());
            }
        }
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setAppId(add.getAppId());
        }
        return flag;
    }

    /**
     * 修改应用工单分类
     */
    @Override
    public Boolean updateByBo(WorkOrderAppBo bo) {
        WorkOrderApp update = BeanUtil.toBean(bo, WorkOrderApp.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 根据应用ID（applicationId）解析应用包名。
     * 查询顺序：先查 workorder_app，再查 wf_app。
     * 两张表都查不到或 appPackage 为空时返回 null，由调用方流程变量 app_package 兜底。
     */
    public String resolveAppPackageByApplicationId(String applicationId) {
        if (com.baomidou.mybatisplus.core.toolkit.StringUtils.isBlank(applicationId)) {
            return null;
        }
        try {
            // 1. 先查 workorder_app
            LambdaQueryWrapper<WorkOrderApp> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(WorkOrderApp::getApplicationId, applicationId);
            wrapper.last("LIMIT 1");
            WorkOrderApp workOrderApp = baseMapper.selectOne(wrapper);
            if (workOrderApp != null && com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotBlank(workOrderApp.getAppPackage())) {
                return workOrderApp.getAppPackage();
            }
            // 2. workorder_app 查不到，再查 wf_app
            LambdaQueryWrapper<WfApp> wfWrapper = Wrappers.lambdaQuery();
            wfWrapper.eq(WfApp::getApplicationId, applicationId);
            wfWrapper.last("LIMIT 1");
            WfApp wfApp = wfAppMapper.selectOne(wfWrapper);
            if (wfApp != null && com.baomidou.mybatisplus.core.toolkit.StringUtils.isNotBlank(wfApp.getAppPackage())) {
                return wfApp.getAppPackage();
            }
        } catch (Exception e) {
            log.warn("解析应用包名失败，applicationId={}", applicationId, e);
        }
        return null;
    }

    /**
     * 保存前的数据校验
     */
    private void validEntityBeforeSave(WorkOrderApp entity) {
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除应用工单分类
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid) {
        if (isValid) {
            //效验这个分类下是否还有别的数据
            validateService.validateBeforeDeletion(ids);    }
        return baseMapper.deleteBatchIds(ids) > 0;
    }
}
