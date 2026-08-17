package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.util.Date;

@Data
@TableName("vls_mqtt_device")
@EqualsAndHashCode(callSuper = true)
public class MqttDevice extends TenantEntity {
	private String deviceId;
	private String deviceName;
	private String deviceSerial;
	private String deviceModel;
	private String firmwareVersion;
	private String applicationVersion;
	private String rootfsVersion;
	private String faceVersion;
	private String ipAddr;
	private String mac;
	private Boolean online;
	private String onlineReason;
	private Long heartbeatIndex;
	private String lastMessageId;
	private Date lastReportedAt;
	private Date lastHeartbeatTime;
	private String telemetryJson;
	private String serviceStatusJson;
}
