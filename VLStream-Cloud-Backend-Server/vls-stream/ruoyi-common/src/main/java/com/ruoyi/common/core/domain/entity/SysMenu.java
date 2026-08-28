/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.core.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ruoyi.common.core.domain.TreeEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * menu sys_menu
 *
 * @author Lion Li
 */

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends TreeEntity<SysMenu> {

    /**
     * menu ID
     */
    @TableId(value = "menu_id")
    private Long  menuId;

    /**
     * menu name
     */
    @NotBlank(message = "菜单名称不能为空")
    @Size(min = 0, max = 50, message = "菜单名称长度不能超过{max}个字符")
    private String menuName;

    /**
     *
     */
    @NotNull(message = "显示顺序不能为空")
    private Integer orderNum;

    /**
     *
     */
    @Size(min = 0, max = 200, message = "路由地址不能超过{max}个字符")
    private String path;

    /**
     * component
     */
    @Size(min = 0, max = 200, message = "组件路径不能超过{max}个字符")
    private String component;

    /**
     * parameter
     */
    private String queryParam;

    /**
     * whether to (0 is 1 )
     */
    private String isFrame;

    /**
     * whether (0 1 )
     */
    private String isCache;

    /**
     * (M Cmenu Fbutton)
     */
    @NotBlank(message = "菜单类型不能为空")
    private String menuType;

    /**
     * (0 1 )
     */
    private String visible;

    /**
     * menuStatus (0 normal 1 disabled)
     */
    private String status;

    /**
     *
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Size(min = 0, max = 100, message = "权限标识长度不能超过{max}个字符")
    private String perms;

    /**
     * menu
     */
    private String icon;

    /**
     * remark
     */
    private String remark;

}
