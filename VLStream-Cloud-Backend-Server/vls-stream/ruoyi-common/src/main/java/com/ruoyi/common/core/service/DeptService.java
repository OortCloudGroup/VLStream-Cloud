/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.core.service;

/**
 * departmentservice
 *
 * @author Lion Li
 */
public interface DeptService {

    /**
     * department IDQuery department name
     *
     * @param deptIds department ID
     * @return department name
     */
    String selectDeptNameByIds(String deptIds);

}
