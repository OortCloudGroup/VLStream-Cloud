package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.data.DataManagementService;
import com.ruoyi.vlstream.test.vlstream.data.TrainingDatasetLayout;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import com.ruoyi.vlstream.test.vlstream.service.ModelClassFileService;
import com.ruoyi.vlstream.test.vlstream.service.RemoteModelArtifactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.annotation.PreDestroy;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import static com.ruoyi.vlstream.test.vlstream.compute.ComputeSsh.quote;

/** Node-scoped durable queue. DB advisory locks and remote process locks make restart/retry safe. */
@Service
@Slf4j
@RequiredArgsConstructor
public class CloudTrainingWorker {
    private final JdbcTemplate jdbc;
    private final PlatformTransactionManager transactions;
    private final ComputeNodeService nodes;
    private final ComputeSsh ssh;
    private final CloudTrainingDatasetWriter datasets;
    private final DataManagementService data;
    private final ModelArtifactObjectStore objects;
    private final RemoteModelArtifactService artifacts;
    private final ObjectMapper json;
    private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "autodl-training-worker"); thread.setDaemon(true); return thread;
    });
    @Value("${vlstream.model-storage.temp-dir:${java.io.tmpdir}/vls-model-storage}") private String tempDir;

    @Scheduled(fixedDelay = 5000, initialDelay = 20000)
    public void scan() {
        List<Long> ids;
        try { ids = jdbc.queryForList("SELECT node_id FROM vls_cloud_training_job WHERE job_state NOT IN ('COMPLETED','FAILED','CANCELLED') GROUP BY node_id ORDER BY MIN(update_time) LIMIT 100", Long.class); }
        catch (RuntimeException e) { log.debug("Cloud training migrations not available"); return; }
        for (Long id : ids) {
            if (inFlight.size() >= 2) break;
            if (inFlight.add(id)) executor.submit(() -> { try { tick(id); } finally { inFlight.remove(id); } });
        }
    }
    @PreDestroy public void shutdown() { executor.shutdownNow(); }

    void tick(Long nodeId) {
        String previous = TenantContextHolder.getTenantId();
        String lock = "vls-cloud-node-" + nodeId;
        try (Connection db = Objects.requireNonNull(jdbc.getDataSource()).getConnection()) {
            if (!lock(db, "SELECT GET_LOCK(?,0)", lock)) return;
            try {
                List<CloudTrainingJob> jobs = jdbc.query("SELECT * FROM vls_cloud_training_job WHERE node_id=? AND job_state NOT IN ('COMPLETED','FAILED','CANCELLED') ORDER BY create_time,container_record_id LIMIT 1", new BeanPropertyRowMapper<>(CloudTrainingJob.class), nodeId);
                if (jobs.isEmpty()) return;
                CloudTrainingJob job = jobs.get(0);
                TenantContextHolder.setTenantId(job.getTenantId());
                try { process(job); }
                catch (ServiceException e) { finish(job, "FAILED", limited(e.getMessage()), null); }
                catch (Exception e) {
                    // A broken connection does not prove that a remote process has stopped.
                    jdbc.update("UPDATE vls_cloud_training_job SET message=?,update_time=NOW() WHERE tenant_id=? AND id=?",
                        "连接、数据传输或产物回存暂未完成，将自动重试；请检查实例和对象存储", job.getTenantId(), job.getId());
                    log.warn("Cloud training awaiting retry: job={}, stage={}, error={}", job.getId(), job.getJobState(), e.getClass().getSimpleName(), e);
                }
            } finally { lock(db, "SELECT RELEASE_LOCK(?)", lock); }
        } catch (Exception e) { log.warn("Cloud training queue unavailable: {}", e.getClass().getSimpleName()); }
        finally { if (previous == null) TenantContextHolder.clear(); else TenantContextHolder.setTenantId(previous); }
    }

    private boolean lock(Connection db, String sql, String name) throws java.sql.SQLException {
        try (PreparedStatement statement = db.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet result = statement.executeQuery()) { return result.next() && result.getInt(1) == 1; }
        }
    }

    private void process(CloudTrainingJob job) throws Exception {
        if (job.isCancelRequested() && "QUEUED".equals(job.getJobState())) { finish(job, "CANCELLED", "训练已取消", null); return; }
        ComputeNode node = nodes.get(job.getNodeId());
        if (!node.isEnabled()) return;
        if ("QUEUED".equals(job.getJobState())) phase(job, "PREPARING", "正在上传固定版本的数据集", 0, 0);
        if ("PREPARING".equals(job.getJobState()) && !cancelled(job)) persistDatasetReference(job, datasets.ensureArtifact(job));
        try (ComputeSsh.Connection connection = ssh.open(node)) {
            String configPath = job.getRunDir() + "/job.json";
            String prepared = connection.execute("test -s " + quote(configPath) + " && echo READY || echo MISSING", 20).trim();
            if (!"READY".equals(prepared)) {
                if (cancelled(job)) { finish(job, "CANCELLED", "训练已取消", null); return; }
                if (!"PREPARING".equals(job.getJobState())) throw new ServiceException("实例中的本轮任务文件已缺失，无法恢复运行状态");
                connection.mkdir(job.getRunDir());
                datasets.upload(job, data.readSnapshot(job.getSnapshotJson()), connection);
                Map<String, Object> config = json.readValue(job.getOptionsJson(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() { });
                config.put("annotationType", job.getAnnotationType()); config.put("datasetId", String.valueOf(job.getDatasetId()));
                config.put("workDir", node.getWorkDir()); config.put("gpuIndex", node.getGpuIndex());
                config.put("model", prepareBaseModel(job, connection));
                connection.putText(configPath + ".tmp", json.writeValueAsString(config));
                connection.execute("mv -- " + quote(configPath + ".tmp") + " " + quote(configPath), 20);
            }
            String command = quote(node.getPythonPath()) + " " + quote(job.getRunDir() + "/cloud_training_runner.py");
            JsonNode result = remoteStatus(connection.execute(command + (cancelled(job) ? " cancel " : " status ") + quote(configPath), 45));
            jdbc.update("UPDATE vls_cloud_training_job SET log_tail=? WHERE tenant_id=? AND id=?", result.path("logs").asText(""), job.getTenantId(), job.getId());
            String state = result.path("state").asText();
            if ("SUCCEEDED".equals(state)) {
                phase(job, "ARCHIVING", "训练已完成，正在校验并回存 PT 和类别文件", 99, result.path("epoch").asInt());
                archive(job, connection, result);
                finish(job, "COMPLETED", "PT 和类别文件已校验并回存平台", job.modelKey());
            } else if ("FAILED".equals(state) || "CANCELLED".equals(state)) {
                finish(job, state, limited(result.path("message").asText("训练进程结束")), null);
            } else if ("NOT_STARTED".equals(state) || "WAITING".equals(state)) {
                phase(job, "PREPARING", result.path("message").asText("等待 GPU 资源"), 0, 0);
                if (!cancelled(job)) connection.execute("nohup " + command + " run " + quote(configPath)
                    + " </dev/null >>" + quote(job.getRunDir() + "/launcher.log") + " 2>&1 &", 20);
            } else if ("RUNNING".equals(state)) {
                phase(job, "RUNNING", cancelled(job) ? "等待训练进程确认停止" : "所选 AutoDL 实例训练中", Math.min(98, result.path("progress").asInt()), result.path("epoch").asInt());
            } else throw new IOException("实例返回了未知任务状态");
        }
    }

    private void persistDatasetReference(CloudTrainingJob job, String reference) throws IOException {
        if (!com.ruoyi.vlstream.test.vlstream.data.TrainingDatasetArtifactService.isReference(reference)) throw new IOException("线上数据集未完成 MinIO 归档");
        Map<String, Object> options = json.readValue(job.getOptionsJson(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() { });
        options.put("datasetArtifactRef", reference);
        String serialized = json.writeValueAsString(options);
        new TransactionTemplate(transactions).execute(tx -> {
            String config = jdbc.queryForObject("SELECT config_params FROM vls_algorithm_training WHERE tenant_id=? AND id=? FOR UPDATE", String.class, job.getTenantId(), job.getTrainingId());
            try {
                com.fasterxml.jackson.databind.node.ObjectNode current = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(config);
                if (!job.getId().equals(current.path("cloudJobId").asText())) throw new ServiceException("本轮线上训练配置已被替换，已停止执行");
                current.put("datasetArtifactRef", reference); current.put("runtimeDatasetPath", job.getRunDir() + "/dataset/dataset.yaml");
                jdbc.update("UPDATE vls_cloud_training_job SET options_json=? WHERE tenant_id=? AND id=?", serialized, job.getTenantId(), job.getId());
                jdbc.update("UPDATE vls_algorithm_training SET config_params=? WHERE tenant_id=? AND id=?", current.toString(), job.getTenantId(), job.getTrainingId());
            } catch (IOException e) { throw new ServiceException("线上训练配置无法读取"); }
            return null;
        });
        job.setOptionsJson(serialized);
    }

    private String prepareBaseModel(CloudTrainingJob job, ComputeSsh.Connection connection) throws IOException {
        if (job.getBaseModel().startsWith("@preset/")) return job.getBaseModel();
        Path root = Paths.get(tempDir); Files.createDirectories(root);
        Path local = Files.createTempFile(root, "cloud-base-", ".pt");
        try {
            ModelArtifactObjectStore.StoredArtifact stored = objects.find(job.getBaseModel());
            if (stored == null) throw new ServiceException("指定起始模型尚未在当前租户 MinIO 中就绪，无法准备新算力任务");
            try (OutputStream output = Files.newOutputStream(local)) { objects.stream(stored, output); }
            if (Files.size(local) != stored.getFileSize() || !digest(local).equals(stored.getSha256())) throw new IOException("起始模型下载校验失败");
            try (InputStream input = Files.newInputStream(local)) { connection.put(job.getRunDir() + "/base.pt", input); }
            return job.getRunDir() + "/base.pt";
        } finally { Files.deleteIfExists(local); }
    }

    private void archive(CloudTrainingJob job, ComputeSsh.Connection connection, JsonNode result) throws IOException {
        String sha = result.path("sha256").asText(); long size = result.path("size").asLong();
        if (!sha.matches("[a-f0-9]{64}") || size < 1) throw new IOException("实例产物缺少校验信息");
        String yaml = new TrainingDatasetLayout(job.getAnnotationType(), data.readSnapshot(job.getSnapshotJson()).getLabels()).yaml(job.getRunDir() + "/dataset");
        if (!objects.archive(ModelClassFileService.storagePath(job.modelKey()), "data.yaml", target -> Files.write(target, yaml.getBytes(StandardCharsets.UTF_8)))) throw new IOException("类别归档等待重试");
        if (!objects.archive(job.modelKey(), "best.pt", target -> {
            connection.download(job.getRunDir() + "/output/weights/best.pt", target);
            if (Files.size(target) != size || !digest(target).equals(sha)) throw new IOException("PT 从实例取回后校验不一致");
        })) throw new IOException("PT 归档等待重试");
    }

    private boolean cancelled(CloudTrainingJob job) {
        return jdbc.queryForObject("SELECT cancel_requested FROM vls_cloud_training_job WHERE tenant_id=? AND id=?", Boolean.class, job.getTenantId(), job.getId());
    }
    private void phase(CloudTrainingJob job, String state, String message, int progress, int epoch) {
        new TransactionTemplate(transactions).execute(tx -> {
            jdbc.update("UPDATE vls_cloud_training_job SET job_state=?,message=?,progress=?,epoch_current=?,update_time=NOW() WHERE tenant_id=? AND id=?", state, message, progress, epoch, job.getTenantId(), job.getId());
            jdbc.update("UPDATE vls_container_instance SET instance_status=?,error_message=NULL WHERE tenant_id=? AND id=?", "PREPARING".equals(state) ? "starting" : "running", job.getTenantId(), job.getContainerRecordId());
            jdbc.update("UPDATE vls_algorithm_training SET train_status=?,progress=?,epoch_current=?,start_time=COALESCE(start_time,NOW()),error_message=NULL WHERE tenant_id=? AND id=?", "PREPARING".equals(state) ? "pending" : "training", progress, epoch, job.getTenantId(), job.getTrainingId());
            return null;
        });
        job.setJobState(state);
    }

    private void finish(CloudTrainingJob job, String state, String message, String model) {
        new TransactionTemplate(transactions).execute(tx -> {
            boolean success = "COMPLETED".equals(state);
            String trainingState = success ? "completed" : "CANCELLED".equals(state) ? "stop" : "failed";
            jdbc.update("UPDATE vls_cloud_training_job SET job_state=?,message=?,progress=IF(?='COMPLETED',100,progress),update_time=NOW() WHERE tenant_id=? AND id=?", state, message, state, job.getTenantId(), job.getId());
            jdbc.update("UPDATE vls_container_instance SET instance_status=?,stop_time=NOW(),error_message=? WHERE tenant_id=? AND id=?", success ? "completed" : "CANCELLED".equals(state) ? "cancelled" : "error", success ? null : message, job.getTenantId(), job.getContainerRecordId());
            jdbc.update("UPDATE vls_algorithm_training SET train_status=?,model_output_path=?,progress=IF(?='completed',100,progress),error_message=?,end_time=NOW() WHERE tenant_id=? AND id=?", trainingState, model, trainingState, success ? null : message, job.getTenantId(), job.getTrainingId());
            return null;
        });
    }
    static JsonNode remoteStatus(String text) throws IOException {
        int marker = text.lastIndexOf("VLS_CLOUD=");
        if (marker < 0) throw new IOException("实例未返回任务状态");
        return new ObjectMapper().readTree(text.substring(marker + 10).trim());
    }
    private static String limited(String value) { return value == null ? "训练未完成" : value.substring(0, Math.min(450, value.length())); }
    private static String digest(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) { byte[] buffer = new byte[65536]; int size; while ((size = input.read(buffer)) != -1) digest.update(buffer, 0, size); }
            StringBuilder hex = new StringBuilder(); for (byte value : digest.digest()) hex.append(String.format("%02x", value & 255)); return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
