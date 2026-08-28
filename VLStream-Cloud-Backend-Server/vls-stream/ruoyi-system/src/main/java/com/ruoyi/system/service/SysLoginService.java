/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.system.service;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.dto.RoleDTO;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.event.LogininforEvent;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.domain.model.XcxLoginUser;
import com.ruoyi.common.enums.DeviceType;
import com.ruoyi.common.enums.LoginType;
import com.ruoyi.common.exception.user.CaptchaException;
import com.ruoyi.common.exception.user.CaptchaExpireException;
import com.ruoyi.common.exception.user.UserException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.MessageUtils;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * Validate method
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class SysLoginService {

    private final SysUserMapper userMapper;
    private final ISysConfigService configService;
    private final SysPermissionService permissionService;

    @Value("${user.password.maxRetryCount}")
    private Integer maxRetryCount;

    @Value("${user.password.lockTime}")
    private Integer lockTime;

    /**
     *
     *
     * @param username user
     * @param password
     * @param code
     * @param uuid
     * @return
     */
    public String login(String username, String password, String code, String uuid) {
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        //
        if (captchaEnabled) {
            validateCaptcha(username, code, uuid);
        }
        SysUser user = loadUserByUsername(username);
        checkLogin(LoginType.PASSWORD, username, () -> !BCrypt.checkpw(password, user.getPassword()));
        // user data loginUser
        LoginUser loginUser = buildLoginUser(user);
        // Generate token
        LoginHelper.loginByDevice(loginUser, DeviceType.PC);
        recordLogininfor(username, Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success"));
        recordLoginInfo(user.getUserId(), username);
        return StpUtil.getTokenValue();
    }

    /**
     * to already Validate sub user Sa-Token will .
     * , token and Validate .
     */
    public String loginPlatformUser(SysUser user) {
        if (ObjectUtil.isNull(user)) {
            throw new UserException("user.not.exists", "platform-user");
        }
        if (isLoginBlockedStatus(user.getStatus())) {
            throw new UserException("user.blocked", user.getUserName());
        }
        LoginUser loginUser = buildLoginUser(user);
        LoginHelper.loginByDevice(loginUser, DeviceType.PC);
        recordLogininfor(user.getUserName(), Constants.LOGIN_SUCCESS, "统一平台登录成功");
        recordLoginInfo(user.getUserId(), user.getUserName());
        return StpUtil.getTokenValue();
    }

    public String smsLogin(String phonenumber, String smsCode) {
        // find user
        SysUser user = loadUserByPhonenumber(phonenumber);

        checkLogin(LoginType.SMS, user.getUserName(), () -> !validateSmsCode(phonenumber, smsCode));
        // user data loginUser
        LoginUser loginUser = buildLoginUser(user);
        // Generate token
        LoginHelper.loginByDevice(loginUser, DeviceType.APP);

        recordLogininfor(user.getUserName(), Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success"));
        recordLoginInfo(user.getUserId(), user.getUserName());
        return StpUtil.getTokenValue();
    }

    public String emailLogin(String email, String emailCode) {
        // find user
        SysUser user = loadUserByEmail(email);

        checkLogin(LoginType.EMAIL, user.getUserName(), () -> !validateEmailCode(email, emailCode));
        // user data loginUser
        LoginUser loginUser = buildLoginUser(user);
        // Generate token
        LoginHelper.loginByDevice(loginUser, DeviceType.APP);

        recordLogininfor(user.getUserName(), Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success"));
        recordLoginInfo(user.getUserId(), user.getUserName());
        return StpUtil.getTokenValue();
    }

    public String xcxLogin(String xcxCode) {
        // xcxCode to wx.login afterGet
        // todo
        // Validate appid + appsrcret + xcxCode Validate interface Get session_key and openid
        String openid = "";
        SysUser user = loadUserByOpenid(openid);

        // user data loginUser
        XcxLoginUser loginUser = new XcxLoginUser();
        loginUser.setUserId(user.getUserId());
        loginUser.setTenantId(user.getTenantId());
        loginUser.setUsername(user.getUserName());
        loginUser.setUserType(user.getUserType());
        loginUser.setOpenid(openid);
        // Generate token
        LoginHelper.loginByDevice(loginUser, DeviceType.XCX);

        recordLogininfor(user.getUserName(), Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success"));
        recordLoginInfo(user.getUserId(), user.getUserName());
        return StpUtil.getTokenValue();
    }

    /**
     * exit
     */
    public void logout() {
        try {
            LoginUser loginUser = LoginHelper.getLoginUser();
            StpUtil.logout();
            recordLogininfor(loginUser.getUsername(), Constants.LOGOUT, MessageUtils.message("user.logout.success"));
        } catch (NotLoginException ignored) {
        }
    }
    /**
     * exit
     */
    public void logout(String token) {
        try {
//            LoginUser loginUser = LoginHelper.getLoginUser();
//            StpUtil.logout();
//            recordLogininfor(loginUser.getUsername(), Constants.LOGOUT, MessageUtils.message("user.logout.success"));
        } catch (NotLoginException ignored) {
        }
    }

    /**
     * record info
     *
     * @param username user
     * @param status
     * @param message
     */
    private void recordLogininfor(String username, String status, String message) {
        LogininforEvent logininforEvent = new LogininforEvent();
        logininforEvent.setUsername(username);
        logininforEvent.setStatus(status);
        logininforEvent.setMessage(message);
        logininforEvent.setRequest(ServletUtils.getRequest());
        SpringUtils.context().publishEvent(logininforEvent);
    }

    /**
     * Validate
     */
    private boolean validateSmsCode(String phonenumber, String smsCode) {
        String code = RedisUtils.getCacheObject(CacheConstants.CAPTCHA_CODE_KEY + phonenumber);
        if (StringUtils.isBlank(code)) {
            recordLogininfor(phonenumber, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire"));
            throw new CaptchaExpireException();
        }
        return code.equals(smsCode);
    }

    /**
     * Validate
     */
    private boolean validateEmailCode(String email, String emailCode) {
        String code = RedisUtils.getCacheObject(CacheConstants.CAPTCHA_CODE_KEY + email);
        if (StringUtils.isBlank(code)) {
            recordLogininfor(email, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire"));
            throw new CaptchaExpireException();
        }
        return code.equals(emailCode);
    }

    /**
     * Validate
     *
     * @param username user
     * @param code
     * @param uuid
     */
    public void validateCaptcha(String username, String code, String uuid) {
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.defaultString(uuid, "");
        String captcha = RedisUtils.getCacheObject(verifyKey);
        RedisUtils.deleteObject(verifyKey);
        if (captcha == null) {
            recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire"));
            throw new CaptchaExpireException();
        }
        if (!code.equalsIgnoreCase(captcha)) {
            recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.error"));
            throw new CaptchaException();
        }
    }

    private SysUser loadUserByUsername(String username) {
            SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
            .select(SysUser::getUserName, SysUser::getStatus)
            .eq(SysUser::getUserName, username));
        if (ObjectUtil.isNull(user)) {
            log.info("登录用户：{} 不存在.", username);
            throw new UserException("user.not.exists", username);
        } else if (isLoginBlockedStatus(user.getStatus())) {
            log.info("登录用户：{} 已被停用.", username);
            throw new UserException("user.blocked", username);
        }
        return userMapper.selectUserByUserName(username);
    }

    private SysUser loadUserByPhonenumber(String phonenumber) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
            .select(SysUser::getPhonenumber, SysUser::getStatus)
            .eq(SysUser::getPhonenumber, phonenumber));
        if (ObjectUtil.isNull(user)) {
            log.info("登录用户：{} 不存在.", phonenumber);
            throw new UserException("user.not.exists", phonenumber);
        } else if (isLoginBlockedStatus(user.getStatus())) {
            log.info("登录用户：{} 已被停用.", phonenumber);
            throw new UserException("user.blocked", phonenumber);
        }
        return userMapper.selectUserByPhonenumber(phonenumber);
    }

    private SysUser loadUserByEmail(String email) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
            .select(SysUser::getPhonenumber, SysUser::getStatus)
            .eq(SysUser::getEmail, email));
        if (ObjectUtil.isNull(user)) {
            log.info("登录用户：{} 不存在.", email);
            throw new UserException("user.not.exists", email);
        } else if (isLoginBlockedStatus(user.getStatus())) {
            log.info("登录用户：{} 已被停用.", email);
            throw new UserException("user.blocked", email);
        }
        return userMapper.selectUserByEmail(email);
    }

    private SysUser loadUserByOpenid(String openid) {
        // openid Query user not user Process user
        // todo userService.selectUserByOpenid(openid);
        SysUser user = new SysUser();
        if (ObjectUtil.isNull(user)) {
            log.info("登录用户：{} 不存在.", openid);
            // todo user in
        } else if (isLoginBlockedStatus(user.getStatus())) {
            log.info("登录用户：{} 已被停用.", openid);
            // todo user already
        }
        return user;
    }

    /**
     * Build user
     */
    private LoginUser buildLoginUser(SysUser user) {
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getUserId());
        loginUser.setTenantId(user.getTenantId());
        loginUser.setDeptId(user.getDeptId());
        loginUser.setUsername(user.getUserName());
        loginUser.setNickName(user.getUserName());
        loginUser.setUserType(user.getUserType());
        loginUser.setMenuPermission(permissionService.getMenuPermission(user));
        loginUser.setRolePermission(permissionService.getRolePermission(user));
        loginUser.setDeptName(ObjectUtil.isNull(user.getDept()) ? "" : user.getDept().getDeptName());
        List<RoleDTO> roles = BeanUtil.copyToList(user.getRoles(), RoleDTO.class);
        loginUser.setRoles(roles);
        return loginUser;
    }

    static boolean isLoginBlockedStatus(String status) {
        return "1".equals(status)
            || "disabled".equalsIgnoreCase(status)
            || "disable".equalsIgnoreCase(status)
            || "blocked".equalsIgnoreCase(status)
            || "停用".equals(status);
    }

    /**
     * record info
     *
     * @param userId user ID
     */
    public void recordLoginInfo(String userId, String username) {
        SysUser sysUser = new SysUser();
        sysUser.setUserId(userId);
        sysUser.setLoginIp(ServletUtils.getClientIP());
        sysUser.setLoginDate(DateUtils.getTime());
        sysUser.setUpdateBy(username);
        try {
            userMapper.updateById(sysUser);
        } catch (Exception ex) {
            log.warn("登录信息回写失败，忽略本次兼容登录回写: {}", ex.getMessage());
        }
    }

    /**
     * Validate
     */
    private void checkLogin(LoginType loginType, String username, Supplier<Boolean> supplier) {
        String errorKey = CacheConstants.PWD_ERR_CNT_KEY + username;
        String loginFail = Constants.LOGIN_FAIL;

        // Get user ( Custom : key + username + ip)
        Integer errorNumber = RedisUtils.getCacheObject(errorKey);
        //
        if (ObjectUtil.isNotNull(errorNumber) && errorNumber.equals(maxRetryCount)) {
            recordLogininfor(username, loginFail, MessageUtils.message(loginType.getRetryLimitExceed(), maxRetryCount, lockTime));
            throw new UserException(loginType.getRetryLimitExceed(), maxRetryCount, lockTime);
        }

        if (supplier.get()) {
            // whether
            errorNumber = ObjectUtil.isNull(errorNumber) ? 1 : errorNumber + 1;
            //
            if (errorNumber.equals(maxRetryCount)) {
                RedisUtils.setCacheObject(errorKey, errorNumber, Duration.ofMinutes(lockTime));
                recordLogininfor(username, loginFail, MessageUtils.message(loginType.getRetryLimitExceed(), maxRetryCount, lockTime));
                throw new UserException(loginType.getRetryLimitExceed(), maxRetryCount, lockTime);
            } else {
                // not
                RedisUtils.setCacheObject(errorKey, errorNumber);
                recordLogininfor(username, loginFail, MessageUtils.message(loginType.getRetryLimitCount(), errorNumber));
                throw new UserException(loginType.getRetryLimitCount(), errorNumber);
            }
        }

        // successfully null / empty
        RedisUtils.deleteObject(errorKey);
    }
}
