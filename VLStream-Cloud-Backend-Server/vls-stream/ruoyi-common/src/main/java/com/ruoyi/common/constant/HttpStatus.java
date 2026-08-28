/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.common.constant;

/**
 *
 *
 * @author Lion Li
 */
public interface HttpStatus {

    /**
     * operationsuccessfully
     */
    int SUCCESS = 200;
    /**
     * object successfully
     */
    int CREATED = 201;

    /**
     * already
     */
    int ACCEPTED = 202;

    /**
     * operation already Execute successfully, is data
     */
    int NO_CONTENT = 204;

    /**
     * already
     */
    int MOVED_PERM = 301;

    /**
     *
     */
    int SEE_OTHER = 303;

    /**
     * Update
     */
    int NOT_MODIFIED = 304;

    /**
     * parameter ( , )
     */
    int BAD_REQUEST = 400;

    /**
     * not
     */
    int UNAUTHORIZED = 401;

    /**
     * ,
     */
    int FORBIDDEN = 403;

    /**
     * , service not
     */
    int NOT_FOUND = 404;

    /**
     * http method
     */
    int BAD_METHOD = 405;

    /**
     * ,
     */
    int CONFLICT = 409;

    /**
     * data,
     */
    int UNSUPPORTED_TYPE = 415;

    /**
     *
     */
    int ERROR = 500;

    /**
     * interface not
     */
    int NOT_IMPLEMENTED = 501;

    /**
     *
     */
    int WARN = 601;
}
