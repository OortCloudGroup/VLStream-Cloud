/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.framework.handler;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpStatus;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

import java.util.Date;

/**
 * MP注入处理器
 *
 * @author Lion Li
 * @date 2021/4/25
 */
@Slf4j
public class CreateAndUpdateMetaObjectHandler implements MetaObjectHandler {

    private final String singleTenantId;

    /**
     * 创建字段填充器，并绑定后台唯一可信的租户标识。
     */
    public CreateAndUpdateMetaObjectHandler(String singleTenantId) {
        this.singleTenantId = singleTenantId;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        try {
            fillSingleTenant(metaObject);
            Date current = new Date();
            LoginUser loginUser = getLoginUser();
            fillAuditField(metaObject, "createTime", current);
            fillAuditField(metaObject, "updateTime", current);
            fillAuditField(metaObject, "createBy", auditUsername(loginUser));
            fillAuditField(metaObject, "updateBy", auditUsername(loginUser));
            fillAuditField(metaObject, "createUser", auditUserId(loginUser));
            fillAuditField(metaObject, "updateUser", auditUserId(loginUser));
            fillAuditField(metaObject, "createDept", auditDeptId(loginUser));
        } catch (Exception e) {
            throw new ServiceException("自动注入异常 => " + e.getMessage(), HttpStatus.HTTP_UNAUTHORIZED);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        try {
            fillSingleTenant(metaObject);
            LoginUser loginUser = getLoginUser();
            fillAuditField(metaObject, "updateTime", new Date());
            fillAuditField(metaObject, "updateBy", auditUsername(loginUser));
            fillAuditField(metaObject, "updateUser", auditUserId(loginUser));
        } catch (Exception e) {
            throw new ServiceException("自动注入异常 => " + e.getMessage(), HttpStatus.HTTP_UNAUTHORIZED);
        }
    }

    /**
     * 强制覆盖实体上的租户值，保证客户端值、空值和内部硬编码都不能影响持久化结果。
     */
    private void fillSingleTenant(MetaObject metaObject) {
        if (ObjectUtil.isNotNull(metaObject) && metaObject.hasSetter("tenantId")) {
            metaObject.setValue("tenantId", singleTenantId);
        }
    }

    /**
     * Overwrite a supported audit property while leaving entities without that property untouched.
     */
    private void fillAuditField(MetaObject metaObject, String fieldName, Object value) {
        if (ObjectUtil.isNotNull(metaObject) && metaObject.hasSetter(fieldName) && ObjectUtil.isNotNull(value)) {
            metaObject.setValue(fieldName, value);
        }
    }

    /**
     * Resolve the current authenticated user without failing background persistence operations.
     */
    private LoginUser getLoginUser() {
        try {
            return LoginHelper.getLoginUser();
        } catch (Exception e) {
            log.warn("自动注入警告 => 用户未登录");
            return null;
        }
    }

    /**
     * Return the account name used by RuoYi createBy/updateBy columns.
     */
    private String auditUsername(LoginUser loginUser) {
        return ObjectUtil.isNotNull(loginUser) && StringUtils.isNotBlank(loginUser.getUsername())
            ? loginUser.getUsername() : null;
    }

    /**
     * Return the user identifier used by SpringBlade createUser/updateUser columns.
     */
    private String auditUserId(LoginUser loginUser) {
        return ObjectUtil.isNotNull(loginUser) && StringUtils.isNotBlank(loginUser.getUserId())
            ? loginUser.getUserId() : null;
    }

    /**
     * Return the department identifier used by SpringBlade createDept columns.
     */
    private String auditDeptId(LoginUser loginUser) {
        return ObjectUtil.isNotNull(loginUser) && StringUtils.isNotBlank(loginUser.getDeptId())
            ? loginUser.getDeptId() : null;
    }

}
