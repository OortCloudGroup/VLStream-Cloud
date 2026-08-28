/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmAnnotationTypeEnum;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAnnotationInstanceExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationInstance;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AnnotationInstanceVO;

import java.util.List;

/**
 * annotationinstance service
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsAnnotationInstanceService extends BaseService<AnnotationInstance> {
	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsAnnotationInstance Query parameter
	 * @return IPage<VlsAnnotationInstanceVO>
	 */
	IPage<AnnotationInstanceVO> selectVlsAnnotationInstancePage(IPage<AnnotationInstanceVO> page, AnnotationInstanceVO vlsAnnotationInstance);

	/**
	 * Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsAnnotationInstanceExcel>
	 */
	List<VlsAnnotationInstanceExcel> exportVlsAnnotationInstance(Wrapper<AnnotationInstance> queryWrapper);

	/**
	 * annotation item ID and Query annotationinstance
	 *
	 * @param annotationId annotation item ID
	 * @param imageName
	 * @return annotationinstance
	 */
	List<AnnotationInstance> getByAnnotationIdAndImageName(Long annotationId, String imageName);

	/**
	 * annotationinstance
	 *
	 * @param annotationId annotation item ID
	 * @param labelId ID
	 * @param imageId id
	 * @param annotationType annotation
	 * @param annotationData annotationdata (JSON )
	 * @return annotationinstance
	 */
	AnnotationInstance saveAnnotation(Long annotationId, Long labelId, Long imageId, AlgorithmAnnotationTypeEnum annotationType, String annotationData);

	/**
	 * new annotationinstance
	 *
	 * @param instanceId instanceID
	 * @param labelId ID
	 * @param annotationType annotation
	 * @param annotationData annotationdata (JSON )
	 * @return new after annotationinstance
	 */
	AnnotationInstance updateAnnotation(Long instanceId, Long labelId,
										AlgorithmAnnotationTypeEnum annotationType, String annotationData);

	/**
	 * Delete annotationinstance
	 *
	 * @param instanceId instanceID
	 * @return whether Delete successfully
	 */
	boolean deleteAnnotation(Long instanceId);

	/**
	 * annotationinstance
	 *
	 * @param annotationId annotation item ID
	 * @param imageId ID
	 * @param annotations annotationinstance
	 * @return whether successfully
	 */
	boolean batchSaveAnnotations(Long annotationId, Long imageId, List<AnnotationInstance> annotations);

	/**
	 * annotation item IDQuery all annotationinstance
	 *
	 * @param annotationId annotation item ID
	 * @return annotationinstance
	 */
	List<AnnotationInstance> getByAnnotationId(Long annotationId);

	/**
	 * ID
	 *
	 * @param labelId ID
	 * @return
	 */
	Integer countByLabelId(Long labelId);

	/**
	 * Delete related all data
	 * : annotation_image、annotation_instance、 new annotation_label
	 *
	 * @param annotationId annotation item ID
	 * @param imageId ID
	 * @return Delete
	 */
	boolean deleteImageAndRelatedData(Long annotationId, Long imageId);

}
