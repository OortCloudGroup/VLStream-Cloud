/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.demo.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.demo.domain.TestDemo;
import com.ruoyi.demo.domain.bo.TestDemoBo;
import com.ruoyi.demo.domain.vo.TestDemoVo;

import java.util.Collection;
import java.util.List;

/**
 * Serviceinterface
 *
 * @author Lion Li
 * @date 2021-07-26
 */
public interface ITestDemoService {

    /**
     * Query
     *
     * @return
     */
    TestDemoVo queryById(Long id);

    /**
     * Query list
     */
    TableDataInfo<TestDemoVo> queryPageList(TestDemoBo bo, PageQuery pageQuery);

    /**
     * Custom Query
     */
    TableDataInfo<TestDemoVo> customPageList(TestDemoBo bo, PageQuery pageQuery);

    /**
     * Query list
     */
    List<TestDemoVo> queryList(TestDemoBo bo);

    /**
     * Add object
     *
     * @param bo Add object
     * @return
     */
    Boolean insertByBo(TestDemoBo bo);

    /**
     * objectUpdate
     *
     * @param bo object
     * @return
     */
    Boolean updateByBo(TestDemoBo bo);

    /**
     * Validate Delete data
     *
     * @param ids primary keycollection
     * @param isValid whether Validate ,true-Delete beforeValidate ,false- Validate
     * @return
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     *
     */
    Boolean saveBatch(List<TestDemo> list);
}
