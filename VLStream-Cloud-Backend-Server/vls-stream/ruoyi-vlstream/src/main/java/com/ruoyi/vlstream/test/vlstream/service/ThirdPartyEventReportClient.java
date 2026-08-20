/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.vlstream.test.vlstream.config.VlsEventReportProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/** Anonymous HTTP client for the external location event endpoint. */
@Component
@RequiredArgsConstructor
public class ThirdPartyEventReportClient {

	private final VlsEventReportProperties properties;

	public void report(JSONObject eventPayload, String idempotencyKey) {
		if (StringUtils.isBlank(properties.getUrl())) {
			throw new IllegalStateException("第三方事件上报地址未配置");
		}
		JSONObject requestBody = new JSONObject();
		JSONArray reports = new JSONArray();
		reports.add(eventPayload);
		requestBody.set("event_report", reports);
		int timeout = Math.max(1000, properties.getTimeoutMillis() == null ? 5000 : properties.getTimeoutMillis());
		try (HttpResponse response = HttpRequest.post(properties.getUrl())
			.header("Content-Type", "application/json")
			.header("X-Idempotency-Key", idempotencyKey)
			.body(requestBody.toString())
			.timeout(timeout)
			.execute()) {
			if (!response.isOk()) {
				throw new IllegalStateException("第三方事件接口 HTTP 状态异常：" + response.getStatus());
			}
			validateBusinessResponse(response.body());
		}
	}

	private void validateBusinessResponse(String body) {
		if (StringUtils.isBlank(body) || !JSONUtil.isTypeJSONObject(body)) {
			return;
		}
		JSONObject result = JSONUtil.parseObj(body);
		Boolean success = result.getBool("success");
		if (Boolean.FALSE.equals(success)) {
			throw new IllegalStateException(message(result));
		}
		Integer code = result.getInt("code");
		if (code != null && code.intValue() != 0 && code.intValue() != 200) {
			throw new IllegalStateException(message(result));
		}
	}

	private String message(JSONObject result) {
		return StringUtils.defaultIfBlank(result.getStr("msg"),
			StringUtils.defaultIfBlank(result.getStr("message"), "第三方事件接口业务处理失败"));
	}
}
