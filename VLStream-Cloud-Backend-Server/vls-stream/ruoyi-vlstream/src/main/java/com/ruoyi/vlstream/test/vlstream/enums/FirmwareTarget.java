package com.ruoyi.vlstream.test.vlstream.enums;

import org.apache.commons.lang3.StringUtils;

/** OTA partitions supported by the VLS 2.2 device protocol. */
public enum FirmwareTarget {
	ROOTFS("rootfs", true, true);

	private final String value;
	private final boolean rollbackEnable;
	private final boolean rebootAfter;

	FirmwareTarget(String value, boolean rollbackEnable, boolean rebootAfter) {
		this.value = value;
		this.rollbackEnable = rollbackEnable;
		this.rebootAfter = rebootAfter;
	}

	public String getValue() {
		return value;
	}

	public boolean isRollbackEnable() {
		return rollbackEnable;
	}

	public boolean isRebootAfter() {
		return rebootAfter;
	}

	public static FirmwareTarget fromValue(String value) {
		for (FirmwareTarget target : values()) {
			if (StringUtils.equals(target.value, StringUtils.lowerCase(StringUtils.trimToEmpty(value)))) {
				return target;
			}
		}
		throw new IllegalArgumentException("target must be rootfs");
	}
}
