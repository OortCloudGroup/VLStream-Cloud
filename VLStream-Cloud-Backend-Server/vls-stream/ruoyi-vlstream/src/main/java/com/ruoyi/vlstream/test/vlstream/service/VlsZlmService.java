package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.vlstream.test.vlstream.config.VlsZlmProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Minimal ZLMediaKit REST client for on-demand RTSP/RTMP to WebRTC preview. */
@Service
@RequiredArgsConstructor
public class VlsZlmService {

	private final VlsZlmProperties properties;

	public Map<String, Object> createProxy(Long streamId, String sourceUrl) {
		return createProxy("mqtt_" + streamId, sourceUrl);
	}

	/** Creates an isolated on-demand proxy for callers outside the MQTT stream table. */
	public Map<String, Object> createProxy(String streamKey, String sourceUrl) {
		assertConfigured();
		String app = StringUtils.defaultIfBlank(properties.getApp(), "vlstream");
		String stream = normalizeStreamKey(streamKey);
		Map<String, String> mediaParams = baseParams();
		mediaParams.put("schema", "rtsp");
		mediaParams.put("vhost", "__defaultVhost__");
		mediaParams.put("app", app);
		mediaParams.put("stream", stream);
		try {
			JSONObject mediaInfo = get("/index/api/getMediaInfo", mediaParams);
			if (mediaInfo.getInt("code", -1) == 0) {
				return previewResult(app, stream, null);
			}
		} catch (RuntimeException ignored) {
			// addStreamProxy below remains the authoritative operation and error.
		}
		Map<String, String> params = baseParams();
		params.put("vhost", "__defaultVhost__");
		params.put("app", app);
		params.put("stream", stream);
		params.put("url", sourceUrl);
		params.put("enable_audio", "1");
		params.put("enable_rtsp", "1");
		params.put("enable_rtmp", "0");
		params.put("enable_hls", "0");
		params.put("enable_mp4", "0");
		params.put("rtp_type", "0");
		params.put("timeout_sec", String.valueOf(properties.getProxyTimeoutSeconds()));
		params.put("auto_close", "1");
		JSONObject response = get("/index/api/addStreamProxy", params);
		assertSuccess(response, "ZLM创建拉流代理失败");
		JSONObject data = response.getJSONObject("data");
		String proxyKey = data == null ? null : data.getStr("key");
		return previewResult(app, stream, proxyKey);
	}

	private String normalizeStreamKey(String streamKey) {
		String normalized = StringUtils.trimToEmpty(streamKey).replaceAll("[^A-Za-z0-9_-]", "_");
		if (StringUtils.isBlank(normalized)) {
			throw new IllegalArgumentException("ZLM流标识不能为空");
		}
		return normalized;
	}

	private Map<String, Object> previewResult(String app, String stream, String proxyKey) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("app", app);
		result.put("stream", stream);
		result.put("proxyKey", proxyKey);
		result.put("webrtcUrl", normalize(properties.getPublicBaseUrl())
			+ "/index/api/webrtc?app=" + encode(app) + "&stream=" + encode(stream) + "&type=play");
		return result;
	}

	public Map<String, Object> status() {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("enabled", properties.isEnabled());
		if (!properties.isEnabled()) {
			result.put("available", false);
			result.put("message", "ZLMediaKit未启用");
			return result;
		}
		try {
			JSONObject response = get("/index/api/getServerConfig", baseParams());
			boolean available = response.getInt("code", -1) == 0;
			result.put("available", available);
			result.put("message", available ? "ZLMediaKit可用" : response.getStr("msg"));
		} catch (RuntimeException ex) {
			result.put("available", false);
			result.put("message", ex.getMessage());
		}
		return result;
	}

	private JSONObject get(String path, Map<String, String> params) {
		HttpURLConnection connection = null;
		try {
			StringBuilder query = new StringBuilder();
			for (Map.Entry<String, String> entry : params.entrySet()) {
				if (query.length() > 0) query.append('&');
				query.append(encode(entry.getKey())).append('=').append(encode(entry.getValue()));
			}
			connection = (HttpURLConnection) new URL(normalize(properties.getInternalBaseUrl()) + path + "?" + query).openConnection();
			connection.setConnectTimeout(properties.getConnectTimeoutMillis());
			connection.setReadTimeout(properties.getReadTimeoutMillis());
			connection.setRequestMethod("GET");
			int status = connection.getResponseCode();
			InputStream input = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
			StringBuilder body = new StringBuilder();
			if (input != null) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
					String line;
					while ((line = reader.readLine()) != null) body.append(line);
				}
			}
			if (status < 200 || status >= 300) {
				throw new IllegalStateException("ZLM HTTP " + status + ": " + body);
			}
			return JSONUtil.parseObj(body.toString());
		} catch (Exception ex) {
			throw new IllegalStateException("无法访问ZLMediaKit: " + ex.getMessage(), ex);
		} finally {
			if (connection != null) connection.disconnect();
		}
	}

	private void assertConfigured() {
		if (!properties.isEnabled()) throw new IllegalStateException("ZLMediaKit未启用");
		if (StringUtils.isBlank(properties.getSecret())) throw new IllegalStateException("vlstream.zlm.secret未配置");
	}

	private void assertSuccess(JSONObject response, String prefix) {
		if (response.getInt("code", -1) != 0) {
			throw new IllegalStateException(prefix + ": " + StringUtils.defaultIfBlank(response.getStr("msg"), response.toString()));
		}
	}

	private Map<String, String> baseParams() {
		assertConfigured();
		Map<String, String> params = new LinkedHashMap<>();
		params.put("secret", properties.getSecret());
		return params;
	}

	private String normalize(String value) {
		return StringUtils.removeEnd(StringUtils.trimToEmpty(value), "/");
	}

	private String encode(String value) {
		try {
			return URLEncoder.encode(StringUtils.defaultString(value), "UTF-8");
		} catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}
}
