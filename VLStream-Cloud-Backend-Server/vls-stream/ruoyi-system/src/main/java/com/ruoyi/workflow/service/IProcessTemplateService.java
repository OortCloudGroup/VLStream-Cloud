/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.workflow.domain.bo.ProcessTemplateBo;
import com.ruoyi.workflow.domain.vo.ProcessTemplateVo;

import java.util.Collection;
import java.util.List;

/**
 * 流程初始化模版Service接口
 *
 * @author lcq
 * @date 2025-01-07
 */
public interface IProcessTemplateService {

    /**
     * 查询流程初始化模版
     */
    ProcessTemplateVo queryById(String id);

    /**
     * 查询流程初始化模版列表
     */
    TableDataInfo<ProcessTemplateVo> queryPageList(ProcessTemplateBo bo, PageQuery pageQuery);

    /**
     * 查询流程初始化模版列表
     */
    List<ProcessTemplateVo> queryList(ProcessTemplateBo bo);

    /**
     * 新增流程初始化模版
     */
    Boolean insertByBo(ProcessTemplateBo bo);

    /**
     * 修改流程初始化模版
     */
    Boolean updateByBo(ProcessTemplateBo bo);

    /**
     * 校验并批量删除流程初始化模版信息
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);
}
