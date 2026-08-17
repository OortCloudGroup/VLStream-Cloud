package com.ruoyi.vlstream.test.vlstream.pojo.dto;

import com.ruoyi.vlstream.test.vlstream.pojo.entity.MqttDevice;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/** Device detail enriched with server-side OTA compatibility decisions. */
@Data
@Builder
public class MqttDeviceDetailView {
	private MqttDevice device;
	private boolean hasNewFirmware;
	private boolean canUpgrade;
	private String upgradeBlockedReason;
	private List<FirmwareUpgradeCandidate> availableUpgrades;
	private FirmwareDeployTaskView latestTask;
}
