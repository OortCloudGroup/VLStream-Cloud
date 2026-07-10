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
 * 部门管理 数据层
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
     * 查询部门管理数据
     *
     * @param queryWrapper 查询条件
     * @return 部门信息集合
     */
//    @DataPermission({
//        @DataColumn(key = "deptName", value = "dept_id")
//    })
    List<SysDeptView> selectDeptList(@Param(Constants.WRAPPER) Wrapper<SysDeptView> queryWrapper);

    /**
     * 根据角色ID查询部门树信息
     *
     * @param roleId            角色ID
     * @param deptCheckStrictly 部门树选择项是否关联显示
     * @return 选中部门列表
     */
    List<Long> selectDeptListByRoleId(@Param("roleId") Long roleId, @Param("deptCheckStrictly") boolean deptCheckStrictly);

    Date selectLatestUpdateTime();
}
