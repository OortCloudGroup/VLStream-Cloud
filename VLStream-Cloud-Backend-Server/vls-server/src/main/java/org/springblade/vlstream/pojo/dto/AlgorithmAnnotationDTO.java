package org.springblade.vlstream.pojo.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.AlgorithmAnnotation;

import java.io.Serial;

/**
 * Algorithm annotation data table data transfer object entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AlgorithmAnnotationDTO extends AlgorithmAnnotation {
	@Serial
	private static final long serialVersionUID = 1L;

}
