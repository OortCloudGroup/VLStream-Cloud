package org.springblade.vlstream.pojo.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.AudioLinkageModeSetting;

import java.io.Serial;

/**
 * Audio linkage mode settings table view entity class
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AudioLinkageModeSettingVO extends AudioLinkageModeSetting {
	@Serial
	private static final long serialVersionUID = 1L;
}
