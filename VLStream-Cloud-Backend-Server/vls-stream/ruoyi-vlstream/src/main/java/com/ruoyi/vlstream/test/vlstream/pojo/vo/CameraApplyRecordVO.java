package com.ruoyi.vlstream.test.vlstream.pojo.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.CameraApplyRecord;


/**
 * 摄像头申请审批记录视图
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CameraApplyRecordVO extends CameraApplyRecord {
	private static final long serialVersionUID = 1L;

	private String deviceName;
}
