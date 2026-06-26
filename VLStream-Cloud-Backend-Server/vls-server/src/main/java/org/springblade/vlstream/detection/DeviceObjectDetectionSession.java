package org.springblade.vlstream.detection;

import ai.djl.modality.cv.Image;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.smartjavaai.common.entity.DetectionInfo;
import cn.smartjavaai.common.enums.DeviceEnum;
import cn.smartjavaai.common.enums.VideoSourceType;
import cn.smartjavaai.common.utils.ImageUtils;
import cn.smartjavaai.objectdetection.config.DetectorModelConfig;
import cn.smartjavaai.objectdetection.enums.DetectorModelEnum;
import cn.smartjavaai.objectdetection.model.DetectorModel;
import cn.smartjavaai.objectdetection.stream.StreamDetectionListener;
import cn.smartjavaai.objectdetection.stream.StreamDetector;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springblade.common.constant.CommonConstant;
import org.springblade.common.enums.YesNoEnum;
import org.springblade.modules.system.service.IFileUploadService;
import org.springblade.vlstream.config.VlsSshProperties;
import org.springblade.vlstream.pojo.dto.FileResponseDto;
import org.springblade.vlstream.pojo.entity.Algorithm;
import org.springblade.vlstream.pojo.entity.DeviceInfo;
import org.springblade.vlstream.service.IVlsEventManagementService;
import org.springblade.vlstream.service.SSHService;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Single-device object detection session: load the model, perform object detection on the video stream, take a screenshot, upload it, and create an event when a result is generated; support abnormal restart and resource cleanup.
 */
@Slf4j
public class DeviceObjectDetectionSession implements DeviceDetectionSession {

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

    private volatile StreamDetector detector;
    private volatile DetectorModel detectorModel;
    private volatile Path tempModelDirectory;

    public DeviceObjectDetectionSession(VlsSshProperties sshProperties,
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
        log.info("Stop object detection: deviceId={}, deviceName={}, algorithmId={}, reason={}",
            deviceInfo.getId(), deviceInfo.getDeviceName(), algorithmId, reason);
        stopDetector();
        cleanupTempModelDirectory();
    }

    private DetectorModel buildDetectorModel(String localModelPath) {
        DetectorModelConfig config = new DetectorModelConfig();
        DetectorModelEnum modelEnum = YesNoEnum.YES.equals(algorithm.getIsSystem())
            ? DetectorModelEnum.YOLOV8_OFFICIAL_ONNX
            : DetectorModelEnum.YOLOV8_CUSTOM_ONNX;
        config.setModelEnum(modelEnum);
        config.setModelPath(localModelPath);
        config.setTopK(100);
        config.setDevice(device);
        config.setThreshold(0.2f);
        DetectorModel model = new DetectorModel();
        model.loadModel(config);
        return model;
    }

    private void startDetectionWithRetry(DetectorModel detectorModel) {
        int retryTimes = 0;
        while (true) {
            StreamDetector streamDetector = buildStreamDetector(detectorModel);
            detectorClosed.set(false);
            this.detector = streamDetector;
            try {
                streamDetector.startDetection();
                log.info("Start object detection: deviceId={}, deviceName={}, algorithmId={}, streamUrl={}",
                    deviceInfo.getId(), deviceInfo.getDeviceName(), algorithmId, streamUrl);
                return;
            } catch (Exception startException) {
                retryTimes++;
                log.error("Failed to start object detector: deviceId={}, deviceName={}, algorithmId={}, retryTimes={}, error={}",
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

    private StreamDetector buildStreamDetector(DetectorModel detectorModel) {
        return new StreamDetector.Builder()
            .sourceType(VideoSourceType.STREAM)
            .streamUrl(streamUrl)
            .frameDetectionInterval(5)
            .detectorModel(detectorModel)
            .listener(new StreamDetectionListener() {
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
                            log.warn("Device {} failed to copy screenshot, skipping", deviceInfo.getDeviceName());
                            return;
                        }
                        ImageUtils.drawRectAndText(snapshotImage, detectionInfoList);

                        LocalDateTime now = LocalDateTimeUtil.now();
                        Path snapshot = outputDir.resolve("rtsp_detect_" + System.currentTimeMillis() + ".png");
                        File snapFile = snapshot.toFile();
                        try {
                            ImageUtils.save(snapshotImage, snapFile.getName(), snapFile.getParent());
                            log.info("Object detection screenshot saved: deviceId={}, deviceName={}, time={}, path={}",
                                deviceInfo.getId(), deviceInfo.getDeviceName(), now, snapshot.toAbsolutePath());
                        } catch (Exception saveException) {
                            log.warn("Device {} failed to save screenshot", deviceInfo.getDeviceName(), saveException);
                            return;
                        }

                        FileResponseDto fileResponseDto = DetectionSessionSupport.uploadSnapshot(fileUploadService, snapFile);
                        if (fileResponseDto == null || StringUtils.isBlank(fileResponseDto.getUrl())) {
                            log.warn("Device {} failed to upload screenshot, skipping event creation", deviceInfo.getDeviceName());
                            return;
                        }
                        createEvent(deviceInfo, algorithm, detectionInfoList, fileResponseDto.getUrl());
                        log.info("Object detection event created: deviceId={}, deviceName={}, algorithmId={}, detections={}",
                            deviceInfo.getId(), deviceInfo.getDeviceName(), algorithmId, detectionInfoList.size());
                    } catch (Exception detectException) {
                        log.warn("Device {} object detection callback processing failed", deviceInfo.getDeviceName(), detectException);
                        scheduleDetectorRestart("detectException");
                    } finally {
                        detectionInProgress.set(false);
                        DetectionSessionSupport.closeQuietly(snapshotImage, log);
                    }
                }

                @Override
                public void onStreamEnded() {
                    log.info("Object detection video stream ended: deviceId={}, deviceName={}, algorithmId={}",
                        deviceInfo.getId(), deviceInfo.getDeviceName(), algorithmId);
                    scheduleDetectorRestart("streamEnded");
                }

                @Override
                public void onStreamDisconnected() {
                    log.info("Object detection video stream disconnected: deviceId={}, deviceName={}, algorithmId={}",
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
                log.info("Restart triggered for object detection: deviceId={}, deviceName={}, algorithmId={}, reason={}",
                    deviceInfo.getId(), deviceInfo.getDeviceName(), algorithmId, reason);
                stopDetector();
                Thread.sleep(START_RETRY_DELAY_MILLIS);
                if (stopRequested.get()) {
                    return;
                }
                DetectorModel model = this.detectorModel;
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
        restartThread.setName("device-object-detect-restart-" + deviceInfo.getId());
        restartThread.setDaemon(true);
        restartThread.start();
    }

    private void stopDetector() {
        StreamDetector currentDetector = this.detector;
        if (currentDetector == null) {
            return;
        }
        if (!detectorClosed.compareAndSet(false, true)) {
            return;
        }
        try {
            currentDetector.stopDetection();
        } catch (Exception stopException) {
            log.warn("Failed to stop object detector", stopException);
        }
        try {
            currentDetector.close();
        } catch (Exception closeException) {
            log.warn("Failed to stop object detector", closeException);
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
            "object-detect-model-",
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
        String eventDesc = "Device" + deviceInfo.getDeviceName() + "Detected" + detectionInfos.size() + "targets";
        DetectionSessionSupport.createEvent(eventManagementService, deviceInfo, algorithm, eventDesc, detectionInfos, snapshotPath);
    }
}
