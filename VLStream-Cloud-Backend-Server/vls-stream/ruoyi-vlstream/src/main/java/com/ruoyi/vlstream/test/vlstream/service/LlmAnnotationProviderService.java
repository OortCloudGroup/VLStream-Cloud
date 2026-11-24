/* SPDX-License-Identifier: MIT */
package com.ruoyi.vlstream.test.vlstream.service;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.mapper.LlmProviderMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.LlmProvider;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Reuses the tenant's provider authorization without changing its event review model. */
@Service
@RequiredArgsConstructor
public class LlmAnnotationProviderService {
    private final LlmProviderMapper providers;
    private final LlmSystemProviderService systemProvider;
    private final OpenAiVisionClient vision;

    public List<ProviderOption> listProviders(String tenantId) {
        return providers.selectList(scope(tenantId).eq(LlmProvider::getEnabled, true).orderByAsc(LlmProvider::getName))
            .stream().filter(provider -> isAvailable(tenantId, provider))
            .map(provider -> new ProviderOption(provider.getId().toString(), provider.getName(), provider.getModelName()))
            .collect(Collectors.toList());
    }

    public List<String> listModels(String tenantId, Long providerId) {
        return vision.listAnnotationModels(requireProvider(tenantId, providerId)).stream()
            .filter(this::validModelName).collect(Collectors.toList());
    }

    public Binding bind(String tenantId, Long providerId, String modelName) {
        LlmProvider provider = requireProvider(tenantId, providerId);
        String model = StringUtils.defaultIfBlank(StringUtils.trim(modelName), provider.getModelName());
        if (!validModelName(model)) throw new ServiceException("请选择有效的视觉模型");
        return new Binding(model, fingerprint(provider, model));
    }

    public void verifyBinding(String tenantId, Long providerId, String modelName, String providerFingerprint) {
        requireBinding(tenantId, providerId, modelName, providerFingerprint);
    }

    public String annotate(String tenantId, Long providerId, String modelName, String providerFingerprint, String prompt, byte[] image) {
        return vision.annotate(requireBinding(tenantId, providerId, modelName, providerFingerprint), modelName, prompt, image);
    }

    private LlmProvider requireBinding(String tenantId, Long providerId, String modelName, String providerFingerprint) {
        LlmProvider provider = requireProvider(tenantId, providerId);
        if (!validModelName(modelName) || !StringUtils.equals(providerFingerprint, fingerprint(provider, modelName))) {
            throw new ServiceException("大模型连接或授权已变更，请使用当前授权重新创建任务", 409);
        }
        return provider;
    }

    private LlmProvider requireProvider(String tenantId, Long providerId) {
        if (providerId == null) throw new ServiceException("请选择已授权的大模型提供商");
        LlmProvider provider = providers.selectOne(scope(tenantId).eq(LlmProvider::getId, providerId));
        if (!isAvailable(tenantId, provider)) throw new ServiceException("大模型提供商不可用，请检查启用状态和授权", 409);
        return provider;
    }

    private LambdaQueryWrapper<LlmProvider> scope(String tenantId) {
        if (StringUtils.isBlank(tenantId)) throw new ServiceException("缺少可信租户上下文");
        return new LambdaQueryWrapper<LlmProvider>().eq(LlmProvider::getTenantId, tenantId);
    }

    private boolean isAvailable(String tenantId, LlmProvider provider) {
        return provider != null && provider.getId() != null && tenantId.equals(provider.getTenantId())
            && Boolean.TRUE.equals(provider.getEnabled()) && StringUtils.isNotBlank(provider.getApiKeyCiphertext())
            && (!systemProvider.isSystemProvider(provider) || systemProvider.isAuthorized(provider))
            && validEndpoint(provider.getBaseUrl());
    }

    private boolean validModelName(String value) {
        return StringUtils.isNotBlank(value) && value.length() <= 200 && value.equals(value.trim())
            && value.chars().noneMatch(Character::isISOControl);
    }

    private boolean validEndpoint(String baseUrl) {
        try {
            URI endpoint = URI.create(StringUtils.trimToEmpty(baseUrl));
            return ("http".equalsIgnoreCase(endpoint.getScheme()) || "https".equalsIgnoreCase(endpoint.getScheme()))
                && endpoint.getHost() != null && endpoint.getUserInfo() == null
                && endpoint.getQuery() == null && endpoint.getFragment() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String fingerprint(LlmProvider provider, String model) {
        return DigestUtils.sha256Hex(JSONUtil.toJsonStr(Arrays.asList(
            provider.getId(), provider.getTenantId(), provider.getBaseUrl(), model,
            provider.getPlatformUserId(), provider.getAuthorizedAt() == null ? null : provider.getAuthorizedAt().getTime(),
            provider.getApiKeyCiphertext())));
    }

    @Getter
    @RequiredArgsConstructor
    public static class ProviderOption {
        private final String id;
        private final String name;
        private final String modelName;
    }

    @Getter
    @RequiredArgsConstructor
    public static class Binding {
        private final String modelName;
        @JsonIgnore
        private final String providerFingerprint;
    }
}
