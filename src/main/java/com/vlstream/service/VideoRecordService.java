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
 * Video recording record service interface
 *
 * @author VLStream Team
 * @since 1.0.0
 */
public interface VideoRecordService extends IService<VideoRecord> {

    /**
     * Query video recording records with pagination
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
     * Query recording records by device ID and date
     */
    List<VideoRecord> getRecordsByDeviceAndDate(Long deviceId, LocalDate recordDate);

    /**
     * Query recording records by device ID and time range
     */
    List<VideoRecord> getRecordsByDeviceAndTimeRange(Long deviceId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Start video recording
     */
    VideoRecord startRecording(Long deviceId, String deviceName, Integer duration, String quality);

    /**
     * Stop video recording
     */
    boolean stopRecording(Long recordId);

    /**
     * Complete recording (update recording status and file information)
     */
    boolean completeRecording(Long recordId, String filePath, Long fileSize, Integer duration, String thumbnailPath);

    /**
     * Mark recording as failed
     */
    boolean markRecordingFailed(Long recordId, String errorMessage);

    /**
     * Delete recording record (soft delete)
     */
    boolean deleteRecord(Long recordId);

    /**
     * Batch delete recording records
     */
    boolean batchDeleteRecords(List<Long> recordIds);

    /**
     * Get device recording statistics
     */
    List<Map<String, Object>> getDeviceRecordStatistics(Long deviceId, LocalDate startDate, LocalDate endDate);

    /**
     * Get date statistics
     */
    List<Map<String, Object>> getDateStatistics(Long deviceId, LocalDate startDate, LocalDate endDate);

    /**
     * Get currently recording video records
     */
    List<VideoRecord> getRecordingVideos();

    /**
     * Get latest recording record by device
     */
    VideoRecord getLatestRecordByDevice(Long deviceId);

    /**
     * Query recording record by file path
     */
    VideoRecord getRecordByFilePath(String filePath);

    /**
     * Batch update recording status
     */
    boolean batchUpdateStatus(List<Long> recordIds, String newStatus, String updatedBy);

    /**
     * Get storage statistics
     */
    Map<String, Object> getStorageStatistics(Long deviceId);

    /**
     * Clean up expired recording records
     */
    int cleanupExpiredRecords();

    /**
     * Get playback URL for recording record
     */
    String getPlaybackUrl(Long recordId);

    /**
     * Generate video thumbnail
     */
    String generateThumbnail(String videoPath);

    /**
     * Validate recording file integrity
     */
    boolean validateRecordFile(String filePath);

    /**
     * Count recording records
     */
    RecordStatistics getRecordStatistics(Long deviceId, LocalDate startDate, LocalDate endDate);

    /**
     * Recording statistics class
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