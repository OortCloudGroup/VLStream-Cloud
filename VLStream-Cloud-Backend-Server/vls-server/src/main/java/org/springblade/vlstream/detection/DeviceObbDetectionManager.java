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
 * Device oriented bounding box (OBB) detection task manager: Scans device configurations periodically and maintains oriented bounding box detection sessions.
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "vlstream.obb-detection.enabled", havingValue = "true", matchIfMissing = false)
public class DeviceObbDetectionManager extends AbstractDeviceDetectionManager<DeviceObbDetectionSession> {

    @Scheduled(fixedDelayString = "${vlstream.obb-detection.refresh-interval-millis:30000}")
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
            log.warn("Device {} has no stream address configured, skipping oriented bounding box detection", deviceInfo.getDeviceName());
            return null;
        }

        AlgorithmSelection algorithmSelection = selectAlgorithmByCategory(
            deviceInfo,
            AlgorithmCategoryEnum.obb,
            "Rotated bounding box detection",
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
    protected DeviceObbDetectionSession createSession(DesiredDeviceConfig desiredConfig) {
        return new DeviceObbDetectionSession(
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
        return "Device oriented bounding box detection algorithm not configured or configuration incomplete";
    }

    @Override
    protected String getConfigChangedReason() {
        return "Device oriented bounding box detection configuration changed";
    }

    @Override
    protected String getRefreshErrorMessage() {
        return "Failed to refresh device oriented bounding box detection task";
    }

    @Override
    protected String getStopErrorMessage() {
        return "Failed to stop device oriented bounding box detection: deviceId={}, reason={}";
    }

}
