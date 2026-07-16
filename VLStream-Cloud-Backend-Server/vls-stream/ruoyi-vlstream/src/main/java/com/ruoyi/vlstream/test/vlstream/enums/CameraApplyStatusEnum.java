package com.ruoyi.vlstream.test.vlstream.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 摄像头申请审批状态
 */
@Getter
public enum CameraApplyStatusEnum {

	pending("pending", "待审批"),
	approved("approved", "已通过"),
	rejected("rejected", "已驳回"),
	completed("completed", "已完成");

	@EnumValue
	private final String code;
	private final String description;

	CameraApplyStatusEnum(String code, String description) {
		this.code = code;
		this.description = description;
	}
}
