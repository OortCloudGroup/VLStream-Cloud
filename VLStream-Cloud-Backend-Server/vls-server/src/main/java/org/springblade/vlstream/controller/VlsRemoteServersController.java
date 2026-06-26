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
import org.springblade.vlstream.excel.VlsRemoteServersExcel;
import org.springblade.vlstream.pojo.entity.RemoteServers;
import org.springblade.vlstream.pojo.vo.RemoteServersVO;
import org.springblade.vlstream.service.IVlsRemoteServersService;
import org.springblade.vlstream.wrapper.VlsRemoteServersWrapper;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Remote Server Configuration Table Controller
 *
 * @author Oort
 * @since 2025-12-23
 */
@RestController
@AllArgsConstructor
@RequestMapping("/vlsRemoteServers")
@Tag(name = "Remote Server Configuration Table", description = "Remote Server Configuration Table Interface")
public class VlsRemoteServersController extends BladeController {

	private final IVlsRemoteServersService vlsRemoteServersService;

	/**
	 * Remote Server Configuration Table Details
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "Details", description  = "Pass in vlsRemoteServers")
	public R<RemoteServersVO> detail(RemoteServers vlsRemoteServers) {
		RemoteServers detail = vlsRemoteServersService.getOne(Condition.getQueryWrapper(vlsRemoteServers));
		return R.data(VlsRemoteServersWrapper.build().entityVO(detail));
	}

	/**
	 * Remote Server Configuration Table Pagination
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "Pagination", description  = "Pass in vlsRemoteServers")
	public R<IPage<RemoteServersVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsRemoteServers, Query query) {
		IPage<RemoteServers> pages = vlsRemoteServersService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsRemoteServers, RemoteServers.class));
		return R.data(VlsRemoteServersWrapper.build().pageVO(pages));
	}


	/**
	 * Remote Server Configuration Table Custom Pagination
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "Pagination", description  = "Pass in vlsRemoteServers")
	public R<IPage<RemoteServersVO>> page(RemoteServersVO vlsRemoteServers, Query query) {
		IPage<RemoteServersVO> pages = vlsRemoteServersService.selectVlsRemoteServersPage(Condition.getPage(query), vlsRemoteServers);
		return R.data(pages);
	}

	/**
	 * Remote Server Configuration Table Add
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "Add", description  = "Pass in vlsRemoteServers")
	public R save(@Valid @RequestBody RemoteServers vlsRemoteServers) {
		return R.status(vlsRemoteServersService.save(vlsRemoteServers));
	}

	/**
	 * Remote Server Configuration Table Modify
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "Modify", description  = "Pass in vlsRemoteServers")
	public R update(@Valid @RequestBody RemoteServers vlsRemoteServers) {
		return R.status(vlsRemoteServersService.updateById(vlsRemoteServers));
	}

	/**
	 * Remote Server Configuration Table Add or Modify
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "Add or modify", description  = "Pass in vlsRemoteServers")
	public R submit(@Valid @RequestBody RemoteServers vlsRemoteServers) {
		return R.status(vlsRemoteServersService.saveOrUpdate(vlsRemoteServers));
	}

	/**
	 * Remote Server Configuration Table Delete
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "Logical delete", description  = "Pass in ids")
	public R remove(@Parameter(description = "Primary key collection", required = true) @RequestParam String ids) {
		return R.status(vlsRemoteServersService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * Export data
	 */
	@GetMapping("/export-vlsRemoteServers")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "Export data", description  = "Pass in vlsRemoteServers")
	public void exportVlsRemoteServers(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsRemoteServers, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<RemoteServers> queryWrapper = Condition.getQueryWrapper(vlsRemoteServers, RemoteServers.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsRemoteServersEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsRemoteServersEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsRemoteServersExcel> list = vlsRemoteServersService.exportVlsRemoteServers(queryWrapper);
		ExcelUtil.export(response, "Remote Server Configuration Table Data" + DateUtil.today(), "Remote Server Configuration Table Data Table", list, VlsRemoteServersExcel.class);
	}

}
