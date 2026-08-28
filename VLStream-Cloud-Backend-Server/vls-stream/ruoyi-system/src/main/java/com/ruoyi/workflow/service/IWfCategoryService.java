/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.workflow.domain.WfCategory;
import com.ruoyi.workflow.domain.vo.WfCategoryVo;

import java.util.Collection;
import java.util.List;

/**
 * workflow Serviceinterface
 *
 * @author KonBAI
 * @date 2022-01-15
 */
public interface IWfCategoryService {
    /**
     * Query
     * @return
     */
    WfCategoryVo queryById(Long categoryId);

    /**
     * Query list
     */
    TableDataInfo<WfCategoryVo> queryPageList(WfCategory category, PageQuery pageQuery);

    /**
     * Query list
     */
    List<WfCategoryVo> queryList(WfCategory category);

    /**
     * Add workflow
     *
     * @param category workflow info
     * @return
     */
    int insertCategory(WfCategory category);

    /**
     * workflow
     * @param category workflow info
     * @return
     */
    int updateCategory(WfCategory category);

    /**
     * Validate Delete data
     * @param ids primary keycollection
     * @param isValid whether Validate ,true-Delete beforeValidate ,false- Validate
     * @return
     */
    int deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * Validate whether
     *
     * @param category workflow
     * @return
     */
    boolean checkCategoryCodeUnique(WfCategory category);
}
