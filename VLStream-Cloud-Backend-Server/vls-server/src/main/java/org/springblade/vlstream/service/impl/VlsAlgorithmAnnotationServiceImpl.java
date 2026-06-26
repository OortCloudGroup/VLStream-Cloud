package org.springblade.vlstream.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jcraft.jsch.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.common.constant.CommonConstant;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.core.tool.utils.StringUtil;
import org.springblade.modules.system.service.IFileUploadService;
import org.springblade.vlstream.config.VlsSshProperties;
import org.springblade.vlstream.enums.AlgorithmAnnotationStatusEnum;
import org.springblade.vlstream.enums.AlgorithmAnnotationTypeEnum;
import org.springblade.vlstream.excel.VlsAlgorithmAnnotationExcel;
import org.springblade.vlstream.mapper.VlsAlgorithmAnnotationMapper;
import org.springblade.vlstream.pojo.dto.FileResponseDto;
import org.springblade.vlstream.pojo.entity.AlgorithmAnnotation;
import org.springblade.vlstream.pojo.entity.AnnotationImage;
import org.springblade.vlstream.pojo.entity.AnnotationInstance;
import org.springblade.vlstream.pojo.entity.AnnotationLabel;
import org.springblade.vlstream.pojo.vo.AlgorithmAnnotationVO;
import org.springblade.vlstream.service.IVlsAlgorithmAnnotationService;
import org.springblade.vlstream.service.IVlsAnnotationImageService;
import org.springblade.vlstream.service.IVlsAnnotationInstanceService;
import org.springblade.vlstream.service.IVlsAnnotationLabelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.Yaml;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Algorithm annotation data table service implementation class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VlsAlgorithmAnnotationServiceImpl extends BaseServiceImpl<VlsAlgorithmAnnotationMapper, AlgorithmAnnotation> implements IVlsAlgorithmAnnotationService {

	private static final String FILE_UPLOAD_APP_ID = "818301f0e77f4cd8a117414cbeb32d9e";
	private static final String FILE_UPLOAD_SECRET_KEY = "5f0de11687d744bc95e84e207d319493";

	private final VlsAlgorithmAnnotationMapper algorithmAnnotationMapper;
	private final IVlsAnnotationImageService annotationImageService;
	private final IVlsAnnotationInstanceService annotationInstanceService;
	private final IVlsAnnotationLabelService annotationLabelService;
	private final IFileUploadService fileUploadService;
	private final VlsSshProperties sshProperties;

	@Override
	public IPage<AlgorithmAnnotationVO> selectVlsAlgorithmAnnotationPage(IPage<AlgorithmAnnotationVO> page, AlgorithmAnnotationVO vlsAlgorithmAnnotation) {
		return page.setRecords(baseMapper.selectVlsAlgorithmAnnotationPage(page, vlsAlgorithmAnnotation));
	}

	@Override
	public List<VlsAlgorithmAnnotationExcel> exportVlsAlgorithmAnnotation(Wrapper<AlgorithmAnnotation> queryWrapper) {
		List<VlsAlgorithmAnnotationExcel> vlsAlgorithmAnnotationList = baseMapper.exportVlsAlgorithmAnnotation(queryWrapper);
		//vlsAlgorithmAnnotationList.forEach(vlsAlgorithmAnnotation -> {
		//	vlsAlgorithmAnnotation.setTypeName(DictCache.getValue(DictEnum.YES_NO, VlsAlgorithmAnnotationEntity.getType()));
		//});
		return vlsAlgorithmAnnotationList;
	}

	@Override
	public IPage<AlgorithmAnnotation> selectAnnotationPage(Page<AlgorithmAnnotation> page,
														   String annotationName,
														   String annotationType,
														   String annotationStatus) {
		log.info("Paginated query for algorithm annotation list, parameters: annotationName={}, annotationType={}, annotationStatus={}",
			annotationName, annotationType, annotationStatus);

		// Convert empty string to null for correct processing of SQL query conditions
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

		// Check if annotation name is duplicated
		QueryWrapper<AlgorithmAnnotation> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("annotation_name", annotation.getAnnotationName())
			.eq("is_deleted", 0);
		if (count(queryWrapper) > 0) {
			log.warn("Annotation name already exists: {}", annotation.getAnnotationName());
			return false;
		}

		// Set default value
		if (annotation.getTotalCount() == null) {
			annotation.setTotalCount(0);
		}
		if (annotation.getAnnotatedCount() == null) {
			annotation.setAnnotatedCount(0);
		}
		if (annotation.getAnnotationStatus() == null) {
			annotation.setAnnotationStatus(AlgorithmAnnotationStatusEnum.none);
		}
		if (annotation.getProgress() == null) {
			annotation.setProgress(0);
		}

		// Dataset path is set when saving annotations, it is not automatically generated here
		if (annotation.getDatasetPath() == null) {
			annotation.setDatasetPath(null);
			log.info("Dataset path will be set when saving annotations");
		}

		// Calculate progress
		annotation.setProgress(calculateProgress(annotation.getAnnotatedCount(), annotation.getTotalCount()));

		return save(annotation);
	}

	/**
	 * Save annotation data to dataset file
	 *
	 * @param annotationId   annotation ID
	 * @return whether saved successfully
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
				log.warn("No annotated images found, unable to generate dataset: annotationId={}", annotationId);
				return false;
			}

			List<AnnotationInstance> imageInstances = annotationInstanceService.getByAnnotationId(annotationId);
			Map<Long, List<AnnotationInstance>> instancesByImageId = new HashMap<>();
			Map<String, List<AnnotationInstance>> instancesByImageName = new HashMap<>();
			Map<Long, String> labelIdNameMap = new HashMap<>();

			// Preload all tags under the current labeling, and establish labelId -> name mapping
			List<AnnotationLabel> allLabels = annotationLabelService.getByAnnotationIdWithUsageCount(annotationId);
			if (allLabels != null) {
				for (AnnotationLabel label : allLabels) {
					labelIdNameMap.put(label.getId(), label.getName());
				}
			}

			// Aggregate instances by imageId / imageName and collect label names
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

			String datasetsRoot = CommonConstant.BASE_DATASETS_PATH + "vls";
			String datasetPath = datasetsRoot + "/annotation_" + annotationId;

			// Establish SFTP connection
			JSch jsch = new JSch();
			session = jsch.getSession(sshProperties.getUsername(), sshProperties.getHost(), sshProperties.getPort());
			session.setPassword(sshProperties.getPassword());
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

			Map<Long, ImageLocalInfo> localImageInfoMap = new HashMap<>();
			List<String> uploadedImageNames = new ArrayList<>();
			ExecutorService downloadExecutor = null;
			try {
				int downloadThreadCount = Math.max(1, Math.min(images.size(),
					Math.max(2, Runtime.getRuntime().availableProcessors())));
				downloadExecutor = Executors.newFixedThreadPool(downloadThreadCount);
				Map<Long, Future<ImageLocalInfo>> downloadFutures = new LinkedHashMap<>();
				for (AnnotationImage image : images) {
					Long imageId = image.getId();
					if (imageId == null) {
						log.warn("Image id is null, skip download: annotationId={}, imageName={}", annotationId, image.getImageName());
						continue;
					}
					downloadFutures.put(imageId, downloadExecutor.submit(() -> downloadImageFile(image)));
				}
				for (Map.Entry<Long, Future<ImageLocalInfo>> downloadEntry : downloadFutures.entrySet()) {
					ImageLocalInfo imageInfo = getDownloadResult(downloadEntry.getValue(), downloadEntry.getKey());
					if (imageInfo != null) {
						localImageInfoMap.put(downloadEntry.getKey(), imageInfo);
					}
				}
			} finally {
				if (downloadExecutor != null) {
					downloadExecutor.shutdown();
					try {
						if (!downloadExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
							downloadExecutor.shutdownNow();
						}
					} catch (InterruptedException interruptedException) {
						downloadExecutor.shutdownNow();
						Thread.currentThread().interrupt();
					}
				}
			}

			for (AnnotationImage image : images) {
				String imageName = image.getImageName();

				// Upload image to train/val using local temp file
				Long imageId = image.getId();
				ImageLocalInfo localImageInfo = imageId != null ? localImageInfoMap.get(imageId) : null;
				if (localImageInfo == null) {
					log.warn("Image download missing, skip process: annotationId={}, imageId={}, imageName={}", annotationId, imageId, imageName);
					continue;
				}
				ImageLocalInfo imageInfo = uploadImageFile(sftp, datasetPath, image, localImageInfo);
				if (imageInfo == null) {
					log.warn("Image upload failed, skip process: annotationId={}, imageId={}, imageName={}", annotationId, imageId, imageName);
					continue;
				}
				uploadedImageNames.add(imageName);

				int[] dims = readImageSize(imageInfo.localPath);
				double imageWidth = dims[0] > 0 ? dims[0] : -1;
				double imageHeight = dims[1] > 0 ? dims[1] : -1;

				// Process and upload the corresponding annotation file
				List<Map<String, Object>> annotationMaps = new ArrayList<>();
				List<AnnotationInstance> perImageInstances = new ArrayList<>();
				if (instancesByImageId.containsKey(image.getId())) {
					perImageInstances.addAll(instancesByImageId.get(image.getId()));
				}
				if (perImageInstances.isEmpty()) {
					perImageInstances.addAll(instancesByImageName.getOrDefault(imageName, new ArrayList<>()));
				}
				if (perImageInstances.isEmpty()) {
					// Fallback: Query again by annotationId + imageName
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

				// Delete temporary files
				if (imageInfo.tempFile != null && imageInfo.tempFile.exists() && !imageInfo.tempFile.delete()) {
					log.debug("Failed to delete temporary image: {}", imageInfo.tempFile.getAbsolutePath());
				}
			}

			// If label names are not collected in the loop, fall back to fill in according to the label list under the annotation
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
			List<String> cocoSubsetPaths = uploadedImageNames.stream()
				.filter(Objects::nonNull)
				.map(String::trim)
				.filter(name -> !name.isEmpty())
				.distinct()
				.limit(20)
				.map(name -> datasetPath + "/images/train/" + name)
				.collect(Collectors.toList());
			uploadCocoSubsetFile(sftp, datasetPath, cocoSubsetPaths);

			UpdateWrapper<AlgorithmAnnotation> updateWrapper = new UpdateWrapper<>();
			updateWrapper.eq("id", annotationId).set("dataset_path", datasetPath + "/dataset.yaml");
			boolean updateResult = update(updateWrapper);
			if (!updateResult) {
				log.warn("Failed to update dataset path: annotationId={}", annotationId);
			}

			return true;
		} catch (Exception e) {
			e.printStackTrace();
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
	 * @param annotationData annotation data in JSON format
	 * @return parsed data
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
	 * @param path directory path
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
				// Directory does not exist, creating it
				try {
					sftp.mkdir(currentPath);
					log.info("Create remote directory: {}", currentPath);
				} catch (SftpException mkdirException) {
					log.warn("Failed to create directory, it may already exist: {}", currentPath);
				}
			}
		}
	}

	/**
	 * Create complete YOLO dataset directory structure
	 *
	 * @param sftp        SFTP channel
	 * @param datasetPath dataset path
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
			// Recursive creation to avoid mkdir errors caused by the non-existence of parent directories
			createRemoteDirectory(sftp, subdir);
			log.info("Ensure subdirectory exists: {}", subdir);
		}

		log.info("Complete YOLO dataset directory structure created: {}", datasetPath);
	}

	private ImageLocalInfo getDownloadResult(Future<ImageLocalInfo> downloadFuture, Long imageId) {
		if (downloadFuture == null) {
			return null;
		}
		try {
			return downloadFuture.get(60, TimeUnit.SECONDS);
		} catch (TimeoutException timeoutException) {
			downloadFuture.cancel(true);
			log.warn("Image download timeout: imageId={}", imageId);
			return null;
		} catch (InterruptedException interruptedException) {
			Thread.currentThread().interrupt();
			log.warn("Image download interrupted: imageId={}", imageId);
			return null;
		} catch (ExecutionException executionException) {
			log.warn("Image download failed: imageId={}, error={}", imageId, executionException.getMessage());
			return null;
		}
	}

	/**
	 * Download image to local temp file.
	 */
	private ImageLocalInfo downloadImageFile(AnnotationImage image) {
		if (image == null) {
			return null;
		}
		String imageName = image.getImageName();
		String rawPath = image.getLocalPath();
		if (rawPath == null || rawPath.trim().isEmpty()) {
			log.warn("Image path is empty, skip download: imageName={}", imageName);
			return null;
		}
		File downloadedFile = FileUtil.createTempFile();
		String downloadPath = rawPath.replaceAll("http://183.62.103.20:21410/bus/wj1", "http://go-fastdfs-management.p-lvgemgroup:8080");
		log.info("Image download path: imageName={}, path={}", imageName, downloadPath);
		try {
			HttpUtil.downloadFile(downloadPath, downloadedFile);
		} catch (Exception downloadException) {
			log.warn("Image download failed: imageName={}, path={}, error={}", imageName, downloadPath, downloadException.getMessage());
			return null;
		}
		return new ImageLocalInfo(downloadedFile.getAbsolutePath(), downloadedFile);
	}

	/**
	 * Upload image to train/val directories using local temp file.
	 */
	private ImageLocalInfo uploadImageFile(ChannelSftp sftp, String datasetPath, AnnotationImage image, ImageLocalInfo imageInfo) {
		if (imageInfo == null || imageInfo.localPath == null) {
			log.warn("Local image missing, skip upload: imageName={}", image != null ? image.getImageName() : null);
			return null;
		}
		String imageName = image.getImageName();
		String localImagePath = imageInfo.localPath;
		String trainPath = datasetPath + "/images/train/" + imageName;
		String valPath = datasetPath + "/images/val/" + imageName;
		try {
			sftp.put(localImagePath, trainPath);
			sftp.put(localImagePath, valPath);
			log.info("Image upload completed: train={}, val={}", trainPath, valPath);
		} catch (Exception uploadException) {
			log.error("Image upload failed: imageName={}, path={}, error={}", imageName, localImagePath, uploadException.getMessage());
			return null;
		}
		return imageInfo;
	}

	/**
	 * Read local image dimensions
	 */
	private int[] readImageSize(String localPath) {
		try {
			BufferedImage img = ImageIO.read(new File(localPath));
			if (img != null) {
				return new int[]{img.getWidth(), img.getHeight()};
			}
		} catch (Exception e) {
			log.warn("Failed to read image dimensions: path={}, error={}", localPath, e.getMessage());
		}
		return new int[]{-1, -1};
	}

	/**
	 * Upload annotation files to training/validation directory
	 */
	private void uploadLabelFile(ChannelSftp sftp, String datasetPath, String imageName,
								 List<Map<String, Object>> annotationMaps,
								 Map<String, Integer> labelIndexMap,
								 double imageWidth, double imageHeight) throws Exception {
		int dotIndex = imageName.lastIndexOf('.');
		if (dotIndex <= 0) {
			log.warn("Image name is missing extension, skip annotation upload: {}", imageName);
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
		log.info("Annotation file upload completed: train={}, val={}", trainLabelPath, valLabelPath);

		if (!localLabelFile.delete()) {
			log.debug("Failed to delete temporary annotation file: {}", localLabelFile.getAbsolutePath());
		}
	}

	/**
	 * Upload dataset YAML file
	 */
	private void uploadDatasetYaml(ChannelSftp sftp, String datasetPath, String yamlContent) throws Exception {
		String datasetFileName = "dataset.yaml";
		File localYamlFile = new File(System.getProperty("java.io.tmpdir"), datasetFileName);
		Files.writeString(localYamlFile.toPath(), yamlContent);

		String remoteYamlPath = datasetPath + "/" + datasetFileName;
		sftp.put(localYamlFile.getAbsolutePath(), remoteYamlPath);
		log.info("Dataset configuration file upload completed: {}", remoteYamlPath);

		if (!localYamlFile.delete()) {
			log.debug("Failed to delete temporary configuration file: {}", localYamlFile.getAbsolutePath());
		}
	}

	/**
	 * Generate RKNN Model Zoo coco subset file for int8 quantization.
	 */
	private void uploadCocoSubsetFile(ChannelSftp sftp, String datasetPath, List<String> imagePaths) throws Exception {
		String subsetFileName = "coco_subset_20.txt";
		File localSubsetFile = new File(System.getProperty("java.io.tmpdir"), subsetFileName);
		String content = imagePaths == null ? "" : String.join("\n", imagePaths);
		Files.writeString(localSubsetFile.toPath(), content);

		String remoteSubsetPath = datasetPath + "/" + subsetFileName;
		sftp.put(localSubsetFile.getAbsolutePath(), remoteSubsetPath);
		log.info("coco subset uploaded: path={}, count={}", remoteSubsetPath, imagePaths == null ? 0 : imagePaths.size());

		if (!localSubsetFile.delete()) {
			log.debug("Temp coco subset delete failed: {}", localSubsetFile.getAbsolutePath());
		}
	}

	/**
	 * Extract image name from annotation instance, used as fallback matching when imageId is empty
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
			log.debug("Failed to parse labeled instance image name: {}", e.getMessage());
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

					// Determine if normalized: four values all in (0, 1] are treated as relative ratios
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
	 * Build Dataset YAML Content
	 */
	private String buildDatasetYaml(AlgorithmAnnotation algorithmAnnotation, Set<String> labelNames) {
		StringBuilder content = new StringBuilder();
		// Use the path relative to the parent directory to facilitate referencing under the YOLO project
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
	 * Convert any numeric type to Double, compatible with Integer/Long/Double and numeric strings.
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
	 * @param annotationName annotation name
	 * @param imageInstances annotation data
	 * @return dataset file content
	 */
	private String generateDatasetContent(String annotationName, List<AnnotationInstance> imageInstances) {
		StringBuilder content = new StringBuilder();
		content.append("# YOLO dataset configuration file\n");
		content.append("# Annotation name:").append(annotationName).append("\n");
		content.append("# Creation time:").append(java.time.LocalDateTime.now()).append("\n\n");

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

		content.append("# Class configuration\n");
		content.append("nc: ").append(uniqueLabels.size()).append("\n");
		content.append("names: [");
		if (!uniqueLabels.isEmpty()) {
			content.append("'").append(String.join("', '", uniqueLabels)).append("'");
		}
		content.append("]\n\n");

		content.append("# Dataset information\n");
		content.append("description: ").append(annotationName).append("Annotation dataset\n");
		content.append("total_images: ").append(imageInstances.size()).append("\n");
		content.append("total_annotations: ").append(imageInstances.size()).append("\n");
		content.append("unique_labels: ").append(uniqueLabels.size()).append("\n\n");

		content.append("# Note: This configuration file only contains basic settings\n");
		content.append("# Actual training requires the following directory structure:\n");
		content.append("# - images/train/ (Training images)\n");
		content.append("# - images/val/ (Validation images)\n");
		content.append("# - labels/train/ (Training annotations)\n");
		content.append("# - labels/val/ (Validation annotations)\n");

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
			log.error("Failed to extract image quantity: {}", e.getMessage());
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
	 * @param annotationName annotation name
	 * @return dataset file name
	 */
	private String generateDatasetFileName(String annotationName) {
		// Remove special characters, keeping only letters, digits, and underscores
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

		// Get original annotation info
		AlgorithmAnnotation existing = getById(annotation.getId());
		if (existing == null) {
			log.warn("Annotation does not exist: ID={}", annotation.getId());
			return false;
		}

		// Recalculate progress
		annotation.setProgress(calculateProgress(annotation.getAnnotatedCount(), annotation.getTotalCount()));

		// Automatically update annotation status
		annotation.setAnnotationStatus(AlgorithmAnnotationStatusEnum.of(calculateAnnotationStatus(annotation.getProgress())));

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
			log.error("Failed to delete annotated image file: ID={}, Error={}", id, e.getMessage());
			// Do not block delete operation, only log error
		}

		return removeById(id);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean batchDeleteAnnotations(List<Long> ids) {
		log.info("Batch delete algorithm annotations: IDs={}", ids);

		// Retrieve all annotation information to be deleted first
		List<AlgorithmAnnotation> annotations = listByIds(ids);

		// Delete related image files
		for (AlgorithmAnnotation annotation : annotations) {
			try {
				deleteAnnotationImages(annotation);
			} catch (Exception e) {
				log.error("Failed to delete annotated image file: ID={}, Error={}", annotation.getId(), e.getMessage());
				// Do not block delete operation, only log error
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
		log.info("Reset labeling task: ID={}", id);

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
		result.put("message", "Imported successfully");
		result.put("importedCount", 100); // Sample data

		// Actual import logic can be added here

		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public Map<String, Object> importAnnotationDatasetZip(Long annotationId, MultipartFile zipFile) {
		String originalFileName = zipFile == null ? null : zipFile.getOriginalFilename();
		log.info("Import annotation dataset zip: annotationId={}, fileName={}", annotationId, originalFileName);

		if (annotationId == null) {
			throw new IllegalArgumentException("annotationId is required");
		}
		if (zipFile == null || zipFile.isEmpty()) {
			throw new IllegalArgumentException("zipFile is empty");
		}

		AlgorithmAnnotation annotation = getById(annotationId);
		if (annotation == null) {
			throw new IllegalArgumentException("Annotation not found: " + annotationId);
		}

		Path tempDirectory = null;
		try {
			tempDirectory = Files.createTempDirectory("annotation-zip-");
			Path zipPath = saveZipToTemp(tempDirectory, zipFile);
			Path extractRoot = tempDirectory.resolve("unzipped");
			unzipFile(zipPath, extractRoot);

			Path datasetYamlPath = locateDatasetYaml(extractRoot);
			if (datasetYamlPath == null) {
				throw new IllegalArgumentException("dataset.yaml not found in zip");
			}

			Path datasetRoot = datasetYamlPath.getParent();
			Path imagesRoot = datasetRoot.resolve("images");
			Path labelsRoot = datasetRoot.resolve("labels");
			if (!Files.isDirectory(imagesRoot)) {
				throw new IllegalArgumentException("images directory not found");
			}
			if (!Files.isDirectory(labelsRoot)) {
				throw new IllegalArgumentException("labels directory not found");
			}

			DatasetYamlInfo datasetYamlInfo = parseDatasetYaml(datasetYamlPath);
			List<String> labelNames = new ArrayList<>(datasetYamlInfo.labelNames);
			if (labelNames.isEmpty() && datasetYamlInfo.labelCount != null && datasetYamlInfo.labelCount > 0) {
				for (int index = 0; index < datasetYamlInfo.labelCount; index++) {
					labelNames.add("class_" + index);
				}
			}
			expandLabelNamesFromLabelFiles(labelsRoot, labelNames);

			List<Path> imageFiles = collectImageFiles(imagesRoot);
			if (imageFiles.isEmpty()) {
				throw new IllegalArgumentException("No images found in dataset");
			}

			clearExistingAnnotationData(annotationId);

			Map<Integer, AnnotationLabel> labelByIndex = createLabels(annotationId, labelNames);
			Map<Integer, Long> labelIdByIndex = labelByIndex.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getId()));

			Set<String> usedImageNames = new HashSet<>();
			int totalImages = 0;
			int totalInstances = 0;

			for (Path imagePath : imageFiles) {
				String imageName = resolveImageName(imagesRoot, imagePath, usedImageNames);
				AnnotationImage savedImage = uploadAndSaveImage(annotationId, imageName, imagePath);
				totalImages++;

				Path labelPath = resolveLabelPath(labelsRoot, imagesRoot, imagePath);
				List<YoloLabelRecord> labelRecords = readLabelRecords(labelPath);
				if (labelRecords.isEmpty()) {
					continue;
				}

				int[] imageSize = readImageSize(imagePath.toString());
				int imageWidth = imageSize[0];
				int imageHeight = imageSize[1];
				if (imageWidth <= 0 || imageHeight <= 0) {
					throw new IllegalStateException("Invalid image size: " + imagePath.getFileName());
				}

				for (YoloLabelRecord record : labelRecords) {
					Long labelId = labelIdByIndex.get(record.classId);
					if (labelId == null) {
						labelId = createMissingLabel(annotationId, record.classId, labelNames, labelIdByIndex);
					}
					String labelName = labelNames.size() > record.classId ? labelNames.get(record.classId) : "class_" + record.classId;
					Map<String, Object> annotationData = buildAnnotationData(record, imageName, labelName, imageWidth, imageHeight);

					AnnotationInstance instance = new AnnotationInstance();
					instance.setAnnotationId(annotationId);
					instance.setLabelId(labelId);
					instance.setImageId(savedImage.getId());
					instance.setAnnotationType(AlgorithmAnnotationTypeEnum.rect);
					instance.setAnnotationData(JSONUtil.toJsonStr(annotationData));
					instance.setConfidence(new BigDecimal("1.0000"));
					instance.setVerified(0);

					annotationInstanceService.save(instance);
					totalInstances++;
				}
			}

			labelIdByIndex.values().forEach(annotationLabelService::updateUsageCount);

			int progress = calculateProgress(totalInstances, totalImages);
			AlgorithmAnnotationStatusEnum statusEnum = AlgorithmAnnotationStatusEnum.of(calculateAnnotationStatus(progress));

			UpdateWrapper<AlgorithmAnnotation> updateWrapper = new UpdateWrapper<>();
			updateWrapper.eq("id", annotationId)
				.set("total_count", totalImages)
				.set("annotated_count", totalInstances)
				.set("progress", progress)
				.set("annotation_status", statusEnum);
			update(updateWrapper);

			Map<String, Object> result = new HashMap<>();
			result.put("success", true);
			result.put("message", "Import success");
			result.put("totalImages", totalImages);
			result.put("totalInstances", totalInstances);
			result.put("totalLabels", labelNames.size());
			return result;
		} catch (Exception importException) {
			log.error("Import annotation dataset zip failed: annotationId={}, fileName={}, error={}",
				annotationId, originalFileName, importException.getMessage(), importException);
			throw new RuntimeException("Import annotation dataset zip failed: " + importException.getMessage(), importException);
		} finally {
			if (tempDirectory != null) {
				FileUtil.del(tempDirectory.toFile());
			}
		}
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
		log.info("Get annotation progress statistics");
		return algorithmAnnotationMapper.selectProgressStatistics();
	}

	@Override
	public Map<String, Object> getWorkloadStatistics() {
		log.info("Get annotation workload statistics");
		return algorithmAnnotationMapper.selectWorkloadStatistics();
	}

	@Override
	public Map<String, Object> validateAnnotationData(Long id) {
		log.info("Verify labeling data: ID={}", id);

		AlgorithmAnnotation annotation = getById(id);
		if (annotation == null) {
			log.warn("Annotation does not exist: ID={}", id);
			return null;
		}

		Map<String, Object> result = new HashMap<>();
		result.put("success", true);
		result.put("message", "Verification successful");
		result.put("validCount", annotation.getAnnotatedCount());
		result.put("invalidCount", 0);
		result.put("validationDetails", new HashMap<>());

		// Actual validation logic can be added here

		return result;
	}

	private static class DatasetYamlInfo {
		private final List<String> labelNames;
		private final Integer labelCount;

		private DatasetYamlInfo(List<String> labelNames, Integer labelCount) {
			this.labelNames = labelNames;
			this.labelCount = labelCount;
		}
	}

	private static class YoloLabelRecord {
		private final int classId;
		private final double centerX;
		private final double centerY;
		private final double width;
		private final double height;

		private YoloLabelRecord(int classId, double centerX, double centerY, double width, double height) {
			this.classId = classId;
			this.centerX = centerX;
			this.centerY = centerY;
			this.width = width;
			this.height = height;
		}
	}

	private Path saveZipToTemp(Path tempDirectory, MultipartFile zipFile) throws IOException {
		String originalFileName = zipFile.getOriginalFilename();
		String safeFileName = originalFileName == null || originalFileName.trim().isEmpty()
			? "dataset.zip"
			: sanitizeFileName(Paths.get(originalFileName).getFileName().toString());
		if (!safeFileName.toLowerCase(Locale.ROOT).endsWith(".zip")) {
			safeFileName = safeFileName + ".zip";
		}
		Path zipPath = tempDirectory.resolve(safeFileName);
		zipFile.transferTo(zipPath.toFile());
		return zipPath;
	}

	private void unzipFile(Path zipPath, Path targetDirectory) throws IOException {
		Files.createDirectories(targetDirectory);
		try (InputStream inputStream = Files.newInputStream(zipPath);
			 ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
			ZipEntry entry;
			while ((entry = zipInputStream.getNextEntry()) != null) {
				String entryName = entry.getName().replace("\\", "/");
				Path entryTarget = targetDirectory.resolve(entryName).normalize();
				if (!entryTarget.startsWith(targetDirectory)) {
					throw new IOException("Invalid zip entry: " + entry.getName());
				}
				if (entry.isDirectory()) {
					Files.createDirectories(entryTarget);
				} else {
					Path parent = entryTarget.getParent();
					if (parent != null) {
						Files.createDirectories(parent);
					}
					try (OutputStream outputStream = Files.newOutputStream(entryTarget)) {
						zipInputStream.transferTo(outputStream);
					}
				}
				zipInputStream.closeEntry();
			}
		}
	}

	private Path locateDatasetYaml(Path rootDirectory) throws IOException {
		try (Stream<Path> pathStream = Files.walk(rootDirectory)) {
			return pathStream
				.filter(path -> "dataset.yaml".equalsIgnoreCase(path.getFileName().toString()))
				.findFirst()
				.orElse(null);
		}
	}

	@SuppressWarnings("unchecked")
	private DatasetYamlInfo parseDatasetYaml(Path yamlPath) throws IOException {
		String yamlContent = Files.readString(yamlPath, StandardCharsets.UTF_8);
		Yaml yaml = new Yaml();
		Object yamlObject = yaml.load(yamlContent);
		if (!(yamlObject instanceof Map)) {
			return new DatasetYamlInfo(new ArrayList<>(), null);
		}
		Map<String, Object> yamlMap = (Map<String, Object>) yamlObject;
		List<String> labelNames = parseLabelNames(yamlMap.get("names"));
		Integer labelCount = parseLabelCount(yamlMap.get("nc"));
		return new DatasetYamlInfo(labelNames, labelCount);
	}

	private List<String> parseLabelNames(Object namesObject) {
		if (namesObject == null) {
			return new ArrayList<>();
		}
		if (namesObject instanceof List<?>) {
			List<?> rawList = (List<?>) namesObject;
			return rawList.stream()
				.map(item -> item == null ? null : item.toString().trim())
				.filter(value -> value != null && !value.isEmpty())
				.collect(Collectors.toList());
		}
		if (namesObject instanceof Map<?, ?> rawMap) {
			Map<Integer, String> sortedMap = new TreeMap<>();
			for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
				Integer index = parseInteger(entry.getKey());
				String labelName = entry.getValue() == null ? null : entry.getValue().toString().trim();
				if (index != null && labelName != null && !labelName.isEmpty()) {
					sortedMap.put(index, labelName);
				}
			}
			List<String> names = new ArrayList<>();
			for (Map.Entry<Integer, String> entry : sortedMap.entrySet()) {
				int index = entry.getKey();
				while (names.size() < index) {
					names.add("class_" + names.size());
				}
				if (names.size() == index) {
					names.add(entry.getValue());
				} else {
					names.set(index, entry.getValue());
				}
			}
			return names;
		}
		String nameValue = namesObject.toString().trim();
		if (nameValue.isEmpty()) {
			return new ArrayList<>();
		}
		List<String> names = new ArrayList<>();
		names.add(nameValue);
		return names;
	}

	private Integer parseLabelCount(Object value) {
		Integer parsed = parseInteger(value);
		return parsed == null ? null : Math.max(parsed, 0);
	}

	private Integer parseInteger(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number) {
			return ((Number) value).intValue();
		}
		if (value instanceof String) {
			try {
				return Integer.parseInt(((String) value).trim());
			} catch (NumberFormatException ignoredException) {
				return null;
			}
		}
		return null;
	}

	private void expandLabelNamesFromLabelFiles(Path labelsRoot, List<String> labelNames) throws IOException {
		int maxClassId = resolveMaxClassId(labelsRoot);
		for (int index = labelNames.size(); index <= maxClassId; index++) {
			labelNames.add("class_" + index);
		}
	}

	private int resolveMaxClassId(Path labelsRoot) throws IOException {
		try (Stream<Path> pathStream = Files.walk(labelsRoot)) {
			OptionalInt maxValue = pathStream
				.filter(Files::isRegularFile)
				.filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".txt"))
				.map(this::extractClassIds)
				.flatMap(Collection::stream)
				.mapToInt(Integer::intValue)
				.max();
			return maxValue.orElse(-1);
		}
	}

	private List<Integer> extractClassIds(Path labelPath) {
		try (Stream<String> lineStream = Files.lines(labelPath, StandardCharsets.UTF_8)) {
			return lineStream
				.map(String::trim)
				.filter(line -> !line.isEmpty() && !line.startsWith("#"))
				.map(this::parseClassId)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		} catch (IOException ioException) {
			log.warn("Read label file failed: {}", labelPath.getFileName(), ioException);
			return Collections.emptyList();
		}
	}

	private Integer parseClassId(String line) {
		String[] parts = line.trim().split("\\s+");
		if (parts.length < 1) {
			return null;
		}
		return parseInteger(parts[0]);
	}

	private List<Path> collectImageFiles(Path imagesRoot) throws IOException {
		try (Stream<Path> pathStream = Files.walk(imagesRoot)) {
			return pathStream
				.filter(Files::isRegularFile)
				.filter(path -> isImageFile(path.getFileName().toString()))
				.sorted(Comparator.comparing(path -> imagesRoot.relativize(path).toString()))
				.collect(Collectors.toList());
		}
	}

	private void clearExistingAnnotationData(Long annotationId) {
		QueryWrapper<AnnotationInstance> instanceWrapper = new QueryWrapper<>();
		instanceWrapper.eq("annotation_id", annotationId);
		annotationInstanceService.remove(instanceWrapper);

		QueryWrapper<AnnotationImage> imageWrapper = new QueryWrapper<>();
		imageWrapper.eq("annotation_id", annotationId);
		annotationImageService.remove(imageWrapper);

		QueryWrapper<AnnotationLabel> labelWrapper = new QueryWrapper<>();
		labelWrapper.eq("annotation_id", annotationId);
		annotationLabelService.remove(labelWrapper);
	}

	private Map<Integer, AnnotationLabel> createLabels(Long annotationId, List<String> labelNames) {
		Map<Integer, AnnotationLabel> labelByIndex = new LinkedHashMap<>();
		for (int index = 0; index < labelNames.size(); index++) {
			String labelName = labelNames.get(index);
			if (labelName == null || labelName.trim().isEmpty()) {
				labelName = "class_" + index;
				labelNames.set(index, labelName);
			}

			AnnotationLabel label = new AnnotationLabel();
			label.setAnnotationId(annotationId);
			label.setName(labelName);
			label.setSortOrder(index + 1);
			label.setUsageCount(0);
			label.setColor("#409eff");
			annotationLabelService.save(label);
			labelByIndex.put(index, label);
		}
		return labelByIndex;
	}

	private AnnotationImage uploadAndSaveImage(Long annotationId, String imageName, Path imagePath) throws IOException {
		FileResponseDto fileResponse = fileUploadService.uploadFile(FILE_UPLOAD_APP_ID, FILE_UPLOAD_SECRET_KEY, imagePath.toFile());
		if (fileResponse == null || fileResponse.getUrl() == null || fileResponse.getUrl().trim().isEmpty()) {
			throw new IllegalStateException("File upload failed: " + imagePath.getFileName());
		}

		AnnotationImage annotationImage = new AnnotationImage();
		annotationImage.setAnnotationId(annotationId);
		annotationImage.setImageName(imageName);
		annotationImage.setOriginalName(sanitizeFileName(imagePath.getFileName().toString()));
		annotationImage.setLocalPath(fileResponse.getUrl());
		annotationImage.setFileSize(Files.size(imagePath));
		annotationImage.setIsImported(1);
		annotationImage.setImportTime(new Date());
		annotationImageService.saveImage(annotationImage);
		return annotationImage;
	}

	private String resolveImageName(Path imagesRoot, Path imagePath, Set<String> usedNames) {
		Path relativePath = imagesRoot.relativize(imagePath);
		Path relativeParent = relativePath.getParent();
		String baseFileName = sanitizeFileName(imagePath.getFileName().toString());
		String prefix = null;
		if (relativeParent != null) {
			prefix = relativeParent.toString().replace("\\", "_").replace("/", "_");
		}
		String baseName = prefix == null || prefix.isBlank() ? baseFileName : prefix + "_" + baseFileName;
		return ensureUniqueFileName(baseName, usedNames);
	}

	private String sanitizeFileName(String fileName) {
		if (fileName == null || fileName.trim().isEmpty()) {
			return "unnamed";
		}
		String safeName = fileName.replace("\\", "_").replace("/", "_");
		return safeName.replaceAll("[<>:\"|?*]", "_");
	}

	private String ensureUniqueFileName(String baseName, Set<String> usedNames) {
		if (usedNames.add(baseName)) {
			return baseName;
		}
		String extension = "";
		String nameWithoutExtension = baseName;
		int dotIndex = baseName.lastIndexOf('.');
		if (dotIndex > 0) {
			nameWithoutExtension = baseName.substring(0, dotIndex);
			extension = baseName.substring(dotIndex);
		}
		int counter = 1;
		String candidate = nameWithoutExtension + "_" + counter + extension;
		while (!usedNames.add(candidate)) {
			counter++;
			candidate = nameWithoutExtension + "_" + counter + extension;
		}
		return candidate;
	}

	private Path resolveLabelPath(Path labelsRoot, Path imagesRoot, Path imagePath) {
		Path relativePath = imagesRoot.relativize(imagePath);
		Path relativeParent = relativePath.getParent();
		String baseName = relativePath.getFileName().toString();
		int dotIndex = baseName.lastIndexOf('.');
		String labelFileName = dotIndex > 0 ? baseName.substring(0, dotIndex) + ".txt" : baseName + ".txt";
		if (relativeParent == null) {
			return labelsRoot.resolve(labelFileName);
		}
		return labelsRoot.resolve(relativeParent).resolve(labelFileName);
	}

	private List<YoloLabelRecord> readLabelRecords(Path labelPath) {
		if (labelPath == null || !Files.exists(labelPath) || !Files.isRegularFile(labelPath)) {
			return Collections.emptyList();
		}
		try (Stream<String> lineStream = Files.lines(labelPath, StandardCharsets.UTF_8)) {
			return lineStream
				.map(String::trim)
				.filter(line -> !line.isEmpty() && !line.startsWith("#"))
				.map(line -> parseYoloLabelLine(line, labelPath))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		} catch (IOException ioException) {
			log.warn("Read label file failed: {}", labelPath.getFileName(), ioException);
			return Collections.emptyList();
		}
	}

	private YoloLabelRecord parseYoloLabelLine(String line, Path labelPath) {
		String[] parts = line.trim().split("\\s+");
		if (parts.length < 5) {
			log.warn("Invalid label line: file={}, line={}", labelPath.getFileName(), line);
			return null;
		}
		Integer classId = parseInteger(parts[0]);
		Double centerX = toDouble(parts[1]);
		Double centerY = toDouble(parts[2]);
		Double width = toDouble(parts[3]);
		Double height = toDouble(parts[4]);
		if (classId == null || centerX == null || centerY == null || width == null || height == null) {
			log.warn("Invalid label line: file={}, line={}", labelPath.getFileName(), line);
			return null;
		}
		return new YoloLabelRecord(classId, centerX, centerY, width, height);
	}

	private Long createMissingLabel(Long annotationId, int classId, List<String> labelNames, Map<Integer, Long> labelIdByIndex) {
		while (labelNames.size() <= classId) {
			labelNames.add("class_" + labelNames.size());
		}
		String labelName = labelNames.get(classId);
		AnnotationLabel label = new AnnotationLabel();
		label.setAnnotationId(annotationId);
		label.setName(labelName);
		label.setSortOrder(classId + 1);
		label.setUsageCount(0);
		annotationLabelService.save(label);
		labelIdByIndex.put(classId, label.getId());
		return label.getId();
	}

	private Map<String, Object> buildAnnotationData(YoloLabelRecord record, String imageName, String labelName, int imageWidth, int imageHeight) {
		double boxWidth = record.width * imageWidth;
		double boxHeight = record.height * imageHeight;
		double boxX = record.centerX * imageWidth - (boxWidth / 2.0);
		double boxY = record.centerY * imageHeight - (boxHeight / 2.0);

		Map<String, Object> annotationData = new LinkedHashMap<>();
		annotationData.put("x", boxX);
		annotationData.put("y", boxY);
		annotationData.put("width", boxWidth);
		annotationData.put("height", boxHeight);
		annotationData.put("imageWidth", imageWidth);
		annotationData.put("imageHeight", imageHeight);
		annotationData.put("imageName", imageName);
		annotationData.put("labelName", labelName);
		return annotationData;
	}

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
	 * @param progress progress percentage
	 * @return annotation status
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
	 * Delete image files related to annotations
	 *
	 * @param annotation annotation information
	 */
	private void deleteAnnotationImages(AlgorithmAnnotation annotation) {
		if (annotation == null || annotation.getDatasetPath() == null) {
			log.warn("Annotation or dataset path is empty, skipping image deletion");
			return;
		}

		try {
			String datasetPath = annotation.getDatasetPath();
			log.info("Start deleting annotated image file: ID={}, DatasetPath={}", annotation.getId(), datasetPath);

			// If dataset path is relative path, convert to absolute path
			String absolutePath = datasetPath;
			if (!datasetPath.startsWith("/") && !datasetPath.contains(":")) {
				// Relative path, based on the current working directory
				String currentDir = System.getProperty("user.dir");
				absolutePath = currentDir + "/" + datasetPath;
			}

			File datasetDir = new File(absolutePath);
			if (!datasetDir.exists()) {
				log.warn("Dataset directory does not exist: {}", absolutePath);
				return;
			}

			// Delete all image files in the directory
			deleteImageFiles(datasetDir);
			log.info("Annotation image file deletion completed: ID={}", annotation.getId());

		} catch (Exception e) {
			log.error("Failed to delete annotated image file: ID={}, Error={}", annotation.getId(), e.getMessage());
			throw e;
		}
	}

	/**
	 * Recursively delete image files under the directory
	 *
	 * @param directory directory
	 */
	private void deleteImageFiles(File directory) {
		if (!directory.exists() || !directory.isDirectory()) {
			return;
		}

		File[] files = directory.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					// Recursively delete subdirectories
					deleteImageFiles(file);
					// Delete empty directory
					File[] nestedFiles = file.listFiles();
					if (nestedFiles == null || nestedFiles.length == 0) {
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
	 * Determine if it is an image file
	 *
	 * @param fileName file name
	 * @return whether it is an image file
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

	private DatasetZipTarget resolveDatasetZipTarget(String datasetPath) {
		if (StringUtil.isBlank(datasetPath)) {
			return null;
		}
		String normalizedPath = datasetPath.trim();
		String lowerPath = normalizedPath.toLowerCase(Locale.ROOT);
		boolean isYamlFile = lowerPath.endsWith(".yaml") || lowerPath.endsWith(".yml");
		String targetPath = normalizedPath;
		if (isYamlFile) {
			int fileSlash = normalizedPath.lastIndexOf('/');
			if (fileSlash < 0) {
				return null;
			}
			targetPath = normalizedPath.substring(0, fileSlash);
		}
		int lastSlash = targetPath.lastIndexOf('/');
		String parentPath;
		String datasetDirName;
		if (lastSlash >= 0) {
			parentPath = targetPath.substring(0, lastSlash);
			datasetDirName = targetPath.substring(lastSlash + 1);
			if (parentPath.isEmpty()) {
				parentPath = "/";
			}
		} else {
			parentPath = ".";
			datasetDirName = targetPath;
		}
		if (StringUtil.isBlank(datasetDirName)) {
			return null;
		}
		return new DatasetZipTarget(parentPath, datasetDirName, targetPath);
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

			DatasetZipTarget zipTarget = resolveDatasetZipTarget(datasetPath);
			if (zipTarget == null) {
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().write("Dataset path is invalid");
				return;
			}

			String zipFileName = zipTarget.getDatasetDirName() + ".zip";
			String remoteZipPath = "/tmp/" + zipTarget.getDatasetDirName() + "_" + System.currentTimeMillis() + ".zip";

			JSch jsch = new JSch();
			session = jsch.getSession(sshProperties.getUsername(), sshProperties.getHost(), sshProperties.getPort());
			session.setPassword(sshProperties.getPassword());
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect(30000);

			String zipCommand = String.format("cd \"%s\" && zip -r \"%s\" \"%s\"", zipTarget.getParentPath(), remoteZipPath, zipTarget.getDatasetDirName());
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
				String errMsg = errorStream.toString(StandardCharsets.UTF_8);
				log.error("Failed to zip dataset, path={}, exitStatus={}, error={}", zipTarget.getTargetPath(), exitStatus, errMsg);
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				response.getWriter().write("Failed to compress dataset: " + errMsg);
				return;
			}

			Channel sftpChannel = session.openChannel("sftp");
			sftpChannel.connect(30000);
			sftp = (ChannelSftp) sftpChannel;

			response.setContentType("application/zip");
			String encodedFileName = URLEncoder.encode(zipFileName, StandardCharsets.UTF_8).replace("+", "%20");
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

	@Data
	private static final class DatasetZipTarget {
		private final String parentPath;
		private final String datasetDirName;
		private final String targetPath;

		private DatasetZipTarget(String parentPath, String datasetDirName, String targetPath) {
			this.parentPath = parentPath;
			this.datasetDirName = datasetDirName;
			this.targetPath = targetPath;
		}
	}

}
