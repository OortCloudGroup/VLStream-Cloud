package com.ruoyi.vlstream.test.vlstream.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.excel.util.ExcelUtil;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import com.ruoyi.vlstream.test.vlstream.config.VlsMqttProperties;
import com.ruoyi.vlstream.test.vlstream.excel.VlsTimeStrategyExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TimeStrategy;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.TimeStrategyVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsTimeStrategyService;
import com.ruoyi.vlstream.test.vlstream.service.VlsMqttPublishService;
import com.ruoyi.vlstream.test.vlstream.service.impl.VlsRtspRecordingManager;
import com.ruoyi.vlstream.test.vlstream.wrapper.VlsTimeStrategyWrapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 时间策略表 控制器
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/vlsTimeStrategy")
@Tag(name = "时间策略表", description = "时间策略表接口")
public class VlsTimeStrategyController extends BladeController {

	private static final Map<String, Object> DEFAULT_PROTECTION_TIME = org.springblade.core.tool.utils.CollectionUtil.mapOf(
		"frequency", "每天",
		"time_periods", java.util.Arrays.asList(
			org.springblade.core.tool.utils.CollectionUtil.mapOf("start", "08:00:00", "end", "12:00:00"),
			org.springblade.core.tool.utils.CollectionUtil.mapOf("start", "14:00:00", "end", "18:00:00")
		)
	);

	private final IVlsTimeStrategyService vlsTimeStrategyService;
	private final VlsMqttPublishService vlsMqttPublishService;
	private final VlsMqttProperties vlsMqttProperties;
	private final ObjectProvider<VlsRtspRecordingManager> rtspRecordingManagerProvider;

	/**
	 * 时间策略表 详情
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入vlsTimeStrategy")
	public R<TimeStrategyVO> detail(TimeStrategy vlsTimeStrategy) {
		TimeStrategy detail = vlsTimeStrategyService.getOne(Condition.getQueryWrapper(vlsTimeStrategy));
		if (detail == null) {
			detail = new TimeStrategy();
			if (vlsTimeStrategy != null) {
				detail.setDeviceId(vlsTimeStrategy.getDeviceId());
			}
			detail.setProtectionTime(DEFAULT_PROTECTION_TIME);
		}
		if (Objects.isNull(detail.getProtectionTime())) {
			detail.setProtectionTime(DEFAULT_PROTECTION_TIME);
		}
		return R.data(VlsTimeStrategyWrapper.build().entityVO(detail));
	}

	/**
	 * 时间策略表 分页
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入vlsTimeStrategy")
	public R<IPage<TimeStrategyVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsTimeStrategy, Query query) {
		IPage<TimeStrategy> pages = vlsTimeStrategyService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsTimeStrategy, TimeStrategy.class));
		return R.data(VlsTimeStrategyWrapper.build().pageVO(pages));
	}


	/**
	 * 时间策略表 自定义分页
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入vlsTimeStrategy")
	public R<IPage<TimeStrategyVO>> page(TimeStrategyVO vlsTimeStrategy, Query query) {
		IPage<TimeStrategyVO> pages = vlsTimeStrategyService.selectVlsTimeStrategyPage(Condition.getPage(query), vlsTimeStrategy);
		return R.data(pages);
	}

	/**
	 * 时间策略表 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入vlsTimeStrategy")
	public R save(@Valid @RequestBody TimeStrategy vlsTimeStrategy) {
		boolean success = vlsTimeStrategyService.save(vlsTimeStrategy);
		if (!success) {
			return R.status(false);
		}
		boolean publishSuccess = vlsMqttPublishService.publish(vlsMqttProperties.getVlsTimeStrategyTopic(), vlsTimeStrategy);
		notifyRecordingRefresh();
		if (!publishSuccess) {
			return R.fail("保存成功，但MQTT消息发送失败");
		}
		return R.status(true);
	}

	/**
	 * 时间策略表 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入vlsTimeStrategy")
	public R update(@Valid @RequestBody TimeStrategy vlsTimeStrategy) {
		boolean success = vlsTimeStrategyService.updateById(vlsTimeStrategy);
		if (success) {
			notifyRecordingRefresh();
		}
		return R.status(success);
	}

	/**
	 * 时间策略表 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入vlsTimeStrategy")
	public R submit(@Valid @RequestBody TimeStrategy vlsTimeStrategy) {
		boolean success = vlsTimeStrategyService.saveOrUpdate(vlsTimeStrategy);
		if (!success) {
			return R.status(false);
		}
		boolean publishSuccess = vlsMqttPublishService.publish(vlsMqttProperties.getVlsTimeStrategyTopic(), vlsTimeStrategy);
		notifyRecordingRefresh();
		if (!publishSuccess) {
			return R.fail("保存成功，但MQTT消息发送失败");
		}
		return R.status(true);
	}

	/**
	 * 时间策略表 删除
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		boolean success = vlsTimeStrategyService.deleteLogic(Func.toLongList(ids));
		if (success) {
			notifyRecordingRefresh();
		}
		return R.status(success);
	}

	/**
	 * 导出数据
	 */
	@GetMapping("/export-vlsTimeStrategy")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "导出数据", description = "传入vlsTimeStrategy")
	public void exportVlsTimeStrategy(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsTimeStrategy, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<TimeStrategy> queryWrapper = Condition.getQueryWrapper(vlsTimeStrategy, TimeStrategy.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsTimeStrategyEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsTimeStrategyEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsTimeStrategyExcel> list = vlsTimeStrategyService.exportVlsTimeStrategy(queryWrapper);
		ExcelUtil.export(response, "时间策略表数据" + DateUtil.today(), "时间策略表数据表", list, VlsTimeStrategyExcel.class);
	}


	/**
	 * 保存或更新时间策略
	 */
	@PostMapping
	@Operation(summary = "保存时间策略", description = "保存或更新时间策略")
	public R<Boolean> saveOrUpdate(@RequestBody TimeStrategy timeStrategy) {
		boolean success = vlsTimeStrategyService.saveOrUpdateStrategy(timeStrategy);
		if (!success) {
			return R.fail("保存失败");
		}
		boolean publishSuccess = vlsMqttPublishService.publish(vlsMqttProperties.getVlsTimeStrategyTopic(), timeStrategy);
		notifyRecordingRefresh();
		if (!publishSuccess) {
			return R.fail("保存成功，但MQTT消息发送失败");
		}
		return R.data(true);
	}

	/**
	 * 根据设备ID删除时间策略
	 */
	@DeleteMapping("/{deviceId}")
	@Operation(summary = "删除时间策略", description = "根据设备ID删除时间策略")
	public R<Boolean> deleteByDeviceId(@PathVariable String deviceId) {
		boolean success = vlsTimeStrategyService.deleteByDeviceId(deviceId);
		if (success) {
			notifyRecordingRefresh();
		}
		return success ? R.data(true) : R.fail("删除失败");
	}

	private void notifyRecordingRefresh() {
		VlsRtspRecordingManager rtspRecordingManager = rtspRecordingManagerProvider.getIfAvailable();
		if (rtspRecordingManager != null) {
			log.info("触发录制刷新通知: controller=VlsTimeStrategyController");
			rtspRecordingManager.refreshNowAsync();
		} else {
			log.warn("触发录制刷新失败: VlsRtspRecordingManager未加载");
		}
	}

}
