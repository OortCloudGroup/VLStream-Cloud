package org.springblade.vlstream.pojo.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.AnalysisRequest;

import java.io.Serial;

/**
 * Intelligent Analysis Request Table View Entity Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AnalysisRequestVO extends AnalysisRequest {
	@Serial
	private static final long serialVersionUID = 1L;

	private String cameraName;

}
