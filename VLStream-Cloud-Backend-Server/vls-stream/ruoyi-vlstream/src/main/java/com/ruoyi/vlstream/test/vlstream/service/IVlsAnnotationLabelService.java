/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAnnotationLabelExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationLabel;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AnnotationLabelVO;

import java.util.List;

/**
 * annotation service
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsAnnotationLabelService extends BaseService<AnnotationLabel> {
	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsAnnotationLabel Query parameter
	 * @return IPage<VlsAnnotationLabelVO>
	 */
	IPage<AnnotationLabelVO> selectVlsAnnotationLabelPage(IPage<AnnotationLabelVO> page, AnnotationLabelVO vlsAnnotationLabel);

	/**
	 * Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsAnnotationLabelExcel>
	 */
	List<VlsAnnotationLabelExcel> exportVlsAnnotationLabel(Wrapper<AnnotationLabel> queryWrapper);

	/**
	 * annotation item IDQuery list ( )
	 *
	 * @param annotationId annotation item ID
	 * @return
	 */
	List<AnnotationLabel> getByAnnotationIdWithUsageCount(Long annotationId);

	/**
	 *
	 *
	 * @param annotationId annotation item ID
	 * @param name
	 * @param color
	 * @param description
	 * @return
	 */
	AnnotationLabel createLabel(Long annotationId, String name, String color, String description);

	/**
	 * new
	 *
	 * @param labelId ID
	 * @param name
	 * @param color
	 * @param description
	 * @return new after
	 */
	AnnotationLabel updateLabel(Long labelId, String name, String color, String description);

	/**
	 * Delete
	 *
	 * @param labelId ID
	 * @return whether Delete successfully
	 */
	boolean deleteLabel(Long labelId);

	/**
	 * new
	 *
	 * @param labelId ID
	 * @return whether new successfully
	 */
	boolean updateUsageCount(Long labelId);

	/**
	 * new
	 *
	 * @param annotationId annotation item ID
	 * @param labelIds ID ( )
	 * @return whether new successfully
	 */
	boolean updateSortOrder(Long annotationId, List<Long> labelIds);

	/**
	 *
	 *
	 * @param annotationId annotation item ID
	 * @param keyword
	 * @return
	 */
	List<AnnotationLabel> searchLabels(Long annotationId, String keyword);

}
