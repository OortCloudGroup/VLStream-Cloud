/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.workflow.domain.WfSynthesis;
import com.ruoyi.workflow.domain.bo.WfSynthesisBo;
import com.ruoyi.workflow.domain.vo.WfSynthesisVo;

import java.util.Collection;
import java.util.List;

/**
 * 综合通用流程Service接口
 *
 * @author 雷超群
 * @date 2025-01-04
 */
public interface IWfSynthesisService extends IService<WfSynthesis> {

    /**
     * 查询综合通用流程
     */
    WfSynthesisVo queryById(String synthesisId);


    /**
     * 查询综合通用流程列表
     */
    List<WfSynthesisVo> queryList(WfSynthesisBo bo);

    /**
     * 查询全部综合通用流程
     */
    List<WfSynthesisVo> queryListAll(String categoryName);

    /**
     * 新增综合通用流程
     */
    Boolean insertByBo(WfSynthesisBo bo);

    /**
     * 修改综合通用流程
     */
    Boolean updateByBo(WfSynthesisBo bo);

    /**
     * 校验并批量删除综合通用流程信息
     */
    Boolean deleteWithValidByIds(Collection<String> ids, Boolean isValid);
    /**
     * 根据父id递归查询子节点
     */
    List<String > selectChildById(String  parentId);
}
