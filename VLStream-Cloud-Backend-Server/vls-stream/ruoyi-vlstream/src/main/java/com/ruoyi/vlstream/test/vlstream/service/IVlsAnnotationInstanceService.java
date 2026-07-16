/*
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
 * 标注实例实体类 服务类
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsAnnotationInstanceService extends BaseService<AnnotationInstance> {
	/**
	 * 自定义分页
	 *
	 * @param page 分页参数
	 * @param vlsAnnotationInstance 查询参数
	 * @return IPage<VlsAnnotationInstanceVO>
	 */
	IPage<AnnotationInstanceVO> selectVlsAnnotationInstancePage(IPage<AnnotationInstanceVO> page, AnnotationInstanceVO vlsAnnotationInstance);

	/**
	 * 导出数据
	 *
	 * @param queryWrapper 查询条件
	 * @return List<VlsAnnotationInstanceExcel>
	 */
	List<VlsAnnotationInstanceExcel> exportVlsAnnotationInstance(Wrapper<AnnotationInstance> queryWrapper);

	/**
	 * 根据标注项目ID和图片名称查询标注实例
	 *
	 * @param annotationId 标注项目ID
	 * @param imageName 图片名称
	 * @return 标注实例列表
	 */
	List<AnnotationInstance> getByAnnotationIdAndImageName(Long annotationId, String imageName);

	/**
	 * 保存标注实例
	 *
	 * @param annotationId 标注项目ID
	 * @param labelId 标签ID
	 * @param imageId 图片id
	 * @param annotationType 标注类型
	 * @param annotationData 标注数据（JSON格式）
	 * @return 保存的标注实例
	 */
	AnnotationInstance saveAnnotation(Long annotationId, Long labelId, Long imageId, AlgorithmAnnotationTypeEnum annotationType, String annotationData);

	/**
	 * 更新标注实例
	 *
	 * @param instanceId 实例ID
	 * @param labelId 标签ID
	 * @param annotationType 标注类型
	 * @param annotationData 标注数据（JSON格式）
	 * @return 更新后的标注实例
	 */
	AnnotationInstance updateAnnotation(Long instanceId, Long labelId,
										AlgorithmAnnotationTypeEnum annotationType, String annotationData);

	/**
	 * 删除标注实例
	 *
	 * @param instanceId 实例ID
	 * @return 是否删除成功
	 */
	boolean deleteAnnotation(Long instanceId);

	/**
	 * 批量保存标注实例
	 *
	 * @param annotationId 标注项目ID
	 * @param imageId 图片ID
	 * @param annotations 标注实例列表
	 * @return 是否保存成功
	 */
	boolean batchSaveAnnotations(Long annotationId, Long imageId, List<AnnotationInstance> annotations);

	/**
	 * 根据标注项目ID查询所有标注实例
	 *
	 * @param annotationId 标注项目ID
	 * @return 标注实例列表
	 */
	List<AnnotationInstance> getByAnnotationId(Long annotationId);

	/**
	 * 根据标签ID统计使用次数
	 *
	 * @param labelId 标签ID
	 * @return 使用次数
	 */
	Integer countByLabelId(Long labelId);

	/**
	 * 删除图片及其相关的所有数据
	 * 包括：annotation_image、annotation_instance、更新annotation_label的使用计数
	 *
	 * @param annotationId 标注项目ID
	 * @param imageId 图片ID
	 * @return 删除结果
	 */
	boolean deleteImageAndRelatedData(Long annotationId, Long imageId);

}
