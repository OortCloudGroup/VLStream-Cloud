/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.sms.core;

import com.ruoyi.sms.entity.SmsResult;

import java.util.Map;

/**
 *
 *
 * @author Lion Li
 * @version 4.2.0
 */
public interface SmsTemplate {

    /**
     *
     *
     * @param phones ( )
     * @param templateId id
     * @param param parameter
     * variable : code=1234
     * variable : 1=1234, 1 to parameter
     */
    SmsResult send(String phones, String templateId, Map<String, String> param);

}
