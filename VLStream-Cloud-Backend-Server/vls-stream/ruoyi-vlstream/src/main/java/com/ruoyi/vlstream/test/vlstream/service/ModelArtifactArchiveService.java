package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PreDestroy;

/** Resumable archival of new training outputs and historical published model versions. */
@Service
@Slf4j
@RequiredArgsConstructor
public class ModelArtifactArchiveService {
    private final JdbcTemplate jdbc;
    private final ModelArtifactObjectStore store;
    private final RemoteModelArtifactService remote;
    private final ModelClassFileService classes;
    private final AtomicBoolean scanning = new AtomicBoolean();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "model-artifact-archive");
        thread.setDaemon(true);
        return thread;
    });

    @Scheduled(fixedDelayString = "${vlstream.model-storage.scan-delay-ms:60000}", initialDelay = 30000)
    public void schedule() {
        if (!scanning.compareAndSet(false, true)) return;
        worker.submit(() -> {
            try { scan(); }
            catch (RuntimeException ex) { log.error("Model archival scan failed", ex); }
            finally { scanning.set(false); }
        });
    }

    @PreDestroy
    public void close() { worker.shutdown(); }

    public void scan() {
        // Include saved versions whose training task has since been rerun or soft-deleted.
        List<Map<String, Object>> rows = jdbc.queryForList(
            "SELECT t.tenant_id,t.id training_id,t.dataset_id,t.config_params,t.model_output_path pt,t.onnx_model_output_path onnx,t.om_model_output_path om,t.rknn_model_output_path rknn,t.int8_rknn_model_output_path int8_rknn FROM vls_algorithm_training t "
            + "WHERE t.is_deleted=0 AND t.train_status='completed' AND COALESCE(t.onnx_conversion_status,'')<>'converting' AND COALESCE(t.om_conversion_status,'')<>'converting' "
            + "AND NOT EXISTS(SELECT 1 FROM vls_dataset_conversion_guard g WHERE g.tenant_id=t.tenant_id AND g.training_id=t.id) "
            + "UNION ALL SELECT m.tenant_id,m.training_id,t.dataset_id,t.config_params,m.model_path,m.onnx_model_path,m.om_model_output_path,m.rknn_model_path,m.int8_rknn_model_output_path FROM vls_algorithm_model m "
            + "LEFT JOIN vls_algorithm_training t ON t.tenant_id=m.tenant_id AND t.id=m.training_id AND BINARY t.model_output_path=BINARY m.model_path "
            + "WHERE m.is_deleted=0 AND NOT EXISTS(SELECT 1 FROM vls_dataset_conversion_guard g WHERE g.tenant_id=m.tenant_id AND g.training_id=m.training_id)");
        for (Map<String, Object> row : rows) archiveRow(row);
    }

    void archiveRow(Map<String, Object> row) {
        String previous = TenantContextHolder.getTenantId();
        String tenant = string(row, "tenant_id");
        if (tenant == null || tenant.trim().isEmpty()) return;
        TenantContextHolder.setTenantId(tenant);
        try {
            for (String type : new String[]{"pt", "onnx", "om", "rknn", "int8_rknn"}) {
                String path = string(row, type);
                if (path == null || path.trim().isEmpty()) continue;
                try {
                    store.archive(path, path.substring(path.lastIndexOf('/') + 1), local -> {
                        RemoteModelArtifactService.ArtifactMetadata source = remote.inspectRemote(path);
                        try (OutputStream output = Files.newOutputStream(local)) { remote.streamRemote(path, output); }
                        try (InputStream input = Files.newInputStream(local)) {
                            if (Files.size(local) != source.getFileSize() || !ModelArtifactObjectStore.digest(input).equals(source.getSha256())) {
                                throw new IOException("远端模型复制校验失败");
                            }
                        }
                    });
                } catch (IOException ex) { log.warn("Model archival pending: training={}, type={}, reason={}", row.get("training_id"), type, ex.getMessage()); }
            }
            String pt = string(row, "pt");
            if (pt != null && !pt.trim().isEmpty() && row.get("training_id") != null) {
                AlgorithmTraining training = new AlgorithmTraining();
                training.setId(((Number) row.get("training_id")).longValue());
                training.setTenantId(tenant);
                training.setModelOutputPath(pt);
                training.setDatasetId(row.get("dataset_id") == null ? null : ((Number) row.get("dataset_id")).longValue());
                training.setConfigParams(string(row, "config_params"));
                try {
                    store.archive(ModelClassFileService.storagePath(pt), "classes.yaml", local -> {
                        ModelClassFileService.ClassFile file = classes.prepare(training);
                        Files.write(local, file.getContent().getBytes(StandardCharsets.UTF_8));
                    });
                } catch (IOException ex) { log.warn("Class archival pending: training={}, reason={}", training.getId(), ex.getMessage()); }
            }
        } finally {
            if (previous == null) TenantContextHolder.clear(); else TenantContextHolder.setTenantId(previous);
        }
    }

    private static String string(Map<String, Object> row, String name) {
        Object value = row.get(name);
        return value == null ? null : value.toString();
    }
}
