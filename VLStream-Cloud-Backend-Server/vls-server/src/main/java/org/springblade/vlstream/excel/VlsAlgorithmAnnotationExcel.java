package org.springblade.vlstream.excel;


import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * Algorithm annotation data table Excel entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@ColumnWidth(25)
@HeadRowHeight(20)
@ContentRowHeight(18)
public class VlsAlgorithmAnnotationExcel implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Annotation Name
	 */
	@ColumnWidth(20)
	@ExcelProperty("Annotation Name")
	private String annotationName;
	/**
	 * Annotation type: object_detection-object detection, image_classification-image classification, instance_segmentation-instance segmentation, semantic_segmentation-semantic segmentation
	 */
	@ColumnWidth(20)
	@ExcelProperty("Annotation type: object_detection-object detection, image_classification-image classification, instance_segmentation-instance segmentation, semantic_segmentation-semantic segmentation")
	private String annotationType;
	/**
	 * Dataset path
	 */
	@ColumnWidth(20)
	@ExcelProperty("Dataset path")
	private String datasetPath;
	/**
	 * Total quantity
	 */
	@ColumnWidth(20)
	@ExcelProperty("Total quantity")
	private Integer totalCount;
	/**
	 * Annotated quantity
	 */
	@ColumnWidth(20)
	@ExcelProperty("Annotated quantity")
	private Integer annotatedCount;
	/**
	 * Annotation status: none-unannotated, partial-partially annotated, completed-completed
	 */
	@ColumnWidth(20)
	@ExcelProperty("Annotation status: none-unannotated, partial-partially annotated, completed-completed")
	private String annotationStatus;
	/**
	 * Annotation Progress Percentage
	 */
	@ColumnWidth(20)
	@ExcelProperty("Annotation Progress Percentage")
	private Integer progress;
	/**
	 * Annotation Rule
	 */
	@ColumnWidth(20)
	@ExcelProperty("Annotation Rule")
	private String annotationRules;
	/**
	 * Remarks
	 */
	@ColumnWidth(20)
	@ExcelProperty("Remarks")
	private String remark;

}
