package org.springblade.vlstream.detection;

import ai.djl.modality.cv.Image;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.smartjavaai.common.cv.SmartImageFactory;
import cn.smartjavaai.common.entity.DetectionInfo;
import cn.smartjavaai.common.enums.DeviceEnum;
import cn.smartjavaai.common.enums.VideoSourceType;
import cn.smartjavaai.common.utils.ImageUtils;
import cn.smartjavaai.instanceseg.config.InstanceSegModelConfig;
import cn.smartjavaai.instanceseg.enums.InstanceSegModelEnum;
import cn.smartjavaai.instanceseg.model.InstanceSegModel;
import cn.smartjavaai.instanceseg.model.InstanceSegModelFactory;
import cn.smartjavaai.instanceseg.stream.InstanceSegStreamDetectionListener;
import cn.smartjavaai.instanceseg.stream.InstanceSegStreamDetector;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springblade.common.constant.CommonConstant;
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
 * Single-device instance segmentation session: load the model, perform instance segmentation on the video stream, take a screenshot, upload it, and create an event when a result is generated; support abnormal restart and resource cleanup.
 */
@Slf4j
public class DeviceInstanceSegDetectionSession implements DeviceDetectionSession {

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

    private volatile InstanceSegStreamDetector detector;
    private volatile InstanceSegModel detectorModel;
    private volatile Path tempModelDirectory;

    public DeviceInstanceSegDetectionSession(VlsSshProperties sshProperties,
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
        log.info("Stop instance segmentation: deviceId={}, deviceName={}, algorithmId={}, reason={}",
            deviceInfo.getId(), deviceInfo.getDeviceName(), algorithmId, reason);
        stopDetector();
        cleanupTempModelDirectory();
    }

    private InstanceSegModel buildDetectorModel(String localModelPath) {
        InstanceSegModelConfig config = new InstanceSegModelConfig();
        config.setModelEnum(InstanceSegModelEnum.SEG_YOLO11N_ONNX);
        config.setModelPath(localModelPath);
        config.setDevice(device);
        config.setThreshold(0.5f);
        return InstanceSegModelFactory.getInstance().getModel(config);
    }

    private void startDetectionWithRetry(InstanceSegModel detectorModel) {
        int retryTimes = 0;
        while (true) {
            InstanceSegStreamDetector streamDetector = buildStreamDetector(detectorModel);
            detectorClosed.set(false);
            this.detector = streamDetector;
            try {
                streamDetector.startDetection();
                log.info("Start instance segmentation: deviceId={}, deviceName={}, algorithmId={}, streamUrl={}",
                    deviceInfo.getId(), deviceInfo.getDeviceName(), algorithmId, streamUrl);
                return;
            } catch (Exception startException) {
                retryTimes++;
                log.error("Failed to start instance segmenter: deviceId={}, deviceName={}, algorithmId={}, retryTimes={}, error={}",
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

    private InstanceSegStreamDetector buildStreamDetector(InstanceSegModel detectorModel) {
        return new InstanceSegStreamDetector.Builder()
            .sourceType(VideoSourceType.STREAM)
            .streamUrl(streamUrl)
            .frameDetectionInterval(20)
            .detectorModel(detectorModel)
            .listener(new InstanceSegStreamDetectionListener() {
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
                        Path snapshot = outputDir.resolve("rtsp_instance_seg_" + System.currentTimeMillis() + ".png");
                        File snapFile = snapshot.toFile();
                        try {
                            ImageUtils.save(snapshotImage, snapFile.getName(), snapFile.getParent());
                            log.info("Instance segmentation screenshot saved: deviceId={}, deviceName={}, time={}, path={}",
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
                        log.info("Instance segmentation event created: deviceId={}, deviceName={}, algorithmId={}, detections={}",
                            deviceInfo.getId(), deviceInfo.getDeviceName(), algorithmId, detectionInfoList.size());
                    } catch (Exception detectException) {
                        log.warn("Device {} instance segmentation callback processing failed", deviceInfo.getDeviceName(), detectException);
                        scheduleDetectorRestart("detectException");
                    } finally {
                        detectionInProgress.set(false);
                        DetectionSessionSupport.closeQuietly(snapshotImage, log);
                    }
                }

                @Override
                public void onStreamEnded() {
                    log.info("Instance segmentation video stream ended: deviceId={}, deviceName={}, algorithmId={}",
                        deviceInfo.getId(), deviceInfo.getDeviceName(), algorithmId);
                    scheduleDetectorRestart("streamEnded");
                }

                @Override
                public void onStreamDisconnected() {
                    log.info("Instance segmentation video stream disconnected: deviceId={}, deviceName={}, algorithmId={}",
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
                log.info("Restart triggered for instance segmentation: deviceId={}, deviceName={}, algorithmId={}, reason={}",
                    deviceInfo.getId(), deviceInfo.getDeviceName(), algorithmId, reason);
                stopDetector();
                Thread.sleep(START_RETRY_DELAY_MILLIS);
                if (stopRequested.get()) {
                    return;
                }
                InstanceSegModel model = this.detectorModel;
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
        restartThread.setName("device-instance-seg-restart-" + deviceInfo.getId());
        restartThread.setDaemon(true);
        restartThread.start();
    }

    private void stopDetector() {
        InstanceSegStreamDetector currentDetector = this.detector;
        if (currentDetector == null) {
            return;
        }
        if (!detectorClosed.compareAndSet(false, true)) {
            return;
        }
        try {
            currentDetector.stopDetection();
        } catch (Exception stopException) {
            log.warn("Failed to stop instance segmenter", stopException);
        }
        try {
            currentDetector.close();
        } catch (Exception closeException) {
            log.warn("Failed to close instance segmenter", closeException);
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
            "instance-seg-model-",
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
        String eventDesc = "Device" + deviceInfo.getDeviceName() + "Split to" + detectionInfos.size() + "targets";
        DetectionSessionSupport.createEvent(eventManagementService, deviceInfo, algorithm, eventDesc, detectionInfos, snapshotPath);
    }
}
