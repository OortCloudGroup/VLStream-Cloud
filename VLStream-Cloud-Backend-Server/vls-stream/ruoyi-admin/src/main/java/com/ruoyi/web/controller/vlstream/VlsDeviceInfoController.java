/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.AlgorithmModel;
import com.ruoyi.vlstream.domain.AlgorithmTraining;
import com.ruoyi.vlstream.domain.DeviceInfo;
import com.ruoyi.vlstream.mapper.VlsAlgorithmModelMapper;
import com.ruoyi.vlstream.mapper.VlsAlgorithmTrainingMapper;
import com.ruoyi.vlstream.mapper.VlsDeviceInfoMapper;
import com.ruoyi.vlstream.service.IVlsDeviceInfoService;
import com.ruoyi.vlstream.service.IVlsDeviceTagRelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;

/**
 * SpringBlade-compatible device routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor(onConstructor_ = @org.springframework.beans.factory.annotation.Autowired)
@RequestMapping("/vlsDeviceInfo")
public class VlsDeviceInfoController extends VlsControllerSupport {

    private final IVlsDeviceInfoService deviceInfoService;
    private final VlsDeviceInfoMapper deviceInfoMapper;
    private final IVlsDeviceTagRelationService deviceTagRelationService;
    private final VlsAlgorithmTrainingMapper trainingMapper;
    private final VlsAlgorithmModelMapper modelMapper;

    /** Preserve the controller's original unit-test construction surface. */
    public VlsDeviceInfoController(IVlsDeviceInfoService deviceInfoService) {
        this.deviceInfoService = deviceInfoService;
        this.deviceInfoMapper = null;
        this.deviceTagRelationService = null;
        this.trainingMapper = null;
        this.modelMapper = null;
    }

    @GetMapping("/page")
    public BladeResult<BladePage<DeviceInfo>> page(@RequestParam(value = "current", required = false) Long current,
                                                   @RequestParam(value = "page", required = false) Long page,
                                                   @RequestParam(value = "size", defaultValue = "10") Long size,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String deviceName,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String tag) {
        long resolvedCurrent = current == null ? (page == null ? 1L : page) : current;
        long resolvedSize = size == null ? 10L : size;
        return BladeResult.success(deviceInfoService.getDevicePage(
            resolvedCurrent, resolvedSize, keyword, deviceName, status, tag));
    }

    @GetMapping("/{id}")
    public BladeResult<DeviceInfo> getDevice(@PathVariable Long id) {
        return BladeResult.success(deviceInfoService.getDevice(id));
    }

    @PostMapping
    public BladeResult<DeviceInfo> createDevice(@RequestBody DeviceInfo deviceInfo) {
        try {
            return BladeResult.success(deviceInfoService.createDevice(deviceInfo));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    public BladeResult<DeviceInfo> updateDevice(@PathVariable Long id, @RequestBody DeviceInfo deviceInfo) {
        try {
            return BladeResult.success(deviceInfoService.updateDevice(id, deviceInfo));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public BladeResult<Void> deleteDevice(@PathVariable Long id) {
        return booleanResult(deviceInfoService.deleteDevice(id), "删除设备失败");
    }

    @DeleteMapping("/batch")
    public BladeResult<Void> deleteDevices(@RequestBody List<Long> ids) {
        return booleanResult(deviceInfoService.deleteDevices(ids), "批量删除设备失败");
    }

    @GetMapping("/statistics")
    public BladeResult<Map<String, Object>> statistics() {
        return BladeResult.success(deviceInfoService.getDeviceStatistics());
    }

    @GetMapping("/tree")
    public BladeResult<List<Map<String, Object>>> tree() {
        return BladeResult.success(deviceInfoService.getDeviceTree());
    }

    @PostMapping("/{id}/test")
    public BladeResult<Map<String, Object>> testConnection(@PathVariable Long id) {
        return operationResult(deviceInfoService.testDeviceConnection(id), "设备连接测试失败");
    }

    @PostMapping("/{id}/refresh")
    public BladeResult<Map<String, Object>> refresh(@PathVariable Long id) {
        return operationResult(deviceInfoService.refreshDeviceStatus(id), "设备状态刷新失败");
    }

    @PostMapping("/batch/refresh")
    public BladeResult<Map<String, Object>> batchRefresh(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = deviceInfoService.batchRefreshDevices(toLongList(body == null ? null : body.get("ids")));
        return Boolean.TRUE.equals(result.get("operationSuccess")) ? BladeResult.success(result)
            : BladeResult.<Map<String, Object>>fail(String.valueOf(result.get("message")));
    }

    @GetMapping("/type-statistics")
    public BladeResult<List<VlsDeviceInfoMapper.TypeStatistics>> typeStatistics() {
        return BladeResult.success(deviceInfoService.getTypeStatistics());
    }

    @GetMapping("/tags")
    public BladeResult<List<String>> tags() {
        return BladeResult.success(deviceInfoService.getAllTags());
    }

    @PostMapping("/{id}/ptz/move")
    public BladeResult<Map<String, Object>> ptzMove(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return operationResult(deviceInfoService.ptzControl(id, "move", body), "PTZ 移动失败");
    }

    @PostMapping("/{id}/ptz/stop")
    public BladeResult<Map<String, Object>> ptzStop(@PathVariable Long id) {
        return operationResult(deviceInfoService.ptzControl(id, "stop", Collections.<String, Object>emptyMap()), "PTZ 停止失败");
    }

    @PostMapping("/{id}/ptz/zoom")
    public BladeResult<Map<String, Object>> ptzZoom(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return operationResult(deviceInfoService.ptzControl(id, "zoom", body), "PTZ 变焦失败");
    }

    @GetMapping("/{id}/stream")
    public BladeResult<Map<String, Object>> stream(@PathVariable Long id) {
        Map<String, Object> result = deviceInfoService.getVideoStreamInfo(id);
        return result == null || result.isEmpty() ? BladeResult.<Map<String, Object>>fail("设备不存在或未配置视频流")
            : BladeResult.success(result);
    }

    @GetMapping("/export")
    public BladeResult<List<DeviceInfo>> export(@RequestParam(required = false) String deviceIds) {
        return BladeResult.success(deviceInfoService.exportDevices(deviceIds));
    }

    @PostMapping("/import")
    public BladeResult<Map<String, Object>> importDevices(@RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            return BladeResult.success(deviceInfoService.importDevices(file));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/{algorithmId}/algorithms")
    public BladeResult<Map<String, Object>> dispatchAlgorithms(@PathVariable Long algorithmId,
                                                              @RequestParam String deviceIds) {
        return dispatchAlgorithmResult(algorithmId, deviceIds);
    }

    /**
     * Source VLS compatible route: GET /vlsDeviceInfo/dispatchAlgorithms.
     */
    @GetMapping("/dispatchAlgorithms")
    public BladeResult<Map<String, Object>> dispatchAlgorithmsCompat(@RequestParam Long algorithmId,
                                                                     @RequestParam String deviceIds) {
        return dispatchAlgorithmResult(algorithmId, deviceIds);
    }

    /** Return one real device through the SpringBlade detail route. */
    @GetMapping("/detail")
    public BladeResult<DeviceInfo> detail(@RequestParam Long id) {
        return deviceResult(deviceInfoService.getDevice(id));
    }

    /** Return the real device page through the SpringBlade list route. */
    @GetMapping("/list")
    public BladeResult<BladePage<DeviceInfo>> list(@RequestParam(required = false) Long current,
                                                   @RequestParam(required = false) Long size,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String deviceName,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String tag) {
        return page(current, null, size, keyword, deviceName, status, tag);
    }

    /** Create a device through the SpringBlade save route. */
    @PostMapping("/save")
    public BladeResult<DeviceInfo> save(@RequestBody DeviceInfo device) {
        return createDevice(device);
    }

    /** Update a device through the SpringBlade update route. */
    @PostMapping("/update")
    public BladeResult<DeviceInfo> update(@RequestBody DeviceInfo device) {
        return device == null || device.getId() == null ? BladeResult.<DeviceInfo>fail("Device ID is required")
            : updateDevice(device.getId(), device);
    }

    /** Insert or update a device through the SpringBlade submit route. */
    @PostMapping("/submit")
    public BladeResult<DeviceInfo> submit(@RequestBody DeviceInfo device) {
        return device != null && device.getId() != null ? update(device) : createDevice(device);
    }

    /** Delete real device rows through the SpringBlade remove route. */
    @GetMapping("/remove")
    public BladeResult<Void> remove(@RequestParam String ids) {
        try {
            List<Long> parsed = parseIds(ids);
            return parsed.isEmpty() ? BladeResult.fail("ids is required")
                : booleanResult(deviceInfoService.deleteDevices(parsed), "Delete devices failed");
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Query a device by its business device number. */
    @GetMapping("/deviceId/{deviceId}")
    public BladeResult<DeviceInfo> getByDeviceId(@PathVariable String deviceId) {
        return deviceResult(deviceInfoMapper.selectByDeviceId(deviceId));
    }

    /** Query actual devices by status. */
    @GetMapping("/status/{status}")
    public BladeResult<List<DeviceInfo>> devicesByStatus(@PathVariable String status) {
        try {
            return BladeResult.success(deviceInfoMapper.selectList(new LambdaQueryWrapper<DeviceInfo>()
                .eq(DeviceInfo::getStatus, parseStatus(status)).orderByAsc(DeviceInfo::getDeviceName)));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Query actual devices by device type. */
    @GetMapping("/type/{deviceType}")
    public BladeResult<List<DeviceInfo>> devicesByType(@PathVariable String deviceType) {
        return BladeResult.success(deviceInfoMapper.selectList(new LambdaQueryWrapper<DeviceInfo>()
            .eq(DeviceInfo::getDeviceType, deviceType).orderByAsc(DeviceInfo::getDeviceName)));
    }

    /** Persist one device status transition. */
    @PutMapping("/{id}/status/{status}")
    public BladeResult<DeviceInfo> updateStatus(@PathVariable Long id, @PathVariable String status) {
        DeviceInfo device = deviceInfoService.getDevice(id);
        if (device == null) {
            return BladeResult.fail("Device does not exist");
        }
        try {
            device.setStatus(parseStatus(status));
            return BladeResult.success(deviceInfoService.updateDevice(id, device));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Persist one status on every requested device. */
    @PutMapping("/status/{status}")
    public BladeResult<Boolean> updateStatusBatch(@PathVariable String status, @RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return BladeResult.fail("Device IDs are required");
        }
        try {
            Integer value = parseStatus(status);
            for (Long id : ids) {
                DeviceInfo device = deviceInfoService.getDevice(id);
                if (device == null) {
                    return BladeResult.fail("Device does not exist: " + id);
                }
                device.setStatus(value);
                deviceInfoService.updateDevice(id, device);
            }
            return BladeResult.success(true);
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Return statistics grouped from actual device rows. */
    @GetMapping("/group-statistics")
    public BladeResult<List<Map<String, Object>>> groupStatistics() {
        Map<String, List<DeviceInfo>> groups = new LinkedHashMap<String, List<DeviceInfo>>();
        for (DeviceInfo device : deviceInfoMapper.selectList(new LambdaQueryWrapper<DeviceInfo>())) {
            String type = device.getDeviceType() == null || device.getDeviceType().trim().isEmpty()
                ? "Uncategorized" : device.getDeviceType();
            List<DeviceInfo> rows = groups.get(type);
            if (rows == null) {
                rows = new ArrayList<DeviceInfo>();
                groups.put(type, rows);
            }
            rows.add(device);
        }
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, List<DeviceInfo>> entry : groups.entrySet()) {
            long online = 0L;
            long offline = 0L;
            long fault = 0L;
            for (DeviceInfo device : entry.getValue()) {
                online += Integer.valueOf(0).equals(device.getStatus()) ? 1L : 0L;
                offline += Integer.valueOf(1).equals(device.getStatus()) ? 1L : 0L;
                fault += Integer.valueOf(2).equals(device.getStatus()) ? 1L : 0L;
            }
            Map<String, Object> row = new LinkedHashMap<String, Object>();
            row.put("type", entry.getKey());
            row.put("total", entry.getValue().size());
            row.put("online", online);
            row.put("offline", offline);
            row.put("fault", fault);
            result.add(row);
        }
        return BladeResult.success(result);
    }

    /** Report the unsupported source brand field explicitly instead of fabricating data. */
    @GetMapping("/brands")
    public BladeResult<List<String>> brands() {
        return BladeResult.fail("The main-source vls_device_info schema has no brand column; brand data cannot be queried");
    }

    /** Resolve the latest model through the device algorithm, latest training and model rows. */
    @GetMapping("/latest-training-model")
    public BladeResult<AlgorithmModel> latestTrainingModel(@RequestParam String deviceId) {
        DeviceInfo device = deviceInfoMapper.selectByDeviceId(deviceId);
        if (device == null) {
            return BladeResult.fail("Device not found");
        }
        final Long algorithmId;
        try {
            algorithmId = Long.valueOf(device.getAlgorithmId());
        } catch (RuntimeException ex) {
            return BladeResult.fail("Device algorithm id is missing or invalid");
        }
        AlgorithmTraining training = trainingMapper.selectOne(new LambdaQueryWrapper<AlgorithmTraining>()
            .eq(AlgorithmTraining::getAlgorithmId, algorithmId)
            .orderByDesc(AlgorithmTraining::getUpdateTime).last("limit 1"));
        if (training == null) {
            return BladeResult.fail("Training task not found");
        }
        AlgorithmModel model = modelMapper.selectOne(new LambdaQueryWrapper<AlgorithmModel>()
            .eq(AlgorithmModel::getTrainingId, training.getId())
            .orderByDesc(AlgorithmModel::getCreateTime).last("limit 1"));
        return model == null ? BladeResult.<AlgorithmModel>fail("Model not found") : BladeResult.success(model);
    }

    /** Return persisted device configuration fields. */
    @GetMapping("/{id}/config")
    public BladeResult<Map<String, Object>> getConfig(@PathVariable Long id) {
        DeviceInfo device = deviceInfoService.getDevice(id);
        if (device == null) {
            return BladeResult.fail("Device does not exist");
        }
        Map<String, Object> config = new LinkedHashMap<String, Object>();
        config.put("deviceId", device.getDeviceId());
        config.put("deviceName", device.getDeviceName());
        config.put("streamUrl", device.getStreamUrl());
        config.put("deviceType", device.getDeviceType());
        return BladeResult.success(config);
    }

    /** Persist supported device configuration fields. */
    @PutMapping("/{id}/config")
    public BladeResult<DeviceInfo> updateConfig(@PathVariable Long id, @RequestBody Map<String, Object> config) {
        DeviceInfo device = deviceInfoService.getDevice(id);
        if (device == null) {
            return BladeResult.fail("Device does not exist");
        }
        if (config != null && config.containsKey("deviceName")) {
            device.setDeviceName(String.valueOf(config.get("deviceName")));
        }
        if (config != null && config.containsKey("streamUrl")) {
            device.setStreamUrl(String.valueOf(config.get("streamUrl")));
        }
        if (config != null && config.containsKey("deviceType")) {
            device.setDeviceType(String.valueOf(config.get("deviceType")));
        }
        try {
            return BladeResult.success(deviceInfoService.updateDevice(id, device));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Replace persisted tag relations for one device. */
    @PutMapping("/{id}/tags")
    public BladeResult<Boolean> setTags(@PathVariable Long id, @RequestBody List<Long> tagIds) {
        return mutateTags(id, tagIds, "set");
    }

    /** Return persisted tag relations enriched from the tag table. */
    @GetMapping("/{id}/tags")
    public BladeResult<List<Map<String, Object>>> getTags(@PathVariable Long id) {
        return deviceInfoService.getDevice(id) == null ? BladeResult.<List<Map<String, Object>>>fail("Device does not exist")
            : BladeResult.success(deviceTagRelationService.getDeviceTags(id));
    }

    /** Add persisted device/tag relations. */
    @PostMapping("/{id}/tags")
    public BladeResult<Boolean> addTags(@PathVariable Long id, @RequestBody List<Long> tagIds) {
        return mutateTags(id, tagIds, "add");
    }

    /** Remove selected persisted device/tag relations. */
    @DeleteMapping("/{id}/tags")
    public BladeResult<Boolean> removeTags(@PathVariable Long id, @RequestBody List<Long> tagIds) {
        return mutateTags(id, tagIds, "remove");
    }

    /** Clear every persisted tag relation for a device. */
    @DeleteMapping("/{id}/tags/all")
    public BladeResult<Boolean> clearTags(@PathVariable Long id) {
        return mutateTags(id, Collections.<Long>emptyList(), "clear");
    }

    /** Return real tag details for a device. */
    @GetMapping("/{id}/tag-details")
    public BladeResult<Map<String, Object>> tagDetails(@PathVariable Long id) {
        return deviceInfoService.getDevice(id) == null ? BladeResult.<Map<String, Object>>fail("Device does not exist")
            : BladeResult.success(deviceTagRelationService.getDeviceTagDetails(id));
    }

    /** Copy the source device's persisted relations to existing targets. */
    @PostMapping("/{sourceId}/copy-tags")
    public BladeResult<Boolean> copyTags(@PathVariable Long sourceId, @RequestBody List<Long> targetIds) {
        if (deviceInfoService.getDevice(sourceId) == null) {
            return BladeResult.fail("Source device does not exist");
        }
        if (targetIds == null || targetIds.isEmpty()) {
            return BladeResult.fail("Target device IDs are required");
        }
        for (Long targetId : targetIds) {
            if (deviceInfoService.getDevice(targetId) == null) {
                return BladeResult.fail("Target device does not exist: " + targetId);
            }
        }
        try {
            return BladeResult.success(deviceTagRelationService.copyDeviceTags(sourceId, targetIds));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Export actual device rows through the source-compatible Excel route. */
    @GetMapping("/export-vlsDeviceInfo")
    public void exportVlsDeviceInfo(@RequestParam(required = false) String deviceIds,
                                    HttpServletResponse response) {
        ExcelUtil.exportExcel(deviceInfoService.exportDevices(deviceIds), "VLS Devices", DeviceInfo.class, response);
    }

    /** Apply one real relation mutation after verifying the device. */
    private BladeResult<Boolean> mutateTags(Long deviceId, List<Long> tagIds, String action) {
        if (deviceInfoService.getDevice(deviceId) == null) {
            return BladeResult.fail("Device does not exist");
        }
        try {
            boolean result;
            if ("set".equals(action)) {
                result = deviceTagRelationService.setDeviceTags(deviceId, tagIds);
            } else if ("add".equals(action)) {
                result = deviceTagRelationService.addDeviceTags(deviceId, tagIds);
            } else if ("remove".equals(action)) {
                result = deviceTagRelationService.removeDeviceTags(deviceId, tagIds);
            } else {
                result = deviceTagRelationService.clearDeviceTags(deviceId);
            }
            return result ? BladeResult.success(true) : BladeResult.<Boolean>fail("Device tag operation changed no rows");
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Parse source textual or numeric status values into the persisted integer code. */
    private Integer parseStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("status is required");
        }
        String value = status.trim();
        if ("online".equalsIgnoreCase(value)) {
            return 0;
        }
        if ("offline".equalsIgnoreCase(value)) {
            return 1;
        }
        if ("fault".equalsIgnoreCase(value)) {
            return 2;
        }
        int parsed = Integer.parseInt(value);
        if (parsed < 0 || parsed > 2) {
            throw new IllegalArgumentException("status must be online/offline/fault or 0/1/2");
        }
        return parsed;
    }

    /** Convert a nullable device row into an explicit API result. */
    private BladeResult<DeviceInfo> deviceResult(DeviceInfo device) {
        return device == null ? BladeResult.<DeviceInfo>fail("Device does not exist") : BladeResult.success(device);
    }

    private BladeResult<Map<String, Object>> dispatchAlgorithmResult(Long algorithmId, String deviceIds) {
        Map<String, Object> result = deviceInfoService.dispatchAlgorithms(algorithmId, deviceIds);
        if (result == null || Boolean.FALSE.equals(result.get("success"))) {
            String message = result == null || result.get("message") == null
                ? "Algorithm dispatch failed"
                : String.valueOf(result.get("message"));
            return BladeResult.fail(message);
        }
        return BladeResult.success(result);
    }

    /** Convert an operation payload into a failure response when the service reports failure. */
    private BladeResult<Map<String, Object>> operationResult(Map<String, Object> result, String fallbackMessage) {
        if (result == null || !Boolean.TRUE.equals(result.get("success"))) {
            String message = result == null || result.get("message") == null
                ? fallbackMessage : String.valueOf(result.get("message"));
            return BladeResult.fail(message);
        }
        return BladeResult.success(result);
    }

    private BladeResult<Void> booleanResult(boolean success, String message) {
        return success ? BladeResult.success() : BladeResult.fail(message);
    }

    private List<Long> toLongList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        List<Long> result = new ArrayList<Long>();
        if (value instanceof Iterable) {
            for (Object item : (Iterable<?>) value) {
                Long converted = toLong(item);
                if (converted != null) {
                    result.add(converted);
                }
            }
            return result;
        }
        Long converted = toLong(value);
        if (converted != null) {
            result.add(converted);
        }
        return result;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : Long.valueOf(text);
    }
}
