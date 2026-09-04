/* SPDX-License-Identifier: MIT */
package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.config.VlsLlmReviewProperties;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.LlmProvider;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Tag("dev")
class OpenAiVisionClientTest {

	private final LlmApiKeyCipher apiKeyCipher = mock(LlmApiKeyCipher.class);
	private final VlsLlmReviewProperties properties = new VlsLlmReviewProperties();
	private final OpenAiVisionClient client = new OpenAiVisionClient(apiKeyCipher);

	@Test
	void defaultsToCompanyRelayAndVisionModel() {
		assertEquals("https://workup.oortcloudsmart.com:2443/bus/apaas-newapi/v1/chat/completions",
			properties.getBaseUrl());
		assertEquals("qwen3.6-flash", properties.getModelName());
	}

	@Test
	void decryptsPersistedProviderKey() {
		LlmProvider provider = new LlmProvider();
		provider.setApiKeyCiphertext("ciphertext");
		when(apiKeyCipher.decrypt("ciphertext")).thenReturn("provider-key");

		assertEquals("provider-key", client.resolveApiKey(provider));
	}

	@Test
	void parsesStrictDecisionJsonInsideCodeFence() {
		OpenAiVisionClient.VisionDecision result = client.parseDecision(
			"```json\n{\"decision\":\"confirmed\",\"confidence\":0.91,\"reason\":\"存在目标\"}\n```");

		assertEquals("CONFIRMED", result.getDecision());
		assertEquals(0, new BigDecimal("0.91").compareTo(result.getConfidence()));
		assertEquals("存在目标", result.getReason());
	}

	@Test
	void rejectsDecisionOutsideSupportedValues() {
		assertThrows(ServiceException.class, () -> client.parseDecision(
			"{\"decision\":\"maybe\",\"confidence\":0.8,\"reason\":\"unknown\"}"));
	}

	@Test
	void acceptsPlainTextForConnectivityTestWithoutRelaxingFormalReview() {
		OpenAiVisionClient.VisionDecision result = client.parseTestResponse("图片中是默认占位图。 ");

		assertEquals("CONNECTED", result.getDecision());
		assertEquals("图片中是默认占位图。 ", result.getReason());
		assertThrows(ServiceException.class, () -> client.parseDecision("图片中是默认占位图。 "));
	}

	@Test
	void createsOpenAiCompatibleMultimodalRequest() {
		String body = client.requestBody("vision-model", "review", Collections.singletonList(
			"image".getBytes(StandardCharsets.UTF_8)));
		JSONObject json = JSONUtil.parseObj(body);

		assertEquals("vision-model", json.getStr("model"));
		assertEquals(Boolean.FALSE, json.getBool("stream"));
		assertEquals("review", json.getByPath("messages[0].content[0].text"));
		assertTrue(String.valueOf(json.getByPath("messages[0].content[1].image_url.url"))
			.startsWith("data:image/jpeg;base64,"));
		assertEquals("low", json.getByPath("messages[0].content[1].image_url.detail"));
	}

	@Test
	void convertsIndexedPngToRgbJpegBeforeSending() throws Exception {
		BufferedImage indexed = new BufferedImage(4, 4, BufferedImage.TYPE_BYTE_INDEXED);
		ByteArrayOutputStream png = new ByteArrayOutputStream();
		ImageIO.write(indexed, "png", png);

		String body = client.requestBody("vision-model", "review",
			Collections.singletonList(png.toByteArray()));
		JSONObject json = JSONUtil.parseObj(body);

		assertTrue(String.valueOf(json.getByPath("messages[0].content[1].image_url.url"))
			.startsWith("data:image/jpeg;base64,"));
	}
}
