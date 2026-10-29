package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ModelDispatchTask;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Idempotent saved-model attempts shared by the single and multiple device picker. */
@Service
@RequiredArgsConstructor
public class SavedModelDispatchService {
    private final ModelDispatchService dispatcher;
    private final ModelDispatchTaskService tasks;
    private final ModelDispatchAccess access;
    private final ModelDispatchLocks locks;

    @Data
    public static class Result {
        private String requestId;
        private String state;
        private String message;
        private boolean retryable;
    }

    public Result submit(Long modelId, String deviceId, String operationId) {
        String requestId = requestId(modelId, deviceId, operationId);
        // Keep this lock through validation and publication; a duplicate cannot be classified
        // as safe to retry while the original HTTP request is still creating its task.
        return locks.run("saved-attempt:" + requestId, () -> {
            try {
                dispatcher.requireSavedModelAccess(modelId, deviceId);
                access.requireDevice(deviceId, "om", false);
                ModelDispatchTask existing = tasks.getByRequestId(requestId);
                if (existing != null) return result(existing);
                dispatcher.dispatchSavedModel(modelId, deviceId, requestId);
                ModelDispatchTask created = tasks.getByRequestId(requestId);
                if (created == null) {
                    Result unknown = new Result();
                    unknown.setRequestId(requestId); unknown.setState("UNKNOWN");
                    unknown.setMessage("已调用下发但任务读取未确认，请核实原请求");
                    return unknown;
                }
                return result(created);
            } catch (RuntimeException ex) {
                // A durable task is created BEFORE MQTT publication. Once present, even an
                // exception is uncertain, never a pre-publication failure or automatic retry.
                ModelDispatchTask existing = tasks.getByRequestId(requestId);
                if (existing != null) {
                    Result unknown = new Result();
                    unknown.setRequestId(requestId);
                    unknown.setState("UNKNOWN");
                    unknown.setMessage("任务可能已发送，请核实原任务；" + StringUtils.abbreviate(ex.getMessage(), 500));
                    return unknown;
                }
                Result failed = new Result();
                failed.setRequestId(requestId);
                failed.setState("FAILED");
                failed.setRetryable(true);
                failed.setMessage(StringUtils.defaultIfBlank(StringUtils.abbreviate(ex.getMessage(), 1000), "提交失败，未发送设备指令"));
                return failed;
            }
        });
    }

    public Result get(Long modelId, String deviceId, String operationId) {
        String requestId = requestId(modelId, deviceId, operationId);
        dispatcher.requireSavedModelAccess(modelId, deviceId);
        access.requireDevice(deviceId, "om", false);
        ModelDispatchTask task = tasks.getByRequestId(requestId);
        if (task == null) throw new ServiceException("未查到原任务，请使用核实提交结果恢复原请求");
        return result(task);
    }

    private String requestId(Long modelId, String deviceId, String operationId) {
        if (modelId == null || modelId <= 0 || StringUtils.isBlank(deviceId) || deviceId.length() > 128
            || operationId == null || !operationId.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            throw new ServiceException("模型、设备或操作编号无效");
        }
        String namespace = "saved-model\0" + access.tenant() + "\0" + access.owner() + "\0"
            + operationId.toLowerCase(java.util.Locale.ROOT) + "\0" + modelId + "\0" + deviceId;
        return UUID.nameUUIDFromBytes(namespace.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private Result result(ModelDispatchTask task) {
        Result result = new Result();
        result.setRequestId(task.getRequestId());
        String state = StringUtils.defaultIfBlank(task.getDispatchStatus(), "UNKNOWN");
        if (!"SUCCESS".equals(state) && !"FAILED".equals(state) && task.getDownloadExpiresAt() != null
            && task.getDownloadExpiresAt() < System.currentTimeMillis() / 1000) state = "UNKNOWN";
        result.setState(state);
        result.setRetryable("FAILED".equals(state));
        result.setMessage(task.getFailureReason());
        return result;
    }
}
