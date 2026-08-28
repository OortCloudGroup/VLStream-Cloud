/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.workflow.domain.WfApp;
import com.ruoyi.workflow.domain.bo.WfAppBo;
import com.ruoyi.workflow.domain.vo.WfAppVo;

import java.util.Collection;
import java.util.List;

/**
 * workflowServiceinterface
 *
 * @author
 * @date 2025-01-04
 */
public interface IWfAppService extends IService<WfApp> {

    /**
     * Query workflow
     */
    WfAppVo queryById(String appId);

    /**
     * Query workflow list
     */
    List<WfAppVo> queryPageList(WfAppBo bo, PageQuery pageQuery);

    /**
     * Query workflow list
     */
    List<WfAppVo> queryList(WfAppBo bo);

    /**
     * Add workflow
     */
    WfApp insertByBo(WfAppBo bo);

    /**
     * Update workflow
     */
    Boolean updateByBo(WfAppBo bo);

    /**
     * Validate Batch delete workflowinfo
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);
}
