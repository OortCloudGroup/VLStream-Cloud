package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** No INSERT/UPDATE/seed of compute-node rows. Only probe evidence is held in memory. */
@Component
@RequiredArgsConstructor
public class ConfiguredComputeRegistry {
    private final ConfiguredComputeProperties properties;
    private final ComputeTrustedSelfAssetsProperties assets;
    private final ObjectMapper json;
    private final JdbcTemplate jdbc;
    private final Map<Long, ComputeNode> resources = new LinkedHashMap<>();
    private final Map<Long, Long> aliases = new LinkedHashMap<>();
    private final Map<Long, Object> monitors = new ConcurrentHashMap<>();

    @PostConstruct
    public void initialize() {
        resources.clear(); aliases.clear();
        if(!properties.isEnabled())return;
        Set<String> codes = new HashSet<>(), targets = new HashSet<>();
        if (properties.getResources() == null) throw invalid();
        for (ConfiguredComputeProperties.Resource resource : properties.getResources()) {
            if (resource == null || resource.getNodeId() == null || resource.getNodeId() >= -1
                || resource.getCode() == null || !resource.getCode().matches("[a-z][a-z0-9-]{1,63}")
                || !codes.add(resource.getCode()) || aliases.containsKey(resource.getNodeId())
                || resource.getName() == null || resource.getName().trim().isEmpty() || resource.getName().length() > 100
                || resource.getGpuModel() == null || resource.getGpuModel().trim().isEmpty()
                || resource.getGpuIndex() < 0 || resource.getGpuIndex() > 15 || resource.getMaxConcurrentTasks() != 1)
                throw invalid();
            List<ComputeTrustedSelfAssetsProperties.Asset> matches = new ArrayList<>();
            if (assets.getAssets() != null) for (ComputeTrustedSelfAssetsProperties.Asset asset : assets.getAssets())
                if (asset != null && (resource.getAssetId()!=null && !resource.getAssetId().trim().isEmpty()
                    ? Objects.equals(resource.getAssetId(),asset.getAssetId())
                    : resource.getAssetHost()!=null && Objects.equals(resource.getAssetHost(),asset.getHost()))) matches.add(asset);
            // Uninstalled environments may omit company assets entirely, but cannot advertise readiness.
            if (matches.size() > 1) throw invalid();
            ComputeNode node = new ComputeNode();
            node.setId(resource.getNodeId()); node.setConfigurationManaged(true); node.setConfigurationCode(resource.getCode());
            node.setName(resource.getName()); node.setGpuModel(resource.getGpuModel()); node.setGpuIndex(resource.getGpuIndex());
            node.setProvider("self"); node.setBillingType("FREE"); node.setControlMode("MANUAL");
            node.setSchedulingMode(ComputeSharedQueueService.MODE); node.setResourceScope(resource.isPublished() ? "PLATFORM" : "COMPANY");
            node.setAuthMode(ComputeDeploymentSshIdentity.KEY); node.setPythonPath(resource.getPythonPath()); node.setWorkDir(resource.getWorkDir());
            ComputeNodeService.validatePath(node.getPythonPath()); ComputeNodeService.validatePath(node.getWorkDir());
            if ("/".equals(node.getWorkDir())) throw invalid();
            node.setProbeState("UNTESTED"); node.setPowerState("UNKNOWN"); node.setCapacityState("UNKNOWN");
            node.setEnabled(resource.isEnabled());
            if (matches.size() == 1) {
                ComputeTrustedSelfAssetsProperties.Asset asset = matches.get(0);
                if (!asset.isCompanyPool() || asset.getTenantId() == null || asset.getTenantId().isEmpty()
                    || asset.getHost() == null || asset.getUsername() == null || asset.getHostKey() == null) throw invalid();
                node.setTenantId(asset.getTenantId()); node.setHost(asset.getHost()); node.setPort(asset.getPort());
                node.setUsername(asset.getUsername()); node.setHostKey(asset.getHostKey());
                if (!targets.add(node.getHost() + ":" + node.getPort() + "/" + node.getGpuIndex())) throw invalid();
            }
            resources.put(node.getId(), node); aliases.put(node.getId(), node.getId());
            if (resource.getLegacyNodeIds() == null) throw invalid();
            for (Long legacy : resource.getLegacyNodeIds()) {
                if (legacy == null || legacy <= 0 || aliases.putIfAbsent(legacy, node.getId()) != null) throw invalid();
            }
        }
    }

    private IllegalStateException invalid() { return new IllegalStateException("Company compute configuration contains missing, duplicate or invalid identities/specifications"); }
    public boolean contains(Long id) { return aliases.containsKey(id); }
    public boolean canonical(Long id) { return resources.containsKey(id); }
    public Long canonicalId(Long id) { return aliases.getOrDefault(id, id); }
    public List<Long> queueIds(Long id) {
        Long canonical = canonicalId(id); List<Long> ids = new ArrayList<>();
        for (Map.Entry<Long, Long> alias : aliases.entrySet()) if (canonical.equals(alias.getValue())) ids.add(alias.getKey());
        return ids.isEmpty() ? Collections.singletonList(id) : ids;
    }
    public Object monitor(Long id) { return monitors.computeIfAbsent(canonicalId(id), key -> new Object()); }
    public List<Long> allIds() { return new ArrayList<>(aliases.keySet()); }
    public void requireUnconfiguredEndpoint(String host, int port) {
        for(ComputeNode node:resources.values())if(Objects.equals(node.getHost(),host) && node.getPort()==port)
            throw new ServiceException("该自有服务器由服务配置提供，不能再创建数据库接入记录",403);
    }
    public void requestedProbe(ComputeNode snapshot) {
        ComputeNode node=resources.get(snapshot.getId());
        if(node!=null)synchronized(monitor(node.getId())) { node.setProbeRequestedAt(new Date()); }
    }
    public List<ComputeNode> published() {
        List<ComputeNode> result = new ArrayList<>();
        for (ComputeNode node : resources.values()) if (node.getHost()!=null && node.isEnabled() && "PLATFORM".equals(node.getResourceScope())) result.add(resolve(node.getId()));
        return result;
    }
    public ComputeNode resolve(Long id) {
        ComputeNode original = resources.get(canonicalId(id));
        if (original == null) throw new ServiceException("配置算力不存在");
        // A reserved ID must never silently alias a persisted third-party/private node.
        List<Object> parameters=new ArrayList<>(queueIds(id));
        parameters.add(original.getHost());parameters.add(original.getPort());parameters.add(original.getGpuIndex());
        for (ComputeNode persisted : jdbc.query("SELECT * FROM vls_compute_node WHERE id IN (" + placeholders(queueIds(id).size()) + ") OR (host=? AND port=? AND gpu_index=?)",
            new BeanPropertyRowMapper<>(ComputeNode.class), parameters.toArray())) {
            if (!queueIds(id).contains(persisted.getId()) || persisted.getId() < 0 || !sameTarget(original, persisted))
                throw new ServiceException("旧算力记录尚未正确映射或执行目标冲突，请管理员核对节点ID；暂不能使用或移除旧记录");
        }
        // Already-removed rows cannot supply their old ID. Never silently abandon their jobs
        // or infer a new alias from display names/paths: require explicit operator configuration.
        if(original.getHost()!=null)for(Map<String,Object> job:jdbc.queryForList("SELECT node_id,options_json FROM vls_cloud_training_job WHERE node_id NOT IN ("+placeholders(queueIds(id).size())+") AND LOCATE(?,run_dir)=1",
            jobAliasParameters(id,original.getWorkDir()+"/"))) {
            try {
                JsonNode shared=json.readTree(String.valueOf(job.get("options_json"))).path("sharedQueue");
                if(ComputeSharedQueueService.MODE.equals(shared.path("mode").asText()) && shared.path("gpuIndex").asInt(-1)==original.getGpuIndex())
                    throw new ServiceException("存在尚未映射的历史共享任务，请管理员核对旧节点ID；暂不能使用或移除旧记录");
            } catch(IOException invalid) {throw new ServiceException("历史任务快照无法核对，请管理员确认旧节点映射");}
        }
        ComputeNode copy = new ComputeNode(); synchronized (monitor(id)) { BeanUtils.copyProperties(original, copy); }
        return copy;
    }
    public boolean replaces(ComputeNode node) {
        if (!contains(node.getId()) || canonical(node.getId())) return false;
        ComputeNode resource = resources.get(canonicalId(node.getId()));
        if (!sameTarget(resource, node)) throw new ServiceException("配置算力与旧接入记录不匹配，请管理员核对");
        return true;
    }
    private Object[] jobAliasParameters(Long id,String prefix) {
        List<Object> parameters=new ArrayList<>(queueIds(id));parameters.add(prefix);return parameters.toArray();
    }
    public boolean migrationPending(ComputeNode node) {
        if(node==null || node.isConfigurationManaged())return false;
        if(canonical(node.getId()))return true;
        for(ComputeNode resource:resources.values())if(resource.getHost()!=null && Objects.equals(resource.getHost(),node.getHost())
            && resource.getPort()==node.getPort() && resource.getGpuIndex()==node.getGpuIndex())
            return !Objects.equals(aliases.get(node.getId()),resource.getId()) || !sameTarget(resource,node);
        return false;
    }
    public void requireRemovableAlias(Long id) {
        if(id==null || contains(id) || resources.isEmpty())return;
        for(ComputeNode node:jdbc.query("SELECT * FROM vls_compute_node WHERE id=?",new BeanPropertyRowMapper<>(ComputeNode.class),id))
            if(migrationPending(node))throw new ServiceException("旧算力记录尚未映射到配置资源，请管理员核对节点ID；不能移除",403);
    }
    private boolean sameTarget(ComputeNode configured, ComputeNode row) {
        return configured.getHost() != null && "self".equals(row.getProvider())
            && Objects.equals(configured.getTenantId(), row.getTenantId()) && Objects.equals(configured.getHost(), row.getHost())
            && configured.getPort() == row.getPort() && Objects.equals(configured.getUsername(), row.getUsername())
            && configured.getGpuIndex() == row.getGpuIndex() && Objects.equals(configured.getPythonPath(), row.getPythonPath())
            && Objects.equals(configured.getWorkDir(), row.getWorkDir()) && ComputeSharedQueueService.MODE.equals(row.getSchedulingMode())
            && "FREE".equals(row.getBillingType()) && row.getPublicInstanceKey() == null
            && (row.getHostKey() == null || Objects.equals(configured.getHostKey(), row.getHostKey()));
    }
    public void requireMutable(Long id) {
        if (contains(id)) throw new ServiceException("平台自有算力由服务配置管理，页面仅可使用或移除旧接入记录", 403);
        requireRemovableAlias(id);
    }
    public boolean recordProbe(ComputeNode snapshot, String state, String details, String hostKey) {
        ComputeNode node = resources.get(canonicalId(snapshot.getId()));
        if (node == null || !fingerprint(node).equals(fingerprint(snapshot)) || !Objects.equals(node.getHostKey(), hostKey)) return false;
        synchronized (monitor(node.getId())) {
            node.setProbeState(state); node.setProbeJson(details); node.setProbeTime(new Date());
            if ("READY".equals(state)) { node.setPowerState("RUNNING"); node.setPowerCheckedAt(new Date()); }
        }
        return true;
    }
    public Map<String, Object> snapshot(ComputeNode node) {
        Map<String, Object> value = new LinkedHashMap<>(); value.put("code", node.getConfigurationCode());
        value.put("nodeId", String.valueOf(canonicalId(node.getId()))); value.put("targetFingerprint", fingerprint(node)); return value;
    }
    public void verifyJob(CloudTrainingJob job, ComputeNode node) throws IOException {
        if (!node.isConfigurationManaged()) return;
        String expected = node.getWorkDir() + "/" + ModelArtifactObjectStore.hash(job.getTenantId()).substring(0, 16) + "/" + job.getId();
        JsonNode options = json.readTree(job.getOptionsJson());
        JsonNode identity = options.path("configuredResource");
        if (!Objects.equals(expected, job.getRunDir()) || !ComputeSharedQueueService.MODE.equals(options.path("sharedQueue").path("mode").asText())
            || node.getGpuIndex() != options.path("sharedQueue").path("gpuIndex").asInt(-1)
            || (!identity.isMissingNode() && (!node.getConfigurationCode().equals(identity.path("code").asText())
            || !String.valueOf(node.getId()).equals(identity.path("nodeId").asText()) || !fingerprint(node).equals(identity.path("targetFingerprint").asText()))))
            throw new IOException("配置算力与本轮冻结执行目标不匹配，保留任务等待管理员核对");
    }
    static String placeholders(int count) { return String.join(",", Collections.nCopies(count, "?")); }
    private String fingerprint(ComputeNode node) {
        return ModelArtifactObjectStore.hash(node.getConfigurationCode() + "\n" + node.getTenantId() + "\n" + node.getHost() + "\n" + node.getPort()
            + "\n" + node.getUsername() + "\n" + node.getHostKey() + "\n" + node.getPythonPath() + "\n" + node.getWorkDir() + "\n" + node.getGpuIndex());
    }
}
