/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAnnotationInstanceExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationInstance;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AnnotationInstanceVO;

import java.util.List;

/**
 * annotationinstance Mapper interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsAnnotationInstanceMapper extends BaseMapper<AnnotationInstance> {

	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsAnnotationInstance Query parameter
	 * @return List<VlsAnnotationInstanceVO>
	 */
	List<AnnotationInstanceVO> selectVlsAnnotationInstancePage(IPage page, AnnotationInstanceVO vlsAnnotationInstance);

	/**
	 * Get Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsAnnotationInstanceExcel>
	 */
	List<VlsAnnotationInstanceExcel> exportVlsAnnotationInstance(@Param("ew") Wrapper<AnnotationInstance> queryWrapper);

	/**
	 * annotation item ID and Query annotationinstance
	 *
	 * @param annotationId annotation item ID
	 * @param imageId id
	 * @return annotationinstance
	 */
	@Select("SELECT * FROM vls_annotation_instance " +
		"WHERE annotation_id = #{annotationId} AND image_id = #{imageId} AND is_deleted = 0")
	List<AnnotationInstance> selectByAnnotationIdAndImageName(@Param("annotationId") Long annotationId,
															  @Param("imageId") String imageId);

	/**
	 * ID
	 *
	 * @param labelId ID
	 * @return
	 */
	@Select("SELECT COUNT(*) FROM vls_annotation_instance " +
		"WHERE label_id = #{labelId} AND is_deleted = 0")
	Integer countByLabelId(@Param("labelId") Long labelId);

	/**
	 * Count images that currently contain at least one active annotation.
	 *
	 * @param annotationId annotation project ID
	 * @return number of distinct annotated images
	 */
	@Select("SELECT COUNT(DISTINCT image_id) FROM vls_annotation_instance " +
		"WHERE annotation_id = #{annotationId} AND is_deleted = 0")
	Integer countDistinctAnnotatedImages(@Param("annotationId") Long annotationId);

	/**
	 * annotation item IDQuery all annotationinstance
	 *
	 * @param annotationId annotation item ID
	 * @return annotationinstance
	 */
	@Select("SELECT * FROM vls_annotation_instance " +
		"WHERE annotation_id = #{annotationId} AND is_deleted = 0 " +
		"ORDER BY image_id, create_time")
	List<AnnotationInstance> selectByAnnotationId(@Param("annotationId") Long annotationId);

	/**
	 * annotation item ID and IDQuery annotationinstance
	 *
	 * @param annotationId annotation item ID
	 * @param labelId ID
	 * @return annotationinstance
	 */
	@Select("SELECT * FROM vls_annotation_instance " +
		"WHERE annotation_id = #{annotationId} AND label_id = #{labelId} AND is_deleted = 0")
	List<AnnotationInstance> selectByAnnotationIdAndLabelId(@Param("annotationId") Long annotationId,
															@Param("labelId") Long labelId);

}
