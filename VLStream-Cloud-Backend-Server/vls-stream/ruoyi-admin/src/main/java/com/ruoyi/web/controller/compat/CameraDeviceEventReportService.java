/* SPDX-License-Identifier: MIT */
package com.ruoyi.web.controller.compat;

import com.ruoyi.vlstream.test.vlstream.pojo.entity.EventManagement;
import com.ruoyi.vlstream.test.vlstream.service.VlsDeviceEventReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;

/** Keeps the single camera contract while routing all multi camera HTTP reports to the outbox. */
@Service
@RequiredArgsConstructor
public class CameraDeviceEventReportService {
    private final LocationTaskCompatService singleService;
    private final VlsDeviceEventReportService deviceReports;

    public LocationTaskResult<?> reportSingle(Map<String,Object> body) {
        if (!deviceReports.isMultiTenant()) return singleService.reportCameraEvent(body);
        return report(Collections.singletonList(body));
    }

    public LocationTaskResult<?> reportBatch(Map<String,Object> body) {
        if (!deviceReports.isMultiTenant()) return singleService.reportCameraEvents(body);
        Object value = body.get("event_report");
        if (!(value instanceof List) || ((List<?>)value).isEmpty()) return LocationTaskResult.error(4101,"event_report不能为空");
        List<Map<String,Object>> events = new ArrayList<>();
        for (Object event : (List<?>)value) {
            if (!(event instanceof Map)) return LocationTaskResult.error(4101,"event_report格式不正确");
            @SuppressWarnings("unchecked") Map<String,Object> camera = (Map<String,Object>)event;
            events.add(camera);
        }
        return report(events);
    }

    private LocationTaskResult<?> report(List<Map<String,Object>> reports) {
        try {
            List<EventManagement> accepted = deviceReports.reportCameras(reports);
            List<String> ids = new ArrayList<>();
            for (EventManagement event : accepted) ids.add(String.valueOf(event.getId()));
            Map<String,Object> data = new LinkedHashMap<>();
            data.put("delivery_status","PENDING"); data.put("source_event_ids",ids);
            return LocationTaskResult.success(data);
        } catch (IllegalArgumentException exception) {
            return LocationTaskResult.error(4101,exception.getMessage());
        }
    }
}
