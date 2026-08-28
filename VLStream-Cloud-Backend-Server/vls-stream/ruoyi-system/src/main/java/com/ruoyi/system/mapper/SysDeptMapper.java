/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.ruoyi.common.core.domain.entity.SysDeptView;
import com.ruoyi.common.core.mapper.BaseMapperPlus;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * department data layer
 *
 * @author Lion Li
 */
public interface SysDeptMapper extends BaseMapperPlus<SysDeptMapper, SysDeptView, SysDeptView> {

    @Select("SELECT dept_udid FROM sys_dept")
    List<String> selectOortUdidList();

    SysDeptView selectByDeptId(@Param("deptId") String deptId);

    SysDeptView selectDeptByUdid(@Param("udid") String uuid);

    Long selectDeptIdByCode(@Param("code") String code);
    /**
     * Query department data
     *
     * @param queryWrapper Query
     * @return departmentinfocollection
     */
//    @DataPermission({
//        @DataColumn(key = "deptName", value = "dept_id")
//    })
    List<SysDeptView> selectDeptList(@Param(Constants.WRAPPER) Wrapper<SysDeptView> queryWrapper);

    /**
     * role IDQuery department info
     *
     * @param roleId role ID
     * @param deptCheckStrictly department item whether
     * @return in department
     */
    List<Long> selectDeptListByRoleId(@Param("roleId") Long roleId, @Param("deptCheckStrictly") boolean deptCheckStrictly);

    Date selectLatestUpdateTime();
}
