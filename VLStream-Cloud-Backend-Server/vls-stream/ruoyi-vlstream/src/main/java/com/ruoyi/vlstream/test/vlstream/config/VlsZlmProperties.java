package com.ruoyi.vlstream.test.vlstream.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "vlstream.zlm")
public class VlsZlmProperties {
	private boolean enabled = true;
	private String internalBaseUrl = "http://127.0.0.1";
	private String publicBaseUrl = "http://127.0.0.1";
	private String secret = "";
	private String app = "vlstream";
	private int connectTimeoutMillis = 3000;
	private int readTimeoutMillis = 10000;
	private int proxyTimeoutSeconds = 10;
}
