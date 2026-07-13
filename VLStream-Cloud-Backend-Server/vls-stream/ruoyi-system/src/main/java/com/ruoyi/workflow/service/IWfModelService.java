/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
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
         * 流程数据初始化
         *
         * @return
         */
    String initStart(InitBo initBo);

    Boolean eventManagementInitStart(InitBo initBo);

    List<List<String>> initShow();


    /**
     * 查询流程模型列表
     */
    TableDataInfo<WfModelVo> list(WfModelBo modelBo, PageQuery pageQuery);

    /**
     * 查询流程模型列表
     */
    List<WfModelVo> list(WfModelBo modelBo);

    /**
     * 查询流程模型列表
     */
    TableDataInfo<WfModelVo> historyList(WfModelBo modelBo, PageQuery pageQuery);

    /**
     * 查询流程模型详情信息
     */
    WfModelVo getModel(String modelId,String applicationId);

    /**
     * 查询流程模型bpmn文件
     */
    String queryBpmnXmlById(String modelId);

    /**
     * 新增模型信息
     */
    String  insertModel(WfModelBo modelBo);

    /**
     * 修改模型信息
     */
    void updateModel(WfModelBo modelBo);

    /**
     * 保存流程模型信息
     *
     * @return
     */
    Model saveModel(WfModelBo modelBo, String ToTenantId, ProcessModel processModel);

    /**
     * 设为最新流程模型
     */
    void latestModel(String modelId);

    /**
     * 删除流程模型
     */
    void deleteByIds(Collection<String> ids);

    /**
     * 部署流程模型
     */
    boolean deployModel(String modelId);

    void copyModel(WfModelBo modelBo);

    /**
     * 查询模型总数
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
     * 查询模型列表
     */
    List<Model> selectModelList( WfModelBo modelBo, IPage<Model> page,List<String> wfSyntheses, List<String> workOrderSyntheses,String tenantId,Boolean history);

    void batchRemove(String modelKey);
}
