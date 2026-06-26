package org.springblade.vlstream.pojo.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.TagManagement;

import java.io.Serial;

/**
 * Label Management Table View Entity Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TagManagementVO extends TagManagement {
	@Serial
	private static final long serialVersionUID = 1L;

}
