package org.springblade.vlstream.pojo.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.AlgorithmOrchestration;

import java.io.Serial;

/**
 * Algorithm orchestration table data transfer object entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AlgorithmOrchestrationDTO extends AlgorithmOrchestration {
	@Serial
	private static final long serialVersionUID = 1L;

}
