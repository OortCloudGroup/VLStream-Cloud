package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.MqttDevice;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

public interface MqttDeviceMapper extends BaseMapper<MqttDevice> {

	/** Narrow cross-tenant lookup used only to establish trusted MQTT tenant context. */
	@InterceptorIgnore(tenantLine = "true")
	@Select("SELECT DISTINCT tenant_id FROM vls_mqtt_device "
		+ "WHERE device_id = #{deviceId} AND is_deleted = 0 "
		+ "AND tenant_id IS NOT NULL AND tenant_id <> '' LIMIT 2")
	List<String> selectTenantIdsByDeviceId(@Param("deviceId") String deviceId);

	/** Global infrastructure update; every predicate is explicit and no tenant data is returned. */
	@InterceptorIgnore(tenantLine = "true")
	@Update("UPDATE vls_mqtt_device SET online = 0, online_reason = 'heartbeat_timeout', "
		+ "update_time = #{now} WHERE is_deleted = 0 AND online = 1 "
		+ "AND last_heartbeat_time < #{expiredBefore}")
	int markExpiredDevicesOffline(@Param("expiredBefore") Date expiredBefore,
								 @Param("now") Date now);
}
