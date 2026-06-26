package org.springblade.vlstream.pojo.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.AlgorithmRepository;

import java.io.Serial;

/**
 * Algorithm repository table View entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AlgorithmRepositoryVO extends AlgorithmRepository {
	@Serial
	private static final long serialVersionUID = 1L;

}
