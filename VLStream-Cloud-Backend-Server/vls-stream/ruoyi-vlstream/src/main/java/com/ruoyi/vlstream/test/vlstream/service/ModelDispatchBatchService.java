package com.ruoyi.vlstream.test.vlstream.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ModelDispatchTask;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

/** Durable orchestration around the existing per-device model deployment, with no device retry worker. */
@Service
@RequiredArgsConstructor
public class ModelDispatchBatchService {
    public static final int MAX_DEVICES = 20;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final ModelDispatchService dispatch;
    private final ModelDispatchTaskService tasks;
    private final ModelDispatchAccess access;
    private final ModelDispatchLocks locks;

    @Data
    public static class Submit {
        private String operationId;
        private Long algorithmId;
        private Long modelId;
        private String modelType = "om";
        private List<String> deviceIds;
    }
    @Data
    public static class Retry {
        private String retryId;
        private List<String> deviceIds;
    }
    @Data
    public static class Item {
        private String deviceId;
        private String requestId;
        private String state = "QUEUED";
        private String reason;
        private int attempt;
        private boolean retryable;
    }
    @Data
    public static class View {
        private String operationId;
        private Long algorithmId;
        private Long modelId;
        private Long trainingId;
        private String modelType;
        private String fileName;
        private String sha256;
        private String createdAt;
        private List<Item> items;
        private int succeeded;
        private int failed;
        private int pending;
        private int unknown;
    }
    private static class Batch {
        String id;
        String hash;
        String created;
        ModelDispatchService.DeploymentSnapshot snapshot;
        List<Item> items;
        Map<String, List<String>> retries;
    }

    public View submit(Submit request) {
        if (request == null) throw new ServiceException("请求不能为空");
        String id = uuid(request.getOperationId());
        if ((request.getAlgorithmId() == null) == (request.getModelId() == null)) throw new ServiceException("请选择一个算法或一个已保存模型");
        List<String> devices = devices(request.getDeviceIds());
        if (!Arrays.asList("om", "pt", "onnx", "rknn", "int8-rknn").contains(request.getModelType())) throw new ServiceException("模型格式无效");
        String hash = hash(encode(Arrays.asList(request.getAlgorithmId(), request.getModelId(), request.getModelType(), devices)));
        return locks.run("batch:" + id, () -> {
            Batch existing = find(id);
            if (existing != null) {
                if (!hash.equals(existing.hash)) throw new ServiceException("同一提交编号不能更改算法、模型或设备范围");
                return view(existing);
            }
            Batch batch = new Batch();
            batch.id = id; batch.hash = hash; batch.created = java.time.Instant.now().toString();
            batch.snapshot = dispatch.prepare(request.getAlgorithmId(), request.getModelId(), request.getModelType());
            batch.items = new ArrayList<>(); batch.retries = new LinkedHashMap<>();
            for (String device : devices) { Item item = new Item(); item.setDeviceId(device); batch.items.add(item); }
            jdbc.update("INSERT INTO vls_model_dispatch_batch (operation_id,tenant_id,owner_id,request_hash,snapshot_json,items_json,retries_json,create_time,update_time) VALUES (?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)",
                id, access.tenant(), access.owner(), hash, encode(batch.snapshot), encode(batch.items), encode(batch.retries));
            for (Item item : batch.items) execute(batch, item);
            return view(batch);
        });
    }

    public View get(String operationId) {
        Batch batch = find(uuid(operationId));
        if (batch == null) throw new ServiceException("批次不存在或无权访问");
        return view(batch);
    }

    public List<View> recent() {
        List<View> result = new ArrayList<>();
        List<String> ids = jdbc.queryForList("SELECT operation_id FROM vls_model_dispatch_batch WHERE tenant_id=? AND owner_id=? ORDER BY create_time DESC LIMIT 10",
            String.class, access.tenant(), access.owner());
        for (String id : ids) { Batch batch = find(id); if (batch != null) result.add(view(batch)); }
        return result;
    }

    public View retry(String operationId, Retry request) {
        String id = uuid(operationId);
        if (request == null) throw new ServiceException("重试请求不能为空");
        String retryId = uuid(request.getRetryId());
        List<String> requested = devices(request.getDeviceIds());
        return locks.run("batch:" + id, () -> {
            Batch batch = find(id);
            if (batch == null) throw new ServiceException("批次不存在或无权访问");
            if (batch.retries.containsKey(retryId)) {
                if (!requested.equals(batch.retries.get(retryId))) throw new ServiceException("同一重试编号不能更改设备范围");
                return view(batch);
            }
            if (batch.retries.size() >= 10) throw new ServiceException("该批次已重试10次，请先排查失败原因");
            view(batch); // Read the current, authoritative hardware outcomes before choosing any item.
            for (String device : requested) {
                Item item = batch.items.stream().filter(row -> device.equals(row.getDeviceId())).findFirst()
                    .orElseThrow(() -> new ServiceException("重试设备不属于该批次"));
                if (!item.isRetryable()) throw new ServiceException("只能重试明确失败或尚未发送的设备；请先刷新结果");
            }
            batch.retries.put(retryId, requested);
            save(batch); // Record retry intent before any external effect; a repeated key never re-executes it.
            for (Item item : batch.items) if (requested.contains(item.getDeviceId())) execute(batch, item);
            return view(batch);
        });
    }

    private void execute(Batch batch, Item item) {
        item.setAttempt(item.getAttempt() + 1);
        item.setRequestId(UUID.randomUUID().toString());
        item.setState("CHECKING"); item.setRetryable(false); item.setReason(null);
        save(batch); // A crash here is uncertain, never permission to blindly re-publish.
        try {
            dispatch.dispatchSnapshot(batch.snapshot, item.getDeviceId(), item.getRequestId());
            item.setState("PENDING");
        } catch (Exception ex) {
            ModelDispatchTask task = tasks.getByRequestId(item.getRequestId());
            if (task == null) {
                item.setState("FAILED"); item.setRetryable(true);
                item.setReason(StringUtils.abbreviate(ex.getMessage(), 500));
            } else {
                item.setState("UNKNOWN"); item.setReason("已创建下发任务，发送或执行结果未确认，请先核实设备");
            }
        }
        save(batch);
    }

    private View view(Batch batch) {
        View view = new View(); view.setOperationId(batch.id); view.setCreatedAt(batch.created);
        view.setAlgorithmId(batch.snapshot.getAlgorithmId()); view.setModelId(batch.snapshot.getModelId());
        view.setTrainingId(batch.snapshot.getTrainingId()); view.setModelType(batch.snapshot.getModelType());
        view.setFileName(batch.snapshot.getFileName()); view.setSha256(batch.snapshot.getSha256());
        // Never expose paths, class content, download signatures, tokens or raw MQTT payloads.
        List<Item> items = new ArrayList<>();
        for (Item stored : batch.items) {
            Item item = decode(encode(stored), Item.class);
            ModelDispatchTask task = StringUtils.isBlank(item.getRequestId()) ? null : tasks.getByRequestId(item.getRequestId());
            if (task != null) {
                if (!access.tenant().equals(task.getTenantId()) || !item.getDeviceId().equals(task.getDeviceId())) throw new ServiceException("下发任务归属不一致");
                String status = task.getDispatchStatus();
                item.setState(status);
                item.setReason(task.getFailureReason());
                if (!"SUCCESS".equals(status) && !"FAILED".equals(status)
                    && task.getDownloadExpiresAt() != null && task.getDownloadExpiresAt() < System.currentTimeMillis() / 1000) {
                    item.setState("UNKNOWN"); item.setReason("下载有效期已过，设备最终结果未确认；请核实设备，不能自动重试");
                }
            } else if ("CHECKING".equals(item.getState())) {
                item.setState("UNKNOWN"); item.setReason("正在校验或提交中，请刷新；中断后需核实任务");
            }
            item.setRetryable("FAILED".equals(item.getState()) || "QUEUED".equals(item.getState()));
            if ("SUCCESS".equals(item.getState())) view.succeeded++;
            else if ("FAILED".equals(item.getState())) view.failed++;
            else if ("UNKNOWN".equals(item.getState())) view.unknown++;
            else view.pending++;
            stored.setRetryable(item.isRetryable()); stored.setState(item.getState()); stored.setReason(item.getReason());
            items.add(item);
        }
        view.setItems(items);
        return view;
    }

    private Batch find(String id) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM vls_model_dispatch_batch WHERE operation_id=?", id);
        if (rows.isEmpty()) return null;
        Map<String, Object> row = rows.get(0);
        if (!access.tenant().equals(row.get("tenant_id")) || !access.owner().equals(row.get("owner_id"))) throw new ServiceException("批次不存在或无权访问");
        Batch batch = new Batch(); batch.id = id; batch.hash = row.get("request_hash").toString();
        batch.created = String.valueOf(row.get("create_time"));
        batch.snapshot = decode(row.get("snapshot_json").toString(), ModelDispatchService.DeploymentSnapshot.class);
        try {
            batch.items = json.readValue(row.get("items_json").toString(), new TypeReference<List<Item>>() {});
            batch.retries = json.readValue(row.get("retries_json").toString(), new TypeReference<Map<String, List<String>>>() {});
        } catch (Exception ex) { throw new ServiceException("批次记录无法读取，请联系管理员核实"); }
        return batch;
    }

    private void save(Batch batch) {
        int updated = jdbc.update("UPDATE vls_model_dispatch_batch SET items_json=?, retries_json=?, update_time=CURRENT_TIMESTAMP WHERE operation_id=? AND tenant_id=? AND owner_id=?",
            encode(batch.items), encode(batch.retries), batch.id, access.tenant(), access.owner());
        if (updated != 1) throw new ServiceException("批次状态保存失败，请刷新核实");
    }
    private List<String> devices(List<String> ids) {
        if (ids == null || ids.isEmpty() || ids.size() > MAX_DEVICES) throw new ServiceException("每批请选择1至20台摄像机，便于逐台核验和恢复");
        TreeSet<String> result = new TreeSet<>();
        for (String id : ids) {
            if (id == null || !id.matches("[A-Za-z0-9_.:-]{1,100}")) throw new ServiceException("设备业务编号无效");
            if (!result.add(id)) throw new ServiceException("设备业务编号不能重复");
        }
        return new ArrayList<>(result);
    }
    private String uuid(String id) {
        if (id == null || !id.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) throw new ServiceException("提交编号无效");
        return id.toLowerCase(java.util.Locale.ROOT);
    }
    private String encode(Object value) {
        try { return json.writeValueAsString(value); }
        catch (Exception ex) { throw new ServiceException("批次数据无法保存"); }
    }
    private <T> T decode(String value, Class<T> type) {
        try { return json.readValue(value, type); }
        catch (Exception ex) { throw new ServiceException("批次数据无法读取"); }
    }
    private String hash(String text) {
        try {
            StringBuilder value = new StringBuilder();
            for (byte b : MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8))) value.append(String.format("%02x", b));
            return value.toString();
        } catch (Exception ex) { throw new IllegalStateException(ex); }
    }
}
