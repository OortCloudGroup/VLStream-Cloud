package org.springblade.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;
import org.springblade.vlstream.enums.AlgorithmAnnotationTypeEnum;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * Annotation Instance Entity Class Entity Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_annotation_instance")
@Schema(description = "VlsAnnotationInstanceEntity object")
@EqualsAndHashCode(callSuper = true)
public class AnnotationInstance extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Associated annotation project ID
	 */
	@Schema(description = "Associated annotation project ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long annotationId;
	/**
	 * Label ID
	 */
	@Schema(description = "Label ID")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long labelId;
	/**
	 * Image id
	 */
	@Schema(description = "Image id")
	@JsonSerialize(using = ToStringSerializer.class)
	private Long imageId;
	/**
	 * Annotation Type
	 */
	@Schema(description = "Annotation Type")
	private AlgorithmAnnotationTypeEnum annotationType;
	/**
	 * Annotation Coordinate Data (JSON format)
	 */
	@Schema(description = "Annotation Coordinate Data (JSON format)")
	private String annotationData;
	/**
	 * Confidence
	 */
	@Schema(description = "Confidence")
	private BigDecimal confidence;
	/**
	 * Whether verified
	 */
	@Schema(description = "Whether verified")
	private Integer verified;

}
