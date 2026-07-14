/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.workflow.domain.WfSynthesis;
import com.ruoyi.workflow.domain.vo.WfSynthesisVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 综合通用流程Mapper接口
 *
 * @author 雷超群
 * @date 2025-01-04
 */
public interface WfSynthesisMapper extends BaseMapperPlus<WfSynthesisMapper, WfSynthesis, WfSynthesisVo> {

    /**
     * 根据父id递归查询子节点
     */
    List<String > selectChildById(@Param("parentId") String  parentId);
}
