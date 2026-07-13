/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

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
