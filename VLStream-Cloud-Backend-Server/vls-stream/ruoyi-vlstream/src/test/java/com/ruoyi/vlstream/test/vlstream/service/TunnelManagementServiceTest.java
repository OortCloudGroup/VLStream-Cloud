package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.core.domain.model.LoginUser;
import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage;
import cn.dev33.satoken.context.model.SaStorage;
import com.ruoyi.vlstream.test.vlstream.config.VlsTunnelProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.TunnelAccessSessionMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.TunnelEndpointMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.TunnelEnrollmentMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("dev")
class TunnelManagementServiceTest {
    private final SaTokenContext previousContext = SaManager.getSaTokenContext();

    @AfterEach
    void resetTenant() {
        TenantContextHolder.clear();
        SaTokenContextForThreadLocalStorage.clearBox();
        SaManager.setSaTokenContext(previousContext);
    }

    @Test
    void enrollmentUsesWvpOwnershipAndRejectsCrossTenantDeviceId() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "test"),
            com.ruoyi.vlstream.test.vlstream.pojo.entity.TunnelEnrollment.class);
        TenantContextHolder.setTenantId("tenant-a");
        LoginUser user = new LoginUser();
        user.setTenantId("tenant-a");
        SaStorage storage = mock(SaStorage.class);
        when(storage.get(LoginHelper.LOGIN_USER_KEY)).thenReturn(user);
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        SaTokenContextForThreadLocalStorage.setBox(null, null, storage);
        VlsTunnelProperties properties = new VlsTunnelProperties();
        properties.setEnabled(true);
        TunnelEndpointMapper endpoints = mock(TunnelEndpointMapper.class);
        TunnelEnrollmentMapper enrollments = mock(TunnelEnrollmentMapper.class);
        WvpVlStreamDeviceResolver resolver = mock(WvpVlStreamDeviceResolver.class);
        DeviceInfo device = new DeviceInfo();
        device.setDeviceId("CAM-1");
        device.setId(1L);
        device.setTenantId("tenant-b");
        when(resolver.resolve("CAM-1")).thenReturn(device);
        TunnelManagementService service = new TunnelManagementService(endpoints, enrollments,
            mock(TunnelAccessSessionMapper.class), resolver, new TunnelTokenService(properties), properties);
        assertThrows(ServiceException.class, () -> service.issueEnrollment("CAM-1"));
        verifyNoInteractions(endpoints, enrollments);

        device.setTenantId("tenant-a");
        assertEquals("CAM-1", service.issueEnrollment("CAM-1").getDeviceId());
        verify(endpoints).insert(any());
        verify(enrollments).insert(any());
    }
}
