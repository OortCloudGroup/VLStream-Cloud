package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/** The platform owns supply. An authenticated encrypted snapshot only recovers existing work. */
@Component
@RequiredArgsConstructor
@Slf4j
public class UnifiedComputeCatalog {
    private final ComputeCatalogProperties properties;
    private final ConfiguredComputeRegistry registry;
    private final ComputeCredentialCipher cipher;
    private final ObjectMapper json;
    private volatile long lastAttempt;
    private final java.util.concurrent.atomic.AtomicBoolean refreshing = new java.util.concurrent.atomic.AtomicBoolean();
    private final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor(action -> {
        Thread worker = new Thread(action, "compute-catalog-refresh"); worker.setDaemon(true); return worker;
    });

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void start() {
        if (!properties.configured()) return;
        try {
            Path file = snapshotPath();
            if (Files.isRegularFile(file) && Files.size(file) <= 16000000) {
                JsonNode saved = json.readTree(cipher.decrypt(new String(Files.readAllBytes(file), StandardCharsets.UTF_8)));
                apply(saved, false);
            }
        } catch (Exception invalid) {
            registry.catalogUnavailable();
            log.warn("Compute catalog recovery snapshot unavailable, type={}", invalid.getClass().getSimpleName());
        }
        refreshing.set(true);
        try { refresh(); } finally { refreshing.set(false); }
    }

    @Scheduled(fixedDelay = 1000, initialDelay = 5000)
    public void poll() {
        if (!properties.configured() || System.currentTimeMillis() - lastAttempt < Math.max(1000, properties.getRefreshMillis())
            || !refreshing.compareAndSet(false, true)) return;
        executor.execute(() -> { try { refresh(); } finally { refreshing.set(false); } });
    }

    @javax.annotation.PreDestroy
    public void close() { executor.shutdownNow(); }

    public synchronized void refresh() {
        if (!properties.configured()) return;
        lastAttempt = System.currentTimeMillis();
        try {
            JsonNode document = fetch();
            if (document.path("code").asInt() != 200 || !document.path("data").path("resources").isArray()
                || !document.path("data").path("versions").isArray()) throw new IllegalArgumentException("Invalid execution catalog");
            // Validate and persist before publishing. A restart can always recover the same approved revisions.
            registry.installCatalog(resources(document, "resources"), resources(document, "versions"), true, () -> {
                try { persist(document); }
                catch (Exception failure) { throw new IllegalStateException("Compute catalog snapshot could not be persisted"); }
            });
        } catch (Exception unavailable) {
            registry.catalogUnavailable();
            log.warn("Compute catalog refresh unavailable, type={}", unavailable.getClass().getSimpleName());
        }
    }

    private void apply(JsonNode document, boolean fresh) throws Exception {
        registry.installCatalog(resources(document, "resources"), resources(document, "versions"), fresh);
    }

    private List<ConfiguredComputeProperties.Resource> resources(JsonNode document, String field) throws Exception {
        List<ConfiguredComputeProperties.Resource> result = new ArrayList<>();
        for (JsonNode source : document.path("data").path(field)) {
            ConfiguredComputeProperties.Resource resource = json.treeToValue(source, ConfiguredComputeProperties.Resource.class);
            if (resource.getExecutionRevision() < 1) throw new IllegalArgumentException("Execution revision required");
            resource.setRuntimeResolved(true); resource.setBasePrivateKeyFile(resource.getPrivateKeyFile());
            resource.setBasePublicKeyFingerprint(resource.getPublicKeyFingerprint());
            ConfiguredComputeProperties.EnvironmentRuntime selected = null;
            for (ConfiguredComputeProperties.EnvironmentRuntime runtime : resource.getEnvironmentRuntimes()) {
                if (properties.getEnvironmentId().equals(runtime.getEnvironmentId())) {
                    if (selected != null) throw new IllegalArgumentException("Duplicate execution environment");
                    selected = runtime;
                }
            }
            resource.setEnvironmentAvailable(selected != null);
            if (selected == null) {
                resource.setPythonPath("/unconfigured/python"); resource.setWorkDir("/unconfigured");
            } else {
                resource.setPythonPath(selected.getPythonPath()); resource.setWorkDir(selected.getWorkDir());
                if (selected.getPrivateKeyFile() != null && !selected.getPrivateKeyFile().isEmpty()) resource.setPrivateKeyFile(selected.getPrivateKeyFile());
                if (selected.getPublicKeyFingerprint() != null && !selected.getPublicKeyFingerprint().isEmpty()) resource.setPublicKeyFingerprint(selected.getPublicKeyFingerprint());
            }
            result.add(resource);
        }
        return result;
    }

    private JsonNode fetch() {
        if (blank(properties.getAppId()) || blank(properties.getSecretKey())) throw new IllegalArgumentException("Execution application identity required");
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override protected void prepareConnection(HttpURLConnection connection, String method) throws java.io.IOException {
                super.prepareConnection(connection, method); connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(5000); factory.setReadTimeout(10000);
        RestTemplate rest = new RestTemplate(factory);
        String base = properties.getBaseUrl().replaceAll("/+$", "");
        return rest.execute(base + "/compute/v1/executionCatalog", HttpMethod.POST, request -> {
            request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            request.getHeaders().set("requesttype", "app");
            request.getHeaders().set("appid", properties.getAppId());
            request.getHeaders().set("secretkey", properties.getSecretKey());
            if (!blank(properties.getAccessToken())) request.getHeaders().set("accesstoken", properties.getAccessToken());
            request.getBody().write("{}".getBytes(StandardCharsets.UTF_8));
        }, response -> {
            if (!response.getStatusCode().is2xxSuccessful()) throw new IllegalStateException("Execution catalog refused");
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; int size;
            try (InputStream input = response.getBody()) {
                while ((size = input.read(buffer)) != -1) {
                    if (output.size() + size > 10000000) throw new java.io.IOException("Execution catalog too large");
                    output.write(buffer, 0, size);
                }
            }
            return json.readTree(output.toByteArray());
        });
    }

    private void persist(JsonNode document) throws Exception {
        Path file = snapshotPath(); Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "compute-catalog-", ".tmp");
        try {
            Files.write(temporary, cipher.encrypt(json.writeValueAsString(document)).getBytes(StandardCharsets.UTF_8));
            try { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    private Path snapshotPath() { return Paths.get(properties.getSnapshotFile()).toAbsolutePath().normalize(); }
    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
