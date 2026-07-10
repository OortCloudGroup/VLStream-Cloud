package com.ruoyi.system.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.system.domain.SysUserRoleView;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户与角色关联表 数据层
 *
 * @author Lion Li
 */
public interface SysUserRoleViewMapper extends BaseMapperPlus<SysUserRoleViewMapper, SysUserRoleView, SysUserRoleView> {

    List<String> selectUserIdsByRoleId(Long roleId);

    /**
     * 通过用户ID和角色ID查询角色（支持单独或组合条件）
     *
     * @param userId 用户ID（可为null）
     * @param roleId 角色ID（可为null）
     * @return 角色对象信息
     */
    SysUserRoleView selectByCondition(@Param("userId") String userId, @Param("roleId")String roleId);

}
