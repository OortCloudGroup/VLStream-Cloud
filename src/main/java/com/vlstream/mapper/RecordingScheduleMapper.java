package com.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vlstream.entity.RecordingSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Recording Schedule Mapper Interface
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Mapper
public interface RecordingScheduleMapper extends BaseMapper<RecordingSchedule> {

    /**
     * Query enabled recording schedules by device ID
     */
    @Select("SELECT * FROM recording_schedule " +
            "WHERE deleted = 0 " +
            "AND device_id = #{deviceId} " +
            "AND is_enabled = 1")
    List<RecordingSchedule> selectEnabledByDeviceId(@Param("deviceId") Long deviceId);

    /**
     * Query all enabled recording schedules
     */
    @Select("SELECT * FROM recording_schedule " +
            "WHERE deleted = 0 " +
            "AND is_enabled = 1 " +
            "ORDER BY next_record_time ASC")
    List<RecordingSchedule> selectAllEnabled();

    /**
     * Query recording schedules to execute
     */
    @Select("SELECT * FROM recording_schedule " +
            "WHERE deleted = 0 " +
            "AND is_enabled = 1 " +
            "AND next_record_time <= #{currentTime}")
    List<RecordingSchedule> selectSchedulesToExecute(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Query recording schedules by time strategy ID
     */
    @Select("SELECT * FROM recording_schedule " +
            "WHERE deleted = 0 " +
            "AND time_strategy_id = #{timeStrategyId}")
    List<RecordingSchedule> selectByTimeStrategyId(@Param("timeStrategyId") Long timeStrategyId);

    /**
     * Update recording schedule execution time
     */
    @Update("UPDATE recording_schedule " +
            "SET last_record_time = #{lastRecordTime}, " +
            "next_record_time = #{nextRecordTime}, " +
            "total_records = total_records + 1, " +
            "update_time = NOW() " +
            "WHERE id = #{id} AND deleted = 0")
    int updateExecutionTime(
            @Param("id") Long id,
            @Param("lastRecordTime") LocalDateTime lastRecordTime,
            @Param("nextRecordTime") LocalDateTime nextRecordTime
    );

    /**
     * Increment failed recording count
     */
    @Update("UPDATE recording_schedule " +
            "SET failed_records = failed_records + 1, " +
            "update_time = NOW() " +
            "WHERE id = #{id} AND deleted = 0")
    int incrementFailedRecords(@Param("id") Long id);

    /**
     * Enable/disable recording schedule
     */
    @Update("UPDATE recording_schedule " +
            "SET is_enabled = #{enabled}, " +
            "update_time = NOW(), " +
            "updated_by = #{updatedBy} " +
            "WHERE id = #{id} AND deleted = 0")
    int updateEnabledStatus(
            @Param("id") Long id,
            @Param("enabled") Boolean enabled,
            @Param("updatedBy") String updatedBy
    );

    /**
     * Batch enable/disable recording schedules
     */
    @Update("<script>" +
            "UPDATE recording_schedule " +
            "SET is_enabled = #{enabled}, " +
            "update_time = NOW(), " +
            "updated_by = #{updatedBy} " +
            "WHERE id IN " +
            "<foreach collection=\"ids\" item=\"id\" open=\"(\" separator=\",\" close=\")\">" +
            "#{id}" +
            "</foreach> " +
            "AND deleted = 0 " +
            "</script>")
    int batchUpdateEnabledStatus(
            @Param("ids") List<Long> ids,
            @Param("enabled") Boolean enabled,
            @Param("updatedBy") String updatedBy
    );
} 