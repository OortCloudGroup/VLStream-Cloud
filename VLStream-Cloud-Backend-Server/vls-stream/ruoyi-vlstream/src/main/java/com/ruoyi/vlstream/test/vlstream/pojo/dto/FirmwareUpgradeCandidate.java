package com.ruoyi.vlstream.test.vlstream.pojo.dto;

import lombok.Builder;
import lombok.Data;

/** Latest compatible firmware for one device partition. */
@Data
@Builder
public class FirmwareUpgradeCandidate {
	private Long firmwareId;
	private String target;
	private String currentVersion;
	private String latestVersion;
	private String fileName;
	private Long fileSize;
	private String sha256;
}
