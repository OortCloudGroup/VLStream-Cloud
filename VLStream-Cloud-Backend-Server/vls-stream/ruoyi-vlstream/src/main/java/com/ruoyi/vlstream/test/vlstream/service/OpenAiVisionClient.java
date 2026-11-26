/* SPDX-License-Identifier: MIT */
package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.apache.commons.io.input.BoundedInputStream;

/** Minimal OpenAI-compatible multimodal chat client. */
@Service
@RequiredArgsConstructor
public class OpenAiVisionClient {

	public static final int MAX_ANNOTATION_IMAGE_BYTES = 8 * 1024 * 1024;
	private static final int MAX_ANNOTATION_RESPONSE_BYTES = 2 * 1024 * 1024;
	private static final int MAX_ANNOTATION_CONTENT_CHARS = 1024 * 1024;
	// SSE envelopes and reasoning deltas are much larger than the resulting HTML.
	private static final int MAX_SCENE_STREAM_BYTES = 24 * 1024 * 1024;
	private static final int GLM_SCENE_TOKENS = 32768;
	private static final int SCENE_TOKENS = 16384;
	private static final int SAFE_SCENE_TOKENS = 8192;
	private static final JsonFactory ANNOTATION_JSON = new JsonFactory().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
	private static final String OORTCLOUD_MODELS_BASE = "https://workup.oortcloudsmart.com:2443/bus/apaas-newapi/v1";

	private final LlmApiKeyCipher apiKeyCipher;

	/** Read the provider's actual model catalogue without exposing its stored key. */
	public List<String> listModels(LlmProvider provider) {
		return listModels(provider, false, false);
	}

	List<String> listAnnotationModels(LlmProvider provider) {
		return listModels(provider, true, false);
	}

	List<String> listSceneModels(LlmProvider provider) {
		return listModels(provider, false, true);
	}

	private List<String> listModels(LlmProvider provider, boolean annotation, boolean scene) {
		if (provider == null || !Boolean.TRUE.equals(provider.getEnabled())) {
			throw new ServiceException("大模型配置不存在或已禁用");
		}
		HttpURLConnection connection = null;
		try {
			String key = resolveApiKey(provider);
			if (StringUtils.isBlank(key)) throw new ServiceException("大模型 API Key 未配置");
			String base = StringUtils.removeEnd(StringUtils.trim(provider.getBaseUrl()), "/");
			base = StringUtils.removeEnd(base, "/chat/completions");
			connection = (HttpURLConnection) new URL(base + "/models").openConnection();
			if (annotation || scene) connection.setInstanceFollowRedirects(false);
			connection.setConnectTimeout(15000);
			connection.setReadTimeout(15000);
			connection.setRequestProperty("Authorization", "Bearer " + key);
			int status = connection.getResponseCode();
			if (status < 200 || status >= 300) throw new ServiceException("加载模型列表失败，HTTP " + status);
			String response = annotation || scene ? readModelResponse(connection.getInputStream()) : read(connection.getInputStream());
			JSONArray models = JSONUtil.parseObj(response).getJSONArray("data");
			if (models == null) throw new ServiceException("模型列表响应格式不正确");
			Set<String> names = new LinkedHashSet<>();
			for (int i = 0; i < models.size(); i++) {
				JSONObject model = models.getJSONObject(i);
				String name = model.getStr("id");
				if (StringUtils.isNotBlank(name) && (!annotation || isAnnotationModel(model, name, base))
					&& (!scene || isTextModel(model))) names.add(name);
			}
			return new ArrayList<>(names);
		} catch (ServiceException exception) {
			throw exception;
		} catch (Exception exception) {
			throw new ServiceException("加载模型列表失败，请检查模型服务连接");
		} finally {
			if (connection != null) connection.disconnect();
		}
	}

	private static boolean isTextModel(JSONObject model) {
		Object architecture = model.get("architecture");
		JSONObject details = architecture instanceof JSONObject ? (JSONObject)architecture : new JSONObject();
		return allowsModality(model.get("input_modalities"), "text") && allowsModality(model.get("output_modalities"), "text")
			&& allowsModality(details.get("input_modalities"), "text") && allowsModality(details.get("output_modalities"), "text");
	}

	static boolean isAnnotationModel(JSONObject model, String name, String base) {
		Object architecture = model.get("architecture");
		JSONObject metadata = architecture instanceof JSONObject ? (JSONObject) architecture : new JSONObject();
		Object input = model.get("input_modalities");
		Object architectureInput = metadata.get("input_modalities");
		if (!allowsModality(input, "image") || !allowsModality(architectureInput, "image")
			|| !allowsModality(model.get("output_modalities"), "text") || !allowsModality(metadata.get("output_modalities"), "text")) {
			return false;
		}
		// The OortCloud catalogue omits modalities. These exact relay models have accepted image input in VLS.
		return input != null || architectureInput != null || (OORTCLOUD_MODELS_BASE.equalsIgnoreCase(base)
			&& ("glm-5.3-flash".equalsIgnoreCase(name) || "glm-5.4".equalsIgnoreCase(name)
				|| "qwen3.6-flash".equalsIgnoreCase(name)));
	}

	private static boolean allowsModality(Object declared, String required) {
		if (declared == null) return true;
		return declared instanceof JSONArray && ((JSONArray) declared).stream()
			.anyMatch(value -> value instanceof String && required.equalsIgnoreCase((String) value));
	}

	public VisionDecision review(LlmProvider provider, String prompt, List<byte[]> images) {
		if (provider == null || !Boolean.TRUE.equals(provider.getEnabled())) {
			throw new ServiceException("大模型配置不存在或已禁用");
		}
		String response = post(provider, prompt, images);
		return parseDecision(response);
	}

	/** Connectivity tests may use a free-form prompt, while formal reviews stay strict JSON. */
	public VisionDecision test(LlmProvider provider, String prompt, List<byte[]> images) {
		if (provider == null || !Boolean.TRUE.equals(provider.getEnabled())) {
			throw new ServiceException("大模型配置不存在或已禁用");
		}
		return parseTestResponse(post(provider, prompt, images));
	}

	/** Sends original pixels for dataset geometry and returns the provider's raw text. */
	String annotate(LlmProvider provider, String model, String prompt, byte[] image) {
		if (image == null || image.length == 0 || image.length > MAX_ANNOTATION_IMAGE_BYTES) {
			throw new ServiceException("智能标注图片为空或超过 8 MiB");
		}
		if (StringUtils.isBlank(prompt) || prompt.length() > 32768) {
			throw new ServiceException("智能标注提示词为空或超过处理限制");
		}
		String mimeType = annotationMimeType(image);
		return complete(provider, requestBody(model, prompt, Collections.singletonList(image), mimeType), "智能标注", null);
	}

	void generateScene(LlmProvider provider, String model, String instructions, String input, Consumer<String> delta) {
		JSONArray messages = new JSONArray();
		messages.add(new JSONObject().set("role", "system").set("content", instructions));
		messages.add(new JSONObject().set("role", "user").set("content", input));
		JSONObject body = new JSONObject().set("model", model).set("messages", messages)
			.set("stream", true).set("max_tokens", SCENE_TOKENS);
		// GLM-5.3 always reasons. Its total output allowance includes reasoning and visible code.
		if (StringUtils.equalsAnyIgnoreCase(model, "glm-5.3", "glm-5.3-flash", "glm-5.3-flashx")) {
			body.set("max_tokens", GLM_SCENE_TOKENS).set("reasoning_effort", "low");
		} else if (StringUtils.equalsAnyIgnoreCase(model, "glm-4.6", "glm-4.7", "glm-5", "glm-5.1", "glm-5.2")) {
			body.set("max_tokens", GLM_SCENE_TOKENS).set("thinking", new JSONObject().set("type", "disabled"));
		}
		String buffered;
		try {
			buffered = complete(provider, body.toString(), "视图生成", delta);
		} catch (ServiceException rejected) {
			// A rich 3D page needs more room than 8K tokens, but some upstream models cap the request lower.
			// HTTP 400 arrives before any generation, so one smaller retry costs no quota.
			if (!Integer.valueOf(400).equals(rejected.getCode()) || body.getInt("max_tokens") <= SAFE_SCENE_TOKENS) throw rejected;
			body.set("max_tokens", SAFE_SCENE_TOKENS);
			buffered = complete(provider, body.toString(), "视图生成", delta);
		}
		if (buffered != null) delta.accept(buffered);
	}

	private String complete(LlmProvider provider, String payload, String operation, Consumer<String> delta) {
		HttpURLConnection connection = null;
		int timeoutMillis = Math.max(5, Math.min(defaultInt(provider.getTimeoutSeconds(), 120), 600)) * 1000;
		try {
			String apiKey = resolveApiKey(provider);
			if (StringUtils.isBlank(apiKey)) throw new ServiceException("大模型 API Key 未配置");
			byte[] body = payload.getBytes(StandardCharsets.UTF_8);
			connection = (HttpURLConnection) new URL(completionsUrl(provider.getBaseUrl())).openConnection();
			connection.setInstanceFollowRedirects(false);
			connection.setConnectTimeout(timeoutMillis);
			connection.setReadTimeout(timeoutMillis);
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			// Streaming mode keeps HttpURLConnection from replaying a paid POST.
			connection.setFixedLengthStreamingMode(body.length);
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Authorization", "Bearer " + apiKey);
			try (OutputStream output = connection.getOutputStream()) {
				output.write(body);
			}
			int status = connection.getResponseCode();
			if (status < 200 || status >= 300) {
				if (delta != null && status == 429) {
					throw new ServiceException("OortCloud 返回 HTTP 429，请稍后重试或在视频广场切换模型", status);
				}
				String message = operation + "模型接口返回 HTTP " + status;
				if (status >= 500) message += "，上游执行情况未知";
				throw new ServiceException(message, status);
			}
			if (delta != null && StringUtils.containsIgnoreCase(connection.getContentType(), "text/event-stream")) {
				readSceneStream(connection.getInputStream(), delta);
				return null;
			}
			JSONArray choices = JSONUtil.parseObj(readModelResponse(connection.getInputStream())).getJSONArray("choices");
			if (choices == null || choices.size() != 1) throw new ServiceException(operation + "模型响应缺少单条结果");
			JSONObject choice = choices.getJSONObject(0);
			String finishReason = choice.getStr("finish_reason");
			if (delta != null) {
				checkSceneFinishReason(finishReason);
			} else if ("length".equals(finishReason) || "content_filter".equals(finishReason)) {
				throw new ServiceException(operation + "模型输出被截断或过滤");
			}
			Object content = choice.getByPath("message.content");
			if (!(content instanceof String) || StringUtils.isBlank((String) content)
				|| ((String) content).length() > MAX_ANNOTATION_CONTENT_CHARS) {
				throw new ServiceException(operation + "模型未返回有效文本或输出超过处理限制");
			}
			return (String) content;
		} catch (ServiceException | UncheckedIOException | VideoSquareScenePatch.Mismatch exception) {
			// A closed preview connection or an unusable edit cancels upstream work without another response.
			throw exception;
		} catch (SocketTimeoutException exception) {
			throw new ServiceException(operation + "模型请求超时，上游执行情况未知");
		} catch (Exception exception) {
			throw new ServiceException(operation + "模型通信失败或返回格式无效，上游执行情况未知");
		} finally {
			if (connection != null) connection.disconnect();
		}
	}

	private void readSceneStream(InputStream stream, Consumer<String> delta) throws IOException {
		boolean stopped = false;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(
			new BoundedInputStream(stream, MAX_SCENE_STREAM_BYTES), StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (!line.startsWith("data:")) continue;
				String data = line.substring(5).trim();
				if ("[DONE]".equals(data)) return;
				if (data.isEmpty()) continue;
				JsonNodeChunk chunk = new JsonNodeChunk(JSONUtil.parseObj(data));
				if (chunk.text != null) delta.accept(chunk.text);
				if (chunk.stopped) stopped = true;
			}
		}
		if (!stopped) throw new ServiceException("视图生成数据流中断，请重新生成");
	}

	private static void checkSceneFinishReason(String reason) {
		if (StringUtils.isBlank(reason) || "null".equals(reason) || "stop".equals(reason)) return;
		switch (reason) {
			case "length":
				throw new ServiceException("页面尚未生成完整，模型已达到输出长度上限。请分步描述需求或选择支持较长输出的模型", 422);
			case "content_filter":
			case "sensitive":
				throw new ServiceException("模型服务已过滤本次生成内容，请调整描述后重试", 422);
			case "model_context_window_exceeded":
				throw new ServiceException("当前页面与描述超出模型上下文长度，请精简描述或切换到长上下文模型", 422);
			case "network_error":
				throw new ServiceException("模型服务的连接中断，请稍后重试", 502);
			default:
				throw new ServiceException("模型提前结束，未返回完整页面，请重新生成或切换模型", 422);
		}
	}

	private static class JsonNodeChunk {
		private String text;
		private boolean stopped;

		JsonNodeChunk(JSONObject data) {
			JSONArray choices = data.getJSONArray("choices");
			if (data.containsKey("error") || choices == null) throw new ServiceException("视图生成模型未能完成请求");
			if (choices.isEmpty()) return; // optional usage-only chunk
			JSONObject choice = choices.getJSONObject(0);
			String reason = choice.getStr("finish_reason");
			checkSceneFinishReason(reason);
			stopped = "stop".equals(reason);
			Object content = choice.getByPath("delta.content");
			if (content instanceof String) text = (String) content;
		}
	}

	private String readModelResponse(InputStream stream) throws IOException {
		try (InputStream input = stream; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[8192];
			int count;
			while ((count = input.read(buffer)) != -1) {
				if (output.size() + count > MAX_ANNOTATION_RESPONSE_BYTES) {
					throw new ServiceException("模型响应超过处理限制");
				}
				output.write(buffer, 0, count);
			}
			String response = new String(output.toByteArray(), StandardCharsets.UTF_8);
			try (JsonParser parser = ANNOTATION_JSON.createParser(response)) {
				if (parser.nextToken() != JsonToken.START_OBJECT) throw new ServiceException("模型响应格式无效");
				int depth = 1;
				JsonToken token;
				while ((token = parser.nextToken()) != null) {
					if (depth == 0) throw new ServiceException("模型响应格式无效");
					if (token.isStructStart() && ++depth > 64) throw new ServiceException("模型响应的嵌套层级超过限制");
					if (token.isStructEnd()) depth--;
				}
			}
			return response;
		}
	}

	private String annotationMimeType(byte[] image) {
		String detected = imageMimeType(image);
		if (!"image/jpeg".equals(detected)) return detected;
		if (image.length >= 3 && (image[0] & 0xff) == 0xff && (image[1] & 0xff) == 0xd8 && (image[2] & 0xff) == 0xff) {
			return detected;
		}
		throw new ServiceException("智能标注仅支持 JPEG、PNG 或 WebP 图片");
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
			String apiKey = resolveApiKey(provider);
			if (StringUtils.isBlank(apiKey)) {
				throw new ServiceException("大模型 API Key 未配置");
			}
			connection.setRequestProperty("Authorization", "Bearer " + apiKey);
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

	String resolveApiKey(LlmProvider provider) {
		return StringUtils.isBlank(provider.getApiKeyCiphertext())
			? null : apiKeyCipher.decrypt(provider.getApiKeyCiphertext());
	}

	String requestBody(String model, String prompt, List<byte[]> images) {
		return requestBody(model, prompt, images, null);
	}

	private String requestBody(String model, String prompt, List<byte[]> images, String originalImageMimeType) {
		JSONArray content = new JSONArray();
		JSONObject text = new JSONObject();
		text.set("type", "text");
		text.set("text", prompt);
		content.add(text);
		for (byte[] image : images == null ? new ArrayList<byte[]>() : images) {
			EncodedImage encodedImage = originalImageMimeType == null ? encodeImage(image) : new EncodedImage(originalImageMimeType, image);
			JSONObject imageUrl = new JSONObject();
			imageUrl.set("url", "data:" + encodedImage.getMimeType() + ";base64,"
				+ Base64.getEncoder().encodeToString(encodedImage.getBytes()));
			imageUrl.set("detail", originalImageMimeType == null ? "low" : "high");
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
		body.set("stream", false);
		if (originalImageMimeType != null) body.set("max_tokens", 16384);
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

	VisionDecision parseTestResponse(String raw) {
		try {
			return parseDecision(raw);
		} catch (ServiceException ignored) {
			VisionDecision result = new VisionDecision();
			result.setDecision("CONNECTED");
			result.setReason(abbreviate(raw, 1000));
			result.setRawResponse(abbreviate(raw, 60000));
			return result;
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
