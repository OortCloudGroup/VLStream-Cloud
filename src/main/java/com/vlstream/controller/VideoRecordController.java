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
 * 视频录制记录控制器
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/video-record")
@Api(tags = "视频录制记录管理")
public class VideoRecordController {

    @Autowired
    private VideoRecordService videoRecordService;

    @Value("${recording.storage.path:./recordings}")
    private String recordingStoragePath;

    @ApiOperation("获取录制文件")
    @GetMapping("/file/**")
    public ResponseEntity<Resource> getRecordFile(HttpServletRequest request) {
        log.info("=== 进入getRecordFile方法 ===");
        log.info("请求URI: {}", request.getRequestURI());
        log.info("请求方法: {}", request.getMethod());
        
        try {
            // 获取完整的请求URI
            String requestURI = request.getRequestURI();
            log.info("完整请求URI: {}", requestURI);
            
            // 提取文件路径部分
            String filePath = null;
            if (requestURI.contains("/api/video-record/file/")) {
                filePath = requestURI.substring(requestURI.indexOf("/api/video-record/file/") + "/api/video-record/file/".length());
            } else if (requestURI.contains("/file/")) {
                filePath = requestURI.substring(requestURI.indexOf("/file/") + "/file/".length());
            }
            
            if (filePath == null) {
                log.error("无法提取文件路径: {}", requestURI);
                return ResponseEntity.badRequest().build();
            }
            
            log.info("提取的文件路径: {}", filePath);
            
            // 尝试URL解码
            String decodedFilePath;
            try {
                decodedFilePath = java.net.URLDecoder.decode(filePath, "UTF-8");
                log.info("解码后的文件路径: {}", decodedFilePath);
            } catch (Exception e) {
                log.warn("URL解码失败，使用原始路径: {}", filePath);
                decodedFilePath = filePath;
            }
            
            log.info("=== 文件访问调试信息 ===");
            log.info("recordingStoragePath: {}", recordingStoragePath);
            log.info("最终文件路径: {}", decodedFilePath);
            
            // 构建完整的文件路径
            Path fullPath = Paths.get(recordingStoragePath, decodedFilePath);
            File file = fullPath.toFile();
            
            log.info("完整文件路径: {}", fullPath);
            log.info("文件存在: {}", file.exists());
            log.info("是文件: {}", file.isFile());
            log.info("可读: {}", file.canRead());
            log.info("绝对路径: {}", file.getAbsolutePath());
            
            if (!file.exists()) {
                log.warn("文件不存在: {}", fullPath);
                return ResponseEntity.notFound().build();
            }
            
            if (!file.isFile()) {
                log.warn("路径不是文件: {}", fullPath);
                return ResponseEntity.badRequest().build();
            }
            
            if (!file.canRead()) {
                log.warn("文件无法读取: {}", fullPath);
                return ResponseEntity.status(403).build();
            }
            
            // 检查文件是否在recordings目录下（安全检查）
            Path recordingsPath = Paths.get(recordingStoragePath).toAbsolutePath();
            Path filePathNormalized = fullPath.toAbsolutePath();
            
            log.info("recordingsPath: {}", recordingsPath);
            log.info("filePathNormalized: {}", filePathNormalized);
            log.info("安全检查通过: {}", filePathNormalized.startsWith(recordingsPath));
            
            if (!filePathNormalized.startsWith(recordingsPath)) {
                log.warn("访问路径超出允许范围: {}", filePathNormalized);
                return ResponseEntity.badRequest().build();
            }
            
            Resource resource = new FileSystemResource(file);
            
            // 根据文件扩展名设置Content-Type
            String contentType = getContentType(file.getName());
            
            log.info("成功提供文件: {}", fullPath);
            log.info("Content-Type: {}", contentType);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("获取录制文件失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @ApiOperation("获取录制文件缩略图")
    @GetMapping("/thumbnail/**")
    public ResponseEntity<Resource> getRecordThumbnail(HttpServletRequest request) {
        log.info("=== 进入getRecordThumbnail方法 ===");
        log.info("请求URI: {}", request.getRequestURI());
        log.info("请求方法: {}", request.getMethod());
        
        try {
            // 获取完整的请求URI
            String requestURI = request.getRequestURI();
            log.info("完整请求URI: {}", requestURI);
            
            // 提取文件路径部分
            String filePath = null;
            if (requestURI.contains("/api/video-record/thumbnail/")) {
                filePath = requestURI.substring(requestURI.indexOf("/api/video-record/thumbnail/") + "/api/video-record/thumbnail/".length());
            } else if (requestURI.contains("/thumbnail/")) {
                filePath = requestURI.substring(requestURI.indexOf("/thumbnail/") + "/thumbnail/".length());
            }
            
            if (filePath == null) {
                log.error("无法提取缩略图路径: {}", requestURI);
                return ResponseEntity.badRequest().build();
            }
            
            log.info("提取的缩略图路径: {}", filePath);
            
            // 尝试URL解码
            String decodedFilePath;
            try {
                decodedFilePath = java.net.URLDecoder.decode(filePath, "UTF-8");
                log.info("解码后的缩略图路径: {}", decodedFilePath);
            } catch (Exception e) {
                log.warn("URL解码失败，使用原始路径: {}", filePath);
                decodedFilePath = filePath;
            }
            
            log.info("=== 缩略图访问调试信息 ===");
            log.info("recordingStoragePath: {}", recordingStoragePath);
            log.info("最终缩略图路径: {}", decodedFilePath);
            
            // 构建完整的文件路径
            Path fullPath = Paths.get(recordingStoragePath, decodedFilePath);
            File file = fullPath.toFile();
            
            log.info("完整缩略图路径: {}", fullPath);
            log.info("文件存在: {}", file.exists());
            log.info("是文件: {}", file.isFile());
            log.info("可读: {}", file.canRead());
            log.info("绝对路径: {}", file.getAbsolutePath());
            
            if (!file.exists()) {
                log.warn("缩略图不存在: {}", fullPath);
                return ResponseEntity.notFound().build();
            }
            
            if (!file.isFile()) {
                log.warn("路径不是文件: {}", fullPath);
                return ResponseEntity.badRequest().build();
            }
            
            if (!file.canRead()) {
                log.warn("文件无法读取: {}", fullPath);
                return ResponseEntity.status(403).build();
            }
            
            // 检查文件是否在recordings目录下（安全检查）
            Path recordingsPath = Paths.get(recordingStoragePath).toAbsolutePath();
            Path filePathNormalized = fullPath.toAbsolutePath();
            
            log.info("recordingsPath: {}", recordingsPath);
            log.info("filePathNormalized: {}", filePathNormalized);
            log.info("安全检查通过: {}", filePathNormalized.startsWith(recordingsPath));
            
            if (!filePathNormalized.startsWith(recordingsPath)) {
                log.warn("访问路径超出允许范围: {}", filePathNormalized);
                return ResponseEntity.badRequest().build();
            }
            
            Resource resource = new FileSystemResource(file);
            
            // 根据文件扩展名设置Content-Type
            String contentType = getContentType(file.getName());
            
            log.info("成功提供缩略图: {}", fullPath);
            log.info("Content-Type: {}", contentType);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("获取缩略图失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 根据文件扩展名获取Content-Type
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

    @ApiOperation("分页查询视频录制记录")
    @GetMapping("/page")
    public Result<IPage<VideoRecord>> pageVideoRecords(
            @ApiParam(value = "当前页", defaultValue = "1") @RequestParam(defaultValue = "1") Integer current,
            @ApiParam(value = "每页大小", defaultValue = "10") @RequestParam(defaultValue = "10") Integer size,
            @ApiParam("设备ID") @RequestParam(required = false) Long deviceId,
            @ApiParam("设备名称") @RequestParam(required = false) String deviceName,
            @ApiParam("录制状态") @RequestParam(required = false) String recordStatus,
            @ApiParam("录制质量") @RequestParam(required = false) String quality,
            @ApiParam("开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @ApiParam("结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @ApiParam("开始时间") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @ApiParam("结束时间") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        try {
            Page<VideoRecord> page = new Page<>(current, size);
            IPage<VideoRecord> result = videoRecordService.pageVideoRecords(page, deviceId, deviceName, 
                                                                           recordStatus, quality, startDate, 
                                                                           endDate, startTime, endTime);
            return Result.success(result);
        } catch (Exception e) {
            log.error("分页查询视频录制记录失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @ApiOperation("根据ID查询视频录制记录")
    @GetMapping("/{id}")
    public Result<VideoRecord> getVideoRecord(@ApiParam("录制记录ID") @PathVariable Long id) {
        try {
            VideoRecord record = videoRecordService.getById(id);
            if (record == null) {
                return Result.error("录制记录不存在");
            }
            return Result.success(record);
        } catch (Exception e) {
            log.error("查询视频录制记录失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @ApiOperation("开始录制视频")
    @PostMapping("/start")
    public Result<VideoRecord> startRecording(
            @ApiParam("设备ID") @RequestParam Long deviceId,
            @ApiParam("设备名称") @RequestParam String deviceName,
            @ApiParam(value = "录制时长(秒)", defaultValue = "600") @RequestParam(defaultValue = "600") Integer duration,
            @ApiParam(value = "录制质量", defaultValue = "medium") @RequestParam(defaultValue = "medium") String quality
    ) {
        try {
            VideoRecord record = videoRecordService.startRecording(deviceId, deviceName, duration, quality);
            if (record != null) {
                return Result.success(record);
            } else {
                return Result.error("开始录制失败");
            }
        } catch (Exception e) {
            log.error("开始录制视频失败", e);
            return Result.error("录制失败：" + e.getMessage());
        }
    }

    @ApiOperation("停止录制视频")
    @PostMapping("/stop/{id}")
    public Result<String> stopRecording(@ApiParam("录制记录ID") @PathVariable Long id) {
        try {
            boolean success = videoRecordService.stopRecording(id);
            if (success) {
                return Result.success("停止录制成功");
            } else {
                return Result.error("停止录制失败");
            }
        } catch (Exception e) {
            log.error("停止录制视频失败", e);
            return Result.error("停止录制失败：" + e.getMessage());
        }
    }

    @ApiOperation("删除录制记录")
    @DeleteMapping("/{id}")
    public Result<String> deleteRecord(@ApiParam("录制记录ID") @PathVariable Long id) {
        try {
            boolean success = videoRecordService.deleteRecord(id);
            if (success) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除录制记录失败", e);
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取设备录制统计信息")
    @GetMapping("/statistics/device")
    public Result<List<Map<String, Object>>> getDeviceRecordStatistics(
            @ApiParam("设备ID") @RequestParam(required = false) Long deviceId,
            @ApiParam("开始日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @ApiParam("结束日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {
        try {
            List<Map<String, Object>> statistics = videoRecordService.getDeviceRecordStatistics(deviceId, startDate, endDate);
            return Result.success(statistics);
        } catch (Exception e) {
            log.error("获取设备录制统计失败", e);
            return Result.error("获取统计失败：" + e.getMessage());
        }
    }

    @ApiOperation("获取正在录制的视频记录")
    @GetMapping("/recording")
    public Result<List<VideoRecord>> getRecordingVideos() {
        try {
            List<VideoRecord> records = videoRecordService.getRecordingVideos();
            return Result.success(records);
        } catch (Exception e) {
            log.error("获取正在录制的视频失败", e);
            return Result.error("获取失败：" + e.getMessage());
        }
    }

    @ApiOperation("清理过期录制记录")
    @PostMapping("/cleanup")
    public Result<String> cleanupExpiredRecords() {
        try {
            int cleanupCount = videoRecordService.cleanupExpiredRecords();
            return Result.success("清理完成，删除了 " + cleanupCount + " 条过期记录");
        } catch (Exception e) {
            log.error("清理过期录制记录失败", e);
            return Result.error("清理失败：" + e.getMessage());
        }
    }

    @ApiOperation("根据设备ID和日期查询录制记录")
    @GetMapping("/device/{deviceId}/date/{date}")
    public Result<List<VideoRecord>> getRecordsByDeviceAndDate(
            @ApiParam("设备ID") @PathVariable Long deviceId,
            @ApiParam("日期") @PathVariable @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        try {
            List<VideoRecord> records = videoRecordService.getRecordsByDeviceAndDate(deviceId, date);
            return Result.success(records);
        } catch (Exception e) {
            log.error("根据设备ID和日期查询录制记录失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @ApiOperation("根据设备ID和时间范围查询录制记录")
    @GetMapping("/device/{deviceId}/time-range")
    public Result<List<VideoRecord>> getRecordsByDeviceAndTimeRange(
            @ApiParam("设备ID") @PathVariable Long deviceId,
            @ApiParam("开始时间") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @ApiParam("结束时间") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime
    ) {
        try {
            List<VideoRecord> records = videoRecordService.getRecordsByDeviceAndTimeRange(deviceId, startTime, endTime);
            return Result.success(records);
        } catch (Exception e) {
            log.error("根据设备ID和时间范围查询录制记录失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @ApiOperation("根据设备ID查询录制记录")
    @GetMapping("/device/{deviceId}")
    public Result<List<VideoRecord>> getDeviceRecords(
            @ApiParam("设备ID") @PathVariable Long deviceId,
            @ApiParam("日期") @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @ApiParam("页面大小") @RequestParam(required = false, defaultValue = "100") Integer pageSize,
            @ApiParam("当前页") @RequestParam(required = false, defaultValue = "1") Integer currentPage
    ) {
        try {
            List<VideoRecord> records;
            if (date != null) {
                // 如果传入了日期，按日期查询
                records = videoRecordService.getRecordsByDeviceAndDate(deviceId, date);
            } else {
                // 如果没有传入日期，获取设备的所有录制记录
                records = videoRecordService.getRecordsByDeviceAndDate(deviceId, LocalDate.now());
            }
            
            log.info("查询设备录制记录: deviceId={}, date={}, 找到记录数={}", deviceId, date, records.size());
            return Result.success(records);
        } catch (Exception e) {
            log.error("根据设备ID查询录制记录失败: deviceId={}, date={}", deviceId, date, e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }
}