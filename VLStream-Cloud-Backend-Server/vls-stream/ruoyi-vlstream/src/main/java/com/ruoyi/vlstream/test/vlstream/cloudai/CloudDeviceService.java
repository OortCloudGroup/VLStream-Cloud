package com.ruoyi.vlstream.test.vlstream.cloudai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.compute.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CloudDeviceService {
    public static final Set<String> TERMINAL = new HashSet<>(Arrays.asList("COMPLETED", "CANCELLED", "FAILED", "SKIPPED"));
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final ComputeNodeService nodes;
    private final ConfiguredComputeRegistry configured;
    private final ComputeSharedQueueService queue;
    private final CloudAiSources sources;
    private final CloudAiModels models;

    @org.springframework.beans.factory.annotation.Value("${token.tenant-type:single}")
    private String executionMode;
    @org.springframework.beans.factory.annotation.Value("${vlstream.compute.shared-pool.environment-id:}")
    private String executionEnvironment;

    String executionScope() {
        if (executionEnvironment == null || !executionEnvironment.matches("[A-Za-z0-9_-]{1,64}")
                || !("single".equals(executionMode) || "multi".equals(executionMode)))
            throw new ServiceException("云端检测执行环境标识未配置");
        return executionEnvironment + ":" + executionMode;
    }

    private static boolean freeSelf(ComputeNode node) {
        return !"POINTS".equals(node.getBillingType()) && "self".equalsIgnoreCase(node.getProvider());
    }
    private ComputeNode usable(Long nodeId, boolean ready) {
        if (nodeId == null) throw new ServiceException("请选择自有GPU");
        ComputeNode node = nodes.get(nodeId);
        if (!freeSelf(node)) throw new ServiceException("本期云端检测仅使用自有／公司GPU");
        if (!queue.isShared(node) && !nodes.canUsePrivate(node)) throw new ServiceException("无权使用此GPU", 403);
        if (queue.isShared(node) && queue.viewer().isEmpty()) throw new ServiceException("缺少共享GPU执行身份", 403);
        return ready ? nodes.requireReady(nodeId) : node;
    }

    private ObjectNode validate(CloudAiConfiguration input) {
        input.name = "云端算法配置"; if (input.description != null && input.description.length() > 1000) throw new ServiceException("说明最多1000字");
        if (input.cameras == null || input.cameras.isEmpty() || input.cameras.size() > 20) throw new ServiceException("请选择1至20路摄像机");
        if (input.models == null || input.models.isEmpty() || input.models.size() > 16) throw new ServiceException("请选择1至16个算法模型");
        if (input.preSeconds < 0 || input.preSeconds > 60 || input.postSeconds < 1 || input.postSeconds > 120)
            throw new ServiceException("录像前段支持0至60秒，后段支持1至120秒");
        if (!Double.isFinite(input.sampleFps) || input.sampleFps < 0.2 || input.sampleFps > 30) throw new ServiceException("检测频率须在0.2至30帧/秒之间");
        ComputeNode node = usable(input.nodeId, true);
        ObjectNode snapshot = json.valueToTree(input); snapshot.put("nodeId", String.valueOf(node.getId())); snapshot.put("nodeName", node.getName());
        snapshot.set("resource", json.valueToTree(configured.snapshot(node)));
        snapshot.put("queueOwner", queue.isShared(node) ? queue.viewer() : "");
        snapshot.put("shared", queue.isShared(node));
        snapshot.put("managementOwner", node.getManagementOwnerSubject());
        ArrayNode cameras = json.createArrayNode(), rules = json.createArrayNode(); Set<String> identities = new HashSet<>(), algorithms = new HashSet<>();
        for (String grantId : input.cameras) {
            JsonNode source = sources.owned(grantId);
            String identity = source.path("protocol").asText() + ":" + source.path("deviceRowId").asText() + ":" + source.path("streamId").asText();
            if (!identities.add(identity)) throw new ServiceException("不能重复选择相同摄像机视频源");
            cameras.add(source);
        }
        for (CloudAiConfiguration.ModelRule rule : input.models) {
            if (!Double.isFinite(rule.confidence) || rule.confidence <= 0 || rule.confidence > 1 || rule.reportInterval < 1 || rule.reportInterval > 86400)
                throw new ServiceException("置信度须大于0且不超过1，上报间隔须为1至86400秒");
            ObjectNode fixed = models.snapshot(rule.modelId, node);
            if (!algorithms.add(fixed.path("algorithmId").asText())) throw new ServiceException("同一算法只选择一个固定模型版本");
            if (rule.classes == null) rule.classes = new ArrayList<>();
            for (Integer category : rule.classes) if (category == null || !fixed.path("labels").has(String.valueOf(category))) throw new ServiceException("告警类别不属于所选模型");
            fixed.put("confidence", rule.confidence); fixed.put("reportInterval", rule.reportInterval); fixed.set("classes", json.valueToTree(new TreeSet<>(rule.classes)));
            rules.add(fixed);
        }
        snapshot.set("cameras", cameras); snapshot.set("models", rules);
        snapshot.put("executionScope", executionScope());
        return snapshot;
    }

    private static String tenant() { return CloudAiService.tenant(); }
    private static String actor() { return CloudAiService.actor(); }
    JsonNode read(String value) { try { return json.readTree(value); } catch (Exception e) { throw new ServiceException("云端配置数据异常"); } }
    String write(Object value) { try { return json.writeValueAsString(value); } catch (Exception e) { throw new ServiceException("云端配置序列化失败"); } }
    public static class Batch {
        public String operationId;
        public CloudAiConfiguration config;
        public Map<String,Integer> updates = new HashMap<>();
    }
    private static String cameraKey(JsonNode camera) {
        return com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore.hash(camera.path("protocol").asText()+":"+camera.path("deviceRowId").asText()+":"+camera.path("streamId").asText());
    }
    @Transactional(rollbackFor=Exception.class)
    public Map<String,Object> apply(Batch input) {
        if(input==null || input.operationId==null || !input.operationId.matches("[a-f0-9-]{36}") || input.config==null) throw new ServiceException("批量请求无效");
        String tenant=tenant(),actor=actor(),scope=executionScope();
        String request;
        try { request=json.writeValueAsString(input); } catch(Exception e) { throw new ServiceException("批量请求无效"); }
        String hash=com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore.hash(request);
        jdbc.update("INSERT IGNORE INTO vls_cloud_device_owner(tenant_id,actor_id,scope) VALUES(?,?,?)",tenant,actor,scope);
        jdbc.queryForMap("SELECT * FROM vls_cloud_device_owner WHERE tenant_id=? AND actor_id=? AND scope=? FOR UPDATE",tenant,actor,scope);
        List<Map<String,Object>> prior=jdbc.queryForList("SELECT * FROM vls_cloud_device_operation WHERE tenant_id=? AND actor_id=? AND scope=? AND id=?",tenant,actor,scope,input.operationId);
        if(!prior.isEmpty()) {
            if(!hash.equals(prior.get(0).get("request_hash"))) throw new ServiceException("同一操作编号不能提交不同配置",409);
            return Collections.singletonMap("accepted",true);
        }
        ObjectNode config=validate(input.config);
        List<Map<String,Object>> conflicts=new ArrayList<>();
        for(JsonNode camera:config.path("cameras")) for(JsonNode model:config.path("models")) {
            List<Map<String,Object>> existing=jdbc.queryForList("SELECT id,revision FROM vls_cloud_device_binding WHERE tenant_id=? AND actor_id=? AND scope=? AND camera_key=? AND algorithm_id=?",tenant,actor,scope,cameraKey(camera),model.path("algorithmId").asText());
            if(!existing.isEmpty()) {
                Map<String,Object> old=existing.get(0);
                if(input.updates==null || !Objects.equals(input.updates.get(String.valueOf(old.get("id"))),((Number)old.get("revision")).intValue())) {
                    Map<String,Object> conflict=new LinkedHashMap<>(old); conflict.put("camera",camera.path("name").asText()); conflict.put("model",model.path("name").asText()); conflicts.add(conflict);
                }
            }
        }
        if(!conflicts.isEmpty()) return Collections.singletonMap("conflicts",conflicts);
        long node=config.path("nodeId").asLong();
        String group=ensureGroup(tenant,actor,scope,node,config);
        Set<String> changed=new TreeSet<>(); changed.add(group);
        for(JsonNode camera:config.path("cameras")) for(JsonNode model:config.path("models")) {
            List<Map<String,Object>> existing=jdbc.queryForList("SELECT id,group_id FROM vls_cloud_device_binding WHERE tenant_id=? AND actor_id=? AND scope=? AND camera_key=? AND algorithm_id=?",tenant,actor,scope,cameraKey(camera),model.path("algorithmId").asText());
            ObjectNode rule=((ObjectNode)model).deepCopy();rule.put("sampleFps",input.config.sampleFps);rule.put("preSeconds",input.config.preSeconds);rule.put("postSeconds",input.config.postSeconds);
            if(existing.isEmpty()) jdbc.update("INSERT INTO vls_cloud_device_binding(id,tenant_id,actor_id,scope,group_id,camera_key,algorithm_id,camera_json,rule_json,enabled) VALUES(?,?,?,?,?,?,?,?,?,?)",UUID.randomUUID().toString(),tenant,actor,scope,group,cameraKey(camera),model.path("algorithmId").asText(),camera.toString(),rule.toString(),input.config.enabled);
            else { Map<String,Object> old=existing.get(0);changed.add(String.valueOf(old.get("group_id"))); jdbc.update("UPDATE vls_cloud_device_binding SET group_id=?,camera_json=?,rule_json=?,enabled=?,revision=revision+1 WHERE id=?",group,camera.toString(),rule.toString(),input.config.enabled,old.get("id")); }
        }
        for(String id:changed) changed(id);
        // Enforce aggregate capacity as well as per-batch limits, before committing any change.
        for(String id:changed) aggregate(id);
        jdbc.update("INSERT INTO vls_cloud_device_operation(id,tenant_id,actor_id,scope,request_hash) VALUES(?,?,?,?,?)",input.operationId,tenant,actor,scope,hash);
        return Collections.singletonMap("accepted",true);
    }
    private String ensureGroup(String tenant,String actor,String scope,long node,ObjectNode config) {
        List<String> ids=jdbc.queryForList("SELECT id FROM vls_cloud_device_group WHERE tenant_id=? AND actor_id=? AND scope=? AND node_id=?",String.class,tenant,actor,scope,node);
        String id=ids.isEmpty()?UUID.randomUUID().toString():ids.get(0);
        ObjectNode resource=config.deepCopy();resource.remove(Arrays.asList("cameras","models"));
        if(ids.isEmpty()) jdbc.update("INSERT INTO vls_cloud_device_group(id,tenant_id,actor_id,scope,node_id,resource_json) VALUES(?,?,?,?,?,?)",id,tenant,actor,scope,node,resource.toString());
        else jdbc.update("UPDATE vls_cloud_device_group SET resource_json=? WHERE id=?",resource.toString(),id);
        return id;
    }
    private void changed(String group) {
        jdbc.update("UPDATE vls_cloud_device_group SET revision=revision+1,retry_at=0 WHERE id=?",group);
        jdbc.update("UPDATE vls_cloud_device_run SET cancel_requested=1,message='配置已变更，正在收尾并重新应用' WHERE scene_id=? AND state NOT IN ('COMPLETED','FAILED','CANCELLED','SKIPPED')",group);
    }
    @Transactional(rollbackFor=Exception.class)
    public void enabled(String id,int revision,boolean enabled) {
        String tenant=tenant(),actor=actor(),scope=executionScope();
        jdbc.queryForMap("SELECT * FROM vls_cloud_device_owner WHERE tenant_id=? AND actor_id=? AND scope=? FOR UPDATE",tenant,actor,scope);
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT * FROM vls_cloud_device_binding WHERE id=? AND tenant_id=? AND actor_id=? AND scope=? FOR UPDATE",id,tenant,actor,scope);
        if(rows.isEmpty()) throw new ServiceException("配置不存在或无权操作",403);
        Map<String,Object> row=rows.get(0);if(((Number)row.get("revision")).intValue()!=revision) throw new ServiceException("配置已变更，请刷新后操作",409);
        jdbc.update("UPDATE vls_cloud_device_binding SET enabled=?,revision=revision+1 WHERE id=?",enabled,id); changed(String.valueOf(row.get("group_id")));
    }
    public Map<String,Object> page(int page,int size) {
        size=Math.min(100,Math.max(1,size));String tenant=tenant(),actor=actor(),scope=executionScope();
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT b.*,g.node_id,g.resource_json,r.state,r.message,r.detail_json FROM vls_cloud_device_binding b JOIN vls_cloud_device_group g ON g.id=b.group_id LEFT JOIN vls_cloud_device_run r ON r.id=g.run_id WHERE b.tenant_id=? AND b.actor_id=? AND b.scope=? ORDER BY b.create_time DESC,b.id LIMIT ? OFFSET ?",tenant,actor,scope,size,(Math.max(1,page)-1L)*size);
        for(Map<String,Object> row:rows) {
            row.put("camera",read(String.valueOf(row.remove("camera_json"))));ObjectNode rule=(ObjectNode)read(String.valueOf(row.remove("rule_json")));rule.remove(Arrays.asList("path","sha256","size"));row.put("rule",rule);
            row.put("nodeName",read(String.valueOf(row.remove("resource_json"))).path("nodeName").asText());
            row.remove("tenant_id");row.remove("actor_id");row.remove("camera_key");row.remove("scope");row.remove("detail_json");
        }
        Map<String,Object> result=new LinkedHashMap<>();result.put("records",rows);result.put("total",jdbc.queryForObject("SELECT COUNT(*) FROM vls_cloud_device_binding WHERE tenant_id=? AND actor_id=? AND scope=?",Long.class,tenant,actor,scope));return result;
    }
    ObjectNode aggregate(String group) {
        Map<String,Object> row=jdbc.queryForMap("SELECT * FROM vls_cloud_device_group WHERE id=?",group);
        ObjectNode result=(ObjectNode)read(String.valueOf(row.get("resource_json")));
        Map<String,JsonNode> cameras=new LinkedHashMap<>(),models=new LinkedHashMap<>();ArrayNode rules=json.createArrayNode();int pre=0,post=1;
        for(Map<String,Object> binding:jdbc.queryForList("SELECT * FROM vls_cloud_device_binding WHERE group_id=? AND enabled=1 ORDER BY id",group)) {
            JsonNode camera=read(String.valueOf(binding.get("camera_json")));ObjectNode rule=(ObjectNode)read(String.valueOf(binding.get("rule_json")));
            // Canonical camera key prevents separate grants from decoding the same stream twice.
            String key=String.valueOf(binding.get("camera_key"));cameras.putIfAbsent(key,camera);rule.put("cameraKey",cameras.get(key).path("grantId").asText());rule.put("bindingId",String.valueOf(binding.get("id")));
            String model=rule.path("modelId").asText();JsonNode old=models.putIfAbsent(model,rule);
            if(old!=null && !old.path("sha256").equals(rule.path("sha256"))) throw new ServiceException("同一模型版本内容冲突");
            rules.add(rule);pre=Math.max(pre,rule.path("preSeconds").asInt());post=Math.max(post,rule.path("postSeconds").asInt());
        }
        if(cameras.size()>32 || models.size()>16) throw new ServiceException("同一GPU最多配置32路摄像机和16个模型，请分配到其他GPU");
        result.set("cameras",json.valueToTree(cameras.values()));result.set("models",json.valueToTree(models.values()));result.set("rules",rules);
        result.put("preSeconds",pre);result.put("postSeconds",post);result.put("sceneName","云端算法配置");result.put("taskName","持续检测");result.put("executionScope",String.valueOf(row.get("scope")));result.put("sceneRevision",((Number)row.get("revision")).intValue());return result;
    }
}
