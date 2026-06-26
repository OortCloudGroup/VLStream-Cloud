package org.springblade.vlstream.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.Algorithm;

import java.io.Serial;

/**
 * Algorithm table view entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AlgorithmVO extends Algorithm {
	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Algorithm type name
	 */
	@Schema(description = "Classification name")
	private String categoryName;
}
