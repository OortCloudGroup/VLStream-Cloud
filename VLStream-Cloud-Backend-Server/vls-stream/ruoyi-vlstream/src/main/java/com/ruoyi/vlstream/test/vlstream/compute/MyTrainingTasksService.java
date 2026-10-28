package com.ruoyi.vlstream.test.vlstream.compute;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

/** One owned task per row, across every compute node and historical default-GPU execution. */
@Service
@RequiredArgsConstructor
public class MyTrainingTasksService {
    private final JdbcTemplate jdbc;
    @org.springframework.beans.factory.annotation.Autowired
    private ComputePublicTaskAccess publicAccess;
    private static final Set<String> STATES = new HashSet<>(Arrays.asList("PENDING", "QUEUED", "PREPARING", "RUNNING", "ARCHIVING", "COMPLETED", "FAILED", "CANCELLED", "STOPPED"));
    private static final String BASE = "SELECT CAST(t.id AS CHAR) AS id,t.task_name AS taskName,t.progress,DATE_FORMAT(t.create_time,'%Y-%m-%d %H:%i:%s') AS createTime," +
        "CASE WHEN j.id IS NOT NULL THEN COALESCE(JSON_UNQUOTE(JSON_EXTRACT(IF(JSON_VALID(t.config_params),t.config_params,'{}'),'$.computeNodeName')),n.name,'已移除线上节点') ELSE '默认算力/历史任务' END AS computeName," +
        "CASE WHEN j.id IS NOT NULL THEN j.job_state WHEN t.train_status='completed' THEN 'COMPLETED' WHEN t.train_status='failed' THEN 'FAILED' WHEN t.train_status IN ('stop','stopped','cancelled') THEN 'STOPPED' WHEN c.instance_status='queued' THEN 'QUEUED' WHEN c.instance_status='starting' THEN 'PREPARING' WHEN t.train_status='training' THEN 'RUNNING' ELSE 'PENDING' END AS executionState " +
        "FROM vls_algorithm_training t LEFT JOIN vls_cloud_training_job j ON j.tenant_id=t.tenant_id AND j.training_id=t.id AND j.id=JSON_UNQUOTE(JSON_EXTRACT(IF(JSON_VALID(t.config_params),t.config_params,'{}'),'$.cloudJobId')) " +
        "LEFT JOIN vls_compute_node n ON n.id=j.node_id LEFT JOIN vls_container_instance c ON c.tenant_id=t.tenant_id AND c.training_task_id=t.id AND c.id=(SELECT MAX(c2.id) FROM vls_container_instance c2 WHERE c2.tenant_id=t.tenant_id AND c2.training_task_id=t.id) " +
        "WHERE BINARY t.tenant_id=BINARY ? AND BINARY t.create_user=BINARY ? AND t.is_deleted=0";

    @Data
    public static class Page {
        private List<Map<String,Object>> records;
        private long total;
        private Map<String,Long> statistics = new LinkedHashMap<>();
    }
    @org.springframework.transaction.annotation.Transactional(readOnly = true, isolation = org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Page page(int current, int size, String name, String state, String start, String end) {
        String user = authenticatedUser();
        if (user == null || user.trim().isEmpty()) throw new ServiceException("缺少可信登录用户，不能查询我的训练任务");
        return query(ComputeNodeService.tenant(), user, current, size, name, state, start, end);
    }
    protected String authenticatedUser() { return LoginHelper.getUserId(); }
    Page query(String tenant, String user, int current, int size, String name, String state, String start, String end) {
        if (tenant == null || tenant.trim().isEmpty() || user == null || user.trim().isEmpty()) throw new ServiceException("缺少任务归属上下文");
        if (current < 1 || current > 100000 || size < 1 || size > 100) throw new ServiceException("分页参数无效");
        if (state != null && !state.isEmpty() && !STATES.contains(state)) throw new ServiceException("无效的训练阶段");
        List<Object> params = new ArrayList<>(Arrays.asList(tenant,user));
        StringBuilder filter = new StringBuilder(BASE);
        filter.append(" AND ").append(ComputePublicTaskAccess.jdbcPredicate("t.id",params,
            publicAccess==null?"":publicAccess.viewer(),publicAccess==null?"":publicAccess.sharedViewer(),tenant));
        if (name != null && !name.trim().isEmpty()) {
            if (name.length() > 100) throw new ServiceException("任务名称筛选过长");
            filter.append(" AND LOCATE(?,t.task_name)>0"); params.add(name.trim());
        }
        try {
            if (start != null && !start.isEmpty()) { filter.append(" AND t.create_time>=?"); params.add(java.sql.Timestamp.valueOf(start)); }
            if (end != null && !end.isEmpty()) { filter.append(" AND t.create_time<=?"); params.add(java.sql.Timestamp.valueOf(end)); }
        } catch (IllegalArgumentException e) { throw new ServiceException("时间筛选格式应为 yyyy-MM-dd HH:mm:ss"); }
        String sql = "SELECT * FROM (" + filter + ") owned";
        if (state != null && !state.isEmpty()) { sql += " WHERE executionState=?"; params.add(state); }
        Page result = new Page();
        result.setTotal(jdbc.queryForObject("SELECT COUNT(*) FROM (" + sql + ") counted", Long.class, params.toArray()));
        for (String value : new TreeSet<>(STATES)) result.getStatistics().put(value,0L);
        for (Map<String,Object> row : jdbc.queryForList("SELECT executionState,COUNT(*) AS amount FROM (" + sql + ") grouped GROUP BY executionState",params.toArray()))
            result.getStatistics().put(String.valueOf(row.get("executionState")),((Number)row.get("amount")).longValue());
        params.add(size); params.add((long)(current-1)*size);
        result.setRecords(jdbc.queryForList(sql + " ORDER BY createTime DESC,id DESC LIMIT ? OFFSET ?",params.toArray()));
        return result;
    }
}
