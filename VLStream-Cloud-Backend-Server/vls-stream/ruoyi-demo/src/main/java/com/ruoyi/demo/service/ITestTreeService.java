/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.demo.service;

import com.ruoyi.demo.domain.bo.TestTreeBo;
import com.ruoyi.demo.domain.vo.TestTreeVo;

import java.util.Collection;
import java.util.List;

/**
 * Serviceinterface
 *
 * @author Lion Li
 * @date 2021-07-26
 */
public interface ITestTreeService {
    /**
     * Query
     *
     * @return
     */
    TestTreeVo queryById(Long id);

    /**
     * Query list
     */
    List<TestTreeVo> queryList(TestTreeBo bo);

    /**
     * Add object
     *
     * @param bo Add object
     * @return
     */
    Boolean insertByBo(TestTreeBo bo);

    /**
     * objectUpdate
     *
     * @param bo object
     * @return
     */
    Boolean updateByBo(TestTreeBo bo);

    /**
     * Validate Delete data
     *
     * @param ids primary keycollection
     * @param isValid whether Validate ,true-Delete beforeValidate ,false- Validate
     * @return
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);
}
