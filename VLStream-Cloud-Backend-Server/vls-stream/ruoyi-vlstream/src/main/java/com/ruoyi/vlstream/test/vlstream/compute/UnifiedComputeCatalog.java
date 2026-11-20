package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import com.ruoyi.vlstream.test.vlstream.config.ModelHubProperties;
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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/** The platform owns supply. An authenticated encrypted snapshot only recovers existing work. */
@Component
@RequiredArgsConstructor
@Slf4j
public class UnifiedComputeCatalog {
    private static final int APPLICATION_DENIED = 4001;
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
                if (!callerFingerprint().equals(saved.path("callerFingerprint").asText("")))
                    throw new IllegalArgumentException("Catalog snapshot belongs to a different application identity");
                apply(saved.path("catalog"), false);
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
            if (document.path("code").asInt() == APPLICATION_DENIED) {
                revokeAssignment();
                return;
            }
            apply(document, true);
        } catch (Exception unavailable) {
            registry.catalogUnavailable();
            if (unavailable instanceof HttpStatusCodeException) {
                int status = ((HttpStatusCodeException) unavailable).getRawStatusCode();
                if (status == 401 || status == 403) revokeAssignment();
            }
            log.warn("Compute catalog refresh unavailable, type={}", unavailable.getClass().getSimpleName());
        }
    }

    private void revokeAssignment() {
        registry.catalogUnavailable();
        properties.assignEnvironment(null);
        try { persist(json.createObjectNode().put("code", APPLICATION_DENIED)); }
        catch (Exception unavailable) {
            log.warn("Compute catalog revocation snapshot unavailable, type={}", unavailable.getClass().getSimpleName());
        }
    }

    private void apply(JsonNode document, boolean fresh) throws Exception {
        JsonNode data = document.path("data");
        String environment = data.path("executionEnvironment").asText("");
        String appId = data.path("executionAppId").asText("");
        if (document.path("code").asInt() != 200 || !data.path("resources").isArray() || !data.path("versions").isArray()
                || !environment.matches("[A-Za-z0-9_-]{1,64}") || blank(appId)
                || !appId.equals(properties.getDeployment().getAppId())) {
            throw new IllegalArgumentException("Invalid execution catalog assignment");
        }
        String previous = properties.getEnvironmentId();
        if (previous != null && !previous.equals(environment)) throw new IllegalArgumentException("Execution environment changed");
        List<ConfiguredComputeProperties.Resource> current = resources(document, "resources", environment);
        List<ConfiguredComputeProperties.Resource> history = resources(document, "versions", environment);
        properties.assignEnvironment(environment);
        try {
            registry.installCatalog(current, history, fresh, () -> {
                if (fresh) try { persist(document); }
                catch (Exception failure) { throw new IllegalStateException("Compute catalog snapshot could not be persisted", failure); }
            });
        } catch (Exception failure) {
            properties.assignEnvironment(previous);
            throw failure;
        }
    }

    private List<ConfiguredComputeProperties.Resource> resources(JsonNode document, String field, String environment) throws Exception {
        List<ConfiguredComputeProperties.Resource> result = new ArrayList<>();
        for (JsonNode source : document.path("data").path(field)) {
            ConfiguredComputeProperties.Resource resource = json.treeToValue(source, ConfiguredComputeProperties.Resource.class);
            if (resource.getExecutionRevision() < 1) throw new IllegalArgumentException("Execution revision required");
            resource.setRuntimeResolved(true); resource.setBasePrivateKeyFile(resource.getPrivateKeyFile());
            resource.setBasePublicKeyFingerprint(resource.getPublicKeyFingerprint());
            if (resource.getEnvironmentRuntimes().size() != 1)
                throw new IllegalArgumentException("Expected a single authorized execution environment");
            ConfiguredComputeProperties.EnvironmentRuntime selected = resource.getEnvironmentRuntimes().get(0);
            if (!environment.equals(selected.getEnvironmentId()))
                throw new IllegalArgumentException("Resource is outside the assigned execution environment");
            resource.setEnvironmentAvailable(true);
            resource.setPythonPath(selected.getPythonPath()); resource.setWorkDir(selected.getWorkDir());
            if (!blank(selected.getPrivateKeyFile())) resource.setPrivateKeyFile(selected.getPrivateKeyFile());
            if (!blank(selected.getPublicKeyFingerprint())) resource.setPublicKeyFingerprint(selected.getPublicKeyFingerprint());
            result.add(resource);
        }
        return result;
    }

    private JsonNode fetch() {
        ModelHubProperties deployment = properties.getDeployment();
        if (blank(deployment.getAppId()) || blank(deployment.getSecretKey())) throw new IllegalArgumentException("Execution application identity required");
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override protected void prepareConnection(HttpURLConnection connection, String method) throws java.io.IOException {
                super.prepareConnection(connection, method); connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(5000); factory.setReadTimeout(10000);
        RestTemplate rest = new RestTemplate(factory);
        String base = deployment.getBaseUrl().trim().replaceAll("/+$", "");
        return rest.execute(base + "/compute/v1/executionCatalog", HttpMethod.POST, request -> {
            request.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            request.getHeaders().set("requesttype", "app");
            request.getHeaders().set("appid", deployment.getAppId());
            request.getHeaders().set("secretkey", deployment.getSecretKey());
            // The platform gateway validates its access token before deployment checks the service app.
            // Direct deployment connections may omit it; never substitute a logged-in user's token.
            if (!blank(deployment.getGuestToken())) request.getHeaders().set("accesstoken", deployment.getGuestToken());
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
            ObjectNode scoped = json.createObjectNode().put("callerFingerprint", callerFingerprint());
            scoped.set("catalog", document);
            Files.write(temporary, cipher.encrypt(json.writeValueAsString(scoped)).getBytes(StandardCharsets.UTF_8));
            try { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    private String callerFingerprint() {
        ModelHubProperties deployment = properties.getDeployment();
        return ModelArtifactObjectStore.hash(deployment.getBaseUrl().trim().replaceAll("/+$", "")
            + "\n" + deployment.getAppId() + "\n" + deployment.getSecretKey());
    }

    private Path snapshotPath() { return Paths.get(properties.getSnapshotFile()).toAbsolutePath().normalize(); }
    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
