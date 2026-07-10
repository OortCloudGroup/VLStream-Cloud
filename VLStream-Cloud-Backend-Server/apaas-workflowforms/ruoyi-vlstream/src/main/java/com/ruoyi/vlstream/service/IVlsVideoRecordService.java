package com.ruoyi.vlstream.service;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.domain.VideoRecord;

import java.util.List;
import java.util.Map;

/**
 * VLS video record service contract used by compatibility controllers.
 */
public interface IVlsVideoRecordService {

    BladePage<VideoRecord> getRecordPage(long current, long size, Long deviceId, String deviceName,
                                         String fileName, String recordStatus, String date);

    VideoRecord getRecord(Long id);

    VideoRecord createRecord(VideoRecord videoRecord);

    VideoRecord updateRecord(Long id, VideoRecord videoRecord);

    boolean deleteRecord(Long id);

    boolean deleteRecords(List<Long> ids);

    VideoRecord startRecording(Long deviceId, String deviceName, Integer duration, String quality);

    Map<String, Object> stopRecording(Long recordId);

    Map<String, Object> getRecordingStatus(Long deviceId);

    Map<String, Object> getRecordingStatistics();

    List<VideoRecord> getDeviceRecords(Long deviceId, String date, Long currentPage, Long pageSize);

    Map<String, Object> getRecordPreview(Long id);
}
