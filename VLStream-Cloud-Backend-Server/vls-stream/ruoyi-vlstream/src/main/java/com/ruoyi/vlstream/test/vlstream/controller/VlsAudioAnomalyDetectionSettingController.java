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
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AudioAnomalyDetectionSetting;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AudioAnomalyDetectionSettingVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAudioAnomalyDetectionSettingService;
import com.ruoyi.vlstream.test.vlstream.service.VlsMqttPublishService;
import com.ruoyi.vlstream.test.vlstream.wrapper.VlsAudioAnomalyDetectionSettingWrapper;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

/**
 * 音频异常侦测设置表 控制器
 */
@RestController
@AllArgsConstructor
@RequestMapping("/vlsAudioAnomalyDetectionSetting")
@Tag(name = "音频异常侦测设置", description = "音频异常侦测设置接口")
public class VlsAudioAnomalyDetectionSettingController extends BladeController {

	private final IVlsAudioAnomalyDetectionSettingService vlsAudioAnomalyDetectionSettingService;
	private final VlsMqttPublishService vlsMqttPublishService;
	private final VlsMqttProperties vlsMqttProperties;

	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "按设备ID查询音频异常侦测设置")
	public R<AudioAnomalyDetectionSettingVO> detail(@RequestParam Long deviceId) {
		Assert.notNull(deviceId, "设备主键ID不能为空");
		AudioAnomalyDetectionSetting detail = vlsAudioAnomalyDetectionSettingService.getOne(Wrappers.<AudioAnomalyDetectionSetting>lambdaQuery()
			.eq(AudioAnomalyDetectionSetting::getDeviceId, deviceId)
			.last("limit 1"));
		return R.data(VlsAudioAnomalyDetectionSettingWrapper.build().entityVO(detail));
	}

	@PostMapping("/submit")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "新增或修改", description = "按设备ID保存音频异常侦测设置")
	public R submit(@Valid @RequestBody AudioAnomalyDetectionSetting audioAnomalyDetectionSetting) {
		Assert.notNull(audioAnomalyDetectionSetting.getDeviceId(), "设备主键ID不能为空");
		AudioAnomalyDetectionSetting existed = vlsAudioAnomalyDetectionSettingService.getOne(Wrappers.<AudioAnomalyDetectionSetting>lambdaQuery()
			.eq(AudioAnomalyDetectionSetting::getDeviceId, audioAnomalyDetectionSetting.getDeviceId())
			.last("limit 1"));
		if (existed != null) {
			audioAnomalyDetectionSetting.setId(existed.getId());
		}
		boolean success = vlsAudioAnomalyDetectionSettingService.saveOrUpdate(audioAnomalyDetectionSetting);
		if (!success) {
			return R.status(false);
		}
		boolean publishSuccess = vlsMqttPublishService.publish(vlsMqttProperties.getVlsAudioAnomalyDetectionSettingTopic(), audioAnomalyDetectionSetting);
		if (!publishSuccess) {
			return R.fail("保存成功，但MQTT消息发送失败");
		}
		return R.status(true);
	}
}
