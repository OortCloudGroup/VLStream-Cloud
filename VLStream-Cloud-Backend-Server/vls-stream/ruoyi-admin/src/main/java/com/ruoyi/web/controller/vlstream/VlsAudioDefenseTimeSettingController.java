package com.ruoyi.web.controller.vlstream;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.config.VlsMqttProperties;
import com.ruoyi.vlstream.domain.AudioDefenseTimeSetting;
import com.ruoyi.vlstream.service.IVlsAudioDefenseTimeSettingService;
import com.ruoyi.vlstream.service.VlsMqttPublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Audio arming schedule backed by JSON persistence and MQTT delivery. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsAudioDefenseTimeSetting")
public class VlsAudioDefenseTimeSettingController {

    private final IVlsAudioDefenseTimeSettingService settingService;
    private final VlsMqttPublishService mqttPublishService;
    private final VlsMqttProperties mqttProperties;

    /** Return the persisted schedule or the source-compatible default. */
    @GetMapping("/detail")
    public BladeResult<AudioDefenseTimeSetting> detail(@RequestParam Long deviceId) {
        AudioDefenseTimeSetting setting = find(deviceId);
        if (setting == null) {
            setting = new AudioDefenseTimeSetting();
            setting.setDeviceId(deviceId);
            setting.setProtectionTime(defaultProtectionTime());
        } else if (setting.getProtectionTime() == null) {
            setting.setProtectionTime(defaultProtectionTime());
        }
        return BladeResult.success(setting);
    }

    /** Upsert the schedule by device ID and publish the stored JSON payload. */
    @PostMapping("/submit")
    public BladeResult<AudioDefenseTimeSetting> submit(@RequestBody AudioDefenseTimeSetting setting) {
        try {
            if (setting == null || setting.getDeviceId() == null) {
                throw new IllegalArgumentException("Device primary key ID cannot be empty");
            }
            AudioDefenseTimeSetting existing = find(setting.getDeviceId());
            if (existing != null) {
                setting.setId(existing.getId());
            }
            if (!settingService.saveOrUpdate(setting)) {
                return BladeResult.fail("Audio defense-time setting was not persisted");
            }
            AudioDefenseTimeSetting payload = settingService.getById(setting.getId());
            mqttPublishService.publishOrThrow(mqttProperties.getVlsAudioDefenseTimeSettingTopic(), payload);
            return BladeResult.success(payload);
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Find the unique non-deleted arming schedule for a device. */
    private AudioDefenseTimeSetting find(Long deviceId) {
        if (deviceId == null) {
            return null;
        }
        return settingService.getOne(new LambdaQueryWrapper<AudioDefenseTimeSetting>()
            .eq(AudioDefenseTimeSetting::getDeviceId, deviceId)
            .last("limit 1"));
    }

    /** Build the Java 8 representation of the main-source default schedule. */
    private Map<String, Object> defaultProtectionTime() {
        Map<String, Object> first = new LinkedHashMap<String, Object>();
        first.put("start", "08:00:00");
        first.put("end", "12:00:00");
        Map<String, Object> second = new LinkedHashMap<String, Object>();
        second.put("start", "14:00:00");
        second.put("end", "18:00:00");
        List<Map<String, Object>> periods = new ArrayList<Map<String, Object>>();
        periods.add(first);
        periods.add(second);
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("frequency", "Every day");
        result.put("time_periods", periods);
        return result;
    }
}
