package com.ruoyi.vlstream.test.vlstream.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.AllArgsConstructor;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.tool.api.R;
import com.ruoyi.vlstream.test.vlstream.pojo.dto.*;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.CameraApplyRecordVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsCameraApplyRecordService;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/vlsCameraApply")
@Tag(name = "摄像头申请", description = "摄像头申请表接口")
public class VlsCameraApplyController extends BladeController {

	private final IVlsCameraApplyRecordService vlsCameraApplyRecordService;

	/**
	 * 摄像头使用申请提交
	 */
	@PostMapping("/camera-apply/submit")
	@ApiOperationSupport(order = 31)
	@Operation(summary = "摄像头使用申请", description = "提交摄像头申请记录")
	public R<Boolean> submitCameraApply(@Valid @RequestBody CameraApplySubmitDTO cameraApplySubmitDTO) {
		return R.data(vlsCameraApplyRecordService.submit(cameraApplySubmitDTO));
	}

	/**
	 * 摄像头使用申请审批通过
	 */
	@PostMapping("/camera-apply/approve")
	@ApiOperationSupport(order = 32)
	@Operation(summary = "摄像头申请审批通过", description = "审批通过后状态流转为approved")
	public R<Boolean> approveCameraApply(@Valid @RequestBody CameraApplyApproveDTO cameraApplyApproveDTO) {
		return R.data(vlsCameraApplyRecordService.approve(cameraApplyApproveDTO));
	}

	/**
	 * 摄像头使用申请审批驳回
	 */
	@PostMapping("/camera-apply/reject")
	@ApiOperationSupport(order = 33)
	@Operation(summary = "摄像头申请审批驳回", description = "驳回后状态流转为rejected")
	public R<Boolean> rejectCameraApply(@Valid @RequestBody CameraApplyRejectDTO cameraApplyRejectDTO) {
		return R.data(vlsCameraApplyRecordService.reject(cameraApplyRejectDTO));
	}

	/**
	 * 摄像头使用申请完结
	 */
	@PostMapping("/camera-apply/complete")
	@ApiOperationSupport(order = 34)
	@Operation(summary = "摄像头申请完结", description = "仅approved状态可完结为completed")
	public R<Boolean> completeCameraApply(@Valid @RequestBody CameraApplyCompleteDTO cameraApplyCompleteDTO) {
		return R.data(vlsCameraApplyRecordService.complete(cameraApplyCompleteDTO));
	}

	/**
	 * 摄像头使用申请分页
	 */
	@GetMapping("/camera-apply/page")
	@ApiOperationSupport(order = 35)
	@Operation(summary = "摄像头申请分页", description = "按设备、状态、申请人分页查询")
	public R<IPage<CameraApplyRecordVO>> pageCameraApply(CameraApplyQueryDTO cameraApplyQueryDTO, Query query) {
		IPage<CameraApplyRecordVO> pages = vlsCameraApplyRecordService.page(Condition.getPage(query), cameraApplyQueryDTO);
		return R.data(pages);
	}

}
