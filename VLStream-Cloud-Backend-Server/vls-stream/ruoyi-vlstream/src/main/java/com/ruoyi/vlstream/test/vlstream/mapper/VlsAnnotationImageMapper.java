/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
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
 * 标注图片信息表 Mapper 接口
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsAnnotationImageMapper extends BaseMapper<AnnotationImage> {

	/**
	 * 自定义分页
	 *
	 * @param page 分页参数
	 * @param vlsAnnotationImage 查询参数
	 * @return List<VlsAnnotationImageVO>
	 */
	List<AnnotationImageVO> selectVlsAnnotationImagePage(IPage page, AnnotationImageVO vlsAnnotationImage);

	/**
	 * 获取导出数据
	 *
	 * @param queryWrapper 查询条件
	 * @return List<VlsAnnotationImageExcel>
	 */
	List<VlsAnnotationImageExcel> exportVlsAnnotationImage(@Param("ew") Wrapper<AnnotationImage> queryWrapper);

	/**
	 * 结果映射：数据库字段到Java属性的映射
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
	@Select("SELECT * FROM vls_annotation_image WHERE id = #{id}")
	AnnotationImage selectById(Long id);

	/**
	 * 插入图片记录
	 */
	@Insert("INSERT INTO vls_annotation_image (annotation_id, image_name, original_name, local_path, " +
		"file_size, is_imported, import_time, create_time, update_time) " +
		"VALUES (#{annotationId}, #{imageName}, #{originalName}, #{localPath}, " +
		"#{fileSize}, 1, NOW(), NOW(), NOW())")
	@Options(useGeneratedKeys = true, keyProperty = "id")
	int insert(AnnotationImage image);

	/**
	 * 根据数据集ID查询图片列表 (兼容旧接口，实际使用annotation_id)
	 */
	@ResultMap("AnnotationImageResultMap")
	@Select("SELECT * FROM vls_annotation_image WHERE annotation_id = #{annotationId} ORDER BY create_time DESC")
	List<AnnotationImage> selectByDatasetId(Long annotationId);

	/**
	 * 根据标注项目ID查询图片列表
	 */
	@ResultMap("AnnotationImageResultMap")
	@Select("SELECT * FROM vls_annotation_image WHERE annotation_id = #{annotationId} AND is_deleted = 0 ORDER BY create_time DESC")
	List<AnnotationImage> selectByAnnotationId(@Param("annotationId") Long annotationId);

	/**
	 * 更新图片信息
	 */
	@Update("UPDATE vls_annotation_image SET " +
		"image_name = #{imageName}, original_name = #{originalName}, local_path = #{localPath}, " +
		"file_url = #{fileUrl}, file_size = #{fileSize}, mime_type = #{mimeType}, " +
		"width = #{width}, height = #{height}, category = #{category}, " +
		"annotation_data = #{annotationData}, status = #{status}, tags = #{tags}, " +
		"update_time = #{updateTime}, update_by = #{updateBy} " +
		"WHERE id = #{id}")
	int updateById(AnnotationImage image);

	/**
	 * 删除图片记录
	 */
	@Delete("DELETE FROM vls_annotation_image WHERE id = #{id}")
	int deleteById(Long id);

	/**
	 * 根据数据集ID删除所有图片 (兼容旧接口，实际使用annotation_id)
	 */
	@Delete("DELETE FROM vls_annotation_image WHERE annotation_id = #{datasetId}")
	int deleteByDatasetId(Long datasetId);

	/**
	 * 统计数据集图片数量 (兼容旧接口，实际使用annotation_id)
	 */
	@Select("SELECT COUNT(*) FROM vls_annotation_image WHERE annotation_id = #{datasetId}")
	int countByDatasetId(Long datasetId);

	/**
	 * 根据状态统计图片数量 (兼容旧接口，实际使用annotation_id)
	 */
	@Select("SELECT COUNT(*) FROM vls_annotation_image WHERE annotation_id = #{datasetId} AND status = #{status}")
	int countByDatasetIdAndStatus(@Param("datasetId") Long datasetId, @Param("status") String status);

}
