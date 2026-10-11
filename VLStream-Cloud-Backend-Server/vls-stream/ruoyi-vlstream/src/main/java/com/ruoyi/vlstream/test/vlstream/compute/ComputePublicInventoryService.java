package com.ruoyi.vlstream.test.vlstream.compute;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Persistent whole-instance claims, not another scheduler or a payment gateway.
 * Mutation callers hold the existing node/server transaction locks before this row lock. */
@Service
@RequiredArgsConstructor
public class ComputePublicInventoryService {
    private final JdbcTemplate jdbc;
    @Autowired(required = false) private ComputePlatformAuthorization authorization;

    @Data
    public static class Instance {
        private String instanceKey;
        private String provider;
        private String accountScope;
        private String providerInstanceId;
        private String publicState;
        private String ownerSubject;
        private String claimId;
        private long generation;
        private boolean workspaceDirty;
    }

    public ComputePlatformAuthorization.Context identity() {
        if (authorization == null) throw new ServiceException("公共算力需要可信平台授权，身份校验暂不可用",47002);
        ComputePlatformAuthorization.Context proof = authorization.requireIdentity();
        if (proof == null || !proof.isAuthorized() || blank(proof.getPlatformUserId()) || blank(proof.getVlsUserId())
            || !Objects.equals(ComputeNodeService.tenant(), proof.getVlsTenantId()))
            throw new ServiceException("公共算力身份或当前租户已变化，请重新授权",47001);
        return proof;
    }

    public String viewer() {
        try { return subject(identity()); } catch (ServiceException e) { return null; }
    }

    static String subject(ComputePlatformAuthorization.Context proof) {
        // One configured trusted SSO authority. Tenant is app isolation, never part of its stable user identity.
        return ModelArtifactObjectStore.hash("trusted-platform-user\n" + proof.getPlatformUserId());
    }

    Instance read(ComputeNode node, boolean lock) {
        if (blank(node.getPublicInstanceKey())) return null;
        List<Instance> rows = jdbc.query("SELECT * FROM vls_compute_public_instance WHERE instance_key=?" + (lock ? " FOR UPDATE" : ""),
            new BeanPropertyRowMapper<>(Instance.class), node.getPublicInstanceKey());
        return rows.isEmpty() ? null : rows.get(0);
    }

    String state(ComputeNode node, String viewer) {
        Instance row = read(node,false);
        if (row == null || !("FREE".equals(row.getPublicState()) || "OCCUPIED".equals(row.getPublicState()))
            || "FREE".equals(row.getPublicState()) && row.isWorkspaceDirty()) return "ISOLATED";
        if ("FREE".equals(row.getPublicState())) return "FREE";
        return viewer != null && viewer.equals(row.getOwnerSubject()) ? "MINE" : "OTHER";
    }

    public void bind(ComputeNode node, ComputeRequests.PublicIdentity request) {
        requireEditable(node);
        if (request == null || request.getAccountScope()==null || !request.getAccountScope().matches("[A-Za-z0-9_.:-]{1,128}")
            || request.getInstanceId()==null || !request.getInstanceId().matches("[A-Za-z0-9_-]{1,100}")) throw new ServiceException("请登记明确厂商账户范围和实例ID");
        String provider = node.getProvider();
        if (!("autodl".equals(provider) || "aliyun".equals(provider) || "self".equals(provider))) throw new ServiceException("未支持的公共资源厂商");
        if (!"self".equals(provider) && !Objects.equals(node.getProviderInstanceId(),request.getInstanceId()))
            throw new ServiceException("公共身份必须与已登记厂商实例一致，不自动推断或改写厂商实例");
        String key = ModelArtifactObjectStore.hash(provider + "\n" + request.getAccountScope() + "\n" + request.getInstanceId());
        if (node.getPublicInstanceKey()!=null && !Objects.equals(node.getPublicInstanceKey(),key))
            throw new ServiceException("公共实例身份已绑定，不能修改厂商账号范围或实例ID；请保留已登记身份");
        List<ComputeNode> members = jdbc.query("SELECT * FROM vls_compute_node WHERE public_instance_key=? ORDER BY id FOR UPDATE",
            new BeanPropertyRowMapper<>(ComputeNode.class),key);
        if (members.stream().anyMatch(n -> !Objects.equals(n.getTenantId(),node.getTenantId())))
            throw new ServiceException("公共实例身份已由其他资源管理方登记");
        jdbc.update("INSERT IGNORE INTO vls_compute_public_instance(instance_key,provider,account_scope,provider_instance_id) VALUES(?,?,?,?)",
            key,provider,request.getAccountScope(),request.getInstanceId());
        ComputeNode target = new ComputeNode(); target.setPublicInstanceKey(key);
        Instance row = read(target,true);
        if (!"FREE".equals(row.getPublicState()) || row.isWorkspaceDirty()) throw new ServiceException("整实例已占用或隔离，不能调整GPU成员");
        // Historic task directories have no cleanup proof. Registering identity does not make them clean.
        List<ComputeNode> physical = jdbc.query("SELECT * FROM vls_compute_node WHERE tenant_id=? AND provider=? AND (id=? OR (host=? AND port=?) OR (server_group IS NOT NULL AND server_group<>'' AND server_group=?)) ORDER BY id FOR UPDATE",
            new BeanPropertyRowMapper<>(ComputeNode.class),node.getTenantId(),provider,node.getId(),node.getHost(),node.getPort(),node.getServerGroup());
        int jobs = 0;
        jobs += jdbc.queryForList("SELECT j.id FROM vls_cloud_training_job j JOIN vls_compute_node n ON n.id=j.node_id WHERE n.host=? AND n.port=? FOR UPDATE",String.class,node.getHost(),node.getPort()).size();
        for (ComputeNode member : physical) {
            requireEditable(member);
            if (member.getPublicInstanceKey()!=null && !Objects.equals(member.getPublicInstanceKey(),key))
                throw new ServiceException("服务器成员已登记为另一公共身份，不能通过未绑定别名重绑账号范围或实例ID");
            if (member.getProviderInstanceId()!=null && !Objects.equals(member.getProviderInstanceId(),request.getInstanceId())) throw new ServiceException("服务器成员厂商实例身份不一致，请先核对");
            jobs += jdbc.queryForList("SELECT id FROM vls_cloud_training_job WHERE node_id=? FOR UPDATE",String.class,member.getId()).size();
            jdbc.update("UPDATE vls_compute_node SET public_instance_key=? WHERE tenant_id=? AND id=?",key,node.getTenantId(),member.getId());
        }
        if (jobs > 0) jdbc.update("UPDATE vls_compute_public_instance SET public_state='ISOLATED',workspace_dirty=1 WHERE instance_key=?",key);
    }

    public String claim(ComputeNode node) {
        ComputePlatformAuthorization.Context proof = identity();
        if (!"PLATFORM".equals(node.getResourceScope()) || !node.isEnabled()) throw new ServiceException("公共算力已撤下或停用");
        if (!"FREE".equals(node.getBillingType())) throw new ServiceException("收费租用尚未接通个人余额与资金保护，不能占用收费实例");
        List<ComputeNode> members=jdbc.query("SELECT * FROM vls_compute_node WHERE public_instance_key=? ORDER BY id FOR UPDATE",new BeanPropertyRowMapper<>(ComputeNode.class),node.getPublicInstanceKey());
        if (members.stream().anyMatch(n->!"FREE".equals(n.getBillingType())))
            throw new ServiceException("整实例含收费供给，资金保护未接通，不能通过免费GPU绕过");
        Instance row = read(node,true);
        if (members.stream().anyMatch(n->n.getPowerOperationId()!=null || "CHECKING".equals(n.getProbeState())))
            throw new ServiceException("实例仍有启停或环境检查操作，请完成后再占用");
        if (row == null || "ISOLATED".equals(row.getPublicState()) || row.isWorkspaceDirty() && !"OCCUPIED".equals(row.getPublicState()))
            throw new ServiceException("实例身份或工作空间保障未就绪，资源隔离中");
        String owner = subject(proof);
        if ("OCCUPIED".equals(row.getPublicState())) {
            if (owner.equals(row.getOwnerSubject())) return "已是我的整实例占用，没有重复创建";
            throw new ServiceException("整实例已被其他用户占用，请选择其他资源");
        }
        if (!"FREE".equals(row.getPublicState())) throw new ServiceException("实例占用状态未知，不能租用");
        jdbc.update("UPDATE vls_compute_public_instance SET public_state='OCCUPIED',owner_subject=?,claim_id=?,generation=generation+1 WHERE instance_key=?",
            owner,UUID.randomUUID().toString(),row.getInstanceKey());
        return "已占用整实例；同实例所有GPU仅供当前平台用户使用，未执行开机或扣款";
    }

    Instance requireOwner(ComputeNode node) {
        Instance row = read(node,true);
        String owner = subject(identity());
        if (row == null || !"OCCUPIED".equals(row.getPublicState()) || !owner.equals(row.getOwnerSubject()))
            throw new ServiceException("整实例未由当前平台用户占用或已隔离，请到算力市场确认");
        return row;
    }

    Instance training(ComputeNode node) {
        Instance row = requireOwner(node);
        if (!jdbc.queryForList("SELECT id FROM vls_compute_node WHERE public_instance_key=? AND (power_operation_id IS NOT NULL OR probe_state='CHECKING') ORDER BY id FOR UPDATE",Long.class,node.getPublicInstanceKey()).isEmpty())
            throw new ServiceException("实例仍有启停或环境检查操作，不能提交训练");
        jdbc.update("UPDATE vls_compute_public_instance SET workspace_dirty=1 WHERE instance_key=?",row.getInstanceKey());
        return row;
    }

    public String release(ComputeNode node) {
        Instance row = requireOwner(node);
        if (!jdbc.queryForList("SELECT id FROM vls_compute_node WHERE public_instance_key=? AND (power_operation_id IS NOT NULL OR probe_state='CHECKING') ORDER BY id FOR UPDATE",Long.class,node.getPublicInstanceKey()).isEmpty())
            throw new ServiceException("实例仍有启停或环境检查操作，不能解除占用");
        List<String> active = jdbc.queryForList("SELECT j.id FROM vls_cloud_training_job j JOIN vls_compute_node n ON n.id=j.node_id WHERE (j.public_instance_key=? OR n.public_instance_key=?) AND j.job_state NOT IN ('COMPLETED','FAILED','CANCELLED') FOR UPDATE",String.class,row.getInstanceKey(),row.getInstanceKey());
        if (!active.isEmpty()) throw new ServiceException("实例仍有排队、运行或归档任务，不能解除占用");
        if (row.isWorkspaceDirty()) {
            jdbc.update("UPDATE vls_compute_public_instance SET public_state='ISOLATED' WHERE instance_key=?",row.getInstanceKey());
            return "工作空间已使用但安全清理未验证，实例已隔离；未删除文件，不会分配下一用户";
        }
        jdbc.update("UPDATE vls_compute_public_instance SET public_state='FREE',owner_subject=NULL,claim_id=NULL,generation=generation+1 WHERE instance_key=?",row.getInstanceKey());
        return "未使用工作空间的占用已撤销，未执行删除、停机或扣款";
    }

    void requireEditable(ComputeNode node) {
        Instance row = read(node,true);
        if(node.getPublicInstanceKey()!=null && row==null)throw new ServiceException("公共实例身份记录缺失，不能调整配置或移除");
        if (row != null && (!"FREE".equals(row.getPublicState()) || row.isWorkspaceDirty()))
            throw new ServiceException("公共整实例占用或隔离中，不能改配置、撤下或移除");
    }
    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
