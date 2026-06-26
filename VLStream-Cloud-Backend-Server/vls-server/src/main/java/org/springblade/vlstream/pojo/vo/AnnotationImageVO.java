package org.springblade.vlstream.pojo.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.AnnotationImage;

import java.io.Serial;

/**
 * Annotation Image Info Table View Entity Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnnotationImageVO extends AnnotationImage {
	@Serial
	private static final long serialVersionUID = 1L;

}
