package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

public interface MqttDeviceMessageMapper {
	@Insert("INSERT IGNORE INTO vls_mqtt_device_message "
		+ "(tenant_id, device_id, message_id, reported_at, received_at) "
		+ "VALUES (#{tenantId}, #{deviceId}, #{messageId}, #{reportedAt}, #{receivedAt})")
	int insertIgnore(@Param("tenantId") String tenantId,
		@Param("deviceId") String deviceId,
		@Param("messageId") String messageId,
		@Param("reportedAt") Date reportedAt,
		@Param("receivedAt") Date receivedAt);

	@Delete("DELETE FROM vls_mqtt_device_message WHERE received_at < #{before}")
	@InterceptorIgnore(tenantLine = "true")
	int deleteBefore(@Param("before") Date before);
}
