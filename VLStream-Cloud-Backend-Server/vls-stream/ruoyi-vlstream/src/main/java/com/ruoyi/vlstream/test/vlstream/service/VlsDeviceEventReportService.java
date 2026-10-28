/* SPDX-License-Identifier: MIT */
package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.ResolvedCameraEvent;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.EventManagement;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** Device event adapters share authoritative ownership and the durable multi business path. */
@Service
@RequiredArgsConstructor
public class VlsDeviceEventReportService {
    private final VlsEventReportApplicationService application;
    private final WvpVlStreamDeviceResolver devices;

    public boolean isMultiTenant() { return application.isMultiTenant(); }

    public boolean reportDetection(EventManagement event, DeviceInfo snapshot, IVlsEventManagementService store) {
        if (!isMultiTenant()) return store.createEvent(event);
        if (event == null || snapshot == null || !StringUtils.equals(event.getReportDevice(), snapshot.getDeviceId())) {
            throw new ServiceException("检测事件与设备编号不一致");
        }
        DeviceInfo device = ownedDevice(snapshot.getDeviceId());
        String previous = TenantContextHolder.getTenantId();
        try {
            TenantContextHolder.setTenantId(device.getTenantId());
            return application.persistDeviceEvent(event, device) != null;
        } finally { TenantContextHolder.setTenantId(previous); }
    }

    /** The multipart management API is authenticated and may only report its current tenant's device. */
    public boolean reportMultipart(EventManagement event, MultipartFile file) {
        if (!isMultiTenant() || event == null) throw new ServiceException("多租户事件不能为空");
        DeviceInfo device = ownedDevice(event.getReportDevice());
        if (!StringUtils.equals(TenantContextHolder.getTenantId(), device.getTenantId())) {
            throw new ServiceException("当前会话租户与WVP设备归属不一致");
        }
        return application.reportMultipartEvent(event, file, device);
    }

    /** The legacy public camera API already permits tokenless reports; no UI tenant is authoritative here. */
    public List<EventManagement> reportCameras(List<Map<String, Object>> reports) {
        if (!isMultiTenant() || reports == null || reports.isEmpty()) throw new IllegalArgumentException("event_report不能为空");
        List<ResolvedCameraEvent> resolved = new ArrayList<>();
        for (Map<String, Object> body : reports) validateCamera(body);
        for (Map<String, Object> body : reports) {
            String deviceId = string(body, "device_id");
            DeviceInfo device = ownedDevice(deviceId);
            String declaredTenant = string(body, "device_tenant_id");
            if (StringUtils.isNotBlank(declaredTenant) && !declaredTenant.equals(device.getTenantId())) {
                throw new IllegalArgumentException("上报租户与WVP设备归属不一致");
            }
            String item = StringUtils.defaultIfBlank(string(body, "item"), string(body, "describe"));
            List<String> pictures = urls(body.get("pics"));
            List<String> videos = urls(body.get("video"));
            EventManagement event = new EventManagement();
            event.setReportDevice(deviceId); event.setEventType(item); event.setEventDesc(string(body, "describe"));
            event.setHandleResult(string(body, "describe")); event.setReportTime(new Date());
            event.setReportImg(pictures.isEmpty() ? null : pictures.get(0));
            event.setReportLocation(string((Map<?, ?>)body.get("point"), "address"));
            JSONObject payload = new JSONObject();
            payload.set("device_id", deviceId); payload.set("device_tenant_id", device.getTenantId());
            payload.set("device_name", device.getDeviceName()); payload.set("device_tag", string(body, "device_tag"));
            payload.set("name", string(body, "name")); payload.set("item", item); payload.set("describe", event.getEventDesc());
            payload.set("point", body.get("point")); payload.set("pics", pictures); payload.set("video", videos);
            event.setEventData(payload.toString());
            resolved.add(new ResolvedCameraEvent(event, device, payload));
        }
        return application.persistCameraEvents(resolved);
    }

    private DeviceInfo ownedDevice(String deviceId) {
        DeviceInfo device = devices.resolveEventDevice(deviceId);
        if (device == null || !StringUtils.equals(deviceId, device.getDeviceId()) || StringUtils.isBlank(device.getTenantId())) {
            throw new ServiceException("WVP设备身份或租户归属不完整");
        }
        return device;
    }

    private void validateCamera(Map<String, Object> body) {
        if (body == null || !(body.get("point") instanceof Map)) throw new IllegalArgumentException("point不能为空");
        if (StringUtils.isAnyBlank(string(body, "device_id"), string(body, "device_name"))) {
            throw new IllegalArgumentException("device_id和device_name不能为空");
        }
        String item = StringUtils.defaultIfBlank(string(body, "item"), string(body, "describe"));
        if (item.codePointCount(0,item.length()) > 10 || string(body,"name").codePointCount(0,string(body,"name").length()) > 10) {
            throw new IllegalArgumentException("事件类型和名称不能超过10个字符");
        }
        urls(body.get("pics")); urls(body.get("video"));
    }

    private List<String> urls(Object value) {
        if (value == null) return Collections.emptyList();
        if (!(value instanceof List)) throw new IllegalArgumentException("pics和video必须是URL数组");
        List<String> result = new ArrayList<>();
        for (Object item : (List<?>)value) {
            if (!(item instanceof String)) throw new IllegalArgumentException("媒体URL格式不正确");
            String url = ((String)item).trim();
            if (url.isEmpty()) continue;
            try {
                URI uri = URI.create(url);
                if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || StringUtils.isBlank(uri.getHost()) || uri.getUserInfo() != null) throw new IllegalArgumentException();
            } catch (IllegalArgumentException exception) { throw new IllegalArgumentException("媒体必须是HTTP(S) URL"); }
            if (!result.contains(url)) result.add(url);
        }
        return result;
    }

    private static String string(Map<?, ?> body, String key) {
        Object value = body.get(key); return value == null ? "" : String.valueOf(value).trim();
    }
}
