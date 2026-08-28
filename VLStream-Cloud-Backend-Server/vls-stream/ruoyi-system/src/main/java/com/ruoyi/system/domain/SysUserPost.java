/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * user and sys_user_post
 *
 * @author Lion Li
 */

@Data
@TableName("sys_user_post")
public class SysUserPost {

    /**
     * user ID
     */
    @TableId(type = IdType.INPUT)
    private String userId;

    /**
     * ID
     */
    private Long postId;

}
