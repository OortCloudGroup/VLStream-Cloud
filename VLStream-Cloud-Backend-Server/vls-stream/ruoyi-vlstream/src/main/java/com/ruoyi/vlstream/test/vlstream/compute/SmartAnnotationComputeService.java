package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
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

    public void enqueue(SmartAnnotationTask task, SmartAnnotationRound round, DatasetSnapshot snapshot) {
        nodes.lockServer(task.getComputeNodeId());
        ComputeNode node = nodes.requireReady(task.getComputeNodeId());
        String owner = nodes.trainingSubject(node, String.valueOf(task.getCreateUser()));
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
        options.put("workload", "smart_annotation"); options.put("mode", task.getMode()); options.put("epochs", task.getEpochs());
        options.put("confidence", task.getConfidence()); options.put("workers", 0);
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
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT job_state AS state,message,progress,options_json FROM vls_cloud_training_job WHERE tenant_id=? AND smart_round_id=? ORDER BY create_time DESC LIMIT 1",task.getTenantId(),roundId);
        if(rows.isEmpty())return Collections.emptyMap();
        Map<String,Object> result=rows.get(0);
        try { result.put("nodeName",json.readTree(String.valueOf(result.remove("options_json"))).path("computeNodeName").asText()); }
        catch(java.io.IOException error) { result.remove("options_json"); }
        return result;
    }
    public String logs(SmartAnnotationTask task, Long roundId) {
        List<String> rows=jdbc.queryForList("SELECT log_tail FROM vls_cloud_training_job WHERE tenant_id=? AND smart_round_id=? ORDER BY create_time DESC LIMIT 1",String.class,task.getTenantId(),roundId);
        return rows.isEmpty() || rows.get(0)==null?"等待所选算力节点调度":rows.get(0);
    }
    public void phase(CloudTrainingJob job, String state, String message) {
        String mapped="ARCHIVING".equals(state)?"RUNNING":state;
        jdbc.update("UPDATE vls_smart_annotation_round SET round_state=?,error_message=NULL WHERE tenant_id=? AND id=? AND round_state<>'REVIEW'",mapped,job.getTenantId(),job.getSmartRoundId());
        jdbc.update("UPDATE vls_smart_annotation_task SET task_state=?,error_message=NULL WHERE tenant_id=? AND id=(SELECT task_id FROM vls_smart_annotation_round WHERE id=? AND tenant_id=?) AND task_state<>'CANCEL_REQUESTED'",mapped,job.getTenantId(),job.getSmartRoundId(),job.getTenantId());
    }
    public void finished(CloudTrainingJob job, String state, String message) {
        if ("COMPLETED".equals(state)) {
            Integer cancelled=jdbc.queryForObject("SELECT COUNT(*) FROM vls_smart_annotation_task WHERE tenant_id=? AND task_state='CANCEL_REQUESTED' AND id=(SELECT task_id FROM vls_smart_annotation_round WHERE id=? AND tenant_id=?)",Integer.class,job.getTenantId(),job.getSmartRoundId(),job.getTenantId());
            state=cancelled!=null && cancelled>0?"CANCELLED":"REVIEW";
            message=null;
        }
        jdbc.update("UPDATE vls_smart_annotation_round SET round_state=?,error_message=? WHERE tenant_id=? AND id=?",state,message,job.getTenantId(),job.getSmartRoundId());
        jdbc.update("UPDATE vls_smart_annotation_task SET task_state=?,error_message=? WHERE tenant_id=? AND id=(SELECT task_id FROM vls_smart_annotation_round WHERE id=? AND tenant_id=?)",state,message,job.getTenantId(),job.getSmartRoundId(),job.getTenantId());
    }
}
