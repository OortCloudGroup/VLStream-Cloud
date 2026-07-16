/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.domain.VideoRecord;

import java.util.List;
import java.util.Map;
import java.util.Date;

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

    List<VideoRecord> listPlaybackRecords(Long deviceId, Date startTime, Date endTime);

    Map<Integer, List<Integer>> getTimelineCalendar(Long deviceId, Integer year);

    List<VideoRecord> listDayRecords(Long deviceId, String recordDate);

    List<VideoRecord> listRecords(Long deviceId, String deviceName, String fileName, String recordStatus, String date);
}
