/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.flowable.utils;

import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.flowable.common.constant.TaskConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 工作流任务工具类
 *
 * @author konbai
 * @createTime 2022/4/24 12:42
 */
public class TaskUtils {

    public static String getUserId() {
        return String.valueOf(LoginHelper.getUserId());
    }

    /**
     * 获取用户组信息
     *
     * @return candidateGroup
     */
    public static List<String> getCandidateGroup() {
        List<String> list = new ArrayList<>();
        LoginUser user = LoginHelper.getLoginUser();
        if (ObjectUtil.isNotNull(user)) {
            if (ObjectUtil.isNotEmpty(user.getRoles())) {
                user.getRoles().forEach(role -> list.add(TaskConstants.ROLE_GROUP_PREFIX + role.getRoleId()));
            }
            if (ObjectUtil.isNotNull(user.getDeptId())) {
                list.add(TaskConstants.DEPT_GROUP_PREFIX + user.getDeptId());
            }
        }
        return list;
    }

    /**
     * 获取用户组信息
     *
     * @return candidateGroup
     */
    public static List<String> getCandidateGroup(SysUser sysUser) {
        List<String> list = new ArrayList<>();
        if (sysUser == null) {
            throw new RuntimeException("未获取到用户组信息");
        }
        if (ObjectUtil.isNotEmpty(sysUser.getRoles())) {
            sysUser.getRoles().forEach(role -> list.add(TaskConstants.ROLE_GROUP_PREFIX + role.getRoleId()));
        }
        if (StringUtils.isNotBlank(sysUser.getDeptId())) {
            list.add(TaskConstants.DEPT_GROUP_PREFIX + sysUser.getDeptId());
        }
        if (StringUtils.isNotBlank(sysUser.getJobId())) {
            String[] jobs = sysUser.getJobId().split(",");
            Arrays.stream(jobs).forEach(job -> list.add(TaskConstants.JOB_GROUP_PREFIX + job));
        }
        if (StringUtils.isNotBlank(sysUser.getPostId())) {
            String[] posts = sysUser.getPostId().split(",");
            Arrays.stream(posts).forEach(post -> list.add(TaskConstants.POST_GROUP_PREFIX + post));
        }
        return list;
    }
}
