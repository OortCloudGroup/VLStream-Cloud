/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
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
 * 标注实例实体类 Mapper 接口
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsAnnotationInstanceMapper extends BaseMapper<AnnotationInstance> {

	/**
	 * 自定义分页
	 *
	 * @param page 分页参数
	 * @param vlsAnnotationInstance 查询参数
	 * @return List<VlsAnnotationInstanceVO>
	 */
	List<AnnotationInstanceVO> selectVlsAnnotationInstancePage(IPage page, AnnotationInstanceVO vlsAnnotationInstance);

	/**
	 * 获取导出数据
	 *
	 * @param queryWrapper 查询条件
	 * @return List<VlsAnnotationInstanceExcel>
	 */
	List<VlsAnnotationInstanceExcel> exportVlsAnnotationInstance(@Param("ew") Wrapper<AnnotationInstance> queryWrapper);

	/**
	 * 根据标注项目ID和图片名称查询标注实例
	 *
	 * @param annotationId 标注项目ID
	 * @param imageId 图片id
	 * @return 标注实例列表
	 */
	@Select("SELECT * FROM vls_annotation_instance " +
		"WHERE annotation_id = #{annotationId} AND image_id = #{imageId} AND is_deleted = 0")
	List<AnnotationInstance> selectByAnnotationIdAndImageName(@Param("annotationId") Long annotationId,
															  @Param("imageId") String imageId);

	/**
	 * 根据标签ID统计使用次数
	 *
	 * @param labelId 标签ID
	 * @return 使用次数
	 */
	@Select("SELECT COUNT(*) FROM vls_annotation_instance " +
		"WHERE label_id = #{labelId} AND is_deleted = 0")
	Integer countByLabelId(@Param("labelId") Long labelId);

	/**
	 * 根据标注项目ID查询所有标注实例
	 *
	 * @param annotationId 标注项目ID
	 * @return 标注实例列表
	 */
	@Select("SELECT * FROM vls_annotation_instance " +
		"WHERE annotation_id = #{annotationId} AND is_deleted = 0 " +
		"ORDER BY image_id, create_time")
	List<AnnotationInstance> selectByAnnotationId(@Param("annotationId") Long annotationId);

	/**
	 * 根据标注项目ID和标签ID查询标注实例
	 *
	 * @param annotationId 标注项目ID
	 * @param labelId 标签ID
	 * @return 标注实例列表
	 */
	@Select("SELECT * FROM vls_annotation_instance " +
		"WHERE annotation_id = #{annotationId} AND label_id = #{labelId} AND is_deleted = 0")
	List<AnnotationInstance> selectByAnnotationIdAndLabelId(@Param("annotationId") Long annotationId,
															@Param("labelId") Long labelId);

}
