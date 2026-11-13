package com.ruoyi.vlstream.test.vlstream.cloudai;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;

/** Desired bindings survive restarts; only a confirmed terminal run permits a replacement. */
@Component
@RequiredArgsConstructor
@Slf4j
public class CloudDeviceCoordinator {
    private final JdbcTemplate jdbc;
    private final CloudDeviceService service;
    private final TransactionTemplate transactions;

    @Scheduled(fixedDelay=5000, initialDelay=15000)
    public void tick() {
        try {
            List<Map<String,Object>> groups=jdbc.queryForList("SELECT id,tenant_id,actor_id,scope FROM vls_cloud_device_group WHERE scope=? AND retry_at<=? ORDER BY id",service.executionScope(),System.currentTimeMillis());
            for(Map<String,Object> group:groups) {
                try { transactions.execute(status->{ reconcile(group);return null; }); }
                catch(RuntimeException error) { log.warn("Cloud device group {} could not reconcile ({})",group.get("id"),error.getClass().getSimpleName()); }
            }
        } catch(RuntimeException error) { log.debug("Cloud device configuration is not ready ({})",error.getClass().getSimpleName()); }
    }
    void reconcile(Map<String,Object> candidate) {
        // Same lock order as user mutations prevents lost updates and duplicate launches.
        jdbc.queryForMap("SELECT * FROM vls_cloud_device_owner WHERE tenant_id=? AND actor_id=? AND scope=? FOR UPDATE",candidate.get("tenant_id"),candidate.get("actor_id"),candidate.get("scope"));
        Map<String,Object> group=jdbc.queryForMap("SELECT * FROM vls_cloud_device_group WHERE id=? FOR UPDATE",candidate.get("id"));
        if(!service.executionScope().equals(group.get("scope"))) return;
        List<Map<String,Object>> active=jdbc.queryForList("SELECT * FROM vls_cloud_device_run WHERE id=?",group.get("run_id"));
        long now=System.currentTimeMillis();
        if(!active.isEmpty()) {
            Map<String,Object> run=active.get(0);
            if(!CloudAiService.TERMINAL.contains(String.valueOf(run.get("state")))) return;
            if("FAILED".equals(run.get("state")) && run.get("ended_at")!=null && now-((Number)run.get("ended_at")).longValue()<60000) return;
        }
        ObjectNode snapshot=service.aggregate(String.valueOf(group.get("id")));
        if(snapshot.path("rules").isEmpty()) return;
        // A binding moved across GPUs must not run on both while the old GPU is stopping.
        List<Map<String,Object>> retiring=jdbc.queryForList("SELECT snapshot_json FROM vls_cloud_device_run WHERE tenant_id=? AND actor_id=? AND scene_id<>? AND cancel_requested=1 AND state NOT IN ('COMPLETED','FAILED','CANCELLED','SKIPPED')",group.get("tenant_id"),group.get("actor_id"),group.get("id"));
        Set<String> desired=new HashSet<>();
        snapshot.path("rules").forEach(rule->desired.add(rule.path("bindingId").asText()));
        for(Map<String,Object> old:retiring) {
            for(com.fasterxml.jackson.databind.JsonNode rule:service.read(String.valueOf(old.get("snapshot_json"))).path("rules"))
                if(desired.contains(rule.path("bindingId").asText())) return;
        }
        try { service.requireRunAdmission(snapshot); }
        catch (com.ruoyi.common.exception.ServiceException unavailable) {
            if (!active.isEmpty() && "SKIPPED".equals(active.get(0).get("state"))) {
                jdbc.update("UPDATE vls_cloud_device_run SET message=? WHERE id=?", unavailable.getMessage(), group.get("run_id"));
            } else {
                String skipped = UUID.randomUUID().toString();
                jdbc.update("INSERT INTO vls_cloud_device_run(id,tenant_id,actor_id,task_id,scene_id,window_key,planned_start,snapshot_json,node_id,state,message,ended_at) VALUES(?,?,?,?,?,?,?,?,?,'SKIPPED',?,?)",
                    skipped, group.get("tenant_id"), group.get("actor_id"), group.get("id"), group.get("id"), skipped, now, snapshot.toString(), group.get("node_id"), unavailable.getMessage(), now);
                jdbc.update("UPDATE vls_cloud_device_group SET run_id=? WHERE id=?", skipped, group.get("id"));
            }
            jdbc.update("UPDATE vls_cloud_device_group SET retry_at=? WHERE id=?", now + 60000, group.get("id"));
            return;
        }
        String id=UUID.randomUUID().toString();
        jdbc.update("INSERT INTO vls_cloud_device_run(id,tenant_id,actor_id,task_id,scene_id,window_key,planned_start,snapshot_json,node_id,state,message) VALUES(?,?,?,?,?,?,?,?,?,'QUEUED','等待GPU，持续配置已保存')",id,group.get("tenant_id"),group.get("actor_id"),group.get("id"),group.get("id"),id,now,snapshot.toString(),group.get("node_id"));
        jdbc.update("UPDATE vls_cloud_device_group SET run_id=? WHERE id=?",id,group.get("id"));
    }
}
