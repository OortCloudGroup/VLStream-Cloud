package com.vlstream.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vlstream.common.Result;
import com.vlstream.entity.VideoRecord;
import com.vlstream.service.VideoRecordService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Video Recording Record Controller
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/video-record")
@Api(tags = "Video Recording Record Management")
public class VideoRecordController {

    @Autowired
    private VideoRecordService videoRecordService;

    @Value("${recording.storage.path:./recordings}")
    private String recordingStoragePath;

    @ApiOperation("Get recording file")
    @GetMapping("/file/**")
    public ResponseEntity<Resource> getRecordFile(HttpServletRequest request) {
        log.info("=== Enter getRecordFile method ===");
        log.info("Request URI: {}", request.getRequestURI());
        log.info("Request method: {}", request.getMethod());
        
        try {
            // Get complete request URI
            String requestURI = request.getRequestURI();
            log.info("Complete request URI: {}", requestURI);
            
            // Extract file path part
            String filePath = null;
            if (requestURI.contains("/api/video-record/file/")) {
                filePath = requestURI.substring(requestURI.indexOf("/api/video-record/file/") + "/api/video-record/file/".length());
            } else if (requestURI.contains("/file/")) {
                filePath = requestURI.substring(requestURI.indexOf("/file/") + "/file/".length());
            }
            
            if (filePath == null) {
                log.error("Unable to extract file path: {}", requestURI);
                return ResponseEntity.badRequest().build();
            }
            
            log.info("Extracted file path: {}", filePath);
            
            // Try URL decoding
            String decodedFilePath;
            try {
                decodedFilePath = java.net.URLDecoder.decode(filePath, "UTF-8");
                log.info("Decoded file path: {}", decodedFilePath);
            } catch (Exception e) {
                log.warn("URL decode failed, using original path: {}", filePath);
                decodedFilePath = filePath;
            }
            
            log.info("=== File access debug information ===");
            log.info("recordingStoragePath: {}", recordingStoragePath);
            log.info("Final file path: {}", decodedFilePath);
            
            // Build complete file path
            Path fullPath = Paths.get(recordingStoragePath, decodedFilePath);
            File file = fullPath.toFile();
            
            log.info("Complete file path: {}", fullPath);
            log.info("File exists: {}", file.exists());
            log.info("Is file: {}", file.isFile());
            log.info("Readable: {}", file.canRead());
            log.info("Absolute path: {}", file.getAbsolutePath());
            
            if (!file.exists()) {
                log.warn("File does not exist: {}", fullPath);
                return ResponseEntity.notFound().build();
            }
            
            if (!file.isFile()) {
                log.warn("Path is not a file: {}", fullPath);
                return ResponseEntity.badRequest().build();
            }
            
            if (!file.canRead()) {
                log.warn("File cannot be read: {}", fullPath);
                return ResponseEntity.status(403).build();
            }
            
            // Check if file is in recordings directory (security check)
            Path recordingsPath = Paths.get(recordingStoragePath).toAbsolutePath();
            Path filePathNormalized = fullPath.toAbsolutePath();
            
            log.info("recordingsPath: {}", recordingsPath);
            log.info("filePathNormalized: {}", filePathNormalized);
            log.info("Security check passed: {}", filePathNormalized.startsWith(recordingsPath));
            
            if (!filePathNormalized.startsWith(recordingsPath)) {
                log.warn("Access path exceeds allowed range: {}", filePathNormalized);
                return ResponseEntity.badRequest().build();
            }
            
            Resource resource = new FileSystemResource(file);
            
            // Set Content-Type based on file extension
            String contentType = getContentType(file.getName());
            
            log.info("Successfully serving file: {}", fullPath);
            log.info("Content-Type: {}", contentType);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Failed to get recording file", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @ApiOperation("Get recording file thumbnail")
    @GetMapping("/thumbnail/**")
    public ResponseEntity<Resource> getRecordThumbnail(HttpServletRequest request) {
        log.info("=== Enter getRecordThumbnail method ===");
        log.info("Request URI: {}", request.getRequestURI());
        log.info("Request method: {}", request.getMethod());
        
        try {
            // Get complete request URI
            String requestURI = request.getRequestURI();
            log.info("Complete request URI: {}", requestURI);
            
            // Extract file path part
            String filePath = null;
            if (requestURI.contains("/api/video-record/thumbnail/")) {
                filePath = requestURI.substring(requestURI.indexOf("/api/video-record/thumbnail/") + "/api/video-record/thumbnail/".length());
            } else if (requestURI.contains("/thumbnail/")) {
                filePath = requestURI.substring(requestURI.indexOf("/thumbnail/") + "/thumbnail/".length());
            }
            
            if (filePath == null) {
                log.error("Unable to extract thumbnail path: {}", requestURI);
                return ResponseEntity.badRequest().build();
            }
            
            log.info("Extracted thumbnail path: {}", filePath);
            
            // Try URL decoding
            String decodedFilePath;
            try {
                decodedFilePath = java.net.URLDecoder.decode(filePath, "UTF-8");
                log.info("Decoded thumbnail path: {}", decodedFilePath);
            } catch (Exception e) {
                log.warn("URL decode failed, using original path: {}", filePath);
                decodedFilePath = filePath;
            }
            
            log.info("=== Thumbnail access debug information ===");
            log.info("recordingStoragePath: {}", recordingStoragePath);
            log.info("Final thumbnail path: {}", decodedFilePath);
            
            // Build complete file path
            Path fullPath = Paths.get(recordingStoragePath, decodedFilePath);
            File file = fullPath.toFile();
            
            log.info("Complete thumbnail path: {}", fullPath);
            log.info("File exists: {}", file.exists());
            log.info("Is file: {}", file.isFile());
            log.info("Readable: {}", file.canRead());
            log.info("Absolute path: {}", file.getAbsolutePath());
            
            if (!file.exists()) {
                log.warn("Thumbnail does not exist: {}", fullPath);
                return ResponseEntity.notFound().build();
            }
            
            if (!file.isFile()) {
                log.warn("Path is not a file: {}", fullPath);
                return ResponseEntity.badRequest().build();
            }
            
            if (!file.canRead()) {
                log.warn("File cannot be read: {}", fullPath);
                return ResponseEntity.status(403).build();
            }
            
            // Check if file is in recordings directory (security check)
            Path recordingsPath = Paths.get(recordingStoragePath).toAbsolutePath();
            Path filePathNormalized = fullPath.toAbsolutePath();
            
            log.info("recordingsPath: {}", recordingsPath);
            log.info("filePathNormalized: {}", filePathNormalized);
            log.info("Security check passed: {}", filePathNormalized.startsWith(recordingsPath));
            
            if (!filePathNormalized.startsWith(recordingsPath)) {
                log.warn("Access path exceeds allowed range: {}", filePathNormalized);
                return ResponseEntity.badRequest().build();
            }
            
            Resource resource = new FileSystemResource(file);
            
            // Set Content-Type based on file extension
            String contentType = getContentType(file.getName());
            
            log.info("Successfully serving thumbnail: {}", fullPath);
            log.info("Content-Type: {}", contentType);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("Failed to get thumbnail", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get Content-Type based on file extension
     */
    private String getContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        switch (extension) {
            case "mp4":
                return "video/mp4";
            case "avi":
                return "video/x-msvideo";
            case "mov":
                return "video/quicktime";
            case "mkv":
                return "video/x-matroska";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            default:
                return "application/octet-stream";
        }
    }

    @ApiOperation("Page query video recording records")
    @GetMapping("/page")
    public Result<IPage<VideoRecord>> pageVideoRecords(
            @ApiParam(value = "Current page", defaultValue = "1") @RequestParam(defaultValue = "1") Integer current,
            @ApiParam(value = "Page size", defaultValue = "10") @RequestParam(defaultValue = "10") Integer size,
            @ApiParam("Device ID") @RequestParam(required = false) Long deviceId,
            @ApiParam("Device name") @RequestParam(required = false) String deviceName,
            @ApiParam("Recording status") @RequestParam(required = false) String recordStatus,
            @ApiParam("Recording quality") @RequestParam(required = false) String quality,
            @ApiParam("Start date") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @ApiParam("End date") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @ApiParam("Start time") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @ApiParam("End time") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        try {
            Page<VideoRecord> page = new Page<>(current, size);
            IPage<VideoRecord> result = videoRecordService.pageVideoRecords(page, deviceId, deviceName, 
                                                                           recordStatus, quality, startDate, 
                                                                           endDate, startTime, endTime);
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to query video recording records by page", e);
            return Result.error("Query failed: " + e.getMessage());
        }
    }

    @ApiOperation("Query video recording record by ID")
    @GetMapping("/{id}")
    public Result<VideoRecord> getVideoRecord(@ApiParam("Recording record ID") @PathVariable Long id) {
        try {
            VideoRecord record = videoRecordService.getById(id);
            if (record == null) {
                return Result.error("Recording record does not exist");
            }
            return Result.success(record);
        } catch (Exception e) {
            log.error("Failed to query video recording record", e);
            return Result.error("Query failed: " + e.getMessage());
        }
    }

    @ApiOperation("Start recording video")
    @PostMapping("/start")
    public Result<VideoRecord> startRecording(
            @ApiParam("Device ID") @RequestParam Long deviceId,
            @ApiParam("Device name") @RequestParam String deviceName,
            @ApiParam(value = "Recording duration (seconds)", defaultValue = "600") @RequestParam(defaultValue = "600") Integer duration,
            @ApiParam(value = "Recording quality", defaultValue = "medium") @RequestParam(defaultValue = "medium") String quality
    ) {
        try {
            VideoRecord record = videoRecordService.startRecording(deviceId, deviceName, duration, quality);
            if (record != null) {
                return Result.success(record);
            } else {
                return Result.error("Failed to start recording");
            }
        } catch (Exception e) {
            log.error("Failed to start recording video", e);
            return Result.error("Recording failed: " + e.getMessage());
        }
    }

    @ApiOperation("Stop recording video")
    @PostMapping("/stop/{id}")
    public Result<String> stopRecording(@ApiParam("Recording record ID") @PathVariable Long id) {
        try {
            boolean success = videoRecordService.stopRecording(id);
            if (success) {
                return Result.success("Stop recording successful");
            } else {
                return Result.error("Failed to stop recording");
            }
        } catch (Exception e) {
            log.error("Failed to stop recording video", e);
            return Result.error("Failed to stop recording: " + e.getMessage());
        }
    }

    @ApiOperation("Delete recording record")
    @DeleteMapping("/{id}")
    public Result<String> deleteRecord(@ApiParam("Recording record ID") @PathVariable Long id) {
        try {
            boolean success = videoRecordService.deleteRecord(id);
            if (success) {
                return Result.success("Delete successful");
            } else {
                return Result.error("Delete failed");
            }
        } catch (Exception e) {
            log.error("Failed to delete recording record", e);
            return Result.error("Delete failed: " + e.getMessage());
        }
    }

    @ApiOperation("Get device recording statistics")
    @GetMapping("/statistics/device")
    public Result<List<Map<String, Object>>> getDeviceRecordStatistics(
            @ApiParam("Device ID") @RequestParam(required = false) Long deviceId,
            @ApiParam("Start date") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @ApiParam("End date") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {
        try {
            List<Map<String, Object>> statistics = videoRecordService.getDeviceRecordStatistics(deviceId, startDate, endDate);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("Failed to get device recording statistics", e);
            return Result.error("Failed to get statistics: " + e.getMessage());
        }
    }

    @ApiOperation("Get currently recording video records")
    @GetMapping("/recording")
    public Result<List<VideoRecord>> getRecordingVideos() {
        try {
            List<VideoRecord> records = videoRecordService.getRecordingVideos();
            return Result.success(records);
        } catch (Exception e) {
            log.error("Failed to get currently recording videos", e);
            return Result.error("Get failed: " + e.getMessage());
        }
    }

    @ApiOperation("Clean up expired recording records")
    @PostMapping("/cleanup")
    public Result<String> cleanupExpiredRecords() {
        try {
            int cleanupCount = videoRecordService.cleanupExpiredRecords();
            return Result.success("Cleanup completed, deleted " + cleanupCount + " expired records");
        } catch (Exception e) {
            log.error("Failed to clean up expired recording records", e);
            return Result.error("Cleanup failed: " + e.getMessage());
        }
    }

    @ApiOperation("Query recording records by device ID and date")
    @GetMapping("/device/{deviceId}/date/{date}")
    public Result<List<VideoRecord>> getRecordsByDeviceAndDate(
            @ApiParam("Device ID") @PathVariable Long deviceId,
            @ApiParam("Date") @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        try {
            List<VideoRecord> records = videoRecordService.getRecordsByDeviceAndDate(deviceId, date);
            return Result.success(records);
        } catch (Exception e) {
            log.error("Failed to query recording records by device ID and date", e);
            return Result.error("Query failed: " + e.getMessage());
        }
    }

    @ApiOperation("Query recording records by device ID and time range")
    @GetMapping("/device/{deviceId}/time-range")
    public Result<List<VideoRecord>> getRecordsByDeviceAndTimeRange(
            @ApiParam("Device ID") @PathVariable Long deviceId,
            @ApiParam("Start time") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @ApiParam("End time") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        try {
            List<VideoRecord> records = videoRecordService.getRecordsByDeviceAndTimeRange(deviceId, startTime, endTime);
            return Result.success(records);
        } catch (Exception e) {
            log.error("Failed to query recording records by device ID and time range", e);
            return Result.error("Query failed: " + e.getMessage());
        }
    }

    @ApiOperation("Query recording records by device ID")
    @GetMapping("/device/{deviceId}")
    public Result<List<VideoRecord>> getDeviceRecords(
            @ApiParam("Device ID") @PathVariable Long deviceId,
            @ApiParam("Date") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @ApiParam("Page size") @RequestParam(required = false, defaultValue = "100") Integer pageSize,
            @ApiParam("Current page") @RequestParam(required = false, defaultValue = "1") Integer currentPage
    ) {
        try {
            List<VideoRecord> records;
            if (date != null) {
                // If date is passed, query by date
                records = videoRecordService.getRecordsByDeviceAndDate(deviceId, date);
            } else {
                // If no date is passed, get all recording records for the device
                records = videoRecordService.getRecordsByDeviceAndDate(deviceId, LocalDate.now());
            }
            
            log.info("Query device recording records: deviceId={}, date={}, record count={}", deviceId, date, records.size());
            return Result.success(records);
        } catch (Exception e) {
            log.error("Failed to query recording records by device ID: deviceId={}, date={}", deviceId, date, e);
            return Result.error("Query failed: " + e.getMessage());
        }
    }
}