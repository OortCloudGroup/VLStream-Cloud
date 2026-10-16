package com.ruoyi.web.service;

import cn.dev33.satoken.secure.BCrypt;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.enums.TenantType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.config.properties.TokenProperties;
import com.ruoyi.system.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Local credentials never become credentials for a platform SSO identity. */
@Service
@RequiredArgsConstructor
public class LocalAccountPasswordService {
    private final TokenProperties tokenProperties;
    private final ISysUserService userService;

    public boolean isLocalAccount(SysUser user) {
        return TenantType.SIGNLE_TENANT.getType().equalsIgnoreCase(tokenProperties.getTenantType())
            && user != null && StringUtils.isBlank(user.getPlatformUserId());
    }

    public void requireLocalAccount(SysUser user) {
        if (!isLocalAccount(user)) {
            throw new ServiceException("此账号由统一身份平台管理，请在统一身份平台设置或修改密码");
        }
    }

    public void validateNewPassword(String password) {
        if (StringUtils.isBlank(password) || password.length() < UserConstants.PASSWORD_MIN_LENGTH
            || password.length() > UserConstants.PASSWORD_MAX_LENGTH) {
            throw new ServiceException("密码长度必须为 " + UserConstants.PASSWORD_MIN_LENGTH
                + " 至 " + UserConstants.PASSWORD_MAX_LENGTH + " 个字符");
        }
    }

    public boolean changeOwnPassword(String userId, String oldPassword, String newPassword) {
        SysUser user = userService.selectUserById(userId);
        requireLocalAccount(user);
        validateNewPassword(newPassword);
        if (StringUtils.isBlank(oldPassword) || StringUtils.isBlank(user.getPassword())
            || !BCrypt.checkpw(oldPassword, user.getPassword())) {
            throw new ServiceException("修改密码失败，旧密码错误");
        }
        if (BCrypt.checkpw(newPassword, user.getPassword())) {
            throw new ServiceException("新密码不能与旧密码相同");
        }
        SysUser passwordUpdate = new SysUser();
        passwordUpdate.setUserId(user.getUserId());
        passwordUpdate.setPassword(BCrypt.hashpw(newPassword));
        return userService.resetPwd(passwordUpdate) > 0;
    }
}
