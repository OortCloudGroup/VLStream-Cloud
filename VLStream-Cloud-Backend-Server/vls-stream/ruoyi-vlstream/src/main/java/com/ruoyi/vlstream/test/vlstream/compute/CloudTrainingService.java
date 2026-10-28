package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.data.*;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import com.ruoyi.vlstream.test.vlstream.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CloudTrainingService {
    private final JdbcTemplate jdbc;
    private final ComputeNodeService nodes;
    private final DataManagementService data;
    private final TrainingPublicationService publication;
    private final IVlsAlgorithmTrainingService trainings;
    private final IVlsContainerInstanceService containers;
    private final VlsAlgorithmMapper algorithms;
    private final ObjectMapper json;
    @javax.annotation.Resource
    private ModelArtifactObjectStore modelObjects;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ComputeSharedQueueService sharedQueue;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ComputePublicTaskAccess publicAccess;

    private ComputePublicTaskAccess access() {
        return publicAccess==null?ComputePublicTaskAccess.forCompute(jdbc,nodes.publicInventory(),sharedQueue):publicAccess;
    }

    @Transactional(rollbackFor = Exception.class)
    public RemoteTrainingService.StartResult start(Long trainingId, ComputeRequests.Start request) {
        String tenant = ComputeNodeService.tenant();
        data.lockDatasetForTask(request.getDatasetId());
        publication.lockForStart(trainingId);
        // Serialize edits/removal with queue creation, including jobs not yet picked up.
        nodes.lockServer(request.getNodeId());
        ComputeNode node = nodes.requireReady(request.getNodeId());
        AlgorithmTraining training = trainings.selectAlgorithmTrainingById(trainingId);
        AlgorithmAnnotation dataset = data.project(request.getDatasetId());
        if (training == null) throw new ServiceException("训练任务不存在");
        String taskOwner=nodes.trainingSubject(node,training.getCreateUser());
        ComputePublicInventoryService.Instance claim=null;
        boolean companyShared=nodes.isCompanyShared(node);
        if (!companyShared && ("PLATFORM".equals(node.getResourceScope()) || node.getPublicInstanceKey()!=null)) {
            ComputePublicInventoryService inventory=nodes.publicInventory();
            if (!Objects.equals(training.getCreateUser(),inventory.identity().getVlsUserId()))
                throw new ServiceException("只能在公共实例提交当前用户自己的训练任务");
            claim=inventory.training(node);
        }
        Algorithm algorithm = algorithms.selectById(training.getAlgorithmId());
        AnnotationTaskType type = AnnotationTaskType.of(dataset.getAnnotationType());
        if (algorithm == null || !tenant.equals(algorithm.getTenantId()) || algorithm.getCategory() == null || !type.getModelTask().equals(algorithm.getCategory().getCode())) throw new ServiceException("算法不存在、无权访问或与数据集标注类型不匹配");
        String baseModel = resolveBaseModel(request.getModelSource(), algorithm.getPtModelFilePath(), type.getModelTask());
        if(companyShared)baseModel=sharedQueue.verifyBaseModel(baseModel,type.getCode(),taskOwner);
        DatasetSnapshot snapshot = data.snapshot(request.getDatasetId());
        CloudTrainingDatasetWriter.members(snapshot);
        DataRequests.Version versionRequest = new DataRequests.Version();
        versionRequest.setName("线上训练快照"); versionRequest.setDescription("线上算力 SSH 训练固定输入");
        DatasetVersion version = data.saveVersion(request.getDatasetId(), versionRequest);
        TrainingOptions options = new TrainingOptions(request.getEpochs(), request.getBatchSize(), request.getImgSize(), request.getExtraParams());
        String jobId = UUID.randomUUID().toString();
        String runDir = node.getWorkDir() + "/" + ModelArtifactObjectStore.hash(tenant).substring(0, 16) + "/" + jobId;
        ContainerInstance instance = new ContainerInstance();
        instance.setTenantId(tenant); instance.setInstanceName("线上算力-" + training.getTaskName());
        instance.setInstanceType("cloud_training"); instance.setImageType("training"); instance.setImageName("线上 Python");
        instance.setInstanceCount(1); instance.setInstanceStatus("queued"); instance.setHealthStatus("unknown");
        instance.setTrainingTaskId(trainingId); instance.setServerId(node.getId()); instance.setServerIp(node.getHost());
        instance.setGpuIndex(node.getGpuIndex()); instance.setGpuLimit("GPU " + node.getGpuIndex());
        instance.setLogsPath(runDir + "/training.log"); instance.setQueueTime(new Date()); instance.setRestartCount(0);
        if (!containers.saveTaskRecord(instance)) throw new ServiceException("保存线上训练队列失败");
        String durableOptions=options.toJson();
        if(companyShared)try {
            Map<String,Object> configured=json.readValue(durableOptions,new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>(){});
            configured.put("sharedQueue",sharedQueue.configuration(node,jobId,taskOwner));durableOptions=json.writeValueAsString(configured);
            if(node.isConfigurationManaged()) { configured.put("configuredResource",nodes.configurationSnapshot(node));durableOptions=json.writeValueAsString(configured); }
        } catch(java.io.IOException error){throw new ServiceException("共享GPU任务快照保存失败");}
        jdbc.update("INSERT INTO vls_cloud_training_job(id,tenant_id,training_id,node_id,container_record_id,dataset_id,version_id,annotation_type,snapshot_json,base_model,options_json,run_dir,public_instance_key,public_claim_id,public_owner_subject) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            jobId, tenant, trainingId, node.getId(), instance.getId(), request.getDatasetId(), version.getId(), type.getCode(), version.getSnapshotJson(), baseModel, durableOptions, runDir,
            claim==null?null:claim.getInstanceKey(),claim==null?null:claim.getClaimId(),taskOwner);
        publication.configure(trainingId, options.isAutoPublish());
        try {
            Map<String, Object> config = json.readValue(options.toJson(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() { });
            config.put("cloudJobId", jobId); config.put("computeNodeId", String.valueOf(node.getId()));
            config.put("computeNodeName", node.getName()); config.put("trainType", type.getModelTask());
            config.put("annotationType", type.getCode());
            config.put("modelSource", baseModel.startsWith("@preset/") ? "preset" : "algorithm");
            jdbc.update("UPDATE vls_algorithm_training SET dataset_id=?,train_status='pending',progress=0,epoch_total=?,epoch_current=0,config_params=?,log_path=?,error_message=NULL,onnx_conversion_status='not_required',om_conversion_status='not_required',update_time=NOW() WHERE tenant_id=? AND id=?",
                request.getDatasetId(), options.getEpochs(), json.writeValueAsString(config), instance.getLogsPath(), tenant, trainingId);
        } catch (java.io.IOException e) { throw new ServiceException("保存线上训练配置失败"); }
        RemoteTrainingService.StartResult result = new RemoteTrainingService.StartResult();
        result.setLogPath(instance.getLogsPath()); result.setTrainType(type.getModelTask());
        result.setMessage("已进入所选算力实例队列，正在准备数据");
        return result;
    }

    String resolveBaseModel(String source, String configured, String task) {
        if ("preset".equals(source)) return "@preset/" + task;
        if (source != null && !"algorithm".equals(source)) throw new ServiceException("无效的起始模型来源");
        if (configured == null || configured.trim().isEmpty()) {
            if ("algorithm".equals(source)) throw new ServiceException("算法尚未指定模型，请选择系统基础模型");
            return "@preset/" + task;
        }
        String path = configured.trim();
        if (path.startsWith("@preset/")) {
            if (!path.equals("@preset/" + task)) throw new ServiceException("基础模型与当前任务类型不匹配");
            return path;
        }
        if (modelObjects.find(path) == null)
            throw new ServiceException("指定模型尚未在当前租户的 MinIO 中就绪，请先导入或归档；新算力不会读取开发测试服务器的文件");
        return path;
    }

    public static boolean isCloud(AlgorithmTraining training) {
        if (training == null || training.getConfigParams() == null) return false;
        try { return new ObjectMapper().readTree(training.getConfigParams()).hasNonNull("cloudJobId"); }
        catch (java.io.IOException e) { return false; }
    }
    public CloudTrainingJob current(AlgorithmTraining training) {
        if(training==null || !Objects.equals(ComputeNodeService.tenant(),training.getTenantId()))
            throw new ServiceException("线上训练记录不存在或无权访问");
        try {
            String jobId = json.readTree(training.getConfigParams()).path("cloudJobId").asText();
            List<CloudTrainingJob> rows = jdbc.query("SELECT * FROM vls_cloud_training_job WHERE BINARY tenant_id=BINARY ? AND training_id=? AND BINARY id=BINARY ?", new BeanPropertyRowMapper<>(CloudTrainingJob.class), ComputeNodeService.tenant(), training.getId(), jobId);
            if (!rows.isEmpty()) {
                CloudTrainingJob job=rows.get(0);
                authorize(job,training);
                return job;
            }
        } catch (java.io.IOException e) { throw new ServiceException("线上训练配置无效"); }
        throw new ServiceException("线上训练记录不存在或无权访问");
    }
    private void authorize(CloudTrainingJob job,AlgorithmTraining training) {
        if(training==null || !Objects.equals(job.getTrainingId(),training.getId()) || !Objects.equals(ComputeNodeService.tenant(),training.getTenantId()))
            throw new ServiceException("线上训练记录不存在或无权访问");
        access().training(training.getId());
        boolean shared=false;
        try { shared=ComputeSharedQueueService.MODE.equals(json.readTree(job.getOptionsJson()).path("sharedQueue").path("mode").asText()); }
        catch(Exception ignored) { }
        if(shared) {
            if(sharedQueue==null || !Objects.equals(job.getPublicOwnerSubject(),sharedQueue.requireCreator(training.getCreateUser())))
                throw new ServiceException("无权读取或控制其他用户的共享GPU任务");
        } else if(job.getPublicOwnerSubject()!=null) {
            ComputePlatformAuthorization.Context actor=nodes.publicInventory().identity();
            if(!Objects.equals(training.getCreateUser(),actor.getVlsUserId()) || !Objects.equals(job.getPublicOwnerSubject(),ComputePublicInventoryService.subject(actor)))
                throw new ServiceException("无权读取或控制其他用户的算力任务");
        } else {
            String user=com.ruoyi.common.helper.LoginHelper.getUserId();
            if(user==null || user.isEmpty() || !Objects.equals(user,training.getCreateUser()))
                throw new ServiceException("历史私人训练归属未确认或不属于当前用户");
        }
    }
    public void stop(AlgorithmTraining training) {
        CloudTrainingJob job = current(training);
        jdbc.update("UPDATE vls_cloud_training_job SET cancel_requested=1,message='停止请求已记录，等待实例确认',update_time=NOW() WHERE BINARY tenant_id=BINARY ? AND BINARY id=BINARY ? AND job_state NOT IN ('COMPLETED','FAILED','CANCELLED')", ComputeNodeService.tenant(), job.getId());
    }
    public void assertInactive(AlgorithmTraining training) {
        CloudTrainingJob job = current(training);
        if (!Arrays.asList("COMPLETED", "FAILED", "CANCELLED").contains(job.getJobState()))
            throw new ServiceException("线上任务仍在排队、训练或回存，请等待结束或确认停止后再修改、删除");
    }
    public void preserveExecutionConfig(AlgorithmTraining existing, AlgorithmTraining update) {
        assertInactive(existing);
        try {
            Map<String, Object> original = json.readValue(existing.getConfigParams(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() { });
            Map<String, Object> merged = new LinkedHashMap<>(original);
            if (update.getConfigParams() != null && !update.getConfigParams().trim().isEmpty())
                merged.putAll(json.readValue(update.getConfigParams(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() { }));
            for (String key : Arrays.asList("cloudJobId", "computeNodeId", "computeNodeName", "annotationType", "trainType", "datasetArtifactRef", "runtimeDatasetPath", "modelSource"))
                if (original.containsKey(key)) merged.put(key, original.get(key));
            update.setConfigParams(json.writeValueAsString(merged));
        } catch (java.io.IOException e) { throw new ServiceException("训练配置格式无效"); }
    }
    public String containerLogs(Long containerId) {
        access().container(containerId);
        List<CloudTrainingJob> rows = jdbc.query("SELECT * FROM vls_cloud_training_job WHERE BINARY tenant_id=BINARY ? AND container_record_id=?",new BeanPropertyRowMapper<>(CloudTrainingJob.class),ComputeNodeService.tenant(),containerId);
        if (rows.isEmpty()) throw new ServiceException("线上训练记录不存在或无权访问");
        for(CloudTrainingJob job:rows)authorize(job,trainings.selectAlgorithmTrainingById(job.getTrainingId()));
        return rows.get(0).getLogTail()==null?"":rows.get(0).getLogTail();
    }
    public RemoteTrainingService.TrainingProgress progress(AlgorithmTraining training) {
        CloudTrainingJob job = current(training);
        RemoteTrainingService.TrainingProgress result = new RemoteTrainingService.TrainingProgress();
        result.setTaskId(training.getId()); result.setTotalEpochs(training.getEpochTotal()); result.setCurrentEpoch(job.getEpochCurrent());
        result.setPercentage(job.getProgress()); result.setStatus(training.getTrainStatus().getCode());
        result.setCompleted("COMPLETED".equals(job.getJobState())); result.setFailed("FAILED".equals(job.getJobState()));
        result.setMessage(job.getMessage() == null ? "等待所选实例调度" : job.getMessage());
        return result;
    }
    public RemoteTrainingService.LogResult logs(AlgorithmTraining training) {
        CloudTrainingJob job = current(training);
        RemoteTrainingService.LogResult result = new RemoteTrainingService.LogResult();
        result.setServerManaged(true);
        result.setLogPath(job.getRunDir() + "/training.log"); result.setLogContent(job.getLogTail() == null ? "" : job.getLogTail());
        result.setCurrentEpoch(job.getEpochCurrent()); result.setTotalEpoch(training.getEpochTotal()); result.setProgress(job.getProgress());
        result.setCompleted("COMPLETED".equals(job.getJobState())); result.setStatus(training.getTrainStatus().getCode()); result.setMessage(job.getMessage());
        result.setModelPath(training.getModelOutputPath()); return result;
    }
}
