/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.enums;

import com.ruoyi.common.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * data
 * <p>
 * method spel
 * <p>
 * data user current user LoginUser
 * data {@link com.ruoyi.common.helper.DataPermissionHelper} operation
 * service sdss data service SysDataScopeService
 * Customservice sdss
 *
 * @author Lion Li
 * @version 3.5.0
 */
@Getter
@AllArgsConstructor
public enum DataScopeType {

    /**
     * full data
     */
    ALL("1", "", ""),

    /**
     * data
     */
    CUSTOM("2", " #{#deptName} IN ( #{@sdss.getRoleCustom( #user.roleId )} ) ", ""),

    /**
     * departmentdata
     */
    DEPT("3", " #{#deptName} = #{#user.deptId} ", ""),

    /**
     * department data
     */
    DEPT_AND_CHILD("4", " #{#deptName} IN ( #{@sdss.getDeptAndChild( #user.deptId )} )", ""),

    /**
     * data
     */
    SELF("5", " #{#userName} = #{#user.userId} ", " 1 = 0 ");

    private final String code;

    /**
     * method spel
     */
    private final String sqlTemplate;

    /**
     * sqlTemplate fill
     */
    private final String elseSql;

    public static DataScopeType findCode(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        for (DataScopeType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
