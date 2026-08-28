/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.workflow.domain.WfFormSynthesis;
import com.ruoyi.workflow.domain.bo.WfFormSynthesisBo;
import com.ruoyi.workflow.domain.vo.WfFormSynthesisVo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;



/**
 * form Serviceinterface
 *
 * @author
 * @date 2024-12-25
 */
public interface IWfFormSynthesisService extends IService<WfFormSynthesis> {

    /**
     * Query form
     */
    WfFormSynthesisVo queryById(String categoryId);


    /**
     * Query form list
     */
    Optional<List<WfFormSynthesisVo>> queryList(WfFormSynthesisBo bo);

    /**
     * Add form
     */
    Boolean insertByBo(WfFormSynthesisBo bo);

    /**
     * Update form
     */
    Boolean updateByBo(WfFormSynthesisBo bo);

    /**
     * Validate Batch delete form info
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);
}
