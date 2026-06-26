package org.springblade.vlstream.pojo.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.AlgorithmOrchestration;

import java.io.Serial;

/**
 * Algorithm orchestration table view entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AlgorithmOrchestrationVO extends AlgorithmOrchestration {
	@Serial
	private static final long serialVersionUID = 1L;

}
