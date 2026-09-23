/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "vlstream.tunnel")
public class VlsTunnelProperties {
    private boolean enabled;
    private String remoteAddress = "";
    private String transport = "noise";
    private String noiseRemotePublicKey = "";
    private String serviceSigningSecret = "";
    private String controlApiToken = "";
    private String gatewayApiToken = "";
    private String gatewayBaseUrl = "";
    private String serverBindHost = "127.0.0.1";
    private int serverPortStart = 61000;
    private int serverPortEnd = 61999;
    private int enrollmentTtlSeconds = 600;
    private int accessSessionTtlSeconds = 10 * 60 * 60;
    private int heartbeatIntervalSeconds = 30;
    private int heartbeatTimeoutSeconds = 90;
}
