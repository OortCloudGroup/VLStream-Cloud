/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.constant;

/**
 * user info
 *
 * @author ruoyi
 */
public interface UserConstants {

    /**
     * user
     */
    String SYS_USER = "SYS_USER";

    /**
     *
     */
    String NORMAL = "0";

    /**
     *
     */
    String EXCEPTION = "1";

    /**
     * user
     */
    String USER_NORMAL = "0";

    /**
     * user
     */
    String USER_DISABLE = "1";

    /**
     * role
     */
    String ROLE_NORMAL = "0";

    /**
     * role
     */
    String ROLE_DISABLE = "1";

    /**
     * department
     */
    String DEPT_NORMAL = "0";

    /**
     * department
     */
    String DEPT_DISABLE = "1";

    /**
     * dict
     */
    String DICT_NORMAL = "0";

    /**
     * whether to ( is )
     */
    String YES = "Y";

    /**
     * whether menu ( is )
     */
    String YES_FRAME = "0";

    /**
     * whether menu ( )
     */
    String NO_FRAME = "1";

    /**
     * menu
     */
    String MENU_NORMAL = "0";

    /**
     * menu
     */
    String MENU_DISABLE = "1";

    /**
     * menu ( )
     */
    String TYPE_DIR = "M";

    /**
     * menu (menu)
     */
    String TYPE_MENU = "C";

    /**
     * menu (button)
     */
    String TYPE_BUTTON = "F";

    /**
     * Layoutcomponent
     */
    String LAYOUT = "Layout";

    /**
     * ParentViewcomponent
     */
    String PARENT_VIEW = "ParentView";

    /**
     * InnerLinkcomponent
     */
    String INNER_LINK = "InnerLink";

    /**
     * user
     */
    int USERNAME_MIN_LENGTH = 2;
    int USERNAME_MAX_LENGTH = 20;

    /**
     *
     */
    int PASSWORD_MIN_LENGTH = 5;
    int PASSWORD_MAX_LENGTH = 20;

    /**
     * administratorID
     */
    String ADMIN_ID = "1";

    /**
     * administrator id
     */
    Long AD_ID = 1L;

    /**
     * role id
     */
    Long COMMON_ID = 2L;

}
