/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.config;

import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * item relatedconfiguration
 *
 * @author Lion Li
 */

@Data
@Component
@ConfigurationProperties(prefix = "ruoyi")
public class RuoYiConfig {

    /**
     * item
     */
    private String name;

    /**
     *
     */
    private String version;

    /**
     *
     */
    private String copyrightYear;

    /**
     * instance
     */
    private boolean demoEnabled;

    /**
     * Load
     */
    private boolean cacheLazy;

    /**
     * Get
     */
    @Getter
    private static boolean addressEnabled;

    public void setAddressEnabled(boolean addressEnabled) {
        RuoYiConfig.addressEnabled = addressEnabled;
    }

}
