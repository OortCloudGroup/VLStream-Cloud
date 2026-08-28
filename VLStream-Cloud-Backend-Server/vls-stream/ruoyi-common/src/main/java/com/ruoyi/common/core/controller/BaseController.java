/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.core.controller;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.interceptor.TokenHeaderResolver;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.redis.RedisUtils;

/**
 * web layer dataProcess
 *
 * @author Lion Li
 */
public class BaseController {

    /**
     *
     *
     * @param rows
     * @return operation
     */
    protected R<Void> toAjax(int rows) {
        return rows > 0 ? R.ok() : R.fail();
    }

    /**
     *
     *
     * @param result
     * @return operation
     */
    protected R<Void> toAjax(boolean result) {
        return result ? R.ok() : R.fail();
    }

    /**
     * page
     */
    public String redirect(String url) {
        return StringUtils.format("redirect:{}", url);
    }

    /**
     * Get user info
     */
    public LoginUser getLoginUser() {
        return LoginHelper.getLoginUser();
    }

    /**
     * Get user ID
     */
    public String getUserId() {
        return LoginHelper.getUserId();
    }
    /**
     * tokenGet user ID
     */
    public String getUserId(String token) {
        String normalizedToken = TokenHeaderResolver.normalize(token);
        SysUser sysUser = normalizedToken == null ? null : RedisUtils.getCacheObject(normalizedToken);
        return sysUser == null ? LoginHelper.getUserId() : sysUser.getUserId();
    }

    /**
     * Get department ID
     */
    public String  getDeptId() {
        return LoginHelper.getDeptId();
    }

    /**
     * Get user
     */
    public String getUsername() {
        return LoginHelper.getUsername();
    }
}
