/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.vlstream;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.compat.BladeResult;
import com.ruoyi.vlstream.domain.VideoRecord;
import com.ruoyi.vlstream.service.IVlsVideoRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
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
import java.util.List;
import java.util.Map;

/**
 * SpringBlade-compatible video record routes used by VLStream-Web.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/vlsVideoRecord")
public class VlsVideoRecordController {

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
        return BladeResult.success(videoRecordService.startRecording(deviceId, deviceName, duration, quality));
    }

    @PostMapping("/stop/{recordId}")
    public BladeResult<Map<String, Object>> stopRecording(@PathVariable Long recordId) {
        return BladeResult.success(videoRecordService.stopRecording(recordId));
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
