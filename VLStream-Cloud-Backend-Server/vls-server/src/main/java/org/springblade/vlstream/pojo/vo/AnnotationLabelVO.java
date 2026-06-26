package org.springblade.vlstream.pojo.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.AnnotationLabel;

import java.io.Serial;

/**
 * Annotation Label Entity Class View Entity Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnnotationLabelVO extends AnnotationLabel {
	@Serial
	private static final long serialVersionUID = 1L;

}
