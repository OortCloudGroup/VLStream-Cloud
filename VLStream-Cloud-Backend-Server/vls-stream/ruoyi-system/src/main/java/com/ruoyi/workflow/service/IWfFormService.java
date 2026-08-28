/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.workflow.domain.WfForm;
import com.ruoyi.workflow.domain.bo.WfFormBo;
import com.ruoyi.workflow.domain.vo.WfFormVo;

import java.util.Collection;
import java.util.List;

/**
 * form
 *
 * @author KonBAI
 * @createTime 2022/3/7 22:07
 */
public interface IWfFormService extends IService<WfForm> {
    /**
     * Query workflowform
     *
     * @param formId workflowformID
     * @return workflowform
     */
    WfFormVo queryById(String formId);

    /**
     * Query workflowform list
     *
     * @param bo workflowform
     * @return workflowformcollection
     */
    TableDataInfo<WfFormVo>  queryPageList(WfFormBo bo, PageQuery pageQuery);

    /**
     * Query workflowform list
     *
     * @param bo workflowform
     * @return workflowformcollection
     */
    List<WfFormVo> queryList(WfFormBo bo);

    /**
     * Add workflowform
     *
     * @param bo workflowform
     * @return
     */
    WfForm insertForm(WfFormBo bo);

    /**
     * Update workflowform
     *
     * @param bo workflowform
     * @return
     */
    int updateForm(WfFormBo bo);

    /**
     * Batch delete workflowform
     *
     * @param formIds need to Delete workflowformID
     * @return
     */
    Boolean deleteWithValidByIds(Collection<Long> formIds);
}
