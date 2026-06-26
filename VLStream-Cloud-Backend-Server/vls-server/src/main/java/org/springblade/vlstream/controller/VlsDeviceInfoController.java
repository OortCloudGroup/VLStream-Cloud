package org.springblade.vlstream.controller;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.excel.util.ExcelUtil;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.vlstream.detection.*;
import org.springblade.vlstream.excel.VlsDeviceInfoExcel;
import org.springblade.vlstream.pojo.dto.DeviceTagRelationDTO;
import org.springblade.vlstream.pojo.entity.*;
import org.springblade.vlstream.pojo.vo.AlgorithmModelVO;
import org.springblade.vlstream.pojo.vo.DeviceInfoVO;
import org.springblade.vlstream.service.*;
import org.springblade.vlstream.wrapper.VlsAlgorithmModelWrapper;
import org.springblade.vlstream.wrapper.VlsDeviceInfoWrapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Device Information Table Controller
 *
 * @author Oort
 * @since 2025-12-23
 */
@RestController
@AllArgsConstructor
@RequestMapping("/vlsDeviceInfo")
@Tag(name = "Device Information Table", description = "Device Information Table Interface")
public class VlsDeviceInfoController extends BladeController {

	private final IVlsDeviceInfoService vlsDeviceInfoService;
	private final IVlsDeviceTagRelationService deviceTagRelationService;
	private final IVlsAlgorithmService vlsAlgorithmService;
	private final IVlsAlgorithmTrainingService vlsAlgorithmTrainingService;
	private final IVlsAlgorithmModelService vlsAlgorithmModelService;
	private final ObjectProvider<DeviceClassifyDetectionManager> deviceClassifyDetectionManagerProvider;
	private final ObjectProvider<DeviceInstanceSegDetectionManager> deviceInstanceSegDetectionManagerProvider;
	private final ObjectProvider<DeviceObbDetectionManager> deviceObbDetectionManagerProvider;
	private final ObjectProvider<DeviceObjectDetectionManager> deviceObjectDetectionManagerProvider;
	private final ObjectProvider<DevicePersonDetectionManager> devicePersonDetectionManagerProvider;
	private final ObjectProvider<DevicePoseDetectionManager> devicePoseDetectionManagerProvider;
	private final ObjectProvider<DeviceSemSegDetectionManager> deviceSemSegDetectionManagerProvider;

	/**
	 * Device Information Table Details
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "Details", description = "Pass in vlsDeviceInfo")
	public R<DeviceInfoVO> detail(DeviceInfo vlsDeviceInfo) {
		DeviceInfo detail = vlsDeviceInfoService.getOne(Condition.getQueryWrapper(vlsDeviceInfo));
		DeviceInfoVO deviceInfoVO = VlsDeviceInfoWrapper.build().entityVO(detail);
		fillAlgorithmName(deviceInfoVO);
		return R.data(deviceInfoVO);
	}

	/**
	 * Device Information Table Pagination
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "Pagination", description = "Pass in vlsDeviceInfo")
	public R<IPage<DeviceInfoVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsDeviceInfo, Query query) {
		IPage<DeviceInfo> pages = vlsDeviceInfoService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsDeviceInfo, DeviceInfo.class));
		IPage<DeviceInfoVO> pageVO = VlsDeviceInfoWrapper.build().pageVO(pages);
		fillAlgorithmName(pageVO.getRecords());
		return R.data(pageVO);
	}


	/**
	 * Device Information Table Custom Pagination
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "Pagination", description = "Pass in vlsDeviceInfo")
	public R<IPage<DeviceInfoVO>> page(DeviceInfoVO vlsDeviceInfo, Query query) {
		IPage<DeviceInfoVO> pages = vlsDeviceInfoService.selectVlsDeviceInfoPage(Condition.getPage(query), vlsDeviceInfo);
		fillAlgorithmName(pages.getRecords());
		return R.data(pages);
	}

	private void fillAlgorithmName(DeviceInfoVO deviceInfoVO) {
		if (deviceInfoVO == null) {
			return;
		}
		List<DeviceInfoVO> deviceInfoVOList = new ArrayList<>();
		deviceInfoVOList.add(deviceInfoVO);
		fillAlgorithmName(deviceInfoVOList);
	}

	private void fillAlgorithmName(List<DeviceInfoVO> deviceInfoVOList) {
		if (deviceInfoVOList == null || deviceInfoVOList.isEmpty()) {
			return;
		}
		Set<Long> algorithmIdSet = new HashSet<>();
		for (DeviceInfoVO deviceInfoVO : deviceInfoVOList) {
			if (deviceInfoVO == null || StringUtils.isBlank(deviceInfoVO.getAlgorithmId())) {
				continue;
			}
			String[] algorithmIdArray = deviceInfoVO.getAlgorithmId().split(",");
			for (String algorithmIdText : algorithmIdArray) {
				if (StringUtils.isBlank(algorithmIdText)) {
					continue;
				}
				String algorithmIdTrimText = algorithmIdText.trim();
				if (!StringUtils.isNumeric(algorithmIdTrimText)) {
					continue;
				}
				algorithmIdSet.add(Long.valueOf(algorithmIdTrimText));
			}
		}
		if (algorithmIdSet.isEmpty()) {
			return;
		}
		List<Algorithm> algorithmList = vlsAlgorithmService.listByIds(algorithmIdSet);
		Map<Long, String> algorithmNameMap = new HashMap<>();
		for (Algorithm algorithm : algorithmList) {
			if (algorithm == null || algorithm.getId() == null) {
				continue;
			}
			algorithmNameMap.put(algorithm.getId(), algorithm.getName());
		}
		for (DeviceInfoVO deviceInfoVO : deviceInfoVOList) {
			if (deviceInfoVO == null || StringUtils.isBlank(deviceInfoVO.getAlgorithmId())) {
				continue;
			}
			List<String> algorithmNameList = new ArrayList<>();
			String[] algorithmIdArray = deviceInfoVO.getAlgorithmId().split(",");
			for (String algorithmIdText : algorithmIdArray) {
				if (StringUtils.isBlank(algorithmIdText)) {
					continue;
				}
				String algorithmIdTrimText = algorithmIdText.trim();
				if (!StringUtils.isNumeric(algorithmIdTrimText)) {
					continue;
				}
				String algorithmName = algorithmNameMap.get(Long.valueOf(algorithmIdTrimText));
				if (StringUtils.isNotBlank(algorithmName)) {
					algorithmNameList.add(algorithmName);
				}
			}
			deviceInfoVO.setAlgorithmName(String.join(",", algorithmNameList));
		}
	}

	/**
	 * Device Information Table Add
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "Add", description = "Pass in vlsDeviceInfo")
	public R save(@Valid @RequestBody DeviceInfo vlsDeviceInfo) {
		return R.status(vlsDeviceInfoService.save(vlsDeviceInfo));
	}

	/**
	 * Device Information Table Modify
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "Modify", description = "Pass in vlsDeviceInfo")
	public R update(@Valid @RequestBody DeviceInfo vlsDeviceInfo) {
		String beforeAlgorithmIds = null;
		if (vlsDeviceInfo != null && vlsDeviceInfo.getId() != null) {
			DeviceInfo before = vlsDeviceInfoService.getById(vlsDeviceInfo.getId());
			beforeAlgorithmIds = before == null ? null : before.getAlgorithmId();
		}
		String afterAlgorithmIds = vlsDeviceInfo == null ? null : vlsDeviceInfo.getAlgorithmId();
		boolean algorithmChanged = !normalizeAlgorithmIds(beforeAlgorithmIds).equals(normalizeAlgorithmIds(afterAlgorithmIds));
		boolean algorithmProvided = afterAlgorithmIds != null;

		boolean updated = vlsDeviceInfoService.updateById(vlsDeviceInfo);
		if (updated && (algorithmChanged || algorithmProvided)) {
			refreshDeviceDetection();
		}
		return R.status(updated);
	}

	/**
	 * Device Information Table Add or Modify
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "Add or modify", description = "Pass in vlsDeviceInfo")
	public R submit(@Valid @RequestBody DeviceInfo vlsDeviceInfo) {
		String beforeAlgorithmIds = null;
		boolean hasBefore = false;
		if (vlsDeviceInfo != null && vlsDeviceInfo.getId() != null) {
			DeviceInfo before = vlsDeviceInfoService.getById(vlsDeviceInfo.getId());
			if (before != null) {
				hasBefore = true;
				beforeAlgorithmIds = before.getAlgorithmId();
			}
		}
		String afterAlgorithmIds = vlsDeviceInfo == null ? null : vlsDeviceInfo.getAlgorithmId();
		String normalizedBefore = normalizeAlgorithmIds(beforeAlgorithmIds);
		String normalizedAfter = normalizeAlgorithmIds(afterAlgorithmIds);
		boolean algorithmProvided = afterAlgorithmIds != null;
		boolean shouldRefresh = hasBefore
			? (algorithmProvided || !normalizedBefore.equals(normalizedAfter))
			: StringUtils.isNotBlank(normalizedAfter);

		boolean saved = vlsDeviceInfoService.saveOrUpdate(vlsDeviceInfo);
		if (saved && shouldRefresh) {
			refreshDeviceDetection();
		}
		return R.status(saved);
	}

	private void refreshDeviceDetection() {
		CompletableFuture.runAsync(() -> {
			DeviceClassifyDetectionManager classifyDetectionManager = deviceClassifyDetectionManagerProvider.getIfAvailable();
			if (classifyDetectionManager != null) {
				classifyDetectionManager.refreshNow();
			}
			DeviceInstanceSegDetectionManager instanceSegDetectionManager = deviceInstanceSegDetectionManagerProvider.getIfAvailable();
			if (instanceSegDetectionManager != null) {
				instanceSegDetectionManager.refreshNow();
			}
			DeviceObbDetectionManager obbDetectionManager = deviceObbDetectionManagerProvider.getIfAvailable();
			if (obbDetectionManager != null) {
				obbDetectionManager.refreshNow();
			}
			DeviceObjectDetectionManager objectDetectionManager = deviceObjectDetectionManagerProvider.getIfAvailable();
			if (objectDetectionManager != null) {
				objectDetectionManager.refreshNow();
			}
			DevicePoseDetectionManager poseDetectionManager = devicePoseDetectionManagerProvider.getIfAvailable();
			if (poseDetectionManager != null) {
				poseDetectionManager.refreshNow();
			}
			DeviceSemSegDetectionManager semSegDetectionManager = deviceSemSegDetectionManagerProvider.getIfAvailable();
			if (semSegDetectionManager != null) {
				semSegDetectionManager.refreshNow();
			}
			DevicePersonDetectionManager personDetectionManager = devicePersonDetectionManagerProvider.getIfAvailable();
			if (personDetectionManager != null) {
				personDetectionManager.refreshNow();
			}
		});
	}

	private String normalizeAlgorithmIds(String algorithmIds) {
		if (StringUtils.isBlank(algorithmIds)) {
			return "";
		}
		String[] parts = algorithmIds.split(",");
		List<String> normalized = new ArrayList<>();
		for (String part : parts) {
			String trimmed = part == null ? null : part.trim();
			if (StringUtils.isBlank(trimmed)) {
				continue;
			}
			if (!normalized.contains(trimmed)) {
				normalized.add(trimmed);
			}
		}
		return String.join(",", normalized);
	}

	/**
	 * Device Information Table Delete
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "Logical delete", description = "Pass in ids")
	public R remove(@Parameter(description = "Primary key collection", required = true) @RequestParam String ids) {
		return R.status(vlsDeviceInfoService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * Export data
	 */
	@GetMapping("/export-vlsDeviceInfo")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "Export data", description = "Pass in vlsDeviceInfo")
	public void exportVlsDeviceInfo(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsDeviceInfo, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<DeviceInfo> queryWrapper = Condition.getQueryWrapper(vlsDeviceInfo, DeviceInfo.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsDeviceInfoEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsDeviceInfoEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsDeviceInfoExcel> list = vlsDeviceInfoService.exportVlsDeviceInfo(queryWrapper);
		ExcelUtil.export(response, "Device Information Table Data" + DateUtil.today(), "Device Information Table Data Table", list, VlsDeviceInfoExcel.class);
	}

	/**
	 * Query device information by ID
	 */
	@Operation(summary = "Query device information by ID")
	@GetMapping("/{id}")
	public R<Map<String, Object>> getDeviceById(@PathVariable Long id) {
		DeviceInfo deviceInfo = vlsDeviceInfoService.getById(id);
		if (deviceInfo == null) {
			return R.fail("Device does not exist");
		}

		Map<String, Object> result = buildDeviceInfoMap(deviceInfo);
		return R.data(result);
	}

	@Operation(summary = "Get device training model")
	@GetMapping("/latest-training-model")
	public R<AlgorithmModelVO> getLatestTrainingModel(@RequestParam String deviceId) {
		DeviceInfo query = new DeviceInfo();
		query.setDeviceId(deviceId);
		DeviceInfo deviceInfo = vlsDeviceInfoService.queryDetail(query);
		if (deviceInfo == null) {
			return R.fail("Device not found");
		}
		String algorithmIdText = deviceInfo.getAlgorithmId();
		if (StringUtils.isBlank(algorithmIdText)) {
			return R.fail("Device algorithm not set");
		}
		Long algorithmId;
		try {
			algorithmId = Long.valueOf(algorithmIdText.trim());
		} catch (NumberFormatException parseException) {
			return R.fail("Device algorithm id invalid");
		}

		AlgorithmTraining latestTraining = vlsAlgorithmTrainingService.getOne(Wrappers.<AlgorithmTraining>lambdaQuery()
			.eq(AlgorithmTraining::getAlgorithmId, algorithmId)
			.orderByDesc(AlgorithmTraining::getUpdateTime)
			.last("limit 1"));
		if (latestTraining == null) {
			return R.fail("Training task not found");
		}

		AlgorithmModel latestModel = vlsAlgorithmModelService.getOne(Wrappers.<AlgorithmModel>lambdaQuery()
			.eq(AlgorithmModel::getTrainingId, latestTraining.getId())
			.orderByDesc(AlgorithmModel::getCreateTime)
			.last("limit 1"));
		if (latestModel == null) {
			return R.fail("Model not found");
		}
		return R.data(VlsAlgorithmModelWrapper.build().entityVO(latestModel));
	}


	/**
	 * Query device info by device number
	 */
	@Operation(summary = "Query device info by device number")
	@GetMapping("/deviceId/{deviceId}")
	public R<Map<String, Object>> getDeviceByDeviceId(@PathVariable String deviceId) {
		DeviceInfo deviceInfo = vlsDeviceInfoService.getByDeviceId(deviceId);
		if (deviceInfo == null) {
			return R.fail("Device does not exist");
		}

		Map<String, Object> result = buildDeviceInfoMap(deviceInfo);
		return R.data(result);
	}

	/**
	 * Add device information
	 */
	@Operation(summary = "Add device information")
	@PostMapping
	public R<String> addDevice(@Valid @RequestBody DeviceInfo deviceInfo) {
		// Check if device number already exists
		if (vlsDeviceInfoService.checkDeviceIdExists(deviceInfo.getDeviceId())) {
			return R.fail("Device number already exists");
		}

		boolean success = vlsDeviceInfoService.addDevice(deviceInfo);
		if (success) {
			return R.success("Successfully added");
		} else {
			return R.fail("Failed to add");
		}
	}

	/**
	 * Update device information
	 */
	@Operation(summary = "Update device information")
	@PutMapping("/{id}")
	public R<String> updateDevice(@PathVariable Long id, @RequestBody Map<String, Object> requestData) {

		// Extract device information
		DeviceInfo deviceInfo = extractDeviceInfo(requestData);
		deviceInfo.setId(id);

		// Update device information
		boolean success = vlsDeviceInfoService.updateDevice(deviceInfo);
		if (!success) {
			return R.fail("Device information update failed");
		}

		// Process device tags
		if (requestData.containsKey("selectedTags")) {
			Object selectedTagsObj = requestData.get("selectedTags");
			if (selectedTagsObj instanceof List) {
				@SuppressWarnings("unchecked")
				List<Object> selectedTagsList = (List<Object>) selectedTagsObj;
				List<Long> tagIds = new ArrayList<>();

				for (Object tagObj : selectedTagsList) {
					if (tagObj instanceof Number) {
						tagIds.add(((Number) tagObj).longValue());
					} else if (tagObj instanceof String) {
						try {
							tagIds.add(Long.parseLong((String) tagObj));
						} catch (NumberFormatException e) {
							System.err.println("Invalid label ID:" + tagObj);
						}
					}
				}

				if (!tagIds.isEmpty()) {
					boolean tagSuccess = deviceTagRelationService.setDeviceTags(id, tagIds, "admin");
					if (!tagSuccess) {
						System.err.println("Failed to save device tag, device ID:" + id);
					}
				}
			}
		}

		return R.success("Update successful");
	}

	/**
	 * Camera algorithm delivery
	 */
	@Operation(summary = "Camera algorithm delivery")
	@GetMapping("/dispatchAlgorithms")
	public R<String> dispatchAlgorithms(@RequestParam Long algorithmId, @RequestParam String deviceIds) {
		boolean success = vlsDeviceInfoService.dispatchAlgorithms(algorithmId, deviceIds);
		if (success) {
			return R.success("Algorithm delivery succeeded");
		} else {
			return R.fail("Algorithm delivery failed, device does not exist");
		}
	}

	/**
	 * Extract device information from request data
	 */
	private DeviceInfo extractDeviceInfo(Map<String, Object> requestData) {
		DeviceInfo deviceInfo = new DeviceInfo();

		// Set basic fields
		setIfNotNull(deviceInfo::setDeviceName, requestData.get("deviceName"));
		setIfNotNull(deviceInfo::setDeviceId, requestData.get("deviceId"));
		setIfNotNull(deviceInfo::setStreamUrl, requestData.get("streamUrl"));
		setIfNotNull(deviceInfo::setDeviceType, requestData.get("deviceType"));
		setIfNotNull(deviceInfo::setRemark, requestData.get("remark"));

		// Process newly added fields
		setIfNotNull(deviceInfo::setTag, requestData.get("tag"));
		setIfNotNull(deviceInfo::setImagePath, requestData.get("imagePath"));
		setIfNotNull(deviceInfo::setHeightPosition, requestData.get("heightPosition"));
		setIfNotNull(deviceInfo::setAddress, requestData.get("address"));

		// Process region field (JSON format)
		if (requestData.containsKey("region")) {
			Object regionObj = requestData.get("region");
			if (regionObj != null) {
				if (regionObj instanceof List) {
					@SuppressWarnings("unchecked")
					List<String> regionList = (List<String>) regionObj;
					// Simple JSON format conversion, avoiding introduction of Jackson dependency
					String regionJson = "[\"" + String.join("\",\"", regionList) + "\"]";
					deviceInfo.setRegion(regionJson);
				} else if (regionObj instanceof String) {
					deviceInfo.setRegion((String) regionObj);
				}
			}
		}

		return deviceInfo;
	}

	/**
	 * Helper method: set if value is not null
	 */
	private void setIfNotNull(java.util.function.Consumer<String> setter, Object value) {
		if (value != null) {
			setter.accept(value.toString());
		}
	}

	/**
	 * Build a complete map of device information, including associated label information
	 */
	private Map<String, Object> buildDeviceInfoMap(DeviceInfo deviceInfo) {
		Map<String, Object> result = new HashMap<>();

		// Device Basic Information
		result.put("id", deviceInfo.getId());
		result.put("deviceName", deviceInfo.getDeviceName());
		result.put("deviceId", deviceInfo.getDeviceId());
		result.put("streamUrl", deviceInfo.getStreamUrl());
		result.put("status", deviceInfo.getStatus());
		result.put("deviceType", deviceInfo.getDeviceType());
		result.put("remark", deviceInfo.getRemark());
		result.put("createTime", deviceInfo.getCreateTime());
		result.put("updateTime", deviceInfo.getUpdateTime());

		// Add field
		result.put("tag", deviceInfo.getTag());
		result.put("longitude", deviceInfo.getLongitude());
		result.put("latitude", deviceInfo.getLatitude());
		result.put("imagePath", deviceInfo.getImagePath());
		result.put("heightPosition", deviceInfo.getHeightPosition());
		result.put("address", deviceInfo.getAddress());
		result.put("region", deviceInfo.getRegion());

		// Query Associated Label Information
		try {
			List<DeviceTagRelationDTO> tagRelations = deviceTagRelationService.getDeviceTags(deviceInfo.getId());
			List<Long> selectedTags = new ArrayList<>();
			for (DeviceTagRelation relation : tagRelations) {
				selectedTags.add(relation.getTagId());
			}
			result.put("selectedTags", selectedTags);
		} catch (Exception e) {
			System.err.println("Failed to query device label:" + e.getMessage());
			result.put("selectedTags", new ArrayList<>());
		}

		return result;
	}

	/**
	 * Delete device information
	 */
	@Operation(description = "Delete device information")
	@DeleteMapping("/{id}")
	public R<String> deleteDevice(@PathVariable Long id) {
		boolean success = vlsDeviceInfoService.deleteDevice(id);
		if (success) {
			return R.success("Deleted successfully");
		} else {
			return R.fail("Delete failed");
		}
	}

	/**
	 * Batch delete device information
	 */
	@Operation(description = "Batch delete device information")
	@DeleteMapping("/batch")
	public R<String> deleteDeviceBatch(@RequestBody List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return R.fail("Please select devices to delete");
		}

		boolean success = vlsDeviceInfoService.deleteDeviceBatch(ids);
		if (success) {
			return R.success("Batch delete succeeded");
		} else {
			return R.fail("Batch delete failed");
		}
	}

	/**
	 * Update device status
	 */
	@Operation(description = "Update device status")
	@PutMapping("/{id}/status/{status}")
	public R<String> updateDeviceStatus(@PathVariable Long id, @PathVariable Integer status) {

		boolean success = vlsDeviceInfoService.updateDeviceStatus(id, status);
		if (success) {
			return R.success("Status updated successfully");
		} else {
			return R.fail("Failed to update status");
		}
	}

	/**
	 * Batch update device status
	 */
	@Operation(description = "Batch update device status")
	@PutMapping("/status/{status}")
	public R<String> updateDeviceStatusBatch(@PathVariable String status, @RequestBody List<Long> ids) {

		if (ids == null || ids.isEmpty()) {
			return R.fail("Please select devices to update");
		}

		boolean success = vlsDeviceInfoService.updateDeviceStatusBatch(ids, status);
		if (success) {
			return R.success("Successfully batch updated status");
		} else {
			return R.fail("Failed to batch update status");
		}
	}

	/**
	 * Query device list by status
	 */
	@Operation(description = "Query device list by status")
	@GetMapping("/status/{status}")
	public R<List<DeviceInfo>> getDevicesByStatus(@PathVariable String status) {
		List<DeviceInfo> devices = vlsDeviceInfoService.getDevicesByStatus(status);
		return R.data(devices);
	}

	/**
	 * Query device list by device type
	 */
	@Operation(description = "Query device list by device type")
	@GetMapping("/type/{deviceType}")
	public R<List<DeviceInfo>> getDevicesByType(@PathVariable String deviceType) {
		List<DeviceInfo> devices = vlsDeviceInfoService.getDevicesByType(deviceType);
		return R.data(devices);
	}

	/**
	 * Test device connection
	 */
	@Operation(description = "Test device connection")
	@PostMapping("/{id}/test")
	public R<Map<String, Object>> testDeviceConnection(@PathVariable Long id) {
		Map<String, Object> result = vlsDeviceInfoService.testDeviceConnection(id);
		if ((Boolean) result.get("success")) {
			return R.data(result);
		} else {
			return R.fail((String) result.get("message"));
		}
	}

	/**
	 * Get device statistics
	 */
	@Operation(description = "Get device statistics")
	@GetMapping("/statistics")
	public R<Map<String, Object>> getDeviceStatistics() {
		Map<String, Object> statistics = vlsDeviceInfoService.getDeviceStatistics();
		return R.data(statistics);
	}

	/**
	 * Get device grouping statistics (grouped by label)
	 */
	@Operation(description = "Get device grouping statistics")
	@GetMapping("/group-statistics")
	public R<List<Map<String, Object>>> getDeviceGroupStatistics() {
		// Get all devices
		List<DeviceInfo> allDevices = vlsDeviceInfoService.list();

		// Statistics grouped by device type
		Map<String, List<DeviceInfo>> devicesByType = new HashMap<>();
		for (DeviceInfo deviceInfo : allDevices) {
			String type = deviceInfo.getDeviceType();
			if (type == null || type.trim().isEmpty()) {
				type = "Uncategorized";
			}
			devicesByType.computeIfAbsent(type, k -> new ArrayList<>()).add(deviceInfo);
		}

		// Build Statistical Results
		List<Map<String, Object>> result = new ArrayList<>();
		for (Map.Entry<String, List<DeviceInfo>> entry : devicesByType.entrySet()) {
			String typeName = entry.getKey();
			List<DeviceInfo> devices = entry.getValue();

			Map<String, Object> groupStat = new HashMap<>();
			groupStat.put("type", typeName);
			groupStat.put("total", devices.size());

			// Count quantity of each status
			long online = devices.stream().filter(d -> "Online".equals(d.getStatus())).count();
			long offline = devices.stream().filter(d -> "Offline".equals(d.getStatus())).count();
			long fault = devices.stream().filter(d -> "Fault".equals(d.getStatus())).count();

			groupStat.put("online", online);
			groupStat.put("offline", offline);
			groupStat.put("fault", fault);

			result.add(groupStat);
		}

		return R.data(result);
	}

	/**
	 * Get device type statistics
	 */
	@Operation(description = "Get device type statistics")
	@GetMapping("/type-statistics")
	public R<Map<String, Object>> getDeviceTypeStatistics() {
		// Get all device types
		List<String> allTypes = vlsDeviceInfoService.getAllTags();
		Map<String, Object> statistics = new HashMap<>();

		for (String type : allTypes) {
			List<DeviceInfo> devices = vlsDeviceInfoService.getDevicesByType(type);
			statistics.put(type, devices.size());
		}

		return R.data(statistics);
	}

	/**
	 * Get all device type list (label list)
	 */
	@Operation(description = "Get all device type list")
	@GetMapping("/tags")
	public R<List<String>> getDeviceTags() {
		List<String> tags = vlsDeviceInfoService.getAllTags();
		return R.data(tags);
	}

	/**
	 * Get all device brand list
	 */
	@Operation(description = "Get all device brand list")
	@GetMapping("/brands")
	public R<List<String>> getDeviceBrands() {
		List<String> brands = vlsDeviceInfoService.getAllBrands();
		return R.data(brands);
	}

	/**
	 * Refresh device status
	 */
	@Operation(description = "Refresh device status")
	@PostMapping("/{id}/refresh")
	public R<String> refreshDeviceStatus(@PathVariable Long id) {
		Map<String, Object> result = vlsDeviceInfoService.refreshDeviceStatus(id);
		if ((Boolean) result.get("success")) {
			return R.success((String) result.get("message"));
		} else {
			return R.fail((String) result.get("message"));
		}
	}

	/**
	 * Batch refresh device status
	 */
	@Operation(description = "Batch refresh device status")
	@PostMapping("/batch/refresh")
	public R<String> batchRefreshDevices(@RequestBody Map<String, List<Long>> request) {
		List<Long> ids = request.get("ids");
		if (ids == null || ids.isEmpty()) {
			return R.fail("Please select devices to refresh");
		}

		int successCount = 0;
		for (Long id : ids) {
			Map<String, Object> result = vlsDeviceInfoService.refreshDeviceStatus(id);
			if ((Boolean) result.get("success")) {
				successCount++;
			}
		}

		return R.success("Batch refresh completed, successfully" + successCount + "devices");
	}

	/**
	 * PTZ control - Move
	 */
	@Operation(description = "PTZ control - Move")
	@PostMapping("/{id}/ptz/move")
	public R<String> ptzMove(@PathVariable Long id, @RequestBody Map<String, Object> params) {

		String direction = (String) params.get("direction");
		Integer speed = (Integer) params.getOrDefault("speed", 4);

		// Check if device exists
		DeviceInfo device = vlsDeviceInfoService.getById(id);
		if (device == null) {
			return R.fail("Device does not exist");
		}

		Map<String, Object> result = vlsDeviceInfoService.ptzControl(id, "move", params);
		if ((Boolean) result.get("success")) {
			return R.success("PTZ move succeeded:" + direction + ", speed:" + speed);
		} else {
			return R.fail((String) result.get("message"));
		}
	}

	/**
	 * PTZ control - Stop
	 */
	@Operation(description = "PTZ control - Stop")
	@PostMapping("/{id}/ptz/stop")
	public R<String> ptzStop(@PathVariable Long id) {
		// Check if device exists
		DeviceInfo device = vlsDeviceInfoService.getById(id);
		if (device == null) {
			return R.fail("Device does not exist");
		}

		Map<String, Object> result = vlsDeviceInfoService.ptzControl(id, "stop", new HashMap<>());
		if ((Boolean) result.get("success")) {
			return R.success("PTZ stop succeeded");
		} else {
			return R.fail((String) result.get("message"));
		}
	}

	/**
	 * PTZ control - Zoom
	 */
	@Operation(description = "PTZ control - Zoom")
	@PostMapping("/{id}/ptz/zoom")
	public R<String> ptzZoom(@PathVariable Long id,
		@RequestBody Map<String, Object> params) {

		String action = (String) params.get("action");
		Integer speed = (Integer) params.getOrDefault("speed", 4);

		// Check if device exists
		DeviceInfo device = vlsDeviceInfoService.getById(id);
		if (device == null) {
			return R.fail("Device does not exist");
		}

		Map<String, Object> result = vlsDeviceInfoService.ptzControl(id, "zoom", params);
		if ((Boolean) result.get("success")) {
			return R.success("PTZ zoom succeeded:" + action + ", speed:" + speed);
		} else {
			return R.fail((String) result.get("message"));
		}
	}

	/**
	 * Get device video stream info
	 */
	@Operation(description = "Get device video stream info")
	@GetMapping("/{id}/stream")
	public R<Map<String, Object>> getDeviceStreamInfo(@PathVariable Long id) {
		Map<String, Object> streamInfo = vlsDeviceInfoService.getVideoStreamInfo(id);
		if (streamInfo.isEmpty()) {
			return R.fail("Device does not exist");
		}
		return R.data(streamInfo);
	}

	/**
	 * Export device list
	 */
	@Operation(description = "Export device list")
	@GetMapping("/export")
	public R<List<DeviceInfo>> exportDevices(@RequestParam(required = false) List<Long> deviceIds) {
		List<DeviceInfo> devices = vlsDeviceInfoService.exportDevices(deviceIds);
		return R.data(devices);
	}

	/**
	 * Import device list
	 */
	@Operation(description = "Import device list")
	@PostMapping("/import")
	public R<Map<String, Object>> importDevices(@RequestParam("file") MultipartFile file) {
		// TODO: Implement file parsing and device import functions
		Map<String, Object> result = new HashMap<>();
		result.put("message", "Import feature to be implemented");
		return R.data(result);
	}

	/**
	 * Get device configuration
	 */
	@Operation(description = "Get device configuration")
	@GetMapping("/{id}/config")
	public R<Map<String, Object>> getDeviceConfig(@PathVariable Long id) {
		Map<String, Object> config = vlsDeviceInfoService.getDeviceConfig(id);
		if (config.isEmpty()) {
			return R.fail("Device does not exist");
		}
		return R.data(config);
	}

	/**
	 * Update device configuration
	 */
	@Operation(description = "Update device configuration")
	@PutMapping("/{id}/config")
	public R<String> updateDeviceConfig( @PathVariable Long id,
		@RequestBody Map<String, Object> config) {

		boolean success = vlsDeviceInfoService.updateDeviceConfig(id, config);
		if (success) {
			return R.success("Configuration updated successfully");
		} else {
			return R.fail("Failed to update configuration");
		}
	}

	// ==================== Device tag related interfaces ====================

	/**
	 * Set device tags
	 */
	@Operation(description = "Set device tags")
	@PutMapping("/{id}/tags")
	public R<String> setDeviceTags(@PathVariable Long id, @RequestBody List<Long> tagIds) {

		// Verify if device exists
		DeviceInfo device = vlsDeviceInfoService.getById(id);
		if (device == null) {
			return R.fail("Device does not exist");
		}

		boolean success = deviceTagRelationService.setDeviceTags(id, tagIds, "admin");
		if (success) {
			return R.success("Device tags set successfully");
		} else {
			return R.fail("Failed to set device tags");
		}
	}

	/**
	 * Get device labels
	 */
	@Operation(description = "Get device labels")
	@GetMapping("/{id}/tags")
	public R<List<DeviceTagRelationDTO>> getDeviceTags(@PathVariable Long id) {

		// Verify if device exists
		DeviceInfo device = vlsDeviceInfoService.getById(id);
		if (device == null) {
			return R.fail("Device does not exist");
		}

		List<DeviceTagRelationDTO> tags = deviceTagRelationService.getDeviceTags(id);
		return R.data(tags);
	}

	/**
	 * Add device tags
	 */
	@Operation(description = "Add device tags")
	@PostMapping("/{id}/tags")
	public R<String> addDeviceTags(@PathVariable Long id, @RequestBody List<Long> tagIds) {

		// Verify if device exists
		DeviceInfo device = vlsDeviceInfoService.getById(id);
		if (device == null) {
			return R.fail("Device does not exist");
		}

		boolean success = deviceTagRelationService.addDeviceTags(id, tagIds, "admin");
		if (success) {
			return R.success("Device tag added successfully");
		} else {
			return R.fail("Failed to add device tag");
		}
	}

	/**
	 * Remove device tags
	 */
	@Operation(description = "Remove device tags")
	@DeleteMapping("/{id}/tags")
	public R<String> removeDeviceTags(@PathVariable Long id, @RequestBody List<Long> tagIds) {

		// Verify if device exists
		DeviceInfo device = vlsDeviceInfoService.getById(id);
		if (device == null) {
			return R.fail("Device does not exist");
		}

		boolean success = deviceTagRelationService.removeDeviceTags(id, tagIds);
		if (success) {
			return R.success("Device tag removed successfully");
		} else {
			return R.fail("Failed to remove device tag");
		}
	}

	/**
	 * Clear all tags of the device
	 */
	@Operation(description = "Clear all tags of the device")
	@DeleteMapping("/{id}/tags/all")
	public R<String> clearDeviceTags(@PathVariable Long id) {

		// Verify if device exists
		DeviceInfo device = vlsDeviceInfoService.getById(id);
		if (device == null) {
			return R.fail("Device does not exist");
		}

		boolean success = deviceTagRelationService.clearDeviceTags(id);
		if (success) {
			return R.success("Device tag cleared successfully");
		} else {
			return R.fail("Failed to clear device tag");
		}
	}

	/**
	 * Get device label details
	 */
	@Operation(description = "Get device label details")
	@GetMapping("/{id}/tag-details")
	public R<Map<String, Object>> getDeviceTagDetails(@PathVariable Long id) {

		// Verify if device exists
		DeviceInfo device = vlsDeviceInfoService.getById(id);
		if (device == null) {
			return R.fail("Device does not exist");
		}

		Map<String, Object> details = deviceTagRelationService.getDeviceTagDetails(id);
		return R.data(details);
	}

	/**
	 * Copy device tags
	 */
	@Operation(description = "Copy device tags to other devices")
	@PostMapping("/{sourceId}/copy-tags")
	public R<String> copyDeviceTags(@PathVariable Long sourceId, @RequestBody List<Long> targetDeviceIds) {

		// Verify if source device exists
		DeviceInfo sourceDevice = vlsDeviceInfoService.getById(sourceId);
		if (sourceDevice == null) {
			return R.fail("Source device does not exist");
		}

		// Verify if all target devices exist
		for (Long targetId : targetDeviceIds) {
			DeviceInfo targetDevice = vlsDeviceInfoService.getById(targetId);
			if (targetDevice == null) {
				return R.fail("Target device does not exist:" + targetId);
			}
		}

		boolean success = deviceTagRelationService.copyDeviceTags(sourceId, targetDeviceIds, "admin");
		if (success) {
			return R.success("Device tags copied successfully");
		} else {
			return R.fail("Failed to copy device tags");
		}
	}

	/**
	 * Get device tree structure
	 */
	@Operation(description = "Get device tree structure")
	@GetMapping("/tree")
	public R<List<Map<String, Object>>> getDeviceTree() {
		try {
			// Define fixed device types
			String[] deviceTypes = {"Dome camera", "PTZ", "Camera", "Bullet Camera", "Hemisphere"};

			List<Map<String, Object>> treeData = new ArrayList<>();

			for (String deviceType : deviceTypes) {
				Map<String, Object> typeNode = new HashMap<>();
				typeNode.put("id", "type_" + deviceType);
				typeNode.put("label", deviceType);
				typeNode.put("type", "device_type");

				// Query all devices under this type using the pagination API (set a large page size to retrieve all data)
				Page<DeviceInfo> page = new Page<>(1, 1000); // Set large page size to retrieve all data
				IPage<DeviceInfo> devicePage = vlsDeviceInfoService.getDevicePage(page, null, deviceType, null);
				List<DeviceInfo> devices = devicePage.getRecords();
				typeNode.put("deviceCount", devices.size());

				// Build Device Node
				List<Map<String, Object>> deviceNodes = new ArrayList<>();
				for (DeviceInfo deviceInfo : devices) {
					Map<String, Object> deviceNode = new HashMap<>();
					deviceNode.putAll(BeanUtil.beanToMap(deviceNode));
					deviceNode.put("id", "device_" + deviceInfo.getId());
					deviceNode.put("label", deviceInfo.getDeviceName());
					deviceNode.put("type", "device");
					deviceNode.put("deviceId", deviceInfo.getId());
					deviceNode.put("deviceName", deviceInfo.getDeviceName());
					deviceNode.put("status", deviceInfo.getStatus());
					deviceNode.put("streamUrl", deviceInfo.getStreamUrl());
					deviceNodes.add(deviceNode);
				}

				typeNode.put("children", deviceNodes);

				// Update label to display count
				typeNode.put("label", deviceType + " (" + devices.size() + ")");

				treeData.add(typeNode);
			}

			return R.data(treeData);
		} catch (Exception e) {
			System.err.println("Failed to get device tree:" + e.getMessage());
			return R.fail("Failed to get device tree:" + e.getMessage());
		}
	}

}
