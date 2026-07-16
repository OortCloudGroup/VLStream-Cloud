package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;
import com.ruoyi.vlstream.test.vlstream.enums.CameraApplyStatusEnum;

import java.util.Date;

/**
 * 摄像头申请审批记录
 */
@Data
@TableName("vls_camera_apply_record")
@Schema(description = "CameraApplyRecord对象")
@EqualsAndHashCode(callSuper = true)
public class CameraApplyRecord extends TenantEntity {
	private static final long serialVersionUID = 1L;

	@Schema(description = "设备主键ID")
	private Long deviceInfoId;

	@Schema(description = "申请原因")
	private String applyReason;

	@Schema(description = "申请备注")
	private String applyRemark;

	@Schema(description = "申请人")
	private String applyUserName;

	@Schema(description = "申请时间")
	private Date applyTime;

	@Schema(description = "审批状态")
	private CameraApplyStatusEnum applyStatus;

	@Schema(description = "审批意见")
	private String approvalComment;

	@Schema(description = "审批人")
	private String approveUserName;

	@Schema(description = "审批时间")
	private Date approveTime;

	@Schema(description = "完结备注")
	private String completeRemark;

	@Schema(description = "完结人")
	private String completeUserName;

	@Schema(description = "完结时间")
	private Date completeTime;
}
