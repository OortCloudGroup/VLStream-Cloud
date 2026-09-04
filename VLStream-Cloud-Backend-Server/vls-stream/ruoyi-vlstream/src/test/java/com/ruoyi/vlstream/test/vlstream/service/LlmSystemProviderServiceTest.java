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
