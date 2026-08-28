/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.workflow.domain.bo.ProcessViewLogBo;
import com.ruoyi.workflow.domain.vo.ProcessViewLogVo;

import java.util.Collection;
import java.util.List;

/**
 * workflow logServiceinterface
 *
 * @author lcq
 * @date 2025-08-15
 */
public interface IProcessViewLogService {

    /**
     * Query workflow log
     */
    ProcessViewLogVo queryById(String id);

    /**
     * Query workflow log list
     */
    TableDataInfo<ProcessViewLogVo> queryPageList(ProcessViewLogBo bo, PageQuery pageQuery);

    /**
     * Query workflow log list
     */
    List<ProcessViewLogVo> queryList(ProcessViewLogBo bo);

    /**
     * Add workflow log
     */
    Boolean insertByBo(ProcessViewLogBo bo, SysUser sysUser);

    /**
     * Update workflow log
     */
    Boolean updateByBo(ProcessViewLogBo bo);

    /**
     * Validate Batch delete workflow loginfo
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);

    TableDataInfo<ProcessViewLogVo> queryUserPageList(ProcessViewLogBo bo, PageQuery pageQuery);
}
