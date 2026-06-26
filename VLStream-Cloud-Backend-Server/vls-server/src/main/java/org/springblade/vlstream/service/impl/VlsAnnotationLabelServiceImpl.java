package org.springblade.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.vlstream.excel.VlsAnnotationLabelExcel;
import org.springblade.vlstream.mapper.VlsAnnotationInstanceMapper;
import org.springblade.vlstream.mapper.VlsAnnotationLabelMapper;
import org.springblade.vlstream.pojo.entity.AnnotationLabel;
import org.springblade.vlstream.pojo.vo.AnnotationLabelVO;
import org.springblade.vlstream.service.IVlsAnnotationLabelService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Annotation Label Entity Class Service Implementation Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@Service
public class VlsAnnotationLabelServiceImpl extends BaseServiceImpl<VlsAnnotationLabelMapper, AnnotationLabel> implements IVlsAnnotationLabelService {

	@Resource
	private VlsAnnotationInstanceMapper annotationInstanceMapper;

	@Override
	public IPage<AnnotationLabelVO> selectVlsAnnotationLabelPage(IPage<AnnotationLabelVO> page, AnnotationLabelVO vlsAnnotationLabel) {
		return page.setRecords(baseMapper.selectVlsAnnotationLabelPage(page, vlsAnnotationLabel));
	}

	@Override
	public List<VlsAnnotationLabelExcel> exportVlsAnnotationLabel(Wrapper<AnnotationLabel> queryWrapper) {
		List<VlsAnnotationLabelExcel> vlsAnnotationLabelList = baseMapper.exportVlsAnnotationLabel(queryWrapper);
		//vlsAnnotationLabelList.forEach(vlsAnnotationLabel -> {
		//	vlsAnnotationLabel.setTypeName(DictCache.getValue(DictEnum.YES_NO, VlsAnnotationLabelEntity.getType()));
		//});
		return vlsAnnotationLabelList;
	}

	@Override
	public List<AnnotationLabel> getByAnnotationIdWithUsageCount(Long annotationId) {
		log.info("Query label list for annotation project [{}]", annotationId);
		return baseMapper.selectByAnnotationIdWithUsageCount(annotationId);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public AnnotationLabel createLabel(Long annotationId, String name, String color, String description) {
		log.info("Create tag: annotationId={}, name={}, color={}", annotationId, name, color);

		// Check if a tag with the same name exists under the same annotation project
		LambdaQueryWrapper<AnnotationLabel> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(AnnotationLabel::getAnnotationId, annotationId).eq(AnnotationLabel::getName, name);

		AnnotationLabel existingLabel = baseMapper.selectOne(wrapper);
		if (existingLabel != null) {
			throw new RuntimeException("Label name already exists");
		}

		// Get current maximum sort value
		LambdaQueryWrapper<AnnotationLabel> sortWrapper = new LambdaQueryWrapper<>();
		sortWrapper.eq(AnnotationLabel::getAnnotationId, annotationId).orderByDesc(AnnotationLabel::getSortOrder).last("LIMIT 1");

		AnnotationLabel lastLabel = baseMapper.selectOne(sortWrapper);
		int nextSortOrder = lastLabel != null ? lastLabel.getSortOrder() + 1 : 1;

		// Create new tag
		AnnotationLabel label = new AnnotationLabel();
		label.setAnnotationId(annotationId);
		label.setName(name);
		label.setColor(color);
		label.setDescription(description);
		label.setSortOrder(nextSortOrder);
		label.setUsageCount(0);

		baseMapper.insert(label);
		log.info("Label created successfully, ID: {}", label.getId());

		return label;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public AnnotationLabel updateLabel(Long labelId, String name, String color, String description) {
		log.info("Update label: labelId={}, name={}, color={}", labelId, name, color);

		AnnotationLabel label = baseMapper.selectById(labelId);
		if (label == null) {
			throw new RuntimeException("Label does not exist");
		}

		// Check if other tags with the same name exist under the same annotation project
		LambdaQueryWrapper<AnnotationLabel> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(AnnotationLabel::getAnnotationId, label.getAnnotationId()).eq(AnnotationLabel::getName, name).ne(AnnotationLabel::getId, labelId);

		AnnotationLabel existingLabel = baseMapper.selectOne(wrapper);
		if (existingLabel != null) {
			throw new RuntimeException("Label name already exists");
		}

		// Update label information
		label.setName(name);
		label.setColor(color);
		label.setDescription(description);

		updateById(label);
		log.info("Label updated successfully");

		return label;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deleteLabel(Long labelId) {
		log.info("Delete tag: labelId={}", labelId);

		AnnotationLabel label = baseMapper.selectById(labelId);
		if (label == null) {
			throw new RuntimeException("Label does not exist");
		}

		// Check if any annotation instances use this tag
		Integer usageCount = annotationInstanceMapper.countByLabelId(labelId);
		if (usageCount > 0) {
			throw new RuntimeException("The tag is in use and cannot be deleted");
		}

		// Execute soft delete
		int result = baseMapper.deleteById(labelId);
		log.info("Label deleted successfully");

		return result > 0;
	}

	@Override
	public boolean updateUsageCount(Long labelId) {
		log.debug("Update label usage count: labelId={}", labelId);

		Integer usageCount = annotationInstanceMapper.countByLabelId(labelId);
		int result = baseMapper.updateUsageCount(labelId, usageCount);

		return result > 0;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean updateSortOrder(Long annotationId, List<Long> labelIds) {
		log.info("Update label sorting: annotationId={}, labelIds={}", annotationId, labelIds);


		return true;
	}

	@Override
	public List<AnnotationLabel> searchLabels(Long annotationId, String keyword) {
		log.debug("Search labels: annotationId={}, keyword={}", annotationId, keyword);

		LambdaQueryWrapper<AnnotationLabel> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(AnnotationLabel::getAnnotationId, annotationId);

		if (StringUtils.hasText(keyword)) {
			wrapper.like(AnnotationLabel::getName, keyword);
		}

		wrapper.orderByAsc(AnnotationLabel::getSortOrder).orderByAsc(AnnotationLabel::getId);

		List<AnnotationLabel> labels = baseMapper.selectList(wrapper);

		// Update usage count
		labels.forEach(label -> {
			Integer usageCount = annotationInstanceMapper.countByLabelId(label.getId());
			label.setUsageCount(usageCount);
		});

		return labels;
	}

}
