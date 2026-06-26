package org.springblade.vlstream.pojo.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.CameraDisplaySetting;

import java.io.Serial;

/**
 * Camera display settings table view entity class
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CameraDisplaySettingVO extends CameraDisplaySetting {
	@Serial
	private static final long serialVersionUID = 1L;
}
