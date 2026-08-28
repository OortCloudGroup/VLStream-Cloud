/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.workflow.domain.ReModelJson;
import com.ruoyi.workflow.domain.bo.ReModeJsonBo;
import com.ruoyi.workflow.domain.vo.ReModelJsonVo;

import java.util.Collection;
import java.util.List;

/**
 * workflow JSONServiceinterface
 *
 * @author
 * @date 2024-11-02
 */
public interface IReModeJsonService extends IService<ReModelJson> {

    /**
     * Query workflow JSON
     */
    ReModelJsonVo queryById(String modelId);

    /**
     * Query workflow JSON list
     */
    TableDataInfo<ReModelJsonVo> queryPageList(ReModeJsonBo bo, PageQuery pageQuery);

    /**
     * Query workflow JSON list
     */
    List<ReModelJsonVo> queryList(ReModeJsonBo bo);

    /**
     * Add workflow JSON
     */
    Boolean insertByBo(ReModeJsonBo bo);

    /**
     * Update workflow JSON
     */
    Boolean updateByBo(ReModeJsonBo bo);

    /**
     * Validate Batch delete workflow JSONinfo
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);
}
