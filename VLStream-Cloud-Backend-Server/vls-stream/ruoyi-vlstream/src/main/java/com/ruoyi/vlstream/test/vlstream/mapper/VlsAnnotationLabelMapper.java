/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAnnotationLabelExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationLabel;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AnnotationLabelVO;

import java.util.List;

/**
 * annotation Mapper interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsAnnotationLabelMapper extends BaseMapper<AnnotationLabel> {

	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsAnnotationLabel Query parameter
	 * @return List<VlsAnnotationLabelVO>
	 */
	List<AnnotationLabelVO> selectVlsAnnotationLabelPage(IPage page, AnnotationLabelVO vlsAnnotationLabel);

	/**
	 * Get Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsAnnotationLabelExcel>
	 */
	List<VlsAnnotationLabelExcel> exportVlsAnnotationLabel(@Param("ew") Wrapper<AnnotationLabel> queryWrapper);

	/**
	 * annotation item IDQuery list ( )
	 *
	 * @param annotationId annotation item ID
	 * @return
	 */
	@Select("SELECT al.*, " +
		"COALESCE((SELECT COUNT(*) FROM vls_annotation_instance ai " +
		"WHERE ai.label_id = al.id AND ai.is_deleted = 0), 0) as usage_count " +
		"FROM vls_annotation_label al " +
		"WHERE al.annotation_id = #{annotationId} AND al.is_deleted = 0 " +
		"ORDER BY al.sort_order ASC, al.id ASC")
	List<AnnotationLabel> selectByAnnotationIdWithUsageCount(@Param("annotationId") Long annotationId);

	/**
	 * new
	 *
	 * @param labelId ID
	 * @param usageCount
	 * @return new
	 */
	@Update("UPDATE vls_annotation_label SET usage_count = #{usageCount} WHERE id = #{labelId}")
	int updateUsageCount(@Param("labelId") Long labelId, @Param("usageCount") Integer usageCount);

}
