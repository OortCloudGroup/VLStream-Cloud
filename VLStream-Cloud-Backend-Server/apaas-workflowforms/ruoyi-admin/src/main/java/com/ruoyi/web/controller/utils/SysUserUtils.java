package com.ruoyi.web.controller.utils;

import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.redis.RedisUtils;

public class SysUserUtils {

    /**
     * 通过token获取用户信息
     * @param token
     * @return
     */
    public static SysUser getSysUser(String token) {
        SysUser user = RedisUtils.getCacheObject(token);
        if(user == null) {
            throw new RuntimeException("未找到用户缓存信息");
        }
        System.out.println(" 用户缓存信息 " + user);
        return user;
    }
}
