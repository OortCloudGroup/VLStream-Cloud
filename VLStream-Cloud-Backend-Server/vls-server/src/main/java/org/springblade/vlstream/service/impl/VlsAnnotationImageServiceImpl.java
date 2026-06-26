package org.springblade.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.modules.system.service.IFileUploadService;
import org.springblade.vlstream.excel.VlsAnnotationImageExcel;
import org.springblade.vlstream.mapper.VlsAlgorithmAnnotationMapper;
import org.springblade.vlstream.mapper.VlsAnnotationImageMapper;
import org.springblade.vlstream.pojo.dto.FileResponseDto;
import org.springblade.vlstream.pojo.entity.AlgorithmAnnotation;
import org.springblade.vlstream.pojo.entity.AnnotationImage;
import org.springblade.vlstream.pojo.vo.AnnotationImageVO;
import org.springblade.vlstream.service.IVlsAnnotationImageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;

/**
 * Annotation Image Info Table Service Implementation Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@Service
public class VlsAnnotationImageServiceImpl extends BaseServiceImpl<VlsAnnotationImageMapper, AnnotationImage> implements IVlsAnnotationImageService {

	@Resource
	private VlsAnnotationImageMapper annotationImageMapper;

	@Resource
	private IFileUploadService fileUploadService;

	@Resource
	private VlsAlgorithmAnnotationMapper algorithmAnnotationMapper;

	@Override
	public IPage<AnnotationImageVO> selectVlsAnnotationImagePage(IPage<AnnotationImageVO> page, AnnotationImageVO vlsAnnotationImage) {
		return page.setRecords(baseMapper.selectVlsAnnotationImagePage(page, vlsAnnotationImage));
	}

	@Override
	public List<VlsAnnotationImageExcel> exportVlsAnnotationImage(Wrapper<AnnotationImage> queryWrapper) {
		List<VlsAnnotationImageExcel> vlsAnnotationImageList = baseMapper.exportVlsAnnotationImage(queryWrapper);
		//vlsAnnotationImageList.forEach(vlsAnnotationImage -> {
		//	vlsAnnotationImage.setTypeName(DictCache.getValue(DictEnum.YES_NO, VlsAnnotationImageEntity.getType()));
		//});
		return vlsAnnotationImageList;
	}

	@Override
	public List<AnnotationImage> uploadImages(MultipartFile[] files, Long annotationId) {
		List<AnnotationImage> uploadedImages = new ArrayList<>();
		int addedCount = 0;

		for (MultipartFile file : files) {
			try {
				String originalName = file.getOriginalFilename();
				if (originalName == null || originalName.trim().isEmpty()) {
					throw new RuntimeException("File name cannot be empty");
				}
				Long fileSize = file.getSize();
				FileResponseDto fileResponse = fileUploadService.uploadFile("818301f0e77f4cd8a117414cbeb32d9e", "5f0de11687d744bc95e84e207d319493", fileUploadService.multipartFileToFile(file));

				originalName = Paths.get(originalName).getFileName().toString();
				originalName = originalName.replace("\\", "_")
					.replace("/", "_")
					.replaceAll("[<>:\"|?*]", "_");

				AnnotationImage annotationImage = new AnnotationImage();
				annotationImage.setAnnotationId(annotationId);
				annotationImage.setImageName(originalName);
				annotationImage.setOriginalName(originalName);
				annotationImage.setLocalPath(fileResponse.getUrl());
				annotationImage.setFileSize(fileSize);
				annotationImage.setIsImported(1);
				annotationImage.setImportTime(new Date());

				annotationImageMapper.insert(annotationImage);
				uploadedImages.add(annotationImage);
				addedCount++;

			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("File upload failed:" + e.getMessage(), e);
			}
		}

		// Update annotation totalCount
		try {
			if (addedCount > 0) {
				AlgorithmAnnotation annotation = algorithmAnnotationMapper.selectById(annotationId);
				if (annotation != null) {
					int currentTotal = annotation.getTotalCount() == null ? 0 : annotation.getTotalCount();
					annotation.setTotalCount(currentTotal + addedCount);
					algorithmAnnotationMapper.updateById(annotation);
				}
			}
		} catch (Exception e) {
			log.warn("Failed to update annotation count after uploading image: annotationId={}, error={}", annotationId, e.getMessage());
		}

		return uploadedImages;
	}

	@Override
	public List<AnnotationImage> getImagesByDataset(Long annotationId) {
		return annotationImageMapper.selectByDatasetId(annotationId);
	}

	@Override
	public AnnotationImage getImageById(Long id) {
		return annotationImageMapper.selectById(id);
	}

	@Override
	public AnnotationImage updateImage(AnnotationImage image) {
		updateById(image);
		return annotationImageMapper.selectById(image.getId());
	}

	@Override
	public void deleteImage(Long id) {
		AnnotationImage image = annotationImageMapper.selectById(id);
		if (image != null) {
			File file = new File(image.getLocalPath());
			if (file.exists()) {
				file.delete();
			}
			annotationImageMapper.deleteById(id);
		}
	}

	@Override
	public void batchDeleteImages(List<Long> ids) {
		for (Long id : ids) {
			deleteImage(id);
		}
	}

	@Override
	public Map<String, Object> getDatasetStats(Long datasetId) {
		List<AnnotationImage> images = getImagesByDataset(datasetId);

		Map<String, Object> stats = new HashMap<>();
		stats.put("totalImages", images.size());

		Map<String, Long> importStats = new HashMap<>();
		importStats.put("IMPORTED", images.stream().filter(img -> Boolean.TRUE.equals(img.getIsImported())).count());
		importStats.put("NOT_IMPORTED", images.stream().filter(img -> !Boolean.TRUE.equals(img.getIsImported())).count());
		stats.put("importStats", importStats);

		long totalSize = images.stream().mapToLong(img -> img.getFileSize() != null ? img.getFileSize() : 0).sum();
		stats.put("totalSize", totalSize);

		return stats;
	}

	@Override
	public boolean saveImage(AnnotationImage annotationImage) {
		try {
			if (annotationImage.getImageName() == null || annotationImage.getImageName().trim().isEmpty()) {
				throw new IllegalArgumentException("File name cannot be empty");
			}
			if (annotationImage.getAnnotationId() == null) {
				throw new IllegalArgumentException("Dataset ID cannot be empty");
			}

			if (annotationImage.getOriginalName() == null || annotationImage.getOriginalName().trim().isEmpty()) {
				annotationImage.setOriginalName(annotationImage.getImageName());
			}

			log.info("Saving annotation image: datasetId={}, fileName={}",
				annotationImage.getAnnotationId(), annotationImage.getImageName());

			annotationImageMapper.insert(annotationImage);
			return true;
		} catch (Exception e) {
			log.error("Failed to save annotation image: datasetId={}, fileName={}",
				annotationImage.getAnnotationId(), annotationImage.getImageName(), e);
			throw new RuntimeException("Failed to save annotated image:" + e.getMessage(), e);
		}
	}

	@Override
	public boolean batchSaveImages(List<AnnotationImage> annotationImages) {
		try {
			for (AnnotationImage image : annotationImages) {
				if (image.getIsImported() == null) {
					image.setIsImported(1);
				}
				save(image);
			}

			return true;
		} catch (Exception e) {
			throw new RuntimeException("Batch save annotated images failed:" + e.getMessage(), e);
		}
	}

	@Override
	public List<AnnotationImage> getImagesByAnnotationId(Long annotationId) {
		return annotationImageMapper.selectByAnnotationId(annotationId);
	}

}
