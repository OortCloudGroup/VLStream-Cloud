/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.helper;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.model.SaStorage;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.enums.DeviceType;
import com.ruoyi.common.enums.UserType;
import com.ruoyi.common.utils.StringUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 *
 * <p>
 * user_type to user user user pc,app
 * deivce to device user device web,ios
 * user and device control
 * <p>
 * user user control
 * user and device control
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginHelper {

    public static final String LOGIN_USER_KEY = "loginUser";
    public static final String USER_KEY = "userId";

    /**
     *
     *
     * @param loginUser userinfo
     */
    public static void login(LoginUser loginUser) {
        loginByDevice(loginUser, null);
    }

    /**
     * device
     * user device
     *
     * @param loginUser userinfo
     */
    public static void loginByDevice(LoginUser loginUser, DeviceType deviceType) {
        SaStorage storage = SaHolder.getStorage();
        storage.set(LOGIN_USER_KEY, loginUser);
        storage.set(USER_KEY, loginUser.getUserId());
        SaLoginModel model = new SaLoginModel();
        if (ObjectUtil.isNotNull(deviceType)) {
            model.setDevice(deviceType.getDevice());
        }
        StpUtil.login(loginUser.getLoginId(), model.setExtra(USER_KEY, loginUser.getUserId()));
        StpUtil.getTokenSession().set(LOGIN_USER_KEY, loginUser);
    }

    /**
     * Get user( )
     */
    public static LoginUser getLoginUser() {
        LoginUser loginUser = (LoginUser) SaHolder.getStorage().get(LOGIN_USER_KEY);
        if (loginUser != null) {
            return loginUser;
        }
            loginUser = (LoginUser) StpUtil.getTokenSession().get(LOGIN_USER_KEY);
        SaHolder.getStorage().set(LOGIN_USER_KEY, loginUser);
        return loginUser;
    }

    /**
     * Get user token
     */
    public static LoginUser getLoginUser(String token) {
        return (LoginUser) StpUtil.getTokenSessionByToken(token).get(LOGIN_USER_KEY);
    }

    /**
     * Get user ID
     */
    public static String getUserId() {
        String userId;
        try {
            userId = Convert.toStr(SaHolder.getStorage().get(USER_KEY));
            if (StringUtils.isBlank(userId)) {
                userId = Convert.toStr(StpUtil.getExtra(USER_KEY));
                SaHolder.getStorage().set(USER_KEY, userId);
            }
        } catch (Exception e) {
            return null;
        }
        return userId;
    }

    /**
     * Get department ID
     */
    public static String  getDeptId() {
        return getLoginUser().getDeptId();
    }

    /**
     * Get user
     */
    public static String getUsername() {
        try {
            LoginUser loginUser = getLoginUser();
            return loginUser == null ? "" : StringUtils.defaultString(loginUser.getUsername());
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Get user
     */
    public static String getNickName() {
        return getLoginUser().getNickName();
    }

    /**
     * Get user
     */
    public static UserType getUserType() {
        String loginId = StpUtil.getLoginIdAsString();
        return UserType.getUserType(loginId);
    }

    /**
     * whether to administrator
     *
     * @param userId user ID
     * @return
     */
    public static boolean isAdmin(String userId) {
        return UserConstants.ADMIN_ID.equals(userId);
    }

    public static boolean isAdmin() {
        return isAdmin(getUserId());
    }

}
