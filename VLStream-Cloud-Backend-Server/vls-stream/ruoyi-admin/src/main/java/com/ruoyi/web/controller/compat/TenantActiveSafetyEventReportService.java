/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */
package com.ruoyi.web.controller.compat;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.ActiveSafetyEventReport;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.ActiveSafetyEventReportResult;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.EventManagement;
import com.ruoyi.vlstream.test.vlstream.service.ActiveSafetyEventReportService;
import com.ruoyi.vlstream.test.vlstream.service.VlsEventReportApplicationService;
import com.ruoyi.vlstream.test.vlstream.service.WvpVlStreamDeviceResolver;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/** Selects the business sink for MQTT ingestion and confirmed LLM review alike. */
@Primary
@Service
@RequiredArgsConstructor
public class TenantActiveSafetyEventReportService implements ActiveSafetyEventReportService {

    private final LocationTaskCompatService singleTenantService;
    private final VlsEventReportApplicationService eventApplicationService;
    private final WvpVlStreamDeviceResolver deviceResolver;

    @Override
    public ActiveSafetyEventReportResult findDuplicate(String messageId, String deviceId, String eventId) {
        if (!eventApplicationService.isMultiTenant()) {
            return singleTenantService.findDuplicate(messageId, deviceId, eventId);
        }
        requireTenant();
        return result(eventApplicationService.findDuplicate(messageId, deviceId, eventId), true);
    }

    @Override
    public ActiveSafetyEventReportResult report(ActiveSafetyEventReport report) {
        if (!eventApplicationService.isMultiTenant()) {
            return singleTenantService.report(report);
        }
        String tenantId = requireTenant();
        if (report == null || StringUtils.isBlank(report.getDeviceId())) {
            throw new ServiceException("事件设备不能为空");
        }
        DeviceInfo device = deviceResolver.resolveEventDevice(report.getDeviceId());
        if (device == null || !StringUtils.equals(tenantId, device.getTenantId())) {
            throw new ServiceException("事件租户与WVP设备归属不一致");
        }
        ActiveSafetyEventReportResult duplicate = findDuplicate(report.getSourceMessageId(),
            report.getDeviceId(), report.getDeviceEventId());
        return duplicate != null ? duplicate
            : result(eventApplicationService.persistActiveSafetyEvent(report, device), false);
    }

    private String requireTenant() {
        String tenantId = TenantContextHolder.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            throw new ServiceException("多租户事件缺少可信租户上下文");
        }
        return tenantId;
    }

    private ActiveSafetyEventReportResult result(EventManagement event, boolean duplicate) {
        return event == null ? null : ActiveSafetyEventReportResult.builder()
            .activeSafetyEventId(String.valueOf(event.getId()))
            .mediaId(event.getMediaId()).duplicate(duplicate).build();
    }
}
