/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.constant;

/**
 * key
 *
 * @author ruoyi
 */
public interface CacheConstants {

    /**
     * in user redis key
     */
    String ONLINE_TOKEN_KEY = "online_tokens:";

    /**
     * redis key
     */
    String CAPTCHA_CODE_KEY = "captcha_codes:";

    /**
     * parameter cache key
     */
    String SYS_CONFIG_KEY = "sys_config:";

    /**
     * dict cache key
     */
    String SYS_DICT_KEY = "sys_dict:";

    /**
     * redis key
     */
    String REPEAT_SUBMIT_KEY = "repeat_submit:";

    /**
     * redis key
     */
    String RATE_LIMIT_KEY = "rate_limit:";

    /**
     * redis key
     */
    String PWD_ERR_CNT_KEY = "pwd_err_cnt:";
}
