package org.springblade.vlstream.pojo.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.AudioDefenseTimeSetting;

import java.io.Serial;

/**
 * Audio arming schedule setting table view entity class
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AudioDefenseTimeSettingVO extends AudioDefenseTimeSetting {
	@Serial
	private static final long serialVersionUID = 1L;
}
