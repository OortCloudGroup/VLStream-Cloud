/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.web.controller.compat;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Deployment-level defaults for work orders created from migrated location events.
 * Tenant, user, process, application, and automatic-dispatch choices remain database settings.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "vls.location-task.workflow")
public class LocationTaskWorkflowProperties {

    private static final String DEFAULT_JUMP_PARAMS = "task={\"id\":\"{{event_id}}\"}";

    /** Default application package used by the work-order detail link. */
    private String appPackage = "com.oort-event.demo";

    /** Default application-relative path used by the work-order detail link. */
    private String jumpPath = "/event-detail";

    /** Default work-order detail parameter template; the event placeholder is replaced at runtime. */
    private String jumpParams = DEFAULT_JUMP_PARAMS;

    /**
     * Return the configured parameter template or the Go-compatible default when the environment value is blank.
     */
    public String getJumpParams() {
        return jumpParams == null || jumpParams.trim().isEmpty() ? DEFAULT_JUMP_PARAMS : jumpParams;
    }
}
