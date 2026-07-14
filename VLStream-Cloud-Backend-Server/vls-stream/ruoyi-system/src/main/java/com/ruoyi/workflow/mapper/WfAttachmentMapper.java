/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.workflow.domain.WfAttachment;
import com.ruoyi.workflow.domain.WfForm;
import com.ruoyi.workflow.domain.vo.WfAttachmentVo;
import com.ruoyi.workflow.domain.vo.WfFormVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 流程附件Mapper接口
 *
 * @author KonBAI
 * @createTime 2022/3/7 22:07
 */
public interface WfAttachmentMapper extends BaseMapperPlus<WfAttachmentMapper, WfAttachment, WfAttachmentVo> {

}
