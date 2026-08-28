/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.constant;

/**
 *
 * <p>
 * key to cacheNames#ttl#maxIdleTime#maxSize
 * <p>
 * ttl if Set to 0 to 0
 * maxIdleTime null / empty LRUalgorithm null / empty data if Set to 0 to 0
 * maxSize LRUalgorithm data if Set to 0 to 0
 * <p>
 * sub : test#60s、test#0#60s、test#0#1m#1000、test#1h#0#500
 *
 * @author Lion Li
 */
public interface CacheNames {

    /**
     *
     */
    String DEMO_CACHE = "demo:cache#60s#10m#20";

    /**
     * configuration
     */
    String SYS_CONFIG = "sys_config";

    /**
     * datadict
     */
    String SYS_DICT = "sys_dict";

    /**
     * user
     */
    String SYS_USER_NAME = "sys_user_name#30d";

    /**
     * user
     */
    String SYS_NICK_NAME = "sys_nick_name#30d";

    /**
     * department
     */
    String SYS_DEPT = "sys_dept#30d";

    /**
     * OSS
     */
    String SYS_OSS = "sys_oss#30d";

    /**
     * OSSconfiguration
     */
    String SYS_OSS_CONFIG = "sys_oss_config";

    /**
     * in user
     */
    String ONLINE_TOKEN = "online_tokens";

    /**
     * full redis key ( key)
     */
    String GLOBAL_REDIS_KEY = "global:";

    /**
     * data redis key
     */
    String DATA_SCOPE_AUTH_CODE_KEY = GLOBAL_REDIS_KEY + "data_scope_codes:";

}
