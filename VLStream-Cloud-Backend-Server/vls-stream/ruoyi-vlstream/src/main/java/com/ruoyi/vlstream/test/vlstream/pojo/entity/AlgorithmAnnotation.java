package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmAnnotationStatusEnum;


/**
 * 算法标注数据表 实体类
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
	 * 标注名称
	 */
	@Schema(description = "标注名称")
	private String annotationName;
	/**
	 * 标注类型：object_detection-物体检测,image_classification-图像分类,instance_segmentation-实例分割,semantic_segmentation-语义分割
	 */
	@Schema(description = "标注类型：object_detection-物体检测,image_classification-图像分类,instance_segmentation-实例分割,semantic_segmentation-语义分割")
	private String annotationType;
	/**
	 * 数据集路径
	 */
	@Schema(description = "数据集路径")
	private String datasetPath;
	/**
	 * 总数量
	 */
	@Schema(description = "总数量")
	private Integer totalCount;
	/**
	 * 已标注数量
	 */
	@Schema(description = "已标注数量")
	private Integer annotatedCount;
	/**
	 * 标注状态
	 */
	@Schema(description = "标注状态")
	private AlgorithmAnnotationStatusEnum annotationStatus;
	/**
	 * 标注进度百分比
	 */
	@Schema(description = "标注进度百分比")
	private Integer progress;
	/**
	 * 标注规则
	 */
	@Schema(description = "标注规则")
	private String annotationRules;
	/**
	 * 备注
	 */
	@Schema(description = "备注")
	private String remark;

}
