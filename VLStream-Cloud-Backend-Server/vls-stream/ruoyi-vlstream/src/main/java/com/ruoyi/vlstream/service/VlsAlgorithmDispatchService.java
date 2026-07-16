/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.vlstream.config.VlsMqttProperties;
import com.ruoyi.vlstream.domain.AlgorithmModel;
import com.ruoyi.vlstream.domain.AlgorithmTraining;
import com.ruoyi.vlstream.domain.DeviceInfo;
import com.ruoyi.vlstream.mapper.VlsAlgorithmModelMapper;
import com.ruoyi.vlstream.mapper.VlsAlgorithmTrainingMapper;
import com.ruoyi.vlstream.mapper.VlsDeviceInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends the latest trained model to selected devices through MQTT.
 */
@Service
@RequiredArgsConstructor
public class VlsAlgorithmDispatchService {

    private final VlsAlgorithmTrainingMapper trainingMapper;
    private final VlsAlgorithmModelMapper modelMapper;
    private final VlsDeviceInfoMapper deviceInfoMapper;
    private final VlsMqttPublishService mqttPublishService;
    private final VlsMqttProperties mqttProperties;

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> dispatch(Long algorithmId, List<Long> deviceIds) {
        if (algorithmId == null) {
            return failed("Algorithm ID is required");
        }
        if (deviceIds == null || deviceIds.isEmpty()) {
            return failed("Device IDs are required");
        }

        AlgorithmTraining training = latestTraining(algorithmId);
        AlgorithmModel model = latestModel(algorithmId, training == null ? null : training.getId());
        if (model == null || model.getId() == null) {
            return failed("No trained model found for algorithm " + algorithmId);
        }

        String topic = StringUtils.hasText(mqttProperties.getDispatchAlgorithmsTopic())
            ? mqttProperties.getDispatchAlgorithmsTopic()
            : "oortcloud/dispatchAlgorithms";
        List<DeviceInfo> devices = new ArrayList<DeviceInfo>();
        for (Long id : deviceIds) {
            DeviceInfo device = deviceInfoMapper.selectById(id);
            if (device == null || !StringUtils.hasText(device.getDeviceId())) {
                return failed("Device does not exist or device number is empty: " + id);
            }
            devices.add(device);
        }

        List<Long> published = new ArrayList<Long>();
        try {
            for (DeviceInfo device : devices) {
                Map<String, Object> payload = new LinkedHashMap<String, Object>();
                payload.put("deviceId", device.getDeviceId());
                payload.put("modelId", model.getId());
                payload.put("algorithmId", algorithmId);
                mqttPublishService.publishOrThrow(topic, payload);
                published.add(device.getId());
            }
        } catch (RuntimeException ex) {
            return failed("MQTT dispatch failed: " + ex.getMessage());
        }

        for (DeviceInfo device : devices) {
            DeviceInfo update = new DeviceInfo();
            update.setId(device.getId());
            update.setAlgorithmId(String.valueOf(algorithmId));
            deviceInfoMapper.updateById(update);
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("success", Boolean.TRUE);
        result.put("status", "completed");
        result.put("algorithmId", algorithmId);
        result.put("trainingId", training == null ? null : training.getId());
        result.put("modelId", model.getId());
        result.put("topic", topic);
        result.put("deviceIds", published);
        result.put("published", published.size());
        result.put("message", "Algorithm model dispatched through MQTT");
        return result;
    }

    private AlgorithmTraining latestTraining(Long algorithmId) {
        return trainingMapper.selectOne(new LambdaQueryWrapper<AlgorithmTraining>()
            .eq(AlgorithmTraining::getAlgorithmId, algorithmId)
            .orderByDesc(AlgorithmTraining::getUpdateTime)
            .orderByDesc(AlgorithmTraining::getCreateTime)
            .orderByDesc(AlgorithmTraining::getId)
            .last("LIMIT 1"));
    }

    private AlgorithmModel latestModel(Long algorithmId, Long trainingId) {
        if (trainingId != null) {
            AlgorithmModel model = modelMapper.selectOne(new LambdaQueryWrapper<AlgorithmModel>()
                .eq(AlgorithmModel::getTrainingId, trainingId)
                .orderByDesc(AlgorithmModel::getCreateTime)
                .orderByDesc(AlgorithmModel::getId)
                .last("LIMIT 1"));
            if (model != null) {
                return model;
            }
        }
        return modelMapper.selectOne(new LambdaQueryWrapper<AlgorithmModel>()
            .eq(AlgorithmModel::getAlgorithmId, algorithmId)
            .orderByDesc(AlgorithmModel::getCreateTime)
            .orderByDesc(AlgorithmModel::getId)
            .last("LIMIT 1"));
    }

    private Map<String, Object> failed(String message) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("success", Boolean.FALSE);
        result.put("status", "failed");
        result.put("message", message);
        return result;
    }
}
