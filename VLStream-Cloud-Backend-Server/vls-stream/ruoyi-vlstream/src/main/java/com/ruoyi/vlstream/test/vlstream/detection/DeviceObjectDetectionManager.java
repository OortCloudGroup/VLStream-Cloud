package com.ruoyi.vlstream.test.vlstream.detection;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.ruoyi.vlstream.test.common.enums.YesNoEnum;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmCategoryEnum;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.Algorithm;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmModel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * 设备目标检测任务管理器：定时扫描设备配置并维护目标检测会话。
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
            log.warn("设备 {} 未配置流地址，跳过目标检测", deviceInfo.getDeviceName());
            return null;
        }

        AlgorithmSelection algorithmSelection = selectAlgorithmByCategory(
            deviceInfo,
            AlgorithmCategoryEnum.detect,
            "目标检测",
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
        return "设备未配置目标检测算法或配置不完整";
    }

    @Override
    protected String getConfigChangedReason() {
        return "设备目标检测配置发生变化";
    }

    @Override
    protected String getRefreshErrorMessage() {
        return "刷新设备目标检测任务失败";
    }

    @Override
    protected String getStopErrorMessage() {
        return "停止设备目标检测失败: deviceId={}, reason={}";
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
