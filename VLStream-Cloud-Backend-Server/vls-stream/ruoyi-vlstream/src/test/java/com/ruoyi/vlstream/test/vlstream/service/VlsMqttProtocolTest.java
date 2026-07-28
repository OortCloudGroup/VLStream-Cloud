package com.ruoyi.vlstream.test.vlstream.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("dev")
class VlsMqttProtocolTest {

	@Test
	void buildsOneBusTopicPerDevice() {
		assertEquals(
			"vlstream/v2.2/dev/CAM-20260001/bus",
			VlsMqttProtocol.deviceBusTopic("CAM-20260001"));
	}

	@Test
	void rejectsDeviceIdsThatCanEscapeTheBusTopic() {
		assertThrows(IllegalArgumentException.class,
			() -> VlsMqttProtocol.deviceBusTopic("CAM/other"));
		assertThrows(IllegalArgumentException.class,
			() -> VlsMqttProtocol.deviceBusTopic("CAM+"));
		assertThrows(IllegalArgumentException.class,
			() -> VlsMqttProtocol.deviceBusTopic(""));
	}
}
