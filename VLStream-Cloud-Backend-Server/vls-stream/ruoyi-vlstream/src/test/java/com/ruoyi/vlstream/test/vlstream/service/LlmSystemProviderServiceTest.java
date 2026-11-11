/* SPDX-License-Identifier: MIT */
package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.ruoyi.vlstream.test.vlstream.config.VlsLlmReviewProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.LlmProviderMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.LlmProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("dev")
class LlmSystemProviderServiceTest {

	@Mock
	private LlmProviderMapper providerMapper;

	@Mock
	private LlmApiKeyCipher apiKeyCipher;

	@Test
	void cancelledAuthorizationAndSelectedModelSurviveAccountReload() {
		VlsLlmReviewProperties properties = new VlsLlmReviewProperties();
		LlmProvider provider = new LlmProvider();
		provider.setId(1L);
		provider.setBaseUrl(properties.getBaseUrl());
		provider.setTimeoutSeconds(properties.getTimeoutSeconds());
		provider.setModelName("selected-vision-model");
		provider.setEnabled(false);
		when(providerMapper.selectOne(any())).thenReturn(provider);
		LlmSystemProviderService service = new LlmSystemProviderService(providerMapper, properties, apiKeyCipher);
		assertFalse(service.getOrCreate().getEnabled());
		assertEquals("selected-vision-model", service.getOrCreate().getModelName());
		assertFalse(service.isAuthorized(service.authorize("new-account", "New", "new-key", true)));
		org.mockito.Mockito.verifyNoInteractions(apiKeyCipher);
		org.mockito.Mockito.verify(providerMapper, org.mockito.Mockito.never()).updateById(any());
		org.mockito.Mockito.verify(providerMapper, org.mockito.Mockito.never()).update(any(), any());
	}

	@Test
	void disabledProviderCannotUseRetainedCredential() {
		LlmProvider provider = new LlmProvider();
		provider.setEnabled(false);
		provider.setPlatformUserId("account");
		provider.setAuthorizedAt(new java.util.Date());
		provider.setApiKeyCiphertext("ciphertext");
		assertFalse(new LlmSystemProviderService(providerMapper, new VlsLlmReviewProperties(), apiKeyCipher)
			.isAuthorized(provider));
	}

	@Test
	void createsProviderWithExplicitPersistedEmptyApiKey() throws Exception {
		when(providerMapper.selectOne(any())).thenReturn(null);
		LlmSystemProviderService service = new LlmSystemProviderService(providerMapper,
			new VlsLlmReviewProperties(), apiKeyCipher);

		LlmProvider provider = service.getOrCreate();

		ArgumentCaptor<LlmProvider> inserted = ArgumentCaptor.forClass(LlmProvider.class);
		verify(providerMapper).insert(inserted.capture());
		assertEquals("", provider.getApiKeyCiphertext());
		assertEquals("", inserted.getValue().getApiKeyCiphertext());

		Field field = LlmProvider.class.getDeclaredField("apiKeyCiphertext");
		TableField mapping = field.getAnnotation(TableField.class);
		assertNotNull(mapping);
		assertEquals(FieldStrategy.IGNORED, mapping.insertStrategy());
		assertEquals(FieldStrategy.IGNORED, mapping.updateStrategy());
	}

	@Test
	void reportsWhetherPersistedTenantCredentialIsConfigured() {
		VlsLlmReviewProperties properties = new VlsLlmReviewProperties();
		LlmSystemProviderService service = new LlmSystemProviderService(providerMapper, properties, apiKeyCipher);
		LlmProvider provider = new LlmProvider();

		assertFalse(service.mask(provider).getApiKeyConfigured());
		provider.setApiKeyCiphertext("ciphertext");
		assertTrue(service.mask(provider).getApiKeyConfigured());
		assertTrue(provider.getSystemProvider());
	}
}
