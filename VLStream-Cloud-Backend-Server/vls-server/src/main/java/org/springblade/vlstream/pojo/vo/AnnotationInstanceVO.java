package org.springblade.vlstream.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.AnnotationInstance;

import java.io.Serial;

/**
 * Annotation Instance Entity Class View Entity Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnnotationInstanceVO extends AnnotationInstance {
	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Image name")
	private String imageName;

}
