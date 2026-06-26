package org.springblade.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;
import org.springblade.vlstream.enums.AlgorithmAnnotationStatusEnum;

import java.io.Serial;

/**
 * Algorithm annotation data table entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_algorithm_annotation")
@Schema(description = "VlsAlgorithmAnnotationEntity object")
@EqualsAndHashCode(callSuper = true)
public class AlgorithmAnnotation extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Annotation Name
	 */
	@Schema(description = "Annotation Name")
	private String annotationName;
	/**
	 * Annotation type: object_detection-object detection, image_classification-image classification, instance_segmentation-instance segmentation, semantic_segmentation-semantic segmentation
	 */
	@Schema(description = "Annotation type: object_detection-object detection, image_classification-image classification, instance_segmentation-instance segmentation, semantic_segmentation-semantic segmentation")
	private String annotationType;
	/**
	 * Dataset path
	 */
	@Schema(description = "Dataset path")
	private String datasetPath;
	/**
	 * Total quantity
	 */
	@Schema(description = "Total quantity")
	private Integer totalCount;
	/**
	 * Annotated quantity
	 */
	@Schema(description = "Annotated quantity")
	private Integer annotatedCount;
	/**
	 * Annotation Status
	 */
	@Schema(description = "Annotation Status")
	private AlgorithmAnnotationStatusEnum annotationStatus;
	/**
	 * Annotation Progress Percentage
	 */
	@Schema(description = "Annotation Progress Percentage")
	private Integer progress;
	/**
	 * Annotation Rule
	 */
	@Schema(description = "Annotation Rule")
	private String annotationRules;
	/**
	 * Remarks
	 */
	@Schema(description = "Remarks")
	private String remark;

}
