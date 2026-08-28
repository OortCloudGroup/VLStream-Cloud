/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.workflow.domain.bo.ProcessTemplateBo;
import com.ruoyi.workflow.domain.vo.ProcessTemplateVo;

import java.util.Collection;
import java.util.List;

/**
 * workflowInitialize Serviceinterface
 *
 * @author lcq
 * @date 2025-01-07
 */
public interface IProcessTemplateService {

    /**
     * Query workflowInitialize
     */
    ProcessTemplateVo queryById(String id);

    /**
     * Query workflowInitialize list
     */
    TableDataInfo<ProcessTemplateVo> queryPageList(ProcessTemplateBo bo, PageQuery pageQuery);

    /**
     * Query workflowInitialize list
     */
    List<ProcessTemplateVo> queryList(ProcessTemplateBo bo);

    /**
     * Add workflowInitialize
     */
    Boolean insertByBo(ProcessTemplateBo bo);

    /**
     * Update workflowInitialize
     */
    Boolean updateByBo(ProcessTemplateBo bo);

    /**
     * Validate Batch delete workflowInitialize info
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);
}
