package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.DeviceInfo;
import com.ruoyi.vlstream.mapper.VlsDeviceInfoMapper;
import com.ruoyi.vlstream.service.IVlsDeviceInfoService;
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

/**
 * SpringBlade-compatible device routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsDeviceInfo")
public class VlsDeviceInfoController {

    private final IVlsDeviceInfoService deviceInfoService;

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
        return BladeResult.success(deviceInfoService.testDeviceConnection(id));
    }

    @PostMapping("/{id}/refresh")
    public BladeResult<Map<String, Object>> refresh(@PathVariable Long id) {
        return BladeResult.success(deviceInfoService.refreshDeviceStatus(id));
    }

    @PostMapping("/batch/refresh")
    public BladeResult<Map<String, Object>> batchRefresh(@RequestBody Map<String, Object> body) {
        return BladeResult.success(deviceInfoService.batchRefreshDevices(toLongList(body == null ? null : body.get("ids"))));
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
        return BladeResult.success(deviceInfoService.ptzControl(id, "move", body));
    }

    @PostMapping("/{id}/ptz/stop")
    public BladeResult<Map<String, Object>> ptzStop(@PathVariable Long id) {
        return BladeResult.success(deviceInfoService.ptzControl(id, "stop", Collections.<String, Object>emptyMap()));
    }

    @PostMapping("/{id}/ptz/zoom")
    public BladeResult<Map<String, Object>> ptzZoom(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return BladeResult.success(deviceInfoService.ptzControl(id, "zoom", body));
    }

    @GetMapping("/{id}/stream")
    public BladeResult<Map<String, Object>> stream(@PathVariable Long id) {
        return BladeResult.success(deviceInfoService.getVideoStreamInfo(id));
    }

    @GetMapping("/export")
    public BladeResult<List<DeviceInfo>> export(@RequestParam(required = false) String deviceIds) {
        return BladeResult.success(deviceInfoService.exportDevices(deviceIds));
    }

    @PostMapping("/import")
    public BladeResult<Map<String, Object>> importDevices(@RequestParam(value = "file", required = false) MultipartFile file) {
        return BladeResult.success(deviceInfoService.importDevices(file));
    }

    @PostMapping("/{algorithmId}/algorithms")
    public BladeResult<Map<String, Object>> dispatchAlgorithms(@PathVariable Long algorithmId,
                                                              @RequestParam String deviceIds) {
        return BladeResult.success(deviceInfoService.dispatchAlgorithms(algorithmId, deviceIds));
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
