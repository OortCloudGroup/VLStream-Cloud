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
import com.ruoyi.vlstream.test.vlstream.excel.VlsSceneGovernanceExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.SceneGovernance;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.SceneGovernanceVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsSceneGovernanceService;
import com.ruoyi.vlstream.test.vlstream.wrapper.VlsSceneGovernanceWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 场景治理表 控制器
 *
 * @author Oort
 * @since 2025-12-23
 */
@RestController
@AllArgsConstructor
@RequestMapping("/vlsSceneGovernance")
@Tag(name = "场景治理表", description = "场景治理表接口")
public class VlsSceneGovernanceController extends BladeController {

	private final IVlsSceneGovernanceService vlsSceneGovernanceService;

	/**
	 * 场景治理表 详情
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入vlsSceneGovernance")
	public R<SceneGovernanceVO> detail(SceneGovernance vlsSceneGovernance) {
		SceneGovernance detail = vlsSceneGovernanceService.getOne(Condition.getQueryWrapper(vlsSceneGovernance));
		return R.data(VlsSceneGovernanceWrapper.build().entityVO(detail));
	}

	/**
	 * 场景治理表 分页
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入vlsSceneGovernance")
	public R<IPage<SceneGovernanceVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsSceneGovernance, Query query) {
		IPage<SceneGovernance> pages = vlsSceneGovernanceService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsSceneGovernance, SceneGovernance.class));
		return R.data(VlsSceneGovernanceWrapper.build().pageVO(pages));
	}


	/**
	 * 场景治理表 自定义分页
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入vlsSceneGovernance")
	public R<IPage<SceneGovernanceVO>> page(SceneGovernanceVO vlsSceneGovernance, Query query) {
		IPage<SceneGovernanceVO> pages = vlsSceneGovernanceService.selectVlsSceneGovernancePage(Condition.getPage(query), vlsSceneGovernance);
		return R.data(pages);
	}

	/**
	 * 场景治理表 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description = "传入vlsSceneGovernance")
	public R save(@Valid @RequestBody SceneGovernance vlsSceneGovernance) {
		return R.status(vlsSceneGovernanceService.saveAndSchedule(vlsSceneGovernance));
	}

	/**
	 * 场景治理表 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description = "传入vlsSceneGovernance")
	public R update(@Valid @RequestBody SceneGovernance vlsSceneGovernance) {
		return R.status(vlsSceneGovernanceService.updateAndSchedule(vlsSceneGovernance));
	}

	/**
	 * 场景治理表 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description = "传入vlsSceneGovernance")
	public R submit(@Valid @RequestBody SceneGovernance vlsSceneGovernance) {
		return R.status(vlsSceneGovernanceService.submitAndSchedule(vlsSceneGovernance));
	}

	/**
	 * 场景治理表 删除
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(vlsSceneGovernanceService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * 导出数据
	 */
	@GetMapping("/export-vlsSceneGovernance")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "导出数据", description = "传入vlsSceneGovernance")
	public void exportVlsSceneGovernance(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsSceneGovernance, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<SceneGovernance> queryWrapper = Condition.getQueryWrapper(vlsSceneGovernance, SceneGovernance.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsSceneGovernanceEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsSceneGovernanceEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsSceneGovernanceExcel> list = vlsSceneGovernanceService.exportVlsSceneGovernance(queryWrapper);
		ExcelUtil.export(response, "场景治理表数据" + DateUtil.today(), "场景治理表数据表", list, VlsSceneGovernanceExcel.class);
	}

}
