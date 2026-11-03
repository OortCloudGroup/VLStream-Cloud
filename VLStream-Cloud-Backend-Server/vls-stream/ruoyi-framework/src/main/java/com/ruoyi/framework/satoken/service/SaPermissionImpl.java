/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.framework.satoken.service;

import cn.dev33.satoken.stp.StpInterface;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.domain.model.LivePermissionProvider;
import com.ruoyi.common.enums.UserType;
import com.ruoyi.common.helper.LoginHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * sa-token
 *
 * @author Lion Li
 */
public class SaPermissionImpl implements StpInterface {

    private final LivePermissionProvider permissionProvider;

    public SaPermissionImpl(LivePermissionProvider permissionProvider) {
        this.permissionProvider = permissionProvider;
    }

    /**
     * Get menu
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        LoginUser loginUser = verifiedCurrentUser(loginId);
        if (loginUser == null) {
            return new ArrayList<>();
        }
        UserType userType = UserType.getUserType(loginUser.getUserType());
        if (userType == UserType.SYS_USER) {
            return new ArrayList<>(permissionProvider.getMenuPermissions(loginUser.getUserId()));
        } else if (userType == UserType.APP_USER) {
            //
        }
        return new ArrayList<>();
    }

    /**
     * Get role
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        LoginUser loginUser = verifiedCurrentUser(loginId);
        if (loginUser == null) {
            return new ArrayList<>();
        }
        UserType userType = UserType.getUserType(loginUser.getUserType());
        if (userType == UserType.SYS_USER) {
            return new ArrayList<>(permissionProvider.getRolePermissions(loginUser.getUserId()));
        } else if (userType == UserType.APP_USER) {
            //
        }
        return new ArrayList<>();
    }

    private LoginUser verifiedCurrentUser(Object loginId) {
        LoginUser user;
        try {
            user = currentLoginUser();
        } catch (RuntimeException ex) {
            return null;
        }
        if (loginId == null || user == null || user.getUserType() == null || user.getUserId() == null
            || !user.getLoginId().equals(String.valueOf(loginId))) {
            return null;
        }
        return user;
    }

    LoginUser currentLoginUser() {
        return LoginHelper.getLoginUser();
    }
}
