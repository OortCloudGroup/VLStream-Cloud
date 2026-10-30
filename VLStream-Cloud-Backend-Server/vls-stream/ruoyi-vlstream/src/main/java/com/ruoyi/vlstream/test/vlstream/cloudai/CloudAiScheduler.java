package com.ruoyi.vlstream.test.vlstream.cloudai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.*;

/** One durable occurrence per schedule window, including downtime and duplicate scanners. */
@Component
@RequiredArgsConstructor
@Slf4j
public class CloudAiScheduler {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final CloudAiService service;

    @Scheduled(fixedDelayString = "${vlstream.cloud-ai.schedule-delay-ms:5000}")
    public void tick() {
        try {
            List<String> due = jdbc.queryForList("SELECT id FROM vls_cloud_ai_task WHERE enabled=1 AND deleted=0 AND next_start<=? ORDER BY next_start LIMIT 100", String.class, System.currentTimeMillis());
            for (String id : due) {
                try { transactions.execute(status -> { occurrence(id, System.currentTimeMillis()); return null; }); }
                catch (RuntimeException error) { log.warn("Cloud AI schedule {} could not advance ({})", id, error.getClass().getSimpleName()); }
            }
        } catch (org.springframework.dao.DataAccessException error) { log.debug("Cloud AI schedule storage is not ready"); }
    }

    void occurrence(String id, long now) {
        List<Map<String,Object>> tasks = jdbc.queryForList("SELECT * FROM vls_cloud_ai_task WHERE id=? AND enabled=1 AND deleted=0 AND next_start<=? FOR UPDATE", id, now);
        if (tasks.isEmpty()) return;
        Map<String,Object> task = tasks.get(0);
        List<Map<String,Object>> scenes = jdbc.queryForList("SELECT * FROM vls_cloud_ai_scene WHERE id=? AND tenant_id=? AND actor_id=? FOR UPDATE", task.get("scene_id"), task.get("tenant_id"), task.get("actor_id"));
        if (scenes.isEmpty()) {
            jdbc.update("UPDATE vls_cloud_ai_task SET enabled=0 WHERE id=?", id); return;
        }
        Map<String,Object> scene = scenes.get(0);
        long start = ((Number) task.get("next_start")).longValue(), end = ((Number) task.get("next_end")).longValue();
        boolean expired = end <= now, disabled = !CloudAiService.flag(scene.get("enabled")) || CloudAiService.flag(scene.get("deleted"));
        service.createRun(task, scene, Long.toString(start / 1000), start, end, null,
            expired || disabled ? "SKIPPED" : "QUEUED", expired ? "执行窗口已结束，未补跑" : disabled ? "场景未启用，本次已跳过" : "等待GPU调度");
        CloudAiSchedule schedule = service.schedule(String.valueOf(task.get("schedule_json")));
        CloudAiSchedule.Window next = schedule.after(new CloudAiSchedule.Window(Instant.ofEpochMilli(start), Instant.ofEpochMilli(end)));
        jdbc.update("UPDATE vls_cloud_ai_task SET next_start=?,next_end=?,enabled=? WHERE id=?",
            next == null ? null : next.start.toEpochMilli(), next == null ? null : next.end.toEpochMilli(), next != null, id);
    }
}
