package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.config.WvpVlStreamDeviceProperties;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ModelDispatchAccessTest {
    WvpVlStreamDeviceResolver resolver;
    ModelDispatchAccess access;
    DeviceInfo device;
    JSONObject detail;
    @BeforeEach void setup() {
        TenantContextHolder.clear(); resolver = mock(WvpVlStreamDeviceResolver.class);
        LoginUser user = new LoginUser(); user.setTenantId("tenant-a"); user.setUserId("alice");
        device = new DeviceInfo(); device.setDeviceId("CAM-A"); device.setId(101L); device.setTenantId("tenant-a");
        when(resolver.resolveOnline("CAM-A")).thenReturn(device);
        detail = new JSONObject().set("device", new JSONObject().set("deviceId", "CAM-A").set("online", true)
            .set("deviceModel", "Hi3519DV500").set("capabilitiesJson", "[\"video\",\"aiInfer\"]"));
        access = new ModelDispatchAccess(resolver, new WvpVlStreamDeviceProperties()) {
            @Override protected LoginUser actor() { return user; }
            @Override protected JSONObject authorizedDetail(Long row) { assertEquals(101L, row); return detail; }
        };
    }
    @Test void allowsConfirmedBoardOmAndUsesBusinessId() {
        assertSame(device, access.requireDevice("CAM-A", "om", true)); verify(resolver).resolveOnline("CAM-A");
    }
    @Test void rejectsForeignTenantEvenIfDeviceBusinessIdKnown() {
        device.setTenantId("tenant-b"); assertThrows(ServiceException.class, () -> access.requireDevice("CAM-A", "om", true));
    }
    @Test void rejectsUnknownCapabilitiesAndWrongModelType() {
        detail.getJSONObject("device").set("capabilitiesJson", "[]");
        assertThrows(ServiceException.class, () -> access.requireDevice("CAM-A", "om", true));
        detail.getJSONObject("device").set("capabilitiesJson", "[\"aiInfer\"]");
        assertThrows(ServiceException.class, () -> access.requireDevice("CAM-A", "onnx", true));
        detail.getJSONObject("device").set("deviceModel", "unknown");
        assertThrows(ServiceException.class, () -> access.requireDevice("CAM-A", "om", true));
    }
    @Test void rejectsOfflineAndMismatchedAuthorizedDetail() {
        detail.getJSONObject("device").set("online", false);
        assertThrows(ServiceException.class, () -> access.requireDevice("CAM-A", "om", true));
        detail.getJSONObject("device").set("online", true).set("deviceId", "CAM-B");
        assertThrows(ServiceException.class, () -> access.requireDevice("CAM-A", "om", true));
    }
    @Test void deniedWvpPermissionFailsClosed() {
        ModelDispatchAccess denied = spy(access); doThrow(new ServiceException("forbidden")).when(denied).authorizedDetail(101L);
        assertThrows(ServiceException.class, () -> denied.requireDevice("CAM-A", "om", true));
    }
    @Test void legacyDispatchKeepsFormatCompatibilityButStillChecksOwnershipAndOnline() {
        detail.getJSONObject("device").set("capabilitiesJson", null).set("deviceModel", "legacy");
        assertSame(device, access.requireDevice("CAM-A", "pt", false));
        device.setTenantId("tenant-b"); assertThrows(ServiceException.class, () -> access.requireDevice("CAM-A", "pt", false));
    }
}
