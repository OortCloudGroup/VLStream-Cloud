package org.springblade.vlstream.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.AlgorithmModel;

import java.io.Serial;

/**
 * Algorithm model table view entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AlgorithmModelVO extends AlgorithmModel {
	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Model download path")
	private String modelDownloadPath;

}
