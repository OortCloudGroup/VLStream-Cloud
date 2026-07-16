package com.ruoyi.vlstream.test.vlstream.detection;

import ai.djl.modality.cv.Image;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.DataType;
import ai.djl.util.JsonUtils;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.smartjavaai.common.entity.DetectionInfo;
import cn.smartjavaai.common.entity.DetectionRectangle;
import cn.smartjavaai.common.enums.DeviceEnum;
import cn.smartjavaai.common.enums.VideoSourceType;
import cn.smartjavaai.common.utils.ImageUtils;
import cn.smartjavaai.objectdetection.config.PersonDetModelConfig;
import cn.smartjavaai.objectdetection.enums.PersonDetectorModelEnum;
import cn.smartjavaai.objectdetection.model.person.PersonDetModel;
import cn.smartjavaai.objectdetection.model.person.PersonDetModelFactory;
import cn.smartjavaai.objectdetection.stream.StreamDetectionListener;
import cn.smartjavaai.objectdetection.stream.StreamDetector;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 单设备人体检测会话：加载模型并对视频流进行人体检测与结果过滤，在产生结果时截图上传并创建事件。
 */
@Slf4j
public class DevicePersonDetectionSession implements DeviceDetectionSession {

    public static DeviceEnum device = DeviceEnum.CPU;

    private static final float BASE_SCORE_THRESHOLD = 0.5f;
    private static final float LOW_LIGHT_SCORE_THRESHOLD = 0.65f;
    private static final float DARK_SCORE_THRESHOLD = 0.8f;
    private static final double LOW_LIGHT_BRIGHTNESS = 70.0d;
    private static final double DARK_BRIGHTNESS = 45.0d;
    private static final double MIN_DETECTION_AREA_RATIO = 0.0015d;
    private static final double MIN_PERSON_ASPECT_RATIO = 1.4d;
    private static final double MIN_PERSON_HEIGHT_RATIO = 0.08d;
    private static final LocalTime NIGHT_START_TIME = LocalTime.of(22, 0);
    private static final LocalTime NIGHT_END_TIME = LocalTime.of(8, 0);
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

    private volatile StreamDetector detector;
    private volatile PersonDetModel detectorModel;

    private volatile File tempModelFile;

    public DevicePersonDetectionSession(VlsSshProperties sshProperties,
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
        this.detectorModel = getModel(localModelPath);
        startDetectionWithRetry(detectorModel);
        return true;
    }

    public void stop(String reason) {
        log.info("停止设备检测: deviceId={}, deviceName={}, reason={}", deviceInfo.getId(), deviceInfo.getDeviceName(), reason);
        stopDetector();
        cleanupTempModelFile();
    }

    private String prepareLocalModelPath() {
        DetectionSessionSupport.PreparedLocalModel preparedLocalModel = DetectionSessionSupport.prepareModelWithTempFile(
            modelSourcePath,
            algorithmId,
            deviceInfo,
            sshService,
            sshProperties,
            log,
            true
        );
        if (preparedLocalModel == null || StringUtils.isBlank(preparedLocalModel.getLocalModelPath())) {
            return null;
        }
        this.tempModelFile = preparedLocalModel.getTempModelFile();
        return preparedLocalModel.getLocalModelPath();
    }

    private void cleanupTempModelFile() {
        File modelFile = this.tempModelFile;
        this.tempModelFile = null;
        if (modelFile == null) {
            return;
        }
        DetectionSessionSupport.cleanupTempModelFile(modelFile, log);
    }

    private PersonDetModel getModel(String localModelPath) {
        PersonDetModelConfig config = new PersonDetModelConfig();
        config.setModelEnum(PersonDetectorModelEnum.YOLOV8_PERSON);
        config.setModelPath(localModelPath);
        config.setTopK(10);
        config.setDevice(device);
        config.setThreshold(0.8f);
        return PersonDetModelFactory.getInstance().getModel(config);
    }

    private void startDetectionWithRetry(PersonDetModel detectorModel) {
        int retryTimes = 0;
        while (true) {
            StreamDetector streamDetector = buildStreamDetector(detectorModel);
            detectorClosed.set(false);
            this.detector = streamDetector;
            try {
                streamDetector.startDetection();
                log.info("开始检测视频流: deviceId={}, deviceName={}", deviceInfo.getId(), deviceInfo.getDeviceName());
                return;
            } catch (Exception startException) {
                retryTimes++;
                log.error("启动检测器失败: deviceId={}, deviceName={}, retryTimes={}, error={}",
                    deviceInfo.getId(), deviceInfo.getDeviceName(), retryTimes, startException.getMessage(), startException);
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

    private StreamDetector buildStreamDetector(PersonDetModel detectorModel) {
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
                        LocalDateTime now = LocalDateTimeUtil.now();
                        if (isNightTime(now)) {
                            log.info("夜间时段跳过检测: deviceId={}, deviceName={}, time={}", deviceInfo.getId(), deviceInfo.getDeviceName(), now);
                            return;
                        }
                        if (image == null) {
                            return;
                        }
                        double brightness = estimateBrightness(image);
                        if (isTooDark(brightness)) {
                            log.info("亮度过低，跳过检测: deviceId={}, deviceName={}, brightness={}", deviceInfo.getId(), deviceInfo.getDeviceName(), brightness);
                            return;
                        }
                        float minScore = resolveMinScore(brightness);
                        List<DetectionInfo> filteredDetections = filterDetections(detectionInfoList, image, minScore);
                        if (filteredDetections.isEmpty()) {
                            log.info("检测结果被过滤: deviceId={}, deviceName={}, rawSize={}, brightness={}, minScore={}",
                                deviceInfo.getId(), deviceInfo.getDeviceName(), detectionInfoList.size(), brightness, minScore);
                            return;
                        }
                        log.info("检测时间: {}", LocalDateTimeUtil.now());
                        log.info("检测结果: {}", JsonUtils.toJson(filteredDetections));

                        snapshotImage = DetectionSessionSupport.copyForSnapshot(image, log);
                        if (snapshotImage == null) {
                            log.warn("设备 {} 复制截图失败，跳过", deviceInfo.getDeviceName());
                            return;
                        }
                        ImageUtils.drawRectAndText(snapshotImage, filteredDetections);

                        Path snapshot = outputDir.resolve("rtsp_detect_" + System.currentTimeMillis() + ".png");
                        File snapFile = snapshot.toFile();
                        try {
                            ImageUtils.save(snapshotImage, snapFile.getName(), snapFile.getParent());
                        } catch (Exception saveException) {
                            log.warn("设备 {} 保存截图失败", deviceInfo.getDeviceName(), saveException);
                            return;
                        }

                        FileResponseDto fileResponseDto = DetectionSessionSupport.uploadSnapshot(fileUploadService, snapFile);
                        if (fileResponseDto == null || StringUtils.isBlank(fileResponseDto.getUrl())) {
                            log.warn("设备 {} 上传截图失败，跳过事件创建", deviceInfo.getDeviceName());
                            return;
                        }

                        createEvent(deviceInfo, algorithm, filteredDetections, fileResponseDto.getUrl());
                        log.info("设备 {} 事件已创建，检测到 {} 个目标", deviceInfo.getDeviceName(), filteredDetections.size());
                    } catch (Exception detectException) {
                        log.warn("设备 {} 检测处理失败", deviceInfo.getDeviceName(), detectException);
                        scheduleDetectorRestart("detectException");
                    } finally {
                        detectionInProgress.set(false);
                        DetectionSessionSupport.closeQuietly(snapshotImage, log);
                    }
                }

                @Override
                public void onStreamEnded() {
                    log.info("视频流检测结束: deviceId={}, deviceName={}", deviceInfo.getId(), deviceInfo.getDeviceName());
                    stopDetector();
                }

                @Override
                public void onStreamDisconnected() {
                    log.info("视频流断开连接: deviceId={}, deviceName={}", deviceInfo.getId(), deviceInfo.getDeviceName());
                    stopDetector();
                }
            })
            .build();
    }

    private void scheduleDetectorRestart(String reason) {
        if (!restartInProgress.compareAndSet(false, true)) {
            return;
        }
        Thread restartThread = new Thread(() -> {
            try {
                log.info("已触发重启检测: deviceId={}, deviceName={}, reason={}", deviceInfo.getId(), deviceInfo.getDeviceName(), reason);
                stopDetector();
                Thread.sleep(START_RETRY_DELAY_MILLIS);
                PersonDetModel model = this.detectorModel;
                if (model != null) {
                    startDetectionWithRetry(model);
                }
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
            } finally {
                restartInProgress.set(false);
            }
        });
        restartThread.setName("device-person-check-restart-" + deviceInfo.getId());
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
            log.warn("停止检测器失败", stopException);
        }
        try {
            currentDetector.close();
        } catch (Exception closeException) {
            log.warn("关闭检测器失败", closeException);
        }
    }

    private List<DetectionInfo> filterDetections(List<DetectionInfo> detectionInfoList, Image image, float minScore) {
        if (detectionInfoList == null || detectionInfoList.isEmpty()) {
            return Collections.emptyList();
        }
        int imageArea = resolveImageArea(image);
        int imageHeight = resolveImageHeight(image);
        return detectionInfoList.stream()
            .filter(Objects::nonNull)
            .filter(info -> info.getScore() >= minScore)
            .filter(info -> isValidDetectionSize(info, imageArea, imageHeight))
            .collect(java.util.stream.Collectors.toList());
    }

    private boolean isValidDetectionSize(DetectionInfo detectionInfo, int imageArea, int imageHeight) {
        if (detectionInfo == null) {
            return false;
        }
        DetectionRectangle rectangle = detectionInfo.getDetectionRectangle();
        if (rectangle == null) {
            return false;
        }
        int width = rectangle.getWidth();
        int height = rectangle.getHeight();
        if (width <= 0 || height <= 0) {
            return false;
        }
        double aspectRatio = (double) height / (double) width;
        if (aspectRatio < MIN_PERSON_ASPECT_RATIO) {
            return false;
        }
        if (imageHeight > 0) {
            double heightRatio = (double) height / (double) imageHeight;
            if (heightRatio < MIN_PERSON_HEIGHT_RATIO) {
                return false;
            }
        }
        if (imageArea <= 0) {
            return true;
        }
        double areaRatio = (double) width * (double) height / (double) imageArea;
        return areaRatio >= MIN_DETECTION_AREA_RATIO;
    }

    private int resolveImageArea(Image image) {
        if (image == null) {
            return 0;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        if (width <= 0 || height <= 0) {
            return 0;
        }
        return width * height;
    }

    private int resolveImageHeight(Image image) {
        if (image == null) {
            return 0;
        }
        int height = image.getHeight();
        return Math.max(height, 0);
    }

    private float resolveMinScore(double brightness) {
        if (brightness < 0) {
            return BASE_SCORE_THRESHOLD;
        }
        if (brightness <= DARK_BRIGHTNESS) {
            return DARK_SCORE_THRESHOLD;
        }
        if (brightness <= LOW_LIGHT_BRIGHTNESS) {
            return LOW_LIGHT_SCORE_THRESHOLD;
        }
        return BASE_SCORE_THRESHOLD;
    }

    private boolean isTooDark(double brightness) {
        return brightness >= 0 && brightness <= DARK_BRIGHTNESS;
    }

    private double estimateBrightness(Image image) {
        if (image == null) {
            return -1d;
        }
        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray array = image.toNDArray(manager);
            if (array == null) {
                return -1d;
            }
            NDArray mean = array.toType(DataType.FLOAT32, false).mean();
            return mean.getFloat();
        } catch (Exception exception) {
            log.warn("计算图像亮度失败", exception);
            return -1d;
        }
    }

    private boolean isNightTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return false;
        }
        LocalTime time = dateTime.toLocalTime();
        return !time.isBefore(NIGHT_START_TIME) || time.isBefore(NIGHT_END_TIME);
    }

    private void createEvent(DeviceInfo deviceInfo,
                             Algorithm algorithm,
                             List<DetectionInfo> detectionInfos,
                             String snapshotPath) {
        String eventDesc = "设备 " + deviceInfo.getDeviceName() + " 检测到 " + detectionInfos.size() + " 个目标";
        DetectionSessionSupport.createEvent(eventManagementService, deviceInfo, algorithm, eventDesc, detectionInfos, snapshotPath);
    }
}
