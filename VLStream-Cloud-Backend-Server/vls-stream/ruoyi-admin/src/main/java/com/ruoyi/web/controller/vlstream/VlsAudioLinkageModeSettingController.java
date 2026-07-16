package com.ruoyi.web.controller.vlstream;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.config.VlsMqttProperties;
import com.ruoyi.vlstream.domain.AudioLinkageModeSetting;
import com.ruoyi.vlstream.service.IVlsAudioLinkageModeSettingService;
import com.ruoyi.vlstream.service.VlsMqttPublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Audio linkage settings backed by the VLS table and MQTT delivery. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsAudioLinkageModeSetting")
public class VlsAudioLinkageModeSettingController {

    private final IVlsAudioLinkageModeSettingService settingService;
    private final VlsMqttPublishService mqttPublishService;
    private final VlsMqttProperties mqttProperties;

    /** Return the persisted linkage setting for one device. */
    @GetMapping("/detail")
    public BladeResult<AudioLinkageModeSetting> detail(@RequestParam Long deviceId) {
        return BladeResult.success(find(deviceId));
    }

    /** Validate, upsert and publish the linkage configuration. */
    @PostMapping("/submit")
    public BladeResult<AudioLinkageModeSetting> submit(@RequestBody AudioLinkageModeSetting setting) {
        try {
            validate(setting);
            AudioLinkageModeSetting existing = find(setting.getDeviceId());
            if (existing != null) {
                setting.setId(existing.getId());
            }
            if (!settingService.saveOrUpdate(setting)) {
                return BladeResult.fail("Audio linkage-mode setting was not persisted");
            }
            AudioLinkageModeSetting payload = settingService.getById(setting.getId());
            mqttPublishService.publishOrThrow(mqttProperties.getVlsAudioLinkageModeSettingTopic(), payload);
            return BladeResult.success(payload);
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Find the unique non-deleted linkage configuration for a device. */
    private AudioLinkageModeSetting find(Long deviceId) {
        if (deviceId == null) {
            return null;
        }
        return settingService.getOne(new LambdaQueryWrapper<AudioLinkageModeSetting>()
            .eq(AudioLinkageModeSetting::getDeviceId, deviceId)
            .last("limit 1"));
    }

    /** Enforce the channel requirements used by the main-source backend. */
    private void validate(AudioLinkageModeSetting setting) {
        if (setting == null || setting.getDeviceId() == null) {
            throw new IllegalArgumentException("Device primary key ID cannot be empty");
        }
        if (Integer.valueOf(1).equals(setting.getAlarmOutputLinkageEnabled())
            && !StringUtils.hasText(setting.getAlarmOutputChannel())) {
            throw new IllegalArgumentException("When linkage alarm output is enabled, the alarm output channel cannot be empty");
        }
        if (Integer.valueOf(1).equals(setting.getRecordLinkageEnabled())
            && !StringUtils.hasText(setting.getRecordChannel())) {
            throw new IllegalArgumentException("When video linkage is enabled, the recording channel cannot be empty");
        }
    }
}
