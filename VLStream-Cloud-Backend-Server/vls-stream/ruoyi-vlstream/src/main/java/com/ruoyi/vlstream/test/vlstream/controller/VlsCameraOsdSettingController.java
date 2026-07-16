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
import com.ruoyi.vlstream.test.vlstream.pojo.entity.CameraOsdSetting;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.CameraOsdSettingVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsCameraOsdSettingService;
import com.ruoyi.vlstream.test.vlstream.service.VlsMqttPublishService;
import com.ruoyi.vlstream.test.vlstream.wrapper.VlsCameraOsdSettingWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 摄像机OSD设置表 控制器
 */
@RestController
@AllArgsConstructor
@RequestMapping("/vlsCameraOsdSetting")
@Tag(name = "摄像机OSD设置", description = "摄像机OSD设置接口")
public class VlsCameraOsdSettingController extends BladeController {

	private final IVlsCameraOsdSettingService vlsCameraOsdSettingService;
	private final VlsMqttPublishService vlsMqttPublishService;
	private final VlsMqttProperties vlsMqttProperties;

	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入cameraOsdSetting")
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
		defaultSetting.setTimeFormat("24小时制");
		defaultSetting.setDateFormat("XXXX年XX月XX日");
		defaultSetting.setOverlay1Enabled(1);
		defaultSetting.setOverlay1Text("");
		defaultSetting.setOverlay2Enabled(1);
		defaultSetting.setOverlay2Text("");
		defaultSetting.setOverlay3Enabled(1);
		defaultSetting.setOverlay3Text("");
		defaultSetting.setOsdProperty("不透明、不闪烁");
		defaultSetting.setOsdFont("自适应");
		defaultSetting.setOsdColor("黑白自动");
		defaultSetting.setAlignMode("自适应");
		return defaultSetting;
	}

	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入cameraOsdSetting")
	public R<IPage<CameraOsdSettingVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> cameraOsdSetting, Query query) {
		IPage<CameraOsdSetting> pages = vlsCameraOsdSettingService.page(Condition.getPage(query), Condition.getQueryWrapper(cameraOsdSetting, CameraOsdSetting.class));
		return R.data(VlsCameraOsdSettingWrapper.build().pageVO(pages));
	}

	@PostMapping("/save")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "新增", description = "传入cameraOsdSetting")
	public R save(@Valid @RequestBody CameraOsdSetting cameraOsdSetting) {
		boolean success = vlsCameraOsdSettingService.save(cameraOsdSetting);
		if (!success) {
			return R.status(false);
		}
		boolean publishSuccess = vlsMqttPublishService.publish(vlsMqttProperties.getVlsCameraOsdSettingTopic(), cameraOsdSetting);
		if (!publishSuccess) {
			return R.fail("保存成功，但MQTT消息发送失败");
		}
		return R.status(true);
	}

	@PostMapping("/update")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "修改", description = "传入cameraOsdSetting")
	public R update(@Valid @RequestBody CameraOsdSetting cameraOsdSetting) {
		return R.status(vlsCameraOsdSettingService.updateById(cameraOsdSetting));
	}

	@PostMapping("/submit")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "新增或修改", description = "传入cameraOsdSetting")
	public R submit(@Valid @RequestBody CameraOsdSetting cameraOsdSetting) {
		boolean success = vlsCameraOsdSettingService.saveOrUpdate(cameraOsdSetting);
		if (!success) {
			return R.status(false);
		}
		boolean publishSuccess = vlsMqttPublishService.publish(vlsMqttProperties.getVlsCameraOsdSettingTopic(), cameraOsdSetting);
		if (!publishSuccess) {
			return R.fail("保存成功，但MQTT消息发送失败");
		}
		return R.status(true);
	}

	@GetMapping("/remove")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(vlsCameraOsdSettingService.deleteLogic(Func.toLongList(ids)));
	}

	@GetMapping("/restoreDefault")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "恢复默认值", description = "传入deviceId")
	public R<Boolean> restoreDefault(@RequestParam Long deviceId) {
		if (deviceId == null) {
			return R.fail("设备主键ID不能为空");
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
