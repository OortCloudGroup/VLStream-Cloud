package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.vlstream.test.vlstream.data.SmartAnnotationTask;

public interface SmartAnnotationTaskMapper extends BaseMapper<SmartAnnotationTask> {
    @com.baomidou.mybatisplus.annotation.InterceptorIgnore(tenantLine = "true")
    @org.apache.ibatis.annotations.Select("SELECT * FROM vls_smart_annotation_task WHERE task_state='CONFIRMING' AND is_deleted=0 ORDER BY id LIMIT 20")
    java.util.List<SmartAnnotationTask> selectConfirmingForWorker();
}
