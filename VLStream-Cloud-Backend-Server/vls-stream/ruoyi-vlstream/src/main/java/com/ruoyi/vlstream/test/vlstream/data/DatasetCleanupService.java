package com.ruoyi.vlstream.test.vlstream.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.oss.core.OssClient;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationImage;
import com.ruoyi.vlstream.test.vlstream.service.ModelClassFileService;
import com.ruoyi.vlstream.test.vlstream.service.ModelClassSnapshotStore;
import com.ruoyi.vlstream.test.vlstream.service.impl.AnnotationImageObjectKey;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;

/** Durable cleanup intent is committed before any irreversible file operation. */
@Service
@RequiredArgsConstructor
public class DatasetCleanupService {
    private final JdbcTemplate jdbc;
    private final PlatformTransactionManager transactions;
    private final DatasetStorageProvider storage;
    private final DatasetRemoteCleanup remote;
    private final ModelClassFileService classes;
    private final ModelClassSnapshotStore snapshots;
    private final ObjectMapper json;

    public boolean delete(Long id) {
        String tenant = TenantContextHolder.getTenantId();
        if (tenant == null || tenant.trim().isEmpty() || id == null || id <= 0) throw new ServiceException("缺少有效项目或租户");
        TransactionTemplate tx = new TransactionTemplate(transactions);
        // Never join an outer transaction: the manifest and class snapshots must survive failure.
        tx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        tx.execute(status -> { prepare(id, tenant); return null; });
        try {
            tx.execute(status -> {
                jdbc.queryForObject("SELECT id FROM vls_algorithm_annotation WHERE tenant_id=? AND id=? FOR UPDATE", Long.class, tenant, id);
                Map<String, Object> row = jdbc.queryForMap("SELECT cleanup_state,manifest_json FROM vls_dataset_cleanup WHERE tenant_id=? AND annotation_id=? FOR UPDATE", tenant, id);
                if ("COMPLETED".equals(row.get("cleanup_state"))) return null;
                Manifest plan = read(String.valueOf(row.get("manifest_json")), Manifest.class);
                if (plan.isRemote() && !Objects.equals(plan.getRemoteIdentity(), remote.identity())) throw new ServiceException("训练服务器配置已变更，已停止清理");
                // A retry rechecks references; shared objects are never removed.
                for (Map.Entry<String, Set<String>> entry : plan.getObjects().entrySet()) {
                    OssClient client = storage.get(entry.getKey());
                    if (!Objects.equals(client.getBucketName(), plan.getBuckets().get(entry.getKey()))
                        || !Objects.equals(client.getStorageIdentity(), plan.getEndpoints().get(entry.getKey()))) throw new ServiceException("存储配置已变更，已停止清理");
                    for (Multipart upload : plan.getMultiparts()) if (entry.getKey().equals(upload.getConfig())) {
                        try { client.abortMultipart(upload.getKey(), upload.getUploadId()); }
                        catch (com.amazonaws.services.s3.model.AmazonS3Exception e) { if (!"NoSuchUpload".equals(e.getErrorCode())) throw e; }
                    }
                    Set<String> shared = otherReferences(id, tenant, client.getBucketName());
                    for (String key : entry.getValue()) if (!shared.contains(key)) {
                        client.delete(key);
                        jdbc.update("DELETE FROM sys_oss WHERE service=? AND file_name=?", entry.getKey(), key);
                    }
                }
                if (plan.isRemote()) {
                    try { remote.remove(id); }
                    catch (Exception e) { throw new ServiceException("远端训练数据清理失败；模型未删除，请检查连接或目录后重试"); }
                }
                finish(id, tenant);
                return null;
            });
        } catch (RuntimeException failure) {
            String reason = failure instanceof ServiceException ? failure.getMessage() : "对象存储操作失败，请检查服务连接";
            jdbc.update("UPDATE vls_dataset_cleanup SET cleanup_state='FAILED',error_message=?,update_time=NOW() WHERE tenant_id=? AND annotation_id=? AND cleanup_state<>'COMPLETED'",
                reason, tenant, id);
            throw new ServiceException("删除未完成：" + reason + "。部分文件可能已清理，项目已禁止编辑和训练，请重试删除；模型和训练历史保留。");
        }
        return true;
    }

    private void prepare(Long id, String tenant) {
        List<Map<String, Object>> projects = jdbc.queryForList("SELECT dataset_path,is_deleted FROM vls_algorithm_annotation WHERE id=? AND tenant_id=? FOR UPDATE", id, tenant);
        if (projects.isEmpty()) throw new ServiceException("数据集不存在或无权访问");
        if (count("SELECT COUNT(*) FROM vls_dataset_cleanup WHERE tenant_id=? AND annotation_id=?", tenant, id) > 0) return;
        if (((Number) projects.get(0).get("is_deleted")).intValue() != 0) throw new ServiceException("数据集已删除");
        if (count("SELECT COUNT(*) FROM vls_dataset_conversion_guard WHERE tenant_id=? AND dataset_id=?", tenant, id) > 0) throw new ServiceException("模型转换仍在使用数据集，请等待转换结束后再删除");
        if (count("SELECT COUNT(*) FROM vls_algorithm_training WHERE tenant_id=? AND dataset_id=? AND (train_status IS NULL OR train_status NOT IN ('pending','completed','failed','cancelled','canceled','stopped') OR onnx_conversion_status='converting' OR om_conversion_status='converting')", tenant, id) > 0
            || count("SELECT COUNT(*) FROM vls_container_instance c JOIN vls_algorithm_training t ON c.training_task_id=t.id AND c.tenant_id=t.tenant_id WHERE t.tenant_id=? AND t.dataset_id=? AND c.instance_status IN ('queued','pending','starting','running','creating','stopping')", tenant, id) > 0) {
            throw new ServiceException("存在排队、运行或转换中的训练任务，请等待任务结束后再删除数据集");
        }
        if (count("SELECT COUNT(*) FROM vls_dataset_import_job WHERE tenant_id=? AND dataset_id=? AND job_state IN ('UPLOADING','COMPLETING','QUEUED','PROCESSING')", tenant, id) > 0) throw new ServiceException("存在上传或导入中的任务，请先结束任务再删除数据集");
        String root = DatasetRemoteCleanup.root(id);
        if (count("SELECT COUNT(*) FROM vls_container_instance WHERE env_config LIKE ? AND instance_status IN ('queued','pending','starting','running','creating','stopping')", "%" + root + "/%") > 0) throw new ServiceException("有活动容器引用此训练目录，不能删除");
        if (count("SELECT COUNT(*) FROM vls_algorithm_annotation WHERE NOT (tenant_id=? AND id=?) AND (dataset_path=? OR dataset_path LIKE ?)", tenant, id, root, root + "/%") > 0) throw new ServiceException("训练目录被其他项目引用，不能删除");
        Object path = projects.get(0).get("dataset_path");
        if (path != null && !path.toString().trim().isEmpty() && !path.toString().startsWith(root + "/")) throw new ServiceException("数据集路径不属于当前项目的生成目录，需先核对归属");

        List<AlgorithmTraining> models = jdbc.query("SELECT id,tenant_id,model_output_path,onnx_model_output_path,om_model_output_path,rknn_model_output_path,int8_rknn_model_output_path FROM vls_algorithm_training WHERE tenant_id=? AND dataset_id=?",
            (rs, n) -> { AlgorithmTraining t = new AlgorithmTraining(); t.setId(rs.getLong(1)); t.setTenantId(rs.getString(2)); t.setModelOutputPath(rs.getString(3)); t.setOnnxModelOutputPath(rs.getString(4)); t.setOmModelOutputPath(rs.getString(5)); t.setRknnModelOutputPath(rs.getString(6)); t.setInt8RknnModelOutputPath(rs.getString(7)); return t; }, tenant, id);
        for (AlgorithmTraining training : models) {
            if (text(training.getModelOutputPath()) || text(training.getOnnxModelOutputPath()) || text(training.getOmModelOutputPath()) || text(training.getRknnModelOutputPath()) || text(training.getInt8RknnModelOutputPath())) {
                try { snapshots.save(training, classes.prepare(training)); }
                catch (Exception e) { throw new ServiceException("无法保存训练任务 " + training.getId() + " 的原始模型类别信息，已停止删除"); }
            }
        }
        Manifest plan = new Manifest();
        List<String> paths = jdbc.queryForList("SELECT local_path FROM vls_annotation_image WHERE tenant_id=? AND annotation_id=?", String.class, tenant, id);
        List<String> versions = jdbc.queryForList("SELECT snapshot_json FROM vls_dataset_version WHERE tenant_id=? AND annotation_id=?", String.class, tenant, id);
        for (String version : versions) paths.addAll(snapshotPaths(version));
        if (paths.stream().anyMatch(DatasetCleanupService::text)) {
            OssClient client = storage.current();
            for (String stored : paths) if (text(stored)) {
                String normalized = key(stored, client.getBucketName());
                List<String> providers = jdbc.queryForList("SELECT DISTINCT service FROM sys_oss WHERE file_name=?", String.class, normalized);
                if (providers.size() > 1) throw new ServiceException("图片对应多个存储配置，需先核对归属");
                add(plan, providers.isEmpty() ? client : storage.get(providers.get(0)), stored);
            }
        }
        for (Map<String, Object> job : jdbc.queryForList("SELECT storage_config,storage_bucket,object_key,multipart_id,job_state FROM vls_dataset_import_job WHERE tenant_id=? AND dataset_id=? AND object_key IS NOT NULL", tenant, id)) {
            if (text((String) job.get("multipart_id")) && !Arrays.asList("COMPLETED", "PARTIAL", "FAILED", "CANCELLED", "EXPIRED").contains(job.get("job_state"))) throw new ServiceException("上传分片尚未结束，不能删除");
            OssClient client = storage.get((String) job.get("storage_config"));
            if (!Objects.equals(client.getBucketName(), job.get("storage_bucket"))) throw new ServiceException("导入任务的存储桶已变更，已停止删除");
            add(plan, client, (String) job.get("object_key"));
            if (text((String) job.get("multipart_id"))) {
                Multipart upload = new Multipart(); upload.setConfig(client.getConfigKey());
                upload.setKey(key((String) job.get("object_key"), client.getBucketName()));
                upload.setUploadId((String) job.get("multipart_id")); plan.getMultiparts().add(upload);
            }
        }
        // Also inspect the canonical root when dataset_path was invalidated or a publication failed.
        plan.setRemote(true);
        plan.setRemoteIdentity(remote.identity());
        try { jdbc.update("INSERT INTO vls_dataset_cleanup(tenant_id,annotation_id,cleanup_state,manifest_json) VALUES(?,?,'PENDING',?)", tenant, id, json.writeValueAsString(plan)); }
        catch (java.io.IOException e) { throw new ServiceException("无法保存清理清单"); }
    }

    private void add(Manifest plan, OssClient client, String stored) {
        String key = key(stored, client.getBucketName());
        plan.getBuckets().put(client.getConfigKey(), client.getBucketName());
        plan.getEndpoints().put(client.getConfigKey(), client.getStorageIdentity());
        plan.getObjects().computeIfAbsent(client.getConfigKey(), k -> new LinkedHashSet<>()).add(key);
    }

    static String key(String stored, String bucket) {
        if (!text(stored)) throw new ServiceException("图片存储位置为空");
        if (stored.startsWith("http") && !java.net.URI.create(stored).getPath().startsWith("/" + bucket + "/")) throw new ServiceException("历史图片存储桶无法确认，已停止删除");
        String key = AnnotationImageObjectKey.normalize(stored, bucket);
        if (key != null && key.matches("(?i).*\\.(pt|pth|onnx|om|rknn|bin|engine|tflite)")) throw new ServiceException("样本引用了模型文件，已停止删除");
        if (!text(key) || key.contains("\\") || Arrays.asList(key.split("/")).contains("..") || key.endsWith("/")) throw new ServiceException("无效的对象存储路径");
        return key;
    }

    private Set<String> otherReferences(Long id, String tenant, String bucket) {
        Set<String> references = new HashSet<>();
        List<String> paths = jdbc.queryForList("SELECT local_path FROM vls_annotation_image WHERE tenant_id IS NULL OR annotation_id IS NULL OR NOT (tenant_id=? AND annotation_id=?)", String.class, tenant, id);
        for (String version : jdbc.queryForList("SELECT snapshot_json FROM vls_dataset_version WHERE tenant_id IS NULL OR annotation_id IS NULL OR NOT (tenant_id=? AND annotation_id=?)", String.class, tenant, id)) paths.addAll(snapshotPaths(version));
        paths.addAll(jdbc.queryForList("SELECT object_key FROM vls_dataset_import_job WHERE NOT (tenant_id=? AND dataset_id=?)", String.class, tenant, id));
        for (String path : paths) if (text(path)) references.add(AnnotationImageObjectKey.normalize(path, bucket));
        return references;
    }

    private List<String> snapshotPaths(String value) {
        DatasetSnapshot snapshot = read(value, DatasetSnapshot.class);
        List<String> paths = new ArrayList<>();
        if (snapshot.getSamples() != null) for (AnnotationImage sample : snapshot.getSamples()) paths.add(sample.getLocalPath());
        return paths;
    }

    private void finish(Long id, String tenant) {
        jdbc.update("DELETE p FROM vls_dataset_upload_part p JOIN vls_dataset_import_job j ON p.job_id=j.id WHERE j.tenant_id=? AND j.dataset_id=?", tenant, id);
        for (String table : Arrays.asList("vls_annotation_instance", "vls_annotation_label", "vls_annotation_image", "vls_dataset_version")) jdbc.update("DELETE FROM " + table + " WHERE tenant_id=? AND annotation_id=?", tenant, id);
        for (String table : Arrays.asList("vls_dataset_frame_origin", "vls_dataset_import_job")) jdbc.update("DELETE FROM " + table + " WHERE tenant_id=? AND dataset_id=?", tenant, id);
        jdbc.update("UPDATE vls_algorithm_annotation SET is_deleted=1,dataset_path=NULL,total_count=0,annotated_count=0,progress=0 WHERE tenant_id=? AND id=?", tenant, id);
        jdbc.update("UPDATE vls_dataset_cleanup SET cleanup_state='COMPLETED',error_message=NULL,update_time=NOW() WHERE tenant_id=? AND annotation_id=?", tenant, id);
    }

    private int count(String sql, Object... args) { return jdbc.queryForObject(sql, Integer.class, args); }
    private <T> T read(String value, Class<T> type) {
        try { return json.readValue(value, type); }
        catch (Exception e) { throw new ServiceException("数据集历史清单无法解析，已停止删除"); }
    }
    private static boolean text(String s) { return s != null && !s.trim().isEmpty(); }

    @Data
    public static class Manifest {
        private Map<String, Set<String>> objects = new LinkedHashMap<>();
        private Map<String, String> buckets = new LinkedHashMap<>();
        private Map<String, String> endpoints = new LinkedHashMap<>();
        private List<Multipart> multiparts = new ArrayList<>();
        private boolean remote;
        private String remoteIdentity;
    }

    @Data
    public static class Multipart {
        private String config;
        private String key;
        private String uploadId;
    }
}
