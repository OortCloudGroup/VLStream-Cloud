/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.SysOperLog;

import java.util.List;

/**
 * operationlog service layer
 *
 * @author Lion Li
 */
public interface ISysOperLogService {

    TableDataInfo<SysOperLog> selectPageOperLogList(SysOperLog operLog, PageQuery pageQuery);

    /**
     * Add operationlog
     *
     * @param operLog operationlogobject
     */
    void insertOperlog(SysOperLog operLog);

    /**
     * Query operationlogcollection
     *
     * @param operLog operationlogobject
     * @return operationlogcollection
     */
    List<SysOperLog> selectOperLogList(SysOperLog operLog);

    /**
     * Batch delete operationlog
     *
     * @param operIds need to Delete operationlogID
     * @return
     */
    int deleteOperLogByIds(Long[] operIds);

    /**
     * Query operationlog
     *
     * @param operId operationID
     * @return operationlogobject
     */
    SysOperLog selectOperLogById(Long operId);

    /**
     * null / empty operationlog
     */
    void cleanOperLog();
}
