package com.ruoyi.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.domain.VideoRecord;
import com.ruoyi.vlstream.mapper.VlsVideoRecordMapper;
import com.ruoyi.vlstream.service.IVlsVideoRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for the VLS video record frontend compatibility surface.
 */
@Service
@RequiredArgsConstructor
public class VlsVideoRecordServiceImpl implements IVlsVideoRecordService {

    private static final String DEFAULT_TENANT_ID = "000000";
    private static final String DEFAULT_FORMAT = "mp4";
    private static final String RECORDING_STATUS = "recording";
    private static final String COMPLETED_STATUS = "completed";

    private final VlsVideoRecordMapper videoRecordMapper;

    @Override
    public BladePage<VideoRecord> getRecordPage(long current, long size, Long deviceId, String deviceName,
                                                String fileName, String recordStatus, String date) {
        Page<VideoRecord> page = new Page<VideoRecord>(current, size);
        LambdaQueryWrapper<VideoRecord> queryWrapper = buildRecordQuery(deviceId, deviceName, fileName, recordStatus, date);
        queryWrapper.orderByDesc(VideoRecord::getRecordStartTime);
        Page<VideoRecord> result = videoRecordMapper.selectPage(page, queryWrapper);
        for (VideoRecord record : result.getRecords()) {
            applyAliases(record);
        }
        return BladePage.of(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent());
    }

    @Override
    public VideoRecord getRecord(Long id) {
        return applyAliases(videoRecordMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VideoRecord createRecord(VideoRecord videoRecord) {
        if (videoRecord == null) {
            throw new IllegalArgumentException("Video record is required");
        }
        normalizeForSave(videoRecord, false);
        videoRecordMapper.insert(videoRecord);
        updateUrlAfterInsert(videoRecord);
        return applyAliases(videoRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VideoRecord updateRecord(Long id, VideoRecord videoRecord) {
        VideoRecord existing = videoRecordMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Video record does not exist");
        }
        copyNonNull(videoRecord, existing);
        existing.setId(id);
        normalizeForSave(existing, COMPLETED_STATUS.equals(existing.getRecordStatus()) ? false : RECORDING_STATUS.equals(existing.getRecordStatus()));
        videoRecordMapper.updateById(existing);
        return applyAliases(existing);
    }

    @Override
    public boolean deleteRecord(Long id) {
        return videoRecordMapper.deleteById(id) > 0;
    }

    @Override
    public boolean deleteRecords(List<Long> ids) {
        return ids != null && !ids.isEmpty() && videoRecordMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VideoRecord startRecording(Long deviceId, String deviceName, Integer duration, String quality) {
        VideoRecord videoRecord = new VideoRecord();
        videoRecord.setDeviceId(deviceId);
        videoRecord.setDeviceName(StringUtils.hasText(deviceName) ? deviceName.trim() : "Device");
        videoRecord.setDuration(normalizeDuration(duration));
        videoRecord.setRecordStatus(RECORDING_STATUS);
        normalizeForSave(videoRecord, true);
        videoRecordMapper.insert(videoRecord);
        updateUrlAfterInsert(videoRecord);
        return applyAliases(videoRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> stopRecording(Long recordId) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        VideoRecord record = videoRecordMapper.selectById(recordId);
        if (record == null) {
            result.put("success", false);
            result.put("message", "Video record does not exist");
            return result;
        }
        java.util.Date now = new java.util.Date();
        record.setRecordEndTime(now);
        record.setRecordStatus(COMPLETED_STATUS);
        record.setDuration(calculateDuration(record.getRecordStartTime(), now));
        record.setUpdateTime(now);
        videoRecordMapper.updateById(record);

        result.put("success", true);
        result.put("id", record.getId());
        result.put("recordId", record.getId());
        result.put("status", COMPLETED_STATUS);
        result.put("duration", record.getDuration());
        return result;
    }

    @Override
    public Map<String, Object> getRecordingStatus(Long deviceId) {
        Map<String, Object> result = new LinkedHashMap<String, Object>();
        VideoRecord recording = deviceId == null ? null : videoRecordMapper.selectLatestRecording(deviceId);
        result.put("isRecording", recording != null);
        result.put("recording", recording != null);
        result.put("status", recording == null ? COMPLETED_STATUS : RECORDING_STATUS);
        result.put("recordId", recording == null ? null : recording.getId());
        result.put("id", recording == null ? null : recording.getId());
        result.put("record", applyAliases(recording));
        return result;
    }

    @Override
    public Map<String, Object> getRecordingStatistics() {
        Map<String, Object> statistics = new LinkedHashMap<String, Object>();
        long total = countByStatus(null);
        long recording = countByStatus(RECORDING_STATUS);
        long completed = countByStatus(COMPLETED_STATUS);
        long failed = countByStatus("failed");
        statistics.put("total", total);
        statistics.put("totalCount", total);
        statistics.put("recording", recording);
        statistics.put("recordingCount", recording);
        statistics.put("completed", completed);
        statistics.put("completedCount", completed);
        statistics.put("failed", failed);
        statistics.put("failedCount", failed);
        return statistics;
    }

    @Override
    public List<VideoRecord> getDeviceRecords(Long deviceId, String date, Long currentPage, Long pageSize) {
        if (deviceId == null) {
            return Collections.emptyList();
        }
        long current = currentPage == null || currentPage < 1 ? 1L : currentPage;
        long size = pageSize == null || pageSize < 1 ? 100L : pageSize;
        Page<VideoRecord> page = new Page<VideoRecord>(current, size);
        LambdaQueryWrapper<VideoRecord> queryWrapper = buildRecordQuery(deviceId, null, null, null, date);
        queryWrapper.orderByAsc(VideoRecord::getRecordStartTime);
        List<VideoRecord> records = videoRecordMapper.selectPage(page, queryWrapper).getRecords();
        for (VideoRecord record : records) {
            applyAliases(record);
        }
        return records;
    }

    @Override
    public Map<String, Object> getRecordPreview(Long id) {
        Map<String, Object> preview = new LinkedHashMap<String, Object>();
        VideoRecord record = getRecord(id);
        if (record == null) {
            return preview;
        }
        preview.put("id", record.getId());
        preview.put("recordId", record.getId());
        preview.put("fileName", record.getFileName());
        preview.put("filePath", record.getFilePath());
        preview.put("url", record.getUrl());
        preview.put("thumbnailPath", record.getThumbnailPath());
        preview.put("recordStartTime", record.getRecordStartTime());
        preview.put("recordEndTime", record.getRecordEndTime());
        preview.put("recordStatus", record.getRecordStatus());
        return preview;
    }

    private LambdaQueryWrapper<VideoRecord> buildRecordQuery(Long deviceId, String deviceName, String fileName,
                                                            String recordStatus, String date) {
        LambdaQueryWrapper<VideoRecord> queryWrapper = new LambdaQueryWrapper<VideoRecord>();
        if (deviceId != null) {
            queryWrapper.eq(VideoRecord::getDeviceId, deviceId);
        }
        if (StringUtils.hasText(deviceName)) {
            queryWrapper.like(VideoRecord::getDeviceName, deviceName.trim());
        }
        if (StringUtils.hasText(fileName)) {
            queryWrapper.like(VideoRecord::getFileName, fileName.trim());
        }
        if (StringUtils.hasText(recordStatus)) {
            queryWrapper.eq(VideoRecord::getRecordStatus, recordStatus.trim());
        }
        Date recordDate = parseSqlDate(date);
        if (recordDate != null) {
            queryWrapper.eq(VideoRecord::getRecordDate, recordDate);
        }
        return queryWrapper;
    }

    private long countByStatus(String recordStatus) {
        LambdaQueryWrapper<VideoRecord> queryWrapper = new LambdaQueryWrapper<VideoRecord>();
        if (StringUtils.hasText(recordStatus)) {
            queryWrapper.eq(VideoRecord::getRecordStatus, recordStatus);
        }
        return videoRecordMapper.selectCount(queryWrapper);
    }

    private void normalizeForSave(VideoRecord record, boolean recording) {
        java.util.Date now = new java.util.Date();
        if (!StringUtils.hasText(record.getTenantId())) {
            record.setTenantId(DEFAULT_TENANT_ID);
        }
        if (!StringUtils.hasText(record.getStream())) {
            record.setStream(record.getDeviceId() == null ? "manual" : "device-" + record.getDeviceId());
        }
        if (!StringUtils.hasText(record.getDeviceName())) {
            record.setDeviceName("Device");
        }
        if (record.getDuration() == null || record.getDuration() < 0) {
            record.setDuration(0);
        }
        if (!StringUtils.hasText(record.getFileName())) {
            record.setFileName(defaultFileName(record.getRecordStartTime() == null ? now : record.getRecordStartTime()));
        }
        if (!StringUtils.hasText(record.getFilePath())) {
            record.setFilePath(defaultFilePath(record));
        }
        if (record.getFileSize() == null) {
            record.setFileSize(0L);
        }
        if (!StringUtils.hasText(record.getFormat())) {
            record.setFormat(DEFAULT_FORMAT);
        }
        if (record.getRecordStartTime() == null) {
            record.setRecordStartTime(now);
        }
        if (record.getRecordEndTime() == null) {
            long durationMillis = normalizeDuration(record.getDuration()) * 1000L;
            record.setRecordEndTime(new java.util.Date(record.getRecordStartTime().getTime() + durationMillis));
        }
        if (record.getRecordDate() == null) {
            record.setRecordDate(toSqlDate(record.getRecordStartTime()));
        }
        if (!StringUtils.hasText(record.getRecordStatus())) {
            record.setRecordStatus(recording ? RECORDING_STATUS : COMPLETED_STATUS);
        }
        if (record.getStatus() == null) {
            record.setStatus(1);
        }
        if (record.getIsDeleted() == null) {
            record.setIsDeleted(0);
        }
        if (record.getCreateTime() == null) {
            record.setCreateTime(now);
        }
        record.setUpdateTime(now);
    }

    private void updateUrlAfterInsert(VideoRecord record) {
        if (record.getId() == null || StringUtils.hasText(record.getUrl())) {
            return;
        }
        record.setUrl("/vlsVideoRecord/" + record.getId() + "/preview");
        videoRecordMapper.updateById(record);
    }

    private void copyNonNull(VideoRecord source, VideoRecord target) {
        if (source == null) {
            return;
        }
        target.setTenantId(source.getTenantId() == null ? target.getTenantId() : source.getTenantId());
        target.setStream(source.getStream() == null ? target.getStream() : source.getStream());
        target.setDeviceId(source.getDeviceId() == null ? target.getDeviceId() : source.getDeviceId());
        target.setDeviceName(source.getDeviceName() == null ? target.getDeviceName() : source.getDeviceName());
        target.setFileName(source.getFileName() == null ? target.getFileName() : source.getFileName());
        target.setFilePath(source.getFilePath() == null ? target.getFilePath() : source.getFilePath());
        target.setFileSize(source.getFileSize() == null ? target.getFileSize() : source.getFileSize());
        target.setUrl(source.getUrl() == null ? target.getUrl() : source.getUrl());
        target.setDuration(source.getDuration() == null ? target.getDuration() : source.getDuration());
        target.setFormat(source.getFormat() == null ? target.getFormat() : source.getFormat());
        target.setRecordStartTime(source.getRecordStartTime() == null ? target.getRecordStartTime() : source.getRecordStartTime());
        target.setRecordEndTime(source.getRecordEndTime() == null ? target.getRecordEndTime() : source.getRecordEndTime());
        target.setRecordDate(source.getRecordDate() == null ? target.getRecordDate() : source.getRecordDate());
        target.setRecordStatus(source.getRecordStatus() == null ? target.getRecordStatus() : source.getRecordStatus());
        target.setThumbnailPath(source.getThumbnailPath() == null ? target.getThumbnailPath() : source.getThumbnailPath());
        target.setCreateUser(source.getCreateUser() == null ? target.getCreateUser() : source.getCreateUser());
        target.setCreateDept(source.getCreateDept() == null ? target.getCreateDept() : source.getCreateDept());
        target.setCreateTime(source.getCreateTime() == null ? target.getCreateTime() : source.getCreateTime());
        target.setUpdateUser(source.getUpdateUser() == null ? target.getUpdateUser() : source.getUpdateUser());
        target.setUpdateTime(source.getUpdateTime() == null ? target.getUpdateTime() : source.getUpdateTime());
        target.setStatus(source.getStatus() == null ? target.getStatus() : source.getStatus());
        target.setIsDeleted(source.getIsDeleted() == null ? target.getIsDeleted() : source.getIsDeleted());
    }

    private VideoRecord applyAliases(VideoRecord record) {
        if (record != null) {
            record.setRecordId(record.getId());
        }
        return record;
    }

    private int normalizeDuration(Integer duration) {
        return duration == null || duration < 0 ? 0 : duration;
    }

    private int calculateDuration(java.util.Date start, java.util.Date end) {
        if (start == null || end == null || end.before(start)) {
            return 0;
        }
        return (int) ((end.getTime() - start.getTime()) / 1000L);
    }

    private Date parseSqlDate(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Date.valueOf(LocalDate.parse(text.trim()));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private Date toSqlDate(java.util.Date value) {
        LocalDate localDate = value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return Date.valueOf(localDate);
    }

    private String defaultFileName(java.util.Date startTime) {
        return "rec_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(startTime) + "." + DEFAULT_FORMAT;
    }

    private String defaultFilePath(VideoRecord record) {
        String device = record.getDeviceId() == null ? "manual" : String.valueOf(record.getDeviceId());
        return "recordings/device-" + device + "/" + record.getFileName();
    }
}
