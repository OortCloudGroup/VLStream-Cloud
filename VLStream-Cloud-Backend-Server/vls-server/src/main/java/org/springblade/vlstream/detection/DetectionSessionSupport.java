package org.springblade.vlstream.detection;

import ai.djl.modality.cv.BufferedImageFactory;
import ai.djl.modality.cv.Image;
import cn.hutool.core.io.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springblade.modules.system.service.IFileUploadService;
import org.springblade.vlstream.config.VlsSshProperties;
import org.springblade.vlstream.enums.EventLevelEnum;
import org.springblade.vlstream.enums.EventStatusEnum;
import org.springblade.vlstream.pojo.dto.FileResponseDto;
import org.springblade.vlstream.pojo.entity.Algorithm;
import org.springblade.vlstream.pojo.entity.DeviceInfo;
import org.springblade.vlstream.pojo.entity.EventManagement;
import org.springblade.vlstream.service.IVlsEventManagementService;
import org.springblade.vlstream.service.SSHService;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Date;

/**
 * General session detection tools: provide auxiliary capabilities such as screenshot copying and silent closing of resources.
 */
public final class DetectionSessionSupport {

    private DetectionSessionSupport() {

    }

    public static Image copyForSnapshot(Image image, Logger logger) {
        if (image == null) {
            return null;
        }
        File tempFile = FileUtil.createTempFile();
        try (OutputStream outputStream = Files.newOutputStream(tempFile.toPath())) {
            image.save(outputStream, "png");
            Image copiedImage = BufferedImageFactory.getInstance().fromFile(tempFile.toPath());
            FileUtil.del(tempFile);
            return copiedImage;
        } catch (Exception exception) {
            if (logger != null) {
                logger.warn("Failed to copy screenshot image", exception);
            }
            FileUtil.del(tempFile);
            return null;
        }
    }

    public static void closeQuietly(Object resource, Logger logger) {
        if (!(resource instanceof AutoCloseable autoCloseable)) {
            return;
        }
        try {
            autoCloseable.close();
        } catch (Exception closeException) {
            if (logger != null) {
                logger.debug("Failed to close resources", closeException);
            }
        }
    }

    public static PreparedLocalModel prepareModelWithTempFile(String modelSourcePath,
                                                              Long algorithmId,
                                                              DeviceInfo deviceInfo,
                                                              SSHService sshService,
                                                              VlsSshProperties sshProperties,
                                                              Logger logger,
                                                              boolean useMimeDecoder) {
        if (StringUtils.isBlank(modelSourcePath)) {
            if (logger != null && deviceInfo != null) {
                logger.warn("Device {}'s algorithm {} model path is empty", deviceInfo.getDeviceName(), algorithmId);
            }
            return null;
        }
        if (FileUtil.exist(modelSourcePath)) {
            File localModelFile = new File(modelSourcePath);
            String localModelPath = localModelFile.getAbsolutePath();
            if (logger != null && deviceInfo != null) {
                logger.info("Device {} uses local model path: algorithmId={}, modelPath={}", deviceInfo.getDeviceName(), algorithmId, localModelPath);
            }
            return new PreparedLocalModel(localModelPath, null, null);
        }

        byte[] modelContent = fetchRemoteFileContent(sshService, sshProperties, modelSourcePath, "model", deviceInfo, logger, useMimeDecoder);
        if (modelContent == null) {
            if (logger != null && deviceInfo != null) {
                logger.error("Device {}'s algorithm {} model file is unavailable: modelPath={}", deviceInfo.getDeviceName(), algorithmId, modelSourcePath);
            }
            return null;
        }

        File tempModelFile = FileUtil.createTempFile();
        FileUtil.writeBytes(modelContent, tempModelFile);
        String localModelPath = tempModelFile.getAbsolutePath();
        if (logger != null && deviceInfo != null) {
            logger.info("Device {} model file is ready: algorithmId={}, modelPath={}", deviceInfo.getDeviceName(), algorithmId, localModelPath);
        }
        return new PreparedLocalModel(localModelPath, tempModelFile, null);
    }

    public static PreparedLocalModel prepareModelWithTempDirectory(String modelSourcePath,
                                                                   Long algorithmId,
                                                                   DeviceInfo deviceInfo,
                                                                   SSHService sshService,
                                                                   VlsSshProperties sshProperties,
                                                                   Logger logger,
                                                                   String tempDirectoryPrefix,
                                                                   String synsetFileName,
                                                                   boolean useMimeDecoder) {
        if (StringUtils.isBlank(modelSourcePath)) {
            if (logger != null && deviceInfo != null) {
                logger.warn("Device {}'s algorithm {} model path is empty", deviceInfo.getDeviceName(), algorithmId);
            }
            return null;
        }

        String resolvedSynsetFileName = StringUtils.defaultIfBlank(synsetFileName, "synset.txt");
        boolean onnxModel = isOnnxModelPath(modelSourcePath);
        if (FileUtil.exist(modelSourcePath)) {
            File localModelFile = new File(modelSourcePath);
            String localModelPath = localModelFile.getAbsolutePath();
            if (onnxModel && !ensureLocalSynset(localModelFile, resolvedSynsetFileName)) {
                if (logger != null && deviceInfo != null) {
                    logger.warn("Device {} model directory lacks {}: modelPath={}", deviceInfo.getDeviceName(), resolvedSynsetFileName, localModelPath);
                }
                return null;
            }
            if (logger != null && deviceInfo != null) {
                logger.info("Device {} uses local model file: algorithmId={}, modelPath={}", deviceInfo.getDeviceName(), algorithmId, localModelPath);
            }
            return new PreparedLocalModel(localModelPath, null, null);
        }

        Path tempModelDirectory;
        try {
            tempModelDirectory = Files.createTempDirectory(StringUtils.defaultIfBlank(tempDirectoryPrefix, "detect-model-"));
        } catch (Exception createException) {
            if (logger != null && deviceInfo != null) {
                logger.warn("Device {} failed to create temporary directory", deviceInfo.getDeviceName(), createException);
            }
            return null;
        }

        byte[] modelContent = fetchRemoteFileContent(sshService, sshProperties, modelSourcePath, "model", deviceInfo, logger, useMimeDecoder);
        if (modelContent == null) {
            if (logger != null && deviceInfo != null) {
                logger.error("Device {}'s algorithm {} model file is unavailable: modelPath={}", deviceInfo.getDeviceName(), algorithmId, modelSourcePath);
            }
            cleanupTempModelDirectory(tempModelDirectory, logger);
            return null;
        }

        String modelFileName = resolveRemoteFileName(modelSourcePath, onnxModel ? "model.onnx" : "model");
        Path localModelFile = tempModelDirectory.resolve(modelFileName);
        try {
            Files.write(localModelFile, modelContent);
        } catch (Exception writeException) {
            if (logger != null && deviceInfo != null) {
                logger.warn("Device {} failed to write temporary model file", deviceInfo.getDeviceName(), writeException);
            }
            cleanupTempModelDirectory(tempModelDirectory, logger);
            return null;
        }

        if (onnxModel) {
            String synsetRemotePath = resolveSynsetPath(modelSourcePath, resolvedSynsetFileName);
            byte[] synsetContent = fetchRemoteFileContent(sshService, sshProperties, synsetRemotePath, "synset", deviceInfo, logger, useMimeDecoder);
            if (synsetContent == null) {
                if (logger != null && deviceInfo != null) {
                    logger.error("Device {}'s algorithm {} synset file is unavailable: modelPath={}, synsetPath={}",
                        deviceInfo.getDeviceName(), algorithmId, modelSourcePath, synsetRemotePath);
                }
                cleanupTempModelDirectory(tempModelDirectory, logger);
                return null;
            }
            Path synsetFilePath = tempModelDirectory.resolve(resolvedSynsetFileName);
            try {
                Files.write(synsetFilePath, synsetContent);
            } catch (Exception writeException) {
                if (logger != null && deviceInfo != null) {
                    logger.warn("Device {} failed to write synset file", deviceInfo.getDeviceName(), writeException);
                }
                cleanupTempModelDirectory(tempModelDirectory, logger);
                return null;
            }
        }

        String localModelPath = localModelFile.toAbsolutePath().toString();
        if (logger != null && deviceInfo != null) {
            logger.info("Device {} model file is ready: algorithmId={}, modelPath={}", deviceInfo.getDeviceName(), algorithmId, localModelPath);
        }
        return new PreparedLocalModel(localModelPath, null, tempModelDirectory);
    }

    public static void cleanupTempModelFile(File modelFile, Logger logger) {
        if (modelFile == null) {
            return;
        }
        try {
            FileUtil.del(modelFile);
        } catch (Exception exception) {
            if (logger != null) {
                logger.debug("Failed to delete temporary model file: {}", modelFile.getAbsolutePath(), exception);
            }
        }
    }

    public static void cleanupTempModelDirectory(Path tempModelDirectory, Logger logger) {
        if (tempModelDirectory == null) {
            return;
        }
        try {
            FileUtil.del(tempModelDirectory.toFile());
        } catch (Exception exception) {
            if (logger != null) {
                logger.debug("Failed to delete temporary model directory: {}", tempModelDirectory.toAbsolutePath(), exception);
            }
        }
    }

    public static FileResponseDto uploadSnapshot(IFileUploadService fileUploadService, File snapshotFile) {
        if (fileUploadService == null || snapshotFile == null) {
            return null;
        }
        return fileUploadService.uploadFile(
            "818301f0e77f4cd8a117414cbeb32d9e",
            "5f0de11687d744bc95e84e207d319493",
            snapshotFile
        );
    }

    public static byte[] fetchRemoteFileContent(SSHService sshService,
                                                VlsSshProperties sshProperties,
                                                String remotePath,
                                                String fileLabel,
                                                DeviceInfo deviceInfo,
                                                Logger logger,
                                                boolean useMimeDecoder) {
        if (StringUtils.isBlank(remotePath)) {
            if (logger != null && deviceInfo != null) {
                logger.warn("Device {}'s {} path is empty", deviceInfo.getDeviceName(), fileLabel);
            }
            return null;
        }
        if (sshService == null || sshProperties == null) {
            return null;
        }
        String command = String.format("base64 %s", remotePath);
        SSHService.SSHExecutionResult fileResult = sshService.executeCommand(
            sshProperties.getHost(),
            sshProperties.getPort(),
            sshProperties.getUsername(),
            sshProperties.getPassword(),
            command
        );
        if (fileResult != null && fileResult.isSuccess() && StringUtils.isNotBlank(fileResult.getOutput())) {
            String base64Content = fileResult.getOutput().trim().replaceAll("\\s+", "");
            if (logger != null && deviceInfo != null) {
                logger.info("{} Base64 length: deviceId={}, deviceName={}, length={}",
                    fileLabel, deviceInfo.getId(), deviceInfo.getDeviceName(), base64Content.length());
            }
            try {
                return useMimeDecoder
                    ? Base64.getMimeDecoder().decode(base64Content)
                    : Base64.getDecoder().decode(base64Content);
            } catch (IllegalArgumentException decodeException) {
                if (logger != null && deviceInfo != null) {
                    logger.warn("Device {} failed to decode {}: {}", deviceInfo.getDeviceName(), fileLabel, decodeException.getMessage());
                }
                return null;
            }
        }
        if (logger != null && deviceInfo != null) {
            String errorMessage = fileResult == null ? null : fileResult.getErrorMsg();
            logger.warn("Failed to retrieve {} remotely: deviceId={}, deviceName={}, path={}, error={}",
                fileLabel, deviceInfo.getId(), deviceInfo.getDeviceName(), remotePath, errorMessage);
        }
        return null;
    }

    public static void createEvent(IVlsEventManagementService eventManagementService,
                                   DeviceInfo deviceInfo,
                                   Algorithm algorithm,
                                   String eventDesc,
                                   Object eventData,
                                   String snapshotPath) {
        if (eventManagementService == null || deviceInfo == null || algorithm == null) {
            return;
        }
        EventManagement eventManagement = new EventManagement();
        eventManagement.setEventDesc(eventDesc);
        eventManagement.setEventType(algorithm.getName());
        eventManagement.setReportLocation(deviceInfo.getAddress());
        eventManagement.setReportDevice(deviceInfo.getDeviceId());
        eventManagement.setReportImg(snapshotPath);
        eventManagement.setReportTime(new Date());
        eventManagement.setEventLevel(EventLevelEnum.medium);
        eventManagement.setEventStatus(EventStatusEnum.pending);
        eventManagement.setEventData(ai.djl.util.JsonUtils.toJson(eventData));
        eventManagement.setHandleResult("Algorithm:" + algorithm.getName());
        eventManagement.setTenantId("0e391fd7-1033-4f09-88c0-187582fee462");
        eventManagementService.createEvent(eventManagement);
    }

    private static boolean isOnnxModelPath(String modelPath) {
        if (StringUtils.isBlank(modelPath)) {
            return false;
        }
        return modelPath.trim().toLowerCase().endsWith(".onnx");
    }

    private static boolean ensureLocalSynset(File localModelFile, String synsetFileName) {
        if (localModelFile == null) {
            return false;
        }
        File parentDirectory = localModelFile.getParentFile();
        if (parentDirectory == null) {
            return false;
        }
        File synsetFile = new File(parentDirectory, synsetFileName);
        return synsetFile.exists();
    }

    private static String resolveRemoteFileName(String remotePath, String fallbackName) {
        if (StringUtils.isBlank(remotePath)) {
            return fallbackName;
        }
        String trimmedPath = remotePath.trim();
        int lastSlashIndex = trimmedPath.lastIndexOf('/');
        if (lastSlashIndex < 0) {
            return fallbackName;
        }
        String fileName = trimmedPath.substring(lastSlashIndex + 1);
        return StringUtils.isBlank(fileName) ? fallbackName : fileName;
    }

    private static String resolveSynsetPath(String modelPath, String synsetFileName) {
        if (StringUtils.isBlank(modelPath)) {
            return synsetFileName;
        }
        String trimmedPath = modelPath.trim();
        int lastSlashIndex = trimmedPath.lastIndexOf('/');
        if (lastSlashIndex < 0) {
            return synsetFileName;
        }
        return trimmedPath.substring(0, lastSlashIndex + 1) + synsetFileName;
    }

    public static final class PreparedLocalModel {
        private final String localModelPath;
        private final File tempModelFile;
        private final Path tempModelDirectory;

        public PreparedLocalModel(String localModelPath, File tempModelFile, Path tempModelDirectory) {
            this.localModelPath = localModelPath;
            this.tempModelFile = tempModelFile;
            this.tempModelDirectory = tempModelDirectory;
        }

        public String getLocalModelPath() {
            return localModelPath;
        }

        public File getTempModelFile() {
            return tempModelFile;
        }

        public Path getTempModelDirectory() {
            return tempModelDirectory;
        }
    }
}
