package com.ruoyi.vlstream.test.vlstream.pojo.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Date;

/** OTA task fields safe for management-page display. */
@Data
@Builder
public class FirmwareDeployTaskView {
	private String requestId;
	private String target;
	private String currentVersion;
	private String targetVersion;
	private String deployStatus;
	private Date publishedAt;
	private Date lastReplyAt;
	private Date completedAt;
	private String failureReason;
}
