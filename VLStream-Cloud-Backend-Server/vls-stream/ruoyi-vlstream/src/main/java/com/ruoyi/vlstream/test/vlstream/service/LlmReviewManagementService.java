/* SPDX-License-Identifier: MIT */
package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.mapper.AlgorithmLlmReviewConfigMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.LlmProviderMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmLlmReviewConfig;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.LlmProvider;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/** CRUD and validation boundary for provider and algorithm review settings. */
@Service
@RequiredArgsConstructor
public class LlmReviewManagementService {

	public static final String DEFAULT_PROMPT = "你是视频安全事件复核器。设备端YOLO报告事件类型：{{eventType}}，"
		+ "检测对象：{{objectType}}，YOLO置信度：{{yoloConfidence}}，设备：{{deviceName}}。"
		+ "请结合完整场景图和目标裁剪图判断该告警是否真实。只返回JSON："
		+ "{\"decision\":\"confirmed|rejected|uncertain\",\"confidence\":0.0,\"reason\":\"简短依据\"}";

	private final LlmProviderMapper providerMapper;
	private final AlgorithmLlmReviewConfigMapper configMapper;
	private final VlsAlgorithmMapper algorithmMapper;
	private final LlmApiKeyCipher apiKeyCipher;
	private final LlmSystemProviderService systemProviderService;

	public List<LlmProvider> listProviders() {
		systemProviderService.getOrCreate();
		List<LlmProvider> providers = providerMapper.selectList(new LambdaQueryWrapper<LlmProvider>()
			.orderByDesc(LlmProvider::getCreateTime));
		for (LlmProvider provider : providers) {
			mask(provider);
		}
		providers.sort((left, right) -> Boolean.compare(Boolean.TRUE.equals(right.getSystemProvider()),
			Boolean.TRUE.equals(left.getSystemProvider())));
		return providers;
	}

	public LlmProvider getProvider(Long id) {
		LlmProvider provider = providerMapper.selectById(id);
		if (provider == null) {
			throw new ServiceException("大模型配置不存在");
		}
		return provider;
	}

	@Transactional(rollbackFor = Exception.class)
	public LlmProvider saveProvider(LlmProvider input) {
		validateProvider(input);
		if (input.getId() == null) {
			if (LlmSystemProviderService.SYSTEM_PROVIDER_NAME.equalsIgnoreCase(input.getName())) {
				throw new ServiceException("OortCloud 是系统内置大模型名称，请使用其他名称");
			}
			if (StringUtils.isBlank(input.getApiKey())) {
				throw new ServiceException("API Key 不能为空");
			}
			input.setApiKeyCiphertext(apiKeyCipher.encrypt(input.getApiKey()));
			input.setStatus(1);
			input.setIsDeleted(0);
			providerMapper.insert(input);
		} else {
			LlmProvider existing = getProvider(input.getId());
			if (systemProviderService.isSystemProvider(existing)) {
				throw new ServiceException("内置 OortCloud 大模型不能编辑");
			}
			existing.setName(input.getName());
			existing.setBaseUrl(input.getBaseUrl());
			existing.setModelName(input.getModelName());
			existing.setTimeoutSeconds(input.getTimeoutSeconds());
			existing.setEnabled(input.getEnabled());
			if (StringUtils.isNotBlank(input.getApiKey())) {
				existing.setApiKeyCiphertext(apiKeyCipher.encrypt(input.getApiKey()));
			}
			providerMapper.updateById(existing);
			input = existing;
		}
		mask(input);
		return input;
	}

	@Transactional(rollbackFor = Exception.class)
	public void deleteProvider(Long id) {
		LlmProvider provider = getProvider(id);
		if (systemProviderService.isSystemProvider(provider)) {
			throw new ServiceException("内置 OortCloud 大模型不能删除");
		}
		Long references = configMapper.selectCount(new LambdaQueryWrapper<AlgorithmLlmReviewConfig>()
			.eq(AlgorithmLlmReviewConfig::getProviderId, id)
			.eq(AlgorithmLlmReviewConfig::getEnabled, true));
		if (references != null && references > 0) {
			throw new ServiceException("该大模型仍被已启用的算法复核配置引用");
		}
		providerMapper.deleteById(id);
	}

	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> authorizeOortCloud(String platformUserId, String platformUserName,
										 String apiKey) {
		return authorizationStatus(systemProviderService.authorize(platformUserId, platformUserName, apiKey));
	}

	public Map<String, Object> getOortCloudAuthorization() {
		return authorizationStatus(systemProviderService.getOrCreate());
	}

	public AlgorithmLlmReviewConfig getAlgorithmConfig(Long algorithmId) {
		AlgorithmLlmReviewConfig config = configMapper.selectOne(
			new LambdaQueryWrapper<AlgorithmLlmReviewConfig>()
				.eq(AlgorithmLlmReviewConfig::getAlgorithmId, algorithmId)
				.last("limit 1"));
		if (config != null) {
			return config;
		}
		AlgorithmLlmReviewConfig defaults = new AlgorithmLlmReviewConfig();
		defaults.setAlgorithmId(algorithmId);
		defaults.setProviderId(systemProviderService.getOrCreate().getId());
		defaults.setEnabled(false);
		defaults.setPromptTemplate(DEFAULT_PROMPT);
		defaults.setDecisionThreshold(new BigDecimal("0.8000"));
		defaults.setMaxRetries(2);
		defaults.setFailureStrategy("MANUAL_REVIEW");
		defaults.setImageMode("FULL_AND_CROP");
		return defaults;
	}

	@Transactional(rollbackFor = Exception.class)
	public AlgorithmLlmReviewConfig saveAlgorithmConfig(Long algorithmId,
													 AlgorithmLlmReviewConfig input) {
		if (algorithmMapper.selectById(algorithmId) == null) {
			throw new ServiceException("算法不存在");
		}
		input.setAlgorithmId(algorithmId);
		applyConfigDefaults(input);
		validateAlgorithmConfig(input);
		AlgorithmLlmReviewConfig existing = configMapper.selectOne(
			new LambdaQueryWrapper<AlgorithmLlmReviewConfig>()
				.eq(AlgorithmLlmReviewConfig::getAlgorithmId, algorithmId)
				.last("limit 1"));
		if (existing == null) {
			input.setStatus(1);
			input.setIsDeleted(0);
			configMapper.insert(input);
			return input;
		}
		existing.setProviderId(input.getProviderId());
		existing.setEnabled(input.getEnabled());
		existing.setPromptTemplate(input.getPromptTemplate());
		existing.setDecisionThreshold(input.getDecisionThreshold());
		existing.setMaxRetries(input.getMaxRetries());
		existing.setFailureStrategy(input.getFailureStrategy());
		existing.setImageMode(input.getImageMode());
		configMapper.updateById(existing);
		return existing;
	}

	private void applyConfigDefaults(AlgorithmLlmReviewConfig config) {
		config.setEnabled(Boolean.TRUE.equals(config.getEnabled()));
		config.setPromptTemplate(StringUtils.defaultIfBlank(config.getPromptTemplate(), DEFAULT_PROMPT));
		config.setDecisionThreshold(config.getDecisionThreshold() == null
			? new BigDecimal("0.8000") : config.getDecisionThreshold());
		config.setMaxRetries(clamp(config.getMaxRetries(), 0, 5, 2));
		config.setFailureStrategy(StringUtils.defaultIfBlank(config.getFailureStrategy(), "MANUAL_REVIEW"));
		config.setImageMode(StringUtils.defaultIfBlank(config.getImageMode(), "FULL_AND_CROP"));
	}

	private void validateProvider(LlmProvider provider) {
		if (provider == null || StringUtils.isAnyBlank(provider.getName(), provider.getBaseUrl(), provider.getModelName())) {
			throw new ServiceException("名称、API 地址和模型名称不能为空");
		}
		if (!StringUtils.startsWithAny(provider.getBaseUrl(), "http://", "https://")) {
			throw new ServiceException("API 地址必须以 http:// 或 https:// 开头");
		}
		provider.setTimeoutSeconds(clamp(provider.getTimeoutSeconds(), 5, 600, 120));
		provider.setEnabled(provider.getEnabled() == null || provider.getEnabled());
	}

	private void validateAlgorithmConfig(AlgorithmLlmReviewConfig config) {
		if (config.getDecisionThreshold().compareTo(BigDecimal.ZERO) < 0
			|| config.getDecisionThreshold().compareTo(BigDecimal.ONE) > 0) {
			throw new ServiceException("自动判定阈值必须在 0 到 1 之间");
		}
		if (!"FULL".equals(config.getImageMode()) && !"CROP".equals(config.getImageMode())
			&& !"FULL_AND_CROP".equals(config.getImageMode())) {
			throw new ServiceException("图片模式不合法");
		}
		if (!"MANUAL_REVIEW".equals(config.getFailureStrategy())) {
			throw new ServiceException("当前仅支持失败后转人工复核");
		}
		if (Boolean.TRUE.equals(config.getEnabled())) {
			if (config.getProviderId() == null || StringUtils.isBlank(config.getPromptTemplate())) {
				throw new ServiceException("开启复核时必须选择大模型并填写提示词");
			}
			LlmProvider provider = getProvider(config.getProviderId());
			if (!Boolean.TRUE.equals(provider.getEnabled())) {
				throw new ServiceException("选择的大模型已禁用");
			}
			if (StringUtils.isBlank(provider.getApiKeyCiphertext())) {
				throw new ServiceException("选择的大模型 API Key 未配置");
			}
			if (systemProviderService.isSystemProvider(provider)
				&& !systemProviderService.isAuthorized(provider)) {
				throw new ServiceException("请先点击页面顶部“登录 OortCloud”完成授权");
			}
		}
	}

	private void mask(LlmProvider provider) {
		provider.setApiKey(null);
		provider.setApiKeyConfigured(StringUtils.isNotBlank(provider.getApiKeyCiphertext()));
		provider.setSystemProvider(systemProviderService.isSystemProvider(provider));
		provider.setAuthorized(systemProviderService.isSystemProvider(provider)
			? systemProviderService.isAuthorized(provider) : null);
	}

	private Map<String, Object> authorizationStatus(LlmProvider provider) {
		Map<String, Object> result = new LinkedHashMap<String, Object>();
		result.put("authorized", systemProviderService.isAuthorized(provider));
		result.put("platformUserName", provider.getPlatformUserName());
		result.put("authorizedAt", provider.getAuthorizedAt());
		result.put("apiKeyConfigured", StringUtils.isNotBlank(provider.getApiKeyCiphertext()));
		return result;
	}

	private int clamp(Integer value, int min, int max, int fallback) {
		int number = value == null ? fallback : value;
		return Math.max(min, Math.min(number, max));
	}
}
