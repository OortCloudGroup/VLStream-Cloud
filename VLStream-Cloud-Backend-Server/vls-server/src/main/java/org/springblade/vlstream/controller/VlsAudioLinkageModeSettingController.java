package org.springblade.vlstream.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.tool.api.R;
import org.springblade.vlstream.config.VlsMqttProperties;
import org.springblade.vlstream.pojo.entity.AudioLinkageModeSetting;
import org.springblade.vlstream.pojo.vo.AudioLinkageModeSettingVO;
import org.springblade.vlstream.service.IVlsAudioLinkageModeSettingService;
import org.springblade.vlstream.service.VlsMqttPublishService;
import org.springblade.vlstream.wrapper.VlsAudioLinkageModeSettingWrapper;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

/**
 * Audio linkage mode settings table controller
 */
@RestController
@AllArgsConstructor
@RequestMapping("/vlsAudioLinkageModeSetting")
@Tag(name = "Audio linkage mode settings", description = "Audio linkage mode settings interface")
public class VlsAudioLinkageModeSettingController extends BladeController {

	private final IVlsAudioLinkageModeSettingService vlsAudioLinkageModeSettingService;
	private final VlsMqttPublishService vlsMqttPublishService;
	private final VlsMqttProperties vlsMqttProperties;

	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "Details", description = "Query audio linkage method settings by device ID")
	public R<AudioLinkageModeSettingVO> detail(@RequestParam Long deviceId) {
		Assert.notNull(deviceId, "Device primary key ID cannot be empty");
		AudioLinkageModeSetting detail = vlsAudioLinkageModeSettingService.getOne(Wrappers.<AudioLinkageModeSetting>lambdaQuery()
			.eq(AudioLinkageModeSetting::getDeviceId, deviceId)
			.last("limit 1"));
		return R.data(VlsAudioLinkageModeSettingWrapper.build().entityVO(detail));
	}

	@PostMapping("/submit")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "Add or modify", description = "Save audio linkage method settings by device ID")
	public R submit(@Valid @RequestBody AudioLinkageModeSetting audioLinkageModeSetting) {
		Assert.notNull(audioLinkageModeSetting.getDeviceId(), "Device primary key ID cannot be empty");
		if (Integer.valueOf(1).equals(audioLinkageModeSetting.getAlarmOutputLinkageEnabled())) {
			Assert.isTrue(StringUtils.isNotBlank(audioLinkageModeSetting.getAlarmOutputChannel()), "When linkage alarm output is enabled, the alarm output channel cannot be empty");
		}
		if (Integer.valueOf(1).equals(audioLinkageModeSetting.getRecordLinkageEnabled())) {
			Assert.isTrue(StringUtils.isNotBlank(audioLinkageModeSetting.getRecordChannel()), "When video linkage is enabled, the recording channel cannot be empty");
		}
		AudioLinkageModeSetting existed = vlsAudioLinkageModeSettingService.getOne(Wrappers.<AudioLinkageModeSetting>lambdaQuery()
			.eq(AudioLinkageModeSetting::getDeviceId, audioLinkageModeSetting.getDeviceId())
			.last("limit 1"));
		if (existed != null) {
			audioLinkageModeSetting.setId(existed.getId());
		}
		boolean success = vlsAudioLinkageModeSettingService.saveOrUpdate(audioLinkageModeSetting);
		if (!success) {
			return R.status(false);
		}
		boolean publishSuccess = vlsMqttPublishService.publish(vlsMqttProperties.getVlsAudioLinkageModeSettingTopic(), audioLinkageModeSetting);
		if (!publishSuccess) {
			return R.fail("Saved successfully, but MQTT message sending failed");
		}
		return R.status(true);
	}
}
