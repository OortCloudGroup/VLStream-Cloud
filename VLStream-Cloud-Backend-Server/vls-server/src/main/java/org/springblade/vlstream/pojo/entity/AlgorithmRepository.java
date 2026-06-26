package org.springblade.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;
import org.springblade.vlstream.enums.AlgorithmRepositoryTypeEnum;

import java.io.Serial;

/**
 * Algorithm repository table Entity class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@TableName("vls_algorithm_repository")
@Schema(description = "VlsAlgorithmRepositoryEntity object")
@EqualsAndHashCode(callSuper = true)
public class AlgorithmRepository extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * Algorithm repository name
	 */
	@Schema(description = "Algorithm repository name")
	private String name;
	/**
	 * Number of algorithms owned
	 */
	@Schema(description = "Number of algorithms owned")
	private Integer algorithmCount;
	/**
	 * Repository type
	 */
	@Schema(description = "Repository type")
	private AlgorithmRepositoryTypeEnum repositoryType;
	/**
	 * Remarks
	 */
	@Schema(description = "Remarks")
	private String remark;

}
