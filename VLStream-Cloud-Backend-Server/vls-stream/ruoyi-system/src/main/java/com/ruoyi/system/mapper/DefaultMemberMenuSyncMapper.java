package com.ruoyi.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.system.domain.DefaultMemberMenuRow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** Cross-tenant maintenance of the default member role; callers must only pass vls_member roles. */
public interface DefaultMemberMenuSyncMapper {

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT role_id, tenant_id FROM sys_role WHERE BINARY role_key = BINARY #{roleKey} "
        + "AND role_id <> '1' AND status = '0' AND del_flag = '0' "
        + "AND tenant_id IS NOT NULL AND TRIM(tenant_id) <> ''")
    List<SysRole> selectActiveDefaultMemberRoles(@Param("roleKey") String roleKey);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT NULLIF(TRIM(tenant_id), '') FROM sys_role WHERE role_id = '1'")
    String selectCatalogueTenant();

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT menu_id, parent_id, path, perms, status, tenant_id FROM sys_menu")
    List<DefaultMemberMenuRow> selectMenuCatalogue();

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT menu_id FROM vls_default_member_menu_sync WHERE role_id = #{roleId}")
    List<Long> selectOfferedMenuIds(@Param("roleId") String roleId);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    List<Long> selectGrantedMenuIds(@Param("roleId") String roleId);

    /** Returns 1 only for the caller that records the offer first (safe across instances). */
    @InterceptorIgnore(tenantLine = "true")
    @Insert("INSERT IGNORE INTO vls_default_member_menu_sync (role_id, menu_id, tenant_id) "
        + "VALUES (#{roleId}, #{menuId}, #{tenantId})")
    int insertOffer(@Param("roleId") String roleId, @Param("tenantId") String tenantId, @Param("menuId") Long menuId);

    @InterceptorIgnore(tenantLine = "true")
    @Insert("INSERT IGNORE INTO vls_default_member_menu_sync (role_id, menu_id, tenant_id) "
        + "SELECT #{roleId}, menu_id, #{tenantId} FROM vls_default_member_menu_sync WHERE role_id = #{templateRoleId}")
    int copyOffers(@Param("templateRoleId") String templateRoleId, @Param("roleId") String roleId,
                   @Param("tenantId") String tenantId);

    @InterceptorIgnore(tenantLine = "true")
    @Insert("INSERT IGNORE INTO sys_role_menu (role_id, tenant_id, menu_id) VALUES (#{roleId}, #{tenantId}, #{menuId})")
    int insertGrant(@Param("roleId") String roleId, @Param("tenantId") String tenantId, @Param("menuId") Long menuId);

    @InterceptorIgnore(tenantLine = "true")
    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId} AND menu_id = #{menuId}")
    int deleteGrant(@Param("roleId") String roleId, @Param("menuId") Long menuId);
}
