/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.vlstream.domain.VideoRecord;
import com.ruoyi.vlstream.service.IVlsVideoRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * SpringBlade-compatible video record routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsVideoRecord")
public class VlsVideoRecordController extends VlsControllerSupport {

    private static final long STREAM_CHUNK_SIZE = 1024L * 1024L;

    private final IVlsVideoRecordService videoRecordService;

    @GetMapping("/page")
    public BladeResult<BladePage<VideoRecord>> page(@RequestParam(value = "current", required = false) Long current,
                                                    @RequestParam(value = "page", required = false) Long page,
                                                    @RequestParam(value = "size", required = false) Long size,
                                                    @RequestParam(value = "pageSize", required = false) Long pageSize,
                                                    @RequestParam(required = false) Long deviceId,
                                                    @RequestParam(required = false) String deviceName,
                                                    @RequestParam(required = false) String fileName,
                                                    @RequestParam(required = false) String recordStatus,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) String date) {
        long resolvedCurrent = current == null ? (page == null ? 1L : page) : current;
        long resolvedSize = size == null ? (pageSize == null ? 10L : pageSize) : size;
        String resolvedStatus = StringUtils.hasText(recordStatus) ? recordStatus : status;
        return BladeResult.success(videoRecordService.getRecordPage(
            resolvedCurrent, resolvedSize, deviceId, deviceName, fileName, resolvedStatus, date));
    }

    @GetMapping("/{id}")
    public BladeResult<VideoRecord> getRecord(@PathVariable Long id) {
        return BladeResult.success(videoRecordService.getRecord(id));
    }

    @PostMapping
    public BladeResult<VideoRecord> createRecord(@RequestBody VideoRecord videoRecord) {
        try {
            return BladeResult.success(videoRecordService.createRecord(videoRecord));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    public BladeResult<VideoRecord> updateRecord(@PathVariable Long id, @RequestBody VideoRecord videoRecord) {
        try {
            return BladeResult.success(videoRecordService.updateRecord(id, videoRecord));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public BladeResult<Void> deleteRecord(@PathVariable Long id) {
        return booleanResult(videoRecordService.deleteRecord(id), "Delete video record failed");
    }

    @DeleteMapping("/batch")
    public BladeResult<Void> deleteRecords(@RequestBody List<Long> ids) {
        return booleanResult(videoRecordService.deleteRecords(ids), "Batch delete video records failed");
    }

    @PostMapping("/start")
    public BladeResult<VideoRecord> startRecording(@RequestParam Long deviceId,
                                                   @RequestParam(required = false) String deviceName,
                                                   @RequestParam(required = false) Integer duration,
                                                   @RequestParam(required = false) String quality) {
        try {
            return BladeResult.success(videoRecordService.startRecording(deviceId, deviceName, duration, quality));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @PostMapping("/stop/{recordId}")
    public BladeResult<Map<String, Object>> stopRecording(@PathVariable Long recordId) {
        try {
            Map<String, Object> result = videoRecordService.stopRecording(recordId);
            return Boolean.TRUE.equals(result.get("success")) ? BladeResult.success(result)
                : BladeResult.<Map<String, Object>>fail(String.valueOf(result.get("message")));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    @GetMapping("/status/{deviceId}")
    public BladeResult<Map<String, Object>> recordingStatus(@PathVariable Long deviceId) {
        return BladeResult.success(videoRecordService.getRecordingStatus(deviceId));
    }

    @GetMapping("/statistics")
    public BladeResult<Map<String, Object>> statistics() {
        return BladeResult.success(videoRecordService.getRecordingStatistics());
    }

    @GetMapping("/device/{deviceId}")
    public BladeResult<List<VideoRecord>> deviceRecords(@PathVariable Long deviceId,
                                                        @RequestParam(required = false) String date,
                                                        @RequestParam(required = false) Long currentPage,
                                                        @RequestParam(required = false) Long pageSize) {
        return BladeResult.success(videoRecordService.getDeviceRecords(deviceId, date, currentPage, pageSize));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> download(@PathVariable Long id) {
        VideoRecord record = videoRecordService.getRecord(id);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        return fileResponse(record.getFilePath(), true, record.getFileName());
    }

    @GetMapping("/{id}/preview")
    public BladeResult<Map<String, Object>> preview(@PathVariable Long id) {
        return BladeResult.success(videoRecordService.getRecordPreview(id));
    }

    @GetMapping("/file/{filePath}")
    public ResponseEntity<?> file(@PathVariable String filePath) {
        return fileResponse(filePath, false, null);
    }

    @GetMapping("/thumbnail/{filePath}")
    public ResponseEntity<?> thumbnail(@PathVariable String filePath) {
        return fileResponse(filePath, false, null);
    }

    /** Return one actual recording through the SpringBlade detail route. */
    @GetMapping("/detail")
    public BladeResult<VideoRecord> detail(@RequestParam Long id) {
        VideoRecord record = videoRecordService.getRecord(id);
        return record == null ? BladeResult.<VideoRecord>fail("Video record does not exist") : BladeResult.success(record);
    }

    /** Return the real recording page through the SpringBlade list route. */
    @GetMapping("/list")
    public BladeResult<BladePage<VideoRecord>> list(@RequestParam(required = false) Long current,
                                                    @RequestParam(required = false) Long size,
                                                    @RequestParam(required = false) Long deviceId,
                                                    @RequestParam(required = false) String deviceName,
                                                    @RequestParam(required = false) String fileName,
                                                    @RequestParam(required = false) String recordStatus,
                                                    @RequestParam(required = false) String date) {
        return page(current, null, size, null, deviceId, deviceName, fileName, recordStatus, null, date);
    }

    /** Query recordings that overlap the requested playback interval. */
    @GetMapping("/playback")
    public BladeResult<List<VideoRecord>> playback(@RequestParam Long deviceId,
                                                    @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
                                                    @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime) {
        try {
            return BladeResult.success(videoRecordService.listPlaybackRecords(deviceId, startTime, endTime));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Return months and days backed by stored recording dates. */
    @GetMapping("/timeline/calendar")
    public BladeResult<Map<Integer, List<Integer>>> timelineCalendar(@RequestParam Long deviceId,
                                                                     @RequestParam Integer year) {
        try {
            return BladeResult.success(videoRecordService.getTimelineCalendar(deviceId, year));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Return actual recordings for one device and day. */
    @GetMapping("/timeline/day")
    public BladeResult<List<VideoRecord>> timelineDay(@RequestParam Long deviceId,
                                                       @RequestParam String recordDate) {
        try {
            return BladeResult.success(videoRecordService.listDayRecords(deviceId, recordDate));
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Stream a local recording with HTTP Range support. */
    @GetMapping("/stream/{recordId}")
    public ResponseEntity<?> stream(@PathVariable Long recordId,
                                    @org.springframework.web.bind.annotation.RequestHeader HttpHeaders headers) throws IOException {
        VideoRecord record = videoRecordService.getRecord(recordId);
        if (record == null || !StringUtils.hasText(record.getFilePath())) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(record.getFilePath());
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }
        long contentLength = resource.contentLength();
        if (contentLength <= 0L) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        List<HttpRange> ranges = headers.getRange();
        if (ranges.isEmpty()) {
            return ResponseEntity.ok().contentType(mediaType).contentLength(contentLength)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes").body(resource);
        }
        HttpRange range = ranges.get(0);
        long start = range.getRangeStart(contentLength);
        long requested = range.getRangeEnd(contentLength) - start + 1L;
        ResourceRegion region = new ResourceRegion(resource, start, Math.min(STREAM_CHUNK_SIZE, requested));
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).contentType(mediaType)
            .header(HttpHeaders.ACCEPT_RANGES, "bytes").body(region);
    }

    /** Create a recording through the SpringBlade save route. */
    @PostMapping("/save")
    public BladeResult<VideoRecord> save(@RequestBody VideoRecord record) {
        return createRecord(record);
    }

    /** Update a recording through the SpringBlade update route. */
    @PostMapping("/update")
    public BladeResult<VideoRecord> update(@RequestBody VideoRecord record) {
        return record == null || record.getId() == null ? BladeResult.<VideoRecord>fail("Video record ID is required")
            : updateRecord(record.getId(), record);
    }

    /** Insert or update a recording through the SpringBlade submit route. */
    @PostMapping("/submit")
    public BladeResult<VideoRecord> submit(@RequestBody VideoRecord record) {
        return record != null && record.getId() != null ? update(record) : createRecord(record);
    }

    /** Delete actual recording rows by comma-separated IDs. */
    @GetMapping("/remove")
    public BladeResult<Void> remove(@RequestParam String ids) {
        try {
            List<Long> parsed = parseIds(ids);
            return parsed.isEmpty() ? BladeResult.fail("ids is required")
                : booleanResult(videoRecordService.deleteRecords(parsed), "Delete video records failed");
        } catch (RuntimeException ex) {
            return BladeResult.fail(ex.getMessage());
        }
    }

    /** Export real filtered recording rows. */
    @GetMapping("/export-vlsVideoRecord")
    public void exportVlsVideoRecord(@RequestParam(required = false) Long deviceId,
                                     @RequestParam(required = false) String deviceName,
                                     @RequestParam(required = false) String fileName,
                                     @RequestParam(required = false) String recordStatus,
                                     @RequestParam(required = false) String date,
                                     javax.servlet.http.HttpServletResponse response) {
        ExcelUtil.exportExcel(videoRecordService.listRecords(deviceId, deviceName, fileName, recordStatus, date),
            "VLS Video Records", VideoRecord.class, response);
    }

    private BladeResult<Void> booleanResult(boolean success, String message) {
        return success ? BladeResult.success() : BladeResult.fail(message);
    }

    private ResponseEntity<?> fileResponse(String filePath, boolean attachment, String attachmentName) {
        String decodedPath = decode(filePath);
        if (!StringUtils.hasText(decodedPath)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(decodedPath);
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok().contentType(mediaType);
        if (attachment) {
            builder.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safeFileName(attachmentName) + "\"");
        }
        return builder.body(resource);
    }

    private String decode(String value) {
        if (value == null) {
            return null;
        }
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException ex) {
            return value;
        }
    }

    private String safeFileName(String value) {
        if (!StringUtils.hasText(value)) {
            return "recording";
        }
        return value.replace("\"", "");
    }
}
