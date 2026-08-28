/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.workflow.convert.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.utils.OkHttpClientHolder;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MessageNode task
 * notificationtask, interface
 */
@Slf4j
@Component
public class MessageVerificationScheduler {

    // ( 3 )
    private static final int MAX_VERIFY_COUNT = 3;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 20 Execute
     */
    @Scheduled(fixedDelay = 20000)
    public void verifyPendingMessages() {
        try {
            // 1. Process to "readyToComplete" task (priority != 0/1)
            List<Task> readyTasks = taskService.createTaskQuery()
                    .taskVariableValueEquals("messageNode_readyToComplete", true)
                    .list();

            for (Task task : readyTasks) {
                try {
                    log.info("MessageVerificationScheduler: 自动完成任务 taskId={}", task.getId());
                    taskService.complete(task.getId());
                } catch (Exception e) {
                    log.error("自动完成任务失败 taskId={}", task.getId(), e);
                }
            }

            // 2. Process need to task (priority == 0/1)
            List<Task> verifyTasks = taskService.createTaskQuery()
                    .taskVariableValueEquals("messageNode_needVerify", true)
                    .list();

            for (Task task : verifyTasks) {
                verifyTask(task);
            }

        } catch (Exception e) {
            log.error("MessageVerificationScheduler 执行异常", e);
        }
    }

    private void verifyTask(Task task) {
        try {
            // Get variable
            Map<String, Object> variables = taskService.getVariablesLocal(task.getId());


            // Get current
            Integer verifyCount = (Integer) variables.get("messageNode_verifyCount");
            if (verifyCount == null) {
                verifyCount = 0;
            }

            // whether
            if (verifyCount >= MAX_VERIFY_COUNT) {
                log.info("任务 {} 验证次数已达上限 ({}/{}), 停止验证，等待超时处理",
                    task.getId(), verifyCount, MAX_VERIFY_COUNT);
                // , after
                taskService.removeVariableLocal(task.getId(), "messageNode_needVerify");
                return;
            }

            String msgNo = (String) variables.get("messageNode_msgNo");
            String uid = (String) variables.get("messageNode_uid");

            if (msgNo == null || uid == null) {
                log.warn("任务 {} 缺少验证参数，跳过", task.getId());
                return;
            }

            // interface
            int status = callVerificationApi(msgNo, uid);

            // receipt_status: 1: already , 2: already => successfully
            if (status == 1 || status == 2) {
                log.info("消息验证成功 (status={}, 验证次数={}), 完成任务 taskId={}",
                        status, verifyCount + 1, task.getId());
                taskService.complete(task.getId());
            } else {
                // failed,
                verifyCount++;
                taskService.setVariableLocal(task.getId(), "messageNode_verifyCount", verifyCount);
                log.debug("消息验证未通过 (status={}, 验证次数={}/{}), 继续等待 taskId={}",
                        status, verifyCount, MAX_VERIFY_COUNT, task.getId());
            }

        } catch (Exception e) {
            log.error("验证任务失败 taskId={}", task.getId(), e);
        }
    }

    private int callVerificationApi(String msgNo, String uid) {
        try {
            Environment env = applicationContext.getBean(Environment.class);
            String url = env.getProperty("UnifiedMessagingSend.url") + "msg/v1/recipient/status";
            // is GET , parameter in URL in ?
            // user : is get , parametermsg_no, uid

            String fullUrl = url + "?msg_no=" + msgNo + "&uid=" + uid;

            OkHttpClient client = OkHttpClientHolder.CLIENT;
            Request request = new Request.Builder()
                    .url(fullUrl)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(responseBody);

                    if (root.path("code").asInt() == 200) {
                        return root.path("data").path("receipt_status").asInt();
                    }
                }
            }
        } catch (Exception e) {
            log.error("调用验证接口失败", e);
        }
        return -1; // not
    }
}
