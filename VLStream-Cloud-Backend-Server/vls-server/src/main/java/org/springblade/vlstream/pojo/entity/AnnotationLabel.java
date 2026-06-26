package org.springblade.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.io.Serial;

/**
 * Annotation Label Entity Class Entity Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_annotation_label")
@Schema(description = "VlsAnnotationLabelEntity object")
@EqualsAndHashCode(callSuper = true)
public class AnnotationLabel extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Associated annotation project ID
	 */
	@Schema(description = "Associated annotation project ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long annotationId;
	/**
	 * Label Name
	 */
	@Schema(description = "Label Name")
	private String name;
	/**
	 * Label Color (Hexadecimal)
	 */
	@Schema(description = "Label Color (Hexadecimal)")
	private String color;
	/**
	 * Label Description
	 */
	@Schema(description = "Label Description")
	private String description;
	/**
	 * Sort order
	 */
	@Schema(description = "Sort order")
	private Integer sortOrder;
	/**
	 * Usage frequency statistics
	 */
	@Schema(description = "Usage frequency statistics")
	private Integer usageCount;

}
