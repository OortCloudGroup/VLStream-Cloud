/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.SysLogininfor;

import java.util.List;

/**
 * log info service layer
 *
 * @author Lion Li
 */
public interface ISysLogininforService {


    TableDataInfo<SysLogininfor> selectPageLogininforList(SysLogininfor logininfor, PageQuery pageQuery);

    /**
     * Add log
     *
     * @param logininfor logobject
     */
    void insertLogininfor(SysLogininfor logininfor);

    /**
     * Query logcollection
     *
     * @param logininfor logobject
     * @return recordcollection
     */
    List<SysLogininfor> selectLogininforList(SysLogininfor logininfor);

    /**
     * Batch delete log
     *
     * @param infoIds need to Delete logID
     * @return
     */
    int deleteLogininforByIds(Long[] infoIds);

    /**
     * null / empty log
     */
    void cleanLogininfor();
}
