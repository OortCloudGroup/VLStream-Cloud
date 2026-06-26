package org.springblade.vlstream.detection;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springblade.common.enums.YesNoEnum;
import org.springblade.vlstream.enums.AlgorithmCategoryEnum;
import org.springblade.vlstream.pojo.entity.Algorithm;
import org.springblade.vlstream.pojo.entity.AlgorithmModel;
import org.springblade.vlstream.pojo.entity.DeviceInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * Device object detection task manager: Scans device configurations periodically and maintains object detection sessions.
 */
@Slf4j
@Component
@ConditionalOnProperty(value = "vlstream.object-detection.enabled", havingValue = "true", matchIfMissing = false)
public class DeviceObjectDetectionManager extends AbstractDeviceDetectionManager<DeviceObjectDetectionSession> {

    @Scheduled(fixedDelayString = "${vlstream.object-detection.refresh-interval-millis:30000}")
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
            log.warn("Device {} has no stream address configured, skipping object detection", deviceInfo.getDeviceName());
            return null;
        }

        AlgorithmSelection algorithmSelection = selectAlgorithmByCategory(
            deviceInfo,
            AlgorithmCategoryEnum.detect,
            "Object detection",
            this::resolveObjectModelSourcePath,
            (modelPath, latestModel) -> isOnnxModel(modelPath, latestModel)
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
    protected DeviceObjectDetectionSession createSession(DesiredDeviceConfig desiredConfig) {
        return new DeviceObjectDetectionSession(
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
        return "Device object detection algorithm not configured or configuration incomplete";
    }

    @Override
    protected String getConfigChangedReason() {
        return "Device object detection configuration changed";
    }

    @Override
    protected String getRefreshErrorMessage() {
        return "Failed to refresh device object detection task";
    }

    @Override
    protected String getStopErrorMessage() {
        return "Failed to stop device object detection: deviceId={}, reason={}";
    }

    private String resolveObjectModelSourcePath(Algorithm algorithm, AlgorithmModel latestModel) {
        if (algorithm == null) {
            return null;
        }
        if (YesNoEnum.YES.equals(algorithm.getIsSystem())) {
            return StringUtils.trimToNull(algorithm.getOnnxModelFilePath());
        }
        if (latestModel != null && StringUtils.isNotBlank(latestModel.getOnnxModelPath())) {
            return latestModel.getOnnxModelPath();
        }
        String normalizedFormat = normalizeModelFormat(latestModel != null ? latestModel.getModelFormat() : null,
            latestModel != null ? latestModel.getOnnxModelPath() : null,
            latestModel != null ? latestModel.getModelPath() : null);
        if (StringUtils.equals("onnx", normalizedFormat) && latestModel != null && StringUtils.isNotBlank(latestModel.getModelPath())) {
            return latestModel.getModelPath();
        }
        return StringUtils.trimToNull(algorithm.getOnnxModelFilePath());
    }

}
