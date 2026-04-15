package com.vlstream.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vlstream.entity.VideoRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 视频录制记录服务接口
 *
 * @author VLStream Team
 * @since 1.0.0
 */
public interface VideoRecordService extends IService<VideoRecord> {

    /**
     * 分页查询视频录制记录
     */
    IPage<VideoRecord> pageVideoRecords(
            Page<VideoRecord> page,
            Long deviceId,
            String deviceName,
            String recordStatus,
            String quality,
            LocalDate startDate,
            LocalDate endDate,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    /**
     * 根据设备ID查询指定日期的录制记录
     */
    List<VideoRecord> getRecordsByDeviceAndDate(Long deviceId, LocalDate recordDate);

    /**
     * 根据设备ID查询指定时间范围的录制记录
     */
    List<VideoRecord> getRecordsByDeviceAndTimeRange(Long deviceId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 开始录制视频
     */
    VideoRecord startRecording(Long deviceId, String deviceName, Integer duration, String quality);

    /**
     * 停止录制视频
     */
    boolean stopRecording(Long recordId);

    /**
     * 完成录制（更新录制状态和文件信息）
     */
    boolean completeRecording(Long recordId, String filePath, Long fileSize, Integer duration, String thumbnailPath);

    /**
     * 标记录制失败
     */
    boolean markRecordingFailed(Long recordId, String errorMessage);

    /**
     * 删除录制记录（软删除）
     */
    boolean deleteRecord(Long recordId);

    /**
     * 批量删除录制记录
     */
    boolean batchDeleteRecords(List<Long> recordIds);

    /**
     * 获取设备录制统计信息
     */
    List<Map<String, Object>> getDeviceRecordStatistics(Long deviceId, LocalDate startDate, LocalDate endDate);

    /**
     * 获取日期统计信息
     */
    List<Map<String, Object>> getDateStatistics(Long deviceId, LocalDate startDate, LocalDate endDate);

    /**
     * 获取正在录制的视频记录
     */
    List<VideoRecord> getRecordingVideos();

    /**
     * 获取设备最新的录制记录
     */
    VideoRecord getLatestRecordByDevice(Long deviceId);

    /**
     * 根据文件路径查询录制记录
     */
    VideoRecord getRecordByFilePath(String filePath);

    /**
     * 批量更新录制状态
     */
    boolean batchUpdateStatus(List<Long> recordIds, String newStatus, String updatedBy);

    /**
     * 获取存储空间统计
     */
    Map<String, Object> getStorageStatistics(Long deviceId);

    /**
     * 清理过期的录制记录
     */
    int cleanupExpiredRecords();

    /**
     * 获取录制记录的播放URL
     */
    String getPlaybackUrl(Long recordId);

    /**
     * 生成视频缩略图
     */
    String generateThumbnail(String videoPath);

    /**
     * 验证录制文件完整性
     */
    boolean validateRecordFile(String filePath);

    /**
     * 统计录制记录数量
     */
    RecordStatistics getRecordStatistics(Long deviceId, LocalDate startDate, LocalDate endDate);

    /**
     * 录制统计信息类
     */
    class RecordStatistics {
        private Long totalRecords;
        private Long completedRecords;
        private Long failedRecords;
        private Long recordingRecords;
        private Long totalFileSize;
        private Long totalDuration;
        private Double successRate;

        // Getters and Setters
        public Long getTotalRecords() {
            return totalRecords;
        }

        public void setTotalRecords(Long totalRecords) {
            this.totalRecords = totalRecords;
        }

        public Long getCompletedRecords() {
            return completedRecords;
        }

        public void setCompletedRecords(Long completedRecords) {
            this.completedRecords = completedRecords;
        }

        public Long getFailedRecords() {
            return failedRecords;
        }

        public void setFailedRecords(Long failedRecords) {
            this.failedRecords = failedRecords;
        }

        public Long getRecordingRecords() {
            return recordingRecords;
        }

        public void setRecordingRecords(Long recordingRecords) {
            this.recordingRecords = recordingRecords;
        }

        public Long getTotalFileSize() {
            return totalFileSize;
        }

        public void setTotalFileSize(Long totalFileSize) {
            this.totalFileSize = totalFileSize;
        }

        public Long getTotalDuration() {
            return totalDuration;
        }

        public void setTotalDuration(Long totalDuration) {
            this.totalDuration = totalDuration;
        }

        public Double getSuccessRate() {
            return successRate;
        }

        public void setSuccessRate(Double successRate) {
            this.successRate = successRate;
        }
    }
} 