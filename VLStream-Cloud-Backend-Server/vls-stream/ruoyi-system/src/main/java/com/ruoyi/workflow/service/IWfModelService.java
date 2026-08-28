/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.workflow.convert.ProcessModel;
import com.ruoyi.workflow.domain.bo.InitBo;
import com.ruoyi.workflow.domain.bo.WfModelBo;
import com.ruoyi.workflow.domain.vo.WfModelVo;
import org.flowable.engine.repository.Model;

import java.util.Collection;
import java.util.List;

/**
 * @author KonBAI
 * @createTime 2022/6/21 9:11
 */
public interface IWfModelService {

    void deleteModelCascade(String modelId,boolean isWorkOrder);

        /**
         * workflowdataInitialize
         *
         * @return
         */
    String initStart(InitBo initBo);

    Boolean eventManagementInitStart(InitBo initBo);

    List<List<String>> initShow();


    /**
     * Query workflowmodel list
     */
    TableDataInfo<WfModelVo> list(WfModelBo modelBo, PageQuery pageQuery);

    /**
     * Query workflowmodel list
     */
    List<WfModelVo> list(WfModelBo modelBo);

    /**
     * Query workflowmodel list
     */
    TableDataInfo<WfModelVo> historyList(WfModelBo modelBo, PageQuery pageQuery);

    /**
     * Query workflowmodel info
     */
    WfModelVo getModel(String modelId,String applicationId);

    /**
     * Query workflowmodelbpmn
     */
    String queryBpmnXmlById(String modelId);

    /**
     * Add modelinfo
     */
    String  insertModel(WfModelBo modelBo);

    /**
     * Update modelinfo
     */
    void updateModel(WfModelBo modelBo);

    /**
     * workflowmodelinfo
     *
     * @return
     */
    Model saveModel(WfModelBo modelBo, String ToTenantId, ProcessModel processModel);

    /**
     * to new workflowmodel
     */
    void latestModel(String modelId);

    /**
     * Delete workflowmodel
     */
    void deleteByIds(Collection<String> ids);

    /**
     * workflowmodel
     */
    boolean deployModel(String modelId);

    void copyModel(WfModelBo modelBo);

    /**
     * Query model
     *
     * @param modelBo
     * @param wfSyntheses
     * @param workOrderSyntheses
     * @param tenantId
     * @param history
     * @return
     */
    Long selectModelCount(WfModelBo modelBo, List<String> wfSyntheses, List<String> workOrderSyntheses,String tenantId,Boolean history);

    /**
     * Query model list
     */
    List<Model> selectModelList( WfModelBo modelBo, IPage<Model> page,List<String> wfSyntheses, List<String> workOrderSyntheses,String tenantId,Boolean history);

    void batchRemove(String modelKey);
}
