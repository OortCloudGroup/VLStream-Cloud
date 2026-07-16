package com.ruoyi.vlstream.test.vlstream.detection;

import ai.djl.modality.cv.Image;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.smartjavaai.cls.config.ClsModelConfig;
import cn.smartjavaai.cls.enums.ClsModelEnum;
import cn.smartjavaai.cls.model.ClsModel;
import cn.smartjavaai.cls.model.ClsModelFactory;
import cn.smartjavaai.cls.stream.ClsStreamDetectionListener;
import cn.smartjavaai.cls.stream.ClsStreamDetector;
import cn.smartjavaai.common.cv.SmartImageFactory;
import cn.smartjavaai.common.entity.DetectionInfo;
import cn.smartjavaai.common.enums.DeviceEnum;
import cn.smartjavaai.common.enums.VideoSourceType;
import cn.smartjavaai.common.utils.ImageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.ruoyi.vlstream.test.common.constant.CommonConstant;
import com.ruoyi.vlstream.test.modules.system.service.IFileUploadService;
import com.ruoyi.vlstream.test.vlstream.config.VlsSshProperties;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FileResponseDto;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.Algorithm;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import com.ruoyi.vlstream.test.vlstream.service.IVlsEventManagementService;
import com.ruoyi.vlstream.test.vlstream.service.SSHService;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 单设备图像分类检测会话：加载模型并对视频流进行分类，在产生结果时截图上传并创建事件；支持异常重启与资源清理。
 */
@Slf4j
public class DeviceClassifyDetectionSession implements DeviceDetectionSession {

    public static DeviceEnum device = DeviceEnum.CPU;

    private static final int MAX_START_RETRY_TIMES = 3;
    private static final long START_RETRY_DELAY_MILLIS = 5000L;

    private final VlsSshProperties sshProperties;
    private final SSHService sshService;
    private final IFileUploadService fileUploadService;
    private final IVlsEventManagementService eventManagementService;

    private final DeviceInfo deviceInfo;
    private final Algorithm algorithm;
    private final Long algorithmId;
    private final String streamUrl;
    private final String modelSourcePath;
    private final Path outputDir;

    private final AtomicBoolean detectionInProgress = new AtomicBoolean(false);
    private final AtomicBoolean detectorClosed = new AtomicBoolean(false);
    private final AtomicBoolean restartInProgress = new AtomicBoolean(false);
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);

    private volatile ClsStreamDetector detector;
    private volatile ClsModel detectorModel;
    private volatile Path tempModelDirectory;

    public DeviceClassifyDetectionSession(VlsSshProperties sshProperties,
                                          SSHService sshService,
                                          IFileUploadService fileUploadService,
                                          IVlsEventManagementService eventManagementService,
                                          DeviceInfo deviceInfo,
                                          Algorithm algorithm,
                                          Long algorithmId,
                                          String streamUrl,
                                          String modelSourcePath,
                                          Path outputDir) {
        this.sshProperties = sshProperties;
        this.sshService = sshService;
        this.fileUploadService = fileUploadService;
        this.eventManagementService = eventManagementService;
        this.deviceInfo = deviceInfo;
        this.algorithm = algorithm;
        this.algorithmId = algorithmId;
        this.streamUrl = streamUrl;
        this.modelSourcePath = modelSourcePath;
        this.outputDir = outputDir;
    }

    public boolean matches(Long algorithmId, String streamUrl, String modelSourcePath) {
        return Objects.equals(this.algorithmId, algorithmId)
            && Objects.equals(this.streamUrl, streamUrl)
            && Objects.equals(this.modelSourcePath, modelSourcePath);
    }

    public boolean start() {
        SmartImageFactory.setEngine(SmartImageFactory.Engine.OPENCV);
        String localModelPath = prepareLocalModelPath();
        if (StringUtils.isBlank(localModelPath)) {
            return false;
        }
        this.detectorModel = buildDetectorModel(localModelPath);
        startDetectionWithRetry(detectorModel);
        return true;
    }

    public void stop(String reason) {
        stopRequested.set(true);
        log.info("停止图像分类: deviceId={}, deviceName={}, algorithmId={}, reason={}",
            deviceInfo.getId(), deviceInfo.getDeviceName(), algorithmId, reason);
        stopDetector();
        cleanupTempModelDirectory();
    }

    private ClsModel buildDetectorModel(String localModelPath) {
        ClsModelConfig config = new ClsModelConfig();
        config.setModelEnum(ClsModelEnum.YOLOV8);
        config.setModelPath(localModelPath);
        config.setDevice(device);
        config.setThreshold(0.5f);
        return ClsModelFactory.getInstance().getModel(config);
    }

    private void startDetectionWithRetry(ClsModel detectorModel) {
        int retryTimes = 0;
        while (true) {
            ClsStreamDetector streamDetector = buildStreamDetector(detectorModel);
            detectorClosed.set(false);
            this.detector = streamDetector;
            try {
                streamDetector.startDetection();
                log.info("开始图像分类: deviceId={}, deviceName={}, algorithmId={}, streamUrl={}",
                    deviceInfo.getId(), deviceInfo.getDeviceName(), algorithmId, streamUrl);
                return;
            } catch (Exception startException) {
                retryTimes++;
                log.error("启动图像分类器失败: deviceId={}, deviceName={}, algorithmId={}, retryTimes={}, error={}",
                    deviceInfo.getId(), deviceInfo.getDeviceName(), algorithmId, retryTimes, startException.getMessage(), startException);
                stopDetector();
                if (retryTimes > MAX_START_RETRY_TIMES) {
                    return;
                }
                try {
                    Thread.sleep(START_RETRY_DELAY_MILLIS);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private ClsStreamDetector buildStreamDetector(ClsModel detectorModel) {
        return new ClsStreamDetector.Builder()
            .sourceType(VideoSourceType.STREAM)
            .streamUrl(streamUrl)
            .frameDetectionInterval(20)
            .detectorModel(detectorModel)
            .listener(new ClsStreamDetectionListener() {
                @Override
                public void onObjectDetected(List<DetectionInfo> detectionInfoList, Image image) {
                    if (detectionInfoList == null || detectionInfoList.isEmpty()) {
                        return;
                    }
                    if (!detectionInProgress.compareAndSet(false, true)) {
                        return;
                    }
                    Image snapshotImage = null;
                    try {
                        if (image == null) {
                            return;
                        }
                        snapshotImage = DetectionSessionSupport.copyForSnapshot(image, log);
                        if (snapshotImage == null) {
                            log.warn("设备 {} 复制截图失败，跳过", deviceInfo.getDeviceName());
                            return;
                        }

                        LocalDateTime now = LocalDateTimeUtil.now();
                        Path snapshot = outputDir.resolve("rtsp_cls_" + System.currentTimeMillis() + ".png");
                        File snapFile = snapshot.toFile();
                        try {
                            ImageUtils.save(snapshotImage, snapFile.getName(), snapFile.getParent());
                            log.info("图像分类截图已保存: deviceId={}, deviceName={}, time={}, path={}",
                                deviceInfo.getId(), deviceInfo.getDeviceName(), now, snapshot.toAbsolutePath());
                        } catch (Exception saveException) {
                            log.warn("设备 {} 保存截图失败", deviceInfo.getDeviceName(), saveException);
                            return;
                        }

                        FileResponseDto fileResponseDto = DetectionSessionSupport.uploadSnapshot(fileUploadService, snapFile);
                        if (fileResponseDto == null || StringUtils.isBlank(fileResponseDto.getUrl())) {
                            log.warn("设备 {} 上传截图失败，跳过事件创建", deviceInfo.getDeviceName());
                            return;
                        }
                        createEvent(deviceInfo, algorithm, detectionInfoList, fileResponseDto.getUrl());
                        log.info("图像分类事件已创建: deviceId={}, deviceName={}, algorithmId={}, results={}",
                            deviceInfo.getId(), deviceInfo.getDeviceName(), algorithmId, detectionInfoList.size());
                    } catch (Exception detectException) {
                        log.warn("设备 {} 图像分类回调处理失败", deviceInfo.getDeviceName(), detectException);
                        scheduleDetectorRestart("detectException");
                    } finally {
                        detectionInProgress.set(false);
                        DetectionSessionSupport.closeQuietly(snapshotImage, log);
                    }
                }

                @Override
                public void onStreamEnded() {
                    log.info("图像分类视频流结束: deviceId={}, deviceName={}, algorithmId={}",
                        deviceInfo.getId(), deviceInfo.getDeviceName(), algorithmId);
                    scheduleDetectorRestart("streamEnded");
                }

                @Override
                public void onStreamDisconnected() {
                    log.info("图像分类视频流断开: deviceId={}, deviceName={}, algorithmId={}",
                        deviceInfo.getId(), deviceInfo.getDeviceName(), algorithmId);
                    scheduleDetectorRestart("streamDisconnected");
                }
            })
            .build();
    }

    private void scheduleDetectorRestart(String reason) {
        if (stopRequested.get()) {
            return;
        }
        if (!restartInProgress.compareAndSet(false, true)) {
            return;
        }
        Thread restartThread = new Thread(() -> {
            try {
                if (stopRequested.get()) {
                    return;
                }
                log.info("已触发重启图像分类: deviceId={}, deviceName={}, algorithmId={}, reason={}",
                    deviceInfo.getId(), deviceInfo.getDeviceName(), algorithmId, reason);
                stopDetector();
                Thread.sleep(START_RETRY_DELAY_MILLIS);
                if (stopRequested.get()) {
                    return;
                }
                ClsModel model = this.detectorModel;
                if (model != null) {
                    startDetectionWithRetry(model);
                    return;
                }
                start();
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            } finally {
                restartInProgress.set(false);
            }
        });
        restartThread.setName("device-cls-detect-restart-" + deviceInfo.getId());
        restartThread.setDaemon(true);
        restartThread.start();
    }

    private void stopDetector() {
        ClsStreamDetector currentDetector = this.detector;
        if (currentDetector == null) {
            return;
        }
        if (!detectorClosed.compareAndSet(false, true)) {
            return;
        }
        try {
            currentDetector.stopDetection();
        } catch (Exception stopException) {
            log.warn("停止图像分类器失败", stopException);
        }
        try {
            currentDetector.close();
        } catch (Exception closeException) {
            log.warn("关闭图像分类器失败", closeException);
        }
    }

    private String prepareLocalModelPath() {
        DetectionSessionSupport.PreparedLocalModel preparedLocalModel = DetectionSessionSupport.prepareModelWithTempDirectory(
            modelSourcePath,
            algorithmId,
            deviceInfo,
            sshService,
            sshProperties,
            log,
            "cls-model-",
            CommonConstant.SYNSET_TXT,
            false
        );
        if (preparedLocalModel == null || StringUtils.isBlank(preparedLocalModel.getLocalModelPath())) {
            return null;
        }
        this.tempModelDirectory = preparedLocalModel.getTempModelDirectory();
        return preparedLocalModel.getLocalModelPath();
    }

    private void cleanupTempModelDirectory() {
        cleanupTempModelDirectory(this.tempModelDirectory);
        this.tempModelDirectory = null;
    }

    private void cleanupTempModelDirectory(Path directory) {
        DetectionSessionSupport.cleanupTempModelDirectory(directory, log);
    }

    private void createEvent(DeviceInfo deviceInfo,
                             Algorithm algorithm,
                             List<DetectionInfo> detectionInfos,
                             String snapshotPath) {
        String eventDesc = "设备 " + deviceInfo.getDeviceName() + " 分类到 " + detectionInfos.size() + " 个结果";
        DetectionSessionSupport.createEvent(eventManagementService, deviceInfo, algorithm, eventDesc, detectionInfos, snapshotPath);
    }
}
