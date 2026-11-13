package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
public class UnifiedComputeCatalogTest {
    final ObjectMapper json = new ObjectMapper();
    final ComputeCredentialCipher cipher = new ComputeCredentialCipher("0123456789abcdef0123456789abcdef");
    JdbcTemplate jdbc;
    ComputeCatalogProperties properties;
    ConfiguredComputeRegistry registry;

    @BeforeEach void setup() {
        DriverManagerDataSource source = new DriverManagerDataSource("jdbc:h2:mem:catalog_" + UUID.randomUUID()
            + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE vls_compute_node(id BIGINT,host VARCHAR(253),port INT,gpu_index INT,provider VARCHAR(32),provider_instance_id VARCHAR(100))");
        jdbc.execute("CREATE TABLE vls_cloud_training_job(id VARCHAR(40),node_id BIGINT,options_json TEXT,run_dir TEXT,job_state VARCHAR(32))");
        jdbc.execute("CREATE TABLE vls_cloud_ai_run(node_id BIGINT,snapshot_json TEXT,state VARCHAR(32))");
        jdbc.execute("CREATE TABLE vls_cloud_device_run(node_id BIGINT,snapshot_json TEXT,state VARCHAR(32))");
        jdbc.execute("CREATE TABLE vls_compute_paid_run(node_id BIGINT,quote_snapshot TEXT,active_instance_key VARCHAR(100),usage_id VARCHAR(40))");
        jdbc.execute("CREATE TABLE vls_compute_billing_session(usage_id VARCHAR(40),control_snapshot TEXT)");
        jdbc.execute("CREATE TABLE vls_compute_public_instance(provider VARCHAR(32),provider_instance_id VARCHAR(100),instance_key VARCHAR(100))");
        jdbc.execute("CREATE ALIAS GET_LOCK FOR 'com.ruoyi.vlstream.test.vlstream.compute.UnifiedComputeCatalogTest.acquireLock'");
        jdbc.execute("CREATE ALIAS RELEASE_LOCK FOR 'com.ruoyi.vlstream.test.vlstream.compute.UnifiedComputeCatalogTest.releaseLock'");
        properties = new ComputeCatalogProperties(); properties.setBaseUrl("http://127.0.0.1:1");
        properties.setAppId("catalog-app"); properties.setSecretKey("catalog-app-secret");
        registry = registry();
    }

    public static int acquireLock(String name, int seconds) { return 1; }
    public static int releaseLock(String name) { return 1; }

    ConfiguredComputeRegistry registry() {
        ConfiguredComputeRegistry value = new ConfiguredComputeRegistry(new ConfiguredComputeProperties(), new ComputeTrustedSelfAssetsProperties(), json, jdbc);
        ReflectionTestUtils.setField(value, "catalogProperties", properties);
        ReflectionTestUtils.setField(value, "credentialCipher", cipher); value.initialize(); return value;
    }

    ConfiguredComputeProperties.Resource resource(long revision) throws Exception {
        ConfiguredComputeProperties.Resource value = new ConfiguredComputeProperties.Resource();
        value.setExecutionRevision(revision); value.setNodeId(-300L); value.setCode("company-new-gpu"); value.setAssetId("asset-new-gpu");
        value.setLegacyNodeIds(Collections.singletonList(300L)); value.setName("New company GPU"); value.setGpuModel("A100");
        value.setTenantId("company"); value.setHost("192.168.55.10"); value.setUsername("trainer"); value.setHostKey(hostKey());
        value.setPythonPath("/opt/vls/bin/python"); value.setWorkDir("/work/vls/localhost"); value.setAuthMode("PASSWORD"); value.setPassword("fixture-password");
        value.setQueueRoot("/var/lib/vls-shared");
        ConfiguredComputeProperties.EnvironmentRuntime runtime = new ConfiguredComputeProperties.EnvironmentRuntime();
        runtime.setEnvironmentId("localhost"); runtime.setPythonPath(value.getPythonPath()); runtime.setWorkDir(value.getWorkDir());
        value.setEnvironmentRuntimes(Collections.singletonList(runtime)); return value;
    }

    String hostKey() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream wire = new DataOutputStream(bytes);
        byte[] type = "ssh-ed25519".getBytes(StandardCharsets.US_ASCII); wire.writeInt(type.length); wire.write(type);
        wire.writeInt(32); wire.write(new byte[32]); return Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    ConfiguredComputeProperties.Resource official(long revision) throws Exception {
        ConfiguredComputeProperties.Resource value = resource(revision); value.setProvider("autodl"); value.setNodeId(-6000L);
        value.setCode("official-autodl"); value.setUsername("root"); value.setInstanceId("pro-123456abcdef");
        value.setAccountScope("official-account"); value.setAuthMode("DEPLOYMENT_KEY"); value.setPassword(null);
        value.setHost(null); value.setHostKey(null); value.setProviderApiToken("fixture-provider-token");
        value.setLegacyNodeIds(Collections.emptyList());
        ConfiguredComputeProperties.FixedPrice price = new ConfiguredComputeProperties.FixedPrice();
        price.setPerInstanceHour(new java.math.BigDecimal("13.00")); value.setFixedPrice(price); return value;
    }

    void install(ConfiguredComputeProperties.Resource current, ConfiguredComputeProperties.Resource... history) {
        registry.installCatalog(Collections.singletonList(current), Arrays.asList(history), true);
    }

    void active(ComputeNode node) throws Exception {
        String options = json.createObjectNode().set("configuredResource", json.valueToTree(registry.snapshot(node))).toString();
        jdbc.update("INSERT INTO vls_cloud_training_job VALUES('active',?,?,?,'RUNNING')", node.getId(), options, node.getWorkDir() + "/active");
    }

    @Test void additionalPrivateNetworkMachineUsesApprovedCatalogIdentityAndPasswordWithoutHardcodedHosts() throws Exception {
        ConfiguredComputeProperties.Resource value = resource(1);
        value.setSpecifications(new LinkedHashMap<>()); value.getSpecifications().put("cpuCount", 32); value.getSpecifications().put("password", "never-public");
        install(value, value); ComputeNode node = registry.resolve(-300L);
        assertEquals(-300L, registry.resolve(300L).getId()); assertEquals("fixture-password", cipher.decrypt(node.getPasswordCipher()));
        ComputeTrustedSelfPolicy policy = new ComputeTrustedSelfPolicy(new ComputeTrustedSelfAssetsProperties());
        assertEquals("asset-new-gpu", policy.requireAsset(node).getAssetId());
        ComputeSharedQueueProperties queue = new ComputeSharedQueueProperties(); queue.setEnvironmentId("localhost");
        assertDoesNotThrow(() -> new ComputeSharedQueueService(queue, policy, jdbc, json).requireTarget(node));
        registry.requireAdmission(node);
        String visible = json.writeValueAsString(node);
        assertFalse(visible.contains("fixture-password")); assertFalse(visible.contains("never-public")); assertFalse(visible.contains("catalogDefinition"));
        assertEquals(32, node.getSpecifications().get("cpuCount"));
    }

    @Test void metadataRefreshRetainsHealthButChangedTargetResetsIt() throws Exception {
        ConfiguredComputeProperties.Resource first = resource(1); install(first, first);
        ComputeNode node = registry.resolve(-300L); assertTrue(registry.recordProbe(node, "READY", "{\"ready\":true}", node.getHostKey()));
        ConfiguredComputeProperties.Resource second = resource(2); second.setName("Renamed"); install(second, first, second);
        assertEquals("READY", registry.resolve(-300L).getProbeState()); assertEquals("Renamed", registry.resolve(-300L).getName());
        ConfiguredComputeProperties.Resource third = resource(3); third.setHost("192.168.55.11"); install(third, first, second, third);
        assertEquals("UNTESTED", registry.resolve(-300L).getProbeState());
    }

    @Test void targetChangeWaitsForCommittedTaskAndRestartRecoversOriginalRevision() throws Exception {
        ConfiguredComputeProperties.Resource first = resource(1); install(first, first); ComputeNode old = registry.resolve(-300L); active(old);
        ConfiguredComputeProperties.Resource second = resource(2); second.setHost("192.168.55.11"); install(second, first, second);
        assertEquals(old.getHost(), registry.resolve(-300L).getHost()); assertThrows(RuntimeException.class, () -> registry.requireAdmission(old));
        registry = registry(); install(second, first, second);
        assertEquals(1, registry.resolve(-300L).getConfigurationRevision()); assertEquals(old.getHost(), registry.resolve(-300L).getHost());
        jdbc.update("UPDATE vls_cloud_training_job SET job_state='COMPLETED'"); install(second, first, second);
        assertEquals("192.168.55.11", registry.resolve(-300L).getHost()); registry.requireAdmission(registry.resolve(-300L));
    }

    @Test void deletionAndDisableBlockAdmissionWhileOriginalTargetRemainsAvailable() throws Exception {
        ConfiguredComputeProperties.Resource first = resource(1); install(first, first); ComputeNode old = registry.resolve(-300L); active(old);
        ConfiguredComputeProperties.Resource deleted = resource(2); deleted.setDeleted(true); deleted.setEnabled(false); deleted.setPublished(false);
        install(deleted, first, deleted);
        assertEquals(old.getHost(), registry.resolve(-300L).getHost()); assertThrows(RuntimeException.class, () -> registry.requireAdmission(old));
        registry = registry(); install(deleted, first, deleted); assertEquals(old.getHost(), registry.resolve(-300L).getHost());
        jdbc.update("UPDATE vls_cloud_training_job SET job_state='COMPLETED'"); install(deleted, first, deleted);
        assertTrue(registry.published().isEmpty());
    }

    @Test void catalogFailurePreservesRecoveryAndStopsNewAdmission() throws Exception {
        ConfiguredComputeProperties.Resource value = resource(1); install(value, value); ComputeNode old = registry.resolve(-300L);
        registry.catalogUnavailable(); assertEquals(old.getHost(), registry.resolve(-300L).getHost());
        assertFalse(registry.resolve(-300L).isCatalogAdmissionAllowed()); assertThrows(RuntimeException.class, () -> registry.requireAdmission(old));
        install(value, value); registry.requireAdmission(registry.resolve(-300L));
    }

    @Test void startupWithoutCatalogKeepsReservedIdentitiesUnavailableWithoutDatabaseFallback() {
        assertTrue(registry.contains(-300L));
        com.ruoyi.common.exception.ServiceException unavailable = assertThrows(com.ruoyi.common.exception.ServiceException.class, () -> registry.resolve(-300L));
        assertEquals(ConfiguredComputeRegistry.UNAVAILABLE, unavailable.getCode());
    }

    @Test void failedPersistenceAndMissingActiveRevisionDoNotReplaceTrustedState() throws Exception {
        ConfiguredComputeProperties.Resource first = resource(1); install(first, first); ComputeNode old = registry.resolve(-300L); active(old);
        ConfiguredComputeProperties.Resource second = resource(2); second.setHost("192.168.55.11");
        AtomicBoolean persisted = new AtomicBoolean();
        assertThrows(RuntimeException.class, () -> registry.installCatalog(Collections.singletonList(second), Collections.singletonList(second), true, () -> persisted.set(true)));
        assertFalse(persisted.get()); assertEquals(old.getHost(), registry.resolve(-300L).getHost());
        jdbc.update("UPDATE vls_cloud_training_job SET job_state='COMPLETED'");
        assertThrows(RuntimeException.class, () -> registry.installCatalog(Collections.singletonList(second), Arrays.asList(first, second), true, () -> { throw new IllegalStateException("fixture-disk-full"); }));
        assertEquals(old.getHost(), registry.resolve(-300L).getHost());
    }

    @Test void otherExecutionEnvironmentPinsSharedPhysicalTargetUsingItsOwnRuntimeProfile() throws Exception {
        ConfiguredComputeProperties.Resource first = resource(1);
        ConfiguredComputeProperties.EnvironmentRuntime vm = new ConfiguredComputeProperties.EnvironmentRuntime();
        vm.setEnvironmentId("vm"); vm.setPythonPath(first.getPythonPath()); vm.setWorkDir("/work/vls/vm");
        first.setEnvironmentRuntimes(Arrays.asList(first.getEnvironmentRuntimes().get(0), vm)); install(first, first);
        ComputeNode foreign = registry.resolve(-300L); foreign.setWorkDir(vm.getWorkDir());
        ObjectNode resource = json.valueToTree(registry.snapshot(foreign)); resource.put("executionEnvironment", "vm");
        jdbc.update("INSERT INTO vls_cloud_training_job VALUES('vm-active',-300,?,'/vm/active','RUNNING')",
            json.createObjectNode().set("configuredResource", resource).toString());
        ConfiguredComputeProperties.Resource second = resource(2); second.setHost("192.168.55.11"); install(second, first, second);
        assertEquals("192.168.55.10", registry.resolve(-300L).getHost());
        assertThrows(RuntimeException.class, () -> registry.requireAdmission(registry.resolve(-300L)));
    }

    @Test void newCloudRunsRejectDisabledAndChangedExecutionTargets() throws Exception {
        ConfiguredComputeProperties.Resource first = resource(1); install(first, first);
        ObjectNode frozen = json.valueToTree(registry.snapshot(registry.resolve(-300L)));
        registry.requireRunAdmission(-300L, frozen);
        ConfiguredComputeProperties.Resource changed = resource(2); changed.setHost("192.168.55.11"); install(changed, first, changed);
        assertThrows(RuntimeException.class, () -> registry.requireRunAdmission(-300L, frozen));
        changed.setEnabled(false); install(changed, first, changed);
        assertThrows(RuntimeException.class, () -> registry.requireRunAdmission(-300L, json.valueToTree(registry.snapshot(registry.resolve(-300L)))));
    }

    @Test void staleInstanceCannotAdmitAnotherTargetBesideTheCommittedActiveRevision() throws Exception {
        ConfiguredComputeProperties.Resource first = resource(1); install(first, first);
        ConfiguredComputeRegistry stale = registry(); stale.installCatalog(Collections.singletonList(first), Collections.singletonList(first), true);
        ConfiguredComputeProperties.Resource second = resource(2); second.setHost("192.168.55.11"); install(second, first, second);
        ComputeNode old = stale.resolve(-300L); stale.requireAdmission(old); active(old);
        assertThrows(RuntimeException.class, () -> registry.requireAdmission(registry.resolve(-300L)));
    }

    @Test void credentialRefreshRejectsAPreviouslyReadAdmissionSnapshot() throws Exception {
        ConfiguredComputeProperties.Resource first = resource(1); install(first, first); ComputeNode stale = registry.resolve(-300L);
        ConfiguredComputeProperties.Resource second = resource(2); second.setPassword("rotated-fixture-password"); install(second, first, second);
        assertThrows(RuntimeException.class, () -> registry.requireAdmission(stale)); registry.requireAdmission(registry.resolve(-300L));
    }

    @Test void admissionUsesFreshRowsBeyondTheCallersRepeatableReadSnapshot() throws Exception {
        ConfiguredComputeProperties.Resource first = resource(1); install(first, first);
        ConfiguredComputeRegistry stale = registry(); stale.installCatalog(Collections.singletonList(first), Collections.singletonList(first), true);
        ConfiguredComputeProperties.Resource second = resource(2); second.setHost("192.168.55.11"); install(second, first, second);
        TransactionTemplate transaction = new TransactionTemplate(new DataSourceTransactionManager(jdbc.getDataSource()));
        transaction.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);
        transaction.execute(status -> {
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM vls_cloud_training_job", Integer.class));
            CompletableFuture.runAsync(() -> new TransactionTemplate(new DataSourceTransactionManager(jdbc.getDataSource())).execute(other -> {
                ComputeNode node = stale.resolve(-300L); stale.requireAdmission(node);
                try { active(node); } catch (Exception failure) { throw new RuntimeException(failure); }
                return null;
            })).join();
            assertThrows(RuntimeException.class, () -> registry.requireAdmission(registry.resolve(-300L)));
            return null;
        });
    }

    @Test void failedLockReleaseDestroysConnectionBeforeReturningItToPool() throws Exception {
        javax.sql.DataSource source = org.mockito.Mockito.mock(javax.sql.DataSource.class);
        java.sql.Connection connection = org.mockito.Mockito.mock(java.sql.Connection.class);
        java.sql.PreparedStatement acquire = org.mockito.Mockito.mock(java.sql.PreparedStatement.class);
        java.sql.ResultSet acquired = org.mockito.Mockito.mock(java.sql.ResultSet.class);
        org.mockito.Mockito.when(source.getConnection()).thenReturn(connection);
        org.mockito.Mockito.when(connection.prepareStatement("SELECT GET_LOCK(?,15)")).thenReturn(acquire);
        org.mockito.Mockito.when(acquire.executeQuery()).thenReturn(acquired);
        org.mockito.Mockito.when(acquired.next()).thenReturn(true); org.mockito.Mockito.when(acquired.getInt(1)).thenReturn(1);
        org.mockito.Mockito.when(connection.prepareStatement("SELECT RELEASE_LOCK(?)")).thenThrow(new java.sql.SQLException("fixture-timeout"));
        ComputeCatalogLease.acquire(source).close();
        org.mockito.InOrder order = org.mockito.Mockito.inOrder(connection);
        order.verify(connection).abort(org.mockito.ArgumentMatchers.any()); order.verify(connection).close();
    }

    @Test void legacyAliasesRemainAvailableAfterTheyAreRemovedFromCurrentMetadata() throws Exception {
        ConfiguredComputeProperties.Resource first = resource(1); install(first, first);
        ConfiguredComputeProperties.Resource second = resource(2); second.setLegacyNodeIds(Collections.emptyList()); install(second, first, second);
        assertEquals(-300L, registry.resolve(300L).getId());
    }

    @Test void centralCompanyPublicationBindingsKeepHistoricalIdentityAfterRetirementAndRestart() throws Exception {
        ConfiguredComputeProperties.Resource first = official(1); first.setLegacyNodeIds(Collections.singletonList(792022144331380001L)); install(first, first);
        ConfiguredComputeProperties.Resource second = official(2); second.setInstanceId("pro-abcdef123456"); second.setDeleted(true);
        install(second, first, second);
        assertEquals(Collections.singleton("pro-123456abcdef"), registry.centralizedCompanyOfferBindings().get(792022144331380001L));
        registry.catalogUnavailable();
        assertEquals(Collections.singleton("pro-123456abcdef"), registry.centralizedCompanyOfferBindings().get(792022144331380001L));
        registry = registry(); install(second, first, second);
        assertEquals(Collections.singleton("pro-123456abcdef"), registry.centralizedCompanyOfferBindings().get(792022144331380001L));
    }

    @Test void paidOrderRetainsOriginalInstanceTokenAndPriceDuringChangesAndRestart() throws Exception {
        ConfiguredComputeProperties.Resource first = official(1); install(first, first); ComputeNode old = registry.resolve(-6000L);
        ObjectNode quote = json.createObjectNode().set("configuredResource", json.valueToTree(registry.snapshot(old)));
        jdbc.update("INSERT INTO vls_compute_paid_run VALUES(-6000,?,'original-instance','usage-1')", quote.toString());
        ConfiguredComputeProperties.Resource second = official(2); second.setInstanceId("pro-abcdef123456"); second.setProviderApiToken("new-provider-token");
        second.getFixedPrice().setPerInstanceHour(new java.math.BigDecimal("20.00")); install(second, first, second);
        assertEquals("pro-123456abcdef", registry.resolve(-6000L).getProviderInstanceId());
        assertEquals("fixture-provider-token", registry.providerToken(registry.resolve(-6000L))); assertEquals("13.00", registry.resolve(-6000L).getUserHourlyPrice());
        registry = registry(); install(second, first, second);
        assertEquals("fixture-provider-token", registry.providerToken(registry.resolve(-6000L)));
        assertThrows(RuntimeException.class, () -> registry.requireAdmission(registry.resolve(-6000L)));
    }

    @Test void legacyYamlNullAndCatalogEmptyOptionalFieldsKeepFrozenFingerprint() throws Exception {
        ConfiguredComputeProperties.Resource old = official(1); old.setExecutionRevision(0);
        ConfiguredComputeProperties legacy = new ConfiguredComputeProperties(); legacy.setResources(Collections.singletonList(old));
        ConfiguredComputeRegistry yaml = new ConfiguredComputeRegistry(legacy, new ComputeTrustedSelfAssetsProperties(), json, jdbc); yaml.initialize();
        String frozen = String.valueOf(yaml.snapshot(yaml.resolve(-6000L)).get("targetFingerprint"));
        jdbc.update("INSERT INTO vls_compute_paid_run VALUES(-6000,?,'original-instance','usage-1')",
            "{\"configuredResource\":{\"targetFingerprint\":\"" + frozen + "\"}}");
        ConfiguredComputeProperties.Resource platform = official(1); platform.setPrivateKeyFile(""); platform.setPublicKeyFingerprint(""); platform.setHostKey("");
        install(platform, platform);
        assertEquals(frozen, registry.snapshot(registry.resolve(-6000L)).get("targetFingerprint"));
    }

    @Test void foreignProfileUsesItsOwnKeyReferenceFallbackAcrossEnvironments() throws Exception {
        ConfiguredComputeProperties.Resource original = official(1); original.setExecutionRevision(0);
        original.setPrivateKeyFile(""); original.setPublicKeyFingerprint("");
        original.setPythonPath("/root/vm/bin/python"); original.setWorkDir("/root/vm/work");
        ConfiguredComputeProperties legacy = new ConfiguredComputeProperties(); legacy.setResources(Collections.singletonList(original));
        ConfiguredComputeRegistry yaml = new ConfiguredComputeRegistry(legacy, new ComputeTrustedSelfAssetsProperties(), json, jdbc); yaml.initialize();
        String fingerprint = String.valueOf(yaml.snapshot(yaml.resolve(-6000L)).get("targetFingerprint"));
        ObjectNode frozen = json.createObjectNode().put("targetFingerprint", fingerprint).put("executionRevision", 1).put("executionEnvironment", "vm");
        jdbc.update("INSERT INTO vls_compute_paid_run VALUES(-6000,?,'original-instance','usage-1')", json.createObjectNode().set("configuredResource", frozen).toString());
        ConfiguredComputeProperties.Resource platform = official(1); platform.setPrivateKeyFile(""); platform.setPublicKeyFingerprint("");
        ConfiguredComputeProperties.EnvironmentRuntime vm = new ConfiguredComputeProperties.EnvironmentRuntime();
        vm.setEnvironmentId("vm"); vm.setPythonPath(original.getPythonPath()); vm.setWorkDir(original.getWorkDir());
        platform.setEnvironmentRuntimes(Arrays.asList(platform.getEnvironmentRuntimes().get(0), vm));
        ObjectNode document = document(platform);
        for (String field : Arrays.asList("resources", "versions")) {
            ObjectNode value = (ObjectNode) document.path("data").path(field).get(0);
            value.put("privateKeyFile", "");
            ((ObjectNode) value.path("environmentRuntimes").get(0)).put("privateKeyFile", "C:/fixture/local_key");
            ((ObjectNode) value.path("environmentRuntimes").get(0)).put("publicKeyFingerprint", "SHA256:" + String.join("", Collections.nCopies(43, "a")));
        }
        UnifiedComputeCatalog client = new UnifiedComputeCatalog(properties, registry, cipher, json);
        List<ConfiguredComputeProperties.Resource> current = ReflectionTestUtils.invokeMethod(client, "resources", document, "resources");
        List<ConfiguredComputeProperties.Resource> versions = ReflectionTestUtils.invokeMethod(client, "resources", document, "versions");
        assertDoesNotThrow(() -> registry.installCatalog(current, versions, true));
        assertEquals("C:/fixture/local_key", registry.definition(registry.resolve(-6000L)).getPrivateKeyFile());
    }

    @Test void refreshWaitsForAdmissionTransactionThenRetainsItsCommittedSnapshot() throws Exception {
        ConfiguredComputeProperties.Resource first = resource(1); install(first, first);
        ConfiguredComputeProperties.Resource second = resource(2); second.setHost("192.168.55.11");
        CountDownLatch admitted = new CountDownLatch(1), commit = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> submit = pool.submit(() -> new TransactionTemplate(new DataSourceTransactionManager(jdbc.getDataSource())).execute(status -> {
                ComputeNode node = registry.resolve(-300L); registry.requireAdmission(node); admitted.countDown();
                try { assertTrue(commit.await(5, TimeUnit.SECONDS)); active(node); } catch (Exception failure) { throw new RuntimeException(failure); }
                return null;
            }));
            assertTrue(admitted.await(5, TimeUnit.SECONDS));
            Future<?> refresh = pool.submit(() -> install(second, first, second));
            assertThrows(TimeoutException.class, () -> refresh.get(100, TimeUnit.MILLISECONDS));
            commit.countDown(); submit.get(5, TimeUnit.SECONDS); refresh.get(5, TimeUnit.SECONDS);
            assertEquals("192.168.55.10", registry.resolve(-300L).getHost());
        } finally { commit.countDown(); pool.shutdownNow(); }
    }

    @Test void authenticatedHttpCatalogCachesOnlyEncryptedContentAndRecoversDuringOutage() throws Exception {
        ConfiguredComputeProperties.Resource value = resource(1); ObjectNode document = document(value);
        AtomicBoolean failure = new AtomicBoolean(); AtomicBoolean authenticated = new AtomicBoolean();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/compute/v1/executionCatalog", exchange -> {
            authenticated.set("catalog-app".equals(exchange.getRequestHeaders().getFirst("appid"))
                && "catalog-app-secret".equals(exchange.getRequestHeaders().getFirst("secretkey")));
            byte[] body = document.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(failure.get() ? 503 : 200, body.length); exchange.getResponseBody().write(body); exchange.close();
        }); server.start();
        Path directory = testDirectory(); properties.setSnapshotFile(directory.resolve("catalog.snapshot").toString());
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        try {
            UnifiedComputeCatalog catalog = new UnifiedComputeCatalog(properties, registry, cipher, json); catalog.refresh();
            assertTrue(authenticated.get()); registry.requireAdmission(registry.resolve(-300L));
            String saved = new String(Files.readAllBytes(directory.resolve("catalog.snapshot")), StandardCharsets.UTF_8);
            assertTrue(saved.startsWith("v1:")); assertFalse(saved.contains("fixture-password"));
            failure.set(true); catalog.refresh(); assertFalse(registry.resolve(-300L).isCatalogAdmissionAllowed());
            registry = registry(); new UnifiedComputeCatalog(properties, registry, cipher, json).start();
            assertEquals("192.168.55.10", registry.resolve(-300L).getHost()); assertFalse(registry.resolve(-300L).isCatalogAdmissionAllowed());
        } finally { server.stop(0); }
    }

    @Test void resourceWithoutCurrentEnvironmentDoesNotRejectOtherMachines() throws Exception {
        ConfiguredComputeProperties.Resource first = resource(1), vmOnly = resource(1);
        vmOnly.setCode("vm-only-gpu"); vmOnly.setNodeId(-301L); vmOnly.setAssetId("vm-only-asset"); vmOnly.setHost("192.168.55.11");
        vmOnly.setLegacyNodeIds(Collections.emptyList()); vmOnly.getEnvironmentRuntimes().get(0).setEnvironmentId("vm");
        ObjectNode document = document(first); ((com.fasterxml.jackson.databind.node.ArrayNode) document.path("data").path("resources")).add(executionJson(vmOnly));
        ((com.fasterxml.jackson.databind.node.ArrayNode) document.path("data").path("versions")).add(executionJson(vmOnly));
        properties.setSnapshotFile(testDirectory().resolve("catalog.snapshot").toString());
        Files.write(Paths.get(properties.getSnapshotFile()), cipher.encrypt(document.toString()).getBytes(StandardCharsets.UTF_8));
        new UnifiedComputeCatalog(properties, registry, cipher, json).start();
        assertEquals(2, registry.published().size()); assertEquals("/unconfigured", registry.resolve(-301L).getWorkDir());
        assertFalse(registry.resolve(-301L).isCatalogAdmissionAllowed()); assertEquals("/work/vls/localhost", registry.resolve(-300L).getWorkDir());
    }

    ObjectNode document(ConfiguredComputeProperties.Resource resource) {
        ObjectNode document = json.createObjectNode(); document.put("code", 200); ObjectNode data = document.putObject("data");
        data.putArray("resources").add(executionJson(resource)); data.putArray("versions").add(executionJson(resource)); return document;
    }
    ObjectNode executionJson(ConfiguredComputeProperties.Resource resource) {
        ObjectNode value = json.valueToTree(resource); value.put("password", resource.getPassword()); value.put("providerApiToken", resource.getProviderApiToken()); return value;
    }
    Path testDirectory() throws Exception {
        Path root = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("AGENTS.md"))) root = root.getParent();
        assertNotNull(root); Path directory = root.resolve("codex/compute-platform-catalog-20261113/test-cache/" + UUID.randomUUID());
        Files.createDirectories(directory); return directory;
    }
}
