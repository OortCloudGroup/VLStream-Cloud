/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
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
import com.ruoyi.vlstream.test.vlstream.detection.*;
import com.ruoyi.vlstream.test.vlstream.excel.VlsDeviceInfoExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.DeviceTagRelationDTO;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmModelVO;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.DeviceInfoVO;
import com.ruoyi.vlstream.test.vlstream.service.*;
import com.ruoyi.vlstream.test.vlstream.wrapper.VlsAlgorithmModelWrapper;
import com.ruoyi.vlstream.test.vlstream.wrapper.VlsDeviceInfoWrapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * deviceinfo control
 *
 * @author Oort
 * @since 2025-12-23
 */
@RestController
@AllArgsConstructor
@RequestMapping("/vlsDeviceInfo")
@Tag(name = "设备信息表", description = "设备信息表接口")
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
	private final VlsZlmService zlmService;

	/**
	 * deviceinfo
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入vlsDeviceInfo")
	public R<DeviceInfoVO> detail(DeviceInfo vlsDeviceInfo) {
		DeviceInfo detail = vlsDeviceInfoService.getOne(Condition.getQueryWrapper(vlsDeviceInfo));
		DeviceInfoVO deviceInfoVO = VlsDeviceInfoWrapper.build().entityVO(detail);
		fillAlgorithmName(deviceInfoVO);
		return R.data(deviceInfoVO);
	}

	/**
	 * deviceinfo
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入vlsDeviceInfo")
	public R<IPage<DeviceInfoVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsDeviceInfo, Query query) {
		IPage<DeviceInfo> pages = vlsDeviceInfoService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsDeviceInfo, DeviceInfo.class));
		IPage<DeviceInfoVO> pageVO = VlsDeviceInfoWrapper.build().pageVO(pages);
		fillAlgorithmName(pageVO.getRecords());
		return R.data(pageVO);
	}


	/**
	 * deviceinfo Custom
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入vlsDeviceInfo")
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
	 * deviceinfo Add
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入vlsDeviceInfo")
	public R save(@Valid @RequestBody DeviceInfo vlsDeviceInfo) {
		return R.status(vlsDeviceInfoService.save(vlsDeviceInfo));
	}

	/**
	 * deviceinfo Update
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入vlsDeviceInfo")
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
	 * deviceinfo Add Update
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入vlsDeviceInfo")
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
	 * deviceinfo Delete
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(vlsDeviceInfoService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * Export data
	 */
	@GetMapping("/export-vlsDeviceInfo")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "导出数据", description = "传入vlsDeviceInfo")
	public void exportVlsDeviceInfo(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsDeviceInfo, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<DeviceInfo> queryWrapper = Condition.getQueryWrapper(vlsDeviceInfo, DeviceInfo.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsDeviceInfoEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsDeviceInfoEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsDeviceInfoExcel> list = vlsDeviceInfoService.exportVlsDeviceInfo(queryWrapper);
		ExcelUtil.export(response, "设备信息表数据" + DateUtil.today(), "设备信息表数据表", list, VlsDeviceInfoExcel.class);
	}

	/**
	 * IDQuery deviceinfo
	 */
	@Operation(summary = "根据ID查询设备信息")
	@GetMapping("/{id}")
	public R<Map<String, Object>> getDeviceById(@PathVariable Long id) {
		DeviceInfo deviceInfo = vlsDeviceInfoService.getById(id);
		if (deviceInfo == null) {
			return R.fail("设备不存在");
		}

		Map<String, Object> result = buildDeviceInfoMap(deviceInfo);
		return R.data(result);
	}

	/**
	 * ZLMediaKit Customdevice RTSP/RTMP Convert to WebRTC.
	 */
	@PostMapping("/{id}/preview")
	@Operation(summary = "创建ZLM实时预览")
	public R<Map<String, Object>> preview(@PathVariable Long id) {
		DeviceInfo deviceInfo = vlsDeviceInfoService.getById(id);
		if (deviceInfo == null) {
			return R.fail("设备不存在");
		}
		String streamUrl = StringUtils.trimToEmpty(deviceInfo.getStreamUrl());
		if (!(StringUtils.startsWithIgnoreCase(streamUrl, "rtsp://")
			|| StringUtils.startsWithIgnoreCase(streamUrl, "rtmp://"))) {
			return R.fail("设备未配置可由ZLM代理的RTSP/RTMP视频流");
		}
		try {
			return R.data(zlmService.createProxy("custom_device_" + id, streamUrl));
		} catch (RuntimeException exception) {
			return R.fail(exception.getMessage());
		}
	}

	@Operation(summary = "获取设备训练模型")
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
	 * device Query deviceinfo
	 */
	@Operation(summary = "根据设备编号查询设备信息")
	@GetMapping("/deviceId/{deviceId}")
	public R<Map<String, Object>> getDeviceByDeviceId(@PathVariable String deviceId) {
		DeviceInfo deviceInfo = vlsDeviceInfoService.getByDeviceId(deviceId);
		if (deviceInfo == null) {
			return R.fail("设备不存在");
		}

		Map<String, Object> result = buildDeviceInfoMap(deviceInfo);
		return R.data(result);
	}

	/**
	 * Add deviceinfo
	 */
	@Operation(summary = "新增设备信息")
	@PostMapping
	@Transactional(rollbackFor = Exception.class)
	public R<String> addDevice(@RequestBody Map<String, Object> requestData) {
		DeviceInfo deviceInfo = extractDeviceInfo(requestData);

		// device whether already in
		if (vlsDeviceInfoService.checkDeviceIdExists(deviceInfo.getDeviceId())) {
			return R.fail("设备编号已存在");
		}

		boolean success = vlsDeviceInfoService.addDevice(deviceInfo);
		if (success) {
			List<Long> tagIds = extractTagIds(requestData.get("selectedTags"));
			if (!tagIds.isEmpty() && !deviceTagRelationService.setDeviceTags(deviceInfo.getId(), tagIds, currentUserId())) {
				return R.fail("设备已新增，但标签保存失败");
			}
			return R.success("新增成功");
		} else {
			return R.fail("新增失败");
		}
	}

	/**
	 * new deviceinfo
	 */
	@Operation(summary = "更新设备信息")
	@PutMapping("/{id}")
	@Transactional(rollbackFor = Exception.class)
	public R<String> updateDevice(@PathVariable Long id, @RequestBody Map<String, Object> requestData) {

		// deviceinfo
		DeviceInfo deviceInfo = extractDeviceInfo(requestData);
		deviceInfo.setId(id);

		// new deviceinfo
		boolean success = vlsDeviceInfoService.updateDevice(deviceInfo);
		if (!success) {
			return R.fail("设备信息更新失败");
		}

		// Process device
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
							System.err.println("无效的标签ID: " + tagObj);
						}
					}
				}

				if (!tagIds.isEmpty()) {
					boolean tagSuccess = deviceTagRelationService.setDeviceTags(id, tagIds, currentUserId());
					if (!tagSuccess) {
						System.err.println("设备标签保存失败，设备ID: " + id);
					}
				}
			}
		}

		return R.success("更新成功");
	}

	/**
	 * old Query parameter algorithm .
	 */
	@Operation(summary = "摄像头算法下发（兼容）")
	@GetMapping("/dispatchAlgorithms")
	public R<String> dispatchAlgorithmsLegacy(@RequestParam Long algorithmId,
		@RequestParam String deviceIds,
		@RequestParam(defaultValue = "om") String modelType) {
		return dispatchAlgorithmRequest(algorithmId, deviceIds, modelType);
	}

	/**
	 * WVPdevice , VLS-Protocol 2.2 modelDeploy notification model.
	 */
	@Operation(summary = "摄像头算法下发")
	@PostMapping("/{algorithmId}/algorithms")
	public R<String> dispatchAlgorithms(@PathVariable Long algorithmId,
		@RequestParam String deviceIds,
		@RequestParam(defaultValue = "om") String modelType) {
		return dispatchAlgorithmRequest(algorithmId, deviceIds, modelType);
	}

	/**
	 * new old interface , .
	 */
	private R<String> dispatchAlgorithmRequest(Long algorithmId, String deviceIds, String modelType) {
		boolean success = vlsDeviceInfoService.dispatchAlgorithms(algorithmId, deviceIds, modelType);
		if (success) {
			return R.success("模型下发任务已通过VLS-Protocol 2.2 MQTT总线发布");
		} else {
			return R.fail("算法下发失败，请检查模型产物、设备信息、MQTT和下载地址配置");
		}
	}

	/**
	 * from data in deviceinfo
	 */
	private DeviceInfo extractDeviceInfo(Map<String, Object> requestData) {
		DeviceInfo deviceInfo = new DeviceInfo();

		// Set field
		setIfNotNull(deviceInfo::setDeviceName, requestData.get("deviceName"));
		setIfNotNull(deviceInfo::setDeviceId, requestData.get("deviceId"));
		setIfNotNull(deviceInfo::setStreamUrl, requestData.get("streamUrl"));
		setIfNotNull(deviceInfo::setDeviceType, requestData.get("deviceType"));
		setIfNotNull(deviceInfo::setRemark, requestData.get("remark"));

		// Process Add field
		setIfNotNull(deviceInfo::setTag, requestData.get("tag"));
		setIfNotNull(deviceInfo::setImagePath, requestData.get("imagePath"));
		setIfNotNull(deviceInfo::setHeightPosition, requestData.get("heightPosition"));
		setIfNotNull(deviceInfo::setAddress, requestData.get("address"));
		setDecimalIfNotNull(deviceInfo::setLongitude, requestData.get("longitude"), "longitude");
		setDecimalIfNotNull(deviceInfo::setLatitude, requestData.get("latitude"), "latitude");

		// Process regionfield (JSON )
		if (requestData.containsKey("region")) {
			Object regionObj = requestData.get("region");
			if (regionObj != null) {
				if (regionObj instanceof List) {
					deviceInfo.setRegion(JSONUtil.toJsonStr(regionObj));
				} else if (regionObj instanceof String) {
					deviceInfo.setRegion((String) regionObj);
				}
			}
		}

		return deviceInfo;
	}

	/**
	 * method : if value to null Set
	 */
	private void setIfNotNull(java.util.function.Consumer<String> setter, Object value) {
		if (value != null) {
			setter.accept(value.toString());
		}
	}

	/**
	 * in Convert to value , in parameterinfo.
	 */
	private void setDecimalIfNotNull(java.util.function.Consumer<java.math.BigDecimal> setter, Object value, String fieldName) {
		if (value == null || StringUtils.isBlank(value.toString())) {
			return;
		}
		try {
			setter.accept(new java.math.BigDecimal(value.toString()));
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException(fieldName + " must be a valid decimal", exception);
		}
	}

	/**
	 * in to Long , method Parse value .
	 */
	private List<Long> extractTagIds(Object selectedTags) {
		if (!(selectedTags instanceof List)) {
			return Collections.emptyList();
		}

		List<Long> tagIds = new ArrayList<>();
		for (Object tag : (List<?>) selectedTags) {
			if (tag instanceof Number) {
				tagIds.add(((Number) tag).longValue());
			} else if (tag != null && StringUtils.isNumeric(tag.toString())) {
				tagIds.add(Long.valueOf(tag.toString()));
			}
		}
		return tagIds;
	}

	/**
	 * in user Convert to field need to Long.
	 */
	private Long currentUserId() {
		String userId = getUserId();
		if (StringUtils.isBlank(userId)) {
			throw new IllegalStateException("无法获取当前登录用户ID");
		}
		try {
			return Long.valueOf(userId);
		} catch (NumberFormatException exception) {
			throw new IllegalStateException("当前登录用户ID不是有效数字: " + userId, exception);
		}
	}

	/**
	 * Build deviceinfo Map, info
	 */
	private Map<String, Object> buildDeviceInfoMap(DeviceInfo deviceInfo) {
		Map<String, Object> result = new HashMap<>();

		// device info
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

		// Query info
		try {
			List<DeviceTagRelationDTO> tagRelations = deviceTagRelationService.getDeviceTags(deviceInfo.getId());
			List<Long> selectedTags = new ArrayList<>();
			for (DeviceTagRelation relation : tagRelations) {
				selectedTags.add(relation.getTagId());
			}
			result.put("selectedTags", selectedTags);
		} catch (Exception e) {
			System.err.println("查询设备标签失败: " + e.getMessage());
			result.put("selectedTags", new ArrayList<>());
		}

		return result;
	}

	/**
	 * Delete deviceinfo
	 */
	@Operation(description = "删除设备信息")
	@DeleteMapping("/{id}")
	public R<String> deleteDevice(@PathVariable Long id) {
		boolean success = vlsDeviceInfoService.deleteDevice(id);
		if (success) {
			return R.success("删除成功");
		} else {
			return R.fail("删除失败");
		}
	}

	/**
	 * Batch delete deviceinfo
	 */
	@Operation(description = "批量删除设备信息")
	@DeleteMapping("/batch")
	public R<String> deleteDeviceBatch(@RequestBody List<Long> ids) {
		if (ids == null || ids.isEmpty()) {
			return R.fail("请选择要删除的设备");
		}

		boolean success = vlsDeviceInfoService.deleteDeviceBatch(ids);
		if (success) {
			return R.success("批量删除成功");
		} else {
			return R.fail("批量删除失败");
		}
	}

	/**
	 * new device
	 */
	@Operation(description = "更新设备状态")
	@PutMapping("/{id}/status/{status}")
	public R<String> updateDeviceStatus(@PathVariable Long id, @PathVariable Integer status) {

		boolean success = vlsDeviceInfoService.updateDeviceStatus(id, status);
		if (success) {
			return R.success("状态更新成功");
		} else {
			return R.fail("状态更新失败");
		}
	}

	/**
	 * new device
	 */
	@Operation(description = "批量更新设备状态")
	@PutMapping("/status/{status}")
	public R<String> updateDeviceStatusBatch(@PathVariable String status, @RequestBody List<Long> ids) {

		if (ids == null || ids.isEmpty()) {
			return R.fail("请选择要更新的设备");
		}

		boolean success = vlsDeviceInfoService.updateDeviceStatusBatch(ids, status);
		if (success) {
			return R.success("批量状态更新成功");
		} else {
			return R.fail("批量状态更新失败");
		}
	}

	/**
	 * Query device list
	 */
	@Operation(description = "根据状态查询设备列表")
	@GetMapping("/status/{status}")
	public R<List<DeviceInfo>> getDevicesByStatus(@PathVariable String status) {
		List<DeviceInfo> devices = vlsDeviceInfoService.getDevicesByStatus(status);
		return R.data(devices);
	}

	/**
	 * device Query device list
	 */
	@Operation(description = "根据设备类型查询设备列表")
	@GetMapping("/type/{deviceType}")
	public R<List<DeviceInfo>> getDevicesByType(@PathVariable String deviceType) {
		List<DeviceInfo> devices = vlsDeviceInfoService.getDevicesByType(deviceType);
		return R.data(devices);
	}

	/**
	 * device
	 */
	@Operation(description = "测试设备连接")
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
	 * Get device info
	 */
	@Operation(description = "获取设备统计信息")
	@GetMapping("/statistics")
	public R<Map<String, Object>> getDeviceStatistics() {
		Map<String, Object> statistics = vlsDeviceInfoService.getDeviceStatistics();
		return R.data(statistics);
	}

	/**
	 * Get devicegroup ( group)
	 */
	@Operation(description = "获取设备分组统计")
	@GetMapping("/group-statistics")
	public R<List<Map<String, Object>>> getDeviceGroupStatistics() {
		// Get all device
		List<DeviceInfo> allDevices = vlsDeviceInfoService.list();

		// device group
		Map<String, List<DeviceInfo>> devicesByType = new HashMap<>();
		for (DeviceInfo deviceInfo : allDevices) {
			String type = deviceInfo.getDeviceType();
			if (type == null || type.trim().isEmpty()) {
				type = "未分类";
			}
			devicesByType.computeIfAbsent(type, k -> new ArrayList<>()).add(deviceInfo);
		}

		// Build
		List<Map<String, Object>> result = new ArrayList<>();
		for (Map.Entry<String, List<DeviceInfo>> entry : devicesByType.entrySet()) {
			String typeName = entry.getKey();
			List<DeviceInfo> devices = entry.getValue();

			Map<String, Object> groupStat = new HashMap<>();
			groupStat.put("type", typeName);
			groupStat.put("total", devices.size());

			//
			long online = devices.stream().filter(d -> "在线".equals(d.getStatus())).count();
			long offline = devices.stream().filter(d -> "离线".equals(d.getStatus())).count();
			long fault = devices.stream().filter(d -> "故障".equals(d.getStatus())).count();

			groupStat.put("online", online);
			groupStat.put("offline", offline);
			groupStat.put("fault", fault);

			result.add(groupStat);
		}

		return R.data(result);
	}

	/**
	 * Get device
	 */
	@Operation(description = "获取设备类型统计")
	@GetMapping("/type-statistics")
	public R<Map<String, Object>> getDeviceTypeStatistics() {
		// Get all device
		List<String> allTypes = vlsDeviceInfoService.getAllTags();
		Map<String, Object> statistics = new HashMap<>();

		for (String type : allTypes) {
			List<DeviceInfo> devices = vlsDeviceInfoService.getDevicesByType(type);
			statistics.put(type, devices.size());
		}

		return R.data(statistics);
	}

	/**
	 * Get all device ( )
	 */
	@Operation(description = "获取所有设备类型列表")
	@GetMapping("/tags")
	public R<List<String>> getDeviceTags() {
		List<String> tags = vlsDeviceInfoService.getAllTags();
		return R.data(tags);
	}

	/**
	 * Get all device
	 */
	@Operation(description = "获取所有设备品牌列表")
	@GetMapping("/brands")
	public R<List<String>> getDeviceBrands() {
		List<String> brands = vlsDeviceInfoService.getAllBrands();
		return R.data(brands);
	}

	/**
	 * new device
	 */
	@Operation(description = "刷新设备状态")
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
	 * new device
	 */
	@Operation(description = "批量刷新设备状态")
	@PostMapping("/batch/refresh")
	public R<String> batchRefreshDevices(@RequestBody Map<String, List<Long>> request) {
		List<Long> ids = request.get("ids");
		if (ids == null || ids.isEmpty()) {
			return R.fail("请选择要刷新的设备");
		}

		int successCount = 0;
		for (Long id : ids) {
			Map<String, Object> result = vlsDeviceInfoService.refreshDeviceStatus(id);
			if ((Boolean) result.get("success")) {
				successCount++;
			}
		}

		return R.success("批量刷新完成，成功 " + successCount + " 台设备");
	}

	/**
	 * PTZcontrol -
	 */
	@Operation(description = "PTZ控制 - 移动")
	@PostMapping("/{id}/ptz/move")
	public R<String> ptzMove(@PathVariable Long id, @RequestBody Map<String, Object> params) {

		String direction = (String) params.get("direction");
		Integer speed = (Integer) params.getOrDefault("speed", 4);

		// devicewhether in
		DeviceInfo device = vlsDeviceInfoService.getById(id);
		if (device == null) {
			return R.fail("设备不存在");
		}

		Map<String, Object> result = vlsDeviceInfoService.ptzControl(id, "move", params);
		if ((Boolean) result.get("success")) {
			return R.success("PTZ移动成功: " + direction + ", 速度: " + speed);
		} else {
			return R.fail((String) result.get("message"));
		}
	}

	/**
	 * PTZcontrol -
	 */
	@Operation(description = "PTZ控制 - 停止")
	@PostMapping("/{id}/ptz/stop")
	public R<String> ptzStop(@PathVariable Long id) {
		// devicewhether in
		DeviceInfo device = vlsDeviceInfoService.getById(id);
		if (device == null) {
			return R.fail("设备不存在");
		}

		Map<String, Object> result = vlsDeviceInfoService.ptzControl(id, "stop", new HashMap<>());
		if ((Boolean) result.get("success")) {
			return R.success("PTZ停止成功");
		} else {
			return R.fail((String) result.get("message"));
		}
	}

	/**
	 * PTZcontrol -
	 */
	@Operation(description = "PTZ控制 - 缩放")
	@PostMapping("/{id}/ptz/zoom")
	public R<String> ptzZoom(@PathVariable Long id,
		@RequestBody Map<String, Object> params) {

		String action = (String) params.get("action");
		Integer speed = (Integer) params.getOrDefault("speed", 4);

		// devicewhether in
		DeviceInfo device = vlsDeviceInfoService.getById(id);
		if (device == null) {
			return R.fail("设备不存在");
		}

		Map<String, Object> result = vlsDeviceInfoService.ptzControl(id, "zoom", params);
		if ((Boolean) result.get("success")) {
			return R.success("PTZ缩放成功: " + action + ", 速度: " + speed);
		} else {
			return R.fail((String) result.get("message"));
		}
	}

	/**
	 * Get device info
	 */
	@Operation(description = "获取设备视频流信息")
	@GetMapping("/{id}/stream")
	public R<Map<String, Object>> getDeviceStreamInfo(@PathVariable Long id) {
		Map<String, Object> streamInfo = vlsDeviceInfoService.getVideoStreamInfo(id);
		if (streamInfo.isEmpty()) {
			return R.fail("设备不存在");
		}
		return R.data(streamInfo);
	}

	/**
	 * Export device
	 */
	@Operation(description = "导出设备列表")
	@GetMapping("/export")
	public R<List<DeviceInfo>> exportDevices(@RequestParam(required = false) List<Long> deviceIds) {
		List<DeviceInfo> devices = vlsDeviceInfoService.exportDevices(deviceIds);
		return R.data(devices);
	}

	/**
	 * Import device
	 */
	@Operation(description = "导入设备列表")
	@PostMapping("/import")
	public R<Map<String, Object>> importDevices(@RequestParam("file") MultipartFile file) {
		// TODO: Parse and deviceImport can
		Map<String, Object> result = new HashMap<>();
		result.put("message", "导入功能待实现");
		return R.data(result);
	}

	/**
	 * Get deviceconfiguration
	 */
	@Operation(description = "获取设备配置")
	@GetMapping("/{id}/config")
	public R<Map<String, Object>> getDeviceConfig(@PathVariable Long id) {
		Map<String, Object> config = vlsDeviceInfoService.getDeviceConfig(id);
		if (config.isEmpty()) {
			return R.fail("设备不存在");
		}
		return R.data(config);
	}

	/**
	 * new deviceconfiguration
	 */
	@Operation(description = "更新设备配置")
	@PutMapping("/{id}/config")
	public R<String> updateDeviceConfig( @PathVariable Long id,
		@RequestBody Map<String, Object> config) {

		boolean success = vlsDeviceInfoService.updateDeviceConfig(id, config);
		if (success) {
			return R.success("配置更新成功");
		} else {
			return R.fail("配置更新失败");
		}
	}

	// ==================== device relatedinterface ====================

	/**
	 * Set device
	 */
	@Operation(description = "设置设备标签")
	@PutMapping("/{id}/tags")
	public R<String> setDeviceTags(@PathVariable Long id, @RequestBody List<Long> tagIds) {

		// devicewhether in
		DeviceInfo device = vlsDeviceInfoService.getById(id);
		if (device == null) {
			return R.fail("设备不存在");
		}

		boolean success = deviceTagRelationService.setDeviceTags(id, tagIds, currentUserId());
		if (success) {
			return R.success("设备标签设置成功");
		} else {
			return R.fail("设备标签设置失败");
		}
	}

	/**
	 * Get device
	 */
	@Operation(description = "获取设备标签")
	@GetMapping("/{id}/tags")
	public R<List<DeviceTagRelationDTO>> getDeviceTags(@PathVariable Long id) {

		// devicewhether in
		DeviceInfo device = vlsDeviceInfoService.getById(id);
		if (device == null) {
			return R.fail("设备不存在");
		}

		List<DeviceTagRelationDTO> tags = deviceTagRelationService.getDeviceTags(id);
		return R.data(tags);
	}

	/**
	 * device
	 */
	@Operation(description = "添加设备标签")
	@PostMapping("/{id}/tags")
	public R<String> addDeviceTags(@PathVariable Long id, @RequestBody List<Long> tagIds) {

		// devicewhether in
		DeviceInfo device = vlsDeviceInfoService.getById(id);
		if (device == null) {
			return R.fail("设备不存在");
		}

		boolean success = deviceTagRelationService.addDeviceTags(id, tagIds, currentUserId());
		if (success) {
			return R.success("设备标签添加成功");
		} else {
			return R.fail("设备标签添加失败");
		}
	}

	/**
	 * device
	 */
	@Operation(description = "移除设备标签")
	@DeleteMapping("/{id}/tags")
	public R<String> removeDeviceTags(@PathVariable Long id, @RequestBody List<Long> tagIds) {

		// devicewhether in
		DeviceInfo device = vlsDeviceInfoService.getById(id);
		if (device == null) {
			return R.fail("设备不存在");
		}

		boolean success = deviceTagRelationService.removeDeviceTags(id, tagIds);
		if (success) {
			return R.success("设备标签移除成功");
		} else {
			return R.fail("设备标签移除失败");
		}
	}

	/**
	 * device all
	 */
	@Operation(description = "清除设备的所有标签")
	@DeleteMapping("/{id}/tags/all")
	public R<String> clearDeviceTags(@PathVariable Long id) {

		// devicewhether in
		DeviceInfo device = vlsDeviceInfoService.getById(id);
		if (device == null) {
			return R.fail("设备不存在");
		}

		boolean success = deviceTagRelationService.clearDeviceTags(id);
		if (success) {
			return R.success("设备标签清除成功");
		} else {
			return R.fail("设备标签清除失败");
		}
	}

	/**
	 * Get device info
	 */
	@Operation(description = "获取设备标签详细信息")
	@GetMapping("/{id}/tag-details")
	public R<Map<String, Object>> getDeviceTagDetails(@PathVariable Long id) {

		// devicewhether in
		DeviceInfo device = vlsDeviceInfoService.getById(id);
		if (device == null) {
			return R.fail("设备不存在");
		}

		Map<String, Object> details = deviceTagRelationService.getDeviceTagDetails(id);
		return R.data(details);
	}

	/**
	 * device
	 */
	@Operation(description = "复制设备标签到其他设备")
	@PostMapping("/{sourceId}/copy-tags")
	public R<String> copyDeviceTags(@PathVariable Long sourceId, @RequestBody List<Long> targetDeviceIds) {

		// devicewhether in
		DeviceInfo sourceDevice = vlsDeviceInfoService.getById(sourceId);
		if (sourceDevice == null) {
			return R.fail("源设备不存在");
		}

		// devicewhether in
		for (Long targetId : targetDeviceIds) {
			DeviceInfo targetDevice = vlsDeviceInfoService.getById(targetId);
			if (targetDevice == null) {
				return R.fail("目标设备不存在: " + targetId);
			}
		}

		boolean success = deviceTagRelationService.copyDeviceTags(sourceId, targetDeviceIds, currentUserId());
		if (success) {
			return R.success("复制设备标签成功");
		} else {
			return R.fail("复制设备标签失败");
		}
	}

	/**
	 * Get device
	 */
	@Operation(description = "获取设备树形结构")
	@GetMapping("/tree")
	public R<List<Map<String, Object>>> getDeviceTree() {
		try {
			// device
			String[] deviceTypes = {"球机", "云台", "摄像头", "枪机", "半球"};

			List<Map<String, Object>> treeData = new ArrayList<>();

			for (String deviceType : deviceTypes) {
				Map<String, Object> typeNode = new HashMap<>();
				typeNode.put("id", "type_" + deviceType);
				typeNode.put("label", deviceType);
				typeNode.put("type", "device_type");

				// APIQuery all device (Set pageGet full data)
				Page<DeviceInfo> page = new Page<>(1, 1000); // Set pageGet full data
				IPage<DeviceInfo> devicePage = vlsDeviceInfoService.getDevicePage(page, null, deviceType, null);
				List<DeviceInfo> devices = devicePage.getRecords();
				typeNode.put("deviceCount", devices.size());

				// Build devicenode
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

				// new label
				typeNode.put("label", deviceType + " (" + devices.size() + ")");

				treeData.add(typeNode);
			}

			return R.data(treeData);
		} catch (Exception e) {
			System.err.println("获取设备树失败: " + e.getMessage());
			return R.fail("获取设备树失败: " + e.getMessage());
		}
	}

}
