package org.springblade.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;
import org.springblade.vlstream.enums.CameraApplyStatusEnum;

import java.io.Serial;
import java.util.Date;

/**
 * Camera application approval record
 */
@Data
@TableName("vls_camera_apply_record")
@Schema(description = "CameraApplyRecord object")
@EqualsAndHashCode(callSuper = true)
public class CameraApplyRecord extends TenantEntity {

	@Serial
	private static final long serialVersionUID = 1L;

	@Schema(description = "Device Primary Key ID")
	private Long deviceInfoId;

	@Schema(description = "Reason for application")
	private String applyReason;

	@Schema(description = "Application notes")
	private String applyRemark;

	@Schema(description = "Applicant")
	private String applyUserName;

	@Schema(description = "Application time")
	private Date applyTime;

	@Schema(description = "Approval status")
	private CameraApplyStatusEnum applyStatus;

	@Schema(description = "Approval opinion")
	private String approvalComment;

	@Schema(description = "Approver")
	private String approveUserName;

	@Schema(description = "Approval time")
	private Date approveTime;

	@Schema(description = "Completion remarks")
	private String completeRemark;

	@Schema(description = "Completed by")
	private String completeUserName;

	@Schema(description = "Completion time")
	private Date completeTime;
}
