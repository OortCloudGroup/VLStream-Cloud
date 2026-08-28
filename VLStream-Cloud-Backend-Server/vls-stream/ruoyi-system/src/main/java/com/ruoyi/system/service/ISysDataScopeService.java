/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.service;

/**
 * data service
 *
 * @author Lion Li
 */
public interface ISysDataScopeService {

    /**
     * Get roleCustom
     *
     * @param roleId roleid
     * @return department ID
     */
    String getRoleCustom(Long roleId);

    /**
     * Get department
     *
     * @param deptId department ID
     * @return department ID
     */
    String getDeptAndChild(String deptId);

}
