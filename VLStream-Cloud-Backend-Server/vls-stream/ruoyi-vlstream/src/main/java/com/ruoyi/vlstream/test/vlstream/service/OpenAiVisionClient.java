/* SPDX-License-Identifier: MIT */
package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.LlmProvider;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Minimal OpenAI-compatible multimodal chat client. */
@Service
@RequiredArgsConstructor
public class OpenAiVisionClient {

	private final LlmApiKeyCipher apiKeyCipher;

	public VisionDecision review(LlmProvider provider, String prompt, List<byte[]> images) {
		if (provider == null || !Boolean.TRUE.equals(provider.getEnabled())) {
			throw new ServiceException("大模型配置不存在或已禁用");
		}
		String response = post(provider, prompt, images);
		return parseDecision(response);
	}

	private String post(LlmProvider provider, String prompt, List<byte[]> images) {
		HttpURLConnection connection = null;
		int timeoutSeconds = Math.max(5, Math.min(defaultInt(provider.getTimeoutSeconds(), 120), 600));
		try {
			connection = (HttpURLConnection) new URL(completionsUrl(provider.getBaseUrl())).openConnection();
			int timeoutMillis = timeoutSeconds * 1000;
			connection.setConnectTimeout(timeoutMillis);
			connection.setReadTimeout(timeoutMillis);
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "application/json");
			String apiKey = apiKeyCipher.decrypt(provider.getApiKeyCiphertext());
			if (StringUtils.isNotBlank(apiKey)) {
				connection.setRequestProperty("Authorization", "Bearer " + apiKey);
			}
			byte[] body = requestBody(provider.getModelName(), prompt, images)
				.getBytes(StandardCharsets.UTF_8);
			try (OutputStream output = connection.getOutputStream()) {
				output.write(body);
			}
			int status = connection.getResponseCode();
			String response = read(status >= 400 ? connection.getErrorStream() : connection.getInputStream());
			if (status < 200 || status >= 300) {
				throw new ServiceException("大模型接口返回 HTTP " + status + "：" + abbreviate(response, 500));
			}
			JSONObject root = JSONUtil.parseObj(response);
			JSONArray choices = root.getJSONArray("choices");
			if (choices == null || choices.isEmpty()) {
				throw new ServiceException("大模型响应缺少 choices");
			}
			Object content = choices.getJSONObject(0).getByPath("message.content");
			if (content == null) {
				throw new ServiceException("大模型响应缺少 message.content");
			}
			return String.valueOf(content);
		} catch (ServiceException exception) {
			throw exception;
		} catch (SocketTimeoutException exception) {
			throw new ServiceException("调用大模型超过 " + timeoutSeconds
				+ " 秒未返回，请调大模型超时或使用更小的测试图片");
		} catch (Exception exception) {
			throw new ServiceException("调用大模型失败：" + exception.getMessage());
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	String requestBody(String model, String prompt, List<byte[]> images) {
		JSONArray content = new JSONArray();
		JSONObject text = new JSONObject();
		text.set("type", "text");
		text.set("text", prompt);
		content.add(text);
		for (byte[] image : images == null ? new ArrayList<byte[]>() : images) {
			EncodedImage encodedImage = encodeImage(image);
			JSONObject imageUrl = new JSONObject();
			imageUrl.set("url", "data:" + encodedImage.getMimeType() + ";base64,"
				+ Base64.getEncoder().encodeToString(encodedImage.getBytes()));
			imageUrl.set("detail", "low");
			JSONObject imageContent = new JSONObject();
			imageContent.set("type", "image_url");
			imageContent.set("image_url", imageUrl);
			content.add(imageContent);
		}
		JSONObject message = new JSONObject();
		message.set("role", "user");
		message.set("content", content);
		JSONObject body = new JSONObject();
		body.set("model", model);
		body.set("temperature", 0);
		JSONArray messages = new JSONArray();
		messages.add(message);
		body.set("messages", messages);
		return body.toString();
	}

	VisionDecision parseDecision(String raw) {
		try {
			String json = stripCodeFence(raw);
			JSONObject value = JSONUtil.parseObj(json);
			String decision = StringUtils.lowerCase(value.getStr("decision"));
			if (!"confirmed".equals(decision) && !"rejected".equals(decision)
				&& !"uncertain".equals(decision)) {
				throw new ServiceException("大模型 decision 必须是 confirmed、rejected 或 uncertain");
			}
			BigDecimal confidence = value.getBigDecimal("confidence");
			if (confidence == null || confidence.compareTo(BigDecimal.ZERO) < 0
				|| confidence.compareTo(BigDecimal.ONE) > 0) {
				throw new ServiceException("大模型 confidence 必须在 0 到 1 之间");
			}
			VisionDecision result = new VisionDecision();
			result.setDecision(decision.toUpperCase());
			result.setConfidence(confidence);
			result.setReason(abbreviate(value.getStr("reason"), 1000));
			result.setRawResponse(abbreviate(raw, 60000));
			return result;
		} catch (ServiceException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new ServiceException("大模型未返回合法 JSON：" + exception.getMessage());
		}
	}

	private String completionsUrl(String baseUrl) {
		String value = StringUtils.removeEnd(StringUtils.trim(baseUrl), "/");
		return value.endsWith("/chat/completions") ? value : value + "/chat/completions";
	}

	private String stripCodeFence(String raw) {
		String value = StringUtils.trimToEmpty(raw);
		if (value.startsWith("```")) {
			int firstLine = value.indexOf('\n');
			int lastFence = value.lastIndexOf("```");
			if (firstLine >= 0 && lastFence > firstLine) {
				return value.substring(firstLine + 1, lastFence).trim();
			}
		}
		return value;
	}

	private String read(InputStream input) throws Exception {
		if (input == null) {
			return "";
		}
		StringBuilder result = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				result.append(line);
			}
		}
		return result.toString();
	}

	private int defaultInt(Integer value, int fallback) {
		return value == null ? fallback : value;
	}

	private EncodedImage encodeImage(byte[] image) {
		String mimeType = imageMimeType(image);
		if (!"image/png".equals(mimeType)) {
			return new EncodedImage(mimeType, image);
		}
		// Some OpenAI-compatible gateways hang while decoding palette PNGs, so send a normalized RGB image.
		try {
			BufferedImage source = ImageIO.read(new ByteArrayInputStream(image));
			if (source == null) {
				throw new ServiceException("无法解析 PNG 图片");
			}
			BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(),
				BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics = rgb.createGraphics();
			try {
				graphics.setColor(Color.WHITE);
				graphics.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
				graphics.drawImage(source, 0, 0, null);
			} finally {
				graphics.dispose();
			}
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			if (!ImageIO.write(rgb, "jpg", output)) {
				throw new ServiceException("无法转换 PNG 图片");
			}
			return new EncodedImage("image/jpeg", output.toByteArray());
		} catch (ServiceException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new ServiceException("PNG 图片转换失败：" + exception.getMessage());
		}
	}

	private String imageMimeType(byte[] image) {
		if (image != null && image.length >= 12) {
			if ((image[0] & 0xff) == 0x89 && image[1] == 'P' && image[2] == 'N' && image[3] == 'G') {
				return "image/png";
			}
			if (image[0] == 'R' && image[1] == 'I' && image[2] == 'F' && image[3] == 'F'
				&& image[8] == 'W' && image[9] == 'E' && image[10] == 'B' && image[11] == 'P') {
				return "image/webp";
			}
		}
		return "image/jpeg";
	}

	private static String abbreviate(String value, int max) {
		return StringUtils.abbreviate(StringUtils.defaultString(value), max);
	}

	private static class EncodedImage {
		private final String mimeType;
		private final byte[] bytes;

		EncodedImage(String mimeType, byte[] bytes) {
			this.mimeType = mimeType;
			this.bytes = bytes;
		}

		String getMimeType() {
			return mimeType;
		}

		byte[] getBytes() {
			return bytes;
		}
	}

	@Data
	public static class VisionDecision {
		private String decision;
		private BigDecimal confidence;
		private String reason;
		private String rawResponse;
	}
}
