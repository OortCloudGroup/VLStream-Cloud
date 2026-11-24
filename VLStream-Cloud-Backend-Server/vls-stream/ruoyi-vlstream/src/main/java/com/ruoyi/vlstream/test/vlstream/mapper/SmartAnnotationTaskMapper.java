package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.vlstream.test.vlstream.data.SmartAnnotationTask;

public interface SmartAnnotationTaskMapper extends BaseMapper<SmartAnnotationTask> {
    @com.baomidou.mybatisplus.annotation.InterceptorIgnore(tenantLine = "true")
    @org.apache.ibatis.annotations.Select("SELECT * FROM vls_smart_annotation_task WHERE task_state IN ('CONFIRMING','AUTO_CONFIRMING') AND is_deleted=0 ORDER BY id LIMIT 20")
    java.util.List<SmartAnnotationTask> selectConfirmingForWorker();

    @com.baomidou.mybatisplus.annotation.InterceptorIgnore(tenantLine = "true")
    @org.apache.ibatis.annotations.Select("SELECT id, tenant_id FROM vls_smart_annotation_task "
        + "WHERE mode='llm' AND task_state IN ('QUEUED','RUNNING','" + SmartAnnotationTask.LLM_QUEUED + "','"
        + SmartAnnotationTask.LLM_RUNNING + "') AND is_deleted=0 ORDER BY update_time, id LIMIT 20")
    java.util.List<SmartAnnotationTask> selectLlmForWorker();
}
