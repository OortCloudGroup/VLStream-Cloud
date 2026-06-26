package org.springblade.vlstream.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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
import org.springblade.vlstream.config.VlsMqttProperties;
import org.springblade.vlstream.excel.VlsTimeStrategyExcel;
import org.springblade.vlstream.pojo.entity.TimeStrategy;
import org.springblade.vlstream.pojo.vo.TimeStrategyVO;
import org.springblade.vlstream.service.IVlsTimeStrategyService;
import org.springblade.vlstream.service.VlsMqttPublishService;
import org.springblade.vlstream.service.impl.VlsRtspRecordingManager;
import org.springblade.vlstream.wrapper.VlsTimeStrategyWrapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Time strategy table controller
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/vlsTimeStrategy")
@Tag(name = "Time strategy table", description = "Time strategy table interface")
public class VlsTimeStrategyController extends BladeController {

	private static final Map<String, Object> DEFAULT_PROTECTION_TIME = Map.of(
		"frequency", "Every day",
		"time_periods", List.of(
			Map.of("start", "08:00:00", "end", "12:00:00"),
			Map.of("start", "14:00:00", "end", "18:00:00")
		)
	);

	private final IVlsTimeStrategyService vlsTimeStrategyService;
	private final VlsMqttPublishService vlsMqttPublishService;
	private final VlsMqttProperties vlsMqttProperties;
	private final ObjectProvider<VlsRtspRecordingManager> rtspRecordingManagerProvider;

	/**
	 * Time strategy table details
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "Details", description = "Pass in vlsTimeStrategy")
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
	 * Time strategy table paging
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "Pagination", description = "Pass in vlsTimeStrategy")
	public R<IPage<TimeStrategyVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsTimeStrategy, Query query) {
		IPage<TimeStrategy> pages = vlsTimeStrategyService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsTimeStrategy, TimeStrategy.class));
		return R.data(VlsTimeStrategyWrapper.build().pageVO(pages));
	}


	/**
	 * Time strategy table custom paging
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "Pagination", description = "Pass in vlsTimeStrategy")
	public R<IPage<TimeStrategyVO>> page(TimeStrategyVO vlsTimeStrategy, Query query) {
		IPage<TimeStrategyVO> pages = vlsTimeStrategyService.selectVlsTimeStrategyPage(Condition.getPage(query), vlsTimeStrategy);
		return R.data(pages);
	}

	/**
	 * Time strategy table addition
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "Add", description = "Pass in vlsTimeStrategy")
	public R save(@Valid @RequestBody TimeStrategy vlsTimeStrategy) {
		boolean success = vlsTimeStrategyService.save(vlsTimeStrategy);
		if (!success) {
			return R.status(false);
		}
		boolean publishSuccess = vlsMqttPublishService.publish(vlsMqttProperties.getVlsTimeStrategyTopic(), vlsTimeStrategy);
		notifyRecordingRefresh();
		if (!publishSuccess) {
			return R.fail("Saved successfully, but MQTT message sending failed");
		}
		return R.status(true);
	}

	/**
	 * Time strategy table modification
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "Modify", description = "Pass in vlsTimeStrategy")
	public R update(@Valid @RequestBody TimeStrategy vlsTimeStrategy) {
		boolean success = vlsTimeStrategyService.updateById(vlsTimeStrategy);
		if (success) {
			notifyRecordingRefresh();
		}
		return R.status(success);
	}

	/**
	 * Time strategy table add or modify
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "Add or modify", description = "Pass in vlsTimeStrategy")
	public R submit(@Valid @RequestBody TimeStrategy vlsTimeStrategy) {
		boolean success = vlsTimeStrategyService.saveOrUpdate(vlsTimeStrategy);
		if (!success) {
			return R.status(false);
		}
		boolean publishSuccess = vlsMqttPublishService.publish(vlsMqttProperties.getVlsTimeStrategyTopic(), vlsTimeStrategy);
		notifyRecordingRefresh();
		if (!publishSuccess) {
			return R.fail("Saved successfully, but MQTT message sending failed");
		}
		return R.status(true);
	}

	/**
	 * Time strategy table deletion
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "Logical delete", description = "Pass in ids")
	public R remove(@Parameter(description = "Primary key collection", required = true) @RequestParam String ids) {
		boolean success = vlsTimeStrategyService.deleteLogic(Func.toLongList(ids));
		if (success) {
			notifyRecordingRefresh();
		}
		return R.status(success);
	}

	/**
	 * Export data
	 */
	@GetMapping("/export-vlsTimeStrategy")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "Export data", description = "Pass in vlsTimeStrategy")
	public void exportVlsTimeStrategy(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsTimeStrategy, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<TimeStrategy> queryWrapper = Condition.getQueryWrapper(vlsTimeStrategy, TimeStrategy.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsTimeStrategyEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsTimeStrategyEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsTimeStrategyExcel> list = vlsTimeStrategyService.exportVlsTimeStrategy(queryWrapper);
		ExcelUtil.export(response, "Time strategy table data" + DateUtil.today(), "Time strategy table data table", list, VlsTimeStrategyExcel.class);
	}


	/**
	 * Save or update time strategy
	 */
	@PostMapping
	@Operation(summary = "Save time strategy", description = "Save or update time strategy")
	public R<Boolean> saveOrUpdate(@RequestBody TimeStrategy timeStrategy) {
		boolean success = vlsTimeStrategyService.saveOrUpdateStrategy(timeStrategy);
		if (!success) {
			return R.fail("Save failed");
		}
		boolean publishSuccess = vlsMqttPublishService.publish(vlsMqttProperties.getVlsTimeStrategyTopic(), timeStrategy);
		notifyRecordingRefresh();
		if (!publishSuccess) {
			return R.fail("Saved successfully, but MQTT message sending failed");
		}
		return R.data(true);
	}

	/**
	 * Delete time strategy by device ID
	 */
	@DeleteMapping("/{deviceId}")
	@Operation(summary = "Delete time strategy", description = "Delete time strategy by device ID")
	public R<Boolean> deleteByDeviceId(@PathVariable String deviceId) {
		boolean success = vlsTimeStrategyService.deleteByDeviceId(deviceId);
		if (success) {
			notifyRecordingRefresh();
		}
		return success ? R.data(true) : R.fail("Delete failed");
	}

	private void notifyRecordingRefresh() {
		VlsRtspRecordingManager rtspRecordingManager = rtspRecordingManagerProvider.getIfAvailable();
		if (rtspRecordingManager != null) {
			log.info("Trigger recording refresh notification: controller=VlsTimeStrategyController");
			rtspRecordingManager.refreshNowAsync();
		} else {
			log.warn("Failed to trigger recording refresh: VlsRtspRecordingManager is not loaded");
		}
	}

}
