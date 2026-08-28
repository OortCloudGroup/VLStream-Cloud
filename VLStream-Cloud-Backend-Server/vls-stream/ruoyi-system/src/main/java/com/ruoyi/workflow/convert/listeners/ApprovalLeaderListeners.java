/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.listeners;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.interceptor.AuthorizationInterceptor;
import com.ruoyi.common.utils.ApiHeaderUtil;
import com.ruoyi.common.utils.OkHttpClientHolder;
import com.ruoyi.system.service.impl.SysUserServiceImpl;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ApprovalLeaderListeners implements TaskListener, ApplicationContextAware {
    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApprovalLeaderListeners.applicationContext = applicationContext;
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        try {
            // 1. Get all need to Bean and
            ApplicationContext ctx = applicationContext;
            SysUserServiceImpl userService = ctx.getBean(SysUserServiceImpl.class);
            Environment env = ctx.getBean(Environment.class);
            ObjectMapper mapper = new ObjectMapper();
//            String authUrlPrefix          = env.getProperty("platform.authUrl");
            String authUrlPrefix = null;
            String token = AuthorizationInterceptor.getToken();

            // 2. Parse assignee (" before - ")
            String assignee = delegateTask.getAssignee();
            int dashIndex = assignee.indexOf('-');
            String prefix = assignee.substring(0, dashIndex);   // "job" "post"
            int level = Integer.parseInt(assignee.substring(dashIndex + 1));

            // 3. from workflow variable id value
            String jobId = Optional.ofNullable(delegateTask.getVariable("jobId"))
                                   .map(Object::toString).orElse("");
            String postId = Optional.ofNullable(delegateTask.getVariable("postId"))
                                    .map(Object::toString).orElse("");

            // 4. before Process
            if ("job".equals(prefix) && StringUtils.isNotBlank(jobId)) {
                handleApproval(delegateTask, userService, mapper,
                    authUrlPrefix, token, "job", jobId, level);
            } else if ("post".equals(prefix) && StringUtils.isNotBlank(postId)) {
                handleApproval(delegateTask, userService, mapper,
                    authUrlPrefix, token, "post", postId, level);
            } else {
                delegateTask.setAssignee(null);
            }
        } catch (Exception e) {
            throw new RuntimeException("notify 方法执行异常", e);
        }
    }

    /**
     * Process : approval layer , info
     *
     * @param task Flowable task
     * @param userService userservice
     * @param mapper       Jackson ObjectMapper
     * @param urlPrefix before
     * @param token Token
     * @param groupType "job" "post"
     * @param idValue jobId postId
     * @param currentLevel layer
     */
    private void handleApproval(DelegateTask task,
                                SysUserServiceImpl userService,
                                ObjectMapper mapper,
                                String urlPrefix,
                                String token,
                                String groupType,
                                String idValue,
                                int currentLevel) throws IOException {
        OkHttpClient client = OkHttpClientHolder.CLIENT;
        MediaType jsonType = MediaType.parse("application/json");

        // —— : Query approval layer —— //
        String urlLevel = urlPrefix + "admin/v1/" + groupType + "Level";
        ObjectNode reqLev = mapper.createObjectNode()
                                  .put("accessToken", token)
                                  .put(groupType + "_id", idValue);

        Request.Builder b1 = new Request.Builder()
            .url(urlLevel)
            .post(RequestBody.create(reqLev.toString(), jsonType));
        ApiHeaderUtil.transferHeaders(b1);
        try (Response r1 = client.newCall(b1.build()).execute()) {
            if (r1.code() != 200) {
                task.setAssignee(null);
                return;
            }
            String stringBody = r1.body().string();
            int maxLevel = mapper.readTree(stringBody)
                                 .findValue("parentLevels").asInt(0);
            if (currentLevel > maxLevel) {
                task.setAssignee(null);
                return;
            }
        }

        // —— : current layer Query info —— //
        String urlParent = urlPrefix + "admin/v1/" + groupType + "ParentInfo";
        ObjectNode reqPar = mapper.createObjectNode()
                                  .put("accessToken", token)
                                  .put(groupType + "_id", idValue)
                                  .put("levels", currentLevel);

        Request.Builder b2 = new Request.Builder()
            .url(urlParent)
            .post(RequestBody.create(reqPar.toString(), jsonType));
        ApiHeaderUtil.transferHeaders(b2);
        try (Response r2 = client.newCall(b2.build()).execute()) {
            String parentId = mapper.readTree(r2.body().string())
                                    .findValue(groupType + "_id").asText();

            LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<>();
            if ("job".equals(groupType)) {
                qw.eq(SysUser::getJobId, parentId);
            } else {
                qw.eq(SysUser::getPostId, parentId);
            }
            List<SysUser> users = userService.list(qw);

            if (users.size() == 1) {
                task.setAssignee(users.get(0).getUserId());
            } else {
                List<String> ids = users.stream()
                                        .map(SysUser::getUserId)
                                        .collect(Collectors.toList());
                task.addCandidateGroups(ids);
            }
        }
    }
}
