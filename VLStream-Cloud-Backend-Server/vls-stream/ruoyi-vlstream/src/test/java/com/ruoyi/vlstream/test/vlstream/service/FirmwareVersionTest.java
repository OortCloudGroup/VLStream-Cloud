package com.ruoyi.vlstream.test.vlstream.service;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("dev")
class FirmwareVersionTest {

	@Test
	void comparesNumericSegmentsInsteadOfLexicographicText() {
		assertTrue(FirmwareVersion.isGreater("1.0.1.14", "1.0.1.9"));
		assertTrue(FirmwareVersion.isGreater("1.10.0", "1.9.99"));
		assertFalse(FirmwareVersion.isGreater("1.0.0", "1.0.0.0"));
	}

	@Test
	void rejectsLabelsAndVersionsWithTooFewSegments() {
		assertFalse(FirmwareVersion.isValid("v1.2.3"));
		assertFalse(FirmwareVersion.isValid("1.2"));
		assertTrue(FirmwareVersion.isValid("1.0.1.14"));
	}
}
