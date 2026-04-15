package com.vlstream;

import ai.djl.util.JsonUtils;
import cn.hutool.core.io.FileUtil;
import cn.smartjavaai.common.entity.DetectionInfo;
import cn.smartjavaai.common.entity.DetectionRectangle;
import cn.smartjavaai.common.entity.DetectionResponse;
import cn.smartjavaai.common.entity.R;
import cn.smartjavaai.common.utils.ImageUtils;
import cn.smartjavaai.face.config.FaceDetConfig;
import cn.smartjavaai.face.enums.FaceDetModelEnum;
import cn.smartjavaai.face.factory.FaceDetModelFactory;
import cn.smartjavaai.face.model.facedect.FaceDetModel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.vlstream.dto.FileResponseDto;
import com.vlstream.entity.Algorithm;
import com.vlstream.entity.DeviceInfo;
import com.vlstream.entity.EventManagement;
import com.vlstream.service.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class DeviceModelCheckRunner implements org.springframework.boot.CommandLineRunner {

    private static final int MAX_HEADLESS_HEIGHT = 720;
    private static final String DEFAULT_MODEL_PATH = "D:\\code\\SmartJavaAI\\examples\\face-example\\src\\main\\resources\\model\\retinaface.pt";
    private static final DateTimeFormatter SNAPSHOT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Value("${spring.jackson.time-zone:GMT+8}")
    private String timeZoneId;

    private ZoneId reportZoneId = ZoneId.of("GMT+8");

    @Value("${vlstream.ssh.host}")
    private String sshHost;

    @Value("${vlstream.ssh.port}")
    private Integer sshPort;

    @Value("${vlstream.ssh.username}")
    private String sshUsername;

    @Value("${vlstream.ssh.password}")
    private String sshPassword;

    @Resource
    private DeviceInfoService deviceInfoService;

    @Resource
    private AlgorithmService algorithmService;

    @Resource
    private EventManagementService eventManagementService;

    @Resource
    private SSHService sshService;

    @Resource
    private IFileUploadService fileUploadService;

    @PostConstruct
    public void initTimeZone() {
        try {
            reportZoneId = ZoneId.of(timeZoneId);
        } catch (Exception ex) {
            log.warn("Invalid time zone [{}], fallback to {}", timeZoneId, reportZoneId);
        }
    }

    @Override
    public void run(String... args) {
        List<DeviceInfo> devices = deviceInfoService.list(new LambdaQueryWrapper<DeviceInfo>()
                .isNotNull(DeviceInfo::getAlgorithmId)
                .ne(DeviceInfo::getAlgorithmId, "")
                .eq(DeviceInfo::getDeleted, 0));
        if (devices.isEmpty()) {
            log.info("No devices found with dispatched algorithms.");
            return;
        }

        avutil.av_log_set_level(avutil.AV_LOG_ERROR);
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(devices.size(), 4));
        for (DeviceInfo device : devices) {
            executor.submit(() -> processDeviceStream(device));
        }
    }

    private void processDeviceStream(DeviceInfo deviceInfo) {
        String streamUrl = StringUtils.isNotBlank(deviceInfo.getRtspUrl()) ? deviceInfo.getRtspUrl() : deviceInfo.getStreamUrl();
        if (StringUtils.isBlank(streamUrl)) {
            log.warn("Device {} has no RTSP/stream URL, skip.", deviceInfo.getDeviceName());
            return;
        }

        Long algorithmId = parseAlgorithmId(deviceInfo.getAlgorithmId());
        if (algorithmId == null) {
            log.warn("Device {} algorithm id [{}] invalid, skip.", deviceInfo.getDeviceName(), deviceInfo.getAlgorithmId());
            return;
        }
        Algorithm algorithm = algorithmService.getById(algorithmId);
        if (algorithm == null) {
            log.warn("Algorithm {} not found for device {}, skip.", algorithmId, deviceInfo.getDeviceName());
            return;
        }
        String modelPath = algorithm.getModelFilePath();

        SSHService.SSHExecutionResult fileResult = sshService.executeCommand(
                sshHost,
                sshPort,
                sshUsername,
                sshPassword,
                String.format("cd /data/work/ultralytics_yolov8-main && base64 %s", modelPath)
        );

        String localModelPath = null;
        if (fileResult.isSuccess() && !fileResult.getOutput().trim().isEmpty()) {
            String base64Content = fileResult.getOutput().trim().replaceAll("\\s+", "");
            log.info("Base64 length: {}", base64Content.length());
            byte[] fileContent = java.util.Base64.getDecoder().decode(base64Content);

            File modelFile = FileUtil.createTempFile();
            FileUtil.writeBytes(fileContent, modelFile);
            localModelPath = modelFile.getAbsolutePath();
            log.info("Model file ready at {}", localModelPath);
        } else if (Files.exists(Paths.get(DEFAULT_MODEL_PATH))) {
            log.warn("Remote model fetch failed, fallback to local model: {}", DEFAULT_MODEL_PATH);
            localModelPath = DEFAULT_MODEL_PATH;
        } else {
            log.error("Model file not available, path: {}", modelPath);
            return;
        }

        FaceDetConfig config = new FaceDetConfig();
        config.setModelEnum(FaceDetModelEnum.RETINA_FACE);
        config.setModelPath(localModelPath);
        FaceDetModel faceModel = FaceDetModelFactory.getInstance().getModel(config);

        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(streamUrl);
        grabber.setOption("rtsp_transport", "tcp");
        grabber.setOption("stimeout", "10000000");
        ExecutorService detectExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("face-detect-worker-" + deviceInfo.getId());
            t.setDaemon(true);
            return t;
        });

        try {
            grabber.start();
            boolean headless = GraphicsEnvironment.isHeadless();
            Dimension screenSize = headless ? null : Toolkit.getDefaultToolkit().getScreenSize();

            Path outputDir = Paths.get("output", "events", String.valueOf(deviceInfo.getId()));
            Files.createDirectories(outputDir);
            AtomicInteger snapshotIndex = new AtomicInteger(0);
            AtomicBoolean detecting = new AtomicBoolean(false);

            Frame frame;
            while ((frame = grabber.grabImage()) != null) {
                BufferedImage image = Java2DFrameUtils.toBufferedImage(frame);
                if (image == null) {
                    continue;
                }

                BufferedImage resized = resizeForHeadless(image, headless, screenSize);

                if (detecting.compareAndSet(false, true)) {
                    BufferedImage detectionImage = new BufferedImage(resized.getWidth(), resized.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
                    Graphics2D detectionGraphics = detectionImage.createGraphics();
                    detectionGraphics.drawImage(resized, 0, 0, resized.getWidth(), resized.getHeight(), null);
                    detectionGraphics.dispose();

                    detectExecutor.execute(() -> {
                        try {
                            R<DetectionResponse> detectedResult = faceModel.detect(detectionImage);
                            log.info("Async detect result: " + JsonUtils.toJson(detectedResult));
                            if (!detectedResult.isSuccess() || detectedResult.getData() == null) {
                                return;
                            }

                            List<DetectionInfo> detectionInfos = detectedResult.getData().getDetectionInfoList();
                            if (detectionInfos != null && !detectionInfos.isEmpty()) {
                                for (DetectionInfo info : detectionInfos) {
                                    DetectionRectangle rect = info.getDetectionRectangle();
                                    String text = info.getScore() > 0 ? String.valueOf(info.getScore()) : null;
                                    ImageUtils.drawImageRectWithText(detectionImage, rect, text, Color.red);
                                }

                                LocalDateTime snapshotTime = nowInReportZone();
                                int idx = snapshotIndex.incrementAndGet();
                                Path snapshot = outputDir.resolve("rtsp_detect_" + snapshotTime.format(SNAPSHOT_TIME_FORMATTER) + "_" + idx + ".png");
                                File snapFile = snapshot.toFile();
                                try {
                                    ImageIO.write(detectionImage, "png", snapFile);
                                    log.info("Saved snapshot: {}", snapshot.toAbsolutePath());
                                } catch (Exception writeEx) {
                                    log.warn("Save snapshot failed", writeEx);
                                }

                                FileResponseDto fileResponseDto = fileUploadService.uploadFile("818301f0e77f4cd8a117414cbeb32d9e", "5f0de11687d744bc95e84e207d319493", snapFile);
                                createEvent(deviceInfo, algorithm, detectionInfos, fileResponseDto.getUrl(), snapshotTime);
                                log.info("Event created for device {} with {} detections", deviceInfo.getDeviceName(), detectionInfos.size());
                            }
                        } catch (Exception detectEx) {
                            log.warn("Async face detect failed", detectEx);
                        } finally {
                            detecting.set(false);
                        }
                    });
                }
            }
        } catch (Exception e) {
            log.warn("Processing stream failed for device {}", deviceInfo.getDeviceName(), e);
        } finally {
            try {
                detectExecutor.shutdownNow();
            } catch (Exception ignore) {
                // ignore
            }
            try {
                grabber.stop();
            } catch (Exception ignore) {
                // ignore
            }
            try {
                grabber.release();
            } catch (Exception ignore) {
                // ignore
            }
        }

        log.info("Model ready for detection: {}", localModelPath);
    }

    private BufferedImage resizeForHeadless(BufferedImage image, boolean headless, Dimension screenSize) {
        double ratio = (double) image.getWidth() / image.getHeight();
        int height;
        int width;
        if (headless) {
            height = Math.min(image.getHeight(), MAX_HEADLESS_HEIGHT);
            width = (int) (height * ratio);
        } else {
            height = (int) (screenSize.height * 0.65f);
            width = (int) (height * ratio);
            if (width > screenSize.width) {
                width = screenSize.width;
            }
        }
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D graphics2D = resizedImage.createGraphics();
        graphics2D.drawImage(image, 0, 0, width, height, null);
        graphics2D.dispose();
        return resizedImage;
    }

    private void createEvent(DeviceInfo device,
                             Algorithm algorithm,
                             List<DetectionInfo> detectionInfos,
                             String snapshotPath,
                             LocalDateTime reportTime) {
        EventManagement event = new EventManagement();
        event.setEventDesc("Device " + device.getDeviceName() + " detected " + detectionInfos.size() + " target(s).");
        event.setEventType("algorithm_alert");
        event.setReportLocation(StringUtils.defaultIfBlank(device.getLocation(), device.getAddress()));
        event.setReportDevice(device.getDeviceId());
        event.setReportImg(snapshotPath);
        event.setReportTime(reportTime);
        event.setEventLevel("medium");
        event.setEventStatus("pending");
        event.setEventData(JsonUtils.toJson(detectionInfos));
        event.setHandleResult("algorithm: " + algorithm.getName());
        eventManagementService.createEvent(event);
    }

    private LocalDateTime nowInReportZone() {
        return LocalDateTime.now(reportZoneId);
    }

    private Long parseAlgorithmId(String algorithmId) {
        try {
            return Long.valueOf(algorithmId);
        } catch (Exception e) {
            return null;
        }
    }
}
