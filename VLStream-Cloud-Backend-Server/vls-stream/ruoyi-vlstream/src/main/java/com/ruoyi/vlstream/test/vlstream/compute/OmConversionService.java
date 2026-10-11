package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.data.*;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import com.ruoyi.vlstream.test.vlstream.service.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.annotation.PreDestroy;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.*;

/** Durable optional CPU conversion, using immutable MinIO inputs and a private worker volume. */
@Service
@Slf4j
@RequiredArgsConstructor
public class OmConversionService {
    private final OmConversionProperties properties;
    private final JdbcTemplate jdbc;
    private final PlatformTransactionManager transactions;
    private final IVlsAlgorithmTrainingService trainings;
    private final DataManagementService data;
    private final DatasetConversionGuard guard;
    private final TrainingDatasetArtifactService datasets;
    private final ModelArtifactObjectStore objects;
    private final ObjectMapper json;
    private final AtomicBoolean busy = new AtomicBoolean();
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "om-conversion-dispatch"); t.setDaemon(true); return t; });

    public String enqueue(Long trainingId, boolean retry) {
        if (!properties.isEnabled()) throw new ServiceException("未启用 Hi3519DV500 OM 转换服务，请部署可选转换容器");
        String tenant = ComputeNodeService.tenant();
        return new TransactionTemplate(transactions).execute(tx -> {
            AlgorithmTraining initial = trainings.selectAlgorithmTrainingById(trainingId);
            // The legacy detail mapper omits tenant_id. Check authoritative ownership instead of trusting its DTO.
            if (initial == null || jdbc.queryForObject("SELECT COUNT(*) FROM vls_algorithm_training WHERE tenant_id=? AND id=? AND is_deleted=0", Integer.class, tenant, trainingId) != 1)
                throw new ServiceException("训练任务不存在或无权访问");
            data.lockDatasetForTask(initial.getDatasetId());
            jdbc.queryForList("SELECT id FROM vls_algorithm_training WHERE tenant_id=? AND id=? FOR UPDATE", tenant, trainingId);
            AlgorithmTraining training = trainings.selectAlgorithmTrainingById(trainingId);
            try {
                JsonNode config = json.readTree(training.getConfigParams());
                if (!CloudTrainingService.isCloud(training) || training.getTrainStatus() == null || !"completed".equals(training.getTrainStatus().getCode())
                    || !"detect".equals(config.path("trainType").asText())) throw new ServiceException("自动 OM 转换仅支持已完成的 AutoDL 目标检测训练");
                String sourceJob = config.path("cloudJobId").asText();
                List<String> previous = jdbc.queryForList("SELECT id FROM vls_om_conversion_job WHERE tenant_id=? AND training_id=? AND source_job_id=? AND job_state IN ('QUEUED','CONVERTING','ARCHIVING','COMPLETED') ORDER BY create_time DESC LIMIT 1", String.class, tenant, trainingId, sourceJob);
                if (!previous.isEmpty()) return previous.get(0);
                if (!retry && jdbc.queryForObject("SELECT COUNT(*) FROM vls_om_conversion_job WHERE tenant_id=? AND training_id=? AND source_job_id=?", Integer.class, tenant, trainingId, sourceJob) > 0) return null;
                String pt = training.getModelOutputPath();
                if (objects.find(pt) == null) throw new ServiceException("PT 尚未完成 MinIO 归档");
                String ref = config.path("datasetArtifactRef").asText();
                datasets.require(training.getDatasetId(), ref);
                guard.start(training);
                String id = UUID.randomUUID().toString();
                jdbc.update("INSERT INTO vls_om_conversion_job(id,tenant_id,training_id,source_job_id,source_pt_path,dataset_id,dataset_ref,message) VALUES(?,?,?,?,?,?,?,?)",
                    id, tenant, trainingId, sourceJob, pt, training.getDatasetId(), ref, "等待 Hi3519DV500 转换服务");
                jdbc.update("UPDATE vls_algorithm_training SET om_conversion_status='converting',om_conversion_error=NULL WHERE tenant_id=? AND id=?", tenant, trainingId);
                return id;
            } catch (IOException ex) { throw new ServiceException("训练配置缺少可恢复的模型与数据集引用"); }
        });
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 30000)
    public void scan() {
        if (!properties.isEnabled() || !busy.compareAndSet(false, true)) return;
        executor.submit(() -> { try { tick(); } catch (Exception ex) { log.warn("OM dispatcher: {}", ex.getMessage()); } finally { busy.set(false); } });
    }
    @PreDestroy public void shutdown() { executor.shutdownNow(); }

    void tick() throws Exception {
        String previous = TenantContextHolder.getTenantId();
        try (Connection db = Objects.requireNonNull(jdbc.getDataSource()).getConnection()) {
            try (PreparedStatement lock = db.prepareStatement("SELECT GET_LOCK('vls-om-dispatch',0)")) {
                try (ResultSet result = lock.executeQuery()) { if (!result.next() || result.getInt(1) != 1) return; }
            }
            try {
                List<Map<String,Object>> eligible = jdbc.queryForList("SELECT j.training_id,j.tenant_id FROM vls_cloud_training_job j JOIN vls_algorithm_training t ON t.id=j.training_id WHERE j.job_state='COMPLETED' AND j.annotation_type='object_detection' AND t.is_deleted=0 AND BINARY JSON_UNQUOTE(JSON_EXTRACT(t.config_params,'$.cloudJobId'))=BINARY j.id AND COALESCE(t.om_model_output_path,'')='' AND NOT EXISTS(SELECT 1 FROM vls_om_conversion_job o WHERE o.source_job_id=j.id AND o.training_id=j.training_id) LIMIT 10");
                for (Map<String,Object> row : eligible) {
                    TenantContextHolder.setTenantId((String) row.get("tenant_id"));
                    try { enqueue(((Number) row.get("training_id")).longValue(), false); }
                    catch (ServiceException ex) { recordRejected((String) row.get("tenant_id"), ((Number) row.get("training_id")).longValue(), limit(ex.getMessage())); }
                }
                for (Job job : jdbc.query("SELECT * FROM vls_om_conversion_job WHERE job_state IN ('QUEUED','CONVERTING','ARCHIVING') ORDER BY create_time LIMIT 20", new BeanPropertyRowMapper<>(Job.class))) {
                    TenantContextHolder.setTenantId(job.tenantId);
                    try { process(job); }
                    catch (ServiceException ex) { finish(job, null, limit(ex.getMessage())); }
                    catch (Exception ex) {
                        jdbc.update("UPDATE vls_om_conversion_job SET message=? WHERE id=? AND tenant_id=?", "转换文件传输或归档暂未完成，将重试", job.id, job.tenantId);
                        log.warn("OM conversion retry job={}", job.id, ex);
                    }
                }
            } finally { try (Statement release = db.createStatement()) { release.execute("SELECT RELEASE_LOCK('vls-om-dispatch')"); } }
        } finally { if (previous == null) TenantContextHolder.clear(); else TenantContextHolder.setTenantId(previous); }
    }

    private void process(Job job) throws Exception {
        Path root = Paths.get(properties.getWorkDir()).toAbsolutePath().normalize();
        Files.createDirectories(root.resolve("jobs"));
        Path directory = root.resolve("jobs").resolve(UUID.fromString(job.id).toString());
        Files.createDirectories(directory);
        if (Files.isSymbolicLink(directory)) throw new ServiceException("转换工作目录不合法");
        Path resultFile = directory.resolve("result.json");
        if (Files.isRegularFile(resultFile, LinkOption.NOFOLLOW_LINKS)) {
            JsonNode result = json.readTree(readSmall(resultFile));
            if (!job.id.equals(result.path("id").asText())) throw new ServiceException("转换结果任务编号不匹配");
            if ("FAILED".equals(result.path("state").asText())) { finish(job, null, limit("OM转换失败：" + result.path("message").asText() + " " + result.path("detail").asText())); return; }
            Path output = directory.resolve("model_original.om");
            if (!"COMPLETED".equals(result.path("state").asText()) || !"Hi3519DV500".equals(result.path("soc").asText())
                || !Files.isRegularFile(output, LinkOption.NOFOLLOW_LINKS) || Files.size(output) != result.path("size").asLong()
                || !sha(output).equals(result.path("sha256").asText())) throw new ServiceException("OM产物校验失败");
            phase(job, "ARCHIVING", "正在校验并回存 OM");
            String model = job.sourcePtPath + "." + job.id + ".om";
            if (!objects.archive(model, "model.om", target -> Files.copy(output, target, StandardCopyOption.REPLACE_EXISTING))) return;
            finish(job, model, null);
            return;
        }
        if (Files.exists(directory.resolve("request.json"))) {
            phase(job, "CONVERTING", Files.exists(directory.resolve("state.json")) ? "Hi3519DV500 OM 转换中" : "等待转换容器执行；请确认可选服务已启动");
            return;
        }
        phase(job, "QUEUED", "正在从 MinIO 准备 PT、类别及校准图片");
        Map<String,String> hashes = new LinkedHashMap<>();
        copyObject(job.sourcePtPath, directory.resolve("model.pt"), hashes, "model.pt");
        copyObject(ModelClassFileService.storagePath(job.sourcePtPath), directory.resolve("classes.yaml"), hashes, "classes.yaml");
        Path archive = datasets.download(datasets.require(job.datasetId, job.datasetRef));
        Map<String,String> calibration;
        try { calibration = calibration(archive, directory.resolve("calibration")); }
        finally { Files.deleteIfExists(archive); }
        Map<String,Object> request = new LinkedHashMap<>();
        request.put("protocol", 1); request.put("id", job.id); request.put("soc", "Hi3519DV500"); request.put("sha256", hashes); request.put("calibration", calibration);
        Path staged = directory.resolve("request.tmp");
        Files.write(staged, json.writeValueAsBytes(request));
        Files.move(staged, directory.resolve("request.json"), StandardCopyOption.ATOMIC_MOVE);
        phase(job, "CONVERTING", "等待转换容器执行");
    }

    /** Missing/deleted source data is a terminal preparation error, not an endless discovery loop. */
    void recordRejected(String tenant, Long trainingId, String error) {
        new TransactionTemplate(transactions).execute(tx -> {
            jdbc.queryForList("SELECT id FROM vls_algorithm_training WHERE tenant_id=? AND id=? FOR UPDATE", tenant, trainingId);
            int inserted = jdbc.update("INSERT INTO vls_om_conversion_job(id,tenant_id,training_id,source_job_id,source_pt_path,dataset_id,dataset_ref,job_state,message) "
                + "SELECT ?,j.tenant_id,t.id,j.id,COALESCE(t.model_output_path,''),j.dataset_id,COALESCE(JSON_UNQUOTE(JSON_EXTRACT(t.config_params,'$.datasetArtifactRef')),''),'FAILED',? "
                + "FROM vls_algorithm_training t JOIN vls_cloud_training_job j ON j.training_id=t.id AND BINARY j.tenant_id=BINARY t.tenant_id "
                + "WHERE t.tenant_id=? AND t.id=? AND t.is_deleted=0 AND BINARY JSON_UNQUOTE(JSON_EXTRACT(t.config_params,'$.cloudJobId'))=BINARY j.id "
                + "AND NOT EXISTS(SELECT 1 FROM vls_om_conversion_job o WHERE o.training_id=t.id AND o.source_job_id=j.id)", UUID.randomUUID().toString(), error, tenant, trainingId);
            if (inserted == 1) jdbc.update("UPDATE vls_algorithm_training SET om_conversion_status='failed',om_conversion_error=? WHERE tenant_id=? AND id=?", error, tenant, trainingId);
            return null;
        });
    }

    private void copyObject(String key, Path target, Map<String,String> hashes, String name) throws IOException {
        ModelArtifactObjectStore.StoredArtifact stored = objects.find(key);
        if (stored == null) throw new ServiceException("模型或类别文件尚未归档");
        if (Files.isSymbolicLink(target)) throw new ServiceException("转换输入路径不合法");
        try (OutputStream out = Files.newOutputStream(target)) { objects.stream(stored, out); }
        String actual = sha(target);
        if (Files.size(target) != stored.getFileSize() || !actual.equals(stored.getSha256())) throw new IOException("转换输入文件校验失败");
        hashes.put(name, actual);
    }

    static Map<String,String> calibration(Path archive, Path destination) throws IOException {
        Files.createDirectories(destination);
        if (Files.isSymbolicLink(destination)) throw new ServiceException("校准目录不合法");
        Map<String,String> result = new LinkedHashMap<>();
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements() && result.size() < 20) {
                ZipEntry entry = entries.nextElement(); String name = entry.getName();
                if (entry.isDirectory() || !name.startsWith("images/train/") || name.contains("..") || !name.matches(".*\\.(jpg|jpeg|png)")) continue;
                String local = String.format(Locale.ROOT, "%04d", result.size()) + name.substring(name.lastIndexOf('.'));
                Path target = destination.resolve(local);
                if (Files.isSymbolicLink(target)) throw new ServiceException("校准图片路径不合法");
                try (InputStream input = zip.getInputStream(entry); OutputStream output = Files.newOutputStream(target)) {
                    byte[] buffer = new byte[32768]; long size = 0; int n;
                    while ((n = input.read(buffer)) != -1) { size += n; if (size > 25L * 1024 * 1024) throw new ServiceException("校准图片超过25MiB"); output.write(buffer, 0, n); }
                }
                result.put(local, sha(target));
            }
        }
        if (result.isEmpty()) throw new ServiceException("冻结数据包缺少训练集校准图片");
        return result;
    }

    private void phase(Job job, String state, String message) {
        jdbc.update("UPDATE vls_om_conversion_job SET job_state=?,message=? WHERE tenant_id=? AND id=?", state, message, job.tenantId, job.id);
    }

    private void finish(Job job, String model, String error) {
        new TransactionTemplate(transactions).execute(tx -> {
            jdbc.queryForList("SELECT id FROM vls_algorithm_training WHERE tenant_id=? AND id=? FOR UPDATE", job.tenantId, job.trainingId);
            int updated = jdbc.update("UPDATE vls_algorithm_training SET om_conversion_status=?,om_conversion_error=?,om_model_output_path=? WHERE tenant_id=? AND id=? AND model_output_path=? AND BINARY JSON_UNQUOTE(JSON_EXTRACT(config_params,'$.cloudJobId'))=BINARY ?",
                model == null ? "failed" : "completed", error, model, job.tenantId, job.trainingId, job.sourcePtPath, job.sourceJobId);
            if (updated != 1) throw new ServiceException("训练版本已变化，不能覆盖 OM 结果");
            if (model != null) jdbc.update("UPDATE vls_algorithm_model SET om_model_output_path=? WHERE tenant_id=? AND training_id=? AND model_path=? AND is_deleted=0", model, job.tenantId, job.trainingId, job.sourcePtPath);
            jdbc.update("UPDATE vls_om_conversion_job SET job_state=?,message=? WHERE tenant_id=? AND id=?", model == null ? "FAILED" : "COMPLETED", model == null ? error : "OM已校验并回存平台", job.tenantId, job.id);
            jdbc.update("DELETE FROM vls_dataset_conversion_guard WHERE tenant_id=? AND training_id=?", job.tenantId, job.trainingId);
            return null;
        });
    }
    private static byte[] readSmall(Path path) throws IOException { if (Files.size(path) > 65536) throw new ServiceException("转换结果元数据过大"); return Files.readAllBytes(path); }
    static String sha(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256"); byte[] bytes = new byte[65536]; int n;
            while ((n = in.read(bytes)) != -1) digest.update(bytes, 0, n);
            StringBuilder result = new StringBuilder(); for (byte b : digest.digest()) result.append(String.format("%02x", b & 255)); return result.toString();
        } catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    private static String limit(String value) { return value == null ? "OM转换失败" : value.substring(0, Math.min(450, value.length())); }
    @Data public static class Job {
        private String id, tenantId, sourceJobId, sourcePtPath, datasetRef, jobState, message;
        private Long trainingId, datasetId;
    }
}
