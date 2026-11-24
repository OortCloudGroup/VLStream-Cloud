package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.vlstream.test.vlstream.data.*;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.ContainerInstance;
import com.ruoyi.vlstream.test.vlstream.service.IVlsContainerInstanceService;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

/** Admission to the existing node queue; no legacy SSH target or bypass of shared leases. */
@Service
@RequiredArgsConstructor
public class SmartAnnotationComputeService {
    private final JdbcTemplate jdbc;
    private final ComputeNodeService nodes;
    private final ComputeSharedQueueService shared;
    private final IVlsContainerInstanceService containers;
    private final ObjectMapper json;
    private final ModelArtifactObjectStore objects;

    public void enqueue(SmartAnnotationTask task, SmartAnnotationRound round, DatasetSnapshot snapshot, String executionUserId) {
        if (executionUserId == null || executionUserId.isEmpty()) throw new ServiceException("缺少可信算力执行身份", 403);
        nodes.lockServer(task.getComputeNodeId());
        ComputeNode node = nodes.requireReady(task.getComputeNodeId());
        String owner = nodes.trainingSubject(node, executionUserId);
        boolean company = nodes.isCompanyShared(node);
        ComputePublicInventoryService.Instance claim = null;
        if (!company && ("PLATFORM".equals(node.getResourceScope()) || node.getPublicInstanceKey() != null)) claim = nodes.publicInventory().training(node);
        String model = task.getModelPath();
        if (company) model = shared.verifyBaseModel(model, task.getAnnotationType(), owner);
        else if (nodes.officialConfigured(node)) model = nodes.officialBaseModel(model, task.getAnnotationType(), owner);
        if (!model.startsWith("@preset/") && objects.find(model) == null)
            throw new ServiceException("模型尚未归档到当前租户对象存储，请选择系统基础模型或已归档模型");
        String id = UUID.randomUUID().toString();
        String root = node.getWorkDir() + "/" + ModelArtifactObjectStore.hash(task.getTenantId()).substring(0,16) + "/" + id;
        ContainerInstance container = new ContainerInstance();
        container.setTenantId(task.getTenantId()); container.setInstanceType("smart_annotation");
        container.setInstanceName("智能标注-" + task.getTaskName()); container.setImageType("smart_annotation");
        container.setImageName("节点 Python"); container.setInstanceCount(1); container.setInstanceStatus("queued");
        container.setHealthStatus("unknown"); container.setTrainingTaskId(round.getId());
        container.setServerId(node.getId()); container.setServerIp(node.getHost()); container.setGpuIndex(node.getGpuIndex());
        container.setLogsPath(root + "/training.log"); container.setQueueTime(new Date()); container.setRestartCount(0);
        if (!containers.saveTaskRecord(container)) throw new ServiceException("保存智能标注执行记录失败");
        Map<String,Object> options = new LinkedHashMap<>();
        options.put("computeNodeName", node.getName());
        options.put("executionUserId", executionUserId);
        options.put("workload", "smart_annotation"); options.put("mode", task.getMode()); options.put("epochs", task.getEpochs());
        options.put("confidence", task.getConfidence()); options.put("workers", 0);
        options.put("autoConfirm", true);
        if (company) options.put("sharedQueue", shared.configuration(node,id,owner));
        if (node.isConfigurationManaged()) options.put("configuredResource", nodes.configurationSnapshot(node));
        try {
            jdbc.update("INSERT INTO vls_cloud_training_job(id,tenant_id,smart_round_id,node_id,container_record_id,dataset_id,version_id,annotation_type,snapshot_json,base_model,options_json,run_dir,public_instance_key,public_claim_id,public_owner_subject) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                id,task.getTenantId(),round.getId(),node.getId(),container.getId(),task.getDatasetId(),round.getVersionId(),task.getAnnotationType(),json.writeValueAsString(snapshot),model,json.writeValueAsString(options),root,
                claim==null?null:claim.getInstanceKey(),claim==null?null:claim.getClaimId(),owner);
            jdbc.update("UPDATE vls_smart_annotation_round SET work_directory=? WHERE id=? AND tenant_id=?",root,round.getId(),task.getTenantId());
        } catch (java.io.IOException error) { throw new ServiceException("智能标注执行快照保存失败"); }
    }

    public void cancel(SmartAnnotationTask task, Long roundId) {
        jdbc.update("UPDATE vls_cloud_training_job SET cancel_requested=1,update_time=NOW() WHERE tenant_id=? AND smart_round_id=? AND job_state NOT IN ('COMPLETED','FAILED','CANCELLED')",task.getTenantId(),roundId);
    }
    public Map<String,Object> execution(SmartAnnotationTask task, Long roundId) {
        List<Map<String,Object>> rows = jdbc.query("SELECT job_state, message, progress, options_json FROM vls_cloud_training_job "
            + "WHERE tenant_id=? AND smart_round_id=? ORDER BY create_time DESC,container_record_id DESC LIMIT 1",
            (row, index) -> {
                JsonNode options = readOptions(row.getString("options_json"));
                boolean canReadDiagnostics = canReadDiagnostics(task, options);
                Map<String,Object> result = new LinkedHashMap<>();
                result.put("state", row.getString("job_state"));
                result.put("progress", row.getInt("progress"));
                result.put("canReadDiagnostics", canReadDiagnostics);
                if (canReadDiagnostics) {
                    result.put("message", row.getString("message"));
                    result.put("nodeName", options.path("computeNodeName").asText());
                }
                return result;
            }, task.getTenantId(), roundId);
        if (rows.isEmpty()) return Collections.emptyMap();
        return rows.get(0);
    }

    public String logs(SmartAnnotationTask task, Long roundId) {
        List<String> rows = jdbc.query("SELECT log_tail, options_json FROM vls_cloud_training_job "
            + "WHERE tenant_id=? AND smart_round_id=? ORDER BY create_time DESC,container_record_id DESC LIMIT 1",
            (row, index) -> {
                if (!canReadDiagnostics(task, readOptions(row.getString("options_json"))))
                    throw new ServiceException("本轮执行日志仅本次执行成员可查看", 403);
                return row.getString("log_tail");
            }, task.getTenantId(), roundId);
        return rows.isEmpty() ? "等待所选算力节点调度" : Objects.toString(rows.get(0), "等待所选算力节点调度");
    }

    private JsonNode readOptions(String options) {
        if (options == null) return null;
        try { return json.readTree(options); }
        catch (java.io.IOException invalid) { return null; }
    }

    private boolean canReadDiagnostics(SmartAnnotationTask task, JsonNode options) {
        String viewer = LoginHelper.getUserId();
        if (viewer == null || options == null || !options.isObject()) return false;
        JsonNode executionUser = options.get("executionUserId");
        // Before collaboration, only the task creator could enqueue a round.
        return viewer.equals(executionUser == null ? task.getCreateUser() : executionUser.textValue());
    }
    public void phase(CloudTrainingJob job, String state, String message) {
        String mapped="ARCHIVING".equals(state)?"RUNNING":state;
        jdbc.update("UPDATE vls_smart_annotation_round SET round_state=?,error_message=NULL WHERE tenant_id=? AND id=? AND round_state<>'REVIEW'",mapped,job.getTenantId(),job.getSmartRoundId());
        jdbc.update("UPDATE vls_smart_annotation_task SET task_state=?,error_message=NULL WHERE tenant_id=? AND id=(SELECT task_id FROM vls_smart_annotation_round WHERE id=? AND tenant_id=?) AND task_state<>'CANCEL_REQUESTED'",mapped,job.getTenantId(),job.getSmartRoundId(),job.getTenantId());
    }
    public void finished(CloudTrainingJob job, String state, String message, boolean autoConfirm) {
        if ("COMPLETED".equals(state)) {
            Integer cancelled=jdbc.queryForObject("SELECT COUNT(*) FROM vls_smart_annotation_task WHERE tenant_id=? AND task_state='CANCEL_REQUESTED' AND id=(SELECT task_id FROM vls_smart_annotation_round WHERE id=? AND tenant_id=?)",Integer.class,job.getTenantId(),job.getSmartRoundId(),job.getTenantId());
            state=cancelled!=null && cancelled>0?"CANCELLED":"REVIEW";
            message=null;
        }
        if (autoConfirm && "REVIEW".equals(state)) {
            Integer eligible = jdbc.queryForObject("SELECT COUNT(*) FROM vls_smart_annotation_candidate "
                + "WHERE tenant_id=? AND round_id=? AND review_state='PENDING' AND is_deleted=0 AND (review_error IS NULL OR review_error='')",
                Integer.class, job.getTenantId(), job.getSmartRoundId());
            if (eligible != null && eligible > 0) {
                int updated = jdbc.update("UPDATE vls_smart_annotation_task SET task_state='AUTO_CONFIRMING',error_message=NULL,"
                    + "bulk_cursor=0,bulk_total=?,bulk_processed=0,bulk_accepted=0,bulk_conflicts=0,bulk_error=NULL "
                    + "WHERE tenant_id=? AND task_state IN ('RUNNING','PREPARING','QUEUED') "
                    + "AND id=(SELECT task_id FROM vls_smart_annotation_round WHERE id=? AND tenant_id=?)",
                    eligible, job.getTenantId(), job.getSmartRoundId(), job.getTenantId());
                if (updated > 0) {
                    jdbc.update("UPDATE vls_smart_annotation_round SET round_state='REVIEW',error_message=NULL WHERE tenant_id=? AND id=?",
                        job.getTenantId(), job.getSmartRoundId());
                    return;
                }
            }
        }
        jdbc.update("UPDATE vls_smart_annotation_round SET round_state=?,error_message=? WHERE tenant_id=? AND id=?",state,message,job.getTenantId(),job.getSmartRoundId());
        jdbc.update("UPDATE vls_smart_annotation_task SET task_state=?,error_message=? WHERE tenant_id=? AND id=(SELECT task_id FROM vls_smart_annotation_round WHERE id=? AND tenant_id=?)",state,message,job.getTenantId(),job.getSmartRoundId(),job.getTenantId());
    }
}
