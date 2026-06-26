package org.springblade.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.vlstream.enums.AlgorithmAnnotationTypeEnum;
import org.springblade.vlstream.excel.VlsAnnotationInstanceExcel;
import org.springblade.vlstream.mapper.VlsAnnotationInstanceMapper;
import org.springblade.vlstream.pojo.entity.AnnotationImage;
import org.springblade.vlstream.pojo.entity.AnnotationInstance;
import org.springblade.vlstream.pojo.vo.AnnotationInstanceVO;
import org.springblade.vlstream.service.IVlsAnnotationImageService;
import org.springblade.vlstream.service.IVlsAnnotationInstanceService;
import org.springblade.vlstream.service.IVlsAnnotationLabelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Annotation Instance Entity Class Service Implementation Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@Service
public class VlsAnnotationInstanceServiceImpl extends BaseServiceImpl<VlsAnnotationInstanceMapper, AnnotationInstance> implements IVlsAnnotationInstanceService {

	@Resource
	private IVlsAnnotationLabelService annotationLabelService;

	@Resource
	private IVlsAnnotationImageService annotationImageService;

	@Override
	public IPage<AnnotationInstanceVO> selectVlsAnnotationInstancePage(IPage<AnnotationInstanceVO> page, AnnotationInstanceVO vlsAnnotationInstance) {
		return page.setRecords(baseMapper.selectVlsAnnotationInstancePage(page, vlsAnnotationInstance));
	}

	@Override
	public List<VlsAnnotationInstanceExcel> exportVlsAnnotationInstance(Wrapper<AnnotationInstance> queryWrapper) {
		List<VlsAnnotationInstanceExcel> vlsAnnotationInstanceList = baseMapper.exportVlsAnnotationInstance(queryWrapper);
		//vlsAnnotationInstanceList.forEach(vlsAnnotationInstance -> {
		//	vlsAnnotationInstance.setTypeName(DictCache.getValue(DictEnum.YES_NO, VlsAnnotationInstanceEntity.getType()));
		//});
		return vlsAnnotationInstanceList;
	}

	@Override
	public List<AnnotationInstance> getByAnnotationIdAndImageName(Long annotationId, String imageName) {
		log.debug("Query annotation instance: annotationId={}, imageName={}", annotationId, imageName);
		return baseMapper.selectByAnnotationIdAndImageName(annotationId, imageName);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public AnnotationInstance saveAnnotation(Long annotationId, Long labelId, Long imageId, AlgorithmAnnotationTypeEnum annotationType, String annotationData) {
		log.info("Save annotation instance: annotationId={}, labelId={}, imageId={}", annotationId, labelId, imageId);

		AnnotationInstance instance = new AnnotationInstance();
		instance.setAnnotationId(annotationId);
		instance.setLabelId(labelId);
		instance.setImageId(imageId);
		instance.setAnnotationType(annotationType);
		instance.setAnnotationData(annotationData);
		instance.setConfidence(new BigDecimal("1.0000"));
		instance.setVerified(0);

		save(instance);

		// Update label usage count
		annotationLabelService.updateUsageCount(labelId);

		log.info("Annotation instance saved successfully, ID: {}", instance.getId());
		return instance;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public AnnotationInstance updateAnnotation(Long instanceId, Long labelId,
											   AlgorithmAnnotationTypeEnum annotationType, String annotationData) {
		log.info("Update annotation instance: instanceId={}, labelId={}", instanceId, labelId);

		AnnotationInstance instance = baseMapper.selectById(instanceId);
		if (instance == null) {
			throw new RuntimeException("Annotation instance does not exist");
		}

		Long oldLabelId = instance.getLabelId();

		instance.setLabelId(labelId);
		instance.setAnnotationType(annotationType);
		instance.setAnnotationData(annotationData);

		updateById(instance);

		// Update label usage count
		if (!oldLabelId.equals(labelId)) {
			annotationLabelService.updateUsageCount(oldLabelId);
			annotationLabelService.updateUsageCount(labelId);
		}

		log.info("Annotation instance updated successfully");
		return instance;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deleteAnnotation(Long instanceId) {
		log.info("Delete annotation instance: instanceId={}", instanceId);

		AnnotationInstance instance = baseMapper.selectById(instanceId);
		if (instance == null) {
			throw new RuntimeException("Annotation instance does not exist");
		}

		Long labelId = instance.getLabelId();

		int result = baseMapper.deleteById(instanceId);

		// Update label usage count
		annotationLabelService.updateUsageCount(labelId);

		log.info("Annotation instance deleted successfully");
		return result > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean batchSaveAnnotations(Long annotationId, Long imageId, List<AnnotationInstance> annotations) {
		log.info("Batch save annotation instances: annotationId={}, imageName={}, count={}", annotationId, imageId, annotations.size());

		// Delete all existing annotations of the image first
		LambdaQueryWrapper<AnnotationInstance> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(AnnotationInstance::getAnnotationId, annotationId)
			.eq(AnnotationInstance::getImageId, imageId);

		List<AnnotationInstance> existingInstances = baseMapper.selectList(wrapper);
		if (!existingInstances.isEmpty()) {
			baseMapper.delete(wrapper);

			// Update label usage count of deleted annotations
			existingInstances.forEach(instance -> {
				annotationLabelService.updateUsageCount(instance.getLabelId());
			});
		}

		// Batch insert new annotations
		for (AnnotationInstance annotation : annotations) {
			save(annotation);

			// Update label usage count
			annotationLabelService.updateUsageCount(annotation.getLabelId());
		}

		log.info("Successfully batch saved annotation instances");
		return true;
	}

	@Override
	public List<AnnotationInstance> getByAnnotationId(Long annotationId) {
		log.debug("Query all annotation instances of the annotation project: annotationId={}", annotationId);
		return baseMapper.selectByAnnotationId(annotationId);
	}

	@Override
	public Integer countByLabelId(Long labelId) {
		log.debug("Count label usage: labelId={}", labelId);
		return baseMapper.countByLabelId(labelId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deleteImageAndRelatedData(Long annotationId, Long imageId) {
		log.info("Start deleting image and related data: annotationId={}, imageName={}", annotationId, imageId);

		try {
			// 1. Query all annotation instances of the image
			LambdaQueryWrapper<AnnotationInstance> instanceQuery = new LambdaQueryWrapper<>();
			instanceQuery.eq(AnnotationInstance::getAnnotationId, annotationId)
				.eq(AnnotationInstance::getImageId, imageId);
			List<AnnotationInstance> instances = baseMapper.selectList(instanceQuery);

			log.info("Found {} annotation instances that need to be deleted", instances.size());

			// 2. Count the usage times of each label for subsequent updates of label count
			Map<Long, Integer> labelCountMap = new HashMap<>();
			for (AnnotationInstance instance : instances) {
				Long labelId = instance.getLabelId();
				labelCountMap.put(labelId, labelCountMap.getOrDefault(labelId, 0) + 1);
			}

			// 3. Delete all annotation instances
			if (!instances.isEmpty()) {
				List<Long> instanceIds = instances.stream()
					.map(AnnotationInstance::getId)
					.collect(Collectors.toList());

				int deletedInstances = baseMapper.deleteBatchIds(instanceIds);
				log.info("Deleted {} annotation instances", deletedInstances);
			}

			// 4. Delete image record (annotation_image table)
			try {
				// Get all images of the annotation project
				List<AnnotationImage> allImages = annotationImageService.getImagesByAnnotationId(annotationId);

				// Found the image record to be deleted
				AnnotationImage imageToDelete = null;
				for (AnnotationImage image : allImages) {
					if (imageId.equals(image.getId())) {
						imageToDelete = image;
						break;
					}
				}

				if (imageToDelete != null) {
					annotationImageService.deleteImage(imageToDelete.getId());
					log.info("Delete image record: ID={}, imageId={}", imageToDelete.getId(), imageId);
				} else {
					log.warn("Image record to delete not found: imageId={}", imageId);
				}
			} catch (Exception e) {
				log.error("Failed to delete image record: imageId={}", imageId, e);
				// Do not throw exception, continue with subsequent logic
			}

			// 5. Update label usage count (annotation_label table)
			for (Map.Entry<Long, Integer> entry : labelCountMap.entrySet()) {
				Long labelId = entry.getKey();
				Integer count = entry.getValue();

				// Decrease usage count of the tag
				annotationLabelService.updateUsageCount(labelId);
				log.info("Update usage count of label {}", labelId);
			}

			log.info("Successfully deleted image and related data: imageId={}", imageId);
			return true;

		} catch (Exception e) {
			log.error("Failed to delete image and related data: imageId={}", imageId, e);
			throw new RuntimeException("Failed to delete image and related data:" + e.getMessage());
		}
	}

}
