/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.detection;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmCategoryEnum;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * 设备语义分割任务管理器：定时扫描设备配置并维护语义分割会话（支持图片 URL / 设备图片路径作为输入）。
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "vlstream.semseg-detection.enabled", havingValue = "true", matchIfMissing = false)
public class DeviceSemSegDetectionManager extends AbstractDeviceDetectionManager<DeviceSemSegDetectionSession> {

    @Scheduled(fixedDelayString = "${vlstream.semseg-detection.refresh-interval-millis:30000}")
    public void scheduledRefresh() {
        scheduledRefreshSessionsInternal();
    }

    @Override
    protected DesiredDeviceConfig buildDesiredConfig(DeviceInfo deviceInfo) {
        if (deviceInfo == null || deviceInfo.getId() == null) {
            return null;
        }
        String streamUrl = resolveStreamUrl(deviceInfo);
        String imagePath = StringUtils.trimToNull(deviceInfo.getImagePath());
        if (StringUtils.isBlank(streamUrl) && StringUtils.isBlank(imagePath)) {
            log.warn("设备 {} 未配置图像来源，跳过语义分割", deviceInfo.getDeviceName());
            return null;
        }
        if (StringUtils.isBlank(imagePath) && !isHttpUrl(streamUrl)) {
            log.warn("设备 {} 未配置可用图片地址，跳过语义分割: streamUrl={}", deviceInfo.getDeviceName(), streamUrl);
            return null;
        }


        AlgorithmSelection algorithmSelection = selectAlgorithmByCategory(
            deviceInfo,
            AlgorithmCategoryEnum.semanticSeg,
            "语义分割",
            this::resolveDefaultModelSourcePath,
            null
        );
        if (algorithmSelection == null || StringUtils.isBlank(algorithmSelection.modelSourcePath)) {
            return null;
        }

        Path outputDir = resolveOutputDir(deviceInfo);
        return new DesiredDeviceConfig(
            deviceInfo,
            algorithmSelection.algorithm,
            algorithmSelection.algorithmId,
            streamUrl,
            algorithmSelection.modelSourcePath,
            outputDir
        );
    }

    @Override
    protected DeviceSemSegDetectionSession createSession(DesiredDeviceConfig desiredConfig) {
        return new DeviceSemSegDetectionSession(
            sshProperties,
            sshService,
            fileUploadService,
            eventManagementService,
            desiredConfig.deviceInfo,
            desiredConfig.algorithm,
            desiredConfig.algorithmId,
            desiredConfig.streamUrl,
            desiredConfig.modelSourcePath,
            desiredConfig.outputDir
        );
    }

    @Override
    protected String getMissingConfigReason() {
        return "设备未配置语义分割算法或配置不完整";
    }

    @Override
    protected String getConfigChangedReason() {
        return "设备语义分割配置发生变化";
    }

    @Override
    protected String getRefreshErrorMessage() {
        return "刷新设备语义分割任务失败";
    }

    @Override
    protected String getStopErrorMessage() {
        return "停止设备语义分割失败: deviceId={}, reason={}";
    }

    private boolean isHttpUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }
        String lower = url.trim().toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }
}
