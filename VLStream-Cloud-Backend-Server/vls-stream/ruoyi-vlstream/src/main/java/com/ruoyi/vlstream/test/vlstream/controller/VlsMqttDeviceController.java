/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.vlstream.test.vlstream.mapper.MqttDeviceMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.MqttDeviceStreamMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FirmwareDeployRequest;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FirmwareDeployTaskView;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.MqttDeviceDetailView;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.MqttDevice;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.MqttDeviceStream;
import com.ruoyi.vlstream.test.vlstream.service.FirmwareDeploymentService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springblade.core.tool.api.R;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Legacy device and firmware management; preview is owned by WVP. */
@RestController
@RequestMapping("/vlsMqttDevice")
@ConditionalOnProperty(value = "vlstream.native-device.legacy-enabled", havingValue = "true")
@RequiredArgsConstructor
public class VlsMqttDeviceController {

	private final MqttDeviceMapper deviceMapper;
	private final MqttDeviceStreamMapper streamMapper;
	private final FirmwareDeploymentService firmwareDeploymentService;

	@GetMapping("/page")
	public R<Page<MqttDevice>> page(@RequestParam(defaultValue = "1") long current,
		@RequestParam(defaultValue = "10") long size,
		@RequestParam(required = false) String keyword,
		@RequestParam(required = false) Boolean online) {
		LambdaQueryWrapper<MqttDevice> query = new LambdaQueryWrapper<MqttDevice>()
			.eq(MqttDevice::getIsDeleted, 0)
			.eq(online != null, MqttDevice::getOnline, online)
			.and(StringUtils.isNotBlank(keyword), wrapper -> wrapper
				.like(MqttDevice::getDeviceName, keyword)
				.or().like(MqttDevice::getDeviceId, keyword)
				.or().like(MqttDevice::getDeviceSerial, keyword))
			.orderByDesc(MqttDevice::getLastHeartbeatTime);
		return R.data(deviceMapper.selectPage(new Page<>(current, Math.min(size, 100)), query));
	}

	@GetMapping("/{deviceId}/streams")
	public R<List<MqttDeviceStream>> streams(@PathVariable Long deviceId) {
		return R.data(streamMapper.selectList(new LambdaQueryWrapper<MqttDeviceStream>()
			.eq(MqttDeviceStream::getDeviceRowId, deviceId)
			.eq(MqttDeviceStream::getAvailable, true)
			.eq(MqttDeviceStream::getIsDeleted, 0)
			.orderByDesc(MqttDeviceStream::getIsDefault)
			.orderByAsc(MqttDeviceStream::getChannelId, MqttDeviceStream::getStreamType)));
	}

	@GetMapping("/{deviceId}/detail")
	public R<MqttDeviceDetailView> detail(@PathVariable Long deviceId) {
		return R.data(firmwareDeploymentService.detail(deviceId));
	}

	@SaCheckPermission("vls:firmware:deploy")
	@PostMapping("/{deviceId}/firmware-upgrades")
	public R<FirmwareDeployTaskView> deployFirmware(@PathVariable Long deviceId,
		@RequestBody FirmwareDeployRequest request) {
		return R.data(firmwareDeploymentService.deploy(deviceId,
			request == null ? null : request.getFirmwareId()));
	}

	@SaCheckPermission("vls:firmware:deploy")
	@PostMapping("/{deviceId}/firmware-upgrades/{requestId}/cancel")
	public R<FirmwareDeployTaskView> cancelFirmwareUpgrade(@PathVariable Long deviceId,
		@PathVariable String requestId) {
		return R.data(firmwareDeploymentService.cancel(deviceId, requestId));
	}
}
