/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.workflow.domain.WorkOrderApp;
import com.ruoyi.workflow.domain.bo.WorkOrderAppBo;
import com.ruoyi.workflow.domain.vo.WorkOrderAppVo;

import java.util.Collection;
import java.util.List;

/**
 * 应用工单分类Service接口
 *
 * @author 雷超群
 * @date 2025-01-04
 */
public interface IWorkOrderAppService extends IService<WorkOrderApp> {

    /**
     * 查询应用工单分类
     */
    WorkOrderAppVo queryById(String appId);

    /**
     * 查询应用工单分类列表
     */
    List<WorkOrderAppVo> queryPageList(WorkOrderAppBo bo, PageQuery pageQuery);

    /**
     * 查询应用工单分类列表
     */
    List<WorkOrderAppVo> queryList(WorkOrderAppBo bo);

    /**
     * 新增应用工单分类
     */
    Boolean insertByBo(WorkOrderAppBo bo);

    /**
     * 修改应用工单分类
     */
    Boolean updateByBo(WorkOrderAppBo bo);

    /**
     * 校验并批量删除应用工单分类信息
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);
}
