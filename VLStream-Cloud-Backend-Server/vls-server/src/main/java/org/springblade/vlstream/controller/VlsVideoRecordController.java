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
import org.springblade.vlstream.excel.VlsVideoRecordExcel;
import org.springblade.vlstream.pojo.entity.VideoRecord;
import org.springblade.vlstream.pojo.vo.VideoRecordVO;
import org.springblade.vlstream.service.IVlsVideoRecordService;
import org.springblade.vlstream.wrapper.VlsVideoRecordWrapper;
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
 * Video Recording Record Table Controller
 *
 * @author Oort
 * @since 2025-12-25
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/vlsVideoRecord")
@Tag(name = "Video Recording Record Table (Video Playback)", description = "Video Recording Record Table Interface (Video Playback)")
public class VlsVideoRecordController extends BladeController {

	private static final long STREAM_CHUNK_SIZE = 1024 * 1024;

	private final IVlsVideoRecordService vlsVideoRecordService;

	/**
	 * Video Recording Record Table Details
	 */
	@GetMapping("/detail")
	@ApiOperationSupport(order = 1)
	@Operation(summary = "Details", description = "Pass in vlsVideoRecord")
	public R<VideoRecordVO> detail(VideoRecord vlsVideoRecord) {
		VideoRecord detail = vlsVideoRecordService.getOne(Condition.getQueryWrapper(vlsVideoRecord));
		return R.data(VlsVideoRecordWrapper.build().entityVO(detail));
	}

	/**
	 * Video Recording Record Table Pagination
	 */
	@GetMapping("/list")
	@ApiOperationSupport(order = 2)
	@Operation(summary = "Pagination", description = "Pass in vlsVideoRecord")
	public R<IPage<VideoRecordVO>> list(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsVideoRecord, Query query) {
		IPage<VideoRecord> pages = vlsVideoRecordService.page(Condition.getPage(query), Condition.getQueryWrapper(vlsVideoRecord, VideoRecord.class));
		return R.data(VlsVideoRecordWrapper.build().pageVO(pages));
	}


	/**
	 * Video Recording Record Table Custom Pagination
	 */
	@GetMapping("/page")
	@ApiOperationSupport(order = 3)
	@Operation(summary = "Pagination", description = "Pass in vlsVideoRecord")
	public R<IPage<VideoRecordVO>> page(VideoRecordVO vlsVideoRecord, Query query) {
		IPage<VideoRecordVO> pages = vlsVideoRecordService.selectVlsVideoRecordPage(Condition.getPage(query), vlsVideoRecord);
		return R.data(pages);
	}

	/**
	 * Video playback query
	 */
	@GetMapping("/playback")
	@ApiOperationSupport(order = 4)
	@Operation(summary = "Video playback query", description = "Pass in deviceId and time period")
	public R<List<VideoRecordVO>> playback(@RequestParam Long deviceId,
										   @RequestParam @DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME) LocalDateTime startTime,
										   @RequestParam @DateTimeFormat(pattern = DateUtil.PATTERN_DATETIME) LocalDateTime endTime) {
		List<VideoRecord> records = vlsVideoRecordService.listPlaybackRecords(deviceId, startTime, endTime);
		return R.data(VlsVideoRecordWrapper.build().listVO(records));
	}

	/**
	 * Timeline calendar query
	 */
	@GetMapping("/timeline/calendar")
	@ApiOperationSupport(order = 5)
	@Operation(summary = "Timeline calendar", description = "Return months/dates with recordings by device and year")
	public R<Map<Integer, List<Integer>>> timelineCalendar(@RequestParam Long deviceId, @RequestParam Integer year) {
		List<LocalDate> recordDateList = vlsVideoRecordService.listRecordDates(deviceId, year);
		Map<Integer, List<Integer>> monthDayMap = recordDateList.stream()
			.collect(Collectors.groupingBy(LocalDate::getMonthValue,
				TreeMap::new,
				Collectors.mapping(LocalDate::getDayOfMonth, Collectors.toList())));
		monthDayMap.replaceAll((month, dayList) -> dayList.stream().distinct().sorted().toList());
		return R.data(monthDayMap);
	}

	/**
	 * Timeline recording list of a specific day
	 */
	@GetMapping("/timeline/day")
	@ApiOperationSupport(order = 6)
	@Operation(summary = "Timeline recording of the day", description = "Return video list by device and date")
	public R<List<VideoRecordVO>> timelineDay(@RequestParam Long deviceId, @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate recordDate) {
		List<VideoRecord> records = vlsVideoRecordService.listDayRecords(deviceId, recordDate);
		return R.data(VlsVideoRecordWrapper.build().listVO(records));
	}

	/**
	 * Video File Stream Playback
	 */
	@GetMapping(value = "/stream/{recordId}")
	@ApiOperationSupport(order = 7)
	@Operation(summary = "Video Stream Playback", description = "Play local file by recording ID, support Range")
	public ResponseEntity<?> stream(@PathVariable Long recordId, @RequestHeader HttpHeaders requestHeaders) throws IOException {
		VideoRecord videoRecord = vlsVideoRecordService.getById(recordId);
		if (videoRecord == null || StringUtils.isBlank(videoRecord.getFilePath())) {
			return ResponseEntity.notFound().build();
		}
		Resource videoResource = new FileSystemResource(videoRecord.getFilePath());
		if (!videoResource.exists() || !videoResource.isReadable()) {
			log.error("File does not exist: {}", videoRecord.getFilePath());
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
	 * Video Recording Record Table Add
	 */
	@PostMapping("/save")
	@ApiOperationSupport(order = 8)
	@Operation(summary = "Add", description = "Pass in vlsVideoRecord")
	public R save(@Valid @RequestBody VideoRecord vlsVideoRecord) {
		return R.status(vlsVideoRecordService.save(vlsVideoRecord));
	}

	/**
	 * Modify video recording record table
	 */
	@PostMapping("/update")
	@ApiOperationSupport(order = 9)
	@Operation(summary = "Modify", description = "Pass in vlsVideoRecord")
	public R update(@Valid @RequestBody VideoRecord vlsVideoRecord) {
		return R.status(vlsVideoRecordService.updateById(vlsVideoRecord));
	}

	/**
	 * Video Recording Record Table Add or Modify
	 */
	@PostMapping("/submit")
	@ApiOperationSupport(order = 10)
	@Operation(summary = "Add or modify", description = "Pass in vlsVideoRecord")
	public R submit(@Valid @RequestBody VideoRecord vlsVideoRecord) {
		return R.status(vlsVideoRecordService.saveOrUpdate(vlsVideoRecord));
	}

	/**
	 * Video Recording Record Table Delete
	 */
	@GetMapping("/remove")
	@ApiOperationSupport(order = 11)
	@Operation(summary = "Logical delete", description = "Pass in ids")
	public R remove(@Parameter(description = "Primary key collection", required = true) @RequestParam String ids) {
		return R.status(vlsVideoRecordService.deleteLogic(Func.toLongList(ids)));
	}

	/**
	 * Export data
	 */
	@GetMapping("/export-vlsVideoRecord")
	@ApiOperationSupport(order = 12)
	@Operation(summary = "Export data", description = "Pass in vlsVideoRecord")
	public void exportVlsVideoRecord(@Parameter(hidden = true) @RequestParam Map<String, Object> vlsVideoRecord, BladeUser bladeUser, HttpServletResponse response) {
		QueryWrapper<VideoRecord> queryWrapper = Condition.getQueryWrapper(vlsVideoRecord, VideoRecord.class);
		//if (!AuthUtil.isAdministrator()) {
		//	queryWrapper.lambda().eq(VlsVideoRecordEntity::getTenantId, bladeUser.getTenantId());
		//}
		//queryWrapper.lambda().eq(VlsVideoRecordEntity::getIsDeleted, BladeConstant.DB_NOT_DELETED);
		List<VlsVideoRecordExcel> list = vlsVideoRecordService.exportVlsVideoRecord(queryWrapper);
		ExcelUtil.export(response, "Video Recording Record Table Data" + DateUtil.today(), "Video Recording Record Table Data Table", list, VlsVideoRecordExcel.class);
	}

}
