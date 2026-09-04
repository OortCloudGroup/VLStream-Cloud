/* SPDX-License-Identifier: MIT */
package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.mapper.AlgorithmLlmReviewConfigMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.LlmProviderMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.Algorithm;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmLlmReviewConfig;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.LlmProvider;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
class LlmReviewManagementServiceTest {

	@Mock
	private LlmProviderMapper providerMapper;
	@Mock
	private AlgorithmLlmReviewConfigMapper configMapper;
	@Mock
	private VlsAlgorithmMapper algorithmMapper;
	@Mock
	private LlmApiKeyCipher apiKeyCipher;
	@Mock
	private LlmSystemProviderService systemProviderService;

	@Test
	void createsCustomProviderWithEncryptedApiKey() {
		when(apiKeyCipher.encrypt("external-secret")).thenReturn("encrypted-secret");
		LlmProvider input = provider(null, "External Relay", "external-secret");

		LlmProvider saved = service().saveProvider(input);

		ArgumentCaptor<LlmProvider> inserted = ArgumentCaptor.forClass(LlmProvider.class);
		verify(providerMapper).insert(inserted.capture());
		assertEquals("encrypted-secret", inserted.getValue().getApiKeyCiphertext());
		assertEquals(Integer.valueOf(1), inserted.getValue().getStatus());
		assertEquals(Integer.valueOf(0), inserted.getValue().getIsDeleted());
		assertNull(saved.getApiKey());
		assertTrue(saved.getApiKeyConfigured());
	}

	@Test
	void refusesToEditOrDeleteBuiltInProvider() {
		LlmProvider builtIn = provider(1L, LlmSystemProviderService.SYSTEM_PROVIDER_NAME, null);
		when(providerMapper.selectById(1L)).thenReturn(builtIn);
		when(systemProviderService.isSystemProvider(builtIn)).thenReturn(true);

		assertThrows(ServiceException.class, () -> service().saveProvider(builtIn));
		assertThrows(ServiceException.class, () -> service().deleteProvider(1L));
		verify(providerMapper, never()).updateById(any());
		verify(providerMapper, never()).deleteById(1L);
	}

	@Test
	void enablesReviewWithExplicitCustomProviderWithoutOortCloudAuthorization() {
		LlmProvider custom = provider(22L, "External Relay", null);
		custom.setApiKeyCiphertext("encrypted-secret");
		when(algorithmMapper.selectById(11L)).thenReturn(new Algorithm());
		when(providerMapper.selectById(22L)).thenReturn(custom);
		when(configMapper.selectOne(any())).thenReturn(null);

		AlgorithmLlmReviewConfig input = new AlgorithmLlmReviewConfig();
		input.setProviderId(22L);
		input.setEnabled(true);
		AlgorithmLlmReviewConfig saved = service().saveAlgorithmConfig(11L, input);

		assertEquals(Long.valueOf(22L), saved.getProviderId());
		assertTrue(saved.getEnabled());
		verify(systemProviderService, never()).isAuthorized(any());
		verify(configMapper).insert(saved);
	}

	private LlmProvider provider(Long id, String name, String apiKey) {
		LlmProvider provider = new LlmProvider();
		provider.setId(id);
		provider.setName(name);
		provider.setBaseUrl("https://relay.example.test/v1/chat/completions");
		provider.setModelName("vision-model");
		provider.setApiKey(apiKey);
		provider.setTimeoutSeconds(120);
		provider.setEnabled(true);
		return provider;
	}

	private LlmReviewManagementService service() {
		return new LlmReviewManagementService(providerMapper, configMapper, algorithmMapper,
			apiKeyCipher, systemProviderService);
	}
}
