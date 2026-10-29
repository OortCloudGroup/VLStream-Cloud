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
        Set<String> codes = new HashSet<>(), targets = new HashSet<>();
        if (properties.getResources() == null) throw invalid();
        for (ConfiguredComputeProperties.Resource resource : properties.getResources()) {
            if (resource == null || resource.getNodeId() == null || resource.getNodeId() >= -1
                || resource.getCode() == null || !resource.getCode().matches("[a-z][a-z0-9-]{1,63}")
                || !codes.add(resource.getCode()) || aliases.containsKey(resource.getNodeId())
                || resource.getName() == null || resource.getName().trim().isEmpty() || resource.getName().length() > 100
                || resource.getGpuModel() == null || resource.getGpuModel().trim().isEmpty()
                || resource.getGpuIndex() < 0 || resource.getGpuIndex() > 15 || resource.getMaxConcurrentTasks() != 1
                || !("self".equals(resource.getProvider()) || "autodl".equals(resource.getProvider())))
                throw invalid();
            if("autodl".equals(resource.getProvider())) {
                ComputeNode official=officialNode(resource);
                if(!targets.add("autodl:"+resource.getInstanceId()))throw invalid();
                register(resource,official);continue;
            }
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
            register(resource,node);
        }
    }

    private void register(ConfiguredComputeProperties.Resource resource,ComputeNode node) {
        resources.put(node.getId(),node);aliases.put(node.getId(),node.getId());
        if(resource.getLegacyNodeIds()==null)throw invalid();
        for(Long legacy:resource.getLegacyNodeIds())if(legacy==null||legacy<=0||aliases.putIfAbsent(legacy,node.getId())!=null)throw invalid();
    }
    private ComputeNode officialNode(ConfiguredComputeProperties.Resource resource) {
        if(resource.getInstanceId()==null||!resource.getInstanceId().matches("pro-[a-f0-9]{12}")
            ||resource.getAccountScope()==null||!resource.getAccountScope().matches("[A-Za-z0-9_.:-]{1,128}")
            ||resource.getTenantId()==null||resource.getTenantId().trim().isEmpty()||resource.getPort()<1||resource.getPort()>65535
            ||!"root".equals(resource.getUsername()))throw invalid();
        ComputeNode node=new ComputeNode();node.setId(resource.getNodeId());node.setConfigurationManaged(true);node.setConfigurationCode(resource.getCode());
        node.setProvider("autodl");node.setProviderProduct("AUTODL_PRO");node.setProviderInstanceId(resource.getInstanceId());
        node.setPublicInstanceKey(ModelArtifactObjectStore.hash("autodl\n"+resource.getAccountScope()+"\n"+resource.getInstanceId()));
        node.setTenantId(resource.getTenantId());node.setName(resource.getName());node.setGpuModel(resource.getGpuModel());node.setGpuIndex(resource.getGpuIndex());
        node.setHost(empty(resource.getHost())?null:resource.getHost());node.setPort(resource.getPort());node.setUsername(resource.getUsername());node.setHostKey(resource.getHostKey());
        node.setPythonPath(resource.getPythonPath());node.setWorkDir(resource.getWorkDir());ComputeNodeService.validatePath(node.getPythonPath());ComputeNodeService.validatePath(node.getWorkDir());
        if("/".equals(node.getWorkDir()))throw invalid();
        node.setAuthMode(ComputeDeploymentSshIdentity.KEY);node.setBillingType("POINTS");node.setControlMode("API");node.setSchedulingMode("INSTANCE_EXCLUSIVE");
        node.setResourceScope(resource.isPublished()?"PLATFORM":"COMPANY");node.setEnabled(resource.isEnabled());
        node.setPowerState("UNKNOWN");node.setProbeState("UNTESTED");node.setCapacityState("UNKNOWN");
        ConfiguredComputeProperties.FixedPrice price=resource.getFixedPrice();
        if(price!=null&&price.getPerInstanceHour()!=null) {
            if(price.getCurrency()==null||!price.getCurrency().matches("[A-Z]{3}")||price.getPerInstanceHour().signum()<=0||price.getPerInstanceHour().scale()>12)throw invalid();
            node.setUserHourlyPrice(price.getPerInstanceHour().toPlainString());node.setPriceCurrency(price.getCurrency());node.setPriceUnit("PER_INSTANCE_HOUR");node.setPricePolicy("VLS_FIXED");
        }
        return node;
    }
    private static boolean empty(String value){return value==null||value.trim().isEmpty();}
    boolean official(ComputeNode node){return node!=null&&node.isConfigurationManaged()&&"autodl".equals(node.getProvider())&&resources.containsKey(node.getId());}
    ConfiguredComputeProperties.Resource definition(ComputeNode node) {
        if(!official(node))throw unavailable("官方算力配置身份未匹配");
        return properties.getResources().stream().filter(r->node.getId().equals(r.getNodeId())&&node.getConfigurationCode().equals(r.getCode())).findFirst().orElseThrow(this::invalid);
    }
    void verifyOfficial(ComputeNode node){if(!official(node)||!fingerprint(resources.get(node.getId())).equals(fingerprint(node)))throw unavailable("官方实例配置身份已变化");}
    boolean providerTokenPresent(ComputeNode node){if(!official(node))return false;verifyOfficial(node);return !empty(definition(node).getProviderApiToken());}
    String providerToken(ComputeNode node){verifyOfficial(node);String token=definition(node).getProviderApiToken();if(empty(token)||token.contains("\r")||token.contains("\n"))throw unavailable("官方电源调用身份尚未配置");return token;}
    ComputeBillingRate fixedRate(ComputeNode node,JsonNode config) {
        if(config==null)throw unavailable("平台额度换算尚未确认");
        verifyOfficial(node);ConfiguredComputeProperties.Resource resource=definition(node);ConfiguredComputeProperties.FixedPrice price=resource.getFixedPrice();
        if(price==null||price.getPerInstanceHour()==null)throw unavailable("官方算力用户价格尚未配置");
        ComputeBillingRate rate=new ComputeBillingRate();rate.setPricePolicy("VLS_FIXED");rate.setUserPriceSource("OFFICIAL_CONFIG");rate.setFixedPriceConfigurationId(resource.getCode());
        rate.setFixedPriceUnit("PER_INSTANCE_HOUR");rate.setFixedHourlyPrice(price.getPerInstanceHour());rate.setCurrency(price.getCurrency());
        rate.setVersion(ModelArtifactObjectStore.hash(resource.getCode()+"\n"+price.getCurrency()+"\n"+price.getPerInstanceHour().toPlainString()));
        rate.setQuotaUnit(config.path("quota_unit").asText());rate.setConversionVersion(config.path("conversion_version").asText());
        try {rate.setQuotaPerCurrencyUnit(new java.math.BigDecimal(config.path("quota_per_currency_unit").path("numerator").asText()).divide(new java.math.BigDecimal(config.path("quota_per_currency_unit").path("denominator").asText()),java.math.MathContext.DECIMAL128));}
        catch(RuntimeException invalid){throw unavailable("平台额度换算尚未确认");}
        rate.setQuotaScale(0);rate.setQuotaRounding(java.math.RoundingMode.DOWN);rate.setValidFrom(config.path("effective_at").asText());rate.setValidUntil(config.path("valid_until").asText());return rate;
    }
    String officialBaseModel(String reference,String type,String owner) {
        if(reference.startsWith("@preset/"))return reference;
        for(Map<String,Object> row:jdbc.queryForList("SELECT options_json,annotation_type FROM vls_cloud_training_job WHERE BINARY tenant_id=BINARY ? AND BINARY public_owner_subject=BINARY ? AND job_state='COMPLETED' AND BINARY CONCAT('cloud-training/',node_id,'/',id,'/weights/best.pt')=BINARY ?",ModelArtifactObjectStore.tenant(),owner,reference))try {
            JsonNode source=json.readTree(String.valueOf(row.get("options_json"))).path("configuredResource");
            if(type.equals(row.get("annotation_type"))&&properties.getResources().stream().anyMatch(r->"autodl".equals(r.getProvider())&&r.getCode().equals(source.path("code").asText()))
                &&source.path("targetFingerprint").asText().matches("[a-f0-9]{64}"))return reference;
        }catch(IOException invalid){throw unavailable("归档模型来源无法核对");}
        throw unavailable("官方算力仅接受官方基础模型或本人已验证的官方训练归档，不能执行任意上传PT");
    }

    private IllegalStateException invalid() { return new IllegalStateException("Company compute configuration contains missing, duplicate or invalid identities/specifications"); }
    /** Hold persisted work until its explicit YAML identity is restored. Never infer process exit. */
    static final int UNAVAILABLE = 48010;
    static ServiceException unavailable(String message) { return new ServiceException(message, UNAVAILABLE); }
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
        if(node!=null)synchronized(monitor(node.getId())) { node.setProbeRequestedAt(new Date());if(official(node)){node.setProbeState("CHECKING");node.setProbeTime(null);} }
    }
    public List<ComputeNode> published() {
        List<ComputeNode> result = new ArrayList<>();
        for (ComputeNode node : resources.values()) if (node.isEnabled() && "PLATFORM".equals(node.getResourceScope())) {
            try { result.add(resolve(node.getId())); }
            catch (ServiceException unavailable) {
                ComputeNode copy = new ComputeNode(); BeanUtils.copyProperties(node, copy);
                copy.setProbeState("ERROR"); copy.setProbeTime(new Date());
                copy.setProbeJson(null); copy.setPowerState("UNKNOWN");
                copy.setLifecycleMessage(unavailable.getMessage()); result.add(copy);
            }
        }
        return result;
    }
    public ComputeNode resolve(Long id) {
        ComputeNode original = resources.get(canonicalId(id));
        if (original == null || original.getHost()==null) throw unavailable("自有算力未绑定有效的 YAML 受信资产，请管理员核对配置");
        if(official(original))return resolveOfficial(original);
        // A reserved ID must never silently alias a persisted third-party/private node.
        List<Object> parameters=new ArrayList<>(queueIds(id));
        parameters.add(original.getHost());parameters.add(original.getPort());parameters.add(original.getGpuIndex());
        for (ComputeNode persisted : jdbc.query("SELECT * FROM vls_compute_node WHERE id IN (" + placeholders(queueIds(id).size()) + ") OR (host=? AND port=? AND gpu_index=?)",
            new BeanPropertyRowMapper<>(ComputeNode.class), parameters.toArray())) {
            if (!queueIds(id).contains(persisted.getId()) || persisted.getId() < 0 || !sameTarget(original, persisted))
                throw unavailable("旧算力记录尚未正确映射或执行目标冲突，请管理员核对节点ID；暂不能使用或移除旧记录");
            if(persisted.getPowerOperationId()!=null)throw unavailable("旧服务器仍有挂起维护操作，不能使用配置供给或移除旧记录");
        }
        // Already-removed rows cannot supply their old ID. Never silently abandon their jobs
        // or infer a new alias from display names/paths: require explicit operator configuration.
        if(original.getHost()!=null)for(Map<String,Object> job:jdbc.queryForList("SELECT node_id,options_json FROM vls_cloud_training_job WHERE node_id NOT IN ("+placeholders(queueIds(id).size())+") AND LOCATE(?,run_dir)=1",
            jobAliasParameters(id,original.getWorkDir()+"/"))) {
            try {
                JsonNode shared=json.readTree(String.valueOf(job.get("options_json"))).path("sharedQueue");
                if(ComputeSharedQueueService.MODE.equals(shared.path("mode").asText()) && shared.path("gpuIndex").asInt(-1)==original.getGpuIndex())
                    throw unavailable("存在尚未映射的历史共享任务，请管理员核对旧节点ID；暂不能使用或移除旧记录");
            } catch(IOException invalid) {throw unavailable("历史任务快照无法核对，请管理员确认旧节点映射");}
        }
        ComputeNode copy = new ComputeNode(); synchronized (monitor(id)) { BeanUtils.copyProperties(original, copy); }
        return copy;
    }
    private ComputeNode resolveOfficial(ComputeNode original) {
        for(ComputeNode row:jdbc.query("SELECT * FROM vls_compute_node WHERE id=? OR (provider='autodl' AND provider_instance_id=?) OR (host=? AND port=?)",new BeanPropertyRowMapper<>(ComputeNode.class),original.getId(),original.getProviderInstanceId(),original.getHost(),original.getPort())) {
            if(!queueIds(original.getId()).contains(row.getId())||!sameTarget(original,row)||row.getPowerOperationId()!=null)
                throw unavailable("官方实例仍存在未映射或活动的旧连接，保持隔离；不恢复数据库供给");
        }
        if(!jdbc.queryForList("SELECT instance_key FROM vls_compute_public_instance WHERE provider='autodl' AND provider_instance_id=? AND instance_key<>?",original.getProviderInstanceId(),original.getPublicInstanceKey()).isEmpty())
            throw unavailable("官方实例已有另一公共身份，不能绕过原占用或隔离记录");
        ComputeNode copy=new ComputeNode();synchronized(monitor(original.getId())){BeanUtils.copyProperties(original,copy);}return copy;
    }
    void recordPower(ComputeNode expected,ComputePowerProvider.Snapshot observed) {
        ComputeNode node=resources.get(expected.getId());if(!official(node)||!fingerprint(node).equals(fingerprint(expected)))throw unavailable("官方实例控制身份已变化");
        synchronized(monitor(node.getId())) {
            if(!Objects.equals(node.getPowerState(),observed.power)){node.setProbeState("UNTESTED");node.setProbeTime(null);node.setProbeJson(null);}
            node.setPowerState(observed.power);node.setPowerCheckedAt(new Date());node.setCapacitySource("PROVIDER_API");node.setCapacityCheckedAt(new Date());node.setCapacityState(observed.capacity);
            node.setActualStoppedMode(observed.stoppedMode);node.setProviderChargeType(observed.chargeType);node.setProviderGpuAmount(observed.gpuAmount);
            if("start".equals(node.getPowerOperationAction())&&"RUNNING".equals(observed.power)||"stop".equals(node.getPowerOperationAction())&&"STOPPED".equals(observed.power))node.setPowerOperationId(null);
        }
    }
    void pending(ComputeNode expected,String operation,String action,boolean submitted) {
        ComputeNode node=resources.get(expected.getId());if(!official(node)||!fingerprint(node).equals(fingerprint(expected)))throw unavailable("官方实例控制身份已变化");
        synchronized(monitor(node.getId())){node.setPowerOperationId(operation);node.setPowerOperationAction(action);node.setPowerOperationSubmitted(submitted);node.setPowerOperationStartedAt(new Date());node.setProbeState("UNTESTED");node.setProbeTime(null);}
    }
    void cancelled(ComputeNode expected){ComputeNode node=resources.get(expected.getId());synchronized(monitor(node.getId())){node.setPowerOperationId(null);node.setPowerState("UNKNOWN");node.setPowerCheckedAt(null);}}
    public boolean replaces(ComputeNode node) {
        if (!contains(node.getId()) || canonical(node.getId())) return false;
        ComputeNode resource = resources.get(canonicalId(node.getId()));
        return sameTarget(resource, node);
    }
    private Object[] jobAliasParameters(Long id,String prefix) {
        List<Object> parameters=new ArrayList<>(queueIds(id));parameters.add(prefix);return parameters.toArray();
    }
    public boolean migrationPending(ComputeNode node) {
        if(node==null || node.isConfigurationManaged())return false;
        if("self".equals(node.getProvider()))return !replaces(node);
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
        if(official(configured))return "autodl".equals(row.getProvider())&&Objects.equals(configured.getProviderInstanceId(),row.getProviderInstanceId())
            &&Objects.equals(configured.getHost(),row.getHost())&&configured.getPort()==row.getPort()&&Objects.equals(configured.getUsername(),row.getUsername())
            &&Objects.equals(configured.getPythonPath(),row.getPythonPath())&&Objects.equals(configured.getWorkDir(),row.getWorkDir());
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
            if(official(node)&&(!"RUNNING".equals(node.getPowerState())||node.getPowerOperationId()!=null))return false;
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
        boolean shared=ComputeSharedQueueService.MODE.equals(node.getSchedulingMode());
        if (!Objects.equals(expected, job.getRunDir()) || shared&&(!ComputeSharedQueueService.MODE.equals(options.path("sharedQueue").path("mode").asText())
            || node.getGpuIndex() != options.path("sharedQueue").path("gpuIndex").asInt(-1))
            || !shared&&identity.isMissingNode()
            || (!identity.isMissingNode() && (!node.getConfigurationCode().equals(identity.path("code").asText())
            || !String.valueOf(node.getId()).equals(identity.path("nodeId").asText()) || !fingerprint(node).equals(identity.path("targetFingerprint").asText()))))
            throw new IOException("配置算力与本轮冻结执行目标不匹配，保留任务等待管理员核对");
    }
    static String placeholders(int count) { return String.join(",", Collections.nCopies(count, "?")); }
    private String fingerprint(ComputeNode node) {
        String target=node.getConfigurationCode() + "\n" + node.getTenantId() + "\n" + node.getHost() + "\n" + node.getPort()
            + "\n" + node.getUsername() + "\n" + node.getHostKey() + "\n" + node.getPythonPath() + "\n" + node.getWorkDir() + "\n" + node.getGpuIndex();
        if(official(node)){ConfiguredComputeProperties.Resource r=definition(node);target+="\n"+node.getProviderInstanceId()+"\n"+node.getPublicInstanceKey()+"\n"+r.getPrivateKeyFile()+"\n"+r.getPublicKeyFingerprint()+"\n"+ModelArtifactObjectStore.hash(String.valueOf(r.getProviderApiToken()));}
        return ModelArtifactObjectStore.hash(target);
    }
}
