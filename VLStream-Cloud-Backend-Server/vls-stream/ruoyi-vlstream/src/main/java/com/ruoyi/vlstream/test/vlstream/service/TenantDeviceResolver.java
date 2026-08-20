/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.baomidou.mybatisplus.core.plugins.IgnoreStrategy;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsDeviceInfoMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/** Resolves a device callback to exactly one trusted tenant. */
@Service
@RequiredArgsConstructor
public class TenantDeviceResolver {

	private final VlsDeviceInfoMapper deviceInfoMapper;

	public DeviceInfo resolveUnique(String deviceId) {
		if (StringUtils.isBlank(deviceId)) {
			throw new ServiceException("设备编号不能为空");
		}
		List<DeviceInfo> devices;
		InterceptorIgnoreHelper.handle(IgnoreStrategy.builder().tenantLine(true).build());
		try {
			devices = deviceInfoMapper.selectList(new LambdaQueryWrapper<DeviceInfo>()
				.eq(DeviceInfo::getDeviceId, deviceId)
				.eq(DeviceInfo::getIsDeleted, 0)
				.last("limit 2"));
		} finally {
			InterceptorIgnoreHelper.clearIgnoreStrategy();
		}
		if (devices == null || devices.isEmpty()) {
			throw new ServiceException("平台不存在该设备：" + deviceId);
		}
		if (devices.size() != 1) {
			throw new ServiceException("设备编号未能唯一映射租户：" + deviceId);
		}
		DeviceInfo device = devices.get(0);
		if (StringUtils.isBlank(device.getTenantId())) {
			throw new ServiceException("设备未配置所属租户：" + deviceId);
		}
		return device;
	}
}
