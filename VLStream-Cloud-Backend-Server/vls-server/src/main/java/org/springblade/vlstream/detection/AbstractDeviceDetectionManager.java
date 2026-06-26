package org.springblade.vlstream.detection;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springblade.modules.system.service.IFileUploadService;
import org.springblade.vlstream.config.VlsSshProperties;
import org.springblade.vlstream.enums.AlgorithmCategoryEnum;
import org.springblade.vlstream.pojo.entity.Algorithm;
import org.springblade.vlstream.pojo.entity.AlgorithmModel;
import org.springblade.vlstream.pojo.entity.DeviceInfo;
import org.springblade.vlstream.service.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * Device detection task manager base class: Dynamically creates/updates/stops detection sessions based on device configurations, and cleans them up uniformly when system shuts down.
 */
public abstract class AbstractDeviceDetectionManager<S extends DeviceDetectionSession> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    protected VlsSshProperties sshProperties;

    @Resource
    protected IVlsDeviceInfoService deviceInfoService;

    @Resource
    protected IVlsAlgorithmService algorithmService;

    @Resource
    protected IVlsAlgorithmModelService algorithmModelService;

    @Resource
    protected IVlsEventManagementService eventManagementService;

    @Resource
    protected SSHService sshService;

    @Resource
    protected IFileUploadService fileUploadService;

    protected final ConcurrentHashMap<Long, S> sessions = new ConcurrentHashMap<>();
    private final AtomicBoolean refreshInProgress = new AtomicBoolean(false);
    private final AtomicBoolean externalControlEnabled = new AtomicBoolean(false);

    /**
     * Enable/disable external control mode: Once enabled, the scheduled refresh entry will no longer automatically maintain sessions, and refresh will be triggered externally (e.g., by PowerJob).
     */
    public void setExternalControlEnabled(boolean enabled) {
        externalControlEnabled.set(enabled);
    }

    /**
     * Refresh entry point for @Scheduled calls: skipped when external control mode is enabled.
     */
    protected void scheduledRefreshSessionsInternal() {
        if (externalControlEnabled.get()) {
            return;
        }
        refreshSessionsInternal();
    }

    public void refreshNow() {
        refreshSessionsInternal();
    }

    /**
     * Immediately refresh sessions by specified device set (only keep detection sessions corresponding to these devices).
     */
    public void refreshNowForDeviceIds(List<Long> deviceIds) {
        refreshSessionsForDeviceIdsInternal(deviceIds);
    }

    /**
     * Immediately stop detection sessions corresponding to the specified device set.
     */
    public void stopNowForDeviceIds(List<Long> deviceIds, String reason) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return;
        }
        for (Long deviceId : new HashSet<>(deviceIds)) {
            stopSession(deviceId, reason);
        }
    }

    /**
     * Immediately stop all detection sessions.
     */
    public void stopAllNow(String reason) {
        stopAllSessions(reason);
    }

    protected void refreshSessionsInternal() {
        if (!refreshInProgress.compareAndSet(false, true)) {
            return;
        }
        try {
            List<DeviceInfo> devices = deviceInfoService.list(Wrappers.<DeviceInfo>lambdaQuery()
                .isNotNull(DeviceInfo::getAlgorithmId)
                .ne(DeviceInfo::getAlgorithmId, StringUtils.EMPTY));
            refreshSessionsByDeviceInfoList(devices, "No device configured with algorithm found");
        } catch (Exception exception) {
            logger.error(getRefreshErrorMessage(), exception);
        } finally {
            refreshInProgress.set(false);
        }
    }

    protected void refreshSessionsForDeviceIdsInternal(List<Long> deviceIds) {
        if (!refreshInProgress.compareAndSet(false, true)) {
            return;
        }
        try {
            if (deviceIds == null || deviceIds.isEmpty()) {
                stopAllSessions("No device specified for detection");
                return;
            }

            List<DeviceInfo> devices = deviceInfoService.listByIds(deviceIds);
            refreshSessionsByDeviceInfoList(devices, "No devices to detect found");
        } catch (Exception exception) {
            logger.error(getRefreshErrorMessage(), exception);
        } finally {
            refreshInProgress.set(false);
        }
    }

    private void refreshSessionsByDeviceInfoList(List<DeviceInfo> deviceInfoList, String emptyReason) {
        if (deviceInfoList == null || deviceInfoList.isEmpty()) {
            stopAllSessions(emptyReason);
            return;
        }

        Map<Long, DesiredDeviceConfig> desiredConfigMap = new HashMap<>();
        for (DeviceInfo deviceInfo : deviceInfoList) {
            DesiredDeviceConfig desiredConfig = buildDesiredConfig(deviceInfo);
            if (desiredConfig == null || desiredConfig.deviceInfo == null || desiredConfig.deviceInfo.getId() == null) {
                continue;
            }
            desiredConfigMap.put(desiredConfig.deviceInfo.getId(), desiredConfig);
        }

        Set<Long> desiredDeviceIdSet = new HashSet<>(desiredConfigMap.keySet());
        Set<Long> existingDeviceIdSet = new HashSet<>(sessions.keySet());

        for (Long existingDeviceId : existingDeviceIdSet) {
            if (!desiredDeviceIdSet.contains(existingDeviceId)) {
                stopSession(existingDeviceId, getMissingConfigReason());
            }
        }

        for (Map.Entry<Long, DesiredDeviceConfig> desiredEntry : desiredConfigMap.entrySet()) {
            Long deviceId = desiredEntry.getKey();
            DesiredDeviceConfig desiredConfig = desiredEntry.getValue();
            S existingSession = sessions.get(deviceId);
            if (existingSession == null) {
                startNewSession(desiredConfig);
                continue;
            }
            if (!existingSession.matches(desiredConfig.algorithmId, desiredConfig.streamUrl, desiredConfig.modelSourcePath)) {
                stopSession(deviceId, getConfigChangedReason());
                startNewSession(desiredConfig);
            }
        }
    }

    protected abstract DesiredDeviceConfig buildDesiredConfig(DeviceInfo deviceInfo);

    protected abstract S createSession(DesiredDeviceConfig desiredConfig);

    protected abstract String getMissingConfigReason();

    protected abstract String getConfigChangedReason();

    protected abstract String getRefreshErrorMessage();

    protected abstract String getStopErrorMessage();

    private void startNewSession(DesiredDeviceConfig desiredConfig) {
        if (desiredConfig == null || desiredConfig.deviceInfo == null || desiredConfig.deviceInfo.getId() == null) {
            return;
        }
        Long deviceId = desiredConfig.deviceInfo.getId();
        S session = createSession(desiredConfig);
        if (session == null) {
            return;
        }

        sessions.put(deviceId, session);
        boolean started = session.start();
        if (!started) {
            sessions.remove(deviceId, session);
        }
    }

    private void stopSession(Long deviceId, String reason) {
        if (deviceId == null) {
            return;
        }
        S session = sessions.remove(deviceId);
        if (session == null) {
            return;
        }
        try {
            session.stop(reason);
        } catch (Exception exception) {
            logger.warn(getStopErrorMessage(), deviceId, reason, exception);
        }
    }

    private void stopAllSessions(String reason) {
        if (sessions.isEmpty()) {
            return;
        }
        for (Long deviceId : new HashSet<>(sessions.keySet())) {
            stopSession(deviceId, reason);
        }
    }

    protected List<Long> splitToIds(String rawIds) {
        if (StringUtils.isBlank(rawIds)) {
            return List.of();
        }
        String[] parts = rawIds.split(",");
        List<Long> ids = new ArrayList<>();
        for (String part : parts) {
            Long parsed = parseAlgorithmId(part);
            if (parsed != null) {
                ids.add(parsed);
            }
        }
        return ids;
    }

    private Long parseAlgorithmId(String algorithmIdText) {
        if (StringUtils.isBlank(algorithmIdText)) {
            return null;
        }
        try {
            return Long.valueOf(algorithmIdText.trim());
        } catch (NumberFormatException numberFormatException) {
            return null;
        }
    }

    protected Path resolveOutputDir(DeviceInfo deviceInfo) {
        String outputKey = StringUtils.defaultIfBlank(deviceInfo.getDeviceId(), String.valueOf(deviceInfo.getId()));
        Path outputDir = Paths.get("output", "events", outputKey);
        FileUtil.mkdir(outputDir.toFile());
        return outputDir;
    }

    protected String resolveStreamUrl(DeviceInfo deviceInfo) {
        if (deviceInfo == null) {
            return null;
        }
        return deviceInfo.getStreamUrl();
    }

    protected String resolveDefaultModelSourcePath(Algorithm algorithm, AlgorithmModel latestModel) {
        if (latestModel != null) {
            String modelPath = StringUtils.trimToNull(latestModel.getModelPath());
            if (StringUtils.isNotBlank(modelPath)) {
                return modelPath;
            }
            String onnxModelPath = StringUtils.trimToNull(latestModel.getOnnxModelPath());
            if (StringUtils.isNotBlank(onnxModelPath)) {
                return onnxModelPath;
            }
        }
        if (algorithm == null) {
            return null;
        }
        String ptModelPath = StringUtils.trimToNull(algorithm.getPtModelFilePath());
        if (StringUtils.isNotBlank(ptModelPath)) {
            return ptModelPath;
        }
        return StringUtils.trimToNull(algorithm.getOnnxModelFilePath());
    }

    protected String resolveOnnxModelSourcePath(Algorithm algorithm, AlgorithmModel latestModel) {
        if (latestModel != null) {
            String onnxModelPath = StringUtils.trimToNull(latestModel.getOnnxModelPath());
            if (StringUtils.isNotBlank(onnxModelPath)) {
                return onnxModelPath;
            }
        }
        if (algorithm == null) {
            return null;
        }
        return StringUtils.trimToNull(algorithm.getOnnxModelFilePath());
    }

    protected String normalizeModelFormat(String modelFormat, String... modelPathArray) {
        String normalizedFormat = StringUtils.trimToEmpty(modelFormat).toLowerCase();
        if (StringUtils.isNotBlank(normalizedFormat)) {
            return normalizedFormat;
        }
        if (modelPathArray == null || modelPathArray.length == 0) {
            return StringUtils.EMPTY;
        }
        for (String modelPath : modelPathArray) {
            if (StringUtils.isBlank(modelPath)) {
                continue;
            }
            String loweredPath = modelPath.toLowerCase();
            if (loweredPath.endsWith(".onnx")) {
                return "onnx";
            }
        }
        return StringUtils.EMPTY;
    }

    protected boolean isOnnxModel(String modelPath, AlgorithmModel latestModel) {
        if (StringUtils.isBlank(modelPath)) {
            return false;
        }
        if (modelPath.trim().toLowerCase().endsWith(".onnx")) {
            return true;
        }
        String normalizedFormat = normalizeModelFormat(
            latestModel != null ? latestModel.getModelFormat() : null,
            latestModel != null ? latestModel.getOnnxModelPath() : null,
            latestModel != null ? latestModel.getModelPath() : null,
            modelPath
        );
        return StringUtils.equals("onnx", normalizedFormat);
    }

    protected AlgorithmSelection selectAlgorithmByCategory(DeviceInfo deviceInfo,
                                                           AlgorithmCategoryEnum algorithmCategory,
                                                           String sceneName,
                                                           BiFunction<Algorithm, AlgorithmModel, String> modelPathResolver,
                                                           BiPredicate<String, AlgorithmModel> modelPathValidator) {
        if (deviceInfo == null || algorithmCategory == null || modelPathResolver == null) {
            return null;
        }
        List<Long> algorithmIdList = splitToIds(deviceInfo.getAlgorithmId());
        if (algorithmIdList.isEmpty()) {
            return null;
        }

        for (Long candidateAlgorithmId : algorithmIdList) {
            if (candidateAlgorithmId == null) {
                continue;
            }
            Algorithm candidateAlgorithm = algorithmService.getById(candidateAlgorithmId);
            if (candidateAlgorithm == null) {
                logger.warn("Device {}'s algorithm {} does not exist, skipping {}", deviceInfo.getDeviceName(), candidateAlgorithmId, sceneName);
                continue;
            }
            if (candidateAlgorithm.getCategory() != algorithmCategory) {
                continue;
            }

            AlgorithmModel latestModel = algorithmModelService.getLatestModelByAlgorithmId(candidateAlgorithmId);
            String candidateModelPath = modelPathResolver.apply(candidateAlgorithm, latestModel);
            if (StringUtils.isBlank(candidateModelPath)) {
                logger.warn("Device {}'s algorithm {} found no available model path, skipping {}", deviceInfo.getDeviceName(), candidateAlgorithmId, sceneName);
                continue;
            }
            if (modelPathValidator != null && !modelPathValidator.test(candidateModelPath, latestModel)) {
                logger.warn("Device {}'s algorithm {} model verification failed, skipping {}: modelPath={}, modelFormat={}",
                    deviceInfo.getDeviceName(), candidateAlgorithmId, sceneName, candidateModelPath,
                    latestModel != null ? latestModel.getModelFormat() : null);
                continue;
            }
            return new AlgorithmSelection(candidateAlgorithmId, candidateAlgorithm, latestModel, candidateModelPath);
        }
        return null;
    }

    @PreDestroy
    public void shutdown() {
        stopAllSessions("System shutdown");
    }

    protected static class DesiredDeviceConfig {
        protected final DeviceInfo deviceInfo;
        protected final Algorithm algorithm;
        protected final Long algorithmId;
        protected final String streamUrl;
        protected final String modelSourcePath;
        protected final Path outputDir;

        protected DesiredDeviceConfig(DeviceInfo deviceInfo,
                                      Algorithm algorithm,
                                      Long algorithmId,
                                      String streamUrl,
                                      String modelSourcePath,
                                      Path outputDir) {
            this.deviceInfo = deviceInfo;
            this.algorithm = algorithm;
            this.algorithmId = algorithmId;
            this.streamUrl = streamUrl;
            this.modelSourcePath = modelSourcePath;
            this.outputDir = outputDir;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof DesiredDeviceConfig other)) {
                return false;
            }
            return Objects.equals(deviceInfo != null ? deviceInfo.getId() : null, other.deviceInfo != null ? other.deviceInfo.getId() : null)
                && Objects.equals(algorithmId, other.algorithmId)
                && Objects.equals(streamUrl, other.streamUrl)
                && Objects.equals(modelSourcePath, other.modelSourcePath);
        }

        @Override
        public int hashCode() {
            return Objects.hash(deviceInfo != null ? deviceInfo.getId() : null, algorithmId, streamUrl, modelSourcePath);
        }
    }

    protected static class AlgorithmSelection {
        protected final Long algorithmId;
        protected final Algorithm algorithm;
        protected final AlgorithmModel latestModel;
        protected final String modelSourcePath;

        protected AlgorithmSelection(Long algorithmId,
                                     Algorithm algorithm,
                                     AlgorithmModel latestModel,
                                     String modelSourcePath) {
            this.algorithmId = algorithmId;
            this.algorithm = algorithm;
            this.latestModel = latestModel;
            this.modelSourcePath = modelSourcePath;
        }
    }
}
