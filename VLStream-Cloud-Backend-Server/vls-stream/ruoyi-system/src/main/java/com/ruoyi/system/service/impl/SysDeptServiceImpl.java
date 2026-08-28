/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ruoyi.common.constant.CacheNames;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysDeptView;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.service.DeptService;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.DataBaseHelper;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.TreeBuildUtils;
import com.ruoyi.common.utils.redis.CacheUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.system.mapper.SysDeptMapper;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.service.ISysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * department service
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysDeptServiceImpl implements ISysDeptService, DeptService {

    private final SysDeptMapper baseMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserMapper userMapper;

    /**
     * Query department data
     *
     * @param dept departmentinfo
     * @return departmentinfocollection
     */
    @Override
    public List<SysDeptView> selectDeptList(SysDeptView dept) {
        LambdaQueryWrapper<SysDeptView> lqw = new LambdaQueryWrapper<>();
        lqw.eq(SysDeptView::getDelFlag, "0")
            .eq(ObjectUtil.isNotNull(dept.getDeptId()), SysDeptView::getDeptId, dept.getDeptId())
            .eq(ObjectUtil.isNotNull(dept.getParentId()), SysDeptView::getParentId, dept.getParentId())
            .like(StringUtils.isNotBlank(dept.getDeptName()), SysDeptView::getDeptName, dept.getDeptName())
            .eq(StringUtils.isNotBlank(dept.getStatus()), SysDeptView::getStatus, dept.getStatus())
            .orderByAsc(SysDeptView::getParentId)
            .orderByAsc(SysDeptView::getOrderNum);
        return baseMapper.selectDeptList(lqw);
    }

    /**
     * Query department info
     *
     * @param dept departmentinfo
     * @return department infocollection
     */
    @Override
    public List<Tree<String >> selectDeptTreeList(SysDeptView dept) {
        List<SysDeptView> depts = this.selectDeptList(dept);
        return buildDeptTreeSelect(depts);
    }

    /**
     * Build before need to
     *
     * @param depts department
     * @return
     */
    @Override
    public List<Tree<String >> buildDeptTreeSelect(List<SysDeptView> depts) {
        if (CollUtil.isEmpty(depts)) {
            return CollUtil.newArrayList();
        }
        return TreeBuildUtils.build(depts, (dept, tree) ->
            tree.setId(dept.getDeptId())
                .setParentId(dept.getParentId())
                .setName(dept.getDeptName())
                .setWeight(dept.getOrderNum()));
    }

    /**
     * role IDQuery department info
     *
     * @param roleId role ID
     * @return in department
     */
    @Override
    public List<Long> selectDeptListByRoleId(Long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        return baseMapper.selectDeptListByRoleId(roleId, role.getDeptCheckStrictly());
    }

    /**
     * department IDQuery info
     *
     * @param deptId department ID
     * @return departmentinfo
     */
    @Cacheable(cacheNames = CacheNames.SYS_DEPT, key = "#deptId")
    @Override
    public SysDeptView selectDeptById(String deptId) {
        SysDeptView dept = baseMapper.selectById(deptId);
        if (ObjectUtil.isNull(dept)) {
            return null;
        }
        SysDeptView parentDept = baseMapper.selectOne(new LambdaQueryWrapper<SysDeptView>()
            .select(SysDeptView::getDeptName)
            .eq(SysDeptView::getDeptId, dept.getParentId()));
        dept.setParentName(ObjectUtil.isNotNull(parentDept) ? parentDept.getDeptName() : null);
        return dept;
    }

    /**
     * department IDQuery department name
     *
     * @param deptIds department ID
     * @return department name
     */
    @Override
    public String selectDeptNameByIds(String deptIds) {
        List<String> list = new ArrayList<>();
        for (String id : StringUtils.splitTo(deptIds, Convert::toStr)) {
            SysDeptView dept = SpringUtils.getAopProxy(this).selectDeptById(id);
            if (ObjectUtil.isNotNull(dept)) {
                list.add(dept.getDeptName());
            }
        }
        return String.join(StringUtils.SEPARATOR, list);
    }

    /**
     * IDQuery all sub department ( )
     *
     * @param deptId department ID
     * @return sub department
     */
    @Override
    public long selectNormalChildrenDeptById(String deptId) {
        return baseMapper.selectCount(new LambdaQueryWrapper<SysDeptView>()
            .eq(SysDeptView::getStatus, UserConstants.DEPT_NORMAL)
            .apply(DataBaseHelper.findInSet(deptId, "ancestors")));
    }

    /**
     * whether in sub node
     *
     * @param deptId department ID
     * @return
     */
    @Override
    public boolean hasChildByDeptId(String deptId) {
        return baseMapper.exists(new LambdaQueryWrapper<SysDeptView>()
            .eq(SysDeptView::getParentId, deptId));
    }

    /**
     * Query departmentwhether in user
     *
     * @param deptId department ID
     * @return true in false in
     */
    @Override
    public boolean checkDeptExistUser(String deptId) {
        return userMapper.exists(new LambdaQueryWrapper<SysUser>()
            .eq(SysUser::getDeptId, deptId));
    }

    /**
     * Validate department namewhether
     *
     * @param dept departmentinfo
     * @return
     */
    @Override
    public boolean checkDeptNameUnique(SysDeptView dept) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<SysDeptView>()
            .eq(SysDeptView::getDeptName, dept.getDeptName())
            .eq(SysDeptView::getParentId, dept.getParentId())
            .ne(ObjectUtil.isNotNull(dept.getDeptId()), SysDeptView::getDeptId, dept.getDeptId()));
        return !exist;
    }

    /**
     * Validate departmentwhether data
     *
     * @param deptId department ID
     */
    @Override
    public void checkDeptDataScope(String  deptId) {
        if (!LoginHelper.isAdmin()) {
            SysDeptView dept = new SysDeptView();
            dept.setDeptId(deptId);
            List<SysDeptView> depts = this.selectDeptList(dept);
            if (CollUtil.isEmpty(depts)) {
                throw new ServiceException("没有权限访问部门数据！");
            }
        }
    }

    /**
     * Add departmentinfo
     *
     * @param dept departmentinfo
     * @return
     */
    @Override
    public int insertDept(SysDeptView dept) {
        SysDeptView info = baseMapper.selectById(dept.getParentId());
        // if node to , Add sub node
        if (!UserConstants.DEPT_NORMAL.equals(info.getStatus())) {
            throw new ServiceException("部门停用，不允许新增");
        }
        dept.setAncestors(info.getAncestors() + StringUtils.SEPARATOR + dept.getParentId());
        return baseMapper.insert(dept);
    }

    /**
     * Update departmentinfo
     *
     * @param dept departmentinfo
     * @return
     */
    @CacheEvict(cacheNames = CacheNames.SYS_DEPT, key = "#dept.deptId")
    @Override
    public int updateDept(SysDeptView dept) {
        SysDeptView newParentDept = baseMapper.selectById(dept.getParentId());
        SysDeptView oldDept = baseMapper.selectById(dept.getDeptId());
        if (ObjectUtil.isNotNull(newParentDept) && ObjectUtil.isNotNull(oldDept)) {
            String newAncestors = newParentDept.getAncestors() + StringUtils.SEPARATOR + newParentDept.getDeptId();
            String oldAncestors = oldDept.getAncestors();
            dept.setAncestors(newAncestors);
            updateDeptChildren(dept.getDeptId(), newAncestors, oldAncestors);
        }
        int result = baseMapper.updateById(dept);
        if (UserConstants.DEPT_NORMAL.equals(dept.getStatus()) && StringUtils.isNotEmpty(dept.getAncestors())
            && !StringUtils.equals(UserConstants.DEPT_NORMAL, dept.getAncestors())) {
            // if department is , department all department
            updateParentDeptStatusNormal(dept);
        }
        return result;
    }

    /**
     * Update department department
     *
     * @param dept current department
     */
    private void updateParentDeptStatusNormal(SysDeptView dept) {
        String ancestors = dept.getAncestors();
        Long[] deptIds = Convert.toLongArray(ancestors);
        baseMapper.update(null, new LambdaUpdateWrapper<SysDeptView>()
            .set(SysDeptView::getStatus, UserConstants.DEPT_NORMAL)
            .in(SysDeptView::getDeptId, Arrays.asList(deptIds)));
    }

    /**
     * Update sub element
     *
     * @param deptId Update department ID
     * @param newAncestors new IDcollection
     * @param oldAncestors old IDcollection
     */
    public void updateDeptChildren(String deptId, String newAncestors, String oldAncestors) {
        List<SysDeptView> children = baseMapper.selectList(new LambdaQueryWrapper<SysDeptView>()
            .apply(DataBaseHelper.findInSet(deptId, "ancestors")));
        List<SysDeptView> list = new ArrayList<>();
        for (SysDeptView child :  children) {
            SysDeptView dept = new SysDeptView();
            dept.setDeptId(child.getDeptId());
            dept.setAncestors(child.getAncestors().replaceFirst(oldAncestors, newAncestors));
            list.add(dept);
        }
        if (CollUtil.isNotEmpty(list)) {
            if (baseMapper.updateBatchById(list)) {
                list.forEach(dept -> CacheUtils.evict(CacheNames.SYS_DEPT, dept.getDeptId()));
            }
        }
    }

    /**
     * Delete department info
     *
     * @param deptId department ID
     * @return
     */
    @CacheEvict(cacheNames = CacheNames.SYS_DEPT, key = "#deptId")
    @Override
    public int deleteDeptById(String deptId) {
        return baseMapper.deleteById(deptId);
    }

    public SysDeptView selectDeptByUdid(String udid) {
        return baseMapper.selectDeptByUdid(udid);
    }
}
