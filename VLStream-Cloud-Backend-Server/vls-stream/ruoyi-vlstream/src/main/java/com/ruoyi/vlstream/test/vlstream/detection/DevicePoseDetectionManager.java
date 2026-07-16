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
 * 设备姿态估计任务管理器：定时扫描设备配置并维护姿态估计会话。
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "vlstream.pose-detection.enabled", havingValue = "true", matchIfMissing = false)
public class DevicePoseDetectionManager extends AbstractDeviceDetectionManager<DevicePoseDetectionSession> {

    @Scheduled(fixedDelayString = "${vlstream.pose-detection.refresh-interval-millis:30000}")
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
            log.warn("设备 {} 未配置流地址，跳过姿态估计", deviceInfo.getDeviceName());
            return null;
        }

        AlgorithmSelection algorithmSelection = selectAlgorithmByCategory(
            deviceInfo,
            AlgorithmCategoryEnum.pose,
            "姿态估计",
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
    protected DevicePoseDetectionSession createSession(DesiredDeviceConfig desiredConfig) {
        return new DevicePoseDetectionSession(
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
        return "设备未配置姿态估计算法或配置不完整";
    }

    @Override
    protected String getConfigChangedReason() {
        return "设备姿态估计配置发生变化";
    }

    @Override
    protected String getRefreshErrorMessage() {
        return "刷新设备姿态估计任务失败";
    }

    @Override
    protected String getStopErrorMessage() {
        return "停止设备姿态估计失败: deviceId={}, reason={}";
    }

}
