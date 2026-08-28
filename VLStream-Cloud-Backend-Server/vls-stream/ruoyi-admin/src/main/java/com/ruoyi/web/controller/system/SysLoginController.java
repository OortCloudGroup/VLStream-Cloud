/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.web.controller.system;

import cn.dev33.satoken.annotation.SaIgnore;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.EmailLoginBody;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.common.core.domain.model.SmsLoginBody;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.system.domain.vo.RouterVo;
import com.ruoyi.system.service.ISysMenuService;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.system.service.SysLoginService;
import com.ruoyi.system.service.SysPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import javax.validation.constraints.NotBlank;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 *
 * @author Lion Li
 */
@Validated
@RequiredArgsConstructor
//@RestController
public class SysLoginController {

    private final SysLoginService loginService;
    private final ISysMenuService menuService;
    private final ISysUserService userService;
    private final SysPermissionService permissionService;
    /**
     * method
     *
     * @param loginBody info
     * @return
     */
    @SaIgnore
    @PostMapping("/login")
    public R<Map<String, Object>> login(@Validated @RequestBody LoginBody loginBody) {
        Map<String, Object> ajax = new HashMap<>();
        // Generate
        String token = loginService.login(loginBody.getUsername(), loginBody.getPassword(), loginBody.getCode(),
            loginBody.getUuid());
        ajax.put(Constants.TOKEN, token);
        return R.ok(ajax);
    }

    /**
     *
     *
     * @param smsLoginBody info
     * @return
     */
    @SaIgnore
    @PostMapping("/smsLogin")
    public R<Map<String, Object>> smsLogin(@Validated @RequestBody SmsLoginBody smsLoginBody) {
        Map<String, Object> ajax = new HashMap<>();
        // Generate
        String token = loginService.smsLogin(smsLoginBody.getPhonenumber(), smsLoginBody.getSmsCode());
        ajax.put(Constants.TOKEN, token);
        return R.ok(ajax);
    }

    /**
     *
     *
     * @param body info
     * @return
     */
    @PostMapping("/emailLogin")
    public R<Map<String, Object>> emailLogin(@Validated @RequestBody EmailLoginBody body) {
        Map<String, Object> ajax = new HashMap<>();
        // Generate
        String token = loginService.emailLogin(body.getEmail(), body.getEmailCode());
        ajax.put(Constants.TOKEN, token);
        return R.ok(ajax);
    }

    /**
     * ( )
     *
     * @param xcxCode code
     * @return
     */
    @SaIgnore
    @PostMapping("/xcxLogin")
    public R<Map<String, Object>> xcxLogin(@NotBlank(message = "{xcx.code.not.blank}") String xcxCode) {
        Map<String, Object> ajax = new HashMap<>();
        // Generate
        String token = loginService.xcxLogin(xcxCode);
        ajax.put(Constants.TOKEN, token);
        return R.ok(ajax);
    }

    /**
     * exit
     */
    @SaIgnore
    @PostMapping("/logout")
    public R<Void> logout(@RequestHeader("Authorization")String token) {
        loginService.logout(token);
        return R.ok("退出成功");
    }

    /**
     * Get userinfo
     *
     * @return userinfo( tockenGet userinfo)
     */
    @GetMapping("getInfo")
    public R<Map<String, Object>> getInfo(@RequestHeader("Authorization")String token) {
//        LoginUser loginUser = LoginHelper.getLoginUser();
        SysUser user = RedisUtils.getCacheObject(token);
        if(user == null) {
            throw new RuntimeException("未找到用户缓存信息");
        }
//        SysUser user = userService.selectUserById(loginUser.getUserId());
        Map<String, Object> ajax = new HashMap<>();
        ajax.put("user", user);
        ajax.put("roles", permissionService.getRolePermission(user));
        ajax.put("permissions", permissionService.getMenuPermission(user));
          return R.ok(ajax);
    }

    /**
     * Get info
     *
     * @return info
     */
    @GetMapping("getRouters")
    public R<List<RouterVo>> getRouters(@RequestHeader("Authorization")String token) {
//        Long userId = LoginHelper.getUserId();
//        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
        SysUser user = RedisUtils.getCacheObject(token);
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(user.getUserId());
        return R.ok(menuService.buildMenus(menus));
    }

}
