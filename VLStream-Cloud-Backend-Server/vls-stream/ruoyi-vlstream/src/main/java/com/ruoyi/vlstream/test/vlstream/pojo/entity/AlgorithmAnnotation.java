/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmAnnotationStatusEnum;


/**
 * algorithmannotationdata
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_algorithm_annotation")
@Schema(description = "VlsAlgorithmAnnotationEntity对象")
@EqualsAndHashCode(callSuper = true)
public class AlgorithmAnnotation extends TenantEntity {
	private static final long serialVersionUID = 1L;

	/**
	 * annotation
	 */
	@Schema(description = "标注名称")
	private String annotationName;
	/**
	 * annotation : object_detection- ,image_classification- ,instance_segmentation-instance ,semantic_segmentation-
	 */
	@Schema(description = "标注类型：object_detection-物体检测,image_classification-图像分类,instance_segmentation-实例分割,semantic_segmentation-语义分割")
	private String annotationType;
	/**
	 * dataset
	 */
	@Schema(description = "数据集路径")
	private String datasetPath;
	/**
	 *
	 */
	@Schema(description = "总数量")
	private Integer totalCount;
	/**
	 * already annotation
	 */
	@Schema(description = "已标注数量")
	private Integer annotatedCount;
	/**
	 * annotation
	 */
	@Schema(description = "标注状态")
	private AlgorithmAnnotationStatusEnum annotationStatus;
	/**
	 * annotation
	 */
	@Schema(description = "标注进度百分比")
	private Integer progress;
	/**
	 * annotation
	 */
	@Schema(description = "标注规则")
	private String annotationRules;
	/**
	 * remark
	 */
	@Schema(description = "备注")
	private String remark;

	private String projectCode;
	private String projectType;

}
