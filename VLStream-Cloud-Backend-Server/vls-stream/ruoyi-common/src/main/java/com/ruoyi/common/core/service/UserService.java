package com.ruoyi.common.core.service;

import com.ruoyi.common.core.domain.entity.SysUser;

import java.util.List;

/**
 * 通用 用户服务
 *
 * @author Lion Li
 */
public interface UserService {

    /**
     * 通过用户ID查询用户账户
     *
     * @param userId 用户ID
     * @return 用户账户
     */
    String selectUserNameById(String userId);

    /**
     * 通过用户ID查询用户身份证
     *
     * @param userId 用户ID
     * @return
     */
    String selectIdCardById(String userId);

//    /**
//     * 通过用户ID查询用户昵称
//     *
//     * @param userId 用户ID
//     * @return 用户昵称
//     */
//    String selectUserNameById(String userId);

    List<SysUser> getLeaders(String userId);

}
