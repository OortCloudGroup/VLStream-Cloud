package org.springblade.vlstream.pojo.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.DeviceTagRelation;

import java.io.Serial;

/**
 * Device Tag Association Table View Entity Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceTagRelationVO extends DeviceTagRelation {
	@Serial
	private static final long serialVersionUID = 1L;

}
