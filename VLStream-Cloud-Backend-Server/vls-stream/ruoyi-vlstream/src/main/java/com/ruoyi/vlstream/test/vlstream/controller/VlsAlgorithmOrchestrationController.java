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
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.excel.util.ExcelUtil;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmOrchestrationExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmOrchestration;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmOrchestrationVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAlgorithmOrchestrationService;
import com.ruoyi.vlstream.test.vlstream.wrapper.VlsAlgorithmOrchestrationWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 算法编排表 控制器
 *
 * @author Oort
 * @since 2025-12-23
 */
@RestController
@AllArgsConstructor
@RequestMapping("/vlsAlgorithmOrchestration")
@Tag(name = "算法编排表", description = "算法编排表接口")
public class VlsAlgorithmOrchestrationController extends BladeController {

	private final IVlsAlgorithmOrchestrationService vlsAlgorithmOrchestrationService;

	/**
	 * 算法编排表 详情
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description  = "传入vlsAlgorithmOrchestration")
	public R<AlgorithmOrchestrationVO> detail(AlgorithmOrchestration vlsAlgorithmOrchestration) {
		AlgorithmOrchestration detail = vlsAlgorithmOrchestrationService.getOne(Condition.getQueryWrapper(vlsAlgorithmOrchestration));
		return R.data(VlsAlgorithmOrchestrationWrapper.build().entityVO(detail));
	}

	/**
	 * 算法编排表 分页
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description  = "传入vlsAlgorithmOrchestration")
	public R<IPage<AlgorithmOrchestrationVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAlgorithmOrchestration, Query query) {
		IPage<AlgorithmOrchestration> pages = vlsAlgorithmOrchestrationService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsAlgorithmOrchestration, AlgorithmOrchestration.class));
		return R.data(VlsAlgorithmOrchestrationWrapper.build().pageVO(pages));
	}


	/**
	 * 算法编排表 自定义分页
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description  = "传入vlsAlgorithmOrchestration")
	public R<IPage<AlgorithmOrchestrationVO>> page(AlgorithmOrchestrationVO vlsAlgorithmOrchestration, Query query) {
		IPage<AlgorithmOrchestrationVO> pages = vlsAlgorithmOrchestrationService.selectVlsAlgorithmOrchestrationPage(Condition.getPage(query), vlsAlgorithmOrchestration);
		return R.data(pages);
	}

	/**
	 * 算法编排表 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description  = "传入vlsAlgorithmOrchestration")
	public R save(@Valid @RequestBody AlgorithmOrchestration vlsAlgorithmOrchestration) {
		return R.status(vlsAlgorithmOrchestrationService.save(vlsAlgorithmOrchestration));
	}

	/**
	 * 算法编排表 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description  = "传入vlsAlgorithmOrchestration")
	public R update(@Valid @RequestBody AlgorithmOrchestration vlsAlgorithmOrchestration) {
		return R.status(vlsAlgorithmOrchestrationService.updateById(vlsAlgorithmOrchestration));
	}

	/**
	 * 算法编排表 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description  = "传入vlsAlgorithmOrchestration")
	public R submit(@Valid @RequestBody AlgorithmOrchestration vlsAlgorithmOrchestration) {
		return R.status(vlsAlgorithmOrchestrationService.saveOrUpdate(vlsAlgorithmOrchestration));
	}

	/**
	 * 算法编排表 删除
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description  = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(vlsAlgorithmOrchestrationService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * 导出数据
	 */
	@GetMapping("/export-vlsAlgorithmOrchestration")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "导出数据", description  = "传入vlsAlgorithmOrchestration")
	public void exportVlsAlgorithmOrchestration(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsAlgorithmOrchestration, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<AlgorithmOrchestration> queryWrapper = Condition.getQueryWrapper(vlsAlgorithmOrchestration, AlgorithmOrchestration.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsAlgorithmOrchestrationEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsAlgorithmOrchestrationEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsAlgorithmOrchestrationExcel> list = vlsAlgorithmOrchestrationService.exportVlsAlgorithmOrchestration(queryWrapper);
		ExcelUtil.export(response, "算法编排表数据" + DateUtil.today(), "算法编排表数据表", list, VlsAlgorithmOrchestrationExcel.class);
	}

}
