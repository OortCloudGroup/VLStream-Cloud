/* SPDX-License-Identifier: MIT */
package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.mapper.LlmProviderMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.LlmProvider;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Tag("dev")
class LlmAnnotationProviderServiceTest {
    private final LlmProviderMapper providers = mock(LlmProviderMapper.class);
    private final LlmSystemProviderService systemProvider = mock(LlmSystemProviderService.class);
    private final OpenAiVisionClient vision = mock(OpenAiVisionClient.class);
    private final LlmAnnotationProviderService service = new LlmAnnotationProviderService(providers, systemProvider, vision);

    @Test
    void listsOnlyUsableTenantProvidersWithAnExactStringId() {
        LlmProvider enabled = provider("tenant-a");
        LlmProvider disabled = provider("tenant-a");
        disabled.setEnabled(false);
        LlmProvider foreign = provider("tenant-b");
        LlmProvider revoked = provider("tenant-a");
        revoked.setName("OortCloud");
        when(systemProvider.isSystemProvider(revoked)).thenReturn(true);
        when(providers.selectList(any())).thenReturn(Arrays.asList(enabled, disabled, foreign, revoked));

        List<LlmAnnotationProviderService.ProviderOption> options = service.listProviders("tenant-a");
        assertEquals(1, options.size());
        assertEquals("733305449657851908", options.get(0).getId());
        assertEquals("review-model", options.get(0).getModelName());
        verifyNoInteractions(vision);
    }

    @Test
    void rejectsCrossTenantAccessWithoutCallingProvider() {
        LlmProvider foreign = provider("tenant-b");
        when(providers.selectOne(any())).thenReturn(foreign);

        assertEquals(Integer.valueOf(409), assertThrows(ServiceException.class,
            () -> service.listModels("tenant-a", foreign.getId())).getCode());
        verifyNoInteractions(vision);
    }

    @Test
    void freezesSelectedModelAndRejectsEndpointOrCredentialChanges() {
        LlmProvider provider = provider("tenant-a");
        when(providers.selectOne(any())).thenReturn(provider);
        LlmAnnotationProviderService.Binding binding = service.bind("tenant-a", provider.getId(), "annotation-model");
        assertEquals("annotation-model", binding.getModelName());
        assertEquals("review-model", provider.getModelName());
        assertEquals(64, binding.getProviderFingerprint().length());

        provider.setModelName("other-review-model");
        service.verifyBinding("tenant-a", provider.getId(), binding.getModelName(), binding.getProviderFingerprint());
        assertThrows(ServiceException.class,
            () -> service.verifyBinding("tenant-a", provider.getId(), provider.getModelName(), binding.getProviderFingerprint()));

        provider.setBaseUrl("https://new-provider.example/v1");
        assertThrows(ServiceException.class,
            () -> service.verifyBinding("tenant-a", provider.getId(), binding.getModelName(), binding.getProviderFingerprint()));

        provider.setBaseUrl("https://provider.example/v1");
        provider.setApiKeyCiphertext("rotated-fixture-ciphertext");
        assertThrows(ServiceException.class,
            () -> service.annotate("tenant-a", provider.getId(), binding.getModelName(), binding.getProviderFingerprint(), "prompt", new byte[0]));
        verifyNoInteractions(vision);
    }

    @Test
    void rejectsAuthorizedAccountSwitchEvenWithSameCiphertext() {
        LlmProvider provider = provider("tenant-a");
        provider.setPlatformUserId("old-account");
        provider.setAuthorizedAt(new Date(0));
        when(providers.selectOne(any())).thenReturn(provider);
        when(systemProvider.isSystemProvider(provider)).thenReturn(true);
        when(systemProvider.isAuthorized(provider)).thenReturn(true);
        LlmAnnotationProviderService.Binding binding = service.bind("tenant-a", provider.getId(), null);
        assertEquals("review-model", binding.getModelName());

        provider.setPlatformUserId("new-account");
        assertThrows(ServiceException.class, () -> service.verifyBinding(
            "tenant-a", provider.getId(), binding.getModelName(), binding.getProviderFingerprint()));
    }

    private LlmProvider provider(String tenantId) {
        LlmProvider provider = new LlmProvider();
        provider.setTenantId(tenantId);
        provider.setId(733305449657851908L);
        provider.setEnabled(true);
        provider.setBaseUrl("https://provider.example/v1");
        provider.setModelName("review-model");
        provider.setName("Provider");
        provider.setApiKeyCiphertext("fixture-ciphertext");
        return provider;
    }
}
