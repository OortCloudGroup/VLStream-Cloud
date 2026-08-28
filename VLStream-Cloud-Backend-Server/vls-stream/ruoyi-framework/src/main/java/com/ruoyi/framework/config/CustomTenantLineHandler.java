/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.framework.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.enums.TenantType;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.config.properties.TokenProperties;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Process
 */
@Slf4j
@Component
public class CustomTenantLineHandler implements TenantLineHandler, SmartInitializingSingleton {

    private static final String TENANT_COLUMN = "tenant_id";
    private static final String NO_TENANT = "__NO_TENANT__";

    private final TokenProperties tokenProperties;
    private final Set<String> tenantTables = new HashSet<String>();

    public CustomTenantLineHandler(TokenProperties tokenProperties) {
        this.tokenProperties = tokenProperties;
    }

    /**
     * Get tenant ID value
     */
    @Override
    public Expression getTenantId() {
        return new StringValue(resolveTenantId());
    }

    /**
     * Get field (data tenant IDfield )
     */
    @Override
    public String getTenantIdColumn() {
        return TENANT_COLUMN;
    }

    /**
     * Check whether
     */
    @Override
    public boolean ignoreTable(String tableName) {
        return tableName == null || !tenantTables.contains(tableName.toLowerCase(Locale.ROOT));
    }

    /**
     * , Sa-Token will ; after .
     * value , Query to .
     */
    public String resolveTenantId() {
        String tenantIdFromContext = TenantContextHolder.getTenantId();
        if (StringUtils.isNotBlank(tenantIdFromContext)) {
            return tenantIdFromContext;
        }
        try {
            LoginUser loginUser = LoginHelper.getLoginUser();
            if (loginUser != null && StringUtils.isNotBlank(loginUser.getTenantId())) {
                return loginUser.getTenantId();
            }
        } catch (Exception ignored) {
            // not interface Process .
        }
        if (!TenantType.MULTI_TENANT.getType().equalsIgnoreCase(tokenProperties.getTenantType())) {
            return tokenProperties.getSingleTenantId();
        }
        return NO_TENANT;
    }

    public boolean hasResolvedTenant() {
        return !NO_TENANT.equals(resolveTenantId());
    }

    @Override
    public void afterSingletonsInstantiated() {
        for (TableInfo tableInfo : TableInfoHelper.getTableInfos()) {
            if (containsTenantColumn(tableInfo)) {
                tenantTables.add(tableInfo.getTableName().toLowerCase(Locale.ROOT));
            }
        }
        log.info("租户 SQL 隔离已识别 {} 张包含 {} 字段的实体表", tenantTables.size(), TENANT_COLUMN);
    }

    private boolean containsTenantColumn(TableInfo tableInfo) {
        if (tableInfo == null) {
            return false;
        }
        for (TableFieldInfo fieldInfo : tableInfo.getFieldList()) {
            if (TENANT_COLUMN.equalsIgnoreCase(fieldInfo.getColumn())) {
                return true;
            }
        }
        return false;
    }

    Set<String> getTenantTables() {
        return Collections.unmodifiableSet(tenantTables);
    }
}
