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
 * 视频录制记录 Mapper 接口
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Mapper
public interface VideoRecordMapper extends BaseMapper<VideoRecord> {

    /**
     * 根据设备ID查询指定日期的录制记录
     */
    @Select("SELECT * FROM video_record WHERE deleted = 0 AND device_id = #{deviceId} AND record_date = #{recordDate} ORDER BY record_start_time ASC")
    List<VideoRecord> selectByDeviceAndDate(@Param("deviceId") Long deviceId, @Param("recordDate") LocalDate recordDate);

    /**
     * 根据设备ID查询指定时间范围的录制记录
     */
    @Select("SELECT * FROM video_record WHERE deleted = 0 AND device_id = #{deviceId} AND record_start_time >= #{startTime} AND record_end_time <= #{endTime} ORDER BY record_start_time ASC")
    List<VideoRecord> selectByDeviceAndTimeRange(@Param("deviceId") Long deviceId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    /**
     * 查询正在录制的记录
     */
    @Select("SELECT * FROM video_record WHERE deleted = 0 AND record_status = 'recording' ORDER BY record_start_time DESC")
    List<VideoRecord> selectRecordingVideos();

    /**
     * 获取设备最新的录制记录
     */
    @Select("SELECT * FROM video_record WHERE deleted = 0 AND device_id = #{deviceId} ORDER BY record_start_time DESC LIMIT 1")
    VideoRecord selectLatestByDeviceId(@Param("deviceId") Long deviceId);

    /**
     * 根据文件路径查询录制记录
     */
    @Select("SELECT * FROM video_record WHERE deleted = 0 AND file_path = #{filePath} LIMIT 1")
    VideoRecord selectByFilePath(@Param("filePath") String filePath);
} 