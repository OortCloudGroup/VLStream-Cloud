/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain.dto;

import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 、
 * @author KonBAI
 * @createTime 2022/3/10 00:12
 */
@Data
public class WfNextDto implements Serializable {

    private String type;

    private String vars;

    private List<SysUser> userList;

    private List<SysRole> roleList;
}
