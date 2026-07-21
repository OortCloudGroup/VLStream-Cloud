/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springblade.core.mp.base.BaseServiceImpl;
import com.ruoyi.vlstream.test.vlstream.config.VlsHardwareDispatchProperties;
import com.ruoyi.vlstream.test.vlstream.excel.VlsDeviceInfoExcel;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsDeviceInfoMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.DeviceInfoVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAlgorithmTrainingService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsDeviceInfoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

/**
 * 设备信息表 服务实现类
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
	private VlsHardwareDispatchProperties hardwareDispatchProperties;

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
			// 自动生成device_id（如果没有提供）
			if (deviceInfo.getDeviceId() == null || deviceInfo.getDeviceId().trim().isEmpty()) {
				String deviceId = generateDeviceId();
				deviceInfo.setDeviceId(deviceId);
			} else if (checkDeviceIdExists(deviceInfo.getDeviceId())) {
				return false;
			}
			return save(deviceInfo);
		} catch (Exception e) {
			log.error("新增设备失败", e);
			return false;
		}
	}

	@Override
	public boolean updateDevice(DeviceInfo deviceInfo) {
		return updateById(deviceInfo);
	}

	@Override
	public boolean dispatchAlgorithms(Long algorithmId, String deviceIds) {
		if (algorithmId == null || StringUtils.isBlank(deviceIds)) {
			return false;
		}

		AlgorithmTraining latestTraining = vlsAlgorithmTrainingService.getOne(Wrappers.<AlgorithmTraining>lambdaQuery()
			.eq(AlgorithmTraining::getAlgorithmId, algorithmId)
			.isNotNull(AlgorithmTraining::getOmModelOutputPath)
			.ne(AlgorithmTraining::getOmModelOutputPath, "")
			.orderByDesc(AlgorithmTraining::getUpdateTime)
			.last("limit 1"));
		if (latestTraining == null) {
			log.error("算法下发失败，未找到已生成OM文件的训练任务: algorithmId={}", algorithmId);
			return false;
		}

		String modelDownloadUrl = buildPublicOmDownloadUrl(latestTraining.getId());
		if (StringUtils.isBlank(modelDownloadUrl)) {
			log.error("算法下发失败，模型公开下载地址未配置: algorithmId={}", algorithmId);
			return false;
		}

		boolean allSucceeded = true;
		int dispatchedCount = 0;
		for (String rowId : deviceIds.split(",")) {
			if (StringUtils.isBlank(rowId)) {
				continue;
			}
			DeviceInfo device;
			try {
				device = getById(Long.valueOf(rowId.trim()));
			} catch (NumberFormatException invalidIdException) {
				log.error("算法下发失败，设备主键格式错误: rowId={}", rowId);
				allSucceeded = false;
				continue;
			}
			if (device == null || StringUtils.isBlank(device.getDeviceId())) {
				log.error("算法下发失败，设备不存在或设备编号为空: rowId={}", rowId);
				allSucceeded = false;
				continue;
			}

			if (!requestHardwareModelDownload(device.getDeviceId(), modelDownloadUrl)) {
				allSucceeded = false;
				continue;
			}

			DeviceInfo updateEntity = new DeviceInfo();
			updateEntity.setId(device.getId());
			updateEntity.setAlgorithmId(String.valueOf(algorithmId));
			if (!updateById(updateEntity)) {
				log.error("硬件接口调用成功，但设备算法关联更新失败: rowId={}, deviceId={}", device.getId(), device.getDeviceId());
				allSucceeded = false;
				continue;
			}
			dispatchedCount++;
		}
		return allSucceeded && dispatchedCount > 0;
	}

	/**
	 * Build the unauthenticated OM URL that the hardware device can download directly.
	 */
	private String buildPublicOmDownloadUrl(Long trainingId) {
		String baseUrl = StringUtils.stripEnd(hardwareDispatchProperties.getModelDownloadBaseUrl(), "/");
		if (StringUtils.isBlank(baseUrl) || trainingId == null) {
			return null;
		}
		return baseUrl + "/vlsAlgorithmTraining/public/" + trainingId + "/om";
	}

	/**
	 * Ask the hardware service to download and activate the selected training model.
	 */
	private boolean requestHardwareModelDownload(String deviceId, String modelDownloadUrl) {
		String endpointUrl = hardwareDispatchProperties.getEndpointUrl();
		if (StringUtils.isBlank(endpointUrl)) {
			log.error("算法下发失败，硬件接口地址未配置");
			return false;
		}
		String requestUrl = UriComponentsBuilder.fromHttpUrl(endpointUrl.trim())
			.queryParam("deviceId", deviceId)
			.queryParam("modelDownloadPath", modelDownloadUrl)
			.build()
			.encode()
			.toUriString();
		int timeout = hardwareDispatchProperties.getRequestTimeoutMillis() == null
			? 10000 : Math.max(1000, hardwareDispatchProperties.getRequestTimeoutMillis());

		try (HttpResponse response = HttpRequest.get(requestUrl).timeout(timeout).execute()) {
			int status = response.getStatus();
			if (status >= 200 && status < 300) {
				log.info("硬件模型下发接口调用成功: deviceId={}, modelDownloadUrl={}", deviceId, modelDownloadUrl);
				return true;
			}
			log.error("硬件模型下发接口调用失败: deviceId={}, status={}, body={}",
				deviceId, status, StringUtils.abbreviate(response.body(), 500));
			return false;
		} catch (Exception requestException) {
			log.error("硬件模型下发接口调用异常: deviceId={}, endpoint={}", deviceId, endpointUrl, requestException);
			return false;
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
		return update(new DeviceInfo(), Wrappers.<DeviceInfo>update()
			.in("id", ids)
			.set("status", status));
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
			result.put("message", "设备不存在");
			return result;
		}

		try {
			// 这里实现实际的设备连接测试逻辑
			// TODO: 根据设备类型和连接参数测试连接
			log.info("测试设备连接: {}", deviceInfo.getDeviceName());

			result.put("success", true);
			result.put("message", "连接成功");
			result.put("latency", "20ms");
		} catch (Exception e) {
			log.error("设备连接测试失败: {}", deviceInfo.getDeviceName(), e);
			result.put("success", false);
			result.put("message", "连接失败: " + e.getMessage());
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
				case "离线":
					offline = stat.getCount();
					break;
				case "在线":
					online = stat.getCount();
					break;
				case "故障":
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

		// 验证设备名称
		if (deviceInfo.getDeviceName() == null || deviceInfo.getDeviceName().trim().isEmpty()) {
			errors.add("设备名称不能为空");
		}

		// 验证设备ID
		if (deviceInfo.getDeviceId() == null || deviceInfo.getDeviceId().trim().isEmpty()) {
			errors.add("设备ID不能为空");
		} else if (checkDeviceIdExists(deviceInfo.getDeviceId())) {
			errors.add("设备ID已存在");
		}

		if (errors.isEmpty()) {
			result.put("valid", true);
			result.put("message", "验证通过");
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
			result.put("message", "设备不存在");
			return result;
		}

		try {
			// 模拟状态刷新
			updateDeviceStatus(deviceId, 1);

			result.put("success", true);
			result.put("message", "状态刷新成功");
			result.put("status", 1);
		} catch (Exception e) {
			log.error("刷新设备状态失败: {}", deviceInfo.getDeviceName(), e);
			result.put("success", false);
			result.put("message", "状态刷新失败: " + e.getMessage());
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
				// 验证设备信息
				Map<String, Object> validation = validateDevice(device);
				if (!(Boolean) validation.get("valid")) {
					failCount++;
					errors.add("设备 " + device.getDeviceName() + " 验证失败");
					continue;
				}

				// 保存设备
				if (addDevice(device)) {
					successCount++;
				} else {
					failCount++;
					errors.add("设备 " + device.getDeviceName() + " 保存失败");
				}
			} catch (Exception e) {
				failCount++;
				errors.add("设备 " + device.getDeviceName() + " 处理异常: " + e.getMessage());
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

			// 更新配置
			if (config.containsKey("deviceName")) {
				device.setDeviceName((String) config.get("deviceName"));
			}
			if (config.containsKey("streamUrl")) {
				device.setStreamUrl((String) config.get("streamUrl"));
			}

			return updateById(device);
		} catch (Exception e) {
			log.error("更新设备配置失败", e);
			return false;
		}
	}

	@Override
	public Map<String, Object> ptzControl(Long deviceId, String command, Map<String, Object> params) {
		Map<String, Object> result = new HashMap<>();
		DeviceInfo deviceInfo = getById(deviceId);

		if (deviceInfo == null) {
			result.put("success", false);
			result.put("message", "设备不存在");
			return result;
		}

		try {
			// 这里实现实际的PTZ控制逻辑
			// TODO: 根据设备类型和命令执行PTZ控制
			log.info("PTZ控制: 设备={}, 命令={}, 参数={}", deviceInfo.getDeviceName(), command, params);

			result.put("success", true);
			result.put("message", "PTZ控制成功");
			result.put("command", command);
		} catch (Exception e) {
			log.error("PTZ控制失败: {}", deviceInfo.getDeviceName(), e);
			result.put("success", false);
			result.put("message", "PTZ控制失败: " + e.getMessage());
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
