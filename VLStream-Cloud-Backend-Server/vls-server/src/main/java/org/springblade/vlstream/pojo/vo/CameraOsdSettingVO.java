package org.springblade.vlstream.pojo.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.CameraOsdSetting;

import java.io.Serial;

/**
 * Camera OSD settings table view entity class
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CameraOsdSettingVO extends CameraOsdSetting {
	@Serial
	private static final long serialVersionUID = 1L;
}
