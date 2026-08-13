package com.ruoyi.vlstream.test.vlstream.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springblade.core.mp.base.TenantEntity;

import java.util.Date;

@Data
@TableName("vls_mqtt_device_stream")
@EqualsAndHashCode(callSuper = true)
public class MqttDeviceStream extends TenantEntity {
	private Long deviceRowId;
	private String channelId;
	private String streamName;
	private String streamType;
	private String protocol;
	@JsonIgnore
	private String sourceUrl;
	private Boolean isDefault;
	private Boolean available;
	private String zlmApp;
	private String zlmStream;
	@JsonIgnore
	private String zlmProxyKey;
	private Date lastReportTime;
}
