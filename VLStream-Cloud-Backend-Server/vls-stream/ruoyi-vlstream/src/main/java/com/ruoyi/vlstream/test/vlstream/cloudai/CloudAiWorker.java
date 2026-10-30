package com.ruoyi.vlstream.test.vlstream.cloudai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.compute.*;
import com.ruoyi.vlstream.test.vlstream.service.CloudAiEvidenceStore;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import javax.annotation.PreDestroy;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import static com.ruoyi.vlstream.test.vlstream.compute.ComputeSsh.quote;

/** Durable reconciliation: losing SSH is never evidence that a GPU process ended. */
@Component
@Slf4j
@RequiredArgsConstructor
public class CloudAiWorker {
    private final JdbcTemplate jdbc;
    private final CloudAiService service;
    private final CloudAiSources sources;
    private final CloudAiEvents events;
    private final CloudAiEvidenceStore evidence;
    private final ModelArtifactObjectStore models;
    private final ComputeNodeService nodes;
    private final ConfiguredComputeRegistry configured;
    private final ComputeSharedQueueService queue;
    private final ComputeSsh ssh;
    private final String worker = UUID.randomUUID().toString();
    private final Set<String> active = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "cloud-ai-reconcile"); thread.setDaemon(true); return thread;
    });

    @Scheduled(fixedDelay = 5000, initialDelay = 12000)
    public void scan() {
        long now = System.currentTimeMillis();
        for (String id : active) jdbc.update("UPDATE vls_cloud_ai_run SET lease_until=? WHERE id=? AND lease_owner=?", now + 120000, id, worker);
        if (active.size() >= 4) return;
        List<String> pending = jdbc.queryForList("SELECT id FROM vls_cloud_ai_run WHERE state NOT IN ('COMPLETED','CANCELLED','FAILED','SKIPPED') AND (lease_until IS NULL OR lease_until<?) ORDER BY create_time,id LIMIT 20", String.class, now);
        for (String id : pending) {
            if (active.size() >= 4) break;
            if (active.contains(id) || jdbc.update("UPDATE vls_cloud_ai_run SET lease_owner=?,lease_until=? WHERE id=? AND (lease_until IS NULL OR lease_until<?) AND state NOT IN ('COMPLETED','CANCELLED','FAILED','SKIPPED')", worker, now + 120000, id, now) != 1) continue;
            active.add(id); executor.submit(() -> reconcile(id));
        }
    }

    private void reconcile(String id) {
        String previous = TenantContextHolder.getTenantId();
        Map<String,Object> run = null;
        try {
            run = jdbc.queryForMap("SELECT * FROM vls_cloud_ai_run WHERE id=? AND lease_owner=?", id, worker);
            TenantContextHolder.setTenantId(String.valueOf(run.get("tenant_id")));
            int dispatched = ((Number) run.get("dispatched")).intValue();
            boolean cancel = CloudAiService.flag(run.get("cancel_requested"));
            Long end = run.get("planned_end") == null ? null : ((Number) run.get("planned_end")).longValue();
            if (dispatched < 2 && (cancel || end != null && end <= System.currentTimeMillis())) {
                terminal(id, cancel ? "CANCELLED" : "SKIPPED", cancel ? "在执行前停止" : "排队期间执行窗口已结束", System.currentTimeMillis()); return;
            }
            ObjectNode snapshot = (ObjectNode) service.read(String.valueOf(run.get("snapshot_json")));
            ComputeNode node = nodes.get(((Number) run.get("node_id")).longValue());
            if (!Objects.equals(snapshot.path("resource").path("targetFingerprint").asText(), configured.snapshot(node).get("targetFingerprint"))
                    || !Objects.equals(snapshot.path("managementOwner").asText(""), Objects.toString(node.getManagementOwnerSubject(), "")))
                throw new IOException("RESOURCE_IDENTITY_CHANGED");
            String root = node.getWorkDir() + "/" + ModelArtifactObjectStore.hash(String.valueOf(run.get("tenant_id"))).substring(0,16) + "/" + id;
            if (run.get("run_dir") != null && !root.equals(run.get("run_dir"))) throw new IOException("RUN_DIRECTORY_CHANGED");
            try (ComputeSsh.Connection connection = ssh.open(node)) {
                String python = python(connection, node);
                if (dispatched < 2) {
                    jdbc.update("UPDATE vls_cloud_ai_run SET dispatched=1,run_dir=?,state='PREPARING',message='准备固定模型与视频源' WHERE id=? AND lease_owner=?", root, id, worker);
                    prepare(connection, node, python, root, run, snapshot);
                    // Admission is durable BEFORE the command: a lost reply must be reconciled, never duplicated.
                    if (jdbc.update("UPDATE vls_cloud_ai_run SET dispatched=2 WHERE id=? AND lease_owner=?", id, worker) != 1) return;
                    dispatched = 2;
                }
                boolean authorized = true;
                try {
                    for (JsonNode camera : snapshot.path("cameras")) sources.resolve(camera.path("grantId").asText(), String.valueOf(run.get("tenant_id")), String.valueOf(run.get("actor_id")));
                } catch (RuntimeException error) {
                    authorized = false;
                    jdbc.update("UPDATE vls_cloud_ai_run SET cancel_requested=1,message='视频源授权无法继续核验，正在停止' WHERE id=? AND lease_owner=?", id, worker);
                }
                JsonNode state = command(connection, python, root, cancel || !authorized ? "cancel" : "start", null);
                if(authorized && !cancel && !state.path("exited").asBoolean() && state.path("detail").path("needsSources").asBoolean()) {
                    ArrayNode prepared=(ArrayNode)service.read("[]");
                    for(JsonNode camera:snapshot.path("cameras"))prepared.add(sources.resolve(camera.path("grantId").asText(),String.valueOf(run.get("tenant_id")),String.valueOf(run.get("actor_id")),id));
                    String content=prepared.toString();
                    String checksum=connection.execute("if test -f "+quote(root+"/sources.json")+"; then sha256sum -- "+quote(root+"/sources.json")+"; fi",15);
                    if(!checksum.startsWith(ModelArtifactObjectStore.hash(content)+" "))putAtomic(connection,root+"/sources.json",content);
                }
                archive(connection, python, root, run, snapshot, state);
                String status = state.path("state").asText("UNKNOWN");
                JsonNode detail = state.path("detail");
                jdbc.update("UPDATE vls_cloud_ai_run SET detail_json=?,started_at=COALESCE(started_at,?),heartbeat_at=? WHERE id=? AND lease_owner=?",
                    detail.toString(), nullableLong(detail,"startedAt"), nullableLong(detail,"heartbeatAt"), id, worker);
                if (CloudAiService.TERMINAL.contains(status) && state.path("exited").asBoolean()) {
                    for(JsonNode camera:snapshot.path("cameras"))sources.release(camera.path("grantId").asText(),String.valueOf(run.get("tenant_id")),String.valueOf(run.get("actor_id")),id);
                    if (snapshot.path("shared").asBoolean()) release(connection, python, root, state);
                    terminal(id, status, state.path("message").asText("执行已结束"), state.path("endedAt").asLong(System.currentTimeMillis()));
                } else {
                    if (!Arrays.asList("QUEUED","PREPARING","RUNNING","DEGRADED","STOPPING","UNKNOWN").contains(status)) status = "UNKNOWN";
                    jdbc.update("UPDATE vls_cloud_ai_run SET state=?,message=? WHERE id=? AND lease_owner=?",
                        status, state.path("message").asText("等待执行器确认"), id, worker);
                }
            }
        } catch (Exception error) {
            // No third-party message/stack trace here: SSH / FFmpeg errors can contain credentials.
            Integer admitted = jdbc.queryForObject("SELECT dispatched FROM vls_cloud_ai_run WHERE id=?", Integer.class, id);
            if (admitted != null && admitted >= 2) jdbc.update("UPDATE vls_cloud_ai_run SET state='UNKNOWN',message='执行或证据归档状态待确认，后台将重试；不会重复启动或提前释放GPU' WHERE id=? AND lease_owner=?", id, worker);
            else terminal(id, "FAILED", "准备失败，请核对GPU连接、检测运行环境、模型归档和摄像机授权", System.currentTimeMillis());
            log.warn("Cloud AI reconciliation {}: {}", id, error.getClass().getSimpleName());
        } finally {
            if (previous == null) TenantContextHolder.clear(); else TenantContextHolder.setTenantId(previous);
            jdbc.update("UPDATE vls_cloud_ai_run SET lease_owner=NULL,lease_until=? WHERE id=? AND lease_owner=?", System.currentTimeMillis()+5000, id, worker);
            active.remove(id);
        }
    }

    private String python(ComputeSsh.Connection connection, ComputeNode node) throws IOException {
        String isolated = node.getWorkDir() + "/.cloud-ai-runtime-v1/bin/python";
        String selected = connection.execute("if test -x " + quote(isolated) + "; then printf isolated; else printf base; fi",20).trim();
        return "env -u PYTHONHOME PYTHONPATH='' PYTHONNOUSERSITE=1 " + quote("isolated".equals(selected) ? isolated : node.getPythonPath()) + " -B";
    }
    private void prepare(ComputeSsh.Connection connection, ComputeNode node, String python, String root, Map<String,Object> run, ObjectNode snapshot) throws IOException {
        connection.execute(python + " -c " + quote("import aiortc,aiohttp,av,cv2,torch,ultralytics; assert aiortc.__version__=='1.14.0'; assert ultralytics.__version__=='8.3.240'"),30);
        connection.mkdir(root + "/models");
        connection.execute("chmod 700 -- " + quote(root) + " " + quote(root+"/models"),20);
        for (String script : Arrays.asList("cloud_ai_runner.py","cloud_ai_media.py","camera_rtc_receiver.py","camera_rtc_tls.py","gpu_shared_queue.py")) {
            String resource = ("gpu_shared_queue.py".equals(script) ? "training/" : "cloudai/") + script;
            try (InputStream input = new ClassPathResource(resource).getInputStream()) { putAtomic(connection, root+"/"+script, StreamUtils.copyToString(input,StandardCharsets.UTF_8)); }
        }
        for (JsonNode model : snapshot.path("models")) {
            if (!model.path("modelId").asText().matches("[0-9]{1,20}")) throw new IOException("INVALID_MODEL_ID");
            ModelArtifactObjectStore.StoredArtifact artifact = models.find(model.path("path").asText());
            if (artifact == null || !model.path("sha256").asText().equals(artifact.getSha256()) || model.path("size").asLong() != artifact.getFileSize()) throw new IOException("MODEL_ARCHIVE_CHANGED");
            Path temporary = Files.createTempFile("cloud-ai-model-", ".pt");
            try {
                try (OutputStream output = Files.newOutputStream(temporary)) { models.stream(artifact, output); }
                if (!model.path("sha256").asText().equals(sha(temporary))) throw new IOException("MODEL_CHECKSUM_MISMATCH");
                String target = root + "/models/" + model.path("modelId").asText() + ".pt";
                try (InputStream input = Files.newInputStream(temporary)) { connection.put(target+".tmp",input); }
                connection.execute("chmod 600 -- " + quote(target+".tmp") + " && mv -- " + quote(target+".tmp") + " " + quote(target),20);
            } finally { Files.deleteIfExists(temporary); }
        }
        ObjectNode config = snapshot.deepCopy();
        config.put("kind","CLOUD_AI"); config.put("runId",String.valueOf(run.get("id"))); config.put("gpuIndex",node.getGpuIndex());
        config.put("sourceHandshake",true);
        config.put("plannedEnd",run.get("planned_end") == null ? null : ((Number)run.get("planned_end")).longValue());
        config.put("durationSeconds",run.get("duration_seconds") == null ? null : ((Number)run.get("duration_seconds")).intValue());
        ArrayNode cameras = config.putArray("cameras");
        for (JsonNode camera : snapshot.path("cameras")) cameras.add(sources.resolve(camera.path("grantId").asText(), String.valueOf(run.get("tenant_id")), String.valueOf(run.get("actor_id"))));
        if (snapshot.path("shared").asBoolean()) config.set("sharedQueue", service.read(service.write(queue.configuration(node,String.valueOf(run.get("id")),snapshot.path("queueOwner").asText()))));
        config.put("dispatchedAt",System.currentTimeMillis());
        putAtomic(connection,root+"/job.json",config.toString());
    }
    private void archive(ComputeSsh.Connection connection, String python, String root, Map<String,Object> run, JsonNode snapshot, JsonNode state) throws IOException {
        for (JsonNode entry : state.path("manifest").path("events")) {
            String id = entry.path("id").asText(); if (!id.matches("[a-f0-9-]{36}")) throw new IOException("INVALID_EVENT_ID");
            List<String> archived = jdbc.queryForList("SELECT media_state FROM vls_cloud_ai_event WHERE id=? AND run_id=? AND tenant_id=?",String.class,id,run.get("id"),run.get("tenant_id"));
            if (!archived.isEmpty() && !"PENDING".equals(archived.get(0))) { command(connection,python,root,"ack",id); continue; }
            String directory = root+"/events/"+id;
            Path temporary = Files.createTempFile("cloud-ai-evidence-", ".bin");
            try {
                connection.downloadBounded(directory+"/event.json",temporary,1024*1024);
                JsonNode report = service.read(new String(Files.readAllBytes(temporary),StandardCharsets.UTF_8));
                if (!id.equals(report.path("id").asText())) throw new IOException("EVENT_ID_CHANGED");
                JsonNode camera = CloudAiEvents.member(snapshot.path("cameras"),"grantId",report.path("cameraKey").asText());
                Map<String,String> stored = new LinkedHashMap<>();
                for (String role : Arrays.asList("original","annotated","video")) {
                    JsonNode file = report.path("files").path(role); if (file.isMissingNode()) continue;
                    if ("video".equals(role) && "PENDING".equals(report.path("mediaState").asText())) continue;
                    long maximum = "video".equals(role) ? 256L*1024*1024 : 16L*1024*1024;
                    String name = role + ("video".equals(role) ? ".mp4" : ".jpg");
                    connection.downloadBounded(directory+"/"+name,temporary,maximum);
                    stored.put(role,evidence.archive(temporary,String.valueOf(run.get("id")),id,role,camera.path("deviceId").asText(),file.path("size").asLong(),file.path("sha256").asText()));
                }
                events.ingest(run,report,stored);
                if (!"PENDING".equals(report.path("mediaState").asText())) command(connection,python,root,"ack",id);
            } finally { Files.deleteIfExists(temporary); }
        }
    }
    private JsonNode command(ComputeSsh.Connection connection, String python, String root, String action, String eventId) throws IOException {
        String result = connection.execute(python+" "+quote(root+"/cloud_ai_runner.py")+" "+quote(action)+" "+quote(root+"/job.json")+(eventId==null?"":" "+quote(eventId)),45);
        int marker = result.lastIndexOf("VLS_CLOUD_AI="); if (marker < 0) throw new IOException("RUNNER_REPLY_MISSING");
        return service.read(result.substring(marker+13).trim());
    }
    private void release(ComputeSsh.Connection connection, String python, String root, JsonNode state) throws IOException {
        command(connection,python,root,"release",null);
    }
    private void terminal(String id, String state, String message, long endedAt) {
        jdbc.update("UPDATE vls_cloud_ai_run SET state=?,message=?,ended_at=? WHERE id=? AND lease_owner=?",state,message,endedAt,id,worker);
    }
    private static Long nullableLong(JsonNode value, String key) { return value.path(key).isNumber() ? value.path(key).asLong() : null; }
    private static void putAtomic(ComputeSsh.Connection connection, String path, String value) throws IOException {
        String temporary = path+"."+UUID.randomUUID()+".tmp"; connection.putText(temporary,value);
        connection.execute("chmod 600 -- "+quote(temporary)+" && mv -- "+quote(temporary)+" "+quote(path),20);
    }
    static String sha(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256"); byte[] buffer = new byte[65536];
            try (InputStream input = Files.newInputStream(path)) { int length; while((length=input.read(buffer))!=-1) digest.update(buffer,0,length); }
            StringBuilder value = new StringBuilder(); for(byte b:digest.digest()) value.append(String.format("%02x",b&255)); return value.toString();
        } catch(java.security.NoSuchAlgorithmException error) { throw new IOException(error); }
    }
    @PreDestroy public void shutdown() { executor.shutdown(); }
}
