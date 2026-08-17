package com.ruoyi.vlstream.test.vlstream.service;

import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;
import java.util.regex.Pattern;

/** Numeric dotted firmware versions used by VLS devices (for example 1.0.1.14). */
public final class FirmwareVersion {

	private static final Pattern VERSION = Pattern.compile(
		"(?:0|[1-9]\\d*)(?:\\.(?:0|[1-9]\\d*)){2,}");

	private FirmwareVersion() {
	}

	public static boolean isValid(String value) {
		String normalized = StringUtils.trimToEmpty(value);
		return normalized.length() <= 64 && VERSION.matcher(normalized).matches();
	}

	public static int compare(String left, String right) {
		if (!isValid(left) || !isValid(right)) {
			throw new IllegalArgumentException("Invalid numeric dotted firmware version");
		}
		String[] leftParts = StringUtils.trim(left).split("\\.");
		String[] rightParts = StringUtils.trim(right).split("\\.");
		int length = Math.max(leftParts.length, rightParts.length);
		for (int i = 0; i < length; i++) {
			BigInteger leftPart = i < leftParts.length ? new BigInteger(leftParts[i]) : BigInteger.ZERO;
			BigInteger rightPart = i < rightParts.length ? new BigInteger(rightParts[i]) : BigInteger.ZERO;
			int compared = leftPart.compareTo(rightPart);
			if (compared != 0) {
				return compared;
			}
		}
		return 0;
	}

	public static boolean isGreater(String candidate, String current) {
		return compare(candidate, current) > 0;
	}
}
