package com.ruoyi.vlstream.test.vlstream.service;

import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ModelClassSnapshotStore {
    private final JdbcTemplate jdbc;

    public ModelClassFileService.ClassFile find(AlgorithmTraining training, String path) {
        String tenant = training.getTenantId();
        if (tenant == null || tenant.trim().isEmpty()) tenant = com.ruoyi.common.helper.TenantContextHolder.getTenantId();
        if (tenant == null || tenant.trim().isEmpty()) throw new com.ruoyi.common.exception.ServiceException("缺少模型类别快照的租户上下文");
        List<ModelClassFileService.ClassFile> files = jdbc.query(
            "SELECT file_name,content,sha256 FROM vls_model_class_snapshot WHERE tenant_id=? AND training_id=? AND model_path=?",
            (rs, n) -> new ModelClassFileService.ClassFile(rs.getString(1), rs.getString(2),
                rs.getString(2).getBytes(StandardCharsets.UTF_8).length, rs.getString(3)),
            tenant, training.getId(), path);
        return files.isEmpty() ? null : files.get(0);
    }

    public void save(AlgorithmTraining training, ModelClassFileService.ClassFile file) {
        jdbc.update("INSERT INTO vls_model_class_snapshot(tenant_id,training_id,model_path,file_name,content,sha256) VALUES(?,?,?,?,?,?) "
            + "ON DUPLICATE KEY UPDATE model_path=VALUES(model_path),file_name=VALUES(file_name),content=VALUES(content),sha256=VALUES(sha256)",
            training.getTenantId(), training.getId(), training.getModelOutputPath(), file.getFileName(), file.getContent(), file.getSha256());
    }
}
