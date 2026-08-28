/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.workflow.domain.vo;

import com.ruoyi.common.core.domain.entity.SysDeptView;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.domain.SysUserRoleView;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class WfUserTaskInfoVo implements Serializable {
    private String userTaskId;
    private String userTaskName;
    /**
     * will /
     */
    private String multiInstanceType;
    private List<SysUser> candidateUsers;
    private List<SysDeptView> candidateDeptGroups;
    private List<SysUserRoleView> candidateRoleGroups;
}
