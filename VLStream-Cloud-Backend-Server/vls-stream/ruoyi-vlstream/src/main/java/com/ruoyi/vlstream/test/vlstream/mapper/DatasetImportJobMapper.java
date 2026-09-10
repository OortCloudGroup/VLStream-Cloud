package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.vlstream.test.vlstream.data.DatasetImportJob;
import com.ruoyi.vlstream.test.vlstream.data.DatasetUploadPart;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface DatasetImportJobMapper extends BaseMapper<DatasetImportJob> {
    @Select("SELECT * FROM vls_dataset_import_job WHERE id=#{id} AND tenant_id=#{tenant} AND is_deleted=0 FOR UPDATE")
    DatasetImportJob lock(@Param("id") Long id, @Param("tenant") String tenant);

    @Select("SELECT * FROM vls_dataset_upload_part WHERE job_id=#{id} ORDER BY part_number")
    List<DatasetUploadPart> parts(Long id);

    @Insert("INSERT INTO vls_dataset_upload_part(job_id,part_number,part_size,sha256,etag) VALUES(#{jobId},#{partNumber},#{partSize},#{sha256},#{etag}) " +
        "ON DUPLICATE KEY UPDATE part_size=VALUES(part_size),sha256=VALUES(sha256),etag=VALUES(etag)")
    int savePart(DatasetUploadPart part);

    @Delete("DELETE FROM vls_dataset_upload_part WHERE job_id=#{id}")
    int clearParts(Long id);

    @com.baomidou.mybatisplus.annotation.InterceptorIgnore(tenantLine="true")
    @Select("SELECT * FROM vls_dataset_import_job WHERE is_deleted=0 AND (job_state='QUEUED' OR (job_state='PROCESSING' AND heartbeat_at < DATE_SUB(NOW(), INTERVAL 10 MINUTE))) ORDER BY id LIMIT 5")
    List<DatasetImportJob> pending();

    @com.baomidou.mybatisplus.annotation.InterceptorIgnore(tenantLine="true")
    @Update("UPDATE vls_dataset_import_job SET job_state='PROCESSING',worker_id=#{worker},heartbeat_at=NOW(),update_time=NOW() WHERE id=#{id} AND (job_state='QUEUED' OR (job_state='PROCESSING' AND heartbeat_at < DATE_SUB(NOW(),INTERVAL 10 MINUTE)))")
    int claim(@Param("id") Long id, @Param("worker") String worker);

    @com.baomidou.mybatisplus.annotation.InterceptorIgnore(tenantLine="true")
    @Select("SELECT * FROM vls_dataset_import_job WHERE is_deleted=0 AND expires_at<NOW() AND import_type='local' AND object_key IS NOT NULL AND job_state NOT IN ('PROCESSING','QUEUED') LIMIT 20")
    List<DatasetImportJob> expired();
}
