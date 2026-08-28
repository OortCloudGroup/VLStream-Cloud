/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.core.service;

import com.ruoyi.common.core.domain.entity.SysUser;

import java.util.List;

/**
 * userservice
 *
 * @author Lion Li
 */
public interface UserService {

    /**
     * user IDQuery user
     *
     * @param userId user ID
     * @return user
     */
    String selectUserNameById(String userId);

    /**
     * user IDQuery user
     *
     * @param userId user ID
     * @return
     */
    String selectIdCardById(String userId);

//    /**
// * user IDQuery user
//     *
// * @param userId user ID
// * @return user
//     */
//    String selectUserNameById(String userId);

    List<SysUser> getLeaders(String userId);

}
