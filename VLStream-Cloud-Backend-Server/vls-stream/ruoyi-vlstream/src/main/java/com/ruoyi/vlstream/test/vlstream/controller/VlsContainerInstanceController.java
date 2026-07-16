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
import com.ruoyi.vlstream.test.vlstream.excel.VlsContainerInstanceExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ContainerInstance;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.ContainerInstanceVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsContainerInstanceService;
import com.ruoyi.vlstream.test.vlstream.wrapper.VlsContainerInstanceWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 容器实例表 控制器
 *
 * @author Oort
 * @since 2025-12-23
 */
@RestController
@AllArgsConstructor
@RequestMapping("/vlsContainerInstance")
@Tag(name = "容器实例表", description = "容器实例表接口")
public class VlsContainerInstanceController extends BladeController {

	private final IVlsContainerInstanceService vlsContainerInstanceService;

	/**
	 * 容器实例表 详情
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description  = "传入vlsContainerInstance")
	public R<ContainerInstanceVO> detail(ContainerInstance vlsContainerInstance) {
		ContainerInstance detail = vlsContainerInstanceService.getOne(Condition.getQueryWrapper(vlsContainerInstance));
		return R.data(VlsContainerInstanceWrapper.build().entityVO(detail));
	}

	/**
	 * 容器实例表 分页
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description  = "传入vlsContainerInstance")
	public R<IPage<ContainerInstanceVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsContainerInstance, Query query) {
		IPage<ContainerInstance> pages = vlsContainerInstanceService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsContainerInstance, ContainerInstance.class));
		return R.data(VlsContainerInstanceWrapper.build().pageVO(pages));
	}


	/**
	 * 容器实例表 自定义分页
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description  = "传入vlsContainerInstance")
	public R<IPage<ContainerInstanceVO>> page(ContainerInstanceVO vlsContainerInstance, Query query) {
		IPage<ContainerInstanceVO> pages = vlsContainerInstanceService.selectVlsContainerInstancePage(Condition.getPage(query), vlsContainerInstance);
		return R.data(pages);
	}

	/**
	 * 容器实例表 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "新增", description  = "传入vlsContainerInstance")
	public R save(@Valid @RequestBody ContainerInstance vlsContainerInstance) {
		return R.status(vlsContainerInstanceService.save(vlsContainerInstance));
	}

	/**
	 * 容器实例表 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "修改", description  = "传入vlsContainerInstance")
	public R update(@Valid @RequestBody ContainerInstance vlsContainerInstance) {
		return R.status(vlsContainerInstanceService.updateById(vlsContainerInstance));
	}

	/**
	 * 容器实例表 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "新增或修改", description  = "传入vlsContainerInstance")
	public R submit(@Valid @RequestBody ContainerInstance vlsContainerInstance) {
		return R.status(vlsContainerInstanceService.saveOrUpdate(vlsContainerInstance));
	}

	/**
	 * 容器实例表 删除
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "逻辑删除", description  = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(vlsContainerInstanceService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * 导出数据
	 */
	@GetMapping("/export-vlsContainerInstance")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "导出数据", description  = "传入vlsContainerInstance")
	public void exportVlsContainerInstance(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsContainerInstance, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<ContainerInstance> queryWrapper = Condition.getQueryWrapper(vlsContainerInstance, ContainerInstance.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsContainerInstanceEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsContainerInstanceEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsContainerInstanceExcel> list = vlsContainerInstanceService.exportVlsContainerInstance(queryWrapper);
		ExcelUtil.export(response, "容器实例表数据" + DateUtil.today(), "容器实例表数据表", list, VlsContainerInstanceExcel.class);
	}

}
