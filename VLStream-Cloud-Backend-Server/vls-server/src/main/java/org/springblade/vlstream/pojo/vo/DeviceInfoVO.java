package org.springblade.vlstream.pojo.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.DeviceInfo;

import java.io.Serial;

/**
 * Device Information Table View Entity Class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceInfoVO extends DeviceInfo {
	@Serial
	private static final long serialVersionUID = 1L;

	private String algorithmName;

}
