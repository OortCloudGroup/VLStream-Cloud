package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmModelMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmTrainingMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmModel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmModelVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;

/** Durable, transactionally idempotent publication independent of browser polling. */
@Service
@Slf4j
@RequiredArgsConstructor
public class TrainingPublicationService {
    private final JdbcTemplate jdbc;
    private final PlatformTransactionManager transactions;
    private final VlsAlgorithmTrainingMapper trainings;
    private final VlsAlgorithmModelMapper models;
    private final IVlsAlgorithmModelService modelService;

    public void lockForStart(Long id) {
        String tenant = tenant();
        if (jdbc.queryForList("SELECT id FROM vls_algorithm_training WHERE id=? AND tenant_id=? AND is_deleted=0 FOR UPDATE", id, tenant).isEmpty()) throw new ServiceException("训练任务不存在或无权访问");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM vls_container_instance WHERE tenant_id=? AND training_task_id=? AND instance_status IN ('queued','starting','running','stopping')", Integer.class, tenant, id) > 0) throw new ServiceException("任务已排队或正在运行，请勿重复启动");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM vls_dataset_conversion_guard WHERE tenant_id=? AND training_id=?", Integer.class, tenant, id) > 0) throw new ServiceException("模型转换尚未完成，请勿重新训练");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM vls_algorithm_training WHERE tenant_id=? AND id=? AND (train_status='training' OR onnx_conversion_status='converting' OR om_conversion_status='converting')", Integer.class, tenant, id) > 0) throw new ServiceException("任务仍在训练或转换中");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM vls_training_publication p JOIN vls_algorithm_training t ON t.id=p.training_id AND t.tenant_id=p.tenant_id WHERE p.tenant_id=? AND p.training_id=? AND p.publication_state IN ('PENDING','FAILED') AND t.train_status='completed'", Integer.class, tenant, id) > 0) throw new ServiceException("上一轮模型尚未发布完成，请先重试发布");
    }

    public void configure(Long id, boolean enabled) {
        jdbc.update("UPDATE vls_algorithm_training SET model_output_path=NULL,onnx_model_output_path=NULL,om_model_output_path=NULL,rknn_model_output_path=NULL,int8_rknn_model_output_path=NULL,onnx_conversion_status=NULL,om_conversion_status=NULL,start_time=NULL,end_time=NULL,epoch_current=0 WHERE tenant_id=? AND id=?", tenant(),id);
        jdbc.update("INSERT INTO vls_training_publication(tenant_id,training_id,publication_state) VALUES(?,?,?) ON DUPLICATE KEY UPDATE publication_state=VALUES(publication_state),model_id=NULL,model_path=NULL,attempts=0,error_message=NULL,update_time=NOW()", tenant(), id, enabled ? "PENDING" : "DISABLED");
    }

    public Map<String,Object> status(Long id) {
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT publication_state,CAST(model_id AS CHAR) AS model_id,attempts,error_message FROM vls_training_publication WHERE tenant_id=? AND training_id=?",tenant(),id);
        return rows.isEmpty() ? Collections.singletonMap("publication_state","DISABLED") : rows.get(0);
    }

    public void retry(Long id) {
        jdbc.update("UPDATE vls_training_publication SET publication_state='PENDING',attempts=0,error_message=NULL,update_time=NOW() WHERE tenant_id=? AND training_id=? AND publication_state='FAILED'",tenant(),id);
    }

    @Scheduled(fixedDelay = 15000, initialDelay = 20000)
    public void scan() {
        List<Map<String,Object>> pending;
        try {
            pending=jdbc.queryForList("SELECT p.tenant_id,p.training_id FROM vls_training_publication p JOIN vls_algorithm_training t ON t.id=p.training_id AND t.tenant_id=p.tenant_id WHERE p.publication_state='PENDING' AND t.is_deleted=0 AND t.train_status='completed' AND t.model_output_path IS NOT NULL AND t.model_output_path<>'' LIMIT 20");
        } catch (RuntimeException e) { log.warn("Auto publication scan unavailable; verify database migrations"); return; }
        for(Map<String,Object> row:pending) publish(String.valueOf(row.get("tenant_id")),((Number)row.get("training_id")).longValue());
    }

    public void publish(String tenant, Long id) {
        String previous=TenantContextHolder.getTenantId();
        TenantContextHolder.setTenantId(tenant);
        try {
            new TransactionTemplate(transactions).execute(tx -> {
                if(jdbc.queryForList("SELECT id FROM vls_algorithm_training WHERE tenant_id=? AND id=? AND is_deleted=0 FOR UPDATE",tenant,id).isEmpty()) return null;
                List<String> states=jdbc.queryForList("SELECT publication_state FROM vls_training_publication WHERE tenant_id=? AND training_id=? FOR UPDATE",String.class,tenant,id);
                if(states.isEmpty() || !"PENDING".equals(states.get(0))) return null;
                AlgorithmTraining training=trainings.selectById(id);
                if(training==null || training.getTrainStatus()==null || !"completed".equals(training.getTrainStatus().getCode()) || training.getModelOutputPath()==null || training.getModelOutputPath().trim().isEmpty()) return null;
                if (!Arrays.asList("completed","failed").contains(training.getOnnxConversionStatus())
                    || !Arrays.asList("completed","failed").contains(training.getOmConversionStatus())) return null;
                // Wait for the entire conversion chain, so the model row receives all successful formats.
                if(jdbc.queryForObject("SELECT COUNT(*) FROM vls_dataset_conversion_guard WHERE tenant_id=? AND training_id=?",Integer.class,tenant,id)>0
                    || "converting".equals(training.getOnnxConversionStatus()) || "converting".equals(training.getOmConversionStatus())) return null;
                List<AlgorithmModel> existing=models.selectList(new QueryWrapper<AlgorithmModel>().eq("tenant_id",tenant).eq("training_id",id).eq("model_path",training.getModelOutputPath()).last("LIMIT 1"));
                AlgorithmModel model;
                if(existing.isEmpty()) {
                    AlgorithmModelVO request=new AlgorithmModelVO(); request.setTrainingId(id); request.setAlgorithmId(training.getAlgorithmId());
                    request.setModelName(training.getTaskName()); request.setModelFormat("pt"); request.setVersion(1); request.setAccuracy(training.getAccuracy());
                    model=modelService.createModel(request);
                } else model=existing.get(0);
                jdbc.update("UPDATE vls_training_publication SET publication_state='COMPLETED',model_id=?,model_path=?,error_message=NULL,update_time=NOW() WHERE tenant_id=? AND training_id=?",model.getId(),training.getModelOutputPath(),tenant,id);
                return null;
            });
        } catch(RuntimeException e) {
            jdbc.update("UPDATE vls_training_publication SET attempts=attempts+1,publication_state=IF(attempts>=3,'FAILED','PENDING'),error_message='自动发布失败，请检查服务日志或重试',update_time=NOW() WHERE tenant_id=? AND training_id=? AND publication_state='PENDING'",tenant,id);
            log.warn("Auto publication failed for training {}: {}",id,e.getClass().getSimpleName());
        } finally {
            if(previous==null) TenantContextHolder.clear(); else TenantContextHolder.setTenantId(previous);
        }
    }

    private String tenant() {
        String tenant=TenantContextHolder.getTenantId();
        if(tenant==null || tenant.trim().isEmpty()) throw new ServiceException("缺少可信租户上下文");
        return tenant;
    }
}
