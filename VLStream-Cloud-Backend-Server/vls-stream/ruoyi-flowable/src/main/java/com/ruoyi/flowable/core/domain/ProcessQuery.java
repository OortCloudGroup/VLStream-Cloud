/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.flowable.core.domain;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.*;

/**
 * workflowQuery object
 *
 * @author KonBAI
 * @createTime 2022/6/11 01:15
 */
@Data
public class ProcessQuery {

    /**
     * workflow
     */
    private String processKey;

    /**
     * workflow
     */
    private String processName;

    /**
     * workflow
     */
    private String category;

    /**
     *
     */
    private String state;

    /**
     * whether
     */
    private String showMobile;

    /**
     * workflowcreate timestart
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date proStartBeginTime;

    /**
     * workflowcreate timefinish
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date proStartEndTime;

    /**
     * parameter
     */
    private Map<String, Object> params = new HashMap<>();

    /**
     *
     */
    private String categoryType;

    /**
     * Query full workflow
     */
    private Boolean wfAppAll= false;
    /**
     * Query full workflow
     */
    private Boolean wfSynthesisAll= false;
    /**
     * Query full work orderworkflow
     */
    private Boolean WorkOrderAppAll= false;
    /**
     * Query full work orderworkflow
     */
    private Boolean WorkOrderSynthesisAll= false;
    /**
     *
     */
    List<String> categoryList = new ArrayList<>();
    /**
     * interface
     */
    private String apiPath;
}
