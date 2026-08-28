/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAnnotationImageExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationImage;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AnnotationImageVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * annotation info service
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsAnnotationImageService extends BaseService<AnnotationImage> {
	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsAnnotationImage Query parameter
	 * @return IPage<VlsAnnotationImageVO>
	 */
	IPage<AnnotationImageVO> selectVlsAnnotationImagePage(IPage<AnnotationImageVO> page, AnnotationImageVO vlsAnnotationImage);

	/**
	 * Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsAnnotationImageExcel>
	 */
	List<VlsAnnotationImageExcel> exportVlsAnnotationImage(Wrapper<AnnotationImage> queryWrapper);

	/**
	 *
	 */
	List<AnnotationImage> uploadImages(MultipartFile[] files, Long annotationId);

	/**
	 * datasetIDGet
	 */
	List<AnnotationImage> getImagesByDataset(Long annotationId);

	/**
	 * IDGet
	 */
	AnnotationImage getImageById(Long id);

	/**
	 * new info
	 */
	AnnotationImage updateImage(AnnotationImage image);

	/**
	 * Delete
	 */
	void deleteImage(Long id);

	/**
	 * Batch delete
	 */
	void batchDeleteImages(List<Long> ids);

	/**
	 * Get dataset info
	 */
	Map<String, Object> getDatasetStats(Long datasetId);

	/**
	 * info annotation_image
	 */
	boolean saveImage(AnnotationImage annotationImage);

	/**
	 * info annotation_image
	 */
	boolean batchSaveImages(List<AnnotationImage> annotationImages);

	/**
	 * annotation item IDGet
	 */
	List<AnnotationImage> getImagesByAnnotationId(Long annotationId);

}
