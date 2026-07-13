/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
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
 * 流程访问日志Service接口
 *
 * @author lcq
 * @date 2025-08-15
 */
public interface IProcessViewLogService {

    /**
     * 查询流程访问日志
     */
    ProcessViewLogVo queryById(String id);

    /**
     * 查询流程访问日志列表
     */
    TableDataInfo<ProcessViewLogVo> queryPageList(ProcessViewLogBo bo, PageQuery pageQuery);

    /**
     * 查询流程访问日志列表
     */
    List<ProcessViewLogVo> queryList(ProcessViewLogBo bo);

    /**
     * 新增流程访问日志
     */
    Boolean insertByBo(ProcessViewLogBo bo, SysUser sysUser);

    /**
     * 修改流程访问日志
     */
    Boolean updateByBo(ProcessViewLogBo bo);

    /**
     * 校验并批量删除流程访问日志信息
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);

    TableDataInfo<ProcessViewLogVo> queryUserPageList(ProcessViewLogBo bo, PageQuery pageQuery);
}
