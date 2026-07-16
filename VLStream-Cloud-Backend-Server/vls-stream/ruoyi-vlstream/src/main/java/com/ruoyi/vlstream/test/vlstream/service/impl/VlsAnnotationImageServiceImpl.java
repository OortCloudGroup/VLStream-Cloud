/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.mp.base.BaseServiceImpl;
import com.ruoyi.vlstream.test.modules.system.service.IFileUploadService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAnnotationImageExcel;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmAnnotationMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAnnotationImageMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FileResponseDto;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmAnnotation;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationImage;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AnnotationImageVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAnnotationImageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;

/**
 * 标注图片信息表 服务实现类
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
					throw new RuntimeException("文件名不能为空");
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
				throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
			}
		}

		// 更新标注 totalCount
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
			log.warn("上传图片后更新标注总数失败: annotationId={}, error={}", annotationId, e.getMessage());
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
				throw new IllegalArgumentException("文件名不能为空");
			}
			if (annotationImage.getAnnotationId() == null) {
				throw new IllegalArgumentException("数据集ID不能为空");
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
			throw new RuntimeException("保存标注图片失败: " + e.getMessage(), e);
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
			throw new RuntimeException("批量保存标注图片失败: " + e.getMessage(), e);
		}
	}

	@Override
	public List<AnnotationImage> getImagesByAnnotationId(Long annotationId) {
		return annotationImageMapper.selectByAnnotationId(annotationId);
	}

}
