package org.springblade.vlstream.pojo.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.AnnotationInstance;

import java.io.Serial;

/**
 * Annotation Instance Entity Class Data Transfer Object Entity Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnnotationInstanceDTO extends AnnotationInstance {
	@Serial
	private static final long serialVersionUID = 1L;

}
