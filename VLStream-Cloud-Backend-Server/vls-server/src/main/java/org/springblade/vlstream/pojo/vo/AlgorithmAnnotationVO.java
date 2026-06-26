package org.springblade.vlstream.pojo.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.AlgorithmAnnotation;

import java.io.Serial;

/**
 * Algorithm annotation data table view entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AlgorithmAnnotationVO extends AlgorithmAnnotation {
	@Serial
	private static final long serialVersionUID = 1L;

}
