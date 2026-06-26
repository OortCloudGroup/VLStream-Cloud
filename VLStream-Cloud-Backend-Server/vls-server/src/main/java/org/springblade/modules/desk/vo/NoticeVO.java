package org.springblade.modules.desk.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.modules.desk.entity.Notice;

/**
 * Notice announcement view class
 *
 * @author Chill
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NoticeVO extends Notice {

	@Schema(description = "Notification type name")
	private String categoryName;

}
