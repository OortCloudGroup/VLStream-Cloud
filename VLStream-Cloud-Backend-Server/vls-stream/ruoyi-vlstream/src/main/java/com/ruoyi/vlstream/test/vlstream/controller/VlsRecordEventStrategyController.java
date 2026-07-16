/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.tool.api.R;
import com.ruoyi.vlstream.test.vlstream.config.VlsMqttProperties;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.RecordEventStrategy;
import com.ruoyi.vlstream.test.vlstream.service.IVlsRecordEventStrategyService;
import com.ruoyi.vlstream.test.vlstream.service.VlsMqttPublishService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 摄像头事件策略 控制器
 */
@RestController
@AllArgsConstructor
@RequestMapping("/vlsRecordEventStrategy")
@Tag(name = "摄像头事件策略", description = "摄像头事件策略接口")
public class VlsRecordEventStrategyController extends BladeController {

	private final IVlsRecordEventStrategyService recordEventStrategyService;
	private final VlsMqttPublishService vlsMqttPublishService;
	private final VlsMqttProperties vlsMqttProperties;

	private static final Map<String, Object> DEFAULT_PROTECTION_TIME = org.springblade.core.tool.utils.CollectionUtil.mapOf(
		"frequency", "每天",
		"time_periods", java.util.Arrays.asList(
			org.springblade.core.tool.utils.CollectionUtil.mapOf("start", "08:00:00", "end", "12:00:00"),
			org.springblade.core.tool.utils.CollectionUtil.mapOf("start", "14:00:00", "end", "18:00:00")
		)
	);

	@GetMapping("/{deviceId}")
	@Operation(summary = "获取事件策略", description = "根据设备编号获取事件策略")
	public R<RecordEventStrategy> getByDeviceId(@PathVariable String deviceId) {
		RecordEventStrategy detail = recordEventStrategyService.getByDeviceId(deviceId);
		if (detail == null) {
			detail = new RecordEventStrategy();
			detail.setDeviceId(deviceId);
			detail.setProtectionTime(DEFAULT_PROTECTION_TIME);
		}
		if (Objects.isNull(detail.getProtectionTime())) {
			detail.setProtectionTime(DEFAULT_PROTECTION_TIME);
		}
		return R.data(detail);
	}

	@PostMapping
	@Operation(summary = "保存事件策略", description = "保存或更新事件策略(含触发动作、告警频率、告警级别、告警方式、接收人、周期配置)")
	public R<Boolean> saveOrUpdate(@RequestBody RecordEventStrategy recordEventStrategy) {
		boolean success = recordEventStrategyService.saveOrUpdateStrategy(recordEventStrategy);
		if (!success) {
			return R.fail("保存失败");
		}
		boolean publishSuccess = vlsMqttPublishService.publish(vlsMqttProperties.getVlsRecordEventStrategyTopic(), recordEventStrategy);
		if (!publishSuccess) {
			return R.fail("保存成功，但MQTT消息发送失败");
		}
		return R.data(true);
	}

	@DeleteMapping("/{deviceId}")
	@Operation(summary = "删除事件策略", description = "根据设备编号删除事件策略")
	public R<Boolean> deleteByDeviceId(@PathVariable String deviceId) {
		boolean success = recordEventStrategyService.deleteByDeviceId(deviceId);
		return success ? R.data(true) : R.fail("删除失败");
	}
}
