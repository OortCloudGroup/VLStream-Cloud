/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.core.domain.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * role
 *
 * @author Lion Li
 */

@Data
@NoArgsConstructor
public class RoleDTO implements Serializable {

    /**
     * role ID
     */
    private Long roleId;

    /**
     * role name
     */
    private String roleName;

    /**
     * role
     */
    private String roleKey;

    /**
     * data (1: all data ; 2: Customdata ; 3: departmentdata ; 4: department data ; 5: data )
     */
    private String dataScope;

}
