package com.ruoyi.vlstream.test.vlstream.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.vlstream.test.vlstream.mapper.MqttDeviceMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.MqttDeviceStreamMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FirmwareDeployRequest;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FirmwareDeployTaskView;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.MqttDeviceDetailView;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.MqttDevice;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.MqttDeviceStream;
import com.ruoyi.vlstream.test.vlstream.service.FirmwareDeploymentService;
import com.ruoyi.vlstream.test.vlstream.service.VlsZlmService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springblade.core.tool.api.R;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;
import java.util.Map;

/** Management and preview APIs for native MQTT devices. */
@RestController
@RequestMapping("/vlsMqttDevice")
@RequiredArgsConstructor
public class VlsMqttDeviceController {

	private final MqttDeviceMapper deviceMapper;
	private final MqttDeviceStreamMapper streamMapper;
	private final VlsZlmService zlmService;
	private final FirmwareDeploymentService firmwareDeploymentService;

	@GetMapping("/page")
	public R<Page<MqttDevice>> page(@RequestParam(defaultValue = "1") long current,
		@RequestParam(defaultValue = "10") long size,
		@RequestParam(required = false) String keyword,
		@RequestParam(required = false) Boolean online) {
		LambdaQueryWrapper<MqttDevice> query = new LambdaQueryWrapper<MqttDevice>()
			.eq(MqttDevice::getIsDeleted, 0)
			.eq(online != null, MqttDevice::getOnline, online)
			.and(StringUtils.isNotBlank(keyword), wrapper -> wrapper
				.like(MqttDevice::getDeviceName, keyword)
				.or().like(MqttDevice::getDeviceId, keyword)
				.or().like(MqttDevice::getDeviceSerial, keyword))
			.orderByDesc(MqttDevice::getLastHeartbeatTime);
		return R.data(deviceMapper.selectPage(new Page<>(current, Math.min(size, 100)), query));
	}

	@GetMapping("/{deviceId}/streams")
	public R<List<MqttDeviceStream>> streams(@PathVariable Long deviceId) {
		return R.data(streamMapper.selectList(new LambdaQueryWrapper<MqttDeviceStream>()
			.eq(MqttDeviceStream::getDeviceRowId, deviceId)
			.eq(MqttDeviceStream::getAvailable, true)
			.eq(MqttDeviceStream::getIsDeleted, 0)
			.orderByDesc(MqttDeviceStream::getIsDefault)
			.orderByAsc(MqttDeviceStream::getChannelId, MqttDeviceStream::getStreamType)));
	}

	@GetMapping("/{deviceId}/detail")
	public R<MqttDeviceDetailView> detail(@PathVariable Long deviceId) {
		return R.data(firmwareDeploymentService.detail(deviceId));
	}

	@SaCheckPermission("vls:firmware:deploy")
	@PostMapping("/{deviceId}/firmware-upgrades")
	public R<FirmwareDeployTaskView> deployFirmware(@PathVariable Long deviceId,
		@RequestBody FirmwareDeployRequest request) {
		return R.data(firmwareDeploymentService.deploy(deviceId,
			request == null ? null : request.getFirmwareId()));
	}

	@SaCheckPermission("vls:firmware:deploy")
	@PostMapping("/{deviceId}/firmware-upgrades/{requestId}/cancel")
	public R<FirmwareDeployTaskView> cancelFirmwareUpgrade(@PathVariable Long deviceId,
		@PathVariable String requestId) {
		return R.data(firmwareDeploymentService.cancel(deviceId, requestId));
	}

	@PostMapping("/{deviceId}/preview")
	public R<Map<String, Object>> preview(@PathVariable Long deviceId, @RequestBody PreviewRequest request) {
		MqttDevice device = deviceMapper.selectById(deviceId);
		MqttDeviceStream stream = request == null ? null : streamMapper.selectById(request.getStreamId());
		if (device == null || stream == null || !deviceId.equals(stream.getDeviceRowId())
			|| !Boolean.TRUE.equals(stream.getAvailable())) {
			return R.fail("设备或流不存在");
		}
		try {
			Map<String, Object> preview = zlmService.createProxy(stream.getId(), stream.getSourceUrl());
			stream.setZlmApp(String.valueOf(preview.get("app")));
			stream.setZlmStream(String.valueOf(preview.get("stream")));
			Object proxyKey = preview.get("proxyKey");
			stream.setZlmProxyKey(proxyKey == null ? null : String.valueOf(proxyKey));
			stream.setUpdateTime(new Date());
			streamMapper.updateById(stream);
			return R.data(preview);
		} catch (RuntimeException ex) {
			return R.fail(ex.getMessage());
		}
	}

	@DeleteMapping("/{deviceId}/streams/{streamId}/preview")
	public R<Void> closePreview(@PathVariable Long deviceId, @PathVariable Long streamId) {
		MqttDeviceStream stream = streamMapper.selectById(streamId);
		if (stream == null || !deviceId.equals(stream.getDeviceRowId())) return R.fail("流不存在");
		// Do not delete the shared proxy here: another browser may still be watching it.
		// Closing the WebRTC peer decrements ZLM's reader count and auto_close=1 releases it safely.
		stream.setZlmProxyKey(null);
		stream.setUpdateTime(new Date());
		streamMapper.updateById(stream);
		return R.success();
	}

	@GetMapping("/media/status")
	public R<Map<String, Object>> mediaStatus() {
		return R.data(zlmService.status());
	}

	@Data
	public static class PreviewRequest {
		private Long streamId;
	}
}
