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
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.excel.util.ExcelUtil;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import org.springblade.vlstream.excel.VlsSceneGovernanceExcel;
import org.springblade.vlstream.pojo.entity.SceneGovernance;
import org.springblade.vlstream.pojo.vo.SceneGovernanceVO;
import org.springblade.vlstream.service.IVlsSceneGovernanceService;
import org.springblade.vlstream.wrapper.VlsSceneGovernanceWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Scene governance table controller
 *
 * @author Oort
 * @since 2025-12-23
 */
@RestController
@AllArgsConstructor
@RequestMapping("/vlsSceneGovernance")
@Tag(name = "Scene governance table", description = "Scene governance table interface")
public class VlsSceneGovernanceController extends BladeController {

	private final IVlsSceneGovernanceService vlsSceneGovernanceService;

	/**
	 * Scene governance table details
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "Details", description = "Pass in vlsSceneGovernance")
	public R<SceneGovernanceVO> detail(SceneGovernance vlsSceneGovernance) {
		SceneGovernance detail = vlsSceneGovernanceService.getOne(Condition.getQueryWrapper(vlsSceneGovernance));
		return R.data(VlsSceneGovernanceWrapper.build().entityVO(detail));
	}

	/**
	 * Page scene governance table
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "Pagination", description = "Pass in vlsSceneGovernance")
	public R<IPage<SceneGovernanceVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsSceneGovernance, Query query) {
		IPage<SceneGovernance> pages = vlsSceneGovernanceService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsSceneGovernance, SceneGovernance.class));
		return R.data(VlsSceneGovernanceWrapper.build().pageVO(pages));
	}


	/**
	 * Scene governance table custom paging
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "Pagination", description = "Pass in vlsSceneGovernance")
	public R<IPage<SceneGovernanceVO>> page(SceneGovernanceVO vlsSceneGovernance, Query query) {
		IPage<SceneGovernanceVO> pages = vlsSceneGovernanceService.selectVlsSceneGovernancePage(Condition.getPage(query), vlsSceneGovernance);
		return R.data(pages);
	}

	/**
	 * Add scene governance table
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "Add", description = "Pass in vlsSceneGovernance")
	public R save(@Valid @RequestBody SceneGovernance vlsSceneGovernance) {
		return R.status(vlsSceneGovernanceService.saveAndSchedule(vlsSceneGovernance));
	}

	/**
	 * Modify scene governance table
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "Modify", description = "Pass in vlsSceneGovernance")
	public R update(@Valid @RequestBody SceneGovernance vlsSceneGovernance) {
		return R.status(vlsSceneGovernanceService.updateAndSchedule(vlsSceneGovernance));
	}

	/**
	 * Add or modify scene governance table
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "Add or modify", description = "Pass in vlsSceneGovernance")
	public R submit(@Valid @RequestBody SceneGovernance vlsSceneGovernance) {
		return R.status(vlsSceneGovernanceService.submitAndSchedule(vlsSceneGovernance));
	}

	/**
	 * Delete scene governance table
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "Logical delete", description = "Pass in ids")
	public R remove(@Parameter(description = "Primary key collection", required = true) @RequestParam String ids) {
		return R.status(vlsSceneGovernanceService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * Export data
	 */
	@GetMapping("/export-vlsSceneGovernance")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "Export data", description = "Pass in vlsSceneGovernance")
	public void exportVlsSceneGovernance(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsSceneGovernance, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<SceneGovernance> queryWrapper = Condition.getQueryWrapper(vlsSceneGovernance, SceneGovernance.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsSceneGovernanceEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsSceneGovernanceEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsSceneGovernanceExcel> list = vlsSceneGovernanceService.exportVlsSceneGovernance(queryWrapper);
		ExcelUtil.export(response, "Scene governance table data" + DateUtil.today(), "Scene governance table database table", list, VlsSceneGovernanceExcel.class);
	}

}
