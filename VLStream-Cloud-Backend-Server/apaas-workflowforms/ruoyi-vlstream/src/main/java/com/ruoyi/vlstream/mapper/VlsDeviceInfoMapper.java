package com.ruoyi.vlstream.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.vlstream.domain.DeviceInfo;
import lombok.Data;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Mapper for VLS device information.
 */
@Mapper
public interface VlsDeviceInfoMapper extends BaseMapperPlus<VlsDeviceInfoMapper, DeviceInfo, DeviceInfo> {

    @Select("SELECT * FROM vls_device_info WHERE device_id = #{deviceId} AND is_deleted = 0")
    DeviceInfo selectByDeviceId(@Param("deviceId") String deviceId);

    @Select("SELECT DISTINCT device_type FROM vls_device_info " +
        "WHERE device_type IS NOT NULL AND device_type != '' AND is_deleted = 0 ORDER BY device_type")
    List<String> getAllTags();

    @Select("SELECT device_type as type, COUNT(*) as count FROM vls_device_info " +
        "WHERE is_deleted = 0 GROUP BY device_type ORDER BY device_type")
    List<TypeStatistics> getTypeStatistics();

    @Select("SELECT COUNT(*) FROM vls_device_info WHERE device_id = #{deviceId} AND is_deleted = 0")
    int countByDeviceId(@Param("deviceId") String deviceId);

    @Data
    class TypeStatistics {
        private String type;
        private Long count;
    }
}
