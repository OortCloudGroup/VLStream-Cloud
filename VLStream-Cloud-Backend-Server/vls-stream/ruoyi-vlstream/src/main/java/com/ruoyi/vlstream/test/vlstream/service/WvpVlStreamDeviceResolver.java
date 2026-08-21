/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.config.VlsNativeDeviceProperties;
import com.ruoyi.vlstream.test.vlstream.config.WvpVlStreamDeviceProperties;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/** Resolves the authoritative VLStream device from the mandatory WVP service. */
@Service
public class WvpVlStreamDeviceResolver {

	private final WvpVlStreamDeviceProperties properties;
	private final VlsNativeDeviceProperties nativeDeviceProperties;
	private final VlsEventReportApplicationService eventReportApplicationService;
	private final RestTemplate restTemplate;

	@Autowired
	public WvpVlStreamDeviceResolver(WvpVlStreamDeviceProperties properties,
		VlsNativeDeviceProperties nativeDeviceProperties,
		VlsEventReportApplicationService eventReportApplicationService) {
		this(properties, nativeDeviceProperties, eventReportApplicationService,
			createRestTemplate(properties));
	}

	WvpVlStreamDeviceResolver(WvpVlStreamDeviceProperties properties,
		VlsNativeDeviceProperties nativeDeviceProperties,
		VlsEventReportApplicationService eventReportApplicationService,
		RestTemplate restTemplate) {
		this.properties = properties;
		this.nativeDeviceProperties = nativeDeviceProperties;
		this.eventReportApplicationService = eventReportApplicationService;
		this.restTemplate = restTemplate;
	}

	public DeviceInfo resolve(String deviceId) {
		if (StringUtils.isBlank(deviceId)) {
			throw new ServiceException("设备编号不能为空");
		}
		URI uri = deviceUri(deviceId);
		try {
			ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
			return mapResponse(deviceId, response.getBody());
		} catch (RestClientException exception) {
			throw new ServiceException("WVP设备服务不可用，无法校验VLStream设备："
				+ StringUtils.defaultIfBlank(exception.getMessage(), "连接失败"));
		}
	}

	private DeviceInfo mapResponse(String requestedDeviceId, String body) {
		JSONObject result;
		try {
			result = JSONUtil.parseObj(body);
		} catch (RuntimeException exception) {
			throw new ServiceException("WVP设备查询返回格式不正确");
		}
		Integer code = result.getInt("code");
		if (code == null || code != 200) {
			String message = StringUtils.defaultIfBlank(result.getStr("msg"), "查询失败");
			if (Integer.valueOf(404).equals(code)) {
				throw new ServiceException("WVP不存在该VLStream设备：" + requestedDeviceId);
			}
			throw new ServiceException("WVP设备校验失败：" + message);
		}
		JSONObject data = result.getJSONObject("data");
		String returnedDeviceId = data == null ? null : data.getStr("deviceId");
		if (!StringUtils.equals(requestedDeviceId, returnedDeviceId)) {
			throw new ServiceException("WVP设备查询结果与请求设备不一致");
		}
		DeviceInfo device = new DeviceInfo();
		device.setDeviceId(returnedDeviceId);
		device.setDeviceName(StringUtils.defaultIfBlank(data.getStr("deviceName"), returnedDeviceId));
		device.setTenantId(resolveTenantId());
		return device;
	}

	private String resolveTenantId() {
		String tenantId = eventReportApplicationService.isMultiTenant()
			? nativeDeviceProperties.getMultiTenantDefaultTenantId()
			: nativeDeviceProperties.getDefaultTenantId();
		if (StringUtils.isBlank(tenantId)) {
			throw new ServiceException("WVP VLStream设备默认租户未配置");
		}
		return tenantId;
	}

	private URI deviceUri(String deviceId) {
		String baseUrl = StringUtils.removeEnd(StringUtils.trimToEmpty(properties.getBaseUrl()), "/");
		if (StringUtils.isBlank(baseUrl)) {
			throw new ServiceException("WVP内部服务地址不能为空");
		}
		try {
			return UriComponentsBuilder.fromHttpUrl(baseUrl)
				.pathSegment("internal", "vlstream", "device", deviceId)
				.build().encode().toUri();
		} catch (IllegalArgumentException exception) {
			throw new ServiceException("WVP内部服务地址配置不正确");
		}
	}

	private static RestTemplate createRestTemplate(WvpVlStreamDeviceProperties properties) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Math.max(500, properties.getConnectTimeoutMillis()));
		factory.setReadTimeout(Math.max(500, properties.getReadTimeoutMillis()));
		return new RestTemplate(factory);
	}
}
