/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.workflow.domain.WfFormApp;
import com.ruoyi.workflow.domain.bo.WfFormAppBo;
import com.ruoyi.workflow.domain.vo.WfFormAppVo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * form Serviceinterface
 *
 * @author
 * @date 2025-04-26
 */
public interface IWfFormAppService extends IService<WfFormApp> {

    /**
     * Query form
     */
    WfFormAppVo queryById(String categoryId);

    /**
     * Query form list
     */
    TableDataInfo<WfFormAppVo> queryPageList(WfFormAppBo bo, PageQuery pageQuery);

    /**
     * Query form list
     */
    Optional<List<WfFormAppVo>> queryList(WfFormAppBo bo);

    /**
     * Add form
     */
    WfFormApp insertByBo(WfFormAppBo bo);

    /**
     * Update form
     */
    Boolean updateByBo(WfFormAppBo bo);

    /**
     * Validate Batch delete form info
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);
}
