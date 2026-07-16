package com.ruoyi.vlstream.test.vlstream.detection;

import ai.djl.modality.cv.Image;
import ai.djl.modality.cv.output.CategoryMask;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.smartjavaai.common.cv.SmartImageFactory;
import cn.smartjavaai.common.entity.R;
import cn.smartjavaai.common.enums.DeviceEnum;
import cn.smartjavaai.common.utils.ImageUtils;
import cn.smartjavaai.semseg.config.SemSegModelConfig;
import cn.smartjavaai.semseg.enums.SemSegModelEnum;
import cn.smartjavaai.semseg.model.SemSegModel;
import cn.smartjavaai.semseg.model.SemSegModelFactory;
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
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 单设备语义分割会话：周期性拉取图片（URL/本地路径）并执行语义分割，在产生结果时输出分割图并创建事件。
 */
@Slf4j
public class DeviceSemSegDetectionSession implements DeviceDetectionSession {

    public static DeviceEnum device = DeviceEnum.CPU;

    private static final long DETECTION_INTERVAL_MILLIS = 20000L;

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
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);

    private volatile SemSegModel detectorModel;
    private volatile File tempModelFile;
    private volatile Thread detectionThread;

    public DeviceSemSegDetectionSession(VlsSshProperties sshProperties,
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
        startDetectionLoop();
        return true;
    }

    public void stop(String reason) {
        stopRequested.set(true);
        log.info("停止语义分割: deviceId={}, deviceName={}, algorithmId={}, reason={}",
            deviceInfo.getId(), deviceInfo.getDeviceName(), algorithmId, reason);
        Thread runningThread = this.detectionThread;
        if (runningThread != null) {
            runningThread.interrupt();
        }
        cleanupTempModelFile();
    }

    private SemSegModel buildDetectorModel(String localModelPath) {
        SemSegModelConfig config = new SemSegModelConfig();
        config.setModelEnum(SemSegModelEnum.DEEPLABV3);
        config.setModelPath(localModelPath);
        config.setDevice(device);
        return SemSegModelFactory.getInstance().getModel(config);
    }

    private void startDetectionLoop() {
        Thread thread = new Thread(() -> {
            while (!stopRequested.get()) {
                try {
                    runDetectionOnce();
                } catch (Exception exception) {
                    log.warn("设备 {} 语义分割处理失败", deviceInfo.getDeviceName(), exception);
                }
                if (stopRequested.get()) {
                    return;
                }
                try {
                    Thread.sleep(DETECTION_INTERVAL_MILLIS);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        thread.setName("device-semseg-detect-" + deviceInfo.getId());
        thread.setDaemon(true);
        this.detectionThread = thread;
        thread.start();
    }

    private void runDetectionOnce() {
        if (!detectionInProgress.compareAndSet(false, true)) {
            return;
        }
        Image image = null;
        try {
            SemSegModel model = this.detectorModel;
            if (model == null) {
                return;
            }
            image = loadImage();
            if (image == null) {
                return;
            }
            R<CategoryMask> result = model.detect(image);
            if (result == null || !result.isSuccess() || result.getData() == null) {
                String message = result == null ? null : result.getMessage();
                log.warn("设备 {} 语义分割失败: {}", deviceInfo.getDeviceName(), message);
                return;
            }
            CategoryMask mask = result.getData();
            mask.drawMask(image, 150);

            LocalDateTime now = LocalDateTimeUtil.now();
            Path snapshot = outputDir.resolve("rtsp_semseg_" + System.currentTimeMillis() + ".png");
            File snapFile = snapshot.toFile();
            try {
                ImageUtils.save(image, snapFile.getName(), snapFile.getParent());
                log.info("语义分割截图已保存: deviceId={}, deviceName={}, time={}, path={}",
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
            createEvent(deviceInfo, algorithm, mask, fileResponseDto.getUrl());
            log.info("语义分割事件已创建: deviceId={}, deviceName={}, algorithmId={}",
                deviceInfo.getId(), deviceInfo.getDeviceName(), algorithmId);
        } finally {
            detectionInProgress.set(false);
            DetectionSessionSupport.closeQuietly(image, log);
        }
    }

    private Image loadImage() {
        String imagePath = StringUtils.trimToNull(deviceInfo.getImagePath());
        if (StringUtils.isNotBlank(imagePath)) {
            return loadImageFromPath(imagePath);
        }
        if (isHttpUrl(streamUrl)) {
            return loadImageFromPath(streamUrl);
        }
        log.warn("设备 {} 未配置可用图片地址，停止语义分割", deviceInfo.getDeviceName());
        stopRequested.set(true);
        return null;
    }

    private Image loadImageFromPath(String imagePath) {
        if (StringUtils.isBlank(imagePath)) {
            return null;
        }
        try {
            if (isHttpUrl(imagePath)) {
                return SmartImageFactory.getInstance().fromUrl(new URL(imagePath));
            }
            return SmartImageFactory.getInstance().fromFile(Paths.get(imagePath));
        } catch (Exception exception) {
            log.warn("设备 {} 加载图片失败: path={}", deviceInfo.getDeviceName(), imagePath, exception);
            return null;
        }
    }

    private boolean isHttpUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return false;
        }
        String lower = url.trim().toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private String prepareLocalModelPath() {
        DetectionSessionSupport.PreparedLocalModel preparedLocalModel = DetectionSessionSupport.prepareModelWithTempFile(
            modelSourcePath,
            algorithmId,
            deviceInfo,
            sshService,
            sshProperties,
            log,
            false
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

    private void createEvent(DeviceInfo deviceInfo,
                             Algorithm algorithm,
                             CategoryMask mask,
                             String snapshotPath) {
        String eventDesc = "设备 " + deviceInfo.getDeviceName() + " 完成语义分割";
        DetectionSessionSupport.createEvent(eventManagementService, deviceInfo, algorithm, eventDesc, mask, snapshotPath);
    }
}
