/* SPDX-License-Identifier: MIT */
package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.config.VlsLlmReviewProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.LlmProviderMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.LlmProvider;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Objects;

/** Owns the tenant-local row that points to the operator-managed OortCloud relay. */
@Service
@RequiredArgsConstructor
public class LlmSystemProviderService {

	private static final String SYSTEM_PROVIDER_NAME = "OortCloud";

	private final LlmProviderMapper providerMapper;
	private final VlsLlmReviewProperties properties;

	@Transactional(rollbackFor = Exception.class)
	public LlmProvider getOrCreate() {
		LlmProvider provider = providerMapper.selectOne(new LambdaQueryWrapper<LlmProvider>()
			.eq(LlmProvider::getName, SYSTEM_PROVIDER_NAME)
			.last("limit 1"));
		if (provider == null) {
			provider = new LlmProvider();
			provider.setId(IdUtil.getSnowflakeNextId());
			provider.setName(SYSTEM_PROVIDER_NAME);
			applyRuntimeConfiguration(provider);
			provider.setStatus(1);
			provider.setIsDeleted(0);
			providerMapper.insert(provider);
			return provider;
		}
		if (applyRuntimeConfiguration(provider)) {
			providerMapper.updateById(provider);
		}
		return provider;
	}

	@Transactional(rollbackFor = Exception.class)
	public LlmProvider authorize(String platformUserId, String platformUserName) {
		if (StringUtils.isBlank(platformUserId)) {
			throw new ServiceException("OortCloud 用户标识不能为空");
		}
		LlmProvider provider = getOrCreate();
		provider.setPlatformUserId(StringUtils.abbreviate(StringUtils.trim(platformUserId), 128));
		provider.setPlatformUserName(StringUtils.abbreviate(StringUtils.trimToEmpty(platformUserName), 200));
		if (provider.getAuthorizedAt() == null) {
			provider.setAuthorizedAt(new Date());
		}
		providerMapper.updateById(provider);
		return provider;
	}

	public LlmProvider requireAuthorized() {
		LlmProvider provider = getOrCreate();
		if (!isAuthorized(provider)) {
			throw new ServiceException("请先点击页面顶部“登录 OortCloud”完成授权");
		}
		return provider;
	}

	public boolean isAuthorized(LlmProvider provider) {
		return provider != null && StringUtils.isNotBlank(provider.getPlatformUserId())
			&& provider.getAuthorizedAt() != null;
	}

	public LlmProvider mask(LlmProvider provider) {
		provider.setApiKey(null);
		provider.setApiKeyConfigured(false);
		provider.setAuthorized(isAuthorized(provider));
		return provider;
	}

	private boolean applyRuntimeConfiguration(LlmProvider provider) {
		boolean changed = false;
		changed |= assignIfChanged(provider.getBaseUrl(), properties.getBaseUrl(), provider::setBaseUrl);
		changed |= assignIfChanged(provider.getModelName(), properties.getModelName(), provider::setModelName);
		changed |= assignIfChanged(provider.getTimeoutSeconds(), clampTimeout(properties.getTimeoutSeconds()),
			provider::setTimeoutSeconds);
		if (!Boolean.TRUE.equals(provider.getEnabled())) {
			provider.setEnabled(true);
			changed = true;
		}
		if (StringUtils.isNotEmpty(provider.getApiKeyCiphertext())) {
			provider.setApiKeyCiphertext("");
			changed = true;
		}
		return changed;
	}

	private <T> boolean assignIfChanged(T current, T desired, java.util.function.Consumer<T> setter) {
		if (Objects.equals(current, desired)) {
			return false;
		}
		setter.accept(desired);
		return true;
	}

	private int clampTimeout(Integer value) {
		int timeout = value == null ? 120 : value;
		return Math.max(5, Math.min(timeout, 600));
	}
}
