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
import org.springblade.vlstream.excel.VlsAlgorithmOrchestrationExcel;
import org.springblade.vlstream.pojo.entity.AlgorithmOrchestration;
import org.springblade.vlstream.pojo.vo.AlgorithmOrchestrationVO;
import org.springblade.vlstream.service.IVlsAlgorithmOrchestrationService;
import org.springblade.vlstream.wrapper.VlsAlgorithmOrchestrationWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Algorithm orchestration table controller
 *
 * @author Oort
 * @since 2025-12-23
 */
@RestController
@AllArgsConstructor
@RequestMapping("/vlsAlgorithmOrchestration")
@Tag(name = "Algorithm orchestration table", description = "Algorithm orchestration table interface")
public class VlsAlgorithmOrchestrationController extends BladeController {

	private final IVlsAlgorithmOrchestrationService vlsAlgorithmOrchestrationService;

	/**
	 * Algorithm orchestration table details
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "Details", description  = "Pass in vlsAlgorithmOrchestration")
	public R<AlgorithmOrchestrationVO> detail(AlgorithmOrchestration vlsAlgorithmOrchestration) {
		AlgorithmOrchestration detail = vlsAlgorithmOrchestrationService.getOne(Condition.getQueryWrapper(vlsAlgorithmOrchestration));
		return R.data(VlsAlgorithmOrchestrationWrapper.build().entityVO(detail));
	}

	/**
	 * Algorithm orchestration table paging
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "Pagination", description  = "Pass in vlsAlgorithmOrchestration")
	public R<IPage<AlgorithmOrchestrationVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAlgorithmOrchestration, Query query) {
		IPage<AlgorithmOrchestration> pages = vlsAlgorithmOrchestrationService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsAlgorithmOrchestration, AlgorithmOrchestration.class));
		return R.data(VlsAlgorithmOrchestrationWrapper.build().pageVO(pages));
	}


	/**
	 * Algorithm orchestration table custom paging
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "Pagination", description  = "Pass in vlsAlgorithmOrchestration")
	public R<IPage<AlgorithmOrchestrationVO>> page(AlgorithmOrchestrationVO vlsAlgorithmOrchestration, Query query) {
		IPage<AlgorithmOrchestrationVO> pages = vlsAlgorithmOrchestrationService.selectVlsAlgorithmOrchestrationPage(Condition.getPage(query), vlsAlgorithmOrchestration);
		return R.data(pages);
	}

	/**
	 * Add algorithm orchestration table
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "Add", description  = "Pass in vlsAlgorithmOrchestration")
	public R save(@Valid @RequestBody AlgorithmOrchestration vlsAlgorithmOrchestration) {
		return R.status(vlsAlgorithmOrchestrationService.save(vlsAlgorithmOrchestration));
	}

	/**
	 * Modify algorithm orchestration table
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "Modify", description  = "Pass in vlsAlgorithmOrchestration")
	public R update(@Valid @RequestBody AlgorithmOrchestration vlsAlgorithmOrchestration) {
		return R.status(vlsAlgorithmOrchestrationService.updateById(vlsAlgorithmOrchestration));
	}

	/**
	 * Add or modify algorithm orchestration table
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "Add or modify", description  = "Pass in vlsAlgorithmOrchestration")
	public R submit(@Valid @RequestBody AlgorithmOrchestration vlsAlgorithmOrchestration) {
		return R.status(vlsAlgorithmOrchestrationService.saveOrUpdate(vlsAlgorithmOrchestration));
	}

	/**
	 * Delete algorithm orchestration table
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "Logical delete", description  = "Pass in ids")
	public R remove(@Parameter(description = "Primary key collection", required = true) @RequestParam String ids) {
		return R.status(vlsAlgorithmOrchestrationService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * Export data
	 */
	@GetMapping("/export-vlsAlgorithmOrchestration")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "Export data", description  = "Pass in vlsAlgorithmOrchestration")
	public void exportVlsAlgorithmOrchestration(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAlgorithmOrchestration, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<AlgorithmOrchestration> queryWrapper = Condition.getQueryWrapper(vlsAlgorithmOrchestration, AlgorithmOrchestration.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsAlgorithmOrchestrationEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsAlgorithmOrchestrationEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsAlgorithmOrchestrationExcel> list = vlsAlgorithmOrchestrationService.exportVlsAlgorithmOrchestration(queryWrapper);
		ExcelUtil.export(response, "Algorithm orchestration table data" + DateUtil.today(), "Algorithm orchestration table database table", list, VlsAlgorithmOrchestrationExcel.class);
	}

}
