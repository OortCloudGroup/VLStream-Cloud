package com.ruoyi.vlstream.test.vlstream.detection;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.ruoyi.vlstream.test.modules.system.service.IFileUploadService;
import com.ruoyi.vlstream.test.vlstream.config.VlsSshProperties;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmCategoryEnum;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.Algorithm;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmModel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import com.ruoyi.vlstream.test.vlstream.service.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * 设备检测任务管理器基类：按设备配置动态创建/更新/停止检测会话，并在系统关闭时统一清理。
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

    // Flowable 也声明了名为 eventManagementService 的 Bean，必须按 VLStream 接口类型注入。
    @Autowired
    protected IVlsEventManagementService eventManagementService;

    @Resource
    protected SSHService sshService;

    @Resource
    protected IFileUploadService fileUploadService;

    protected final ConcurrentHashMap<Long, S> sessions = new ConcurrentHashMap<>();
    private final AtomicBoolean refreshInProgress = new AtomicBoolean(false);
    private final AtomicBoolean externalControlEnabled = new AtomicBoolean(false);

    /**
     * 启用/关闭外部控制模式：启用后，定时刷新入口将不再自动维护会话，由外部（如 PowerJob）触发刷新。
     */
    public void setExternalControlEnabled(boolean enabled) {
        externalControlEnabled.set(enabled);
    }

    /**
     * 供 @Scheduled 调用的刷新入口：外部控制模式启用时跳过。
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
     * 立即按指定设备集合刷新会话（仅保留这些设备对应的检测会话）。
     */
    public void refreshNowForDeviceIds(List<Long> deviceIds) {
        refreshSessionsForDeviceIdsInternal(deviceIds);
    }

    /**
     * 立即停止指定设备集合对应的检测会话。
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
     * 立即停止全部检测会话。
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
            refreshSessionsByDeviceInfoList(devices, "未找到配置算法的设备");
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
                stopAllSessions("未指定需要检测的设备");
                return;
            }

            List<DeviceInfo> devices = deviceInfoService.listByIds(deviceIds);
            refreshSessionsByDeviceInfoList(devices, "未找到需要检测的设备");
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
            return java.util.Arrays.asList();
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
                logger.warn("设备 {} 的算法 {} 不存在，跳过{}", deviceInfo.getDeviceName(), candidateAlgorithmId, sceneName);
                continue;
            }
            if (candidateAlgorithm.getCategory() != algorithmCategory) {
                continue;
            }

            AlgorithmModel latestModel = algorithmModelService.getLatestModelByAlgorithmId(candidateAlgorithmId);
            String candidateModelPath = modelPathResolver.apply(candidateAlgorithm, latestModel);
            if (StringUtils.isBlank(candidateModelPath)) {
                logger.warn("设备 {} 的算法 {} 未找到可用模型路径，跳过{}", deviceInfo.getDeviceName(), candidateAlgorithmId, sceneName);
                continue;
            }
            if (modelPathValidator != null && !modelPathValidator.test(candidateModelPath, latestModel)) {
                logger.warn("设备 {} 的算法 {} 模型校验不通过，跳过{}: modelPath={}, modelFormat={}",
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
        stopAllSessions("系统关闭");
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
            if (!(obj instanceof DesiredDeviceConfig)) {
                return false;
            }
            DesiredDeviceConfig other = (DesiredDeviceConfig) obj;
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
