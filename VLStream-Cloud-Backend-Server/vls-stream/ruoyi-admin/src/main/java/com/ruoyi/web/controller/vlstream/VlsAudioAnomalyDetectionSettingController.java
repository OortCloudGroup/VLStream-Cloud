package com.ruoyi.web.controller.vlstream;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.config.VlsMqttProperties;
import com.ruoyi.vlstream.domain.AudioAnomalyDetectionSetting;
import com.ruoyi.vlstream.service.IVlsAudioAnomalyDetectionSettingService;
import com.ruoyi.vlstream.service.VlsMqttPublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Audio anomaly settings backed by the VLS table and MQTT delivery. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsAudioAnomalyDetectionSetting")
public class VlsAudioAnomalyDetectionSettingController {

    private final IVlsAudioAnomalyDetectionSettingService settingService;
    private final VlsMqttPublishService mqttPublishService;
    private final VlsMqttProperties mqttProperties;

    /** Return the persisted anomaly setting for one device. */
    @GetMapping("/detail")
    public BladeResult<AudioAnomalyDetectionSetting> detail(@RequestParam Long deviceId) {
        return BladeResult.success(find(deviceId));
    }

    /** Upsert by device ID and publish the stored setting through MQTT. */
    @PostMapping("/submit")
    public BladeResult<AudioAnomalyDetectionSetting> submit(@RequestBody AudioAnomalyDetectionSetting setting) {
        try {
            if (setting == null || setting.getDeviceId() == null) {
                throw new IllegalArgumentException("Device primary key ID cannot be empty");
            }
            AudioAnomalyDetectionSetting existing = find(setting.getDeviceId());
            if (existing != null) {
                setting.setId(existing.getId());
            }
            if (!settingService.saveOrUpdate(setting)) {
                return BladeResult.fail("Audio anomaly setting was not persisted");
            }
            AudioAnomalyDetectionSetting payload = settingService.getById(setting.getId());
            mqttPublishService.publishOrThrow(mqttProperties.getVlsAudioAnomalyDetectionSettingTopic(), payload);
            return BladeResult.success(payload);
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Find the unique non-deleted setting for a device. */
    private AudioAnomalyDetectionSetting find(Long deviceId) {
        if (deviceId == null) {
            return null;
        }
        return settingService.getOne(new LambdaQueryWrapper<AudioAnomalyDetectionSetting>()
            .eq(AudioAnomalyDetectionSetting::getDeviceId, deviceId)
            .last("limit 1"));
    }
}
