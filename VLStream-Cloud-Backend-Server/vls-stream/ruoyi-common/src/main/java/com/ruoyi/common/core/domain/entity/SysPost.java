/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.core.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.apache.ibatis.type.Alias;

/**
 * ( )
 *
 * @author Lion Li
 */

@Data
@TableName("sys_post_view")
@Alias("CommonSysPost")
public class SysPost {

    /**
     * ( )
     */
    @TableField("oort_jobname")
    private String oortJobname;



    /**
     * etc. ( etc. )
     */
    @TableField("oort_level")
    private Integer oortLevel;

    /**
     * create time
     */
    @TableField("oort_tcreate")
    private Long oortTcreate;

    /**
     * Update
     */
    @TableField("oort_tupdate")
    private Long oortTupdate;

    /**
     * whether Delete 0 1 is
     */
    @TableField("oort_tdelete")
    private Integer oortTdelete;

    // new ---------

    /**
     * ID
     */
    @TableId("post_id")
    private String postId;

    /**
     * user ID
     */
    @TableField(value = "user_id")
    private String userId;

    /**
     *
     */
    @TableField("name")
    private String name;

    /**
     * tenant ID
     */
    @TableField(value = "tenant_id")
    private String tenantId;


    /**
     * ID
     */
    @TableField("ppost_id")
    private String  ppostId;

    /**
     *
     */
    @TableField("code")
    private String  code;

    /**
     *
     */
    @TableField("type")
    private String  type;


}
