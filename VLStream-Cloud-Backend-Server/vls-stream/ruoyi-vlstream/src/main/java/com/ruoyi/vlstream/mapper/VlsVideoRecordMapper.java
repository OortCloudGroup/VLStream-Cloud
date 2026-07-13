/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.vlstream.domain.VideoRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Mapper for VLS video records.
 */
@Mapper
public interface VlsVideoRecordMapper extends BaseMapperPlus<VlsVideoRecordMapper, VideoRecord, VideoRecord> {

    @Select("SELECT * FROM vls_video_record WHERE device_id = #{deviceId} " +
        "AND record_status = 'recording' AND is_deleted = 0 ORDER BY record_start_time DESC LIMIT 1")
    VideoRecord selectLatestRecording(@Param("deviceId") Long deviceId);
}
