/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;

/** Handles one mainBizType/subBizType combination from the VLS device bus. */
public interface VlsMqttMessageHandler {

	boolean supports(String mainBizType, String subBizType);

	/**
	 * @return the business acknowledgement to publish, or {@code null} when no reply is required
	 */
	JSONObject handle(JSONObject envelope, String rawPayload);
}
