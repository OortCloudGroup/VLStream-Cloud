/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SSH defaults for the remote GPU training server.
 */
@Data
@Component
@ConfigurationProperties(prefix = "vlstream.ssh")
public class VlsSshProperties {

    private String host = "192.168.88.173";

    private Integer port = 22;

    private String username = "oort";

    private String password = "oort301";
}
