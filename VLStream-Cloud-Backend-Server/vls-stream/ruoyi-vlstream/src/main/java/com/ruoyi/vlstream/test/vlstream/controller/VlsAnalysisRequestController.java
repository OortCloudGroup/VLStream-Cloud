/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

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
import org.apache.commons.lang3.StringUtils;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.excel.util.ExcelUtil;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import com.ruoyi.vlstream.test.vlstream.enums.AnalysisRequestStatusEnum;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAnalysisRequestExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnalysisRequest;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.DeviceInfo;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AnalysisRequestVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAnalysisRequestService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsDeviceInfoService;
import com.ruoyi.vlstream.test.vlstream.wrapper.VlsAnalysisRequestWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 智能分析请求表 控制器
 *
 * @author Oort
 * @since 2025-12-23
 */
@RestController
@AllArgsConstructor
@RequestMapping("/vlsAnalysisRequest")
@Tag(name = "智能分析请求表", description = "智能分析请求表接口")
public class VlsAnalysisRequestController extends BladeController {

	private final IVlsAnalysisRequestService vlsAnalysisRequestService;
	private final IVlsDeviceInfoService vlsDeviceInfoService;

	/**
	 * 智能分析申请
	 */
	@PostMapping("/apply")
	@ApiOperationSupport(order = 0)
	@Operation(summary = "申请", description = "提交智能分析申请")
	public R<Boolean> apply(@Valid @RequestBody AnalysisRequest vlsAnalysisRequest) {
		vlsAnalysisRequest.setRequestStatus(AnalysisRequestStatusEnum.processing);
		if (vlsAnalysisRequest.getProgress() == null) {
			vlsAnalysisRequest.setProgress(0);
		}
		vlsAnalysisRequest.setResultPath(null);
		vlsAnalysisRequest.setErrorMessage(null);
		vlsAnalysisRequest.setStartTime(null);
		vlsAnalysisRequest.setCompleteTime(null);
		return R.status(vlsAnalysisRequestService.saveOrUpdate(vlsAnalysisRequest));
	}

	/**
	 * 取消智能分析申请
	 */
	@GetMapping("/cancel")
	@ApiOperationSupport(order = 0)
	@Operation(summary = "取消申请", description = "根据id取消智能分析申请")
	public R<Boolean> cancel(@Parameter(description = "申请ID", required = true) @RequestParam Long id) {
		AnalysisRequest analysisRequest = vlsAnalysisRequestService.getById(id);
		if (analysisRequest == null) {
			return R.fail("申请不存在");
		}
		if (analysisRequest.getRequestStatus() != AnalysisRequestStatusEnum.processing) {
			return R.fail("当前状态不支持取消");
		}
		AnalysisRequest updateAnalysisRequest = new AnalysisRequest();
		updateAnalysisRequest.setId(id);
		updateAnalysisRequest.setRequestStatus(AnalysisRequestStatusEnum.cancel);
		return R.status(vlsAnalysisRequestService.updateById(updateAnalysisRequest));
	}

	/**
	 * 智能分析请求表 详情
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description  = "传入vlsAnalysisRequest")
	public R<AnalysisRequestVO> detail(AnalysisRequest vlsAnalysisRequest) {
		AnalysisRequest detail = vlsAnalysisRequestService.getOne(Condition.getQueryWrapper(vlsAnalysisRequest));
		AnalysisRequestVO analysisRequestVO = VlsAnalysisRequestWrapper.build().entityVO(detail);
		fillCameraName(analysisRequestVO);
		return R.data(analysisRequestVO);
	}

	/**
	 * 智能分析请求表 分页
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description  = "传入vlsAnalysisRequest")
	public R<IPage<AnalysisRequestVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAnalysisRequest, Query query) {
		IPage<AnalysisRequest> pages = vlsAnalysisRequestService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsAnalysisRequest, AnalysisRequest.class));
		IPage<AnalysisRequestVO> pageVO = VlsAnalysisRequestWrapper.build().pageVO(pages);
		fillCameraName(pageVO.getRecords());
		return R.data(pageVO);
	}

	private void fillCameraName(AnalysisRequestVO analysisRequestVO) {
		if (analysisRequestVO == null) {
			return;
		}
		List<AnalysisRequestVO> analysisRequestVOList = new ArrayList<>();
		analysisRequestVOList.add(analysisRequestVO);
		fillCameraName(analysisRequestVOList);
	}

	private void fillCameraName(List<AnalysisRequestVO> analysisRequestVOList) {
		if (analysisRequestVOList == null || analysisRequestVOList.isEmpty()) {
			return;
		}
		Set<Long> deviceIdSet = new HashSet<>();
		for (AnalysisRequestVO analysisRequestVO : analysisRequestVOList) {
			if (analysisRequestVO == null || StringUtils.isBlank(analysisRequestVO.getDeviceIds())) {
				continue;
			}
			String[] deviceIdArray = analysisRequestVO.getDeviceIds().split(",");
			for (String deviceIdText : deviceIdArray) {
				String deviceIdTrimText = deviceIdText == null ? null : deviceIdText.trim();
				if (StringUtils.isBlank(deviceIdTrimText) || !StringUtils.isNumeric(deviceIdTrimText)) {
					continue;
				}
				deviceIdSet.add(Long.valueOf(deviceIdTrimText));
			}
		}
		Map<Long, String> deviceNameMap = new HashMap<>();
		if (!deviceIdSet.isEmpty()) {
			List<DeviceInfo> deviceInfoList = vlsDeviceInfoService.listByIds(deviceIdSet);
			for (DeviceInfo deviceInfo : deviceInfoList) {
				if (deviceInfo == null || deviceInfo.getId() == null) {
					continue;
				}
				deviceNameMap.put(deviceInfo.getId(), deviceInfo.getDeviceName());
			}
		}
		for (AnalysisRequestVO analysisRequestVO : analysisRequestVOList) {
			if (analysisRequestVO == null || StringUtils.isBlank(analysisRequestVO.getDeviceIds())) {
				continue;
			}
			List<String> cameraNameList = new ArrayList<>();
			String[] deviceIdArray = analysisRequestVO.getDeviceIds().split(",");
			for (String deviceIdText : deviceIdArray) {
				String deviceIdTrimText = deviceIdText == null ? null : deviceIdText.trim();
				if (StringUtils.isBlank(deviceIdTrimText)) {
					continue;
				}
				String cameraName = null;
				if (StringUtils.isNumeric(deviceIdTrimText)) {
					cameraName = deviceNameMap.get(Long.valueOf(deviceIdTrimText));
				}
				if (StringUtils.isBlank(cameraName)) {
					DeviceInfo deviceInfo = vlsDeviceInfoService.getByDeviceId(deviceIdTrimText);
					if (deviceInfo != null) {
						cameraName = deviceInfo.getDeviceName();
					}
				}
				if (StringUtils.isNotBlank(cameraName)) {
					cameraNameList.add(cameraName);
				}
			}
			analysisRequestVO.setCameraName(String.join(",", cameraNameList));
		}
	}

	/**
	 * 智能分析请求表 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description  = "传入vlsAnalysisRequest")
	public R save(@Valid @RequestBody AnalysisRequest vlsAnalysisRequest) {
		return R.status(vlsAnalysisRequestService.save(vlsAnalysisRequest));
	}

	/**
	 * 智能分析请求表 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description  = "传入vlsAnalysisRequest")
	public R update(@Valid @RequestBody AnalysisRequest vlsAnalysisRequest) {
		return R.status(vlsAnalysisRequestService.updateById(vlsAnalysisRequest));
	}

	/**
	 * 智能分析请求表 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description  = "传入vlsAnalysisRequest")
	public R submit(@Valid @RequestBody AnalysisRequest vlsAnalysisRequest) {
		return R.status(vlsAnalysisRequestService.saveOrUpdate(vlsAnalysisRequest));
	}

	/**
	 * 智能分析请求表 删除
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description  = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(vlsAnalysisRequestService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * 导出数据
	 */
	@GetMapping("/export-vlsAnalysisRequest")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "导出数据", description  = "传入vlsAnalysisRequest")
	public void exportVlsAnalysisRequest(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAnalysisRequest, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<AnalysisRequest> queryWrapper = Condition.getQueryWrapper(vlsAnalysisRequest, AnalysisRequest.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsAnalysisRequestEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsAnalysisRequestEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsAnalysisRequestExcel> list = vlsAnalysisRequestService.exportVlsAnalysisRequest(queryWrapper);
		ExcelUtil.export(response, "智能分析请求表数据" + DateUtil.today(), "智能分析请求表数据表", list, VlsAnalysisRequestExcel.class);
	}

}
