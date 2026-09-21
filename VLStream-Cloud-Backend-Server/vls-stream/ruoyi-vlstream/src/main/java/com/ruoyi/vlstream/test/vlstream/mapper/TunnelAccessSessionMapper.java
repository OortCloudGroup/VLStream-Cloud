/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TunnelAccessSession;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface TunnelAccessSessionMapper extends BaseMapper<TunnelAccessSession> {
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM vls_tunnel_access_session WHERE access_token_hash = #{tokenHash} LIMIT 1 FOR UPDATE")
    TunnelAccessSession selectByTokenHash(@Param("tokenHash") String tokenHash);
}
