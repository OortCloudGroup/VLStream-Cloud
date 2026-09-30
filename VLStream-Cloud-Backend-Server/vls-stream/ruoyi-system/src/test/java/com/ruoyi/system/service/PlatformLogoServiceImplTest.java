/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.SysPlatformLogo;
import com.ruoyi.system.domain.vo.PlatformLogoVo;
import com.ruoyi.system.domain.vo.SysOssVo;
import com.ruoyi.system.mapper.SysPlatformLogoMapper;
import com.ruoyi.system.service.impl.PlatformLogoServiceImpl;
import com.ruoyi.oss.core.OssClient;
import com.ruoyi.oss.enumd.AccessPolicyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("dev")
class PlatformLogoServiceImplTest {

    private SysPlatformLogoMapper mapper;
    private ISysOssService ossService;
    private PlatformLogoServiceImpl service;
    private OssClient storage;

    @BeforeEach
    void setUp() {
        mapper = mock(SysPlatformLogoMapper.class);
        ossService = mock(ISysOssService.class);
        storage = mock(OssClient.class);
        service = new PlatformLogoServiceImpl(mapper, ossService) {
            @Override protected OssClient logoStorage(String configKey) { return storage; }
        };
    }

    @Test
    void listReturnsProtectedDefaultWhenTenantHasNoCustomConfiguration() {
        when(mapper.selectList(any())).thenReturn(Collections.<SysPlatformLogo>emptyList());

        List<PlatformLogoVo> result = service.list();

        assertEquals(1, result.size());
        assertNull(result.get(0).getId());
        assertTrue(result.get(0).getSystemDefault());
        assertTrue(result.get(0).getActive());
        verify(ossService, never()).getById(any());
    }

    @Test
    void currentReturnsCustomLogoWithFreshOssUrl() {
        SysPlatformLogo entity = new SysPlatformLogo();
        entity.setId(10L);
        entity.setOssId(20L);
        entity.setDescription("Police console");
        entity.setActive(true);
        when(mapper.selectOne(any())).thenReturn(entity);
        SysOssVo oss = new SysOssVo();
        oss.setOssId(20L);
        oss.setUrl("https://objects.example/logo.png");
        when(ossService.getById(20L)).thenReturn(oss);

        PlatformLogoVo result = service.current();

        assertEquals(10L, result.getId());
        assertEquals("https://objects.example/logo.png", result.getLogoUrl());
        assertFalse(result.getSystemDefault());
        assertTrue(result.getActive());
    }

    @Test
    void createStoresOnlyOssReferenceAndStartsInactive() {
        MultipartFile file = mock(MultipartFile.class);
        SysOssVo oss = new SysOssVo();
        oss.setOssId(30L);
        oss.setUrl("https://objects.example/new.png");
        when(ossService.upload(file)).thenReturn(oss);
        when(ossService.getById(30L)).thenReturn(oss);

        PlatformLogoVo result = service.create(file, "  New brand  ");

        assertEquals("New brand", result.getDescription());
        assertFalse(result.getActive());
        assertEquals("https://objects.example/new.png", result.getLogoUrl());
        verify(mapper).insert(any(SysPlatformLogo.class));
    }

    @Test
    void missingIdCannotMutateSystemDefault() {
        assertThrows(ServiceException.class, () -> service.update(null, null, "changed"));
        assertThrows(ServiceException.class, () -> service.remove(null));
        verify(mapper, never()).updateById(any(SysPlatformLogo.class));
        verify(mapper, never()).deleteById(any());
    }

    @Test
    void privateLogoSignsObjectKeyWithPublicEndpointWithoutCachingSignature() throws Exception {
        java.lang.reflect.Field endpoint = PlatformLogoServiceImpl.class.getDeclaredField("publicEndpoint");
        endpoint.setAccessible(true);
        endpoint.set(service, "https://public.example:2443");
        SysPlatformLogo entity = new SysPlatformLogo();
        entity.setId(10L); entity.setOssId(20L); entity.setActive(true);
        when(mapper.selectOne(any())).thenReturn(entity);
        SysOssVo oss = new SysOssVo();
        oss.setFileName("2026/09/logo.png"); oss.setUrl("http://frontend/bucket/2026/09/logo.png");
        when(ossService.getById(20L)).thenReturn(oss);
        when(storage.getAccessPolicy()).thenReturn(AccessPolicyType.PRIVATE);
        when(storage.getPrivateUrl("2026/09/logo.png", 3600, "https://public.example:2443"))
            .thenReturn("https://public.example:2443/signed-first", "https://public.example:2443/signed-second");
        assertEquals("https://public.example:2443/signed-first", service.current().getLogoUrl());
        assertEquals("https://public.example:2443/signed-second", service.current().getLogoUrl());
        assertEquals("http://frontend/bucket/2026/09/logo.png", oss.getUrl());
    }

    @Test
    void missingOssMetadataLeavesFallbackAvailable() {
        SysPlatformLogo entity = new SysPlatformLogo();entity.setOssId(20L);
        when(mapper.selectOne(any())).thenReturn(entity);
        assertNull(service.current().getLogoUrl());
        org.mockito.Mockito.verifyNoInteractions(storage);
    }
}
