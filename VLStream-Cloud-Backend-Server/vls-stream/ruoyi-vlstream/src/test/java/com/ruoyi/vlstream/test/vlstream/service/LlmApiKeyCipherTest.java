/* SPDX-License-Identifier: MIT */
package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.vlstream.test.vlstream.config.VlsLlmReviewProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Tag("dev")
class LlmApiKeyCipherTest {

	@Test
	void encryptsAndDecryptsWithBuiltInKey() {
		VlsLlmReviewProperties properties = new VlsLlmReviewProperties();
		LlmApiKeyCipher cipher = new LlmApiKeyCipher(properties);
		String apiKey = "sk-test-only";

		String encrypted = cipher.encrypt(apiKey);

		assertEquals(32, properties.getEncryptionKey().length());
		assertNotEquals(apiKey, encrypted);
		assertEquals(apiKey, cipher.decrypt(encrypted));
	}
}
