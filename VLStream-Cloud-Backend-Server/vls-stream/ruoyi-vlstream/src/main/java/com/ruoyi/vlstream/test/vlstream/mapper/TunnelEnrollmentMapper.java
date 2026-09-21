/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TunnelEnrollment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface TunnelEnrollmentMapper extends BaseMapper<TunnelEnrollment> {
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM vls_tunnel_enrollment WHERE code_hash = #{codeHash} "
        + "AND is_deleted = 0 LIMIT 1 FOR UPDATE")
    TunnelEnrollment selectByCodeHashForUpdate(@Param("codeHash") String codeHash);
}
