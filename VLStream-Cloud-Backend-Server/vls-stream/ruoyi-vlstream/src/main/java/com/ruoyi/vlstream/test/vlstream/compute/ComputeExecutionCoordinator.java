package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.config.ModelHubProperties;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Binds a local durable job to its Deployment claim and retries confirmed terminal releases. */
@Component
@RequiredArgsConstructor
@Slf4j
public class ComputeExecutionCoordinator {
    private final ComputeCatalogProperties properties;
    private final UnifiedComputeCatalog catalog;
    private final ConfiguredComputeRegistry registry;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public boolean managed() {
        return properties.configured() && properties.usesDeploymentLeases();
    }

    public String scopeFingerprint() {
        if (!properties.configured()) throw unavailable();
        ModelHubProperties remote = properties.getDeployment();
        return ModelArtifactObjectStore.hash(remote.getBaseUrl().trim().replaceAll("/+$", "") + "\n" + remote.getAppId());
    }

    public boolean acquire(Long nodeId, String jobRef) {
        return !managed() || acquire(registry.resolve(nodeId), jobRef);
    }

    public boolean acquire(ComputeNode node, String jobRef) {
        if (!managed() || !node.isPlatformCatalogManaged()) return true;
        Long id = node.getId();
        long revision = node.getConfigurationRevision();
        String scope = scopeFingerprint();
        jdbc.update("INSERT INTO vls_compute_execution_lease(node_id,job_ref,app_scope,execution_revision) "
            + "VALUES(?,?,?,?) ON DUPLICATE KEY UPDATE node_id=node_id", id, jobRef, scope, revision);
        Map<String, Object> binding = jdbc.queryForMap(
            "SELECT app_scope,execution_revision,acquired,release_requested FROM vls_compute_execution_lease WHERE node_id=? AND job_ref=?",
            id, jobRef);
        if (!scope.equals(binding.get("app_scope")) || revision != ((Number) binding.get("execution_revision")).longValue())
            throw new ServiceException("算力占用绑定的原应用或执行版本已变化");
        if (flag(binding.get("release_requested"))) return false;
        if (flag(binding.get("acquired"))) return true;
        JsonNode result = call("executionAcquire", jobPayload(id, jobRef).put("executionRevision", revision));
        String state = result.path("state").asText();
        if ("OCCUPIED".equals(state) || "RELEASED".equals(state)) return false;
        if (!"ACQUIRED".equals(state) || result.path("executionRevision").asLong(-1) != revision) throw unavailable();
        return jdbc.update("UPDATE vls_compute_execution_lease SET acquired=1 "
            + "WHERE node_id=? AND job_ref=? AND app_scope=? AND release_requested=0", id, jobRef, scope) > 0;
    }

    /** Joins the transaction that records verified terminal state. */
    public void enqueueRelease(Long nodeId, String jobRef) {
        if (!properties.configured()) return;
        jdbc.update("UPDATE vls_compute_execution_lease SET release_requested=1 "
            + "WHERE node_id=? AND job_ref=? AND app_scope=? AND release_requested=0", nodeId, jobRef, scopeFingerprint());
    }

    /** For paid orders whose original execution application was pinned when the order was accepted. */
    public void enqueueRelease(Long nodeId, String jobRef, String expectedScope) {
        jdbc.update("INSERT INTO vls_compute_execution_lease(node_id,job_ref,app_scope,execution_revision,release_requested) "
            + "VALUES(?,?,?,0,1) ON DUPLICATE KEY UPDATE "
            + "release_requested=CASE WHEN app_scope=VALUES(app_scope) THEN 1 ELSE release_requested END",
            nodeId, jobRef, expectedScope);
    }

    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    void deliverReleases() {
        if (!managed()) return;
        String scope = scopeFingerprint();
        try {
            for (Map<String, Object> row : jdbc.queryForList("SELECT node_id,job_ref,retry_count FROM vls_compute_execution_lease "
                    + "WHERE app_scope=? AND release_requested=1 AND retry_after<=NOW() ORDER BY retry_after LIMIT 20", scope)) {
                long nodeId = ((Number) row.get("node_id")).longValue();
                String jobRef = String.valueOf(row.get("job_ref"));
                try {
                    release(nodeId, jobRef);
                    jdbc.update("DELETE FROM vls_compute_execution_lease WHERE node_id=? AND job_ref=? AND app_scope=? AND release_requested=1",
                        nodeId, jobRef, scope);
                } catch (ServiceException unavailable) {
                    int delay = Math.min(300, 5 << Math.min(6, ((Number) row.get("retry_count")).intValue()));
                    Timestamp retryAfter = new Timestamp(System.currentTimeMillis() + 1000L * delay);
                    jdbc.update("UPDATE vls_compute_execution_lease SET retry_count=retry_count+1,retry_after=? "
                        + "WHERE node_id=? AND job_ref=? AND app_scope=? AND release_requested=1", retryAfter, nodeId, jobRef, scope);
                    return;
                }
            }
        } catch (RuntimeException retry) {
            log.warn("Deployment GPU release retry failed: {}", retry.getClass().getSimpleName());
        }
    }

    public void release(Long nodeId, String jobRef) {
        if (!managed()) return;
        if (!"RELEASED".equals(call("executionRelease", jobPayload(nodeId, jobRef)).path("state").asText())) throw unavailable();
    }

    /** Empty or missing entries mean unknown. Catalog cache is never a source of availability. */
    Map<Long, String> occupancy() {
        if (!managed()) return Collections.emptyMap();
        try {
            JsonNode rows = call("executionOccupancy", json.createObjectNode()).path("resources");
            if (!rows.isArray()) return Collections.emptyMap();
            Map<Long, String> result = new HashMap<>();
            for (JsonNode row : rows) {
                long id = Long.parseLong(row.path("nodeId").asText());
                String state = row.path("state").asText();
                if ("FREE".equals(state)) result.put(id, state);
                else if ("OCCUPIED".equals(state)) result.put(id, row.path("ownedByCaller").asBoolean() ? "OWNED" : "OCCUPIED");
            }
            return result;
        } catch (RuntimeException unavailable) {
            return Collections.emptyMap();
        }
    }

    private ObjectNode jobPayload(Long nodeId, String jobRef) {
        return json.createObjectNode().put("nodeId", String.valueOf(nodeId)).put("jobRef", jobRef);
    }

    private JsonNode call(String action, ObjectNode payload) {
        try {
            JsonNode response = catalog.request(action, json.writeValueAsBytes(payload));
            if (response.path("code").asInt() == 200 && response.path("data").isObject()) return response.path("data");
        } catch (Exception unavailable) {
            // Server responses may contain connection details. Keep the user-facing message fixed.
        }
        throw unavailable();
    }

    private static ServiceException unavailable() {
        return new ServiceException("统一算力占用状态暂不可用，请稍后重试");
    }

    private static boolean flag(Object value) {
        return Boolean.TRUE.equals(value) || value instanceof Number && ((Number) value).intValue() != 0;
    }
}
