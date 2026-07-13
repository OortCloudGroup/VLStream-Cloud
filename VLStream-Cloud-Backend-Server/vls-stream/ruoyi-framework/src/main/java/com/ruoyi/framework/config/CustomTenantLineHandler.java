/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.framework.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.enums.TenantType;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.common.interceptor.AuthorizationInterceptor;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.redis.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 多租户处理插件
 */
@Slf4j
@Component
public class CustomTenantLineHandler implements TenantLineHandler {
    /**
     * 忽略添加租户ID的表
     */
    private static final List<String> IGNORE_TABLE_NAMES = new ArrayList<>();

    static {
        //  IGNORE_TABLE_NAMES.add("report_user");
        IGNORE_TABLE_NAMES.add("sys_dict_data");
        IGNORE_TABLE_NAMES.add("sys_dict_type");
        IGNORE_TABLE_NAMES.add("sys_user");
        IGNORE_TABLE_NAMES.add("ACT_RE_MODEL");
        IGNORE_TABLE_NAMES.add("process_template");
    }

    /**
     * 单租户校验地址
     */
    @Value("${token.singleTenantId}")
    private String singleTenantId;
    @Value("${token.tenantType}")
    private String tenantType;

    /**
     * 获取租户ID值表达式
     */
    @Override
    public Expression getTenantId() {
        String tid = returnTenantId();
        return new StringValue("'" + tid + "'");
    }

    /**
     * 获取租户字段名(数据库的租户ID字段名)
     */
    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    /**
     * 根据表名判断是否忽略拼接多租户条件
     */
    @Override
    public boolean ignoreTable(String tableName) {
        return IGNORE_TABLE_NAMES.contains(tableName);
    }

    /**
     * 从请求中获取到token，从token中解析出tenantId
     */
    private String returnTenantId() {
        String tenantIdFromContext = TenantContextHolder.getTenantId();
        if (StringUtils.isNotBlank(tenantIdFromContext)) {
            return tenantIdFromContext;
        }
        if (tenantType.equals(TenantType.SIGNLE_TENANT.getType())) {
            return singleTenantId;
        }
        //获取当前请求的HttpServletRequest
        String accessToken = AuthorizationInterceptor.getToken();
        SysUser sysUser = null;
        if (StringUtils.isNotBlank(accessToken)) {
            sysUser = RedisUtils.getCacheObject(accessToken);
        }
        // 检查JSON字符串是否为null
        if (sysUser == null) {
            // 处理未找到的情况，比如返回默认租户ID
            return "";
        }
//        // 租户Id，可以从缓存或者cookie，token等中获取
//        String tenantId = reportLoginUser.getTenantId();
        String tenantId = sysUser.getTenantId();
        String toTenantId = RedisUtils.getCacheObject("to_tenant_id");
        if (StringUtils.isNotBlank(toTenantId)) {
            return toTenantId;
        }
        return tenantId;
    }
}
