/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
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
 * 流程图JSONService接口
 *
 * @author 雷超群
 * @date 2024-11-02
 */
public interface IReModeJsonService extends IService<ReModelJson> {

    /**
     * 查询流程图JSON
     */
    ReModelJsonVo queryById(String modelId);

    /**
     * 查询流程图JSON列表
     */
    TableDataInfo<ReModelJsonVo> queryPageList(ReModeJsonBo bo, PageQuery pageQuery);

    /**
     * 查询流程图JSON列表
     */
    List<ReModelJsonVo> queryList(ReModeJsonBo bo);

    /**
     * 新增流程图JSON
     */
    Boolean insertByBo(ReModeJsonBo bo);

    /**
     * 修改流程图JSON
     */
    Boolean updateByBo(ReModeJsonBo bo);

    /**
     * 校验并批量删除流程图JSON信息
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);
}
