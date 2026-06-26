package org.springblade.vlstream.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import org.springblade.vlstream.config.VlsMqttProperties;
import org.springblade.vlstream.pojo.entity.CameraOsdSetting;
import org.springblade.vlstream.pojo.vo.CameraOsdSettingVO;
import org.springblade.vlstream.service.IVlsCameraOsdSettingService;
import org.springblade.vlstream.service.VlsMqttPublishService;
import org.springblade.vlstream.wrapper.VlsCameraOsdSettingWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Camera OSD settings table controller
 */
@RestController
@AllArgsConstructor
@RequestMapping("/vlsCameraOsdSetting")
@Tag(name = "Camera OSD settings", description = "Camera OSD settings interface")
public class VlsCameraOsdSettingController extends BladeController {

	private final IVlsCameraOsdSettingService vlsCameraOsdSettingService;
	private final VlsMqttPublishService vlsMqttPublishService;
	private final VlsMqttProperties vlsMqttProperties;

	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "Details", description = "Pass in cameraOsdSetting")
	public R<CameraOsdSettingVO> detail(CameraOsdSetting cameraOsdSetting) {
		CameraOsdSetting detail = vlsCameraOsdSettingService.getOne(Condition.getQueryWrapper(cameraOsdSetting));
		if (detail == null) {
			detail = buildDefaultOsdSetting(cameraOsdSetting.getDeviceId());
		}
		return R.data(VlsCameraOsdSettingWrapper.build().entityVO(detail));
	}

	private CameraOsdSetting buildDefaultOsdSetting(Long deviceId) {
		CameraOsdSetting defaultSetting = new CameraOsdSetting();
		defaultSetting.setDeviceId(deviceId);
		defaultSetting.setShowName(1);
		defaultSetting.setShowDate(1);
		defaultSetting.setShowWeek(1);
		defaultSetting.setChannelName("IPdpme");
		defaultSetting.setTimeFormat("24-hour format");
		defaultSetting.setDateFormat("XXXX-XX-XX");
		defaultSetting.setOverlay1Enabled(1);
		defaultSetting.setOverlay1Text("");
		defaultSetting.setOverlay2Enabled(1);
		defaultSetting.setOverlay2Text("");
		defaultSetting.setOverlay3Enabled(1);
		defaultSetting.setOverlay3Text("");
		defaultSetting.setOsdProperty("Opaque, non-flashing");
		defaultSetting.setOsdFont("Adaptive");
		defaultSetting.setOsdColor("Black & White Auto");
		defaultSetting.setAlignMode("Adaptive");
		return defaultSetting;
	}

	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "Pagination", description = "Pass in cameraOsdSetting")
	public R<IPage<CameraOsdSettingVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> cameraOsdSetting, Query query) {
		IPage<CameraOsdSetting> pages = vlsCameraOsdSettingService.page(Condition.getPage(query), Condition.getQueryWrapper(cameraOsdSetting, CameraOsdSetting.class));
		return R.data(VlsCameraOsdSettingWrapper.build().pageVO(pages));
	}

	@PostMapping("/save")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "Add", description = "Pass in cameraOsdSetting")
	public R save(@Valid @RequestBody CameraOsdSetting cameraOsdSetting) {
		boolean success = vlsCameraOsdSettingService.save(cameraOsdSetting);
		if (!success) {
			return R.status(false);
		}
		boolean publishSuccess = vlsMqttPublishService.publish(vlsMqttProperties.getVlsCameraOsdSettingTopic(), cameraOsdSetting);
		if (!publishSuccess) {
			return R.fail("Saved successfully, but MQTT message sending failed");
		}
		return R.status(true);
	}

	@PostMapping("/update")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "Modify", description = "Pass in cameraOsdSetting")
	public R update(@Valid @RequestBody CameraOsdSetting cameraOsdSetting) {
		return R.status(vlsCameraOsdSettingService.updateById(cameraOsdSetting));
	}

	@PostMapping("/submit")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "Add or modify", description = "Pass in cameraOsdSetting")
	public R submit(@Valid @RequestBody CameraOsdSetting cameraOsdSetting) {
		boolean success = vlsCameraOsdSettingService.saveOrUpdate(cameraOsdSetting);
		if (!success) {
			return R.status(false);
		}
		boolean publishSuccess = vlsMqttPublishService.publish(vlsMqttProperties.getVlsCameraOsdSettingTopic(), cameraOsdSetting);
		if (!publishSuccess) {
			return R.fail("Saved successfully, but MQTT message sending failed");
		}
		return R.status(true);
	}

	@GetMapping("/remove")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "Logical delete", description = "Pass in ids")
	public R remove(@Parameter(description = "Primary key collection", required = true) @RequestParam String ids) {
		return R.status(vlsCameraOsdSettingService.deleteLogic(Func.toLongList(ids)));
	}

	@GetMapping("/restoreDefault")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "Restore default values", description = "Pass in deviceId")
	public R<Boolean> restoreDefault(@RequestParam Long deviceId) {
		if (deviceId == null) {
			return R.fail("Device primary key ID cannot be empty");
		}
		CameraOsdSetting defaultSetting = buildDefaultOsdSetting(deviceId);
		CameraOsdSetting existedSetting = vlsCameraOsdSettingService.getOne(Wrappers.<CameraOsdSetting>lambdaQuery()
			.eq(CameraOsdSetting::getDeviceId, deviceId));
		if (existedSetting != null) {
			defaultSetting.setId(existedSetting.getId());
		}
		return R.status(vlsCameraOsdSettingService.saveOrUpdate(defaultSetting));
	}
}
