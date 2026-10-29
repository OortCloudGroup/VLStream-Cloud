package com.ruoyi.vlstream.test.vlstream.compute;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import static com.ruoyi.vlstream.test.vlstream.compute.ComputeSsh.quote;

@Service
@RequiredArgsConstructor
public class ComputeNodeService {
    private final JdbcTemplate jdbc;
    private final ComputeCredentialCipher cipher;
    private final ComputeSsh ssh;
    private final ObjectMapper json;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ConfiguredComputeRegistry configured;
    boolean configured(Long id) { return configured != null && configured.contains(id); }
    boolean officialConfigured(ComputeNode node){return configured!=null&&configured.official(node);}
    ComputeBillingRate configuredFixedRate(ComputeNode node,JsonNode config){return configured.fixedRate(node,config);}
    void requireOfficialConnection(ComputeNode node){try{ssh.requireDeploymentIdentity(node);}catch(IOException invalid){throw new ServiceException("官方算力连接身份尚未配置，请管理员核对");}}
    void ensureOfficialIdentity(ComputeNode node){publicInventory().ensureConfiguredIdentity(node,configured.definition(node).getAccountScope());}
    void recordConfiguredPower(ComputeNode node,ComputePowerProvider.Snapshot snapshot){configured.recordPower(node,snapshot);}
    String officialBaseModel(String reference,String type,String owner){return configured.officialBaseModel(reference,type,owner);}
    void requireMutable(Long id) {
        if (configured != null) configured.requireMutable(id);
        if(id!=null)requireConfiguredSupply(stored(id,false));
    }
    static ComputeNode requireConfiguredSupply(ComputeNode node) {
        if("self".equals(node.getProvider()) && !node.isConfigurationManaged())
            throw ConfiguredComputeRegistry.unavailable("自有算力仅由 YAML 提供，旧数据库记录仅供历史关联；请管理员配置稳定ID和旧ID映射");
        return node;
    }
    List<Long> queueIds(Long id) { return configured == null ? java.util.Collections.singletonList(id) : configured.queueIds(id); }
    Long queueId(Long id) { return configured == null ? id : configured.canonicalId(id); }
    void verifyJob(CloudTrainingJob job, ComputeNode node) throws IOException { if (configured != null) configured.verifyJob(job, node); }
    java.util.Map<String,Object> configurationSnapshot(ComputeNode node) { return configured.snapshot(node); }
    void requestedProbe(ComputeNode node) { configured.requestedProbe(node); }
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ComputePlatformAuthorization platformAuthorization;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ComputePublicInventoryService publicInventory;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ComputeNodeOwnershipService ownership;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ComputeSharedQueueService sharedQueue;
    @org.springframework.beans.factory.annotation.Autowired private org.springframework.beans.factory.ObjectProvider<ComputePaidRunService> paidRuns;
    public boolean isCompanyShared(ComputeNode node){return sharedQueue!=null && sharedQueue.isShared(node);}
    ComputeNodeOwnershipService ownership(){if(ownership==null)throw new ServiceException("算力归属服务未就绪，不能管理个人资源");return ownership;}
    public void requireManagement(ComputeNode node){if(node.isConfigurationManaged() || configured!=null && configured.migrationPending(node))throw new ServiceException("平台自有算力由服务配置管理；旧记录身份须先核对",403);ownership().requireManagement(node);}
    public boolean canManage(ComputeNode node){return !node.isConfigurationManaged() && (configured==null || !configured.migrationPending(node)) && ownership().canManage(node);}
    public boolean canUsePrivate(ComputeNode node){return ownership().owns(node);}
    public String trainingSubject(ComputeNode node,String creator){
        if(isCompanyShared(node))return sharedQueue.requireCreator(creator);
        if(!"PLATFORM".equals(node.getResourceScope()) && node.getPublicInstanceKey()==null
            && ownership().isLocalOwner(node)) {
            ownership().requirePrivateUse(node);
            ownership().requireCreator(creator);
            // Private jobs use the established tenant + immutable local creator access path.
            // Never mint a public platform identity from a local VLS login.
            return null;
        }
        ComputePlatformAuthorization.Context actor=ownership().identity();
        if(!java.util.Objects.equals(actor.getVlsUserId(),creator))throw new ServiceException("只能提交当前用户自己的训练任务");
        if(!"PLATFORM".equals(node.getResourceScope()) && node.getPublicInstanceKey()==null)ownership().requirePrivateUse(node);
        return ComputePublicInventoryService.subject(actor);
    }
    ComputePublicInventoryService publicInventory() {
        if (publicInventory == null) throw new ServiceException("公共实例占用服务未就绪，不能操作或训练");
        return publicInventory;
    }
    public ComputePlatformAuthorization.Context platformAuthorization() {
        if (platformAuthorization==null) throw new ServiceException("平台授权校验暂不可用，收费算力操作已阻断",47002);
        return platformAuthorization.check();
    }
    public ComputePlatformAuthorization.Context authorizePaidOperation(Long id) {
        ComputeNode node=get(id);
        if (!"PLATFORM".equals(node.getResourceScope()) || !"POINTS".equals(node.getBillingType())) return null;
        if (platformAuthorization==null) throw new ServiceException("请先授权登录平台；收费算力接入尚未完成",47001);
        return platformAuthorization.requireIdentity();
    }

    public List<ComputeNode> list() {
        boolean administrator=ownership().administrator();
        String viewer=ownership().viewer(), legacy=ownership().legacyViewer();
        if(!administrator && viewer==null)ownership().requireViewer();
        List<ComputeNode> rows=jdbc.query("SELECT * FROM vls_compute_node WHERE BINARY tenant_id=BINARY ?"+(administrator?"":" AND management_owner_subject IN (?,?)")+" ORDER BY create_time DESC",
            mapper(),administrator?new Object[]{tenant()}:new Object[]{tenant(),viewer,legacy});
        for(ComputeNode node:rows) {
            node.setManagementOwnership(node.getManagementOwnerSubject()==null?"UNASSIGNED":java.util.Objects.equals(viewer,node.getManagementOwnerSubject()) || java.util.Objects.equals(legacy,node.getManagementOwnerSubject())?"MINE":"ADMIN");
            if(configured!=null) {
                node.setConfigurationMigrationPending(configured.migrationPending(node));
                node.setConfigurationReplaced(!node.isConfigurationMigrationPending() && configured.replaces(node));
            }
        }
        return rows;
    }
    public ComputeNode get(Long id) { return get(id, false); }
    public ComputeNode getManaged(Long id){ComputeNode node=get(id);requireManagement(node);return node;}
    public ComputeNode getOwned(Long id){
        return requireOwned(get(id));
    }
    private ComputeNode requireOwned(ComputeNode node) {
        if (!node.isSelfServiceNode())
            throw new ServiceException("自助接入仅管理本人的私人SSH节点；公共供给请使用原管理权限",403);
        ownership().requirePrivateUse(node);return node;
    }
    @Transactional(rollbackFor = Exception.class)
    public ComputeNode probeOwn(Long id) {
        requireOwned(lockServer(id));
        return probe(id);
    }
    @Transactional(rollbackFor = Exception.class)
    public void deleteOwn(Long id) {
        requireOwned(lockServer(id));
        delete(id);
    }
    @Transactional(rollbackFor = Exception.class)
    public ComputeNode saveOwn(Long id, ComputeRequests.Node request) {
        ownership().requireViewer();
        if (id != null) requireOwned(lockServer(id));
        if (!ComputeDeploymentSshIdentity.PASSWORD.equals(request.getAuthMode()))
            throw new ServiceException("自助接入使用SSH密码；后台部署身份须由有权限的管理员单独配置",403);
        List<ComputeNode> publicAliases = jdbc.query("SELECT * FROM vls_compute_node WHERE "
            + "(resource_scope='PLATFORM' OR public_instance_key IS NOT NULL OR scheduling_mode='SHARED_GPU_QUEUE') "
            + "AND ((host=? AND port=?) OR (tenant_id=? AND server_group IS NOT NULL AND server_group<>'' AND server_group=?)) FOR UPDATE",
            mapper(), request.getHost().trim().toLowerCase(Locale.ROOT), request.getPort(), tenant(), request.getServerGroup());
        if (!publicAliases.isEmpty()) throw new ServiceException("不能通过自助接入绑定公共算力或其别名",403);
        return save(id, request);
    }
    ComputeNode current(Long id) {
        if(configured(id))return configured.resolve(id);
        List<ComputeNode> rows = jdbc.query("SELECT * FROM vls_compute_node WHERE (tenant_id=? OR resource_scope='PLATFORM') AND id=? FOR UPDATE", mapper(), tenant(), id);
        if (rows.isEmpty()) throw new ServiceException("算力实例不存在或无权访问");
        return requireConfiguredSupply(rows.get(0));
    }
    private ComputeNode get(Long id, boolean lock) {
        if(configured(id))return configured.resolve(id);
        return requireConfiguredSupply(stored(id,lock));
    }
    private ComputeNode stored(Long id, boolean lock) {
        List<ComputeNode> rows = jdbc.query("SELECT * FROM vls_compute_node WHERE (tenant_id=?" + (lock ? "" : " OR resource_scope='PLATFORM'") + ") AND id=?" + (lock ? " FOR UPDATE" : ""), mapper(), tenant(), id);
        if (rows.isEmpty()) throw ConfiguredComputeRegistry.unavailable("算力实例不存在或无权访问，活动任务保留等待管理员核对");
        return rows.get(0);
    }
    public ComputeNode requireReady(Long id) {
        ComputeNode node = current(id);
        if(isCompanyShared(node)) {
            if(!sharedQueue.ready(node))throw new ServiceException("公司共享GPU未完成可信环境/身份检查或已撤下，请管理员核对");
            if(serverRows(node).stream().anyMatch(n->n.getPowerOperationId()!=null))throw new ServiceException("服务器仍有挂起维护操作，不能提交共享GPU任务");
            return node;
        }
        if(sharedQueue!=null && sharedQueue.declaredCompany(node))throw new ServiceException("公司GPU须通过共享任务队列使用，不能作为私人或整机独占实例训练");
        if (node.getPublicInstanceKey()==null) {
            List<ComputeNode> publicAliases=jdbc.query("SELECT * FROM vls_compute_node WHERE public_instance_key IS NOT NULL AND host=? AND port=? ORDER BY id FOR UPDATE",mapper(),node.getHost(),node.getPort());
            if(!publicAliases.isEmpty()) node.setPublicInstanceKey(publicAliases.get(0).getPublicInstanceKey());
        }
        if ("PLATFORM".equals(node.getResourceScope()) || node.getPublicInstanceKey()!=null)
            publicInventory().requireOwner(node);
        else ownership().requirePrivateUse(node);
        if (serverRows(node).stream().anyMatch(n -> n.getPowerOperationId() != null))
            throw new ServiceException("服务器开关机操作尚未确认，不能调度 GPU 训练");
        if (!ComputeAvailability.ready(node, System.currentTimeMillis())) throw new ServiceException("算力状态已过期或未就绪，请刷新并检查连接；关机实例请先到算力市场开机");
        if ("PLATFORM".equals(node.getResourceScope()) && !"FREE".equals(node.getBillingType())) {
            ComputePaidRunService paid=paidRuns==null?null:paidRuns.getIfAvailable();
            if(paid==null)throw new ServiceException("付费开机服务暂不可用");
            paid.requireTraining(node);
        }
        try {
            JsonNode check = json.readTree(node.getProbeJson() == null ? "{}" : node.getProbeJson());
            if (check.path("contractVersion").asInt() != 1 || !check.path("ready").asBoolean())
                throw new ServiceException("请重新检查实例环境，确认标准依赖、GPU 运算和基础模型缓存均已就绪");
        } catch (IOException e) { throw new ServiceException("环境检查记录无效，请重新检查"); }
        return node;
    }

    @Transactional(rollbackFor = Exception.class)
    public ComputeNode save(Long id, ComputeRequests.Node request) {
        if("self".equals(request.getProvider()))throw new ServiceException("自有算力仅在服务 YAML 配置，不能从页面创建或修改",403);
        requireMutable(id);
        String host = request.getHost().trim().toLowerCase(Locale.ROOT);
        if(configured!=null)configured.requireUnconfiguredEndpoint(host,request.getPort());
        if (!host.matches("[a-z0-9][a-z0-9.-]{0,252}") || host.contains("..")) throw new ServiceException("SSH 地址仅填写主机名或 IPv4 地址");
        validatePath(request.getPythonPath()); validatePath(request.getWorkDir());
        if ("/".equals(request.getWorkDir())) throw new ServiceException("请使用专用工作目录");
        if (id != null) lockServer(id);
        ComputeNode before = id == null ? null : get(id, true);
        if(before!=null)requireManagement(before);
        String owner=before==null?ownership().requireViewer():before.getManagementOwnerSubject();
        if (before != null) assertIdle(id);
        List<ComputeNode> aliases=jdbc.query("SELECT * FROM vls_compute_node WHERE id<>? AND public_instance_key IS NOT NULL AND ((host=? AND port=?) OR (tenant_id=? AND server_group IS NOT NULL AND server_group<>'' AND server_group=?)) ORDER BY id FOR UPDATE",mapper(),id==null?-1L:id,host,request.getPort(),tenant(),request.getServerGroup());
        if(aliases.stream().anyMatch(n -> !tenant().equals(n.getTenantId()))) throw new ServiceException("该端点已登记为其他管理方的公共实例，不能创建或改址为私有别名");
        for(ComputeNode alias:jdbc.query("SELECT * FROM vls_compute_node WHERE id<>? AND public_instance_key IS NULL AND ((host=? AND port=?) OR (tenant_id=? AND server_group IS NOT NULL AND server_group<>'' AND server_group=?)) ORDER BY id FOR UPDATE",mapper(),id==null?-1L:id,host,request.getPort(),tenant(),request.getServerGroup()))requireManagement(alias);
        for(ComputeNode alias:aliases) {requireManagement(alias);publicInventory().requireEditable(alias);}
        if(aliases.stream().map(ComputeNode::getPublicInstanceKey).distinct().count()>1
            || aliases.stream().anyMatch(n->!java.util.Objects.equals(request.getProvider(),n.getProvider())))
            throw new ServiceException("目标端点的公共身份或厂商不一致，请先核对物理实例");
        if(before!=null && before.getPublicInstanceKey()==null && !aliases.isEmpty()
            && !jdbc.queryForList("SELECT id FROM vls_cloud_training_job WHERE node_id=? FOR UPDATE",String.class,before.getId()).isEmpty())
            throw new ServiceException("原节点存在历史工作区且缺少安全清理证明，不能改址关联公共实例");
        if(before!=null && before.getPublicInstanceKey()!=null && (!host.equals(before.getHost()) || request.getPort()!=before.getPort()
            || !java.util.Objects.equals(request.getProvider(),before.getProvider()) || !java.util.Objects.equals(request.getServerGroup(),before.getServerGroup())))
            throw new ServiceException("公共实例身份已绑定，不能通过修改SSH地址、厂商或服务器归组静默重绑；请先核对独立实例接入");
        if(before!=null && before.getPublicInstanceKey()!=null && aliases.stream().anyMatch(n->!before.getPublicInstanceKey().equals(n.getPublicInstanceKey())))
            throw new ServiceException("目标端点属于不同公共实例，不能保留旧身份改址");
        if (before != null && !request.getProvider().equals(before.getProvider()) && before.isProviderCredentialConfigured())
            throw new ServiceException("请先停用厂商 API 并清除凭据，再更换厂商");
        String mode = request.getAuthMode();
        if (!ComputeDeploymentSshIdentity.PASSWORD.equals(mode) && !ComputeDeploymentSshIdentity.KEY.equals(mode))
            throw new ServiceException("SSH认证模式无效");
        if (ComputeDeploymentSshIdentity.KEY.equals(mode)) {
            if (!ownership().administrator()) throw new ServiceException("只有资源管理员可以绑定后台部署SSH身份");
            if (request.getPassword() != null && !request.getPassword().isEmpty())
                throw new ServiceException("后台部署SSH身份模式不能提交密码");
            ComputeNode candidate = new ComputeNode();
            candidate.setTenantId(tenant()); candidate.setProvider(request.getProvider());
            candidate.setHost(host); candidate.setPort(request.getPort()); candidate.setUsername(request.getUsername());
            candidate.setAuthMode(mode);
            candidate.setManagementOwnerSubject(owner);
            candidate.setResourceScope(before == null ? "PRIVATE" : before.getResourceScope());
            candidate.setBillingType(before == null ? "EXTERNAL" : before.getBillingType());
            candidate.setSchedulingMode(before == null ? "INSTANCE_EXCLUSIVE" : before.getSchedulingMode());
            candidate.setProviderProduct(before == null ? null : before.getProviderProduct());
            candidate.setProviderInstanceId(before == null ? null : before.getProviderInstanceId());
            candidate.setHostKey(before == null || !host.equals(before.getHost()) || request.getPort() != before.getPort()
                ? null : before.getHostKey());
            candidate.setPublicInstanceKey(before != null && before.getPublicInstanceKey() != null ? before.getPublicInstanceKey()
                : aliases.isEmpty() ? null : aliases.get(0).getPublicInstanceKey());
            try { ssh.requireDeploymentIdentity(candidate); }
            catch (IOException e) { throw new ServiceException("后台部署SSH身份尚未匹配管理员明确授权的实例配置"); }
        }
        String secret = before == null ? null : before.getPasswordCipher();
        if (ComputeDeploymentSshIdentity.PASSWORD.equals(mode) && request.getPassword() != null && !request.getPassword().isEmpty()) {
            try { secret = cipher.encrypt(request.getPassword()); }
            catch (RuntimeException e) { throw new ServiceException("算力凭据保存失败，请联系管理员检查后端持久化目录、原密钥及数据库升级状态"); }
        }
        if (ComputeDeploymentSshIdentity.PASSWORD.equals(mode)
            && (secret == null || (before != null && ComputeDeploymentSshIdentity.KEY.equals(before.getAuthMode())
                && (request.getPassword() == null || request.getPassword().isEmpty()))))
            throw new ServiceException("请输入新的 SSH 密码");
        Long key = id == null ? IdWorker.getId() : id;
        boolean sameEndpoint = before != null && host.equals(before.getHost()) && request.getPort() == before.getPort() && request.getUsername().equals(before.getUsername());
        try {
            if (before == null) jdbc.update("INSERT INTO vls_compute_node(id,tenant_id,name,host,port,username,auth_mode,password_cipher,python_path,work_dir,gpu_index,enabled,management_owner_subject) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                key, tenant(), request.getName().trim(), host, request.getPort(), request.getUsername(), mode, secret, request.getPythonPath(), request.getWorkDir(), request.getGpuIndex(), request.isEnabled(),owner);
            else jdbc.update("UPDATE vls_compute_node SET name=?,host=?,port=?,username=?,auth_mode=?,password_cipher=?,host_key=?,python_path=?,work_dir=?,gpu_index=?,enabled=?,probe_state='UNTESTED',probe_json=NULL,probe_time=NULL,update_time=NOW() WHERE tenant_id=? AND id=?",
                request.getName().trim(), host, request.getPort(), request.getUsername(), mode, secret, sameEndpoint ? before.getHostKey() : null,
                request.getPythonPath(), request.getWorkDir(), request.getGpuIndex(), request.isEnabled(), tenant(), key);
        } catch (org.springframework.dao.DuplicateKeyException e) { throw new ServiceException("此 SSH 实例已接入，请使用现有记录"); }
        jdbc.update("UPDATE vls_compute_node SET provider=?,server_group=?,gpu_model=? WHERE tenant_id=? AND id=?", request.getProvider(), request.getServerGroup(), request.getGpuModel(), tenant(), key);
        if (before==null || before.getPublicInstanceKey()==null) {
            if(!aliases.isEmpty()) jdbc.update("UPDATE vls_compute_node SET public_instance_key=? WHERE id=?",aliases.get(0).getPublicInstanceKey(),key);
        }
        return get(key);
    }

    public ComputeNode probe(Long id) {
        requireMutable(id);
        ComputeNode node = get(id, true);
        requireManagement(node);
        if (node.getPublicInstanceKey()!=null) publicInventory().requireEditable(node);
        if (serverRows(node).stream().anyMatch(n -> n.getPowerOperationId() != null)) throw new ServiceException("实例启停尚未确认，请先刷新厂商状态");
        probeSnapshot(node);
        return getManaged(id);
    }

    /** Fixed environment check; callers must first authorize and reserve the immutable snapshot. */
    boolean probeSnapshot(ComputeNode node) {
        String details; String state = "ERROR"; String key = node.getHostKey();
        try (ComputeSsh.Connection connection = ssh.open(node)) {
            key = connection.hostKey();
            String encoded;
            try (java.io.InputStream input = new org.springframework.core.io.ClassPathResource("training/check_cloud_environment.py").getInputStream()) {
                encoded = java.util.Base64.getEncoder().encodeToString(org.springframework.util.StreamUtils.copyToByteArray(input));
            }
            String script = "import base64;exec(compile(base64.b64decode('" + encoded + "'),'vls-environment-check','exec'))";
            String output = connection.execute((sharedQueue==null?quote(node.getPythonPath()):sharedQueue.pythonCommand(node)) + " -c " + quote(script)
                + " --work-dir " + quote(node.getWorkDir()) + " --gpu " + node.getGpuIndex(), 180);
            int marker = output.lastIndexOf("VLS_PROBE=");
            if (marker < 0) throw new IOException("环境检查无有效结果");
            JsonNode result = json.readTree(output.substring(marker + "VLS_PROBE=".length()).trim());
            if(sharedQueue!=null && sharedQueue.retainBusyProof(node,result))return true;
            details = json.writeValueAsString(result);
            if (result.path("contractVersion").asInt() == 1 && result.path("ready").asBoolean()) state = "READY";
        } catch (Exception e) { details = "{\"message\":\"无法完成环境检查，请先按接入指南初始化实例，并核对 SSH 和初始化脚本输出的 Python 路径\"}"; }
        if(node.isConfigurationManaged())return configured.recordProbe(node,state,details,key);
        // A probe from an older edit must never mark a different connection ready.
        return jdbc.update("UPDATE vls_compute_node SET probe_state=?,probe_json=?,host_key=?,probe_time=NOW(),power_state=IF(?='READY','RUNNING',power_state),power_checked_at=IF(?='READY',NOW(),power_checked_at) WHERE tenant_id=? AND id=? AND host=? AND port=? AND username=? AND auth_mode=? AND password_cipher <=> ? AND python_path=? AND work_dir=? AND gpu_index=? AND power_operation_id IS NULL AND lifecycle_revision=? AND resource_scope=? AND enabled=? AND management_owner_subject <=> ?",
            state, details, key, state, state, node.getTenantId(), node.getId(), node.getHost(), node.getPort(), node.getUsername(), node.getAuthMode(), node.getPasswordCipher(), node.getPythonPath(), node.getWorkDir(), node.getGpuIndex(), node.getLifecycleRevision(), node.getResourceScope(), node.isEnabled(),node.getManagementOwnerSubject()) == 1;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if(configured!=null)configured.requireRemovableAlias(id);
        if(configured(id)) {
            if(configured.canonical(id))throw new ServiceException("配置算力不能从页面删除",403);
            ComputeNode legacy=stored(id,true); ownership().requireManagement(legacy);
            configured.replaces(legacy); configured.resolve(id);
            assertNoActiveJobs(id);
            if(sharedQueue!=null)sharedQueue.assertHostIdle(ssh,configured.resolve(id));
            jdbc.update("DELETE FROM vls_compute_node WHERE tenant_id=? AND id=?",tenant(),id);
            return;
        }
        lockServer(id); requireManagement(get(id, true)); assertIdle(id);
        jdbc.update("DELETE FROM vls_compute_node WHERE tenant_id=? AND id=?", tenant(), id);
    }
    void assertIdle(Long id) {
        assertNoActiveJobs(id);
        ComputeNode node = current(id);
        if(sharedQueue!=null)sharedQueue.assertHostIdle(ssh,node);
        if (node.getPublicInstanceKey()!=null) publicInventory().requireEditable(node);
        if (serverRows(node).stream().anyMatch(n -> n.getPowerOperationId() != null))
            throw new ServiceException("服务器开关机结果尚未确认，不能修改或移除");
    }
    void assertNoActiveJobs(Long id) {
        if(configured(id)) {
            List<Long> ids=queueIds(id);
            if(!jdbc.queryForList("SELECT id FROM vls_cloud_training_job WHERE node_id IN ("+ConfiguredComputeRegistry.placeholders(ids.size())+") AND job_state NOT IN ('COMPLETED','FAILED','CANCELLED') FOR UPDATE",String.class,ids.toArray()).isEmpty())
                throw new ServiceException("该配置算力仍有排队、运行或回存任务，请结束后再移除旧记录");
            return;
        }
        // A locking current read sees jobs committed while we waited for the server lock, even at RR.
        if (!jdbc.queryForList("SELECT j.id FROM vls_cloud_training_job j JOIN vls_compute_node n ON n.id=j.node_id JOIN vls_compute_node target ON target.id=? WHERE (n.id=target.id OR (target.public_instance_key IS NOT NULL AND (n.public_instance_key=target.public_instance_key OR j.public_instance_key=target.public_instance_key)) OR (target.public_instance_key IS NULL AND n.public_instance_key IS NULL AND ((n.host=target.host AND n.port=target.port) OR (target.server_group IS NOT NULL AND target.server_group<>'' AND n.server_group=target.server_group AND n.tenant_id=target.tenant_id) OR (target.provider_instance_id IS NOT NULL AND target.provider_instance_id<>'' AND n.provider=target.provider AND n.provider_instance_id=target.provider_instance_id)))) AND j.job_state NOT IN ('COMPLETED','FAILED','CANCELLED') FOR UPDATE", id).isEmpty())
            throw new ServiceException("实例仍有排队、运行或回存任务，请结束后再修改或移除");
    }
    ComputeNode lockServer(Long id) {
        if(configured(id))return configured.resolve(id);
        ComputeNode node = get(id);
        List<ComputeNode> rows = serverRows(node);
        ComputeNode locked = rows.stream().filter(n -> id.equals(n.getId())).findFirst().orElseThrow(() -> new ServiceException("实例已移除，请刷新"));
        if (!java.util.Objects.equals(node.getHost(), locked.getHost()) || node.getPort()!=locked.getPort()
            || !java.util.Objects.equals(node.getServerGroup(), locked.getServerGroup())
            || !java.util.Objects.equals(node.getProviderInstanceId(), locked.getProviderInstanceId())
            || !java.util.Objects.equals(node.getPublicInstanceKey(), locked.getPublicInstanceKey())
            || !java.util.Objects.equals(node.getTenantId(), locked.getTenantId())) throw new ServiceException("实例配置在等待期间已变化，请刷新重试");
        // Visibility itself must also be rechecked using the current row, not the old RR snapshot.
        if (!tenant().equals(locked.getTenantId()) && !"PLATFORM".equals(locked.getResourceScope())) throw new ServiceException("算力访问权限已变化，请刷新");
        return locked;
    }
    static final String SERVER_WHERE = "(id=? OR (public_instance_key IS NOT NULL AND public_instance_key=?) OR (public_instance_key IS NULL AND ? IS NULL AND ((host=? AND port=?) OR (server_group IS NOT NULL AND server_group<>'' AND server_group=? AND tenant_id=?) OR (provider_instance_id IS NOT NULL AND provider_instance_id<>'' AND provider=? AND provider_instance_id=?))))";
    static Object[] serverArgs(ComputeNode n) { return new Object[]{n.getId(), n.getPublicInstanceKey(), n.getPublicInstanceKey(), n.getHost(), n.getPort(), n.getServerGroup(), n.getTenantId(), n.getProvider(), n.getProviderInstanceId()}; }
    static Object[] serverParams(ComputeNode n, Object... values) {
        Object[] result = java.util.Arrays.copyOf(values, values.length + 9);
        System.arraycopy(serverArgs(n), 0, result, values.length, 9); return result;
    }
    List<ComputeNode> serverRows(ComputeNode node) { return node.isConfigurationManaged()?java.util.Collections.singletonList(node):jdbc.query("SELECT * FROM vls_compute_node WHERE " + SERVER_WHERE + " ORDER BY id FOR UPDATE", mapper(), serverArgs(node)); }
    public static void validatePath(String path) {
        if (path == null || !path.matches("/[A-Za-z0-9_./-]+") || path.contains("..") || path.contains("//")) throw new ServiceException("请填写不含空格和特殊字符的 Linux 绝对路径");
    }
    static String tenant() { return ModelArtifactObjectStore.tenant(); }
    private static BeanPropertyRowMapper<ComputeNode> mapper() { return new BeanPropertyRowMapper<>(ComputeNode.class); }
}
