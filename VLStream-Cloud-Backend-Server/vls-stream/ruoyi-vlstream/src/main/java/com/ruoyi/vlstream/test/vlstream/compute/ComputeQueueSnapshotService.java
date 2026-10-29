package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import static com.ruoyi.vlstream.test.vlstream.compute.ComputeSsh.quote;

/** Read-only physical FIFO inspection, independent of billing and training execution. */
@Service
@RequiredArgsConstructor
public class ComputeQueueSnapshotService {
    private final ComputeNodeService nodes;
    private final ComputeSharedQueueService shared;
    private final ComputeSharedQueueProperties properties;
    private final ComputeSsh ssh;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final ComputeQueueWaitEstimator estimator;

    public ComputeQueueSnapshot snapshot(Long id) {
        if (!shared.authenticated()) throw new ServiceException("请先登录查看共享队列", 403);
        ComputeNode node = nodes.get(id);
        if (!node.isEnabled() || !"PLATFORM".equals(node.getResourceScope()) || !"FREE".equals(node.getBillingType()) || !shared.isShared(node))
            throw new ServiceException("此资源不提供免费共享队列", 403);
        ComputeQueueSnapshot unknown = new ComputeQueueSnapshot();
        String observedPoolKey = null;
        // Queue facts are independent of training readiness. Never turn a successful read
        // into RUNNING/READY or bypass the original training admission checks.
        if (node.getPowerOperationId() != null || "STOPPED".equals(node.getPowerState())
            || "STARTING".equals(node.getPowerState()) || "STOPPING".equals(node.getPowerState())) return unknown;
        try {
            shared.requireTarget(node);
            String source;
            try (InputStream input = new ClassPathResource("training/gpu_shared_queue_snapshot.py").getInputStream()) {
                source = new String(StreamUtils.copyToByteArray(input), StandardCharsets.UTF_8);
            }
            String encodedSource = Base64.getEncoder().encodeToString(source.getBytes(StandardCharsets.UTF_8));
            try (ComputeSsh.Connection connection = ssh.open(node)) {
                // Metadata only: no Torch import, GPU computation, process lock or health write.
                String uuid = connection.execute("nvidia-smi -i " + node.getGpuIndex() + " --query-gpu=uuid --format=csv,noheader,nounits", 10).trim();
                if (!uuid.matches("GPU-[a-fA-F0-9-]{36}")) return unknown;
                Map<String,Object> gpu = new LinkedHashMap<>();
                gpu.put("gpuIndex", node.getGpuIndex()); gpu.put("gpuUuid", uuid);
                Map<String,Object> payload = new LinkedHashMap<>();
                payload.put("gpu", gpu); payload.put("environment", properties.getEnvironmentId()); payload.put("viewer", shared.viewer());
                String poolKey = poolKey(node, uuid); observedPoolKey = poolKey;
                payload.put("previousTicketKey", estimator.previousKey(poolKey));
                String encodedPayload = Base64.getEncoder().encodeToString(json.writeValueAsBytes(payload));
                // Execute source in memory. Never putText/mv/import the mutable task helper.
                String command = shared.pythonCommand(node) + " -B -c " + quote("import base64;exec(compile(base64.b64decode('" + encodedSource + "'),'<queue-snapshot>','exec'))") + " " + quote(encodedPayload);
                String output = connection.execute(command, 10);
                int marker = output.lastIndexOf("VLS_QUEUE_SNAPSHOT=");
                if (marker < 0) { estimator.unavailable(poolKey); return unknown; }
                JsonNode raw = json.readTree(output.substring(marker + 19).trim());
                if (!uuid.equals(raw.path("gpuUuid").asText())) { estimator.unavailable(poolKey); return unknown; }
                return project(raw, node);
            }
        } catch (IOException | RuntimeException unavailable) {
            if (observedPoolKey != null) estimator.unavailable(observedPoolKey);
            // Never project an unreachable, corrupt or uninitialised registry as an empty queue.
            return unknown;
        }
    }

    protected String authenticatedUser() { return LoginHelper.getUserId(); }

    ComputeQueueSnapshot project(JsonNode raw, ComputeNode node) {
        ComputeQueueSnapshot result = new ComputeQueueSnapshot();
        JsonNode count = raw.path("waitingCount"), checked = raw.path("checkedAt");
        String phase = raw.path("activePhase").isNull() ? null : raw.path("activePhase").asText(null);
        long now = System.currentTimeMillis();
        if (!"KNOWN".equals(raw.path("state").asText()) || !count.isIntegralNumber() || !count.canConvertToInt() || count.asInt() < 0 || count.asInt() > 50000
            || !checked.isIntegralNumber() || !checked.canConvertToLong() || checked.asLong() > now + 5000 || now - checked.asLong() > 30000
            || phase != null && !"RUNNING".equals(phase) && !"ARCHIVING".equals(phase)) return result;
        result.setState("KNOWN"); result.setWaitingCount(count.asInt()); result.setActivePhase(phase); result.setCheckedAt(checked.asLong());
        ComputeQueueWaitEstimator.Estimate estimate = estimator.observe(poolKey(node, raw.path("gpuUuid").asText()), raw);
        if (estimate != null) {
            result.setEstimatedWaitSeconds(estimate.getSeconds()); result.setEstimationSampleCount(estimate.getSamples());
            result.setEstimationBasis("MATCHED_OBSERVED_LEASE_P75");
        }
        String jobId = raw.path("activeJobId").asText("");
        String actor = authenticatedUser();
        if (phase != null && jobId.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}") && actor != null && !actor.trim().isEmpty()) {
            List<Long> ids = nodes.queueIds(node.getId());
            if (ids == null || ids.isEmpty()) return result;
            List<Object> args = new java.util.ArrayList<>();
            args.add(jobId); args.add(ComputeNodeService.tenant()); args.add(actor); args.add(shared.viewer()); args.addAll(ids);
            // Even a forged host response cannot name another tenant/user's task.
            List<String> names = jdbc.queryForList("SELECT t.task_name FROM vls_cloud_training_job j JOIN vls_algorithm_training t ON t.id=j.training_id AND BINARY t.tenant_id=BINARY j.tenant_id "
                + "WHERE j.id=? AND BINARY t.tenant_id=BINARY ? AND BINARY t.create_user=BINARY ? AND BINARY j.public_owner_subject=BINARY ? AND t.is_deleted=0 "
                + "AND j.node_id IN (" + ConfiguredComputeRegistry.placeholders(ids.size()) + ") AND JSON_VALID(j.options_json) "
                + "AND JSON_UNQUOTE(JSON_EXTRACT(j.options_json,'$.sharedQueue.mode'))='SHARED_GPU_QUEUE'", String.class, args.toArray());
            if (names.size() == 1) result.setActiveTaskName(names.get(0));
        }
        return result;
    }

    private static String poolKey(ComputeNode node, String uuid) {
        return node.getHost() + ":" + node.getPort() + "/" + node.getGpuIndex() + "/" + uuid;
    }
}
