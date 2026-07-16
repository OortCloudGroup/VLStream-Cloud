package com.ruoyi.vlstream.test.vlstream.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceTagRelation;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TagManagement;


/**
 * 设备标签关联表 数据传输对象实体类
 *
 * @author Oort
 * @since 2025-12-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeviceTagRelationDTO extends DeviceTagRelation {
	private static final long serialVersionUID = 1L;

	// 以下为关联对象，不对应数据库字段
	@Schema(description = "设备信息", hidden = true)
	private DeviceInfo deviceInfo;

	@Schema(description = "标签信息", hidden = true)
	private TagManagement tagInfo;

	@Schema(description = "标签名称", hidden = true)
	private String tagName;

	@Schema(description = "标签类型", hidden = true)
	private String categoryType;

	@Schema(description = "标签颜色", hidden = true)
	private String tagColor;

}
