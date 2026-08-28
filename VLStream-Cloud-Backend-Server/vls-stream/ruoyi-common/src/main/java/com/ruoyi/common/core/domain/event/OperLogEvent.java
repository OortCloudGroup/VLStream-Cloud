/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.core.domain.event;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * operationlogevent
 *
 * @author Lion Li
 */

@Data
public class OperLogEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * logprimary key
     */
    private Long operId;

    /**
     * operation
     */
    private String title;

    /**
     * (0 1Add 2Update 3Delete )
     */
    private Integer businessType;

    /**
     * array
     */
    private Integer[] businessTypes;

    /**
     * method
     */
    private String method;

    /**
     *
     */
    private String requestMethod;

    /**
     * operation (0 1 after user 2 user)
     */
    private Integer operatorType;

    /**
     * operation
     */
    private String operName;

    /**
     * department name
     */
    private String deptName;

    /**
     * url
     */
    private String operUrl;

    /**
     * operation
     */
    private String operIp;

    /**
     * operation
     */
    private String operLocation;

    /**
     * parameter
     */
    private String operParam;

    /**
     * parameter
     */
    private String jsonResult;

    /**
     * operation (0 1 )
     */
    private Integer status;

    /**
     *
     */
    private String errorMsg;

    /**
     * operation
     */
    private Date operTime;

}
