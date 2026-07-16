/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * ssh配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "vlstream.ssh")
public class VlsSshProperties {

	private String host;

	private Integer port;

	private String username;

	private String password;

}
