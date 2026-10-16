package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import static com.ruoyi.vlstream.test.vlstream.compute.ComputeSsh.quote;

@Service
@RequiredArgsConstructor
public class ComputeSharedQueueService {
    public static final String MODE = "SHARED_GPU_QUEUE";
    public static final String EXCLUSIVE = "INSTANCE_EXCLUSIVE";
    private final ComputeSharedQueueProperties properties;
    private final ComputeTrustedSelfPolicy trusted;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public boolean isShared(ComputeNode node) { return MODE.equals(node.getSchedulingMode()); }
    public boolean authenticated() { return !viewer().isEmpty(); }
    String pythonCommand(ComputeNode node) {
        return (isShared(node)||declaredCompany(node)?"env -u PYTHONHOME PYTHONPATH='' PYTHONNOUSERSITE=1 ":"")+quote(node.getPythonPath());
    }
    protected String authenticatedUser() { try { return LoginHelper.getUserId(); } catch (Exception ignored) { return null; } }
    public String viewer() {
        String actor = authenticatedUser(); String environment = properties.getEnvironmentId();
        if (actor == null || actor.trim().isEmpty() || !("localhost".equals(environment) || "vm".equals(environment))) return "";
        return ModelArtifactObjectStore.hash("company-shared-vls-user\n" + environment + "\n" + ComputeNodeService.tenant() + "\n" + actor);
    }
    public String requireCreator(String creator) {
        String viewer = viewer();
        if (viewer.isEmpty() || !Objects.equals(authenticatedUser(), creator)) throw new ServiceException("只能提交当前已登录用户自己的共享GPU训练任务");
        return viewer;
    }
    public boolean declaredCompany(ComputeNode node) {
        try { return trusted.requireAsset(node).isCompanyPool(); }
        catch (IOException error) { return false; }
    }
    public void requireTarget(ComputeNode node) {
        try {
            if (!trusted.requireAsset(node).isCompanyPool()) throw new IOException("未登记为公司公共库存");
            trusted.resolve(node);
        } catch (IOException error) { throw new ServiceException("公司共享GPU未匹配已核验的部署资产身份"); }
        if (!("localhost".equals(properties.getEnvironmentId()) || "vm".equals(properties.getEnvironmentId())))
            throw new ServiceException("共享GPU缺少稳定平台环境标识");
        String prefix = "192.168.88.173".equals(node.getHost()) ? "/data/work/vlstream-gpu/" : "/home/oort/vlstream/";
        String python = "192.168.88.173".equals(node.getHost()) ? "/data/work/vlstream-gpu/runtime-173/envs/vls-standard/bin/python" : "/home/oort/vlstream/envs/vls-standard/bin/python";
        if (!"oort".equals(node.getUsername()) || !python.equals(node.getPythonPath()) || !(prefix + properties.getEnvironmentId()).equals(node.getWorkDir()))
            throw new ServiceException("公司共享GPU必须使用已初始化的独立标准环境和本平台工作目录");
    }
    public void requireOffering(ComputeNode node, ComputeRequests.Offer request, String mode) {
        if (!MODE.equals(mode)) {
            if (declaredCompany(node)) throw new ServiceException("公司公共GPU必须按任务共享排队，不能发布为整实例个人占用");
            return;
        }
        requireTarget(node);
        if (!"FREE".equals(request.getBillingType()) || node.getPublicInstanceKey()!=null)
            throw new ServiceException("公司共享GPU须保持免费且无个人整实例占用关系；既有公共实例不能自动转换");
        if(!isShared(node) && !jdbc.queryForList("SELECT id FROM vls_cloud_training_job WHERE node_id=? FOR UPDATE",String.class,node.getId()).isEmpty())
            throw new ServiceException("原节点已有专属训练历史，不能转换调度类型或改变历史授权；请先核对独立公司共享节点接入");
    }
    public void assertHostIdle(ComputeSsh ssh, ComputeNode node) {
        if (!declaredCompany(node) && !isShared(node)) return;
        requireTarget(node);
        try(ComputeSsh.Connection connection=ssh.open(node)) {
            String uuid=connection.execute("nvidia-smi -i "+node.getGpuIndex()+" --query-gpu=uuid --format=csv,noheader,nounits",20).trim();
            String processes=connection.execute("nvidia-smi -i "+node.getGpuIndex()+" --query-compute-apps=pid --format=csv,noheader,nounits",20).trim();
            Map<String,Object> gpu=new LinkedHashMap<>();gpu.put("gpuIndex",node.getGpuIndex());gpu.put("gpuUuid",uuid);
            Map<String,Object> payload=new LinkedHashMap<>();payload.put("gpu",gpu);
            JsonNode state=command(connection,node,"inspect",payload);
            if(!processes.isEmpty() || state.path("active").asBoolean() || state.path("waiting").asInt()!=0)
                throw new ServiceException("物理GPU仍有本地、线上或外部任务/归档租约，不能修改或移除节点");
        } catch(IOException error) { throw new ServiceException("无法确认物理GPU及跨环境队列空闲，保留节点配置"); }
    }
    public boolean ready(ComputeNode node) {
        if (!isShared(node) || !node.isEnabled() || !"PLATFORM".equals(node.getResourceScope()) || !"FREE".equals(node.getBillingType())
            || !"RUNNING".equals(node.getPowerState()) || node.getPowerOperationId() != null || node.getPublicInstanceKey() != null) return false;
        try { requireTarget(node); proof(node); return true; }
        catch (RuntimeException error) { return false; }
    }
    public JsonNode proof(ComputeNode node) {
        try {
            JsonNode proof = json.readTree(node.getProbeJson() == null ? "{}" : node.getProbeJson());
            if (proof.path("contractVersion").asInt() != 1 || !proof.path("ready").asBoolean() || !proof.path("pythonIsolated").asBoolean()
                || !(proof.path("pythonPrefix").asText()+"/bin/python").equals(node.getPythonPath()) || !proof.path("gpuUuid").asText().matches("GPU-[a-fA-F0-9-]{36}"))
                throw new ServiceException("共享GPU须重新核验标准环境及真实GPU身份");
            return proof;
        } catch (IOException error) { throw new ServiceException("共享GPU环境证据无效"); }
    }
    boolean retainBusyProof(ComputeNode node, JsonNode result) {
        if(!isShared(node) || !result.path("gpuBusy").asBoolean() || !result.path("pythonIsolated").asBoolean()
            || result.path("contractVersion").asInt()!=1 || !result.path("checks").isArray())return false;
        try {
            JsonNode before=proof(node);
            if(!before.path("pythonPrefix").equals(result.path("pythonPrefix")))return false;
            for(String name:new String[]{"python","torch","torchvision","cudaVersion","ultralytics"})
                if(!before.path(name).equals(result.path(name)))return false;
            int failures=0;
            for(JsonNode check:result.path("checks"))if(!check.path("passed").asBoolean()) {
                if(!"GPU 前后向运算与 Torchvision 算子".equals(check.path("name").asText()))return false;
                failures++;
            }
            return failures==1;
        } catch(RuntimeException error) {return false;}
    }
    public Map<String,Object> configuration(ComputeNode node, String jobId, String owner) {
        requireTarget(node); JsonNode proof = proof(node);
        Map<String,Object> value = new LinkedHashMap<>(); value.put("mode",MODE); value.put("environment",properties.getEnvironmentId());
        value.put("jobId",jobId); value.put("owner",owner); value.put("gpuUuid",proof.path("gpuUuid").asText());
        value.put("gpuIndex",node.getGpuIndex());
        Map<String,Object> runtime = new LinkedHashMap<>();
        for (String name : new String[]{"python","torch","torchvision","cudaVersion","ultralytics"}) runtime.put(name,proof.path(name).asText());
        value.put("runtime",runtime); return value;
    }
    public boolean isSharedJob(CloudTrainingJob job) {
        try { return MODE.equals(json.readTree(job.getOptionsJson()).path("sharedQueue").path("mode").asText()); }
        catch (Exception error) { return false; }
    }
    public String verifyBaseModel(String reference, String annotationType, String owner) {
        if (reference.startsWith("@preset/")) return reference;
        // Raw uploaded PT can execute Python. Shared hosts accept only official weights or
        // this user's verified checkpoints generated by this trusted shared training path.
        List<Map<String,Object>> sources=jdbc.queryForList("SELECT options_json,annotation_type FROM vls_cloud_training_job WHERE BINARY tenant_id=BINARY ? AND BINARY public_owner_subject=BINARY ? AND BINARY job_state=BINARY 'COMPLETED' AND BINARY CONCAT('cloud-training/',node_id,'/',id,'/weights/best.pt')=BINARY ?",
            ComputeNodeService.tenant(),owner,reference);
        for(Map<String,Object> source:sources) try {
            if(annotationType.equals(source.get("annotation_type")) && MODE.equals(json.readTree(String.valueOf(source.get("options_json"))).path("sharedQueue").path("mode").asText())) return reference;
        } catch(IOException ignored) { }
        throw new ServiceException("公司共享GPU仅接受系统基础权重或本池已核验归档的本人模型；任意上传PT请使用个人专属实例");
    }
    private Map<String,Object> ticket(CloudTrainingJob job) throws IOException {
        JsonNode shared=json.readTree(job.getOptionsJson()).path("sharedQueue");
        Map<String,Object> ticket=new LinkedHashMap<>(); ticket.put("environment",shared.path("environment").asText());
        ticket.put("jobId",job.getId());ticket.put("owner",job.getPublicOwnerSubject());ticket.put("runDir",job.getRunDir());return ticket;
    }
    private Map<String,Object> gpu(CloudTrainingJob job,ComputeNode node) throws IOException {
        JsonNode shared=json.readTree(job.getOptionsJson()).path("sharedQueue");
        if (!properties.getEnvironmentId().equals(shared.path("environment").asText()) || !job.getId().equals(shared.path("jobId").asText()) || !Objects.equals(job.getPublicOwnerSubject(),shared.path("owner").asText()))
            throw new IOException("共享任务环境或归属快照已变化");
        Map<String,Object> gpu=new LinkedHashMap<>();gpu.put("gpuIndex",shared.path("gpuIndex").asInt(-1));gpu.put("gpuUuid",shared.path("gpuUuid").asText());return gpu;
    }
    public void register(ComputeSsh.Connection connection,ComputeNode node,List<CloudTrainingJob> jobs) throws IOException {
        requireTarget(node); List<Map<String,Object>> batch=new ArrayList<>(); CloudTrainingJob first=null;
        for(CloudTrainingJob job:jobs) if(isSharedJob(job)) {if(first==null)first=job;batch.add(ticket(job));}
        if(first==null)return;
        Map<String,Object> selected=gpu(first,node);
        for(CloudTrainingJob job:jobs) if(isSharedJob(job) && !selected.equals(gpu(job,node))) throw new IOException("同节点共享任务的物理GPU快照不一致，请管理员核对");
        for(int offset=0;offset<batch.size();offset+=500) {
            Map<String,Object> payload=new LinkedHashMap<>();payload.put("gpu",selected);payload.put("tickets",batch.subList(offset,Math.min(offset+500,batch.size())));
            command(connection,node,"register",payload);
        }
    }
    public void release(ComputeSsh.Connection connection,ComputeNode node,CloudTrainingJob job,JsonNode result,String classesSha) throws IOException {
        Map<String,Object> acknowledgement=new LinkedHashMap<>();String state=result.path("state").asText();
        if("SUCCEEDED".equals(state)) {acknowledgement.put("state","ARCHIVED");acknowledgement.put("ptSha256",result.path("sha256").asText());acknowledgement.put("size",result.path("size").asLong());acknowledgement.put("classesSha256",classesSha);}
        else acknowledgement.put("state",state);
        Map<String,Object> payload=new LinkedHashMap<>();payload.put("gpu",gpu(job,node));payload.put("ticket",ticket(job));payload.put("acknowledgement",acknowledgement);
        command(connection,node,"release",payload);
    }
    private JsonNode command(ComputeSsh.Connection connection,ComputeNode node,String action,Map<String,Object> payload) throws IOException {
        requireTarget(node);
        String script=node.getWorkDir()+"/.vls-shared-queue-v1.py";
        try(java.io.InputStream input=new org.springframework.core.io.ClassPathResource("training/gpu_shared_queue.py").getInputStream()) {
            String source=new String(org.springframework.util.StreamUtils.copyToByteArray(input),StandardCharsets.UTF_8);
            String temporary=script+"."+java.util.UUID.randomUUID().toString()+".tmp";
            connection.putText(temporary,source);
            connection.execute("mv -- "+quote(temporary)+" "+quote(script),20);
        }
        String encoded=Base64.getEncoder().encodeToString(json.writeValueAsBytes(payload));
        String output=connection.execute(pythonCommand(node)+" "+quote(script)+" "+quote(action)+" "+quote(encoded),45);
        int marker=output.lastIndexOf("VLS_SHARED=");if(marker<0)throw new IOException("共享节点未返回队列确认");
        return json.readTree(output.substring(marker+11).trim());
    }
}
