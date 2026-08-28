/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.core.domain.model;

import com.ruoyi.common.core.domain.dto.RoleDTO;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * user
 *
 * @author Lion Li
 */

@Data
@NoArgsConstructor
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * user ID
     */
    private String userId;

    /**
     * current will tenant ID.
     */
    private String tenantId;

    /**
     * department ID
     */
    private String deptId;

    /**
     * department
     */
    private String deptName;

    /**
     * user
     */
    private String token;

    /**
     * user
     */
    private String userType;

    /**
     * id
     */
    private String loginId;

    /**
     *
     */
    private Long loginTime;

    /**
     *
     */
    private Long expireTime;

    /**
     * IP
     */
    private String ipaddr;

    /**
     *
     */
    private String loginLocation;

    /**
     *
     */
    private String browser;

    /**
     * operation
     */
    private String os;

    /**
     * menu
     */
    private Set<String> menuPermission;

    /**
     * role
     */
    private Set<String> rolePermission;

    /**
     * user
     */
    private String username;

    /**
     * user
     */
    private String nickName;

    /**
     * roleobject
     */
    private List<RoleDTO> roles;

    /**
     * data current role ID
     */
    private Long roleId;

    /**
     * Get id
     */
    public String getLoginId() {
        if (userType == null) {
            throw new IllegalArgumentException("用户类型不能为空");
        }
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        return userType + ":" + userId;
    }

}
