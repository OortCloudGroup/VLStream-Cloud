package com.ruoyi.vlstream.test.vlstream.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.Func;
import com.ruoyi.vlstream.test.vlstream.config.VlsMqttProperties;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.CameraDisplaySetting;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.CameraDisplaySettingVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsCameraDisplaySettingService;
import com.ruoyi.vlstream.test.vlstream.service.VlsMqttPublishService;
import com.ruoyi.vlstream.test.vlstream.wrapper.VlsCameraDisplaySettingWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 摄像机显示设置表 控制器
 */
@RestController
@AllArgsConstructor
@RequestMapping("/vlsCameraDisplaySetting")
@Tag(name = "摄像机显示设置", description = "摄像机显示设置接口")
public class VlsCameraDisplaySettingController extends BladeController {

	private final IVlsCameraDisplaySettingService vlsCameraDisplaySettingService;
	private final VlsMqttPublishService vlsMqttPublishService;
	private final VlsMqttProperties vlsMqttProperties;

	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入cameraDisplaySetting")
	public R<CameraDisplaySettingVO> detail(CameraDisplaySetting cameraDisplaySetting) {
		CameraDisplaySetting detail = vlsCameraDisplaySettingService.getOne(Condition.getQueryWrapper(cameraDisplaySetting));
		if (detail == null) {
			detail = buildDefaultDisplaySetting(cameraDisplaySetting.getDeviceId());
		}
		return R.data(VlsCameraDisplaySettingWrapper.build().entityVO(detail));
	}

	private CameraDisplaySetting buildDefaultDisplaySetting(Long deviceId) {
		CameraDisplaySetting defaultSetting = new CameraDisplaySetting();
		defaultSetting.setDeviceId(deviceId);
		defaultSetting.setScene("室内");
		defaultSetting.setBrightness(50);
		defaultSetting.setContrast(50);
		defaultSetting.setSaturation(50);
		defaultSetting.setSharpness(50);
		defaultSetting.setExposureMode("自动");
		defaultSetting.setMaxShutterLimit("1/25");
		defaultSetting.setMinShutterLimit("1/3000");
		defaultSetting.setGainLimit(50);
		defaultSetting.setLowLightElectronicShutter("关闭");
		defaultSetting.setFocusMode("半自动");
		defaultSetting.setMinFocusDistance("1.5m");
		defaultSetting.setDayNightSwitch("自动");
		defaultSetting.setSensitivity(2);
		defaultSetting.setAntiFillLightOverExposure("关闭");
		defaultSetting.setInfraredLampMode("自动");
		defaultSetting.setBrightnessLimit(50);
		defaultSetting.setBacklightCompensation("关闭");
		defaultSetting.setWideDynamic("关闭");
		defaultSetting.setStrongLightSuppression("关闭");
		defaultSetting.setWhiteBalance("自动白平衡");
		defaultSetting.setDigitalNoiseReduction("普通模式");
		defaultSetting.setNoiseReductionLevel(50);
		defaultSetting.setDefogMode("关闭");
		defaultSetting.setElectronicStabilization("关闭");
		defaultSetting.setMirrorMode("关闭");
		defaultSetting.setPal50hz("关闭");
		defaultSetting.setLensInitialization("关闭");
		defaultSetting.setZoomLimit(2);
		return defaultSetting;
	}

	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入cameraDisplaySetting")
	public R<IPage<CameraDisplaySettingVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> cameraDisplaySetting, Query query) {
		IPage<CameraDisplaySetting> pages = vlsCameraDisplaySettingService.page(Condition.getPage(query), Condition.getQueryWrapper(cameraDisplaySetting, CameraDisplaySetting.class));
		return R.data(VlsCameraDisplaySettingWrapper.build().pageVO(pages));
	}

	@PostMapping("/save")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "新增", description = "传入cameraDisplaySetting")
	public R save(@Valid @RequestBody CameraDisplaySetting cameraDisplaySetting) {
		boolean success = vlsCameraDisplaySettingService.save(cameraDisplaySetting);
		if (!success) {
			return R.status(false);
		}
		boolean publishSuccess = vlsMqttPublishService.publish(vlsMqttProperties.getVlsCameraDisplaySettingTopic(), cameraDisplaySetting);
		if (!publishSuccess) {
			return R.fail("保存成功，但MQTT消息发送失败");
		}
		return R.status(true);
	}

	@PostMapping("/update")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "修改", description = "传入cameraDisplaySetting")
	public R update(@Valid @RequestBody CameraDisplaySetting cameraDisplaySetting) {
		return R.status(vlsCameraDisplaySettingService.updateById(cameraDisplaySetting));
	}

	@PostMapping("/submit")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "新增或修改", description = "传入cameraDisplaySetting")
	public R submit(@Valid @RequestBody CameraDisplaySetting cameraDisplaySetting) {
		boolean success = vlsCameraDisplaySettingService.saveOrUpdate(cameraDisplaySetting);
		if (!success) {
			return R.status(false);
		}
		boolean publishSuccess = vlsMqttPublishService.publish(vlsMqttProperties.getVlsCameraDisplaySettingTopic(), cameraDisplaySetting);
		if (!publishSuccess) {
			return R.fail("保存成功，但MQTT消息发送失败");
		}
		return R.status(true);
	}

	@GetMapping("/remove")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(vlsCameraDisplaySettingService.deleteLogic(Func.toLongList(ids)));
	}

	@GetMapping("/restoreDefault")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "恢复默认值", description = "传入deviceId")
	public R<Boolean> restoreDefault(@RequestParam Long deviceId) {
		if (deviceId == null) {
			return R.fail("设备主键ID不能为空");
		}
		CameraDisplaySetting defaultSetting = buildDefaultDisplaySetting(deviceId);
		CameraDisplaySetting existedSetting = vlsCameraDisplaySettingService.getOne(Wrappers.<CameraDisplaySetting>lambdaQuery()
			.eq(CameraDisplaySetting::getDeviceId, deviceId));
		if (existedSetting != null) {
			defaultSetting.setId(existedSetting.getId());
		}
		return R.status(vlsCameraDisplaySettingService.saveOrUpdate(defaultSetting));
	}
}
