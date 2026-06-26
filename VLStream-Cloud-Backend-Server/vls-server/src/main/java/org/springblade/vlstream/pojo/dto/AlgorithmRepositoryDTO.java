package org.springblade.vlstream.pojo.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.AlgorithmRepository;

import java.io.Serial;

/**
 * Algorithm repository table Data Transfer Object entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AlgorithmRepositoryDTO extends AlgorithmRepository {
	@Serial
	private static final long serialVersionUID = 1L;

}
