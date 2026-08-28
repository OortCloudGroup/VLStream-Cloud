/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.common.core.domain.event;

import lombok.Data;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;

/**
 * event
 *
 * @author Lion Li
 */

@Data
public class LogininforEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * user
     */
    private String username;

    /**
     * 0successfully 1failed
     */
    private String status;

    /**
     * prompt / tip
     */
    private String message;

    /**
     *
     */
    private HttpServletRequest request;

    /**
     * parameter
     */
    private Object[] args;

}
