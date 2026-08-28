/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

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
