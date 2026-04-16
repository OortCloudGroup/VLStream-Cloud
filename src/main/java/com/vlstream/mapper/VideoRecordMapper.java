package com.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vlstream.entity.VideoRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Video Recording Record Mapper Interface
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Mapper
public interface VideoRecordMapper extends BaseMapper<VideoRecord> {

    /**
     * Query recording records for specified date by device ID
     */
    @Select("SELECT * FROM video_record WHERE deleted = 0 AND device_id = #{deviceId} AND record_date = #{recordDate} ORDER BY record_start_time ASC")
    List<VideoRecord> selectByDeviceAndDate(@Param("deviceId") Long deviceId, @Param("recordDate") LocalDate recordDate);

    /**
     * Query recording records for specified time range by device ID
     */
    @Select("SELECT * FROM video_record WHERE deleted = 0 AND device_id = #{deviceId} AND record_start_time >= #{startTime} AND record_end_time <= #{endTime} ORDER BY record_start_time ASC")
    List<VideoRecord> selectByDeviceAndTimeRange(@Param("deviceId") Long deviceId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * Query recording records in progress
     */
    @Select("SELECT * FROM video_record WHERE deleted = 0 AND record_status = 'recording' ORDER BY record_start_time DESC")
    List<VideoRecord> selectRecordingVideos();

    /**
     * Get latest recording record for device
     */
    @Select("SELECT * FROM video_record WHERE deleted = 0 AND device_id = #{deviceId} ORDER BY record_start_time DESC LIMIT 1")
    VideoRecord selectLatestByDeviceId(@Param("deviceId") Long deviceId);

    /**
     * Query recording record by file path
     */
    @Select("SELECT * FROM video_record WHERE deleted = 0 AND file_path = #{filePath} LIMIT 1")
    VideoRecord selectByFilePath(@Param("filePath") String filePath);
} 