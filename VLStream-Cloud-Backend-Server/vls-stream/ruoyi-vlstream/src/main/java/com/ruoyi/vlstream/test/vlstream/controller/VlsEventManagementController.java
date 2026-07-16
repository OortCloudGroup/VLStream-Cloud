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
import com.ruoyi.vlstream.test.modules.system.service.IFileUploadService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsEventManagementExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.FileResponseDto;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.EventManagement;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.EventManagementVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsEventManagementService;
import com.ruoyi.vlstream.test.vlstream.wrapper.VlsEventManagementWrapper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 事件管理表 控制器
 *
 * @author Oort
 * @since 2025-12-23
 */
@RestController
@AllArgsConstructor
@RequestMapping("/vlsEventManagement")
@Tag(name = "事件管理表", description = "事件管理表接口")
public class VlsEventManagementController extends BladeController {

	private final IVlsEventManagementService vlsEventManagementService;
	private final IFileUploadService fileUploadService;

	private static final String FILE_UPLOAD_APP_ID = "818301f0e77f4cd8a117414cbeb32d9e";
	private static final String FILE_UPLOAD_SECRET_KEY = "5f0de11687d744bc95e84e207d319493";

	/**
	 * 事件管理表 详情
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入vlsEventManagement")
	public R<EventManagementVO> detail(EventManagement vlsEventManagement) {
		EventManagement detail = vlsEventManagementService.getOne(Condition.getQueryWrapper(vlsEventManagement));
		return R.data(VlsEventManagementWrapper.build().entityVO(detail));
	}

	/**
	 * 事件管理表 分页
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入vlsEventManagement")
	public R<IPage<EventManagementVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsEventManagement, Query query) {
		IPage<EventManagement> pages = vlsEventManagementService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsEventManagement, EventManagement.class));
		return R.data(VlsEventManagementWrapper.build().pageVO(pages));
	}


	/**
	 * 事件管理表 自定义分页
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入vlsEventManagement")
	public R<IPage<EventManagementVO>> page(EventManagementVO vlsEventManagement, Query query) {
		IPage<EventManagementVO> pages = vlsEventManagementService.selectVlsEventManagementPage(Condition.getPage(query), vlsEventManagement);
		return R.data(pages);
	}

	/**
	 * 事件管理表 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入vlsEventManagement")
	public R save(@Valid @RequestBody EventManagement vlsEventManagement) {
		return R.status(vlsEventManagementService.save(vlsEventManagement));
	}

	/**
	 * 事件管理表 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入vlsEventManagement")
	public R update(@Valid @RequestBody EventManagement vlsEventManagement) {
		return R.status(vlsEventManagementService.updateById(vlsEventManagement));
	}

	/**
	 * 事件管理表 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入vlsEventManagement")
	public R submit(@Valid @RequestBody EventManagement vlsEventManagement) {
		return R.status(vlsEventManagementService.saveOrUpdate(vlsEventManagement));
	}

	/**
	 * 事件管理表 删除
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(vlsEventManagementService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * 导出数据
	 */
	@GetMapping("/export-vlsEventManagement")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "导出数据", description = "传入vlsEventManagement")
	public void exportVlsEventManagement(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsEventManagement, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<EventManagement> queryWrapper = Condition.getQueryWrapper(vlsEventManagement, EventManagement.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsEventManagementEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsEventManagementEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsEventManagementExcel> list = vlsEventManagementService.exportVlsEventManagement(queryWrapper);
		ExcelUtil.export(response, "事件管理表数据" + DateUtil.today(), "事件管理表数据表", list, VlsEventManagementExcel.class);
	}

	@GetMapping("/{id}")
	public R<EventManagement> getEvent(@PathVariable Long id) {
		EventManagement event = vlsEventManagementService.getEventById(id);
		if (event == null) {
			return R.fail("Event not found");
		}
		return R.data(event);
	}

	@PostMapping
	public R<Boolean> createEvent(@RequestBody EventManagement eventManagement) {
		boolean created = vlsEventManagementService.createEvent(eventManagement);
		if (created) {
			return R.data(true);
		}
		return R.fail("Create event failed");
	}

	@PostMapping(value = "/report", consumes = "multipart/form-data")
	@Operation(summary = "上报事件", description = "Upload image or video and set reportImg")
	public R<Boolean> reportEvent(@Parameter(description = "Event data") @RequestPart("event") EventManagement eventManagement, @Parameter(description = "Image or video file") @RequestPart("file") MultipartFile file) {

		if (eventManagement == null) {
			return R.fail("Event payload is required");
		}
		if (file == null || file.isEmpty()) {
			return R.fail("File is required");
		}
		File localFile = fileUploadService.multipartFileToFile(file);
		if (localFile == null) {
			return R.fail("File convert failed");
		}
		FileResponseDto fileResponseDto = fileUploadService.uploadFile(FILE_UPLOAD_APP_ID, FILE_UPLOAD_SECRET_KEY, localFile);
		if (fileResponseDto == null || StringUtils.isBlank(fileResponseDto.getUrl())) {
			return R.fail("File upload failed");
		}
		eventManagement.setReportImg(fileResponseDto.getUrl());
		boolean created = vlsEventManagementService.createEvent(eventManagement);
		if (created) {
			return R.data(true);
		}
		return R.fail("Event report failed");
	}

}
