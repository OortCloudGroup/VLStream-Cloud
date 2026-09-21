/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TunnelEndpoint;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TunnelEndpointMapper extends BaseMapper<TunnelEndpoint> {
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM vls_tunnel_endpoint WHERE id = #{id} AND is_deleted = 0")
    TunnelEndpoint selectByIdGlobal(@Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM vls_tunnel_endpoint WHERE agent_token_hash = #{tokenHash} "
        + "AND is_deleted = 0 LIMIT 1")
    TunnelEndpoint selectByAgentTokenHash(@Param("tokenHash") String tokenHash);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT COUNT(1) FROM vls_tunnel_endpoint WHERE server_bind_port = #{port} "
        + "AND is_deleted = 0")
    int countByServerBindPort(@Param("port") Integer port);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM vls_tunnel_endpoint WHERE is_deleted = 0 "
        + "AND desired_state <> 'REVOKED' ORDER BY id")
    List<TunnelEndpoint> selectDesiredRoutes();
}
