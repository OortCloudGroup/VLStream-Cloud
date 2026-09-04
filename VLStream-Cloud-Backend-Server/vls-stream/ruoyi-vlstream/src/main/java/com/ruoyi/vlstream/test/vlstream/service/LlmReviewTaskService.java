/* SPDX-License-Identifier: MIT */
package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.vlstream.test.vlstream.config.VlsLlmReviewProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.AlgorithmLlmReviewConfigMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.LlmProviderMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.LlmReviewTaskMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.ActiveSafetyEventReport;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.ActiveSafetyEventReportResult;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmLlmReviewConfig;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceMediaUpload;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.LlmProvider;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.LlmReviewTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Creates, processes, lists, and manually resolves LLM review tasks. */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmReviewTaskService {
	static final String PROVIDER_TEST_PROMPT = "这是视觉大模型连通性测试。请识别这一张图片，只返回JSON："
		+ "{\"decision\":\"confirmed|rejected|uncertain\",\"confidence\":0.0,"
		+ "\"reason\":\"简短说明识别到的画面内容或无法识别的原因\"}";

	private final VlsLlmReviewProperties properties;
	private final AlgorithmLlmReviewConfigMapper configMapper;
	private final LlmProviderMapper providerMapper;
	private final LlmSystemProviderService systemProviderService;
	private final LlmReviewTaskMapper taskMapper;
	private final DeviceMediaUploadService mediaUploadService;
	private final OpenAiVisionClient visionClient;
	private final ActiveSafetyEventReportService activeSafetyEventReportService;

	/** Return true when the event was routed into the optional review gate. */
	@Transactional(rollbackFor = Exception.class)
	public boolean enqueueIfRequired(JSONObject envelope, DeviceInfo device, DeviceMediaUpload upload,
									 String eventId, String eventType, String description, Date eventTime) {
		if (!Boolean.TRUE.equals(properties.getEnabled())
			|| !VlsMqttProtocol.STRUCT_EVENT.equals(envelope.getStr("subBizType"))) {
			return false;
		}
		JSONObject mqttPayload = envelope.getJSONObject("payload");
		String rawAlgorithmId = mqttPayload == null ? null : mqttPayload.getStr("algorithmId");
		Long algorithmId = parseLong(rawAlgorithmId);
		if (algorithmId == null) {
			log.debug("struct 事件未携带有效 algorithmId，保持原处理链路: messageId={}", envelope.getStr("messageId"));
			return false;
		}
		AlgorithmLlmReviewConfig config = configMapper.selectOne(
			new LambdaQueryWrapper<AlgorithmLlmReviewConfig>()
				.eq(AlgorithmLlmReviewConfig::getAlgorithmId, algorithmId)
				.eq(AlgorithmLlmReviewConfig::getEnabled, true)
				.last("limit 1"));
		if (config == null) {
			return false;
		}
		LlmProvider provider = providerForUse(config.getProviderId(), false);
		if (provider == null) {
			log.warn("算法选择的大模型当前不可用，事件保持原安全事件链路: tenantId={}, algorithmId={}, providerId={}, messageId={}",
				device.getTenantId(), algorithmId, config.getProviderId(), envelope.getStr("messageId"));
			return false;
		}

		JSONObject event = new JSONObject();
		event.set("sourceMessageId", envelope.getStr("messageId"));
		event.set("deviceEventId", eventId);
		event.set("deviceId", device.getDeviceId());
		event.set("deviceName", device.getDeviceName());
		event.set("deviceTag", device.getTag());
		event.set("eventType", eventType);
		event.set("description", description);
		event.set("eventTime", eventTime.getTime());
		event.set("mediaId", upload.getMediaId());
		event.set("address", device.getAddress());
		event.set("longitude", device.getLongitude());
		event.set("latitude", device.getLatitude());
		event.set("mqttPayload", mqttPayload);

		JSONObject snapshot = new JSONObject();
		snapshot.set("promptTemplate", config.getPromptTemplate());
		snapshot.set("decisionThreshold", config.getDecisionThreshold());
		snapshot.set("maxRetries", config.getMaxRetries());
		snapshot.set("failureStrategy", config.getFailureStrategy());
		snapshot.set("imageMode", config.getImageMode());

		Date now = new Date();
		LlmReviewTask task = new LlmReviewTask();
		task.setId(IdUtil.getSnowflakeNextId());
		task.setTenantId(device.getTenantId());
		task.setSourceMessageId(envelope.getStr("messageId"));
		task.setDeviceEventId(eventId);
		task.setDeviceId(device.getDeviceId());
		task.setAlgorithmId(algorithmId);
		task.setProviderId(provider.getId());
		task.setMediaId(upload.getMediaId());
		task.setEventPayloadJson(event.toString());
		task.setConfigSnapshotJson(snapshot.toString());
		task.setReviewStatus("PENDING");
		task.setAttemptCount(0);
		task.setNextRetryTime(now);
		task.setCreateTime(now);
		task.setUpdateTime(now);
		taskMapper.insertIgnore(task);
		return true;
	}

	public IPage<LlmReviewTask> page(int current, int size, String status, Long algorithmId) {
		return taskMapper.selectPage(new Page<LlmReviewTask>(Math.max(1, current), Math.max(1, Math.min(size, 100))),
			new LambdaQueryWrapper<LlmReviewTask>()
				.eq(StringUtils.isNotBlank(status), LlmReviewTask::getReviewStatus, status)
				.eq(algorithmId != null, LlmReviewTask::getAlgorithmId, algorithmId)
				.orderByDesc(LlmReviewTask::getCreateTime));
	}

	public LlmReviewTask detail(Long id) {
		LlmReviewTask task = taskMapper.selectById(id);
		if (task == null) {
			throw new ServiceException("复核任务不存在");
		}
		return task;
	}

	public OpenAiVisionClient.VisionDecision testProvider(Long providerId, String prompt, byte[] image) {
		LlmProvider provider = providerForUse(providerId, true);
		List<byte[]> images = new ArrayList<byte[]>();
		images.add(image);
		return visionClient.test(provider, StringUtils.defaultIfBlank(prompt,
			PROVIDER_TEST_PROMPT), images);
	}

	public void processClaimed(LlmReviewTask task, String workerId) {
		int attempt = defaultInt(task.getAttemptCount(), 0) + 1;
		Date now = new Date();
		try {
			LlmProvider provider = providerForUse(task.getProviderId(), true);
			JSONObject event = JSONUtil.parseObj(task.getEventPayloadJson());
			JSONObject config = JSONUtil.parseObj(task.getConfigSnapshotJson());
			byte[] fullImage = mediaUploadService.readBoundImage(task.getMediaId(),
				defaultInt(properties.getMaxImageBytes(), 10485760));
			List<byte[]> images = selectImages(fullImage, event.getJSONObject("mqttPayload"),
				config.getStr("imageMode"));
			String prompt = renderPrompt(config.getStr("promptTemplate"), event);
			OpenAiVisionClient.VisionDecision decision = visionClient.review(provider, prompt, images);
			BigDecimal threshold = config.getBigDecimal("decisionThreshold", new BigDecimal("0.8000"));
			String status = terminalStatus(decision, threshold);
			String formalEventId = null;
			if ("CONFIRMED".equals(status)) {
				formalEventId = persistFormalEvent(task).getActiveSafetyEventId();
			}
			finish(task, workerId, status, attempt, now, decision.getDecision(), decision.getConfidence(),
				decision.getReason(), decision.getRawResponse(), null, formalEventId, now);
		} catch (Exception exception) {
			JSONObject config = safeJson(task.getConfigSnapshotJson());
			int maxRetries = config.getInt("maxRetries", 2);
			String status = attempt <= maxRetries ? "RETRY"
				: StringUtils.defaultIfBlank(config.getStr("failureStrategy"), "MANUAL_REVIEW");
			Date retryAt = new Date(now.getTime() + retryDelay(attempt));
			finish(task, workerId, status, attempt, retryAt, null, null, null, null,
				StringUtils.abbreviate(exception.getMessage(), 2000), null, now);
			log.warn("大模型事件复核失败: taskId={}, tenantId={}, attempt={}, status={}, reason={}",
				task.getId(), task.getTenantId(), attempt, status, exception.getMessage());
		}
	}

	@Transactional(rollbackFor = Exception.class)
	public LlmReviewTask manualDecision(Long id, String decision) {
		LlmReviewTask task = detail(id);
		String normalized = StringUtils.upperCase(StringUtils.trimToEmpty(decision));
		if (!"CONFIRMED".equals(normalized) && !"REJECTED".equals(normalized)) {
			throw new ServiceException("人工复核只能选择 CONFIRMED 或 REJECTED");
		}
		String formalEventId = task.getFormalEventId();
		if ("CONFIRMED".equals(normalized) && StringUtils.isBlank(formalEventId)) {
			formalEventId = persistFormalEvent(task).getActiveSafetyEventId();
		}
		task.setReviewStatus(normalized);
		task.setDecision(normalized);
		task.setReason("人工复核：" + ("CONFIRMED".equals(normalized) ? "确认事件" : "确认误报"));
		task.setFormalEventId(formalEventId);
		task.setReviewedBy(currentUserId());
		task.setReviewedAt(new Date());
		task.setLockedBy(null);
		task.setLockedAt(null);
		task.setUpdateTime(new Date());
		taskMapper.updateById(task);
		return task;
	}

	private ActiveSafetyEventReportResult persistFormalEvent(LlmReviewTask task) {
		JSONObject event = JSONUtil.parseObj(task.getEventPayloadJson());
		return activeSafetyEventReportService.report(ActiveSafetyEventReport.builder()
			.sourceMessageId(event.getStr("sourceMessageId"))
			.deviceEventId(event.getStr("deviceEventId"))
			.deviceId(event.getStr("deviceId"))
			.deviceName(event.getStr("deviceName"))
			.deviceTag(event.getStr("deviceTag"))
			.eventType(event.getStr("eventType"))
			.description(event.getStr("description"))
			.eventTime(new Date(event.getLong("eventTime")))
			.mediaId(event.getStr("mediaId"))
			.address(event.getStr("address"))
			.longitude(event.getBigDecimal("longitude"))
			.latitude(event.getBigDecimal("latitude"))
			.build());
	}

	private List<byte[]> selectImages(byte[] full, JSONObject payload, String mode) throws Exception {
		List<byte[]> images = new ArrayList<byte[]>();
		if (!"CROP".equals(mode)) {
			images.add(full);
		}
		if (!"FULL".equals(mode)) {
			byte[] crop = crop(full, payload);
			if (crop != null) {
				images.add(crop);
			} else if (images.isEmpty()) {
				images.add(full);
			}
		}
		return images;
	}

	private byte[] crop(byte[] image, JSONObject payload) throws Exception {
		if (payload == null || !payload.containsKey("left") || !payload.containsKey("top")
			|| !payload.containsKey("right") || !payload.containsKey("bottom")) {
			return null;
		}
		BufferedImage source = ImageIO.read(new ByteArrayInputStream(image));
		if (source == null) {
			return null;
		}
		int left = Math.max(0, payload.getInt("left", 0));
		int top = Math.max(0, payload.getInt("top", 0));
		int right = Math.min(source.getWidth(), payload.getInt("right", source.getWidth()));
		int bottom = Math.min(source.getHeight(), payload.getInt("bottom", source.getHeight()));
		if (right <= left || bottom <= top) {
			return null;
		}
		BufferedImage rgb = new BufferedImage(right - left, bottom - top, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = rgb.createGraphics();
		try {
			graphics.drawImage(source.getSubimage(left, top, right - left, bottom - top), 0, 0, null);
		} finally {
			graphics.dispose();
		}
		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			ImageIO.write(rgb, "jpg", output);
			return output.toByteArray();
		}
	}

	private String renderPrompt(String template, JSONObject event) {
		JSONObject payload = event.getJSONObject("mqttPayload");
		return StringUtils.defaultIfBlank(template, LlmReviewManagementService.DEFAULT_PROMPT)
			.replace("{{eventType}}", StringUtils.defaultString(event.getStr("eventType")))
			.replace("{{objectType}}", payload == null ? "" : StringUtils.defaultString(payload.getStr("type")))
			.replace("{{yoloConfidence}}", payload == null ? "" : StringUtils.defaultString(payload.getStr("worth")))
			.replace("{{deviceName}}", StringUtils.defaultString(event.getStr("deviceName")));
	}

	private String terminalStatus(OpenAiVisionClient.VisionDecision decision, BigDecimal threshold) {
		if (decision.getConfidence().compareTo(threshold) < 0 || "UNCERTAIN".equals(decision.getDecision())) {
			return "MANUAL_REVIEW";
		}
		return "CONFIRMED".equals(decision.getDecision()) ? "CONFIRMED" : "REJECTED";
	}

	private void finish(LlmReviewTask task, String workerId, String status, int attempt, Date nextRetry,
						String decision, BigDecimal confidence, String reason, String rawResponse,
						String error, String formalEventId, Date now) {
		if (taskMapper.finishAttempt(task.getId(), task.getTenantId(), workerId, status, attempt,
			nextRetry, decision, confidence, reason, rawResponse, error, formalEventId, now) != 1) {
			throw new ServiceException("复核任务状态已被其他工作节点更新");
		}
	}

	private long retryDelay(int attempt) {
		long first = properties.getFirstRetryMillis() == null ? 10000L : properties.getFirstRetryMillis();
		long max = properties.getMaxRetryMillis() == null ? 60000L : properties.getMaxRetryMillis();
		return Math.min(max, first * (1L << Math.max(0, Math.min(attempt - 1, 10))));
	}

	private JSONObject safeJson(String value) {
		try {
			return JSONUtil.parseObj(value);
		} catch (Exception ignored) {
			return new JSONObject();
		}
	}

	private Long parseLong(String value) {
		try {
			return StringUtils.isBlank(value) ? null : Long.valueOf(value);
		} catch (NumberFormatException ignored) {
			return null;
		}
	}

	private LlmProvider providerForUse(Long providerId, boolean failWhenUnavailable) {
		LlmProvider provider = providerId == null ? null : providerMapper.selectById(providerId);
		String reason = null;
		if (provider == null) {
			reason = "大模型配置不存在";
		} else if (!Boolean.TRUE.equals(provider.getEnabled())) {
			reason = "大模型配置已禁用";
		} else if (StringUtils.isBlank(provider.getApiKeyCiphertext())) {
			reason = "大模型 API Key 未配置";
		} else if (systemProviderService.isSystemProvider(provider)
			&& !systemProviderService.isAuthorized(provider)) {
			reason = "请先点击页面顶部“登录 OortCloud”完成授权";
		}
		if (reason != null && failWhenUnavailable) {
			throw new ServiceException(reason);
		}
		return reason == null ? provider : null;
	}

	private int defaultInt(Integer value, int fallback) {
		return value == null ? fallback : value;
	}

	private String currentUserId() {
		try {
			return LoginHelper.getUserId();
		} catch (Exception ignored) {
			return "system";
		}
	}
}
