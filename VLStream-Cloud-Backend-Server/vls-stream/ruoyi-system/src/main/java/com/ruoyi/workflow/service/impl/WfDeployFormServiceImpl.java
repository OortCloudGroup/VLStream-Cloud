/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.flowable.utils.ModelUtils;
import com.ruoyi.workflow.domain.WfDeployForm;
import com.ruoyi.workflow.domain.WfForm;
import com.ruoyi.workflow.domain.vo.WfFormVo;
import com.ruoyi.workflow.mapper.WfDeployFormMapper;
import com.ruoyi.workflow.mapper.WfFormMapper;
import com.ruoyi.workflow.service.IWfDeployFormService;
import lombok.RequiredArgsConstructor;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * workflow instance formService layer Process
 *
 * @author KonBAI
 * @createTime 2022/3/7 22:07
 */
@RequiredArgsConstructor
@Service
public class WfDeployFormServiceImpl implements IWfDeployFormService {

    private final WfDeployFormMapper baseMapper;

    private final WfFormMapper formMapper;

    /**
     * Add workflow instance form
     *
     * @param deployForm workflow instance form
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertWfDeployForm(WfDeployForm deployForm) {
        // Delete workflow and form
        baseMapper.delete(new LambdaQueryWrapper<WfDeployForm>().eq(WfDeployForm::getDeployId, deployForm.getDeployId()));
        // Add workflow and form
        return baseMapper.insert(deployForm);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveInternalDeployForm(String deployId, BpmnModel bpmnModel) {
        List<WfDeployForm> deployFormList = new ArrayList<>();
        // Get startnode
        StartEvent startEvent = ModelUtils.getStartEvent(bpmnModel);
        if (ObjectUtil.isNull(startEvent)) {
            throw new RuntimeException("开始节点不存在，请检查流程设计是否有误！");
        }
        // startnodeforminfo
        WfDeployForm startDeployForm = buildDeployForm(deployId, startEvent);
        if (ObjectUtil.isNotNull(startDeployForm)) {
            deployFormList.add(startDeployForm);
        }
        // usernodeforminfo
        Collection<UserTask> userTasks = ModelUtils.getAllUserTaskEvent(bpmnModel);
        if (CollUtil.isNotEmpty(userTasks)) {
            for (UserTask userTask : userTasks) {
                WfDeployForm userTaskDeployForm = buildDeployForm(deployId, userTask);
                if (ObjectUtil.isNotNull(userTaskDeployForm)) {
                    deployFormList.add(userTaskDeployForm);
                }
            }
        }
        // Add workflow and form info
        return baseMapper.insertBatch(deployFormList);
    }


    /**
     * Query workflow form
     *
     * @param deployId
     * @return
     */
    @Override
    public WfFormVo selectDeployFormByDeployId(String deployId) {
        QueryWrapper<WfForm> wrapper = Wrappers.query();
        wrapper.eq("t2.deploy_id", deployId);
        List<WfFormVo> list = formMapper.selectFormVoList(wrapper);
//        if (ObjectUtil.isNotEmpty(list)) {
//            if (list.size() != 1) {
// throw new ServiceException("forminfoQuery ");
//            } else {
        return list.get(0);
//            }
//        } else {
//            return null;
//        }
    }

    /**
     * Build form infoobject
     *
     * @param deployId ID
     * @param node nodeinfo
     * @return form object. forminfo (formKey), null
     */
    private WfDeployForm buildDeployForm(String deployId, FlowNode node) {
        String formKey = ModelUtils.getFormKey(node);
        if (StringUtils.isEmpty(formKey)) {
            return null;
        }
        Long formId = Convert.toLong(StringUtils.substringAfter(formKey, "key_"));
        WfForm wfForm = formMapper.selectById(formId);
        if (ObjectUtil.isNull(wfForm)) {
            throw new ServiceException("表单信息查询错误，请验证表单是否正确绑定");
        }
        WfDeployForm deployForm = new WfDeployForm();
        deployForm.setDeployId(deployId);
        deployForm.setTenantId(wfForm.getTenantId());
        deployForm.setUserId(wfForm.getUserId());
        deployForm.setFormKey(formKey);
        deployForm.setNodeKey(node.getId());
        deployForm.setFormName(wfForm.getFormName());
        deployForm.setNodeName(node.getName());
        deployForm.setContent(wfForm.getContent());
        return deployForm;
    }
}
