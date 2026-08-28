/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.workflow.domain.WfSynthesis;
import com.ruoyi.workflow.domain.vo.WfSynthesisVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * workflowMapperinterface
 *
 * @author
 * @date 2025-01-04
 */
public interface WfSynthesisMapper extends BaseMapperPlus<WfSynthesisMapper, WfSynthesis, WfSynthesisVo> {

    /**
     * id Query sub node
     */
    List<String > selectChildById(@Param("parentId") String  parentId);
}
