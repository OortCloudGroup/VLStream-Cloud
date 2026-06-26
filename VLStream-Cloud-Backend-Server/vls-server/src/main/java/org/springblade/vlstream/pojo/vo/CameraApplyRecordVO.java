package org.springblade.vlstream.pojo.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.CameraApplyRecord;

import java.io.Serial;

/**
 * Camera application approval record view
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CameraApplyRecordVO extends CameraApplyRecord {

	@Serial
	private static final long serialVersionUID = 1L;

	private String deviceName;
}
