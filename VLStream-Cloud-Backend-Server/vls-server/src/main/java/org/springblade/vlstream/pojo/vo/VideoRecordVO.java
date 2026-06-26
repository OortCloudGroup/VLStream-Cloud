package org.springblade.vlstream.pojo.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.vlstream.pojo.entity.VideoRecord;

import java.io.Serial;

/**
 * Video Recording Record Table View Entity Class
 *
 * @author Oort
 * @since 2025-12-25
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VideoRecordVO extends VideoRecord {
	@Serial
	private static final long serialVersionUID = 1L;

}
