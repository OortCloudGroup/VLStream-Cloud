/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.workflow.domain.WorkOrderSynthesis;
import com.ruoyi.workflow.domain.vo.WorkOrderSynthesisVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 综合工单流程Mapper接口
 *
 * @author Lei Chao Qun
 * @date 2025-01-04
 */
@Mapper
public interface WorkOrderSynthesisMapper extends BaseMapperPlus<WorkOrderSynthesisMapper, WorkOrderSynthesis, WorkOrderSynthesisVo> {
    /**
     * 根据父id递归查询子节点
     */
    List<String> selectChildById(@Param("parentId") String  parentId);
}
