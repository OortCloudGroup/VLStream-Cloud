/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.tool.api.R;
import com.ruoyi.vlstream.test.vlstream.config.VlsMqttProperties;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AudioDefenseTimeSetting;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AudioDefenseTimeSettingVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAudioDefenseTimeSettingService;
import com.ruoyi.vlstream.test.vlstream.service.VlsMqttPublishService;
import com.ruoyi.vlstream.test.vlstream.wrapper.VlsAudioDefenseTimeSettingWrapper;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 音频布防时间设置表 控制器
 */
@RestController
@AllArgsConstructor
@RequestMapping("/vlsAudioDefenseTimeSetting")
@Tag(name = "音频布防时间设置", description = "音频布防时间设置接口")
public class VlsAudioDefenseTimeSettingController extends BladeController {

	private static final Map<String, Object> DEFAULT_PROTECTION_TIME = org.springblade.core.tool.utils.CollectionUtil.mapOf(
		"frequency", "每天",
		"time_periods", java.util.Arrays.asList(
			org.springblade.core.tool.utils.CollectionUtil.mapOf("start", "08:00:00", "end", "12:00:00"),
			org.springblade.core.tool.utils.CollectionUtil.mapOf("start", "14:00:00", "end", "18:00:00")
		)
	);

	private final IVlsAudioDefenseTimeSettingService vlsAudioDefenseTimeSettingService;
	private final VlsMqttPublishService vlsMqttPublishService;
	private final VlsMqttProperties vlsMqttProperties;

	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "按设备ID查询音频布防时间设置")
	public R<AudioDefenseTimeSettingVO> detail(@RequestParam Long deviceId) {
		Assert.notNull(deviceId, "设备主键ID不能为空");
		AudioDefenseTimeSetting detail = vlsAudioDefenseTimeSettingService.getOne(Wrappers.<AudioDefenseTimeSetting>lambdaQuery()
			.eq(AudioDefenseTimeSetting::getDeviceId, deviceId)
			.last("limit 1"));

		if (detail == null) {
			detail = new AudioDefenseTimeSettingVO();
			detail.setDeviceId(deviceId);
			detail.setProtectionTime(DEFAULT_PROTECTION_TIME);
		}
		if (Objects.isNull(detail.getProtectionTime())) {
			detail.setProtectionTime(DEFAULT_PROTECTION_TIME);
		}

		return R.data(VlsAudioDefenseTimeSettingWrapper.build().entityVO(detail));
	}

	@PostMapping("/submit")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "新增或修改", description = "按设备ID保存音频布防时间设置")
	public R submit(@Valid @RequestBody AudioDefenseTimeSetting audioDefenseTimeSetting) {
		Assert.notNull(audioDefenseTimeSetting.getDeviceId(), "设备主键ID不能为空");
		AudioDefenseTimeSetting existed = vlsAudioDefenseTimeSettingService.getOne(Wrappers.<AudioDefenseTimeSetting>lambdaQuery()
			.eq(AudioDefenseTimeSetting::getDeviceId, audioDefenseTimeSetting.getDeviceId())
			.last("limit 1"));
		if (existed != null) {
			audioDefenseTimeSetting.setId(existed.getId());
		}
		boolean success = vlsAudioDefenseTimeSettingService.saveOrUpdate(audioDefenseTimeSetting);
		if (!success) {
			return R.status(false);
		}
		boolean publishSuccess = vlsMqttPublishService.publish(vlsMqttProperties.getVlsAudioDefenseTimeSettingTopic(), audioDefenseTimeSetting);
		if (!publishSuccess) {
			return R.fail("保存成功，但MQTT消息发送失败");
		}
		return R.status(true);
	}
}
