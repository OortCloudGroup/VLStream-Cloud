package org.springblade.vlstream.pojo.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.AudioAnomalyDetectionSetting;

import java.io.Serial;

/**
 * Audio exception detection settings table view entity class
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AudioAnomalyDetectionSettingVO extends AudioAnomalyDetectionSetting {
	@Serial
	private static final long serialVersionUID = 1L;
}
