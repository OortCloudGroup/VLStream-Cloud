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
import org.apache.ibatis.annotations.*;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAnnotationImageExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationImage;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AnnotationImageVO;

import java.util.List;

/**
 * annotation info Mapper interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsAnnotationImageMapper extends BaseMapper<AnnotationImage> {

	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsAnnotationImage Query parameter
	 * @return List<VlsAnnotationImageVO>
	 */
	List<AnnotationImageVO> selectVlsAnnotationImagePage(IPage page, AnnotationImageVO vlsAnnotationImage);

	/**
	 * Get Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsAnnotationImageExcel>
	 */
	List<VlsAnnotationImageExcel> exportVlsAnnotationImage(@Param("ew") Wrapper<AnnotationImage> queryWrapper);

	/**
	 * : data field Javaproperty
	 */
	@Results(id = "AnnotationImageResultMap", value = {
		@Result(property = "id", column = "id"),
		@Result(property = "annotationId", column = "annotation_id"),
		@Result(property = "imageName", column = "image_name"),
		@Result(property = "originalName", column = "original_name"),
		@Result(property = "localPath", column = "local_path"),
		@Result(property = "fileSize", column = "file_size"),
		@Result(property = "lastModified", column = "last_modified"),
		@Result(property = "isImported", column = "is_imported"),
		@Result(property = "importTime", column = "import_time"),
		@Result(property = "createTime", column = "create_time"),
		@Result(property = "updateTime", column = "update_time"),
		@Result(property = "isDeleted", column = "is_deleted")
	})
	@Select("SELECT * FROM vls_annotation_image WHERE id = #{id} AND is_deleted = 0")
	AnnotationImage selectById(Long id);

	/**
	 * record
	 */
	// Use BaseMapper.insert so trusted tenant and audit fields are filled for legacy uploads too.

	/**
	 * datasetIDQuery list ( old interface, annotation_id)
	 */
	@ResultMap("AnnotationImageResultMap")
	@Select("SELECT * FROM vls_annotation_image WHERE annotation_id = #{annotationId} AND is_deleted = 0 AND media_type = 'image' AND quality_status != 'excluded' ORDER BY create_time DESC")
	List<AnnotationImage> selectByDatasetId(Long annotationId);

	/**
	 * annotation item IDQuery list
	 */
	@ResultMap("AnnotationImageResultMap")
	@Select("SELECT * FROM vls_annotation_image WHERE annotation_id = #{annotationId} AND is_deleted = 0 AND media_type = 'image' AND quality_status != 'excluded' ORDER BY create_time DESC")
	List<AnnotationImage> selectByAnnotationId(@Param("annotationId") Long annotationId);

	/**
	 * new info
	 */
	// Generated updateById uses the actual entity columns and preserves null metadata.

	/**
	 * Delete record
	 */
	@Update("UPDATE vls_annotation_image SET is_deleted = 1 WHERE id = #{id}")
	int deleteById(Long id);

	/**
	 * datasetIDDelete all ( old interface, annotation_id)
	 */
	@Update("UPDATE vls_annotation_image SET is_deleted = 1 WHERE annotation_id = #{datasetId}")
	int deleteByDatasetId(Long datasetId);

	/**
	 * dataset ( old interface, annotation_id)
	 */
	@Select("SELECT COUNT(*) FROM vls_annotation_image WHERE annotation_id = #{datasetId}")
	int countByDatasetId(Long datasetId);

	/**
	 * Count active images in an annotation project.
	 */
	@Select("SELECT COUNT(*) FROM vls_annotation_image " +
		"WHERE annotation_id = #{annotationId} AND is_deleted = 0 AND media_type = 'image' AND quality_status != 'excluded'")
	int countActiveImages(@Param("annotationId") Long annotationId);

	/**
	 * ( old interface, annotation_id)
	 */
	@Select("SELECT COUNT(*) FROM vls_annotation_image WHERE annotation_id = #{datasetId} AND status = #{status}")
	int countByDatasetIdAndStatus(@Param("datasetId") Long datasetId, @Param("status") String status);

}
