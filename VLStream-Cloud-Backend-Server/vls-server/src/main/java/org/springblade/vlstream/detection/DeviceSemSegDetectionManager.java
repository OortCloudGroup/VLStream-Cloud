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
 * Device semantic segmentation task manager: Scans device configurations periodically and maintains semantic segmentation sessions (supports image URL / device image path as input).
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
            log.warn("Device {} has no image source configured, skipping semantic segmentation", deviceInfo.getDeviceName());
            return null;
        }
        if (StringUtils.isBlank(imagePath) && !isHttpUrl(streamUrl)) {
            log.warn("Device {} has no available image address configured, skipping semantic segmentation: streamUrl={}", deviceInfo.getDeviceName(), streamUrl);
            return null;
        }


        AlgorithmSelection algorithmSelection = selectAlgorithmByCategory(
            deviceInfo,
            AlgorithmCategoryEnum.semanticSeg,
            "Semantic Segmentation",
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
        return "Device semantic segmentation algorithm not configured or configuration incomplete";
    }

    @Override
    protected String getConfigChangedReason() {
        return "Device semantic segmentation configuration changed";
    }

    @Override
    protected String getRefreshErrorMessage() {
        return "Failed to refresh device semantic segmentation task";
    }

    @Override
    protected String getStopErrorMessage() {
        return "Failed to stop device semantic segmentation: deviceId={}, reason={}";
    }

    private boolean isHttpUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }
        String lower = url.trim().toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }
}
