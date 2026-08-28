/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.oss.constant;

import java.util.Arrays;
import java.util.List;

/**
 * object
 *
 * @author Lion Li
 */
public interface OssConstant {

    /**
     * configurationKEY
     */
    String DEFAULT_CONFIG_KEY = "sys_oss:default_config";

    /**
     * Key
     */
    String PEREVIEW_LIST_RESOURCE_KEY = "sys.oss.previewListResource";

    /**
     * dataids
     */
    List<Long> SYSTEM_DATA_IDS = Arrays.asList(1L, 2L, 3L, 4L);

    /**
     * service
     */
    String[] CLOUD_SERVICE = new String[] {"aliyun", "qcloud", "qiniu", "obs"};

    /**
     * https
     */
    String IS_HTTPS = "Y";

}
