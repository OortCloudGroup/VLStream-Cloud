package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import com.ruoyi.vlstream.test.vlstream.service.ModelClassFileService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Official paid instances only. Persists process-exit evidence without claiming an archive
 * succeeded. Pending source files and their original claim remain protected across restarts. */
@Service
public class ComputePaidArchiveRecoveryService {
    public static final String PENDING = "ARCHIVE_PENDING";
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final TransactionTemplate tx;

    public ComputePaidArchiveRecoveryService(JdbcTemplate jdbc, ObjectMapper json, PlatformTransactionManager manager) {
        this.jdbc = jdbc; this.json = json; this.tx = new TransactionTemplate(manager);
    }

    public boolean hasCheckpoint(CloudTrainingJob job) { return parse(job.getOptionsJson()).has("archiveRecovery"); }

    /** Called only after the read-only remote probe obtained the original job's process lock. */
    public void recordExit(CloudTrainingJob job, ComputeNode node, String targetFingerprint, String currentClaimId,
                           String currentOwnerSubject, JsonNode proof, long now) {
        requireOfficial(node); requireProof(proof);
        tx.execute(status -> {
            Map<String, Object> row = job(job.getId());
            if (!same(currentClaimId, text(row, "public_claim_id")) || !same(currentOwnerSubject, text(row, "public_owner_subject"))
                || !same(node.getPublicInstanceKey(), text(row, "public_instance_key")))
                throw new IllegalStateException("Current archive claim changed");
            ObjectNode options = parse(text(row, "options_json"));
            if (options.has("archiveRecovery")) {
                JsonNode existing = checked(row, node, text(row, "tenant_id"), text(row, "public_owner_subject"),
                    text(row, "public_claim_id"), targetFingerprint);
                if (!existing.path("sha256").asText().equals(proof.path("sha256").asText())
                    || existing.path("size").asLong() != proof.path("size").asLong())
                    throw new IllegalStateException("Original archive result changed");
                return null;
            }
            Map<String, Object> run = activeRun(node);
            if (run == null || !"RUNNING".equals(text(run, "state"))) return null;
            requireRunOwner(row, run);
            if (!"ARCHIVING".equals(text(row, "job_state"))) throw new IllegalStateException("Archive exit requires the original archiving job");
            ObjectNode checkpoint = options.putObject("archiveRecovery");
            checkpoint.put("version", 1); checkpoint.put("jobId", text(row, "id"));
            checkpoint.put("tenantId", text(row, "tenant_id")); checkpoint.put("ownerSubject", text(row, "public_owner_subject"));
            checkpoint.put("claimId", text(row, "public_claim_id")); checkpoint.put("nodeId", Long.toString(node.getId()));
            checkpoint.put("instanceKey", node.getPublicInstanceKey()); checkpoint.put("targetFingerprint", targetFingerprint);
            checkpoint.put("runDir", text(row, "run_dir")); checkpoint.put("originalRunId", text(run, "run_id"));
            checkpoint.put("originalUsageId", text(run, "usage_id")); checkpoint.put("sha256", proof.path("sha256").asText());
            checkpoint.put("size", proof.path("size").longValue()); checkpoint.put("exitObservedAtMs", now);
            checkpoint.set("processExit", proof.deepCopy());
            checkpoint.put("checkpointHash", ModelArtifactObjectStore.hash(checkpoint.toString()));
            options.set("archiveAttempt", attempt(run, now));
            saveOptions(text(row, "id"), options);
            return null;
        });
    }

    /** Before SSH: a paused attempt never resumes itself. Only a newly accepted recovery run
     * that explicitly names this original job may resume the archive, never its training. */
    public boolean mayArchive(CloudTrainingJob job, ComputeNode node, String targetFingerprint, long now) {
        requireOfficial(node);
        return Boolean.TRUE.equals(tx.execute(status -> {
            Map<String, Object> row = job(job.getId());
            JsonNode checkpoint = checked(row, node, text(row, "tenant_id"), text(row, "public_owner_subject"),
                text(row, "public_claim_id"), targetFingerprint);
            if (!("ARCHIVING".equals(text(row, "job_state")) || PENDING.equals(text(row, "job_state")))) return false;
            ObjectNode options = parse(text(row, "options_json"));
            JsonNode previous = options.path("archiveAttempt");
            Map<String, Object> run = activeRun(node);
            if (run == null || !"RUNNING".equals(text(run, "state"))) return false;
            requireRunOwner(row, run);
            if (text(run, "run_id").equals(previous.path("runId").asText())) {
                if (previous.path("paused").asBoolean(true)) return false;
                if (now >= previous.path("deadlineMs").asLong(0)
                    && (number(run, "stop_requested") != 0 || now >= stopDeadline(run))) {
                    pause(row, options);
                    return false;
                }
                return true;
            }
            JsonNode quote = parse(text(run, "quote_snapshot"));
            if (!checkpoint.path("originalRunId").asText().equals(quote.path("archive_recovery_source_run_id").asText())
                || !contains(quote.path("archive_recovery_job_ids"), text(row, "id")) || number(run, "stop_requested") != 0
                || now >= stopDeadline(run)) return false;
            options.set("archiveAttempt", attempt(run, now));
            saveOptions(text(row, "id"), options);
            jdbc.update("UPDATE vls_cloud_training_job SET job_state='ARCHIVING',message='按已接受的原任务恢复报价继续归档，未重新训练',update_time=NOW() WHERE id=?", text(row, "id"));
            return true;
        }));
    }

    /** Lifecycle calls this after current owner/claim and stop_requested checks. Its deadline
     * may interrupt an in-flight transfer; the original source and pending journal are retained. */
    public boolean pauseForStop(String jobId, ComputeNode node, String tenantId, String ownerSubject,
                                String claimId, String targetFingerprint, String usageId, long now) {
        requireOfficial(node);
        return Boolean.TRUE.equals(tx.execute(status -> {
            Map<String, Object> row = job(jobId);
            if (!("ARCHIVING".equals(text(row, "job_state")) || PENDING.equals(text(row, "job_state")))) return false;
            if (!parse(text(row, "options_json")).has("archiveRecovery")) return false;
            JsonNode checkpoint = checked(row, node, tenantId, ownerSubject, claimId, targetFingerprint);
            ObjectNode options = parse(text(row, "options_json"));
            JsonNode attempt = options.path("archiveAttempt");
            if (PENDING.equals(text(row, "job_state")) && attempt.path("paused").asBoolean(false)) {
                if (usageId.equals(attempt.path("usageId").asText())) return true;
                Map<String, Object> run = activeRun(node);
                if (run == null || !usageId.equals(text(run, "usage_id"))) return false;
                requireRunOwner(row, run);
                JsonNode quote = parse(text(run, "quote_snapshot"));
                return checkpoint.path("originalRunId").asText().equals(quote.path("archive_recovery_source_run_id").asText())
                    && contains(quote.path("archive_recovery_job_ids"), jobId);
            }
            if (!usageId.equals(attempt.path("usageId").asText()) || !attempt.path("deadlineMs").isIntegralNumber()
                || now < attempt.path("deadlineMs").longValue()) return false;
            pause(row, options);
            return true;
        }));
    }

    /** The only exception to paid-start's active-job gate. Every outstanding job must be a
     * paused, proved original archive belonging to this exact owner, claim and target. */
    public List<String> requireRecoverableJobs(ComputeNode node, List<Long> nodeIds, String tenantId,
                                              String ownerSubject, String claimId, String targetFingerprint) {
        requireOfficial(node);
        if (nodeIds == null || nodeIds.isEmpty()) throw new IllegalArgumentException("Original archive node identity missing");
        return tx.execute(status -> {
            List<String> result = new ArrayList<>();
            for (Map<String, Object> row : jdbc.queryForList("SELECT * FROM vls_cloud_training_job WHERE node_id IN ("
                + ConfiguredComputeRegistry.placeholders(nodeIds.size()) + ") AND job_state NOT IN ('COMPLETED','FAILED','CANCELLED') ORDER BY id FOR UPDATE", nodeIds.toArray())) {
                checked(row, node, tenantId, ownerSubject, claimId, targetFingerprint);
                JsonNode attempt = parse(text(row, "options_json")).path("archiveAttempt");
                if (!PENDING.equals(text(row, "job_state")) || !attempt.path("paused").asBoolean(false))
                    throw new IllegalStateException("Original archive has not paused safely");
                requireStorageRetryReady(row);
                result.add(text(row, "id"));
            }
            return result;
        });
    }

    private void requireStorageRetryReady(Map<String, Object> job) {
        String model = "cloud-training/" + text(job, "node_id") + "/" + text(job, "id") + "/weights/best.pt";
        JsonNode checkpoint = parse(text(job, "options_json")).path("archiveRecovery");
        for (String path : new String[]{model, ModelClassFileService.storagePath(model)}) {
            for (Map<String, Object> row : jdbc.queryForList("SELECT storage_state,retry_after,sha256,file_size FROM vls_model_artifact_storage WHERE tenant_id=? AND path_hash=?",
                text(job, "tenant_id"), ModelArtifactObjectStore.hash(path))) {
                Object retry = row.get("retry_after");
                if (path.equals(model) && "READY".equals(text(row, "storage_state"))
                    && (!checkpoint.path("sha256").asText().equals(text(row, "sha256"))
                    || !Long.toString(checkpoint.path("size").asLong()).equals(text(row, "file_size"))))
                    throw new ServiceException("原归档的已存模型与退出证明不一致，需核对原任务");
                if (!"READY".equals(text(row, "storage_state")) && retry != null
                    && (!(retry instanceof java.util.Date) || ((java.util.Date) retry).getTime() > System.currentTimeMillis()))
                    throw new ServiceException("原归档的存储租约或重试窗口尚未到期，暂不能付费开机恢复");
            }
        }
    }

    private JsonNode checked(Map<String, Object> row, ComputeNode node, String tenant, String owner, String claim, String target) {
        JsonNode checkpoint = parse(text(row, "options_json")).path("archiveRecovery");
        if (!checkpoint.isObject()) throw new IllegalStateException("Original archive exit proof missing");
        ObjectNode canonical = ((ObjectNode) checkpoint).deepCopy(); String hash = canonical.path("checkpointHash").asText(); canonical.remove("checkpointHash");
        if (!hash.equals(ModelArtifactObjectStore.hash(canonical.toString())) || checkpoint.path("version").asInt() != 1
            || !same(tenant, text(row, "tenant_id")) || !same(owner, text(row, "public_owner_subject")) || !same(claim, text(row, "public_claim_id"))
            || !same(text(row, "id"), checkpoint.path("jobId").asText()) || !same(tenant, checkpoint.path("tenantId").asText())
            || !same(owner, checkpoint.path("ownerSubject").asText()) || !same(claim, checkpoint.path("claimId").asText())
            || !Long.toString(node.getId()).equals(checkpoint.path("nodeId").asText())
            || !same(node.getPublicInstanceKey(), checkpoint.path("instanceKey").asText())
            || !same(node.getPublicInstanceKey(), text(row, "public_instance_key")) || !same(target, checkpoint.path("targetFingerprint").asText())
            || !same(text(row, "run_dir"), checkpoint.path("runDir").asText())
            || !checkpoint.path("sha256").asText().equals(checkpoint.path("processExit").path("sha256").asText())
            || checkpoint.path("size").asLong() != checkpoint.path("processExit").path("size").asLong())
            throw new IllegalStateException("Original archive identity/evidence changed");
        requireProof(checkpoint.path("processExit"));
        return checkpoint;
    }

    private ObjectNode attempt(Map<String, Object> run, long now) {
        JsonNode budget = parse(text(run, "quote_snapshot")).path("run_budget");
        long poll = positive(budget.path("polling_safety_seconds"));
        long deadline = Math.min(Math.addExact(now, Math.multiplyExact(poll, 1000L)), stopDeadline(run));
        ObjectNode attempt = json.createObjectNode(); attempt.put("runId", text(run, "run_id"));
        attempt.put("usageId", text(run, "usage_id")); attempt.put("deadlineMs", deadline); attempt.put("paused", false); return attempt;
    }

    private long stopDeadline(Map<String, Object> run) {
        JsonNode budget = parse(text(run, "quote_snapshot")).path("run_budget");
        return Math.subtractExact(number(run, "hold_until_ms"), Math.multiplyExact(Math.addExact(
            positive(budget.path("stop_budget_seconds")), positive(budget.path("polling_safety_seconds"))), 1000L));
    }
    private void pause(Map<String, Object> row, ObjectNode options) {
        ((ObjectNode) options.path("archiveAttempt")).put("paused", true);
        saveOptions(text(row, "id"), options);
        jdbc.update("UPDATE vls_cloud_training_job SET job_state=?,message='训练进程已确认退出；归档等待到期，原磁盘产物保留，等待显式恢复',update_time=NOW() WHERE id=?", PENDING, text(row, "id"));
    }

    private Map<String, Object> activeRun(ComputeNode node) {
        // The immutable accepted budget/identity is a snapshot; do not acquire run/session
        // locks after the job lock. Lifecycle owns the run/session -> job lock order.
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT r.*,s.stop_requested FROM vls_compute_paid_run r JOIN vls_compute_billing_session s ON s.usage_id=r.usage_id WHERE r.active_instance_key=?", node.getPublicInstanceKey());
        if (rows.size() > 1) throw new IllegalStateException("Conflicting paid archive run");
        return rows.isEmpty() ? null : rows.get(0);
    }
    private void requireRunOwner(Map<String, Object> job, Map<String, Object> run) {
        if (!same(text(job, "tenant_id"), text(run, "tenant_id")) || !same(text(job, "public_instance_key"), text(run, "instance_key"))
            || !same(text(job, "public_owner_subject"), ModelArtifactObjectStore.hash("trusted-platform-user\n" + text(run, "platform_subject"))))
            throw new IllegalStateException("Archive run owner changed");
    }
    private static void requireOfficial(ComputeNode node) {
        if (node == null || !node.isConfigurationManaged() || !"autodl".equals(node.getProvider()) || !"PLATFORM".equals(node.getResourceScope()) || !"POINTS".equals(node.getBillingType())
            || !"API".equals(node.getControlMode()) || ComputeSharedQueueService.MODE.equals(node.getSchedulingMode()) || node.getPublicInstanceKey() == null)
            throw new IllegalStateException("Archive recovery is limited to official paid instances");
    }
    private static void requireProof(JsonNode proof) {
        if (proof == null || !proof.isObject() || proof.path("version").asInt() != 1 || !"SUCCEEDED".equals(proof.path("state").asText())
            || !proof.path("processExited").isBoolean() || !proof.path("processExited").booleanValue()
            || !proof.path("sha256").asText().matches("[a-f0-9]{64}") || !proof.path("resultSha256").asText().matches("[a-f0-9]{64}")
            || !proof.path("size").isIntegralNumber() || !proof.path("size").canConvertToLong() || proof.path("size").longValue() < 1)
            throw new IllegalStateException("Original training exit is not proved");
    }
    private static long positive(JsonNode value) {
        if (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 1) throw new IllegalStateException("Accepted archive budget missing");
        return value.longValue();
    }
    private static boolean contains(JsonNode array, String value) { if (array.isArray()) for (JsonNode item : array) if (item.isTextual() && value.equals(item.textValue())) return true; return false; }
    private Map<String, Object> job(String id) { return jdbc.queryForMap("SELECT * FROM vls_cloud_training_job WHERE id=? FOR UPDATE", id); }
    private void saveOptions(String id, ObjectNode options) { jdbc.update("UPDATE vls_cloud_training_job SET options_json=? WHERE id=?", options.toString(), id); }
    private ObjectNode parse(String value) { try { JsonNode node = json.readTree(value); if (node.isObject()) return (ObjectNode) node; } catch (Exception ignored) { } throw new IllegalStateException("Invalid archive journal JSON"); }
    private static String text(Map<String, Object> row, String field) { Object value = row.get(field); return value == null ? null : String.valueOf(value); }
    private static long number(Map<String, Object> row, String field) { return Long.parseLong(text(row, field)); }
    private static boolean same(String first, String second) { return first != null && first.equals(second); }
}
