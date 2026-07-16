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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springblade.core.boot.ctrl.BladeController;
import org.springblade.core.excel.util.ExcelUtil;
import org.springblade.core.mp.support.Condition;
import org.springblade.core.mp.support.Query;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.tool.api.R;
import org.springblade.core.tool.utils.DateUtil;
import org.springblade.core.tool.utils.Func;
import com.ruoyi.vlstream.test.vlstream.excel.VlsVideoRecordExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.VideoRecord;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.VideoRecordVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsVideoRecordService;
import com.ruoyi.vlstream.test.vlstream.wrapper.VlsVideoRecordWrapper;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 视频录制记录表 控制器
 *
 * @author Oort
 * @since 2025-12-25
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/vlsVideoRecord")
@Tag(name = "视频录制记录表（视频回放）", description = "视频录制记录表接口（视频回放）")
public class VlsVideoRecordController extends BladeController {

	private static final long STREAM_CHUNK_SIZE = 1024 * 1024;

	private final IVlsVideoRecordService vlsVideoRecordService;

	/**
	 * 视频录制记录表 详情
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "详情", description = "传入vlsVideoRecord")
	public R<VideoRecordVO> detail(VideoRecord vlsVideoRecord) {
		VideoRecord detail = vlsVideoRecordService.getOne(Condition.getQueryWrapper(vlsVideoRecord));
		return R.data(VlsVideoRecordWrapper.build().entityVO(detail));
	}

	/**
	 * 视频录制记录表 分页
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "分页", description = "传入vlsVideoRecord")
	public R<IPage<VideoRecordVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsVideoRecord, Query query) {
		IPage<VideoRecord> pages = vlsVideoRecordService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsVideoRecord, VideoRecord.class));
		return R.data(VlsVideoRecordWrapper.build().pageVO(pages));
	}


	/**
	 * 视频录制记录表 自定义分页
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "分页", description = "传入vlsVideoRecord")
	public R<IPage<VideoRecordVO>> page(VideoRecordVO vlsVideoRecord, Query query) {
		IPage<VideoRecordVO> pages = vlsVideoRecordService.selectVlsVideoRecordPage(Condition.getPage(query), vlsVideoRecord);
		return R.data(pages);
	}

	/**
	 * 视频回放查询
	 */
	@GetMapping("/playback")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "视频回放查询", description = "传入deviceId与时间段")
	public R<List<VideoRecordVO>> playback(@RequestParam Long deviceId,
										   @RequestParam @DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME) LocalDateTime startTime,
										   @RequestParam @DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME) LocalDateTime endTime) {
		List<VideoRecord> records = vlsVideoRecordService.listPlaybackRecords(deviceId, startTime, endTime);
		return R.data(VlsVideoRecordWrapper.build().listVO(records));
	}

	/**
	 * 时间轴日历查询
	 */
	@GetMapping("/timeline/calendar")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "时间轴日历", description = "按设备与年份返回有录像的月份/日期")
	public R<Map<Integer, List<Integer>>> timelineCalendar(@RequestParam Long deviceId, @RequestParam Integer year) {
		List<LocalDate> recordDateList = vlsVideoRecordService.listRecordDates(deviceId, year);
		Map<Integer, List<Integer>> monthDayMap = recordDateList.stream()
			.collect(Collectors.groupingBy(LocalDate::getMonthValue,
				TreeMap::new,
				Collectors.mapping(LocalDate::getDayOfMonth, Collectors.toList())));
		monthDayMap.replaceAll((month, dayList) -> dayList.stream().distinct().sorted().collect(java.util.stream.Collectors.toList()));
		return R.data(monthDayMap);
	}

	/**
	 * 时间轴某天录像列表
	 */
	@GetMapping("/timeline/day")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "时间轴当天录像", description = "按设备与日期返回录像列表")
	public R<List<VideoRecordVO>> timelineDay(@RequestParam Long deviceId, @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate recordDate) {
		List<VideoRecord> records = vlsVideoRecordService.listDayRecords(deviceId, recordDate);
		return R.data(VlsVideoRecordWrapper.build().listVO(records));
	}

	/**
	 * 视频文件流播放
	 */
	@GetMapping(value = "/stream/{recordId}")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "视频流播放", description = "根据录像ID播放本地文件，支持Range")
	public ResponseEntity<?> stream(@PathVariable Long recordId, @RequestHeader HttpHeaders requestHeaders) throws IOException {
		VideoRecord videoRecord = vlsVideoRecordService.getById(recordId);
		if (videoRecord == null || StringUtils.isBlank(videoRecord.getFilePath())) {
			return ResponseEntity.notFound().build();
		}
		Resource videoResource = new FileSystemResource(videoRecord.getFilePath());
		if (!videoResource.exists() || !videoResource.isReadable()) {
			log.error("文件不存在：{}", videoRecord.getFilePath());
			return ResponseEntity.notFound().build();
		}

		long contentLength = videoResource.contentLength();
		if (contentLength <= 0) {
			return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
		}

		MediaType mediaType = MediaTypeFactory.getMediaType(videoResource)
			.orElse(MediaType.APPLICATION_OCTET_STREAM);
		List<HttpRange> rangeList = requestHeaders.getRange();
		if (rangeList.isEmpty()) {
			return ResponseEntity.ok()
				.contentType(mediaType)
				.contentLength(contentLength)
				.header(HttpHeaders.ACCEPT_RANGES, "bytes")
				.body(videoResource);
		}
		ResourceRegion resourceRegion = buildResourceRegion(videoResource, requestHeaders, contentLength);
		return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
			.contentType(mediaType)
			.header(HttpHeaders.ACCEPT_RANGES, "bytes")
			.body(resourceRegion);
	}

	private ResourceRegion buildResourceRegion(Resource videoResource,
											   HttpHeaders requestHeaders,
											   long contentLength) {
		List<HttpRange> rangeList = requestHeaders.getRange();
		if (rangeList.isEmpty()) {
			long regionLength = Math.min(STREAM_CHUNK_SIZE, contentLength);
			return new ResourceRegion(videoResource, 0, regionLength);
		}
		HttpRange httpRange = rangeList.get(0);
		long startPosition = httpRange.getRangeStart(contentLength);
		long endPosition = httpRange.getRangeEnd(contentLength);
		long requestedRangeLength = endPosition - startPosition + 1;
		long regionLength = Math.min(STREAM_CHUNK_SIZE, requestedRangeLength);
		return new ResourceRegion(videoResource, startPosition, regionLength);
	}

	/**
	 * 视频录制记录表 新增
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "新增", description = "传入vlsVideoRecord")
	public R save(@Valid @RequestBody VideoRecord vlsVideoRecord) {
		return R.status(vlsVideoRecordService.save(vlsVideoRecord));
	}

	/**
	 * 视频录制记录表 修改
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "修改", description = "传入vlsVideoRecord")
	public R update(@Valid @RequestBody VideoRecord vlsVideoRecord) {
		return R.status(vlsVideoRecordService.updateById(vlsVideoRecord));
	}

	/**
	 * 视频录制记录表 新增或修改
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 10)
	@Operation(summary = "新增或修改", description = "传入vlsVideoRecord")
	public R submit(@Valid @RequestBody VideoRecord vlsVideoRecord) {
		return R.status(vlsVideoRecordService.saveOrUpdate(vlsVideoRecord));
	}

	/**
	 * 视频录制记录表 删除
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 11)
	@Operation(summary = "逻辑删除", description = "传入ids")
	public R remove(@Parameter(description = "主键集合", required = true) @RequestParam String ids) {
		return R.status(vlsVideoRecordService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * 导出数据
	 */
	@GetMapping("/export-vlsVideoRecord")
	@ApiOperationSupport(order = 12)
	@Operation(summary = "导出数据", description = "传入vlsVideoRecord")
	public void exportVlsVideoRecord(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsVideoRecord, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<VideoRecord> queryWrapper = Condition.getQueryWrapper(vlsVideoRecord, VideoRecord.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsVideoRecordEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsVideoRecordEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsVideoRecordExcel> list = vlsVideoRecordService.exportVlsVideoRecord(queryWrapper);
		ExcelUtil.export(response, "视频录制记录表数据" + DateUtil.today(), "视频录制记录表数据表", list, VlsVideoRecordExcel.class);
	}

}
