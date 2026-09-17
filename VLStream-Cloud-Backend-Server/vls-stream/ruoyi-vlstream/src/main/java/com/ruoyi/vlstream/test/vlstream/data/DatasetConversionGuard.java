package com.ruoyi.vlstream.test.vlstream.data;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The caller holds the project row lock until the conversion intent is committed. */
@Repository
@RequiredArgsConstructor
public class DatasetConversionGuard {
    private final JdbcTemplate jdbc;
    public void start(AlgorithmTraining training) {
        String tenant = com.ruoyi.common.helper.TenantContextHolder.getTenantId();
        if (tenant == null || tenant.trim().isEmpty() || jdbc.queryForObject(
            "SELECT COUNT(*) FROM vls_algorithm_training WHERE id=? AND tenant_id=? AND dataset_id=?", Integer.class,
            training.getId(), tenant, training.getDatasetId()) != 1) throw new ServiceException("训练任务不存在或无权访问");
        training.setTenantId(tenant);
        int inserted = jdbc.update("INSERT IGNORE INTO vls_dataset_conversion_guard(tenant_id,training_id,dataset_id) VALUES(?,?,?)",
            training.getTenantId(), training.getId(), training.getDatasetId());
        if (inserted != 1) throw new ServiceException("该任务仍有模型转换占用数据集，请等待结束；服务异常退出后需核实远端任务再解除占用");
    }
    public void finish(AlgorithmTraining training) {
        jdbc.update("DELETE FROM vls_dataset_conversion_guard WHERE tenant_id=? AND training_id=?", training.getTenantId(), training.getId());
    }
}
