package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.util.Date;

/** Durable state of one OTA firmware deployment to one VLS device. */
@Data
@TableName("vls_firmware_deploy_task")
@EqualsAndHashCode(callSuper = true)
public class FirmwareDeployTask extends TenantEntity {
	private String requestId;
	private String mqttMessageId;
	private Long deviceRowId;
	private String deviceId;
	private String deviceModel;
	private String target;
	private String currentVersion;
	private String targetVersion;
	private Long firmwareId;
	private String fileName;
	private Long fileSize;
	private String sha256;
	private Boolean rollbackEnable;
	private Boolean rebootAfter;
	private String deployStatus;
	private String mqttTopic;
	private Long downloadExpiresAt;
	private Date publishedAt;
	private Date lastReplyAt;
	private Date completedAt;
	private String failureReason;
	private String replyPayload;
}
