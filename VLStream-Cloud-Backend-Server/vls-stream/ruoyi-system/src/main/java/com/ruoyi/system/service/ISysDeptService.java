/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.system.service;

import cn.hutool.core.lang.tree.Tree;
import com.ruoyi.common.core.domain.entity.SysDeptView;

import java.util.List;

/**
 * department service layer
 *
 * @author Lion Li
 */
public interface ISysDeptService {
    /**
     * Query department data
     *
     * @param dept departmentinfo
     * @return departmentinfocollection
     */
    List<SysDeptView> selectDeptList(SysDeptView dept);

    /**
     * Query department info
     *
     * @param dept departmentinfo
     * @return department infocollection
     */
    List<Tree<String >> selectDeptTreeList(SysDeptView dept);

    /**
     * Build before need to
     *
     * @param depts department
     * @return
     */
    List<Tree<String >> buildDeptTreeSelect(List<SysDeptView> depts);

    /**
     * role IDQuery department info
     *
     * @param roleId role ID
     * @return in department
     */
    List<Long> selectDeptListByRoleId(Long roleId);

    /**
     * department IDQuery info
     *
     * @param deptId department ID
     * @return departmentinfo
     */
    SysDeptView selectDeptById(String deptId);

    /**
     * IDQuery all sub department ( )
     *
     * @param deptId department ID
     * @return sub department
     */
    long selectNormalChildrenDeptById(String  deptId);

    /**
     * whether in department sub node
     *
     * @param deptId department ID
     * @return
     */
    boolean hasChildByDeptId(String deptId);

    /**
     * Query departmentwhether in user
     *
     * @param deptId department ID
     * @return true in false in
     */
    boolean checkDeptExistUser(String deptId);

    /**
     * Validate department namewhether
     *
     * @param dept departmentinfo
     * @return
     */
    boolean checkDeptNameUnique(SysDeptView dept);

    /**
     * Validate departmentwhether data
     *
     * @param deptId department ID
     */
    void checkDeptDataScope(String  deptId);

    /**
     * Add departmentinfo
     *
     * @param dept departmentinfo
     * @return
     */
    int insertDept(SysDeptView dept);

    /**
     * Update departmentinfo
     *
     * @param dept departmentinfo
     * @return
     */
    int updateDept(SysDeptView dept);

    /**
     * Delete department info
     *
     * @param deptId department ID
     * @return
     */
    int deleteDeptById(String deptId);
}
