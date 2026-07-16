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
 * 设备图像分类任务管理器：定时扫描设备配置并维护图像分类检测会话。
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "vlstream.classify-detection.enabled", havingValue = "true", matchIfMissing = false)
public class DeviceClassifyDetectionManager extends AbstractDeviceDetectionManager<DeviceClassifyDetectionSession> {

	@Scheduled(fixedDelayString = "${vlstream.classify-detection.refresh-interval-millis:30000}")
	public void scheduledRefresh() {
		scheduledRefreshSessionsInternal();
	}

    @Override
    protected DesiredDeviceConfig buildDesiredConfig(DeviceInfo deviceInfo) {
        if (deviceInfo == null || deviceInfo.getId() == null) {
            return null;
        }
        String streamUrl = resolveStreamUrl(deviceInfo);
        if (StringUtils.isBlank(streamUrl)) {
            log.warn("设备 {} 未配置流地址，跳过图像分类", deviceInfo.getDeviceName());
            return null;
        }

        AlgorithmSelection algorithmSelection = selectAlgorithmByCategory(
            deviceInfo,
            AlgorithmCategoryEnum.classify,
            "图像分类",
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
    protected DeviceClassifyDetectionSession createSession(DesiredDeviceConfig desiredConfig) {
        return new DeviceClassifyDetectionSession(
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
        return "设备未配置图像分类算法或配置不完整";
    }

    @Override
    protected String getConfigChangedReason() {
        return "设备图像分类配置发生变化";
    }

    @Override
    protected String getRefreshErrorMessage() {
        return "刷新设备图像分类任务失败";
    }

    @Override
    protected String getStopErrorMessage() {
        return "停止设备图像分类失败: deviceId={}, reason={}";
    }

}
