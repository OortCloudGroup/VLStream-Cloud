package org.springblade.vlstream.detection;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springblade.vlstream.enums.AlgorithmCategoryEnum;
import org.springblade.vlstream.pojo.entity.DeviceInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Device pose estimation task manager: Scans device configurations periodically and maintains pose estimation sessions.
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
            log.warn("Device {} has no stream address configured, skipping pose estimation", deviceInfo.getDeviceName());
            return null;
        }

        AlgorithmSelection algorithmSelection = selectAlgorithmByCategory(
            deviceInfo,
            AlgorithmCategoryEnum.pose,
            "Pose estimation",
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
        return "Device pose estimation algorithm not configured or configuration incomplete";
    }

    @Override
    protected String getConfigChangedReason() {
        return "Device pose estimation configuration changed";
    }

    @Override
    protected String getRefreshErrorMessage() {
        return "Failed to refresh device pose estimation task";
    }

    @Override
    protected String getStopErrorMessage() {
        return "Failed to stop device pose estimation: deviceId={}, reason={}";
    }

}
