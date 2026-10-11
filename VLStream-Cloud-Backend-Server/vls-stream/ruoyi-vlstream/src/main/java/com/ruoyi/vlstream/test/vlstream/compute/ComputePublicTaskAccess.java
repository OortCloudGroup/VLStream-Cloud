package com.ruoyi.vlstream.test.vlstream.compute;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;

/** Narrows public-compute records only; private records keep their existing tenant/role checks. */
@Service
@RequiredArgsConstructor
public class ComputePublicTaskAccess {
    private final JdbcTemplate jdbc;
    private final ComputePublicInventoryService inventory;
    public String viewer() { String value=inventory.viewer();return value==null?"":value; }
    public static String predicate(String trustedTrainingReference) {
        return "NOT EXISTS (SELECT 1 FROM vls_cloud_training_job pj JOIN vls_compute_node pn ON pn.id=pj.node_id WHERE pj.training_id="
            +trustedTrainingReference+" AND (pj.public_instance_key IS NOT NULL OR pn.public_instance_key IS NOT NULL OR pn.resource_scope='PLATFORM')"
            +" AND (pj.public_owner_subject IS NULL OR pj.public_owner_subject <> {0}))";
    }
    public <T> QueryWrapper<T> filter(QueryWrapper<T> query,String trustedTrainingReference) {
        return query.apply(predicate(trustedTrainingReference),viewer());
    }
    public void training(Long id) {
        if(id==null)return;
        List<Map<String,Object>> rows=jdbc.queryForList("SELECT pj.tenant_id,pj.public_owner_subject FROM vls_cloud_training_job pj JOIN vls_compute_node pn ON pn.id=pj.node_id WHERE pj.training_id=? AND (pj.public_instance_key IS NOT NULL OR pn.public_instance_key IS NOT NULL OR pn.resource_scope='PLATFORM')",id);
        if(rows.isEmpty())return;
        String owner=viewer();
        if(owner.isEmpty() || rows.stream().anyMatch(r->!Objects.equals(owner,r.get("public_owner_subject")) || !Objects.equals(ComputeNodeService.tenant(),r.get("tenant_id"))))
            throw new ServiceException("无权读取或操作其他用户的公共训练记录与产物");
    }
    public void model(Long id) {
        if(id==null)return;
        List<Long> rows=jdbc.queryForList("SELECT training_id FROM vls_algorithm_model WHERE id=?",Long.class,id);
        for(Long training:rows)training(training);
    }
    public static String containerPredicate(String trustedId,String trustedTrainingReference) {
        return "NOT EXISTS (SELECT 1 FROM vls_cloud_training_job pj JOIN vls_compute_node pn ON pn.id=pj.node_id WHERE (pj.container_record_id="
            +trustedId+" OR pj.training_id="+trustedTrainingReference+") AND (pj.public_instance_key IS NOT NULL OR pn.public_instance_key IS NOT NULL OR pn.resource_scope='PLATFORM')"
            +" AND (pj.public_owner_subject IS NULL OR pj.public_owner_subject <> {0}))";
    }
    public <T> QueryWrapper<T> filterContainers(QueryWrapper<T> query) {
        return query.apply(containerPredicate("vls_container_instance.id","vls_container_instance.training_task_id"),viewer());
    }
    public void container(Long id) {
        if(id==null)return;
        List<Long> jobs=jdbc.queryForList("SELECT training_id FROM vls_cloud_training_job WHERE container_record_id=? UNION SELECT training_task_id FROM vls_container_instance WHERE id=?",Long.class,id,id);
        for(Long training:jobs)training(training);
    }
    public void containerFromHttp(Long id) { if(RequestContextHolder.getRequestAttributes()!=null)container(id); }
    /** Existing workers use explicit tenant context and persisted jobs, without an HTTP user session. */
    public void trainingFromHttp(Long id) { if(RequestContextHolder.getRequestAttributes()!=null)training(id); }
    public void modelFromHttp(Long id) { if(RequestContextHolder.getRequestAttributes()!=null)model(id); }
}
