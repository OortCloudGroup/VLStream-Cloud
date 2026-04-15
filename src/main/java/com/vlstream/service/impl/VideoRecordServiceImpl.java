package com.vlstream.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vlstream.entity.VideoRecord;
import com.vlstream.entity.DeviceInfo;
import com.vlstream.mapper.VideoRecordMapper;
import com.vlstream.service.VideoRecordService;
import com.vlstream.service.DeviceInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 视频录制记录服务实现类
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class VideoRecordServiceImpl extends ServiceImpl<VideoRecordMapper, VideoRecord> implements VideoRecordService {

    @Autowired
    private VideoRecordMapper videoRecordMapper;

    @Autowired
    private DeviceInfoService deviceInfoService;
    
    @Autowired
    @Lazy
    private RecordingTaskService recordingTaskService;

    @Value("${recording.storage.path:/recordings}")
    private String recordingStoragePath;

    @Value("${recording.base.path:recordings}")
    private String recordingBasePath;

    @Value("${recording.playback.url.prefix:http://localhost:8080/video/}")
    private String playbackUrlPrefix;

    @Override
    public IPage<VideoRecord> pageVideoRecords(Page<VideoRecord> page, Long deviceId, String deviceName, 
                                               String recordStatus, String quality, LocalDate startDate, 
                                               LocalDate endDate, LocalDateTime startTime, LocalDateTime endTime) {
        // 临时实现：使用基础查询方法
        return page(page);
    }

    @Override
    public List<VideoRecord> getRecordsByDeviceAndDate(Long deviceId, LocalDate recordDate) {
        return videoRecordMapper.selectByDeviceAndDate(deviceId, recordDate);
    }

    @Override
    public List<VideoRecord> getRecordsByDeviceAndTimeRange(Long deviceId, LocalDateTime startTime, LocalDateTime endTime) {
        return videoRecordMapper.selectByDeviceAndTimeRange(deviceId, startTime, endTime);
    }

    @Override
    public VideoRecord startRecording(Long deviceId, String deviceName, Integer duration, String quality) {
        VideoRecord record = new VideoRecord();
        record.setDeviceId(deviceId);
        record.setDeviceName(deviceName);
        record.setRecordStatus(VideoRecord.STATUS_RECORDING);
        record.setQuality(quality);
        record.setFormat(VideoRecord.FORMAT_MP4);
        record.setDuration(duration);
        
        LocalDateTime now = LocalDateTime.now();
        record.setRecordStartTime(now);
        record.setRecordEndTime(now.plusSeconds(duration));
        record.setRecordDate(now.toLocalDate());
        
        // 生成文件名和路径
        String dateStr = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String fileName = String.format("%s_%s_%s.mp4", 
                                       deviceName, 
                                       deviceId, 
                                       now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
        
        String filePath = recordingStoragePath + "/" + dateStr + "/" + fileName;
        record.setFileName(fileName);
        record.setFilePath(filePath);
        
        record.setCreatedBy("system");
        
        // 创建目录
        createDirectoryIfNotExists(filePath);
        
        if (save(record)) {
            log.info("开始录制视频: 设备ID={}, 设备名称={}, 录制时长={}秒, 文件路径={}", 
                    deviceId, deviceName, duration, filePath);
            
            // 立即启动录制任务
            try {
                DeviceInfo device = deviceInfoService.getById(deviceId);
                if (device != null) {
                    recordingTaskService.performManualRecording(record, duration, device);
                    log.info("手动录制任务已启动: recordId={}, deviceId={}", record.getId(), deviceId);
                } else {
                    log.error("设备不存在，无法启动录制: deviceId={}", deviceId);
                    markRecordingFailed(record.getId(), "设备不存在");
                }
            } catch (Exception e) {
                log.error("启动录制任务失败: recordId={}, deviceId={}, error={}", record.getId(), deviceId, e.getMessage(), e);
                markRecordingFailed(record.getId(), "启动录制任务失败: " + e.getMessage());
            }
            
            return record;
        }
        
        log.error("创建录制记录失败: 设备ID={}, 设备名称={}", deviceId, deviceName);
        return null;
    }

    @Override
    public boolean stopRecording(Long recordId) {
        VideoRecord record = getById(recordId);
        if (record == null) {
            log.warn("录制记录不存在: recordId={}", recordId);
            return false;
        }
        
        // 如果录制已经完成或失败，直接返回成功
        if (VideoRecord.STATUS_COMPLETED.equals(record.getRecordStatus()) || 
            VideoRecord.STATUS_FAILED.equals(record.getRecordStatus())) {
            log.info("录制已经完成或失败，直接返回成功: recordId={}, status={}", recordId, record.getRecordStatus());
            return true;
        }
        
        if (!VideoRecord.STATUS_RECORDING.equals(record.getRecordStatus())) {
            log.warn("录制记录状态不是录制中: recordId={}, status={}", recordId, record.getRecordStatus());
            return false;
        }

        // 停止实际的录制进程
        try {
            recordingTaskService.stopManualRecording(record.getDeviceId(), recordId);
        } catch (Exception e) {
            log.warn("停止录制进程时发生异常: recordId={}, error={}", recordId, e.getMessage());
        }
        
        // 计算实际录制时长
        LocalDateTime endTime = LocalDateTime.now();
        int actualDuration = record.getRecordStartTime() != null ? 
            (int) java.time.Duration.between(record.getRecordStartTime(), endTime).getSeconds() : 0;
        
        record.setRecordStatus(VideoRecord.STATUS_COMPLETED);
        record.setRecordEndTime(endTime);
        record.setDuration(actualDuration); // 更新为实际录制时长
        record.setUpdatedBy("system");
        
        boolean success = updateById(record);
        if (success) {
            log.info("停止录制视频: recordId={}, 设备ID={}, 实际时长={}秒, 文件路径={}", 
                    recordId, record.getDeviceId(), actualDuration, record.getFilePath());
        } else {
            log.error("停止录制失败: recordId={}", recordId);
        }
        
        return success;
    }

    @Override
    public boolean completeRecording(Long recordId, String filePath, Long fileSize, Integer duration, String thumbnailPath) {
        VideoRecord record = getById(recordId);
        if (record == null) {
            log.warn("录制记录不存在: recordId={}", recordId);
            return false;
        }
        
        record.setRecordStatus(VideoRecord.STATUS_COMPLETED);
        record.setRecordEndTime(LocalDateTime.now());
        record.setFilePath(filePath);
        record.setFileSize(fileSize);
        record.setDuration(duration);
        record.setThumbnailPath(thumbnailPath);
        record.setUpdatedBy("system");
        
        boolean success = updateById(record);
        if (success) {
            log.info("完成录制视频: recordId={}, 设备ID={}, 文件大小={}字节, 时长={}秒", 
                    recordId, record.getDeviceId(), fileSize, duration);
        } else {
            log.error("完成录制失败: recordId={}", recordId);
        }
        
        return success;
    }

    @Override
    public boolean markRecordingFailed(Long recordId, String errorMessage) {
        VideoRecord record = getById(recordId);
        if (record == null) {
            log.warn("录制记录不存在: recordId={}", recordId);
            return false;
        }
        
        record.setRecordStatus(VideoRecord.STATUS_FAILED);
        record.setRecordEndTime(LocalDateTime.now());
        record.setErrorMessage(errorMessage);
        record.setUpdatedBy("system");
        
        boolean success = updateById(record);
        if (success) {
            log.info("标记录制失败: recordId={}, 设备ID={}, 错误信息={}", 
                    recordId, record.getDeviceId(), errorMessage);
        } else {
            log.error("标记录制失败时更新失败: recordId={}", recordId);
        }
        
        return success;
    }

    @Override
    public boolean deleteRecord(Long recordId) {
        VideoRecord record = getById(recordId);
        if (record == null) {
            log.warn("录制记录不存在: recordId={}", recordId);
            return false;
        }
        
        boolean success = removeById(recordId);
        if (success) {
            log.info("删除录制记录: recordId={}, 设备ID={}, 文件路径={}", 
                    recordId, record.getDeviceId(), record.getFilePath());
        } else {
            log.error("删除录制记录失败: recordId={}", recordId);
        }
        
        return success;
    }

    @Override
    public boolean batchDeleteRecords(List<Long> recordIds) {
        if (recordIds == null || recordIds.isEmpty()) {
            return false;
        }
        
        boolean success = removeByIds(recordIds);
        if (success) {
            log.info("批量删除录制记录: 数量={}", recordIds.size());
        } else {
            log.error("批量删除录制记录失败: recordIds={}", recordIds);
        }
        
        return success;
    }

    @Override
    public List<Map<String, Object>> getDeviceRecordStatistics(Long deviceId, LocalDate startDate, LocalDate endDate) {
        // 临时实现：返回空列表
        return new ArrayList<>();
    }

    @Override
    public List<Map<String, Object>> getDateStatistics(Long deviceId, LocalDate startDate, LocalDate endDate) {
        // 临时实现：返回空列表
        return new ArrayList<>();
    }

    @Override
    public List<VideoRecord> getRecordingVideos() {
        return videoRecordMapper.selectRecordingVideos();
    }

    @Override
    public VideoRecord getLatestRecordByDevice(Long deviceId) {
        return videoRecordMapper.selectLatestByDeviceId(deviceId);
    }

    @Override
    public VideoRecord getRecordByFilePath(String filePath) {
        return videoRecordMapper.selectByFilePath(filePath);
    }

    @Override
    public boolean batchUpdateStatus(List<Long> recordIds, String newStatus, String updatedBy) {
        if (recordIds == null || recordIds.isEmpty()) {
            return false;
        }
        
        // 临时实现：逐个更新
        boolean success = true;
        for (Long recordId : recordIds) {
            VideoRecord record = getById(recordId);
            if (record != null) {
                record.setRecordStatus(newStatus);
                record.setUpdatedBy(updatedBy);
                success = success && updateById(record);
            }
        }
        
        if (success) {
            log.info("批量更新录制状态: 数量={}, 新状态={}", recordIds.size(), newStatus);
        } else {
            log.error("批量更新录制状态失败: recordIds={}, newStatus={}", recordIds, newStatus);
        }
        
        return success;
    }

    @Override
    public Map<String, Object> getStorageStatistics(Long deviceId) {
        // 临时实现：返回空Map
        return new HashMap<>();
    }

    @Override
    public int cleanupExpiredRecords() {
        // 临时实现：暂不清理
        log.info("清理过期录制记录完成: 清理数量={}", 0);
        return 0;
    }

    @Override
    public String getPlaybackUrl(Long recordId) {
        VideoRecord record = getById(recordId);
        if (record == null || !VideoRecord.STATUS_COMPLETED.equals(record.getRecordStatus())) {
            return null;
        }
        
        return playbackUrlPrefix + record.getFileName();
    }

    @Override
    public String generateThumbnail(String videoPath) {
        // TODO: 实现视频缩略图生成逻辑
        // 可以使用FFmpeg等工具生成缩略图
        log.info("生成视频缩略图: {}", videoPath);
        return null;
    }

    @Override
    public boolean validateRecordFile(String filePath) {
        try {
            File file = new File(filePath);
            return file.exists() && file.isFile() && file.length() > 0;
        } catch (Exception e) {
            log.error("验证录制文件失败: filePath={}, error={}", filePath, e.getMessage());
            return false;
        }
    }

    @Override
    public RecordStatistics getRecordStatistics(Long deviceId, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> stats = getStorageStatistics(deviceId);
        
        RecordStatistics statistics = new RecordStatistics();
        statistics.setTotalRecords(getLongValue(stats, "total_files"));
        statistics.setCompletedRecords(getLongValue(stats, "completed_files"));
        statistics.setFailedRecords(getLongValue(stats, "failed_files"));
        statistics.setRecordingRecords(getLongValue(stats, "recording_files"));
        statistics.setTotalFileSize(getLongValue(stats, "total_size"));
        statistics.setTotalDuration(getLongValue(stats, "total_duration"));
        
        // 计算成功率
        long total = statistics.getTotalRecords();
        long completed = statistics.getCompletedRecords();
        if (total > 0) {
            statistics.setSuccessRate((double) completed / total * 100);
        } else {
            statistics.setSuccessRate(0.0);
        }
        
        return statistics;
    }
    
    private void createDirectoryIfNotExists(String filePath) {
        try {
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                if (created) {
                    log.info("创建录制目录: {}", parentDir.getAbsolutePath());
                } else {
                    log.warn("创建录制目录失败: {}", parentDir.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            log.error("创建录制目录异常: filePath={}, error={}", filePath, e.getMessage());
        }
    }
    
    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }
} 