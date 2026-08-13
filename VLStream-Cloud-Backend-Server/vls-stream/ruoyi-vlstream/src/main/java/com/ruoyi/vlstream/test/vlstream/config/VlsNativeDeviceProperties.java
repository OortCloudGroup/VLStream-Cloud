package com.ruoyi.vlstream.test.vlstream.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "vlstream.native-device")
public class VlsNativeDeviceProperties {
	private String defaultTenantId = "000000";
	private long offlineTimeoutSeconds = 180L;
	private long messageRetentionHours = 24L;
}
