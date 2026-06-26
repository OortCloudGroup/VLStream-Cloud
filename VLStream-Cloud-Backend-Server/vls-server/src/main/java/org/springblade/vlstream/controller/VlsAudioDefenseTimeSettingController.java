package org.springblade.vlstream.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.tool.api.R;
import org.springblade.vlstream.config.VlsMqttProperties;
import org.springblade.vlstream.pojo.entity.AudioDefenseTimeSetting;
import org.springblade.vlstream.pojo.vo.AudioDefenseTimeSettingVO;
import org.springblade.vlstream.service.IVlsAudioDefenseTimeSettingService;
import org.springblade.vlstream.service.VlsMqttPublishService;
import org.springblade.vlstream.wrapper.VlsAudioDefenseTimeSettingWrapper;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Audio arming schedule setting table controller
 */
@RestController
@AllArgsConstructor
@RequestMapping("/vlsAudioDefenseTimeSetting")
@Tag(name = "Audio arming schedule setting", description = "Audio arming schedule setting interface")
public class VlsAudioDefenseTimeSettingController extends BladeController {

	private static final Map<String, Object> DEFAULT_PROTECTION_TIME = Map.of(
		"frequency", "Every day",
		"time_periods", List.of(
			Map.of("start", "08:00:00", "end", "12:00:00"),
			Map.of("start", "14:00:00", "end", "18:00:00")
		)
	);

	private final IVlsAudioDefenseTimeSettingService vlsAudioDefenseTimeSettingService;
	private final VlsMqttPublishService vlsMqttPublishService;
	private final VlsMqttProperties vlsMqttProperties;

	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "Details", description = "Query audio arming schedule settings by device ID")
	public R<AudioDefenseTimeSettingVO> detail(@RequestParam Long deviceId) {
		Assert.notNull(deviceId, "Device primary key ID cannot be empty");
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
	@Operation(summary = "Add or modify", description = "Save audio arming schedule settings by device ID")
	public R submit(@Valid @RequestBody AudioDefenseTimeSetting audioDefenseTimeSetting) {
		Assert.notNull(audioDefenseTimeSetting.getDeviceId(), "Device primary key ID cannot be empty");
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
			return R.fail("Saved successfully, but MQTT message sending failed");
		}
		return R.status(true);
	}
}
