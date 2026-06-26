package org.springblade.vlstream.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.tool.api.R;
import org.springblade.vlstream.config.VlsMqttProperties;
import org.springblade.vlstream.pojo.entity.RecordEventStrategy;
import org.springblade.vlstream.service.IVlsRecordEventStrategyService;
import org.springblade.vlstream.service.VlsMqttPublishService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Camera event strategy controller
 */
@RestController
@AllArgsConstructor
@RequestMapping("/vlsRecordEventStrategy")
@Tag(name = "Camera event strategy", description = "Camera event strategy interface")
public class VlsRecordEventStrategyController extends BladeController {

	private final IVlsRecordEventStrategyService recordEventStrategyService;
	private final VlsMqttPublishService vlsMqttPublishService;
	private final VlsMqttProperties vlsMqttProperties;

	private static final Map<String, Object> DEFAULT_PROTECTION_TIME = Map.of(
		"frequency", "Every day",
		"time_periods", List.of(
			Map.of("start", "08:00:00", "end", "12:00:00"),
			Map.of("start", "14:00:00", "end", "18:00:00")
		)
	);

	@GetMapping("/{deviceId}")
	@Operation(summary = "Get event policy", description = "Get event strategy by device number")
	public R<RecordEventStrategy> getByDeviceId(@PathVariable String deviceId) {
		RecordEventStrategy detail = recordEventStrategyService.getByDeviceId(deviceId);
		if (detail == null) {
			detail = new RecordEventStrategy();
			detail.setDeviceId(deviceId);
			detail.setProtectionTime(DEFAULT_PROTECTION_TIME);
		}
		if (Objects.isNull(detail.getProtectionTime())) {
			detail.setProtectionTime(DEFAULT_PROTECTION_TIME);
		}
		return R.data(detail);
	}

	@PostMapping
	@Operation(summary = "Save event strategy", description = "Save or update event strategy (including trigger action, alarm frequency, alarm level, alarm method, receiver, and cycle configuration)")
	public R<Boolean> saveOrUpdate(@RequestBody RecordEventStrategy recordEventStrategy) {
		boolean success = recordEventStrategyService.saveOrUpdateStrategy(recordEventStrategy);
		if (!success) {
			return R.fail("Save failed");
		}
		boolean publishSuccess = vlsMqttPublishService.publish(vlsMqttProperties.getVlsRecordEventStrategyTopic(), recordEventStrategy);
		if (!publishSuccess) {
			return R.fail("Saved successfully, but MQTT message sending failed");
		}
		return R.data(true);
	}

	@DeleteMapping("/{deviceId}")
	@Operation(summary = "Delete event strategy", description = "Delete event strategy by device number")
	public R<Boolean> deleteByDeviceId(@PathVariable String deviceId) {
		boolean success = recordEventStrategyService.deleteByDeviceId(deviceId);
		return success ? R.data(true) : R.fail("Delete failed");
	}
}
