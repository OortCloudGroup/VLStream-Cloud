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
 * 算法标注服务实现类
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
        log.info("分页查询算法标注列表，参数：annotationName={}, annotationType={}, annotationStatus={}",
                annotationName, annotationType, annotationStatus);

        // 将空字符串转换为null，以便SQL查询条件正确处理
        String finalAnnotationName = (annotationName != null && annotationName.trim().isEmpty()) ? null : annotationName;
        String finalAnnotationType = (annotationType != null && annotationType.trim().isEmpty()) ? null : annotationType;
        String finalAnnotationStatus = (annotationStatus != null && annotationStatus.trim().isEmpty()) ? null : annotationStatus;

        log.info("转换后的查询参数：annotationName={}, annotationType={}, annotationStatus={}",
                finalAnnotationName, finalAnnotationType, finalAnnotationStatus);

        return algorithmAnnotationMapper.selectAnnotationPage(page, finalAnnotationName, finalAnnotationType, finalAnnotationStatus);
    }

    @Override
    public List<AlgorithmAnnotation> getByAnnotationType(String annotationType) {
        log.info("根据标注类型查询标注列表：{}", annotationType);
        return algorithmAnnotationMapper.selectByAnnotationType(annotationType);
    }

    @Override
    public List<AlgorithmAnnotation> getByAnnotationStatus(String annotationStatus) {
        log.info("根据标注状态查询标注列表：{}", annotationStatus);
        return algorithmAnnotationMapper.selectByAnnotationStatus(annotationStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createAnnotation(AlgorithmAnnotation annotation) {
        log.info("创建算法标注：{}", annotation.getAnnotationName());

        // 检查标注名称是否重复
        QueryWrapper<AlgorithmAnnotation> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("annotation_name", annotation.getAnnotationName())
                .eq("deleted", 0);
        if (count(queryWrapper) > 0) {
            log.warn("标注名称已存在：{}", annotation.getAnnotationName());
            return false;
        }

        // 设置默认值
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

        // 数据集路径在保存标注时设置，这里不自动生成
        if (annotation.getDatasetPath() == null) {
            annotation.setDatasetPath(null);
            log.info("数据集路径将在保存标注时设置");
        }

        // 计算进度
        annotation.setProgress(calculateProgress(annotation.getAnnotatedCount(), annotation.getTotalCount()));

        return save(annotation);
    }

    /**
     * 保存标注数据到数据集文件
     *
     * @param annotationId   标注ID
     * @return 是否保存成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean saveAnnotationToDataset(Long annotationId) {
        Session session = null;
        ChannelSftp sftp = null;
        try {
            AlgorithmAnnotation annotation = getById(annotationId);
            if (annotation == null) {
                log.warn("标注不存在：ID={}", annotationId);
                return false;
            }

            List<AnnotationImage> images = annotationImageService.getImagesByAnnotationId(annotationId);
            if (images == null || images.isEmpty()) {
                log.warn("未找到标注图片，无法生成数据集：annotationId={}", annotationId);
                return false;
            }

            List<AnnotationInstance> imageInstances = annotationInstanceService.getByAnnotationId(annotationId);
            Map<Long, List<AnnotationInstance>> instancesByImageId = new HashMap<>();
            Map<String, List<AnnotationInstance>> instancesByImageName = new HashMap<>();
            Map<Long, String> labelIdNameMap = new HashMap<>();

            // 预先加载当前标注下的全部标签，建立 labelId -> name 映射
            List<AnnotationLabel> allLabels = annotationLabelService.getByAnnotationIdWithUsageCount(annotationId);
            if (allLabels != null) {
                for (AnnotationLabel label : allLabels) {
                    labelIdNameMap.put(label.getId(), label.getName());
                }
            }

            // 按 imageId / imageName 聚合实例，收集标签名称
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

            // 建立 SFTP 连接
            JSch jsch = new JSch();
            session = jsch.getSession(sshUsername, sshHost, sshPort);
            session.setPassword(sshAnnotationPassword);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30000);

            Channel channel = session.openChannel("sftp");
            channel.connect(30000);
            sftp = (ChannelSftp) channel;

            // 创建数据集目录结构
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

                // 上传图片到 train / val 目录（会先从 localPath 下载到本地临时文件）
                ImageLocalInfo imageInfo = uploadImageFile(sftp, datasetPath, image);
                if (imageInfo == null) {
                    log.warn("跳过不存在的图片文件：annotationId={}, imageName={}", annotationId, imageName);
                    continue;
                }

                int[] dims = readImageSize(imageInfo.localPath);
                double imageWidth = dims[0] > 0 ? dims[0] : -1;
                double imageHeight = dims[1] > 0 ? dims[1] : -1;

                // 处理并上传对应的标注文件
                List<Map<String, Object>> annotationMaps = new ArrayList<>();
                List<AnnotationInstance> perImageInstances = new ArrayList<>();
                if (instancesByImageId.containsKey(image.getId())) {
                    perImageInstances.addAll(instancesByImageId.get(image.getId()));
                }
                if (perImageInstances.isEmpty()) {
                    perImageInstances.addAll(instancesByImageName.getOrDefault(imageName, new ArrayList<>()));
                }
                if (perImageInstances.isEmpty()) {
                    // 兜底：按 annotationId + imageName 重新查询
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

                // 删除临时文件
                if (imageInfo.tempFile != null && imageInfo.tempFile.exists() && !imageInfo.tempFile.delete()) {
                    log.debug("临时图片删除失败：{}", imageInfo.tempFile.getAbsolutePath());
                }
            }

            // 若循环中未收集到标签名称，兜底按标注下的标签列表填充
            if (labelNames.isEmpty() && allLabels != null) {
                for (AnnotationLabel label : allLabels) {
                    if (label.getName() != null) {
                        labelNames.add(label.getName());
                        labelIndexMap.putIfAbsent(label.getName(), labelIndexMap.size());
                    }
                }
            }

            // 生成并上传数据集 YAML
            String datasetYamlContent = buildDatasetYaml(annotation, labelNames);
            uploadDatasetYaml(sftp, datasetPath, datasetYamlContent);

            UpdateWrapper<AlgorithmAnnotation> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", annotationId).set("dataset_path", datasetPath);
            boolean updateResult = update(updateWrapper);
            if (!updateResult) {
                log.warn("更新数据集路径失败：annotationId={}", annotationId);
            }

            return true;
        } catch (Exception e) {
            log.error("保存标注数据到数据集失败：annotationId={}, error={}", annotationId, e.getMessage());
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
     * 解析标注数据
     *
     * @param annotationData JSON格式的标注数据
     * @return 解析后的数据
     */
    private Map<String, Object> parseAnnotationData(String annotationData) {
        try {
            if (annotationData == null || annotationData.trim().isEmpty()) {
                return new HashMap<>();
            }

            return JSONUtil.toBean(annotationData, Map.class);
        } catch (Exception e) {
            log.error("解析标注数据失败：{}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * 创建远程目录
     *
     * @param sftp SFTP通道
     * @param path 目录路径
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
                log.debug("目录已存在：{}", currentPath);
            } catch (SftpException e) {
                // 目录不存在，创建它
                try {
                    sftp.mkdir(currentPath);
                    log.info("创建远程目录：{}", currentPath);
                } catch (SftpException mkdirException) {
                    log.warn("创建目录失败，可能已存在：{}", currentPath);
                }
            }
        }
    }

    /**
     * 创建完整的YOLO数据集目录结构
     *
     * @param sftp        SFTP通道
     * @param datasetPath 数据集路径
     */
    private void createCompleteDatasetStructure(ChannelSftp sftp, String datasetPath) throws SftpException {
        // 创建主数据集目录
        createRemoteDirectory(sftp, datasetPath);

        // 创建YOLO标准目录结构
        String[] subdirs = {
                datasetPath + "/images/train",
                datasetPath + "/images/val",
                datasetPath + "/images/test",
                datasetPath + "/labels/train",
                datasetPath + "/labels/val",
                datasetPath + "/labels/test"
        };

        for (String subdir : subdirs) {
            // 递归创建，避免父目录不存在导致 mkdir 报错
            createRemoteDirectory(sftp, subdir);
            log.info("确保子目录存在：{}", subdir);
        }

        log.info("完整的YOLO数据集目录结构创建完成：{}", datasetPath);
    }

    /**
     * 上传图片到训练/验证目录（优先从 localPath 下载到本地临时文件）
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
            log.info("图片上传完成：train={}, val={}", trainPath, valPath);
        } catch (Exception e) {
            log.error("上传图片失败：imageName={}, path={}, error={}", imageName, localImagePath, e.getMessage());
            return null;
        }

        return new ImageLocalInfo(localImagePath, downloadedFile);
    }

    /**
     * 读取本地图片尺寸
     */
    private int[] readImageSize(String localPath) {
        try {
            BufferedImage img = ImageIO.read(new File(localPath));
            if (img != null) {
                return new int[]{img.getWidth(), img.getHeight()};
            }
        } catch (Exception e) {
            log.warn("读取图片尺寸失败：path={}, error={}", localPath, e.getMessage());
        }
        return new int[]{-1, -1};
    }

    /**
     * 上传标注文件到训练/验证目录
     */
    private void uploadLabelFile(ChannelSftp sftp, String datasetPath, String imageName,
                                 List<Map<String, Object>> annotationMaps,
                                 Map<String, Integer> labelIndexMap,
                                 double imageWidth, double imageHeight) throws Exception {
        int dotIndex = imageName.lastIndexOf('.');
        if (dotIndex <= 0) {
            log.warn("图片名称缺少扩展名，跳过标注上传：{}", imageName);
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
        log.info("标注文件上传完成：train={}, val={}", trainLabelPath, valLabelPath);

        if (!localLabelFile.delete()) {
            log.debug("临时标注文件删除失败：{}", localLabelFile.getAbsolutePath());
        }
    }

    /**
     * 上传数据集 YAML 文件
     */
    private void uploadDatasetYaml(ChannelSftp sftp, String datasetPath, String yamlContent) throws Exception {
        String datasetFileName = "dataset.yaml";
        File localYamlFile = new File(System.getProperty("java.io.tmpdir"), datasetFileName);
        Files.write(localYamlFile.toPath(), yamlContent.getBytes(StandardCharsets.UTF_8));

        String remoteYamlPath = datasetPath + "/" + datasetFileName;
        sftp.put(localYamlFile.getAbsolutePath(), remoteYamlPath);
        log.info("数据集配置文件上传完成：{}", remoteYamlPath);

        if (!localYamlFile.delete()) {
            log.debug("临时配置文件删除失败：{}", localYamlFile.getAbsolutePath());
        }
    }

    /**
     * 从标注实例中提取图片名，imageId 为空时用于兜底匹配
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
            log.debug("解析标注实例图片名失败：{}", e.getMessage());
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

                    // 判断是否已归一化：四个值都在(0,1]视为相对比例
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

                    // 裁剪到 [0,1]
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
                log.error("处理标注数据失败：{}", e.getMessage());
            }
        }

        return content.toString();
    }

    /**
     * 构建数据集 YAML 内容
     */
    private String buildDatasetYaml(AlgorithmAnnotation algorithmAnnotation, Set<String> labelNames) {
        StringBuilder content = new StringBuilder();
        // 使用相对上一级的路径，便于在 yolo 项目下引用
        content.append("path: ../datasets/vls/annotation_").append(algorithmAnnotation.getId()).append("\n");
        content.append("train: images/train\n");
        content.append("val: images/val\n");
        content.append("test: images/test\n\n");

        int labelCount = labelNames == null ? 0 : labelNames.size();
        content.append("nc: ").append(labelCount).append("\n");
        content.append("names: [");
        if (labelNames != null && !labelNames.isEmpty()) {
            content.append(labelNames.stream().collect(Collectors.joining("', '", "'", "'")));
        }
        content.append("]\n\n");
        content.append("description: ").append(algorithmAnnotation.getAnnotationName());
        return content.toString();
    }

    /**
     * 将任意数值类型统一转为 Double，兼容 Integer/Long/Double 以及数字字符串。
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
     * 生成数据集文件内容
     *
     * @param annotationName 标注名称
     * @param imageInstances 标注数据
     * @return 数据集文件内容
     */
    private String generateDatasetContent(String annotationName, List<AnnotationInstance> imageInstances) {
        StringBuilder content = new StringBuilder();
        content.append("# YOLO数据集配置文件\n");
        content.append("# 标注名称: ").append(annotationName).append("\n");
        content.append("# 创建时间: ").append(java.time.LocalDateTime.now()).append("\n\n");

        content.append("# 数据集路径配置\n");
        content.append("path: ./vls\n");
        content.append("train: images/train\n");
        content.append("val: images/val\n");
        content.append("test: images/test\n\n");

        // 从标注数据中提取类别信息
        Set<String> uniqueLabels = new HashSet<>();
        for (AnnotationInstance annotationInstance : imageInstances) {
           AnnotationLabel annotationLabel = annotationLabelService.getById(annotationInstance.getLabelId());
            String labelName = annotationLabel.getName();
            if (labelName != null && !labelName.trim().isEmpty()) {
                uniqueLabels.add(labelName);
            }
        }

        content.append("# 类别配置\n");
        content.append("nc: ").append(uniqueLabels.size()).append("\n");
        content.append("names: [");
        if (!uniqueLabels.isEmpty()) {
            content.append("'").append(String.join("', '", uniqueLabels)).append("'");
        }
        content.append("]\n\n");

        content.append("# 数据集信息\n");
        content.append("description: ").append(annotationName).append(" 标注数据集\n");
        content.append("total_images: ").append(imageInstances.size()).append("\n");
        content.append("total_annotations: ").append(imageInstances.size()).append("\n");
        content.append("unique_labels: ").append(uniqueLabels.size()).append("\n\n");

        content.append("# 注意：此配置文件仅包含基本设置\n");
        content.append("# 实际训练需要以下目录结构：\n");
        content.append("# - images/train/ (训练图片)\n");
        content.append("# - images/val/ (验证图片)\n");
        content.append("# - labels/train/ (训练标注)\n");
        content.append("# - labels/val/ (验证标注)\n");

        return content.toString();
    }

    /**
     * 从标注数据中提取标注列表
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractAnnotations(Map<String, Object> annotationData) {
        try {
            Object annotationsObj = annotationData.get("annotations");
            if (annotationsObj instanceof List) {
                return (List<Map<String, Object>>) annotationsObj;
            }
        } catch (Exception e) {
            log.error("提取标注列表失败：{}", e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * 从标注数据中提取图片数量
     */
    private int extractImageCount(Map<String, Object> annotationData) {
        try {
            String imageName = (String) annotationData.get("imageName");
            return imageName != null ? 1 : 0;
        } catch (Exception e) {
            log.error("提取图片数量失败：{}", e.getMessage());
            return 0;
        }
    }

    /**
     * 从标注数据中提取图片名称列表
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
            log.error("提取图片名称失败：{}", e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * 生成数据集文件名
     *
     * @param annotationName 标注名称
     * @return 数据集文件名
     */
    private String generateDatasetFileName(String annotationName) {
        // 移除特殊字符，只保留字母、数字和下划线
        String cleanName = annotationName.replaceAll("[^a-zA-Z0-9_]", "");

        // 转换为小写
        cleanName = cleanName.toLowerCase();

        // 添加时间戳确保唯一性
        String timestamp = String.valueOf(System.currentTimeMillis());

        // 生成数据集文件名
        String datasetFileName = cleanName + timestamp + ".yaml";

        log.info("生成数据集文件名：originalName={}, cleanName={}, datasetFileName={}",
                annotationName, cleanName, datasetFileName);

        return datasetFileName;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAnnotation(AlgorithmAnnotation annotation) {
        log.info("更新算法标注：ID={}, Name={}", annotation.getId(), annotation.getAnnotationName());

        // 获取原标注信息
        AlgorithmAnnotation existing = getById(annotation.getId());
        if (existing == null) {
            log.warn("标注不存在：ID={}", annotation.getId());
            return false;
        }

        // 重新计算进度
        annotation.setProgress(calculateProgress(annotation.getAnnotatedCount(), annotation.getTotalCount()));

        // 自动更新标注状态
        annotation.setAnnotationStatus(calculateAnnotationStatus(annotation.getProgress()));

        return updateById(annotation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAnnotation(Long id) {
        log.info("删除算法标注：ID={}", id);

        AlgorithmAnnotation annotation = getById(id);
        if (annotation == null) {
            log.warn("标注不存在：ID={}", id);
            return false;
        }

        // 删除相关的图片文件
        try {
            deleteAnnotationImages(annotation);
        } catch (Exception e) {
            log.error("删除标注图片文件失败：ID={}, Error={}", id, e.getMessage());
            // 不阻止删除操作，只记录错误
        }

        return removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteAnnotations(List<Long> ids) {
        log.info("批量删除算法标注：IDs={}", ids);

        // 先获取所有要删除的标注信息
        List<AlgorithmAnnotation> annotations = listByIds(ids);

        // 删除相关的图片文件
        for (AlgorithmAnnotation annotation : annotations) {
            try {
                deleteAnnotationImages(annotation);
            } catch (Exception e) {
                log.error("删除标注图片文件失败：ID={}, Error={}", annotation.getId(), e.getMessage());
                // 不阻止删除操作，只记录错误
            }
        }

        return removeByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateAnnotationProgress(Long id, Integer annotatedCount) {
        log.info("更新标注进度：ID={}, AnnotatedCount={}", id, annotatedCount);

        AlgorithmAnnotation annotation = getById(id);
        if (annotation == null) {
            log.warn("标注不存在：ID={}", id);
            return false;
        }

        // 计算进度
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
        log.info("批量更新标注状态：IDs={}, Status={}", ids, annotationStatus);

        UpdateWrapper<AlgorithmAnnotation> updateWrapper = new UpdateWrapper<>();
        updateWrapper.in("id", ids)
                .set("annotation_status", annotationStatus);

        return update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean startAnnotationTask(Long id) {
        log.info("开始标注任务：ID={}", id);

        UpdateWrapper<AlgorithmAnnotation> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id)
                .set("annotation_status", "partial");

        return update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completeAnnotationTask(Long id) {
        log.info("完成标注任务：ID={}", id);

        AlgorithmAnnotation annotation = getById(id);
        if (annotation == null) {
            log.warn("标注不存在：ID={}", id);
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
        log.info("重置标注任务：ID={}", id);

        UpdateWrapper<AlgorithmAnnotation> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id)
                .set("annotation_status", "none")
                .set("progress", 0)
                .set("annotated_count", 0);

        return update(updateWrapper);
    }

    @Override
    public Map<String, Object> importAnnotationData(Long id, String dataPath) {
        log.info("导入标注数据：ID={}, DataPath={}", id, dataPath);

        AlgorithmAnnotation annotation = getById(id);
        if (annotation == null) {
            log.warn("标注不存在：ID={}", id);
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "导入成功");
        result.put("importedCount", 100); // 示例数据

        // 这里可以添加实际的导入逻辑

        return result;
    }

    @Override
    public List<Map<String, Object>> getAnnotationTypeStatistics() {
        log.info("获取标注类型统计");
        return algorithmAnnotationMapper.selectAnnotationTypeStatistics();
    }

    @Override
    public List<Map<String, Object>> getAnnotationStatusStatistics() {
        log.info("获取标注状态统计");
        return algorithmAnnotationMapper.selectAnnotationStatusStatistics();
    }

    @Override
    public List<Map<String, Object>> getProgressStatistics() {
        log.info("获取标注进度统计");
        return algorithmAnnotationMapper.selectProgressStatistics();
    }

    @Override
    public Map<String, Object> getWorkloadStatistics() {
        log.info("获取标注工作量统计");
        return algorithmAnnotationMapper.selectWorkloadStatistics();
    }

    @Override
    public Map<String, Object> validateAnnotationData(Long id) {
        log.info("验证标注数据：ID={}", id);

        AlgorithmAnnotation annotation = getById(id);
        if (annotation == null) {
            log.warn("标注不存在：ID={}", id);
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "验证成功");
        result.put("validCount", annotation.getAnnotatedCount());
        result.put("invalidCount", 0);
        result.put("validationDetails", new HashMap<>());

        // 这里可以添加实际的验证逻辑

        return result;
    }

    /**
     * 计算标注进度
     *
     * @param annotatedCount 已标注数量
     * @param totalCount     总数量
     * @return 进度百分比
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
     * 根据进度计算标注状态
     *
     * @param progress 进度百分比
     * @return 标注状态
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
     * 删除标注相关的图片文件
     *
     * @param annotation 标注信息
     */
    private void deleteAnnotationImages(AlgorithmAnnotation annotation) {
        if (annotation == null || annotation.getDatasetPath() == null) {
            log.warn("标注或数据集路径为空，跳过图片删除");
            return;
        }

        try {
            String datasetPath = annotation.getDatasetPath();
            log.info("开始删除标注图片文件：ID={}, DatasetPath={}", annotation.getId(), datasetPath);

            // 如果数据集路径是相对路径，转换为绝对路径
            String absolutePath = datasetPath;
            if (!datasetPath.startsWith("/") && !datasetPath.contains(":")) {
                // 相对路径，基于当前工作目录
                String currentDir = System.getProperty("user.dir");
                absolutePath = currentDir + "/" + datasetPath;
            }

            java.io.File datasetDir = new java.io.File(absolutePath);
            if (!datasetDir.exists()) {
                log.warn("数据集目录不存在：{}", absolutePath);
                return;
            }

            // 删除目录下的所有图片文件
            deleteImageFiles(datasetDir);
            log.info("标注图片文件删除完成：ID={}", annotation.getId());

        } catch (Exception e) {
            log.error("删除标注图片文件失败：ID={}, Error={}", annotation.getId(), e.getMessage());
            throw e;
        }
    }

    /**
     * 递归删除目录下的图片文件
     *
     * @param directory 目录
     */
    private void deleteImageFiles(java.io.File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }

        java.io.File[] files = directory.listFiles();
        if (files != null) {
            for (java.io.File file : files) {
                if (file.isDirectory()) {
                    // 递归删除子目录
                    deleteImageFiles(file);
                    // 删除空目录
                    if (file.listFiles() == null || Objects.requireNonNull(file.listFiles()).length == 0) {
                        file.delete();
                        log.debug("删除空目录：{}", file.getAbsolutePath());
                    }
                } else if (isImageFile(file.getName())) {
                    // 删除图片文件
                    boolean deleted = file.delete();
                    if (deleted) {
                        log.debug("删除图片文件：{}", file.getAbsolutePath());
                    } else {
                        log.warn("删除图片文件失败：{}", file.getAbsolutePath());
                    }
                }
            }
        }
    }

    /**
     * 判断是否为图片文件
     *
     * @param fileName 文件名
     * @return 是否为图片文件
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
