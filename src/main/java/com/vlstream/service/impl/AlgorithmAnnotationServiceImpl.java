package com.vlstream.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jcraft.jsch.*;
import com.vlstream.entity.AlgorithmAnnotation;
import com.vlstream.entity.AnnotationImage;
import com.vlstream.entity.AnnotationInstance;
import com.vlstream.entity.AnnotationLabel;
import com.vlstream.mapper.AlgorithmAnnotationMapper;
import com.vlstream.service.AlgorithmAnnotationService;
import com.vlstream.service.AnnotationImageService;
import com.vlstream.service.AnnotationInstanceService;
import com.vlstream.service.AnnotationLabelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Algorithm Annotation Service Implementation
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlgorithmAnnotationServiceImpl extends ServiceImpl<AlgorithmAnnotationMapper, AlgorithmAnnotation> implements AlgorithmAnnotationService {

    private final AlgorithmAnnotationMapper algorithmAnnotationMapper;
    private final AnnotationImageService annotationImageService;
    private final AnnotationInstanceService annotationInstanceService;
    private final AnnotationLabelService annotationLabelService;

    @Value("${vlstream.ssh.host}")
    private String sshHost;

    @Value("${vlstream.ssh.port}")
    private Integer sshPort;

    @Value("${vlstream.ssh.username}")
    private String sshUsername;

    @Value("${vlstream.ssh.password}")
    private String sshAnnotationPassword;

    @Override
    public IPage<AlgorithmAnnotation> selectAnnotationPage(Page<AlgorithmAnnotation> page,
                                                           String annotationName,
                                                           String annotationType,
                                                           String annotationStatus) {
        log.info("Pagination query algorithm annotation list, parameters: annotationName={}, annotationType={}, annotationStatus={}",
                annotationName, annotationType, annotationStatus);

        // Convert empty string to null for correct SQL query condition handling
        String finalAnnotationName = (annotationName != null && annotationName.trim().isEmpty()) ? null : annotationName;
        String finalAnnotationType = (annotationType != null && annotationType.trim().isEmpty()) ? null : annotationType;
        String finalAnnotationStatus = (annotationStatus != null && annotationStatus.trim().isEmpty()) ? null : annotationStatus;

        log.info("Converted query parameters: annotationName={}, annotationType={}, annotationStatus={}",
                finalAnnotationName, finalAnnotationType, finalAnnotationStatus);

        return algorithmAnnotationMapper.selectAnnotationPage(page, finalAnnotationName, finalAnnotationType, finalAnnotationStatus);
    }

    @Override
    public List<AlgorithmAnnotation> getByAnnotationType(String annotationType) {
        log.info("Query annotation list by annotation type: {}", annotationType);
        return algorithmAnnotationMapper.selectByAnnotationType(annotationType);
    }

    @Override
    public List<AlgorithmAnnotation> getByAnnotationStatus(String annotationStatus) {
        log.info("Query annotation list by annotation status: {}", annotationStatus);
        return algorithmAnnotationMapper.selectByAnnotationStatus(annotationStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createAnnotation(AlgorithmAnnotation annotation) {
        log.info("Create algorithm annotation: {}", annotation.getAnnotationName());

        // Check if annotation name already exists
        QueryWrapper<AlgorithmAnnotation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("annotation_name", annotation.getAnnotationName())
                .eq("deleted", 0);
        if (count(queryWrapper) > 0) {
            log.warn("Annotation name already exists: {}", annotation.getAnnotationName());
            return false;
        }

        // Set default values
        if (annotation.getTotalCount() == null) {
            annotation.setTotalCount(0);
        }
        if (annotation.getAnnotatedCount() == null) {
            annotation.setAnnotatedCount(0);
        }
        if (annotation.getAnnotationStatus() == null) {
            annotation.setAnnotationStatus("none");
        }
        if (annotation.getProgress() == null) {
            annotation.setProgress(0);
        }

        // Dataset path will be set when saving annotation, not automatically generated here
        if (annotation.getDatasetPath() == null) {
            annotation.setDatasetPath(null);
            log.info("Dataset path will be set when saving annotation");
        }

        // Calculate progress
        annotation.setProgress(calculateProgress(annotation.getAnnotatedCount(), annotation.getTotalCount()));

        return save(annotation);
    }

    /**
     * Save annotation data to dataset file
     *
     * @param annotationId   Annotation ID
     * @return Whether saved successfully
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean saveAnnotationToDataset(Long annotationId) {
        Session session = null;
        ChannelSftp sftp = null;
        try {
            AlgorithmAnnotation annotation = getById(annotationId);
            if (annotation == null) {
                log.warn("Annotation does not exist: ID={}", annotationId);
                return false;
            }

            List<AnnotationImage> images = annotationImageService.getImagesByAnnotationId(annotationId);
            if (images == null || images.isEmpty()) {
                log.warn("No annotation images found, cannot generate dataset: annotationId={}", annotationId);
                return false;
            }

            List<AnnotationInstance> imageInstances = annotationInstanceService.getByAnnotationId(annotationId);
            Map<Long, List<AnnotationInstance>> instancesByImageId = new HashMap<>();
            Map<String, List<AnnotationInstance>> instancesByImageName = new HashMap<>();
            Map<Long, String> labelIdNameMap = new HashMap<>();

            // Preload all labels under current annotation, establish labelId -> name mapping
            List<AnnotationLabel> allLabels = annotationLabelService.getByAnnotationIdWithUsageCount(annotationId);
            if (allLabels != null) {
                for (AnnotationLabel label : allLabels) {
                    labelIdNameMap.put(label.getId(), label.getName());
                }
            }

            // Aggregate instances by imageId / imageName, collect label names
            if (imageInstances != null) {
                for (AnnotationInstance instance : imageInstances) {
                    if (instance.getImageId() != null) {
                        instancesByImageId.computeIfAbsent(instance.getImageId(), k -> new ArrayList<>()).add(instance);
                    }
                    String name = extractImageNameFromInstance(instance);
                    if (name != null) {
                        instancesByImageName.computeIfAbsent(name, k -> new ArrayList<>()).add(instance);
                    }
                    if (!labelIdNameMap.containsKey(instance.getLabelId())) {
                        AnnotationLabel lbl = annotationLabelService.getById(instance.getLabelId());
                        if (lbl != null && lbl.getName() != null) {
                            labelIdNameMap.put(instance.getLabelId(), lbl.getName());
                        }
                    }
                }
            }

            String datasetsRoot = "/data/work/ultralytics_yolov8-main/datasets/vls";
            String datasetPath = datasetsRoot + "/annotation_" + annotationId;

            // Establish SFTP connection
            JSch jsch = new JSch();
            session = jsch.getSession(sshUsername, sshHost, sshPort);
            session.setPassword(sshAnnotationPassword);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30000);

            Channel channel = session.openChannel("sftp");
            channel.connect(30000);
            sftp = (ChannelSftp) channel;

            // Create dataset directory structure
            createCompleteDatasetStructure(sftp, datasetPath);

            Set<String> labelNames = new LinkedHashSet<>();
            if (allLabels != null) {
                allLabels.forEach(label -> {
                    if (label.getName() != null) {
                        labelNames.add(label.getName());
                    }
                });
            } else {
                labelNames.addAll(labelIdNameMap.values());
            }
            Map<String, Integer> labelIndexMap = new LinkedHashMap<>();
            int labelIdxSeed = 0;
            for (String name : labelNames) {
                labelIndexMap.putIfAbsent(name, labelIdxSeed++);
            }

            for (AnnotationImage image : images) {
                String imageName = image.getImageName();

                // Upload image to train / val directory (will download from localPath to local temporary file first)
                ImageLocalInfo imageInfo = uploadImageFile(sftp, datasetPath, image);
                if (imageInfo == null) {
                    log.warn("Skip non-existent image file: annotationId={}, imageName={}", annotationId, imageName);
                    continue;
                }

                int[] dims = readImageSize(imageInfo.localPath);
                double imageWidth = dims[0] > 0 ? dims[0] : -1;
                double imageHeight = dims[1] > 0 ? dims[1] : -1;

                // Process and upload corresponding label file
                List<Map<String, Object>> annotationMaps = new ArrayList<>();
                List<AnnotationInstance> perImageInstances = new ArrayList<>();
                if (instancesByImageId.containsKey(image.getId())) {
                    perImageInstances.addAll(instancesByImageId.get(image.getId()));
                }
                if (perImageInstances.isEmpty()) {
                    perImageInstances.addAll(instancesByImageName.getOrDefault(imageName, new ArrayList<>()));
                }
                if (perImageInstances.isEmpty()) {
                    // Fallback: re-query by annotationId + imageName
                    perImageInstances.addAll(annotationInstanceService.getByAnnotationIdAndImageName(annotationId, imageName));
                }

                for (AnnotationInstance instance : perImageInstances) {
                    Map<String, Object> parsed = parseAnnotationData(instance.getAnnotationData());
                    if (parsed.isEmpty()) {
                        continue;
                    }
                    String labelName = labelIdNameMap.get(instance.getLabelId());
                    if (labelName != null) {
                        parsed.put("labelName", labelName);
                        labelNames.add(labelName);
                        labelIndexMap.putIfAbsent(labelName, labelIndexMap.size());
                    }
                    annotationMaps.add(parsed);
                }

                if (!labelNames.isEmpty()) {
                    int idx = 0;
                    for (String name : labelNames) {
                        labelIndexMap.putIfAbsent(name, idx++);
                    }
                }

                if (!annotationMaps.isEmpty()) {
                    uploadLabelFile(sftp, datasetPath, imageName, annotationMaps, labelIndexMap, imageWidth, imageHeight);
                }

                // Delete temporary file
                if (imageInfo.tempFile != null && imageInfo.tempFile.exists() && !imageInfo.tempFile.delete()) {
                    log.debug("Failed to delete temporary image: {}", imageInfo.tempFile.getAbsolutePath());
                }
            }

            // If no label names collected during loop, fill with label list under annotation
            if (labelNames.isEmpty() && allLabels != null) {
                for (AnnotationLabel label : allLabels) {
                    if (label.getName() != null) {
                        labelNames.add(label.getName());
                        labelIndexMap.putIfAbsent(label.getName(), labelIndexMap.size());
                    }
                }
            }

            // Generate and upload dataset YAML
            String datasetYamlContent = buildDatasetYaml(annotation, labelNames);
            uploadDatasetYaml(sftp, datasetPath, datasetYamlContent);

            UpdateWrapper<AlgorithmAnnotation> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", annotationId).set("dataset_path", datasetPath);
            boolean updateResult = update(updateWrapper);
            if (!updateResult) {
                log.warn("Failed to update dataset path: annotationId={}", annotationId);
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to save annotation data to dataset: annotationId={}, error={}", annotationId, e.getMessage());
            return false;
        } finally {
            if (sftp != null && sftp.isConnected()) {
                sftp.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    /**
     * Parse annotation data
     *
     * @param annotationData JSON format annotation data
     * @return Parsed data
     */
    private Map<String, Object> parseAnnotationData(String annotationData) {
        try {
            if (annotationData == null || annotationData.trim().isEmpty()) {
                return new HashMap<>();
            }

            return JSONUtil.toBean(annotationData, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse annotation data: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Create remote directory
     *
     * @param sftp SFTP channel
     * @param path Directory path
     */
    private void createRemoteDirectory(ChannelSftp sftp, String path) throws SftpException {
        String[] dirs = path.split("/");
        String currentPath = "";

        for (String dir : dirs) {
            if (dir.isEmpty()) {
                continue;
            }

            currentPath += "/" + dir;

            try {
                sftp.cd(currentPath);
                log.debug("Directory already exists: {}", currentPath);
            } catch (SftpException e) {
                // Directory does not exist, create it
                try {
                    sftp.mkdir(currentPath);
                    log.info("Create remote directory: {}", currentPath);
                } catch (SftpException mkdirException) {
                    log.warn("Failed to create directory, may already exist: {}", currentPath);
                }
            }
        }
    }

    /**
     * Create complete YOLO dataset directory structure
     *
     * @param sftp        SFTP channel
     * @param datasetPath Dataset path
     */
    private void createCompleteDatasetStructure(ChannelSftp sftp, String datasetPath) throws SftpException {
        // Create main dataset directory
        createRemoteDirectory(sftp, datasetPath);

        // Create YOLO standard directory structure
        String[] subdirs = {
                datasetPath + "/images/train",
                datasetPath + "/images/val",
                datasetPath + "/images/test",
                datasetPath + "/labels/train",
                datasetPath + "/labels/val",
                datasetPath + "/labels/test"
        };

        for (String subdir : subdirs) {
            // Recursively create to avoid mkdir error when parent directory doesn't exist
            createRemoteDirectory(sftp, subdir);
            log.info("Ensure subdirectory exists: {}", subdir);
        }

        log.info("Complete YOLO dataset directory structure created: {}", datasetPath);
    }

    /**
     * Upload image to train/val directory (prioritize downloading from localPath to local temporary file)
     */
    private ImageLocalInfo uploadImageFile(ChannelSftp sftp, String datasetPath, AnnotationImage image) {
        String imageName = image.getImageName();

        File downloadedFile = FileUtil.createTempFile();
        HttpUtil.downloadFile(image.getLocalPath(), downloadedFile);
        String localImagePath = downloadedFile.getAbsolutePath();

        String trainPath = datasetPath + "/images/train/" + imageName;
        String valPath = datasetPath + "/images/val/" + imageName;
        try {
            sftp.put(localImagePath, trainPath);
            sftp.put(localImagePath, valPath);
            log.info("Image upload completed: train={}, val={}", trainPath, valPath);
        } catch (Exception e) {
            log.error("Failed to upload image: imageName={}, path={}, error={}", imageName, localImagePath, e.getMessage());
            return null;
        }

        return new ImageLocalInfo(localImagePath, downloadedFile);
    }

    /**
     * Read local image size
     */
    private int[] readImageSize(String localPath) {
        try {
            BufferedImage img = ImageIO.read(new File(localPath));
            if (img != null) {
                return new int[]{img.getWidth(), img.getHeight()};
            }
        } catch (Exception e) {
            log.warn("Failed to read image size: path={}, error={}", localPath, e.getMessage());
        }
        return new int[]{-1, -1};
    }

    /**
     * Upload label file to train/val directory
     */
    private void uploadLabelFile(ChannelSftp sftp, String datasetPath, String imageName,
                                 List<Map<String, Object>> annotationMaps,
                                 Map<String, Integer> labelIndexMap,
                                 double imageWidth, double imageHeight) throws Exception {
        int dotIndex = imageName.lastIndexOf('.');
        if (dotIndex <= 0) {
            log.warn("Image name lacks extension, skipping label upload: {}", imageName);
            return;
        }

        String labelFileName = imageName.substring(0, dotIndex) + ".txt";
        String labelContent = generateYoloLabelContent(annotationMaps, labelIndexMap, imageWidth, imageHeight);

        File localLabelFile = new File(System.getProperty("java.io.tmpdir"), labelFileName);
        try (FileWriter writer = new FileWriter(localLabelFile)) {
            writer.write(labelContent);
        }

        String trainLabelPath = datasetPath + "/labels/train/" + labelFileName;
        String valLabelPath = datasetPath + "/labels/val/" + labelFileName;
        sftp.put(localLabelFile.getAbsolutePath(), trainLabelPath);
        sftp.put(localLabelFile.getAbsolutePath(), valLabelPath);
        log.info("Label file upload completed: train={}, val={}", trainLabelPath, valLabelPath);

        if (!localLabelFile.delete()) {
            log.debug("Failed to delete temporary label file: {}", localLabelFile.getAbsolutePath());
        }
    }

    /**
     * Upload dataset YAML file
     */
    private void uploadDatasetYaml(ChannelSftp sftp, String datasetPath, String yamlContent) throws Exception {
        String datasetFileName = "dataset.yaml";
        File localYamlFile = new File(System.getProperty("java.io.tmpdir"), datasetFileName);
        Files.write(localYamlFile.toPath(), yamlContent.getBytes(StandardCharsets.UTF_8));

        String remoteYamlPath = datasetPath + "/" + datasetFileName;
        sftp.put(localYamlFile.getAbsolutePath(), remoteYamlPath);
        log.info("Dataset configuration file upload completed: {}", remoteYamlPath);

        if (!localYamlFile.delete()) {
            log.debug("Failed to delete temporary configuration file: {}", localYamlFile.getAbsolutePath());
        }
    }

    /**
     * Extract image name from annotation instance, used for fallback matching when imageId is empty
     */
    private String extractImageNameFromInstance(AnnotationInstance instance) {
        if (instance == null) {
            return null;
        }
        try {
            Map<String, Object> data = parseAnnotationData(instance.getAnnotationData());
            Object name = data.get("imageName");
            return name == null ? null : name.toString();
        } catch (Exception e) {
            log.debug("Failed to parse annotation instance image name: {}", e.getMessage());
            return null;
        }
    }

    private static class ImageLocalInfo {
        private final String localPath;
        private final File tempFile;

        private ImageLocalInfo(String localPath, File tempFile) {
            this.localPath = localPath;
            this.tempFile = tempFile;
        }
    }

    private String generateYoloLabelContent(List<Map<String, Object>> annotations, Map<String, Integer> labelIndexMap,
                                            double imageWidth, double imageHeight) {
        StringBuilder content = new StringBuilder();

        for (Map<String, Object> annotation : annotations) {
            try {
                Double x = toDouble(annotation.get("x"));
                Double y = toDouble(annotation.get("y"));
                Double width = toDouble(annotation.get("width"));
                Double height = toDouble(annotation.get("height"));

                if (x != null && y != null && width != null && height != null) {
                    double imgW = imageWidth > 0 ? imageWidth : toDouble(annotation.get("imageWidth")) != null ? toDouble(annotation.get("imageWidth")) : 100.0;
                    double imgH = imageHeight > 0 ? imageHeight : toDouble(annotation.get("imageHeight")) != null ? toDouble(annotation.get("imageHeight")) : 100.0;

                    // Determine if already normalized: all four values in (0,1] are considered relative ratio
                    boolean alreadyNormalized = x > 0 && x <= 1 && y > 0 && y <= 1 && width > 0 && width <= 1 && height > 0 && height <= 1;

                    double centerX;
                    double centerY;
                    double normalizedWidth;
                    double normalizedHeight;

                    if (alreadyNormalized) {
                        centerX = x + width / 2.0;
                        centerY = y + height / 2.0;
                        normalizedWidth = width;
                        normalizedHeight = height;
                    } else {
                        centerX = (x + width / 2.0) / imgW;
                        centerY = (y + height / 2.0) / imgH;
                        normalizedWidth = width / imgW;
                        normalizedHeight = height / imgH;
                    }

                    // Clip to [0,1]
                    centerX = Math.min(Math.max(centerX, 0), 1);
                    centerY = Math.min(Math.max(centerY, 0), 1);
                    normalizedWidth = Math.min(Math.max(normalizedWidth, 0), 1);
                    normalizedHeight = Math.min(Math.max(normalizedHeight, 0), 1);

                    int classId = 0;
                    if (labelIndexMap != null && annotation.get("labelName") != null) {
                        Integer mapped = labelIndexMap.get(annotation.get("labelName").toString());
                        if (mapped != null) {
                            classId = mapped;
                        }
                    }

                    content.append(String.format("%d %.6f %.6f %.6f %.6f\n",
                            classId, centerX, centerY, normalizedWidth, normalizedHeight));
                }

            } catch (Exception e) {
                log.error("Failed to process annotation data: {}", e.getMessage());
            }
        }

        return content.toString();
    }

    /**
     * Build dataset YAML content
     */
    private String buildDatasetYaml(AlgorithmAnnotation algorithmAnnotation, Set<String> labelNames) {
        StringBuilder content = new StringBuilder();
        // Use relative path to parent directory for easy reference under yolo project
        content.append("path: ../datasets/vls/annotation_").append(algorithmAnnotation.getId()).append("\n");
        content.append("train: images/train\n");
        content.append("val: images/val\n");
        content.append("test: images/test\n\n");

        int labelCount = labelNames == null ? 0 : labelNames.size();
        content.append("nc: " + labelCount + "\n");
        content.append("names: [");
        if (labelNames != null && !labelNames.isEmpty()) {
            content.append(labelNames.stream().collect(Collectors.joining("', '", "'", "'")));
        }
        content.append("]\n\n");
        content.append("description: " + algorithmAnnotation.getAnnotationName());
        return content.toString();
    }

    /**
     * Unify any numeric type to Double, compatible with Integer/Long/Double and numeric strings.
     */
    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble(((String) value).trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Generate dataset file content
     *
     * @param annotationName  Annotation name
     * @param imageInstances  Annotation data
     * @return Dataset file content
     */
    private String generateDatasetContent(String annotationName, List<AnnotationInstance> imageInstances) {
        StringBuilder content = new StringBuilder();
        content.append("# YOLO dataset configuration file\n");
        content.append("# Annotation name: " + annotationName + "\n");
        content.append("# Creation time: " + java.time.LocalDateTime.now() + "\n\n");

        content.append("# Dataset path configuration\n");
        content.append("path: ./vls\n");
        content.append("train: images/train\n");
        content.append("val: images/val\n");
        content.append("test: images/test\n\n");

        // Extract category information from annotation data
        Set<String> uniqueLabels = new HashSet<>();
        for (AnnotationInstance annotationInstance : imageInstances) {
           AnnotationLabel annotationLabel = annotationLabelService.getById(annotationInstance.getLabelId());
            String labelName = annotationLabel.getName();
            if (labelName != null && !labelName.trim().isEmpty()) {
                uniqueLabels.add(labelName);
            }
        }

        content.append("# Category configuration\n");
        content.append("nc: " + uniqueLabels.size() + "\n");
        content.append("names: [");
        if (!uniqueLabels.isEmpty()) {
            content.append("'" + String.join("',  '" , uniqueLabels) + "'");
        }
        content.append("]\n\n");

        content.append("# Dataset information\n");
        content.append("description: " + annotationName + " annotation dataset\n");
        content.append("total_images: " + imageInstances.size() + "\n");
        content.append("total_annotations: " + imageInstances.size() + "\n");
        content.append("unique_labels: " + uniqueLabels.size() + "\n\n");

        content.append("# Note: This configuration file only contains basic settings\n");
        content.append("# Actual training requires the following directory structure:\n");
        content.append("# - images/train/ (training images)\n");
        content.append("# - images/val/ (validation images)\n");
        content.append("# - labels/train/ (training labels)\n");
        content.append("# - labels/val/ (validation labels)\n");

        return content.toString();
    }

    /**
     * Extract annotation list from annotation data
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractAnnotations(Map<String, Object> annotationData) {
        try {
            Object annotationsObj = annotationData.get("annotations");
            if (annotationsObj instanceof List) {
                return (List<Map<String, Object>>) annotationsObj;
            }
        } catch (Exception e) {
            log.error("Failed to extract annotation list: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * Extract image count from annotation data
     */
    private int extractImageCount(Map<String, Object> annotationData) {
        try {
            String imageName = (String) annotationData.get("imageName");
            return imageName != null ? 1 : 0;
        } catch (Exception e) {
            log.error("Failed to extract image count: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Extract image name list from annotation data
     */
    private List<String> extractImageNames(Map<String, Object> annotationData) {
        try {
            String imageName = (String) annotationData.get("imageName");
            if (imageName != null) {
                List<String> imageNames = new ArrayList<>();
                imageNames.add(imageName);
                return imageNames;
            }
        } catch (Exception e) {
            log.error("Failed to extract image name: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * Generate dataset file name
     *
     * @param annotationName  Annotation name
     * @return Dataset file name
     */
    private String generateDatasetFileName(String annotationName) {
        // Remove special characters, keep only letters, numbers and underscores
        String cleanName = annotationName.replaceAll("[^a-zA-Z0-9_]", "");

        // Convert to lowercase
        cleanName = cleanName.toLowerCase();

        // Add timestamp to ensure uniqueness
        String timestamp = String.valueOf(System.currentTimeMillis());

        // Generate dataset file name
        String datasetFileName = cleanName + timestamp + ".yaml";

        log.info("Generate dataset file name: originalName={}, cleanName={}, datasetFileName={}",
                annotationName, cleanName, datasetFileName);

        return datasetFileName;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAnnotation(AlgorithmAnnotation annotation) {
        log.info("Update algorithm annotation: ID={}, Name={}", annotation.getId(), annotation.getAnnotationName());

        // Get original annotation information
        AlgorithmAnnotation existing = getById(annotation.getId());
        if (existing == null) {
            log.warn("Annotation does not exist: ID={}", annotation.getId());
            return false;
        }

        // Recalculate progress
        annotation.setProgress(calculateProgress(annotation.getAnnotatedCount(), annotation.getTotalCount()));

        // Automatically update annotation status
        annotation.setAnnotationStatus(calculateAnnotationStatus(annotation.getProgress()));

        return updateById(annotation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAnnotation(Long id) {
        log.info("Delete algorithm annotation: ID={}", id);

        AlgorithmAnnotation annotation = getById(id);
        if (annotation == null) {
            log.warn("Annotation does not exist: ID={}", id);
            return false;
        }

        // Delete related image files
        try {
            deleteAnnotationImages(annotation);
        } catch (Exception e) {
            log.error("Failed to delete annotation image files: ID={}, Error={}", id, e.getMessage());
            // Do not block deletion operation, only log error
        }

        return removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteAnnotations(List<Long> ids) {
        log.info("Batch delete algorithm annotations: IDs={}", ids);

        // First get all annotations to be deleted
        List<AlgorithmAnnotation> annotations = listByIds(ids);

        // Delete related image files
        for (AlgorithmAnnotation annotation : annotations) {
            try {
                deleteAnnotationImages(annotation);
            } catch (Exception e) {
                log.error("Failed to delete annotation image files: ID={}, Error={}", annotation.getId(), e.getMessage());
                // Do not block deletion operation, only log error
            }
        }

        return removeByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAnnotationProgress(Long id, Integer annotatedCount) {
        log.info("Update annotation progress: ID={}, AnnotatedCount={}", id, annotatedCount);

        AlgorithmAnnotation annotation = getById(id);
        if (annotation == null) {
            log.warn("Annotation does not exist: ID={}", id);
            return false;
        }

        // Calculate progress
        int progress = calculateProgress(annotatedCount, annotation.getTotalCount());
        String status = calculateAnnotationStatus(progress);

        UpdateWrapper<AlgorithmAnnotation> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id)
                .set("annotated_count", annotatedCount)
                .set("progress", progress)
                .set("annotation_status", status);

        return update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateAnnotationStatus(List<Long> ids, String annotationStatus) {
        log.info("Batch update annotation status: IDs={}, Status={}", ids, annotationStatus);

        UpdateWrapper<AlgorithmAnnotation> updateWrapper = new UpdateWrapper<>();
        updateWrapper.in("id", ids)
                .set("annotation_status", annotationStatus);

        return update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean startAnnotationTask(Long id) {
        log.info("Start annotation task: ID={}", id);

        UpdateWrapper<AlgorithmAnnotation> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id)
                .set("annotation_status", "partial");

        return update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completeAnnotationTask(Long id) {
        log.info("Complete annotation task: ID={}", id);

        AlgorithmAnnotation annotation = getById(id);
        if (annotation == null) {
            log.warn("Annotation does not exist: ID={}", id);
            return false;
        }

        UpdateWrapper<AlgorithmAnnotation> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id)
                .set("annotation_status", "completed")
                .set("progress", 100)
                .set("annotated_count", annotation.getTotalCount());

        return update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resetAnnotationTask(Long id) {
        log.info("Reset annotation task: ID={}", id);

        UpdateWrapper<AlgorithmAnnotation> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id)
                .set("annotation_status", "none")
                .set("progress", 0)
                .set("annotated_count", 0);

        return update(updateWrapper);
    }

    @Override
    public Map<String, Object> importAnnotationData(Long id, String dataPath) {
        log.info("Import annotation data: ID={}, DataPath={}", id, dataPath);

        AlgorithmAnnotation annotation = getById(id);
        if (annotation == null) {
            log.warn("Annotation does not exist: ID={}", id);
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Import successful");
        result.put("importedCount", 100); // Sample data

        // Actual import logic can be added here

        return result;
    }

    @Override
    public List<Map<String, Object>> getAnnotationTypeStatistics() {
        log.info("Get annotation type statistics");
        return algorithmAnnotationMapper.selectAnnotationTypeStatistics();
    }

    @Override
    public List<Map<String, Object>> getAnnotationStatusStatistics() {
        log.info("Get annotation status statistics");
        return algorithmAnnotationMapper.selectAnnotationStatusStatistics();
    }

    @Override
    public List<Map<String, Object>> getProgressStatistics() {
        log.info("Get progress statistics");
        return algorithmAnnotationMapper.selectProgressStatistics();
    }

    @Override
    public Map<String, Object> getWorkloadStatistics() {
        log.info("Get workload statistics");
        return algorithmAnnotationMapper.selectWorkloadStatistics();
    }

    @Override
    public Map<String, Object> validateAnnotationData(Long id) {
        log.info("Validate annotation data: ID={}", id);

        AlgorithmAnnotation annotation = getById(id);
        if (annotation == null) {
            log.warn("Annotation does not exist: ID={}", id);
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Validation successful");
        result.put("validCount", annotation.getAnnotatedCount());
        result.put("invalidCount", 0);
        result.put("validationDetails", new HashMap<>());

        // Actual validation logic can be added here

        return result;
    }

    /**
     * Calculate annotation progress
     *
     * @param annotatedCount  Annotated count
     * @param totalCount      Total count
     * @return Progress percentage
     */
    private int calculateProgress(Integer annotatedCount, Integer totalCount) {
        if (totalCount == null || totalCount == 0) {
            return 0;
        }
        if (annotatedCount == null) {
            return 0;
        }
        return Math.min(100, (annotatedCount * 100) / totalCount);
    }

    /**
     * Calculate annotation status based on progress
     *
     * @param progress  Progress percentage
     * @return Annotation status
     */
    private String calculateAnnotationStatus(int progress) {
        if (progress == 0) {
            return "none";
        } else if (progress < 100) {
            return "partial";
        } else {
            return "completed";
        }
    }

    /**
     * Delete annotation-related image files
     *
     * @param annotation  Annotation information
     */
    private void deleteAnnotationImages(AlgorithmAnnotation annotation) {
        if (annotation == null || annotation.getDatasetPath() == null) {
            log.warn("Annotation or dataset path is empty, skipping image deletion");
            return;
        }

        try {
            String datasetPath = annotation.getDatasetPath();
            log.info("Start deleting annotation image files: ID={}, DatasetPath={}", annotation.getId(), datasetPath);

            // If dataset path is relative, convert to absolute path
            String absolutePath = datasetPath;
            if (!datasetPath.startsWith("/") && !datasetPath.contains(":")) {
                // Relative path, based on current working directory
                String currentDir = System.getProperty("user.dir");
                absolutePath = currentDir + "/" + datasetPath;
            }

            java.io.File datasetDir = new java.io.File(absolutePath);
            if (!datasetDir.exists()) {
                log.warn("Dataset directory does not exist: {}", absolutePath);
                return;
            }

            // Delete all image files in directory
            deleteImageFiles(datasetDir);
            log.info("Annotation image files deletion completed: ID={}", annotation.getId());

        } catch (Exception e) {
            log.error("Failed to delete annotation image files: ID={}, Error={}", annotation.getId(), e.getMessage());
            throw e;
        }
    }

    /**
     * Recursively delete image files in directory
     *
     * @param directory  Directory
     */
    private void deleteImageFiles(java.io.File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }

        java.io.File[] files = directory.listFiles();
        if (files != null) {
            for (java.io.File file : files) {
                if (file.isDirectory()) {
                    // Recursively delete subdirectory
                    deleteImageFiles(file);
                    // Delete empty directory
                    if (file.listFiles() == null || Objects.requireNonNull(file.listFiles()).length == 0) {
                        file.delete();
                        log.debug("Delete empty directory: {}", file.getAbsolutePath());
                    }
                } else if (isImageFile(file.getName())) {
                    // Delete image file
                    boolean deleted = file.delete();
                    if (deleted) {
                        log.debug("Delete image file: {}", file.getAbsolutePath());
                    } else {
                        log.warn("Failed to delete image file: {}", file.getAbsolutePath());
                    }
                }
            }
        }
    }

    /**
     * Check if it is an image file
     *
     * @param fileName  File name
     * @return Whether it is an image file
     */
    private boolean isImageFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(".jpg") ||
                lowerFileName.endsWith(".jpeg") ||
                lowerFileName.endsWith(".png") ||
                lowerFileName.endsWith(".bmp") ||
                lowerFileName.endsWith(".gif") ||
                lowerFileName.endsWith(".webp");
    }

@Override
    public void downloadAnnotationDataset(Long id, HttpServletResponse response) {
        log.info("Downloading annotation dataset zip, id={}", id);

        Session session = null;
        ChannelSftp sftp = null;
        ChannelExec execChannel = null;

        try {
            AlgorithmAnnotation annotation = getById(id);
            if (annotation == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write("Annotation not found");
                return;
            }

            String datasetPath = annotation.getDatasetPath();
            if (datasetPath == null || datasetPath.trim().isEmpty()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("Dataset path is empty");
                return;
            }

            String normalizedPath = datasetPath.trim();
            int lastSlash = normalizedPath.lastIndexOf('/');
            String parentPath;
            String datasetDirName;
            if (lastSlash >= 0) {
                parentPath = normalizedPath.substring(0, lastSlash);
                datasetDirName = normalizedPath.substring(lastSlash + 1);
                if (parentPath.isEmpty()) {
                    parentPath = "/";
                }
            } else {
                parentPath = ".";
                datasetDirName = normalizedPath;
            }

            String zipFileName = datasetDirName + ".zip";
            String remoteZipPath = "/tmp/" + datasetDirName + "_" + System.currentTimeMillis() + ".zip";

            JSch jsch = new JSch();
            session = jsch.getSession(sshUsername, sshHost, sshPort);
            session.setPassword(sshAnnotationPassword);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30000);

            String zipCommand = String.format("cd \"%s\" && zip -r \"%s\" \"%s\"", parentPath, remoteZipPath, datasetDirName);
            execChannel = (ChannelExec) session.openChannel("exec");
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
            execChannel.setErrStream(errorStream);
            execChannel.setOutputStream(new ByteArrayOutputStream());
            execChannel.setCommand(zipCommand);
            execChannel.connect(30000);

            while (!execChannel.isClosed()) {
                Thread.sleep(200L);
            }
            int exitStatus = execChannel.getExitStatus();
            execChannel.disconnect();
            execChannel = null;

            if (exitStatus != 0) {
                String errMsg = errorStream.toString(StandardCharsets.UTF_8.name());
                log.error("Failed to zip dataset, path={}, exitStatus={}, error={}", normalizedPath, exitStatus, errMsg);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("Failed to compress dataset: " + errMsg);
                return;
            }

            Channel sftpChannel = session.openChannel("sftp");
            sftpChannel.connect(30000);
            sftp = (ChannelSftp) sftpChannel;

            response.setContentType("application/zip");
            String encodedFileName = URLEncoder.encode(zipFileName, StandardCharsets.UTF_8.name()).replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

            try (InputStream inputStream = sftp.get(remoteZipPath);
                 OutputStream outputStream = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                }
                outputStream.flush();
            }

            try {
                sftp.rm(remoteZipPath);
            } catch (SftpException cleanupEx) {
                log.warn("Failed to delete remote zip file: {}", cleanupEx.getMessage());
            }

            log.info("Dataset zip download completed, id={}, file={}", id, zipFileName);
        } catch (Exception e) {
            log.error("Download dataset zip failed, id={}, error={}", id, e.getMessage(), e);
            try {
                if (!response.isCommitted()) {
                    response.reset();
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().write("Download dataset zip failed: " + e.getMessage());
                }
            } catch (Exception writeEx) {
                log.error("Failed to write error response: {}", writeEx.getMessage());
            }
        } finally {
            if (execChannel != null) {
                execChannel.disconnect();
            }
            if (sftp != null && sftp.isConnected()) {
                sftp.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }


}