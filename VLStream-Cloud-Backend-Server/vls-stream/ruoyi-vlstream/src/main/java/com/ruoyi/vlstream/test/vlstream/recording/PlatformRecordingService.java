package com.ruoyi.vlstream.test.vlstream.recording;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.cloudai.CloudAiService;
import com.ruoyi.vlstream.test.vlstream.cloudai.CloudAiSources;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import javax.annotation.PreDestroy;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Owns platform recording only. Preview windows never own or terminate these workers. */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformRecordingService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final CloudAiSources sources;
    private final RecordingDeviceAccess access;
    @Value("${platform.recording.directory:./data/platform-recordings}") private String directory;
    @Value("${platform.recording.python:python}") private String python;
    @Value("${platform.recording.reserve-bytes:1073741824}") private long reserveBytes;
    @Value("${platform.recording.max-workers:8}") private int maxWorkers;
    private final String owner = UUID.randomUUID().toString();
    private final Map<String, Worker> workers = new ConcurrentHashMap<>();
    private final Set<String> busy = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor = Executors.newFixedThreadPool(8, runnable -> {
        Thread thread = new Thread(runnable, "platform-recording"); thread.setDaemon(true); return thread;
    });
    private volatile boolean closing;
    private long runtimeChecked;
    private int cursor;

    public List<Map<String,Object>> list(String protocol, String rowId) {
        access.require(protocol, rowId);
        List<Map<String,Object>> result = jdbc.queryForList("SELECT id,protocol,device_row_id,stream_id,source_json,manual_enabled,plan_json,retention_days,state,error_code,IF(lease_until>=CURRENT_TIMESTAMP,1,0) AS lease_valid FROM vls_platform_record_source WHERE tenant_id=? AND protocol=? AND device_row_id=? ORDER BY stream_id",
            CloudAiService.tenant(), protocol, rowId);
        for (Map<String,Object> item : result) {
            item.put("source", read(String.valueOf(item.remove("source_json"))));
            Object plan = item.remove("plan_json"); item.put("plan", plan == null ? null : read(String.valueOf(plan)));
            Object lease = item.remove("lease_valid");
            if (Arrays.asList("STARTING", "RECORDING", "STOPPING").contains(item.get("state"))
                && (!(lease instanceof Number) || ((Number) lease).intValue() != 1)) item.put("state", "UNKNOWN");
        }
        return result;
    }

    public Map<String,Object> bind(String token) {
        JsonNode source = sources.bind(token);
        String tenant = CloudAiService.tenant(), actor = CloudAiService.actor();
        String id = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO vls_platform_record_source(id,tenant_id,actor_id,protocol,device_row_id,stream_id,grant_id,source_json) VALUES(?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE actor_id=VALUES(actor_id),grant_id=VALUES(grant_id),source_json=VALUES(source_json),updated_at=CURRENT_TIMESTAMP",
            id, tenant, actor, source.path("protocol").asText(), source.path("deviceRowId").asText(),
            source.path("streamId").asText(""), source.path("grantId").asText(), source.toString());
        return jdbc.queryForMap("SELECT id FROM vls_platform_record_source WHERE tenant_id=? AND protocol=? AND device_row_id=? AND stream_id=?",
            tenant, source.path("protocol").asText(), source.path("deviceRowId").asText(), source.path("streamId").asText(""));
    }

    public synchronized void retention(String id, Integer days) {
        owned(id);
        validateRetention(days);
        jdbc.update("UPDATE vls_platform_record_source SET retention_days=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND tenant_id=?", days, id, CloudAiService.tenant());
    }

    static void validateRetention(Integer days) {
        if (days == null || !Arrays.asList(0, 7, 30, 90).contains(days)) throw new ServiceException("录像保留时间仅支持7天、30天、90天或永久", 400);
    }

    static long retentionCutoff(int days, long now) {
        validateRetention(days);
        return days == 0 ? Long.MIN_VALUE : now - TimeUnit.DAYS.toMillis(days);
    }

    public boolean recording(String protocol, String rowId) {
        access.require(protocol, rowId);
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM vls_platform_record_source WHERE tenant_id=? AND protocol=? AND device_row_id=? AND state='RECORDING' AND lease_until>=CURRENT_TIMESTAMP", Integer.class, CloudAiService.tenant(), protocol, rowId);
        return count != null && count > 0;
    }

    public void manual(String id, boolean enabled) {
        Map<String,Object> row = owned(id);
        if (enabled) {
            requireOnline(sources.resolve(String.valueOf(row.get("grant_id")), String.valueOf(row.get("tenant_id")), String.valueOf(row.get("actor_id"))));
            checkRuntime();
        }
        jdbc.update("UPDATE vls_platform_record_source SET manual_enabled=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND tenant_id=?",
            enabled, id, CloudAiService.tenant());
    }

    public void plan(String id, RecordingPlan plan) {
        owned(id); plan.validate(); if (plan.enabled) checkRuntime();
        jdbc.update("UPDATE vls_platform_record_source SET plan_json=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND tenant_id=?",
            stringify(plan), id, CloudAiService.tenant());
    }

    public List<Map<String,Object>> segments(String id, String date) {
        owned(id);
        LocalDate day;
        try { day = LocalDate.parse(date); } catch (RuntimeException invalid) { throw new ServiceException("请选择有效日期"); }
        long start = day.atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
        return jdbc.queryForList("SELECT id,start_ms,end_ms,size_bytes FROM vls_platform_record_segment WHERE source_id=? AND end_ms>? AND start_ms<? ORDER BY start_ms LIMIT 10000", id, Math.max(start, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)), start + 86400000L);
    }

    public Path file(String segmentId) {
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT s.source_id FROM vls_platform_record_segment s JOIN vls_platform_record_source r ON r.id=s.source_id WHERE s.id=? AND r.tenant_id=? AND s.end_ms>?",
            segmentId, CloudAiService.tenant(), System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7));
        if (rows.size() != 1) throw new ServiceException("录像不存在、已过期或无权访问", 404);
        owned(String.valueOf(rows.get(0).get("source_id")));
        Path file = segmentPath(String.valueOf(rows.get(0).get("source_id")), segmentId, ".mp4");
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new ServiceException("录像文件暂不可用", 404);
        return file;
    }

    private Map<String,Object> owned(String id) {
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT * FROM vls_platform_record_source WHERE id=? AND tenant_id=?", id, CloudAiService.tenant());
        if (rows.size() != 1) throw new ServiceException("录像来源不存在或无权访问", 403);
        access.require(String.valueOf(rows.get(0).get("protocol")), String.valueOf(rows.get(0).get("device_row_id")));
        return rows.get(0);
    }

    public synchronized Map<String,Object> runtime() {
        Map<String,Object> result = new LinkedHashMap<>(); result.put("retentionDays", 7);
        result.put("retentionOptions", Arrays.asList(7, 30, 90, 0));
        try { checkRuntime(); result.put("ready", true); }
        catch (ServiceException failure) { result.put("ready", false); result.put("message", failure.getMessage()); }
        return result;
    }

    private synchronized void checkRuntime() {
        try {
            Files.createDirectories(root());
            if (Files.getFileStore(root()).getUsableSpace() < reserveBytes) throw new ServiceException("录像存储空间不足，已停止新增录制");
            if (System.currentTimeMillis() - runtimeChecked < 60000) return;
            Path runtime = root().resolve(".runtime"); Files.createDirectories(runtime);
            for (String resource : Arrays.asList("recording/platform_recorder.py", "cloudai/camera_rtc_receiver.py", "cloudai/camera_rtc_tls.py")) {
                try (InputStream in = new ClassPathResource(resource).getInputStream()) {
                    Files.copy(in, runtime.resolve(resource.substring(resource.lastIndexOf('/') + 1)), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            Process check = new ProcessBuilder(python, runtime.resolve("platform_recorder.py").toString(), "--check")
                .redirectErrorStream(true).redirectOutput(nullDevice()).start();
            if (!check.waitFor(15, TimeUnit.SECONDS)) { check.destroyForcibly(); throw new ServiceException("录像运行环境检查超时"); }
            if (check.exitValue() != 0) throw new ServiceException("后台录像需要 Python 3.10+、PyAV、aiortc 和 aiohttp，请配置录像运行环境");
            runtimeChecked = System.currentTimeMillis();
        } catch (ServiceException failure) { throw failure; }
        catch (Exception failure) { if (failure instanceof InterruptedException) Thread.currentThread().interrupt(); throw new ServiceException("后台录像运行环境不可用，请检查 Python 与存储目录"); }
    }

    private static File nullDevice() { return new File(System.getProperty("os.name").startsWith("Windows") ? "NUL" : "/dev/null"); }
    static void requireOnline(JsonNode source) {
        if ("VLSTREAM".equals(source.path("protocol").asText()) && !source.path("online").asBoolean(false)) {
            throw new ServiceException("设备离线或在线状态无法确认，不能开始录像", 409);
        }
    }
    private Path root() { return Paths.get(directory).toAbsolutePath().normalize(); }
    private static void uuid(String id) { if (id == null || !id.matches("[a-f0-9]{8}(-[a-f0-9]{4}){3}-[a-f0-9]{12}")) throw new ServiceException("录像标识无效"); }
    private Path sourcePath(String id) {
        uuid(id); Path path = root().resolve(id);
        if (Files.isSymbolicLink(path)) throw new ServiceException("录像目录不允许符号链接");
        return path;
    }
    private Path segmentPath(String source, String segment, String extension) { uuid(segment); return sourcePath(source).resolve(segment + extension); }
    private JsonNode read(String value) { try { return json.readTree(value); } catch (Exception error) { throw new ServiceException("录像配置损坏"); } }
    private String stringify(Object value) { try { return json.writeValueAsString(value); } catch (Exception error) { throw new ServiceException("录像配置无效"); } }

    @Scheduled(fixedDelayString = "${platform.recording.poll-ms:5000}")
    public void tick() {
        if (closing) return;
        // Bounded admission: do not queue an unbounded number of camera work items.
        List<Map<String,Object>> rows = jdbc.queryForList("SELECT * FROM vls_platform_record_source WHERE manual_enabled=1 OR plan_json IS NOT NULL OR lease_owner IS NOT NULL ORDER BY id");
        if (!rows.isEmpty()) { Collections.rotate(rows, -(cursor++ % rows.size())); }
        rows.sort(Comparator.comparing(row -> !workers.containsKey(String.valueOf(row.get("id")))));
        for (Map<String,Object> row : rows) {
            String id = String.valueOf(row.get("id"));
            if (busy.size() >= 8 || !busy.add(id)) continue;
            executor.submit(() -> { try { reconcile(row); } finally { busy.remove(id); } });
        }
    }

    private void reconcile(Map<String,Object> row) {
        String id = String.valueOf(row.get("id"));
        Worker worker = workers.get(id);
        try {
            boolean desired = ((Number) row.get("manual_enabled")).intValue() == 1;
            if (row.get("plan_json") != null) desired |= json.readValue(String.valueOf(row.get("plan_json")), RecordingPlan.class)
                .matches(LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
            if (!desired && worker == null && row.get("lease_owner") == null) return;
            int claimed = jdbc.update("UPDATE vls_platform_record_source SET lease_owner=?,lease_until=DATE_ADD(CURRENT_TIMESTAMP,INTERVAL 60 SECOND) WHERE id=? AND (lease_owner=? OR lease_until IS NULL OR lease_until<CURRENT_TIMESTAMP)", owner, id, owner);
            if (claimed != 1) { if (worker != null) { worker.stop(); workers.remove(id); } return; }
            importSegments(id);
            if (!desired || closing) {
                if (worker != null) { state(id, "STOPPING", null); worker.stop(); workers.remove(id); importSegments(id); }
                state(id, "IDLE", null);
                jdbc.update("UPDATE vls_platform_record_source SET lease_owner=NULL,lease_until=NULL WHERE id=? AND lease_owner=?", id, owner);
                return;
            }
            if (worker != null && !worker.process.isAlive()) {
                worker.stop(); workers.remove(id); worker = null;
                state(id, "RETRYING", "SOURCE_UNAVAILABLE");
                // Keep the last failure visible for this interval; the next tick retries.
                return;
            }
            if (worker == null) {
                if (workers.size() >= Math.max(1, Math.min(8, maxWorkers))) { state(id, "WAITING", "CAPACITY"); return; }
                checkRuntime();
                state(id, "STARTING", null);
                String run = "record-" + UUID.randomUUID();
                JsonNode source = sources.resolve(String.valueOf(row.get("grant_id")), String.valueOf(row.get("tenant_id")), String.valueOf(row.get("actor_id")), run);
                try {
                    requireOnline(source);
                    worker = new Worker(row, run, source); workers.put(id, worker);
                } catch (Exception failed) {
                    sources.release(String.valueOf(row.get("grant_id")), String.valueOf(row.get("tenant_id")), String.valueOf(row.get("actor_id")), run); throw failed;
                }
            } else {
                if (System.currentTimeMillis() - worker.lastFrameAt > 50000) throw new ServiceException("录像接收进程未确认收帧");
                worker.send("ping");
                if (System.currentTimeMillis() - worker.resolvedAt > 5000) {
                    requireOnline(sources.resolve(String.valueOf(worker.row.get("grant_id")), String.valueOf(worker.row.get("tenant_id")), String.valueOf(worker.row.get("actor_id")), worker.run));
                    worker.resolvedAt = System.currentTimeMillis();
                    worker.send("ping");
                }
            }
        } catch (Exception failure) {
            if (worker != null) { worker.stop(); workers.remove(id); }
            state(id, "RETRYING", failure instanceof ServiceException && Integer.valueOf(409).equals(((ServiceException) failure).getCode()) ? "DEVICE_OFFLINE" : "SOURCE_OR_RUNTIME_UNAVAILABLE");
            // Never log exceptions from media/source resolution: they may carry credentials.
            log.warn("Platform recording unavailable: sourceId={}", id);
        }
    }

    private void state(String id, String state, String error) {
        jdbc.update("UPDATE vls_platform_record_source SET state=?,error_code=?,updated_at=CURRENT_TIMESTAMP WHERE id=? AND lease_owner=?", state, error, id, owner);
    }

    private synchronized void importSegments(String id) throws IOException {
        Path folder = sourcePath(id); if (!Files.isDirectory(folder)) return;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(folder, "*.json")) {
            for (Path manifest : files) {
                if (Files.isSymbolicLink(manifest) || Files.size(manifest) > 4096) continue;
                JsonNode value = json.readTree(Files.readAllBytes(manifest));
                String segment = value.path("id").asText();
                Path file = segmentPath(id, segment, ".mp4");
                long start = value.path("startMs").asLong(), end = value.path("endMs").asLong();
                if (!manifest.getFileName().toString().equals(segment + ".json") || start <= 0 || end <= start || end - start > 65000
                    || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) continue;
                jdbc.update("INSERT IGNORE INTO vls_platform_record_segment(id,source_id,start_ms,end_ms,size_bytes) VALUES(?,?,?,?,?)", segment, id, start, end, Files.size(file));
                Files.deleteIfExists(manifest);
            }
        }
    }

    @Scheduled(fixedDelayString = "${platform.recording.cleanup-ms:3600000}")
    public synchronized void cleanup() {
        for (Map<String,Object> policy : jdbc.queryForList("SELECT id,retention_days FROM vls_platform_record_source")) {
            String source = String.valueOf(policy.get("id"));
            int days = ((Number) policy.get("retention_days")).intValue();
            if (days == 0) continue;
            long cutoff = retentionCutoff(days, System.currentTimeMillis());
            for (Map<String,Object> segment : jdbc.queryForList("SELECT id FROM vls_platform_record_segment WHERE source_id=? AND end_ms<? LIMIT 10000", source, cutoff)) {
                String id = String.valueOf(segment.get("id"));
                try {
                    Files.deleteIfExists(segmentPath(source, id, ".mp4"));
                    Files.deleteIfExists(segmentPath(source, id, ".json"));
                    jdbc.update("DELETE FROM vls_platform_record_segment WHERE id=? AND source_id=? AND end_ms<?", id, source, cutoff);
                } catch (IOException failure) { log.warn("Expired recording cleanup failed: segmentId={}", id); }
            }
            Path folder = sourcePath(source); if (!Files.isDirectory(folder)) continue;
            // Only abandoned spool files may be removed by mtime. Finalized MP4/JSON
            // files must first be indexed, then evaluated using their recording end time.
            try (DirectoryStream<Path> files = Files.newDirectoryStream(folder)) {
                for (Path file : files) {
                    String name = file.getFileName().toString();
                    if (name.matches("[a-f0-9-]{36}\\.(part|json\\.tmp)") && !Files.isSymbolicLink(file)
                        && Files.getLastModifiedTime(file).toMillis() < cutoff) Files.deleteIfExists(file);
                }
            } catch (IOException failure) { log.warn("Recording spool cleanup failed: sourceId={}", source); }
        }
    }

    private class Worker {
        final Map<String,Object> row;
        final String run;
        final Process process;
        final BufferedWriter input;
        long resolvedAt = System.currentTimeMillis();
        volatile long lastFrameAt = System.currentTimeMillis();
        Worker(Map<String,Object> row, String run, JsonNode source) throws IOException {
            this.row = row; this.run = run;
            String id = String.valueOf(row.get("id"));
            Files.createDirectories(sourcePath(id));
            process = new ProcessBuilder(python, "-u", root().resolve(".runtime/platform_recorder.py").toString()).redirectError(nullDevice()).start();
            input = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            Map<String,Object> config = new LinkedHashMap<>(); config.put("source", source); config.put("directory", sourcePath(id).toString()); config.put("reserveBytes", reserveBytes);
            try { send(stringify(config)); }
            catch (IOException failed) { process.destroyForcibly(); throw failed; }
            Thread output = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        JsonNode event = read(line);
                        if (event.has("segment")) importSegments(id);
                        else if (event.has("error")) state(id, "RETRYING", event.path("error").asText());
                        else if ("RECORDING".equals(event.path("state").asText())) { lastFrameAt = System.currentTimeMillis(); state(id, "RECORDING", null); }
                    }
                } catch (Exception failure) { /* Spool is recovered by the next reconciliation. */ }
            }, "recording-output-" + id);
            output.setDaemon(true); output.start();
        }
        synchronized void send(String value) throws IOException { input.write(value); input.newLine(); input.flush(); }
        void stop() {
            try { send("stop"); input.close(); if (!process.waitFor(10, TimeUnit.SECONDS)) process.destroyForcibly(); }
            catch (Exception failed) { process.destroyForcibly(); if (failed instanceof InterruptedException) Thread.currentThread().interrupt(); }
            try { sources.release(String.valueOf(row.get("grant_id")), String.valueOf(row.get("tenant_id")), String.valueOf(row.get("actor_id")), run); }
            catch (RuntimeException failed) { log.warn("Recording source release pending lease expiry: sourceId={}", row.get("id")); }
        }
    }

    @PreDestroy
    public void close() { closing = true; executor.shutdownNow(); workers.values().forEach(Worker::stop); workers.clear(); }
}
