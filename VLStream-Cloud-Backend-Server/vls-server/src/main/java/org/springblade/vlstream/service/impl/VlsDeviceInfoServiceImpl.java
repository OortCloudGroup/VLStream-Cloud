package org.springblade.vlstream.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.vlstream.config.VlsMqttProperties;
import org.springblade.vlstream.excel.VlsDeviceInfoExcel;
import org.springblade.vlstream.mapper.VlsDeviceInfoMapper;
import org.springblade.vlstream.pojo.entity.AlgorithmModel;
import org.springblade.vlstream.pojo.entity.AlgorithmTraining;
import org.springblade.vlstream.pojo.entity.DeviceInfo;
import org.springblade.vlstream.pojo.vo.DeviceInfoVO;
import org.springblade.vlstream.service.IVlsAlgorithmModelService;
import org.springblade.vlstream.service.IVlsAlgorithmTrainingService;
import org.springblade.vlstream.service.IVlsDeviceInfoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Device Information Table Service Implementation Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@Service
public class VlsDeviceInfoServiceImpl extends BaseServiceImpl<VlsDeviceInfoMapper, DeviceInfo> implements IVlsDeviceInfoService {

	@Resource
	private IVlsAlgorithmTrainingService vlsAlgorithmTrainingService;

	@Resource
	private IVlsAlgorithmModelService vlsAlgorithmModelService;

	@Resource
	private VlsMqttProperties vlsMqttProperties;

	@Override
	public IPage<DeviceInfoVO> selectVlsDeviceInfoPage(IPage<DeviceInfoVO> page, DeviceInfoVO vlsDeviceInfo) {
		return page.setRecords(baseMapper.selectVlsDeviceInfoPage(page, vlsDeviceInfo));
	}

	@Override
	public List<VlsDeviceInfoExcel> exportVlsDeviceInfo(Wrapper<DeviceInfo> queryWrapper) {
		List<VlsDeviceInfoExcel> vlsDeviceInfoList = baseMapper.exportVlsDeviceInfo(queryWrapper);
		//vlsDeviceInfoList.forEach(vlsDeviceInfo -> {
		//	vlsDeviceInfo.setTypeName(DictCache.getValue(DictEnum.YES_NO, VlsDeviceInfoEntity.getType()));
		//});
		return vlsDeviceInfoList;
	}

	@Override
	public IPage<DeviceInfo> getDevicePage(Page<DeviceInfo> page, String deviceName, String tag, String status) {
		return baseMapper.selectDevicePage(page, deviceName, tag, status);
	}

	@Override
	public DeviceInfo getByDeviceId(String deviceId) {
		if (StringUtils.isBlank(deviceId)) {
			return null;
		}
		return baseMapper.selectByDeviceId(deviceId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean addDevice(DeviceInfo deviceInfo) {
		try {
			// Automatically generate device_id (if not provided)
			if (deviceInfo.getDeviceId() == null || deviceInfo.getDeviceId().trim().isEmpty()) {
				String deviceId = generateDeviceId();
				deviceInfo.setDeviceId(deviceId);
			} else if (checkDeviceIdExists(deviceInfo.getDeviceId())) {
				return false;
			}
			return save(deviceInfo);
		} catch (Exception e) {
			log.error("Failed to add device", e);
			return false;
		}
	}

	@Override
	public boolean updateDevice(DeviceInfo deviceInfo) {
		return updateById(deviceInfo);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean dispatchAlgorithms(Long algorithmId, String deviceIds) {
		if (StringUtils.isBlank(deviceIds)) {
			return false;
		}

		String mqttTopic = "oortcloud/dispatchAlgorithms";

		AlgorithmTraining latestTraining = vlsAlgorithmTrainingService.getOne(Wrappers.<AlgorithmTraining>lambdaQuery()
			.eq(AlgorithmTraining::getAlgorithmId, algorithmId)
			.orderByDesc(AlgorithmTraining::getUpdateTime)
			.last("limit 1"));
		if (latestTraining == null) {
			log.error("Algorithm delivery failed, latest training task not found: algorithmId={}", algorithmId);
			return false;
		}

		AlgorithmModel latestModel = vlsAlgorithmModelService.getOne(Wrappers.<AlgorithmModel>lambdaQuery()
			.eq(AlgorithmModel::getTrainingId, latestTraining.getId())
			.orderByDesc(AlgorithmModel::getCreateTime)
			.last("limit 1"));
		if (latestModel == null || latestModel.getId() == null) {
			log.error("Algorithm delivery failed, latest training model not found: algorithmId={}, trainingId={}", algorithmId, latestTraining.getId());
			return false;
		}

		MqttClient mqttClient = null;
		try {
			mqttClient = createMqttClient();
			mqttClient.connect(buildMqttConnectOptions());

			for (String deviceId : deviceIds.split(",")) {
				if (StringUtils.isBlank(deviceId)) {
					continue;
				}
				DeviceInfo device = getById(deviceId.trim());
				if (device == null || StringUtils.isBlank(device.getDeviceId())) {
					log.error("Algorithm delivery failed, device does not exist or device number is empty: deviceId={}", deviceId);
					return false;
				}

				DeviceInfo updateEntity = new DeviceInfo();
				updateEntity.setId(Long.valueOf(deviceId.trim()));
				updateEntity.setAlgorithmId(String.valueOf(algorithmId));
				updateById(updateEntity);

				if (!StringUtils.equals(mqttTopic, "oortcloud/dispatchAlgorithms")) {
					log.error("Algorithm delivery failed, MQTT topic only allowed oortcloud/#: topic={}", mqttTopic);
					return false;
				}

				Map<String, Object> messagePayload = new HashMap<>(2);
				messagePayload.put("deviceId", device.getDeviceId());
				messagePayload.put("modelId", latestModel.getId());

				MqttMessage mqttMessage = new MqttMessage(JSONUtil.toJsonStr(messagePayload).getBytes(StandardCharsets.UTF_8));
				mqttMessage.setQos(vlsMqttProperties.getQos());
				mqttClient.publish(mqttTopic, mqttMessage);
			}
			return true;
		} catch (Exception dispatchException) {
			log.error("Algorithm delivery failed, MQTT send exception: algorithmId={}", algorithmId, dispatchException);
			return false;
		} finally {
			closeMqttClient(mqttClient);
		}
	}

	private MqttClient createMqttClient() throws MqttException {
		String clientIdPrefix = StringUtils.defaultIfBlank(vlsMqttProperties.getClientIdPrefix(), "vls-dispatch");
		String brokerUrl = "tcp://" + vlsMqttProperties.getHost() + ":" + vlsMqttProperties.getPort();
		String clientId = clientIdPrefix + "-" + UUID.randomUUID();
		return new MqttClient(brokerUrl, clientId);
	}

	private MqttConnectOptions buildMqttConnectOptions() {
		MqttConnectOptions connectOptions = new MqttConnectOptions();
		connectOptions.setAutomaticReconnect(true);
		connectOptions.setCleanSession(true);
		connectOptions.setConnectionTimeout(vlsMqttProperties.getConnectionTimeoutSeconds());
		connectOptions.setKeepAliveInterval(vlsMqttProperties.getKeepAliveSeconds());
		if (StringUtils.isNotBlank(vlsMqttProperties.getUsername())) {
			connectOptions.setUserName(vlsMqttProperties.getUsername());
		}
		if (StringUtils.isNotBlank(vlsMqttProperties.getPassword())) {
			connectOptions.setPassword(vlsMqttProperties.getPassword().toCharArray());
		}
		return connectOptions;
	}

	private void closeMqttClient(MqttClient mqttClient) {
		if (mqttClient == null) {
			return;
		}
		try {
			if (mqttClient.isConnected()) {
				mqttClient.disconnect();
			}
		} catch (MqttException disconnectException) {
			log.warn("Failed to close MQTT connection", disconnectException);
		}
		try {
			mqttClient.close();
		} catch (MqttException closeException) {
			log.warn("Failed to close MQTT client", closeException);
		}
	}

	@Override
	public boolean deleteDevice(Long id) {
		return removeById(id);
	}

	@Override
	public boolean deleteDeviceBatch(List<Long> ids) {
		return removeByIds(ids);
	}

	@Override
	public boolean updateDeviceStatus(Long id, Integer status) {
		DeviceInfo deviceInfo = new DeviceInfo();
		deviceInfo.setId(id);
		deviceInfo.setStatus(status);
		return updateById(deviceInfo);
	}

	@Override
	public boolean updateDeviceStatusBatch(List<Long> ids, String status) {
		return baseMapper.updateStatusBatch(ids, status) > 0;
	}

	@Override
	public List<DeviceInfo> getDevicesByStatus(String status) {
		return baseMapper.selectByStatus(status);
	}

	@Override
	public List<DeviceInfo> getDevicesByType(String deviceType) {
		return baseMapper.selectByDeviceType(deviceType);
	}

	@Override
	public List<DeviceInfo> getDevicesByPosition(String position) {
		return baseMapper.selectByPosition(position);
	}

	@Override
	public boolean checkDeviceIdExists(String deviceId) {
		if (StringUtils.isBlank(deviceId)) {
			return false;
		}
		return baseMapper.countByDeviceId(deviceId) > 0;
	}

	@Override
	public Map<String, Object> testDeviceConnection(Long id) {
		DeviceInfo deviceInfo = getById(id);
		Map<String, Object> result = new HashMap<>();

		if (deviceInfo == null) {
			result.put("success", false);
			result.put("message", "Device does not exist");
			return result;
		}

		try {
			// Implement actual device connection test logic here
			// TODO: Test connection based on device type and connection parameters
			log.info("Test device connection: {}", deviceInfo.getDeviceName());

			result.put("success", true);
			result.put("message", "Connection successful");
			result.put("latency", "20ms");
		} catch (Exception e) {
			log.error("Device connection test failed: {}", deviceInfo.getDeviceName(), e);
			result.put("success", false);
			result.put("message", "Connection failed:" + e.getMessage());
		}

		return result;
	}

	@Override
	public Map<String, Object> getDeviceStatistics() {
		List<VlsDeviceInfoMapper.StatusStatistics> statusStats = baseMapper.getStatusStatistics();
		List<VlsDeviceInfoMapper.TypeStatistics> typeStats = baseMapper.getTypeStatistics();
		List<VlsDeviceInfoMapper.BrandStatistics> brandStats = baseMapper.getBrandStatistics();

		Map<String, Object> result = new HashMap<>();

		long total = 0;
		long online = 0;
		long offline = 0;
		long fault = 0;

		for (VlsDeviceInfoMapper.StatusStatistics stat : statusStats) {
			total += stat.getCount();
			switch (stat.getStatus()) {
				case "Offline":
					offline = stat.getCount();
					break;
				case "Online":
					online = stat.getCount();
					break;
				case "Fault":
					fault = stat.getCount();
					break;
			}
		}

		result.put("total", total);
		result.put("online", online);
		result.put("offline", offline);
		result.put("fault", fault);
		result.put("typeStatistics", typeStats);
		result.put("brandStatistics", brandStats);

		return result;
	}

	@Override
	public List<String> getAllTags() {
		return baseMapper.getAllTags();
	}

	@Override
	public List<String> getAllBrands() {
		return baseMapper.getAllBrands();
	}

	@Override
	public Map<String, Object> validateDevice(DeviceInfo deviceInfo) {
		Map<String, Object> result = new HashMap<>();
		List<String> errors = new ArrayList<>();

		// Verify device name
		if (deviceInfo.getDeviceName() == null || deviceInfo.getDeviceName().trim().isEmpty()) {
			errors.add("Device name cannot be empty");
		}

		// Verify device ID
		if (deviceInfo.getDeviceId() == null || deviceInfo.getDeviceId().trim().isEmpty()) {
			errors.add("Device ID cannot be empty");
		} else if (checkDeviceIdExists(deviceInfo.getDeviceId())) {
			errors.add("Device ID already exists");
		}

		if (errors.isEmpty()) {
			result.put("valid", true);
			result.put("message", "Verification passed");
		} else {
			result.put("valid", false);
			result.put("errors", errors);
		}

		return result;
	}

	private String generateDeviceId() {
		return "DEV" + System.currentTimeMillis();
	}

	private boolean isValidIpAddress(String ip) {
		try {
			String[] parts = ip.split("\\.");
			if (parts.length != 4) {
				return false;
			}
			for (String part : parts) {
				int num = Integer.parseInt(part);
				if (num < 0 || num > 255) {
					return false;
				}
			}
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	@Override
	public Map<String, Object> refreshDeviceStatus(Long deviceId) {
		Map<String, Object> result = new HashMap<>();
		DeviceInfo deviceInfo = getById(deviceId);

		if (deviceInfo == null) {
			result.put("success", false);
			result.put("message", "Device does not exist");
			return result;
		}

		try {
			// Simulate status refresh
			updateDeviceStatus(deviceId, 1);

			result.put("success", true);
			result.put("message", "Status refreshed successfully");
			result.put("status", 1);
		} catch (Exception e) {
			log.error("Failed to refresh device status: {}", deviceInfo.getDeviceName(), e);
			result.put("success", false);
			result.put("message", "Failed to refresh status:" + e.getMessage());
		}

		return result;
	}

	@Override
	public Map<String, Object> batchImportDevices(List<DeviceInfo> deviceList) {
		Map<String, Object> result = new HashMap<>();
		int successCount = 0;
		int failCount = 0;
		List<String> errors = new ArrayList<>();

		for (DeviceInfo device : deviceList) {
			try {
				// Verify device information
				Map<String, Object> validation = validateDevice(device);
				if (!(Boolean) validation.get("valid")) {
					failCount++;
					errors.add("Device" + device.getDeviceName() + "Verification failed");
					continue;
				}

				// Save device
				if (addDevice(device)) {
					successCount++;
				} else {
					failCount++;
					errors.add("Device" + device.getDeviceName() + "Save failed");
				}
			} catch (Exception e) {
				failCount++;
				errors.add("Device" + device.getDeviceName() + "Process exception:" + e.getMessage());
			}
		}

		result.put("total", deviceList.size());
		result.put("success", successCount);
		result.put("fail", failCount);
		result.put("errors", errors);

		return result;
	}

	@Override
	public List<DeviceInfo> exportDevices(List<Long> deviceIds) {
		if (deviceIds == null || deviceIds.isEmpty()) {
			return list();
		} else {
			return listByIds(deviceIds);
		}
	}

	@Override
	public Map<String, Object> getDeviceConfig(Long deviceId) {
		DeviceInfo deviceInfo = getById(deviceId);
		Map<String, Object> config = new HashMap<>();

		if (deviceInfo != null) {
			config.put("deviceId", deviceInfo.getDeviceId());
			config.put("deviceName", deviceInfo.getDeviceName());
			config.put("streamUrl", deviceInfo.getStreamUrl());
			config.put("deviceType", deviceInfo.getDeviceType());
		}

		return config;
	}

	@Override
	public boolean updateDeviceConfig(Long deviceId, Map<String, Object> config) {
		try {
			DeviceInfo device = getById(deviceId);
			if (device == null) {
				return false;
			}

			// Update configuration
			if (config.containsKey("deviceName")) {
				device.setDeviceName((String) config.get("deviceName"));
			}
			if (config.containsKey("streamUrl")) {
				device.setStreamUrl((String) config.get("streamUrl"));
			}

			return updateById(device);
		} catch (Exception e) {
			log.error("Failed to update device configuration", e);
			return false;
		}
	}

	@Override
	public Map<String, Object> ptzControl(Long deviceId, String command, Map<String, Object> params) {
		Map<String, Object> result = new HashMap<>();
		DeviceInfo deviceInfo = getById(deviceId);

		if (deviceInfo == null) {
			result.put("success", false);
			result.put("message", "Device does not exist");
			return result;
		}

		try {
			// Implement actual PTZ control logic here
			// TODO: Execute PTZ control based on device type and command
			log.info("PTZ control: device={}, command={}, parameters={}", deviceInfo.getDeviceName(), command, params);

			result.put("success", true);
			result.put("message", "PTZ control succeeded");
			result.put("command", command);
		} catch (Exception e) {
			log.error("PTZ control failed: {}", deviceInfo.getDeviceName(), e);
			result.put("success", false);
			result.put("message", "PTZ control failed:" + e.getMessage());
		}

		return result;
	}

	@Override
	public Map<String, Object> getVideoStreamInfo(Long deviceId) {
		DeviceInfo deviceInfo = getById(deviceId);
		Map<String, Object> streamInfo = new HashMap<>();

		if (deviceInfo != null) {
			streamInfo.put("deviceId", deviceInfo.getDeviceId());
			streamInfo.put("deviceName", deviceInfo.getDeviceName());
			streamInfo.put("streamUrl", deviceInfo.getStreamUrl());
			streamInfo.put("status", deviceInfo.getStatus());
			streamInfo.put("deviceType", deviceInfo.getDeviceType());
		}

		return streamInfo;
	}

}
