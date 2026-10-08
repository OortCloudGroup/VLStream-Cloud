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

    public List<ComputeNode> list() {
        return jdbc.query("SELECT * FROM vls_compute_node WHERE tenant_id=? ORDER BY create_time DESC", mapper(), tenant());
    }
    public ComputeNode get(Long id) { return get(id, false); }
    private ComputeNode get(Long id, boolean lock) {
        List<ComputeNode> rows = jdbc.query("SELECT * FROM vls_compute_node WHERE tenant_id=? AND id=?" + (lock ? " FOR UPDATE" : ""), mapper(), tenant(), id);
        if (rows.isEmpty()) throw new ServiceException("算力实例不存在或无权访问");
        return rows.get(0);
    }
    public ComputeNode requireReady(Long id) {
        ComputeNode node = get(id);
        if (!node.isEnabled() || !"READY".equals(node.getProbeState())) throw new ServiceException("请先启用实例并通过连接与训练环境检查");
        try {
            JsonNode check = json.readTree(node.getProbeJson() == null ? "{}" : node.getProbeJson());
            if (check.path("contractVersion").asInt() != 1 || !check.path("ready").asBoolean())
                throw new ServiceException("请重新检查实例环境，确认标准依赖、GPU 运算和基础模型缓存均已就绪");
        } catch (IOException e) { throw new ServiceException("环境检查记录无效，请重新检查"); }
        return node;
    }

    @Transactional(rollbackFor = Exception.class)
    public ComputeNode save(Long id, ComputeRequests.Node request) {
        String host = request.getHost().trim().toLowerCase(Locale.ROOT);
        if (!host.matches("[a-z0-9][a-z0-9.-]{0,252}") || host.contains("..")) throw new ServiceException("SSH 地址仅填写主机名或 IPv4 地址");
        validatePath(request.getPythonPath()); validatePath(request.getWorkDir());
        if ("/".equals(request.getWorkDir())) throw new ServiceException("请使用专用工作目录");
        ComputeNode before = id == null ? null : get(id, true);
        if (before != null) assertIdle(id);
        String secret = before == null ? null : before.getPasswordCipher();
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            try { secret = cipher.encrypt(request.getPassword()); }
            catch (RuntimeException e) { throw new ServiceException("算力凭据保存失败，请联系管理员检查后端持久化目录、原密钥及数据库升级状态"); }
        }
        if (secret == null) throw new ServiceException("请输入 SSH 密码");
        Long key = id == null ? IdWorker.getId() : id;
        boolean sameEndpoint = before != null && host.equals(before.getHost()) && request.getPort() == before.getPort() && request.getUsername().equals(before.getUsername());
        try {
            if (before == null) jdbc.update("INSERT INTO vls_compute_node(id,tenant_id,name,host,port,username,password_cipher,python_path,work_dir,gpu_index,enabled) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                key, tenant(), request.getName().trim(), host, request.getPort(), request.getUsername(), secret, request.getPythonPath(), request.getWorkDir(), request.getGpuIndex(), request.isEnabled());
            else jdbc.update("UPDATE vls_compute_node SET name=?,host=?,port=?,username=?,password_cipher=?,host_key=?,python_path=?,work_dir=?,gpu_index=?,enabled=?,probe_state='UNTESTED',probe_json=NULL,probe_time=NULL,update_time=NOW() WHERE tenant_id=? AND id=?",
                request.getName().trim(), host, request.getPort(), request.getUsername(), secret, sameEndpoint ? before.getHostKey() : null,
                request.getPythonPath(), request.getWorkDir(), request.getGpuIndex(), request.isEnabled(), tenant(), key);
        } catch (org.springframework.dao.DuplicateKeyException e) { throw new ServiceException("此 SSH 实例已接入，请使用现有记录"); }
        return get(key);
    }

    public ComputeNode probe(Long id) {
        ComputeNode node = get(id);
        String details; String state = "ERROR"; String key = node.getHostKey();
        try (ComputeSsh.Connection connection = ssh.open(node)) {
            key = connection.hostKey();
            String encoded;
            try (java.io.InputStream input = new org.springframework.core.io.ClassPathResource("training/check_cloud_environment.py").getInputStream()) {
                encoded = java.util.Base64.getEncoder().encodeToString(org.springframework.util.StreamUtils.copyToByteArray(input));
            }
            String script = "import base64;exec(compile(base64.b64decode('" + encoded + "'),'vls-environment-check','exec'))";
            String output = connection.execute(quote(node.getPythonPath()) + " -c " + quote(script)
                + " --work-dir " + quote(node.getWorkDir()) + " --gpu " + node.getGpuIndex(), 180);
            int marker = output.lastIndexOf("VLS_PROBE=");
            if (marker < 0) throw new IOException("环境检查无有效结果");
            JsonNode result = json.readTree(output.substring(marker + "VLS_PROBE=".length()).trim());
            details = json.writeValueAsString(result);
            if (result.path("contractVersion").asInt() == 1 && result.path("ready").asBoolean()) state = "READY";
        } catch (Exception e) { details = "{\"message\":\"无法完成环境检查，请先按接入指南初始化实例，并核对 SSH 和初始化脚本输出的 Python 路径\"}"; }
        // A probe from an older edit must never mark a different connection ready.
        jdbc.update("UPDATE vls_compute_node SET probe_state=?,probe_json=?,host_key=?,probe_time=NOW() WHERE tenant_id=? AND id=? AND host=? AND port=? AND username=? AND password_cipher=? AND python_path=? AND work_dir=? AND gpu_index=?",
            state, details, key, tenant(), id, node.getHost(), node.getPort(), node.getUsername(), node.getPasswordCipher(), node.getPythonPath(), node.getWorkDir(), node.getGpuIndex());
        return get(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        get(id, true); assertIdle(id);
        jdbc.update("DELETE FROM vls_compute_node WHERE tenant_id=? AND id=?", tenant(), id);
    }
    private void assertIdle(Long id) {
        if (jdbc.queryForObject("SELECT COUNT(*) FROM vls_cloud_training_job WHERE tenant_id=? AND node_id=? AND job_state NOT IN ('COMPLETED','FAILED','CANCELLED')", Integer.class, tenant(), id) > 0)
            throw new ServiceException("实例仍有排队、运行或回存任务，请结束后再修改或移除");
    }
    public static void validatePath(String path) {
        if (path == null || !path.matches("/[A-Za-z0-9_./-]+") || path.contains("..") || path.contains("//")) throw new ServiceException("请填写不含空格和特殊字符的 Linux 绝对路径");
    }
    static String tenant() { return ModelArtifactObjectStore.tenant(); }
    private static BeanPropertyRowMapper<ComputeNode> mapper() { return new BeanPropertyRowMapper<>(ComputeNode.class); }
}
