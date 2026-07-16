package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.config.VlsMqttProperties;
import com.ruoyi.vlstream.domain.RecordEventStrategy;
import com.ruoyi.vlstream.service.IVlsRecordEventStrategyService;
import com.ruoyi.vlstream.service.VlsMqttPublishService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Camera event strategy persistence and MQTT dispatch. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsRecordEventStrategy")
public class VlsRecordEventStrategyController {

    private final IVlsRecordEventStrategyService strategyService;
    private final VlsMqttPublishService mqttPublishService;
    private final VlsMqttProperties mqttProperties;

    /** Return a stored strategy or the source-compatible default for the device. */
    @GetMapping("/{deviceId}")
    public BladeResult<RecordEventStrategy> getByDeviceId(@PathVariable String deviceId) {
        RecordEventStrategy strategy = strategyService.getByDeviceId(deviceId);
        if (strategy == null) {
            strategy = new RecordEventStrategy();
            strategy.setDeviceId(deviceId);
            strategy.setProtectionTime(defaultProtectionTime());
        } else if (strategy.getProtectionTime() == null) {
            strategy.setProtectionTime(defaultProtectionTime());
        }
        return BladeResult.success(strategy);
    }

    /** Persist the unique strategy and publish the stored payload over MQTT. */
    @PostMapping("")
    public BladeResult<RecordEventStrategy> saveOrUpdate(@RequestBody RecordEventStrategy strategy) {
        try {
            RecordEventStrategy stored = strategyService.saveOrUpdateStrategy(strategy);
            mqttPublishService.publishOrThrow(mqttProperties.getVlsRecordEventStrategyTopic(), stored);
            return BladeResult.success(stored);
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Logically delete the device strategy from the real table. */
    @DeleteMapping("/{deviceId}")
    public BladeResult<Boolean> deleteByDeviceId(@PathVariable String deviceId) {
        try {
            boolean deleted = strategyService.deleteByDeviceId(deviceId);
            return deleted ? BladeResult.success(Boolean.TRUE) : BladeResult.<Boolean>fail("Delete affected no rows");
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
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
