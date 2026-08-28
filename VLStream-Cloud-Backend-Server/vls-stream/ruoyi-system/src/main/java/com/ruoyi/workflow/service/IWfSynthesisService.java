/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.workflow.domain.WfSynthesis;
import com.ruoyi.workflow.domain.bo.WfSynthesisBo;
import com.ruoyi.workflow.domain.vo.WfSynthesisVo;

import java.util.Collection;
import java.util.List;

/**
 * workflowServiceinterface
 *
 * @author
 * @date 2025-01-04
 */
public interface IWfSynthesisService extends IService<WfSynthesis> {

    /**
     * Query workflow
     */
    WfSynthesisVo queryById(String synthesisId);


    /**
     * Query workflow list
     */
    List<WfSynthesisVo> queryList(WfSynthesisBo bo);

    /**
     * Query full workflow
     */
    List<WfSynthesisVo> queryListAll(String categoryName);

    /**
     * Add workflow
     */
    Boolean insertByBo(WfSynthesisBo bo);

    /**
     * Update workflow
     */
    Boolean updateByBo(WfSynthesisBo bo);

    /**
     * Validate Batch delete workflowinfo
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);
    /**
     * id Query sub node
     */
    List<String > selectChildById(String  parentId);
}
