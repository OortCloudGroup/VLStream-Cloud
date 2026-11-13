package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** No INSERT/UPDATE/seed of compute-node rows. Only probe evidence is held in memory. */
@Component
@RequiredArgsConstructor
public class ConfiguredComputeRegistry {
    private final ConfiguredComputeProperties properties;
    private final ComputeTrustedSelfAssetsProperties assets;
    private final ObjectMapper json;
    private final JdbcTemplate jdbc;
    private Map<Long, ComputeNode> resources = new LinkedHashMap<>();
    private Map<Long, Long> aliases = new LinkedHashMap<>();
    private Map<Long, Set<String>> centralizedCompanyOffers = Collections.emptyMap();
    private final Map<Long, Object> monitors = new ConcurrentHashMap<>();
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ComputeCatalogProperties catalogProperties;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private ComputeCredentialCipher credentialCipher;
    private boolean catalogFresh;
    private long catalogCheckedAt;
    private final java.util.concurrent.locks.ReentrantReadWriteLock admissionLock = new java.util.concurrent.locks.ReentrantReadWriteLock(true);
    private final Object admissionTransaction = new Object();

    @PostConstruct
    public synchronized void initialize() {
        resources.clear(); aliases.clear();
        centralizedCompanyOffers = Collections.emptyMap();
        if (catalogProperties != null && catalogProperties.configured()) return;
        Set<String> codes = new HashSet<>(), targets = new HashSet<>();
        if (properties.getResources() == null) throw invalid();
        for (ConfiguredComputeProperties.Resource resource : properties.getResources()) {
            if (resource == null || resource.getNodeId() == null || resource.getNodeId() >= -1
                || resource.getCode() == null || !resource.getCode().matches("[a-z][a-z0-9-]{1,63}")
                || !codes.add(resource.getCode()) || aliases.containsKey(resource.getNodeId())
                || resource.getName() == null || resource.getName().trim().isEmpty() || resource.getName().length() > 100
                || resource.getGpuModel() == null || resource.getGpuModel().trim().isEmpty()
                || resource.getGpuIndex() < 0 || resource.getGpuIndex() > 15 || resource.getMaxConcurrentTasks() != 1
                || !("self".equals(resource.getProvider()) || "autodl".equals(resource.getProvider())))
                throw invalid();
            if("autodl".equals(resource.getProvider())) {
                ComputeNode official=officialNode(resource);
                if(!targets.add("autodl:"+resource.getInstanceId()))throw invalid();
                register(resource,official);continue;
            }
            List<ComputeTrustedSelfAssetsProperties.Asset> matches = new ArrayList<>();
            if (assets.getAssets() != null) for (ComputeTrustedSelfAssetsProperties.Asset asset : assets.getAssets())
                if (asset != null && (resource.getAssetId()!=null && !resource.getAssetId().trim().isEmpty()
                    ? Objects.equals(resource.getAssetId(),asset.getAssetId())
                    : resource.getAssetHost()!=null && Objects.equals(resource.getAssetHost(),asset.getHost()))) matches.add(asset);
            // Uninstalled environments may omit company assets entirely, but cannot advertise readiness.
            if (matches.size() > 1) throw invalid();
            ComputeNode node = new ComputeNode();
            node.setId(resource.getNodeId()); node.setConfigurationManaged(true); node.setConfigurationCode(resource.getCode());
            node.setName(resource.getName()); node.setGpuModel(resource.getGpuModel()); node.setGpuIndex(resource.getGpuIndex());
            node.setProvider("self"); node.setBillingType("FREE"); node.setControlMode("MANUAL");
            node.setSchedulingMode(ComputeSharedQueueService.MODE); node.setResourceScope(resource.isPublished() ? "PLATFORM" : "COMPANY");
            node.setAuthMode(ComputeDeploymentSshIdentity.KEY); node.setPythonPath(resource.getPythonPath()); node.setWorkDir(resource.getWorkDir());
            ComputeNodeService.validatePath(node.getPythonPath()); ComputeNodeService.validatePath(node.getWorkDir());
            if ("/".equals(node.getWorkDir())) throw invalid();
            node.setProbeState("UNTESTED"); node.setPowerState("UNKNOWN"); node.setCapacityState("UNKNOWN");
            node.setEnabled(resource.isEnabled());
            if (matches.size() == 1) {
                ComputeTrustedSelfAssetsProperties.Asset asset = matches.get(0);
                if (!asset.isCompanyPool() || asset.getTenantId() == null || asset.getTenantId().isEmpty()
                    || asset.getHost() == null || asset.getUsername() == null || asset.getHostKey() == null) throw invalid();
                node.setTenantId(asset.getTenantId()); node.setHost(asset.getHost()); node.setPort(asset.getPort());
                node.setUsername(asset.getUsername()); node.setHostKey(asset.getHostKey());
                if (!targets.add(node.getHost() + ":" + node.getPort() + "/" + node.getGpuIndex())) throw invalid();
            }
            if (resource.getExecutionRevision() > 0) {
                node.setAuthMode(resource.getAuthMode());
                if (ComputeDeploymentSshIdentity.PASSWORD.equals(resource.getAuthMode())) {
                    if (resource.getPassword() == null || resource.getPassword().isEmpty() || credentialCipher == null) throw invalid();
                    node.setPasswordCipher(credentialCipher.encrypt(resource.getPassword()));
                } else if (!ComputeDeploymentSshIdentity.KEY.equals(resource.getAuthMode())) throw invalid();
            }
            register(resource,node);
        }
    }

    public synchronized void catalogUnavailable() { catalogFresh = false; }

    private void catalogAdmission(ComputeNode node) {
        if (!node.isPlatformCatalogManaged()) return;
        boolean fresh = catalogFresh && catalogProperties != null
            && System.currentTimeMillis() - catalogCheckedAt <= Math.max(1000, catalogProperties.getMaximumAgeMillis());
        node.setCatalogAdmissionAllowed(node.isCatalogAdmissionAllowed() && fresh);
        if (!node.isCatalogAdmissionAllowed()) node.setLifecycleMessage("统一平台算力目录待同步或配置变更待原任务结束，暂不接受新任务");
    }

    public void requireAdmission(ComputeNode node) {
        if (!node.isPlatformCatalogManaged()) return;
        if (org.springframework.transaction.support.TransactionSynchronizationManager.hasResource(admissionTransaction)) {
            Object held = org.springframework.transaction.support.TransactionSynchronizationManager.getResource(admissionTransaction);
            synchronized (this) { checkAdmission(node, held instanceof ComputeCatalogLease ? (ComputeCatalogLease) held : null); }
            return;
        }
        admissionLock.readLock().lock();
        ComputeCatalogLease lease = null;
        boolean retained = false;
        try {
            lease = catalogLease();
            synchronized (this) { checkAdmission(node, lease); }
            if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
                final ComputeCatalogLease held = lease;
                org.springframework.transaction.support.TransactionSynchronizationManager.bindResource(admissionTransaction, lease == null ? Boolean.TRUE : lease);
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override public void afterCompletion(int status) {
                            org.springframework.transaction.support.TransactionSynchronizationManager.unbindResourceIfPossible(admissionTransaction);
                            try { if (held != null) held.close(); }
                            finally { admissionLock.readLock().unlock(); }
                        }
                    });
                retained = true;
            }
        } finally {
            if (!retained) {
                try { if (lease != null) lease.close(); }
                finally { admissionLock.readLock().unlock(); }
            }
        }
    }

    private ComputeCatalogLease catalogLease() {
        return jdbc.getDataSource() == null ? null : ComputeCatalogLease.acquire(jdbc.getDataSource());
    }

    public ComputeNode requireRunAdmission(Long id, JsonNode frozen) {
        if (!contains(id)) {
            if (catalogProperties != null && catalogProperties.configured() && frozen != null && !frozen.path("code").asText("").isEmpty())
                throw unavailable("统一平台算力原执行目录待同步");
            return null;
        }
        ComputeNode node = resolve(id);
        if (!node.isPlatformCatalogManaged()) return node;
        requireAdmission(node);
        if (frozen == null || !Objects.equals(frozen.path("targetFingerprint").asText(), snapshot(node).get("targetFingerprint")))
            throw unavailable("算力执行目标已变化，请重新保存云端算法配置");
        return node;
    }

    private void checkAdmission(ComputeNode node, ComputeCatalogLease lease) {
        ComputeNode current = resources.get(node.getId());
        if (current == null) throw unavailable("统一平台算力已移除");
        ComputeNode checked = new ComputeNode(); BeanUtils.copyProperties(current, checked); catalogAdmission(checked);
        if (!checked.isCatalogAdmissionAllowed() || !checked.isEnabled() || !"PLATFORM".equals(checked.getResourceScope())
            || !executionIdentity(checked).equals(executionIdentity(node))) throw unavailable("统一平台算力已停用、撤下、待同步或执行配置已变化");
        ActiveIdentity active = activeIdentity(node.getId(), queueIds(node.getId()), lease == null ? jdbc : lease.currentReads());
        if (active.busy && !active.matches(node, this)) throw unavailable("该算力仍有另一执行版本的活动任务，等待原任务结束后再提交");
    }

    public synchronized void validateCatalog(List<ConfiguredComputeProperties.Resource> current,
                                             List<ConfiguredComputeProperties.Resource> versions) {
        buildCatalog(current);
        Map<Long, String> identities = new HashMap<>();
        for (ConfiguredComputeProperties.Resource resource : versions) {
            String previous = identities.putIfAbsent(resource.getNodeId(), resource.getCode());
            if (previous != null && !previous.equals(resource.getCode())) throw invalid();
            buildCatalog(Collections.singletonList(resource));
        }
        for (ConfiguredComputeProperties.Resource resource : current) {
            if (!Objects.equals(identities.get(resource.getNodeId()), resource.getCode())) throw invalid();
            ComputeNode old = resources.get(resource.getNodeId());
            if (old != null && !old.getConfigurationCode().equals(resource.getCode())) throw invalid();
        }
    }

    /** Keep the original execution target until all its durable users have ended. */
    public void installCatalog(List<ConfiguredComputeProperties.Resource> current,
                                            List<ConfiguredComputeProperties.Resource> versions, boolean fresh) {
        installCatalog(current, versions, fresh, () -> { });
    }

    void installCatalog(List<ConfiguredComputeProperties.Resource> current,
                        List<ConfiguredComputeProperties.Resource> versions, boolean fresh, Runnable beforePublish) {
        admissionLock.writeLock().lock();
        try (ComputeCatalogLease ignored = catalogLease()) {
            synchronized (this) { replaceCatalog(current, versions, fresh, beforePublish); }
        } finally { admissionLock.writeLock().unlock(); }
    }

    private void replaceCatalog(List<ConfiguredComputeProperties.Resource> current,
                                List<ConfiguredComputeProperties.Resource> versions, boolean fresh, Runnable beforePublish) {
        validateCatalog(current, versions);
        ConfiguredComputeRegistry next = buildCatalog(current);
        Map<Long, Set<String>> companyBindings = new LinkedHashMap<>();
        for (ConfiguredComputeProperties.Resource version : versions) {
            if (!next.resources.containsKey(version.getNodeId())) throw invalid();
            for (Long alias : version.getLegacyNodeIds()) {
                Long prior = next.aliases.putIfAbsent(alias, version.getNodeId());
                if (prior != null && !prior.equals(version.getNodeId())) throw invalid();
                if ("autodl".equals(version.getProvider()))
                    companyBindings.computeIfAbsent(alias, key -> new LinkedHashSet<>()).add(version.getInstanceId());
            }
        }
        for (ComputeNode proposed : new ArrayList<>(next.resources.values())) {
            ComputeNode previous = resources.get(proposed.getId());
            ActiveIdentity active = activeIdentity(proposed.getId(), next.queueIds(proposed.getId()));
            ComputeNode selected = proposed;
            if (active.busy) {
                if (previous != null && active.matches(previous, this)) {
                    if (versions.stream().noneMatch(version -> previous.getId().equals(version.getNodeId())
                        && previous.getConfigurationRevision() == version.getExecutionRevision()))
                        throw unavailable("统一平台缺少活动算力的持久历史版本，保留原目录");
                    selected = previous;
                }
                else {
                    List<ConfiguredComputeProperties.Resource> ordered = new ArrayList<>(versions);
                    ordered.sort(Comparator.comparingLong(ConfiguredComputeProperties.Resource::getExecutionRevision));
                    selected = null;
                    for (ConfiguredComputeProperties.Resource version : ordered) {
                        if (!proposed.getId().equals(version.getNodeId())) continue;
                        ConfiguredComputeRegistry historical = buildCatalog(Collections.singletonList(version));
                        ComputeNode candidate = historical.resources.get(proposed.getId());
                        if (active.matches(candidate, historical)) { selected = candidate; break; }
                    }
                    if (selected == null) throw unavailable("统一平台缺少活动任务的原执行版本，保留原目录等待核对");
                }
                ComputeNode retained = new ComputeNode(); BeanUtils.copyProperties(selected, retained); selected = retained;
                boolean pending = !executionIdentity(selected).equals(next.executionIdentity(proposed));
                selected.setCatalogAdmissionAllowed(!pending && proposed.getCatalogDefinition().isEnvironmentAvailable() && proposed.isEnabled()
                    && "PLATFORM".equals(proposed.getResourceScope()) && !proposed.getCatalogDefinition().isDeleted());
                // Visibility and metadata may change while control of the original instance remains available.
                selected.setName(proposed.getName()); selected.setSpecifications(proposed.getSpecifications());
            } else if (previous != null && executionIdentity(previous).equals(next.executionIdentity(proposed))) {
                copyEvidence(previous, proposed);
            }
            next.resources.put(proposed.getId(), selected);
        }
        // Omitting a prior record from a malformed response cannot silently abandon its work.
        for (ComputeNode previous : resources.values()) if (!next.resources.containsKey(previous.getId())) {
            if (activeIdentity(previous.getId(), queueIds(previous.getId())).busy)
                throw unavailable("统一平台目录缺少活动算力的删除标记和历史版本");
        }
        beforePublish.run();
        resources = next.resources; aliases = next.aliases;
        Map<Long, Set<String>> bindings = new LinkedHashMap<>();
        companyBindings.forEach((id, instances) -> bindings.put(id, Collections.unmodifiableSet(instances)));
        centralizedCompanyOffers = Collections.unmodifiableMap(bindings);
        catalogFresh = fresh; catalogCheckedAt = fresh ? System.currentTimeMillis() : 0;
    }

    public synchronized Map<Long, Set<String>> centralizedCompanyOfferBindings() { return centralizedCompanyOffers; }

    private ConfiguredComputeRegistry buildCatalog(List<ConfiguredComputeProperties.Resource> definitions) {
        List<ConfiguredComputeProperties.Resource> copies = new ArrayList<>();
        for (ConfiguredComputeProperties.Resource definition : definitions) copies.add(copyDefinition(definition));
        ConfiguredComputeProperties source = new ConfiguredComputeProperties(); source.setResources(copies);
        ComputeTrustedSelfAssetsProperties trusted = new ComputeTrustedSelfAssetsProperties();
        Map<String, ComputeTrustedSelfAssetsProperties.Asset> unique = new LinkedHashMap<>();
        for (ConfiguredComputeProperties.Resource resource : copies) {
            if (resource == null || resource.getExecutionRevision() < 1) throw invalid();
            if (!"self".equals(resource.getProvider())) continue;
            ComputeNodeService.validatePath(resource.getQueueRoot());
            if ("/".equals(resource.getQueueRoot())) throw invalid();
            if (resource.getAssetId() == null || resource.getAssetId().isEmpty() || !resource.isCompanyPool()
                || resource.getHost() == null || !resource.getHost().matches("[a-zA-Z0-9][a-zA-Z0-9.-]{0,252}")
                || resource.getHost().contains("..") || resource.getPort() < 1 || resource.getPort() > 65535
                || resource.getUsername() == null || !resource.getUsername().matches("[a-zA-Z_][a-zA-Z0-9_-]{0,63}")
                || resource.getTenantId() == null || resource.getTenantId().isEmpty() || resource.getHostKey() == null) throw invalid();
            ComputeTrustedSelfAssetsProperties.Asset asset = new ComputeTrustedSelfAssetsProperties.Asset();
            asset.setAssetId(resource.getAssetId()); asset.setCompanyPool(true); asset.setTenantId(resource.getTenantId());
            asset.setHost(resource.getHost()); asset.setPort(resource.getPort()); asset.setUsername(resource.getUsername()); asset.setHostKey(resource.getHostKey());
            ComputeTrustedSelfAssetsProperties.Asset existing = unique.putIfAbsent(asset.getAssetId(), asset);
            if (existing != null && !existing.equals(asset)) throw invalid();
        }
        trusted.setAssets(new ArrayList<>(unique.values()));
        ConfiguredComputeRegistry candidate = new ConfiguredComputeRegistry(source, trusted, json, jdbc);
        candidate.credentialCipher = credentialCipher; candidate.initialize();
        ComputeTrustedSelfPolicy policy = new ComputeTrustedSelfPolicy(trusted);
        for (ComputeNode node : candidate.resources.values()) if ("self".equals(node.getProvider())) try { policy.requireAsset(node); }
        catch (IOException invalid) { throw invalid(); }
        return candidate;
    }

    private ConfiguredComputeProperties.Resource copyDefinition(ConfiguredComputeProperties.Resource original) {
        if (original == null) throw invalid();
        ConfiguredComputeProperties.Resource copy = new ConfiguredComputeProperties.Resource(); BeanUtils.copyProperties(original, copy);
        if (original.getLegacyNodeIds() == null) throw invalid();
        copy.setLegacyNodeIds(new ArrayList<>(original.getLegacyNodeIds()));
        if (original.getFixedPrice() != null) {
            ConfiguredComputeProperties.FixedPrice price = new ConfiguredComputeProperties.FixedPrice();
            BeanUtils.copyProperties(original.getFixedPrice(), price); copy.setFixedPrice(price);
        }
        return copy;
    }

    private ActiveIdentity activeIdentity(Long id, List<Long> ids) {
        return activeIdentity(id, ids, jdbc);
    }

    private ActiveIdentity activeIdentity(Long id, List<Long> ids, JdbcTemplate reads) {
        ActiveIdentity active = new ActiveIdentity();
        String where = "node_id IN (" + placeholders(ids.size()) + ")";
        for (Map<String, Object> row : reads.queryForList("SELECT options_json FROM vls_cloud_training_job WHERE " + where
            + " AND job_state NOT IN ('COMPLETED','FAILED','CANCELLED')", ids.toArray())) active.add(row.get("options_json"), "configuredResource");
        for (String table : Arrays.asList("vls_cloud_ai_run", "vls_cloud_device_run"))
            for (Map<String, Object> row : reads.queryForList("SELECT snapshot_json FROM " + table + " WHERE " + where
                + " AND state NOT IN ('COMPLETED','FAILED','CANCELLED','SKIPPED')", ids.toArray())) active.add(row.get("snapshot_json"), "resource");
        for (Map<String, Object> row : reads.queryForList("SELECT r.quote_snapshot,s.control_snapshot FROM vls_compute_paid_run r LEFT JOIN vls_compute_billing_session s ON s.usage_id=r.usage_id WHERE r.node_id IN ("
            + placeholders(ids.size()) + ") AND r.active_instance_key IS NOT NULL", ids.toArray())) {
            if (active.add(row.get("quote_snapshot"), "configuredResource") && row.get("control_snapshot") != null)
                active.add(row.get("control_snapshot"), "");
        }
        return active;
    }

    private class ActiveIdentity {
        boolean busy;
        final Set<String> fingerprints = new HashSet<>();
        final Set<Long> revisions = new HashSet<>();
        final Map<String, Set<String>> foreignFingerprints = new HashMap<>();
        boolean add(Object text, String field) {
            if (text == null) { busy = true; return true; }
            try {
                JsonNode value = json.readTree(String.valueOf(text));
                String environment = value.path(field).path("executionEnvironment").asText(value.path("sharedQueue").path("environment").asText(""));
                String scope = value.path("executionScope").asText("");
                if (environment.isEmpty() && scope.contains(":")) environment = scope.substring(0, scope.indexOf(':'));
                boolean local = environment.isEmpty() || catalogProperties == null || environment.equals(catalogProperties.getEnvironmentId());
                busy = true;
                if (!field.isEmpty()) value = value.path(field);
                String fingerprint = value.path("targetFingerprint").asText(value.path("configured_target").asText(""));
                if (!fingerprint.isEmpty()) {
                    if (local) fingerprints.add(fingerprint);
                    else foreignFingerprints.computeIfAbsent(environment, key -> new HashSet<>()).add(fingerprint);
                }
                if (value.path("executionRevision").asLong() > 0) revisions.add(value.path("executionRevision").asLong());
                return local;
            } catch (IOException invalid) { throw unavailable("活动算力任务的冻结身份无效"); }
        }
        boolean matches(ComputeNode node, ConfiguredComputeRegistry registry) {
            if (!revisions.isEmpty() && (revisions.size() != 1 || !revisions.contains(node.getConfigurationRevision()))) return false;
            if (!fingerprints.isEmpty()) {
                if (fingerprints.size() != 1) return false;
                String expected = fingerprints.iterator().next();
                if (!expected.equals(registry.fingerprint(node)) && !registry.matchLegacyEmptyFingerprint(node, expected)) return false;
            }
            for (Map.Entry<String, Set<String>> foreign : foreignFingerprints.entrySet()) {
                if (foreign.getValue().size() != 1 || !registry.matchesEnvironmentFingerprint(node, foreign.getKey(), foreign.getValue().iterator().next())) return false;
            }
            return true;
        }
    }

    private boolean matchesEnvironmentFingerprint(ComputeNode local, String environment, String expected) {
        for (ConfiguredComputeProperties.EnvironmentRuntime runtime : local.getCatalogDefinition().getEnvironmentRuntimes()) {
            if (!environment.equals(runtime.getEnvironmentId())) continue;
            ComputeNode foreign = new ComputeNode(); BeanUtils.copyProperties(local, foreign);
            ConfiguredComputeProperties.Resource source = copyDefinition(local.getCatalogDefinition()); foreign.setCatalogDefinition(source);
            foreign.setPythonPath(runtime.getPythonPath()); foreign.setWorkDir(runtime.getWorkDir());
            if (source.isRuntimeResolved()) {
                source.setPrivateKeyFile(source.getBasePrivateKeyFile()); source.setPublicKeyFingerprint(source.getBasePublicKeyFingerprint());
            }
            if (runtime.getPrivateKeyFile() != null && !runtime.getPrivateKeyFile().isEmpty()) source.setPrivateKeyFile(runtime.getPrivateKeyFile());
            if (runtime.getPublicKeyFingerprint() != null && !runtime.getPublicKeyFingerprint().isEmpty()) source.setPublicKeyFingerprint(runtime.getPublicKeyFingerprint());
            return expected.equals(fingerprint(foreign)) || matchLegacyEmptyFingerprint(foreign, expected);
        }
        return false;
    }

    /** JSON string fields may represent an unconfigured YAML value as either null or empty. */
    private boolean matchLegacyEmptyFingerprint(ComputeNode node, String expected) {
        if (!official(node)) return false;
        ConfiguredComputeProperties.Resource source = node.getCatalogDefinition();
        String[] original = {node.getHostKey(), source.getPrivateKeyFile(), source.getPublicKeyFingerprint(), source.getProviderApiToken()};
        for (int mask = 0; mask < 16; mask++) {
            String[] values = original.clone();
            for (int index = 0; index < values.length; index++) if (values[index] == null || values[index].isEmpty())
                values[index] = (mask & (1 << index)) == 0 ? null : "";
            node.setHostKey(values[0]); source.setHostKey(values[0]); source.setPrivateKeyFile(values[1]);
            source.setPublicKeyFingerprint(values[2]); source.setProviderApiToken(values[3]);
            if (expected.equals(fingerprint(node))) return true;
        }
        node.setHostKey(original[0]); source.setHostKey(original[0]); source.setPrivateKeyFile(original[1]);
        source.setPublicKeyFingerprint(original[2]); source.setProviderApiToken(original[3]); return false;
    }

    private String executionIdentity(ComputeNode node) {
        ConfiguredComputeProperties.Resource source = node.getCatalogDefinition();
        return ModelArtifactObjectStore.hash(fingerprint(node) + "\n" + source.getAuthMode() + "\n" + source.getPassword() + "\n"
            + source.getPrivateKeyFile() + "\n" + source.getPublicKeyFingerprint() + "\n" + source.getQueueRoot());
    }

    private Map<String, Object> specifications(Map<String, Object> source, boolean platform) {
        if (source == null) return platform ? Collections.emptyMap() : null;
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : Arrays.asList("gpuLabel", "shared", "cpu", "cpuCount", "cpuUnit", "cpuModel", "memory",
            "systemDisk", "dataDisk", "dataDiskShared", "driver", "cuda", "cudaMax", "cuda_max", "source", "observedDate", "observed_date", "recordedAt", "recorded_at", "note")) {
            Object value = source.get(key);
            if (value instanceof Number || value instanceof Boolean) result.put(key, value);
            else if (value instanceof String && ((String) value).length() <= 500) result.put(key, value);
        }
        return Collections.unmodifiableMap(result);
    }

    private void copyEvidence(ComputeNode old, ComputeNode next) {
        next.setProbeState(old.getProbeState()); next.setProbeJson(old.getProbeJson()); next.setProbeTime(old.getProbeTime());
        next.setProbeRequestedAt(old.getProbeRequestedAt()); next.setPowerState(old.getPowerState()); next.setPowerCheckedAt(old.getPowerCheckedAt());
        next.setCapacityState(old.getCapacityState()); next.setCapacitySource(old.getCapacitySource()); next.setCapacityCheckedAt(old.getCapacityCheckedAt());
        next.setPowerOperationId(old.getPowerOperationId()); next.setPowerOperationAction(old.getPowerOperationAction());
        next.setPowerOperationSubmitted(old.isPowerOperationSubmitted()); next.setPowerOperationStartedAt(old.getPowerOperationStartedAt());
        next.setActualStoppedMode(old.getActualStoppedMode()); next.setProviderChargeType(old.getProviderChargeType()); next.setProviderGpuAmount(old.getProviderGpuAmount());
    }

    private void register(ConfiguredComputeProperties.Resource resource,ComputeNode node) {
        node.setCatalogDefinition(resource);
        node.setPlatformCatalogManaged(resource.getExecutionRevision() > 0);
        node.setCatalogAdmissionAllowed(resource.isEnvironmentAvailable());
        node.setConfigurationRevision(resource.getExecutionRevision());
        node.setSpecifications(specifications(resource.getSpecifications(), node.isPlatformCatalogManaged()));
        if (resource.isDeleted()) node.setEnabled(false);
        resources.put(node.getId(),node);aliases.put(node.getId(),node.getId());
        if(resource.getLegacyNodeIds()==null)throw invalid();
        for(Long legacy:resource.getLegacyNodeIds())if(legacy==null||legacy<=0||aliases.putIfAbsent(legacy,node.getId())!=null)throw invalid();
    }
    private ComputeNode officialNode(ConfiguredComputeProperties.Resource resource) {
        if(resource.getInstanceId()==null||!resource.getInstanceId().matches("pro-[a-f0-9]{12}")
            ||resource.getAccountScope()==null||!resource.getAccountScope().matches("[A-Za-z0-9_.:-]{1,128}")
            ||resource.getTenantId()==null||resource.getTenantId().trim().isEmpty()||resource.getPort()<1||resource.getPort()>65535
            ||!"root".equals(resource.getUsername()))throw invalid();
        ComputeNode node=new ComputeNode();node.setId(resource.getNodeId());node.setConfigurationManaged(true);node.setConfigurationCode(resource.getCode());
        node.setProvider("autodl");node.setProviderProduct("AUTODL_PRO");node.setProviderInstanceId(resource.getInstanceId());
        node.setPublicInstanceKey(ModelArtifactObjectStore.hash("autodl\n"+resource.getAccountScope()+"\n"+resource.getInstanceId()));
        node.setTenantId(resource.getTenantId());node.setName(resource.getName());node.setGpuModel(resource.getGpuModel());node.setGpuIndex(resource.getGpuIndex());
        node.setHost(empty(resource.getHost())?null:resource.getHost());node.setPort(resource.getPort());node.setUsername(resource.getUsername());node.setHostKey(resource.getHostKey());
        node.setPythonPath(resource.getPythonPath());node.setWorkDir(resource.getWorkDir());ComputeNodeService.validatePath(node.getPythonPath());ComputeNodeService.validatePath(node.getWorkDir());
        if("/".equals(node.getWorkDir()))throw invalid();
        node.setAuthMode(ComputeDeploymentSshIdentity.KEY);node.setBillingType("POINTS");node.setControlMode("API");node.setSchedulingMode("INSTANCE_EXCLUSIVE");
        node.setResourceScope(resource.isPublished()?"PLATFORM":"COMPANY");node.setEnabled(resource.isEnabled());
        node.setPowerState("UNKNOWN");node.setProbeState("UNTESTED");node.setCapacityState("UNKNOWN");
        ConfiguredComputeProperties.FixedPrice price=resource.getFixedPrice();
        if(price!=null&&price.getPerInstanceHour()!=null) {
            if(price.getCurrency()==null||!price.getCurrency().matches("[A-Z]{3}")||price.getPerInstanceHour().signum()<=0||price.getPerInstanceHour().scale()>12)throw invalid();
            node.setUserHourlyPrice(price.getPerInstanceHour().toPlainString());node.setPriceCurrency(price.getCurrency());node.setPriceUnit("PER_INSTANCE_HOUR");node.setPricePolicy("VLS_FIXED");
        }
        return node;
    }
    private static boolean empty(String value){return value==null||value.trim().isEmpty();}
    synchronized boolean official(ComputeNode node){return node!=null&&node.isConfigurationManaged()&&"autodl".equals(node.getProvider())&&resources.containsKey(node.getId());}
    synchronized ConfiguredComputeProperties.Resource definition(ComputeNode node) {
        if(!official(node))throw unavailable("官方算力配置身份未匹配");
        if (node.getCatalogDefinition() != null) return node.getCatalogDefinition();
        return properties.getResources().stream().filter(r->node.getId().equals(r.getNodeId())&&node.getConfigurationCode().equals(r.getCode())).findFirst().orElseThrow(this::invalid);
    }
    synchronized void verifyOfficial(ComputeNode node){if(!official(node)||!fingerprint(resources.get(node.getId())).equals(fingerprint(node)))throw unavailable("官方实例配置身份已变化");}
    synchronized boolean providerTokenPresent(ComputeNode node){if(!official(node))return false;verifyOfficial(node);return !empty(definition(node).getProviderApiToken());}
    synchronized String providerToken(ComputeNode node){verifyOfficial(node);String token=definition(node).getProviderApiToken();if(empty(token)||token.contains("\r")||token.contains("\n"))throw unavailable("官方电源调用身份尚未配置");return token;}
    synchronized ComputeBillingRate fixedRate(ComputeNode node,JsonNode config) {
        if(config==null)throw unavailable("平台额度换算尚未确认");
        verifyOfficial(node);ConfiguredComputeProperties.Resource resource=definition(node);ConfiguredComputeProperties.FixedPrice price=resource.getFixedPrice();
        if(price==null||price.getPerInstanceHour()==null)throw unavailable("官方算力用户价格尚未配置");
        ComputeBillingRate rate=new ComputeBillingRate();rate.setPricePolicy("VLS_FIXED");rate.setUserPriceSource("OFFICIAL_CONFIG");rate.setFixedPriceConfigurationId(resource.getCode());
        rate.setFixedPriceUnit("PER_INSTANCE_HOUR");rate.setFixedHourlyPrice(price.getPerInstanceHour());rate.setCurrency(price.getCurrency());
        rate.setVersion(ModelArtifactObjectStore.hash(resource.getCode()+"\n"+price.getCurrency()+"\n"+price.getPerInstanceHour().toPlainString()));
        rate.setQuotaUnit(config.path("quota_unit").asText());rate.setConversionVersion(config.path("conversion_version").asText());
        try {rate.setQuotaPerCurrencyUnit(new java.math.BigDecimal(config.path("quota_per_currency_unit").path("numerator").asText()).divide(new java.math.BigDecimal(config.path("quota_per_currency_unit").path("denominator").asText()),java.math.MathContext.DECIMAL128));}
        catch(RuntimeException invalid){throw unavailable("平台额度换算尚未确认");}
        rate.setQuotaScale(0);rate.setQuotaRounding(java.math.RoundingMode.DOWN);rate.setValidFrom(config.path("effective_at").asText());rate.setValidUntil(config.path("valid_until").asText());return rate;
    }
    synchronized String officialBaseModel(String reference,String type,String owner) {
        if(reference.startsWith("@preset/"))return reference;
        for(Map<String,Object> row:jdbc.queryForList("SELECT options_json,annotation_type FROM vls_cloud_training_job WHERE BINARY tenant_id=BINARY ? AND BINARY public_owner_subject=BINARY ? AND job_state='COMPLETED' AND BINARY CONCAT('cloud-training/',node_id,'/',id,'/weights/best.pt')=BINARY ?",ModelArtifactObjectStore.tenant(),owner,reference))try {
            JsonNode source=json.readTree(String.valueOf(row.get("options_json"))).path("configuredResource");
            if(type.equals(row.get("annotation_type"))&&resources.values().stream().anyMatch(r->"autodl".equals(r.getProvider())&&r.getConfigurationCode().equals(source.path("code").asText()))
                &&source.path("targetFingerprint").asText().matches("[a-f0-9]{64}"))return reference;
        }catch(IOException invalid){throw unavailable("归档模型来源无法核对");}
        throw unavailable("官方算力仅接受官方基础模型或本人已验证的官方训练归档，不能执行任意上传PT");
    }

    private IllegalStateException invalid() { return new IllegalStateException("Company compute configuration contains missing, duplicate or invalid identities/specifications"); }
    /** Hold persisted work until its explicit YAML identity is restored. Never infer process exit. */
    public static final int UNAVAILABLE = 48010;
    static ServiceException unavailable(String message) { return new ServiceException(message, UNAVAILABLE); }
    public synchronized boolean contains(Long id) {
        return aliases.containsKey(id) || catalogProperties != null && catalogProperties.configured() && id != null && id < -1;
    }
    public synchronized boolean canonical(Long id) { return resources.containsKey(id); }
    public synchronized Long canonicalId(Long id) { return aliases.getOrDefault(id, id); }
    public synchronized List<Long> queueIds(Long id) {
        Long canonical = canonicalId(id); List<Long> ids = new ArrayList<>();
        for (Map.Entry<Long, Long> alias : aliases.entrySet()) if (canonical.equals(alias.getValue())) ids.add(alias.getKey());
        return ids.isEmpty() ? Collections.singletonList(id) : ids;
    }
    public synchronized Object monitor(Long id) { return monitors.computeIfAbsent(canonicalId(id), key -> new Object()); }
    public synchronized List<Long> allIds() { return new ArrayList<>(aliases.keySet()); }
    public synchronized void requireUnconfiguredEndpoint(String host, int port) {
        for(ComputeNode node:resources.values())if(Objects.equals(node.getHost(),host) && node.getPort()==port)
            throw new ServiceException("该自有服务器由服务配置提供，不能再创建数据库接入记录",403);
    }
    public synchronized void requestedProbe(ComputeNode snapshot) {
        ComputeNode node=resources.get(snapshot.getId());
        if(node!=null)synchronized(monitor(node.getId())) { node.setProbeRequestedAt(new Date());if(official(node)){node.setProbeState("CHECKING");node.setProbeTime(null);} }
    }
    public synchronized List<ComputeNode> published() {
        List<ComputeNode> result = new ArrayList<>();
        for (ComputeNode node : resources.values()) if (node.isEnabled() && "PLATFORM".equals(node.getResourceScope())) {
            try { result.add(resolve(node.getId())); }
            catch (ServiceException unavailable) {
                ComputeNode copy = new ComputeNode(); BeanUtils.copyProperties(node, copy);
                copy.setProbeState("ERROR"); copy.setProbeTime(new Date());
                copy.setProbeJson(null); copy.setPowerState("UNKNOWN");
                copy.setLifecycleMessage(unavailable.getMessage()); result.add(copy);
            }
        }
        return result;
    }
    public synchronized ComputeNode resolve(Long id) {
        ComputeNode original = resources.get(canonicalId(id));
        if (original == null) throw unavailable("自有算力未绑定有效的 YAML 受信资产，请管理员核对配置");
        if(official(original))return resolveOfficial(original);
        if (original.getHost()==null) throw unavailable("自有算力未绑定有效的 YAML 受信资产，请管理员核对配置");
        // A reserved ID must never silently alias a persisted third-party/private node.
        List<Object> parameters=new ArrayList<>(queueIds(id));
        parameters.add(original.getHost());parameters.add(original.getPort());parameters.add(original.getGpuIndex());
        for (ComputeNode persisted : jdbc.query("SELECT * FROM vls_compute_node WHERE id IN (" + placeholders(queueIds(id).size()) + ") OR (host=? AND port=? AND gpu_index=?)",
            new BeanPropertyRowMapper<>(ComputeNode.class), parameters.toArray())) {
            if (!queueIds(id).contains(persisted.getId()) || persisted.getId() < 0 || !sameTarget(original, persisted))
                throw unavailable("旧算力记录尚未正确映射或执行目标冲突，请管理员核对节点ID；暂不能使用或移除旧记录");
            if(persisted.getPowerOperationId()!=null)throw unavailable("旧服务器仍有挂起维护操作，不能使用配置供给或移除旧记录");
        }
        // Already-removed rows cannot supply their old ID. Never silently abandon their jobs
        // or infer a new alias from display names/paths: require explicit operator configuration.
        if(original.getHost()!=null)for(Map<String,Object> job:jdbc.queryForList("SELECT node_id,options_json FROM vls_cloud_training_job WHERE node_id NOT IN ("+placeholders(queueIds(id).size())+") AND LOCATE(?,run_dir)=1",
            jobAliasParameters(id,original.getWorkDir()+"/"))) {
            try {
                JsonNode shared=json.readTree(String.valueOf(job.get("options_json"))).path("sharedQueue");
                if(ComputeSharedQueueService.MODE.equals(shared.path("mode").asText()) && shared.path("gpuIndex").asInt(-1)==original.getGpuIndex())
                    throw unavailable("存在尚未映射的历史共享任务，请管理员核对旧节点ID；暂不能使用或移除旧记录");
            } catch(IOException invalid) {throw unavailable("历史任务快照无法核对，请管理员确认旧节点映射");}
        }
        ComputeNode copy = new ComputeNode(); synchronized (monitor(id)) { BeanUtils.copyProperties(original, copy); }
        catalogAdmission(copy);
        return copy;
    }
    private ComputeNode resolveOfficial(ComputeNode original) {
        for(ComputeNode row:jdbc.query("SELECT * FROM vls_compute_node WHERE id=? OR (provider='autodl' AND provider_instance_id=?) OR (host=? AND port=?)",new BeanPropertyRowMapper<>(ComputeNode.class),original.getId(),original.getProviderInstanceId(),original.getHost(),original.getPort())) {
            if (unusedCompanyDisplaySeed(original, row)) continue;
            if(!queueIds(original.getId()).contains(row.getId())||!sameTarget(original,row)||row.getPowerOperationId()!=null)
                throw unavailable("官方实例仍存在未映射或活动的旧连接，保持隔离；不恢复数据库供给");
        }
        if(!jdbc.queryForList("SELECT instance_key FROM vls_compute_public_instance WHERE provider='autodl' AND provider_instance_id=? AND instance_key<>?",original.getProviderInstanceId(),original.getPublicInstanceKey()).isEmpty())
            throw unavailable("官方实例已有另一公共身份，不能绕过原占用或隔离记录");
        ComputeNode copy=new ComputeNode();synchronized(monitor(original.getId())){BeanUtils.copyProperties(original,copy);}
        catalogAdmission(copy);return copy;
    }
    /** Migration 047 inserted a disabled display-only company row. An explicit YAML alias
     * may supersede that unused seed without deleting it or treating it as SSH identity.
     * Real connections, user owners, claims, jobs and accounting history remain blockers. */
    private boolean unusedCompanyDisplaySeed(ComputeNode original, ComputeNode row) {
        if (row.getId()==null || row.getId()<=0 || !queueIds(original.getId()).contains(row.getId())
            || row.isEnabled() || !"autodl".equals(row.getProvider())
            || !Objects.equals(original.getProviderInstanceId(),row.getProviderInstanceId())
            || !"PLATFORM".equals(row.getResourceScope()) || !"AUTODL_PRO".equals(row.getProviderProduct())
            || !("approved-company-"+original.getProviderInstanceId()).equals(row.getManagementOwnerSubject())
            || !"UNCONFIGURED".equals(row.getAuthMode()) || row.isPasswordConfigured() || row.isProviderCredentialConfigured()
            || !empty(row.getHostKey()) || !empty(row.getUsername()) || !empty(row.getPythonPath()) || !empty(row.getWorkDir())
            || row.getPublicInstanceKey()!=null || row.getPowerOperationId()!=null || row.getPowerOperationAction()!=null
            || row.getPowerOperationStartedAt()!=null || row.isPowerOperationSubmitted()) return false;
        return Long.valueOf(0).equals(jdbc.queryForObject("SELECT COUNT(*) FROM vls_cloud_training_job WHERE node_id=?",Long.class,row.getId()))
            && Long.valueOf(0).equals(jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_usage WHERE server_key=?",Long.class,"node-server:"+row.getId()))
            && Long.valueOf(0).equals(jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_paid_run WHERE node_id=?",Long.class,row.getId()))
            && Long.valueOf(0).equals(jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_billing_session WHERE instance_ref=?",Long.class,"node-server:"+row.getId()));
    }
    synchronized void recordPower(ComputeNode expected,ComputePowerProvider.Snapshot observed) {
        ComputeNode node=resources.get(expected.getId());if(!official(node)||!fingerprint(node).equals(fingerprint(expected)))throw unavailable("官方实例控制身份已变化");
        synchronized(monitor(node.getId())) {
            if(!Objects.equals(node.getPowerState(),observed.power)){node.setProbeState("UNTESTED");node.setProbeTime(null);node.setProbeJson(null);}
            node.setPowerState(observed.power);node.setPowerCheckedAt(new Date());node.setCapacitySource("PROVIDER_API");node.setCapacityCheckedAt(new Date());node.setCapacityState(observed.capacity);
            node.setActualStoppedMode(observed.stoppedMode);node.setProviderChargeType(observed.chargeType);node.setProviderGpuAmount(observed.gpuAmount);
            if("start".equals(node.getPowerOperationAction())&&"RUNNING".equals(observed.power)||"stop".equals(node.getPowerOperationAction())&&"STOPPED".equals(observed.power))node.setPowerOperationId(null);
        }
    }
    synchronized void pending(ComputeNode expected,String operation,String action,boolean submitted) {
        ComputeNode node=resources.get(expected.getId());if(!official(node)||!fingerprint(node).equals(fingerprint(expected)))throw unavailable("官方实例控制身份已变化");
        synchronized(monitor(node.getId())){node.setPowerOperationId(operation);node.setPowerOperationAction(action);node.setPowerOperationSubmitted(submitted);node.setPowerOperationStartedAt(new Date());node.setProbeState("UNTESTED");node.setProbeTime(null);}
    }
    synchronized void cancelled(ComputeNode expected){ComputeNode node=resources.get(expected.getId());synchronized(monitor(node.getId())){node.setPowerOperationId(null);node.setPowerState("UNKNOWN");node.setPowerCheckedAt(null);}}
    public synchronized boolean replaces(ComputeNode node) {
        if (!contains(node.getId()) || canonical(node.getId())) return false;
        ComputeNode resource = resources.get(canonicalId(node.getId()));
        return sameTarget(resource, node);
    }
    private Object[] jobAliasParameters(Long id,String prefix) {
        List<Object> parameters=new ArrayList<>(queueIds(id));parameters.add(prefix);return parameters.toArray();
    }
    public synchronized boolean migrationPending(ComputeNode node) {
        if(node==null || node.isConfigurationManaged())return false;
        if("self".equals(node.getProvider()))return !replaces(node);
        if(canonical(node.getId()))return true;
        for(ComputeNode resource:resources.values())if(resource.getHost()!=null && Objects.equals(resource.getHost(),node.getHost())
            && resource.getPort()==node.getPort() && resource.getGpuIndex()==node.getGpuIndex())
            return !Objects.equals(aliases.get(node.getId()),resource.getId()) || !sameTarget(resource,node);
        return false;
    }
    public synchronized void requireRemovableAlias(Long id) {
        if(id==null || contains(id) || resources.isEmpty())return;
        for(ComputeNode node:jdbc.query("SELECT * FROM vls_compute_node WHERE id=?",new BeanPropertyRowMapper<>(ComputeNode.class),id))
            if(migrationPending(node))throw new ServiceException("旧算力记录尚未映射到配置资源，请管理员核对节点ID；不能移除",403);
    }
    private boolean sameTarget(ComputeNode configured, ComputeNode row) {
        if(official(configured))return "autodl".equals(row.getProvider())&&Objects.equals(configured.getProviderInstanceId(),row.getProviderInstanceId())
            &&Objects.equals(configured.getHost(),row.getHost())&&configured.getPort()==row.getPort()&&Objects.equals(configured.getUsername(),row.getUsername())
            &&Objects.equals(configured.getPythonPath(),row.getPythonPath())&&Objects.equals(configured.getWorkDir(),row.getWorkDir());
        return configured.getHost() != null && "self".equals(row.getProvider())
            && Objects.equals(configured.getTenantId(), row.getTenantId()) && Objects.equals(configured.getHost(), row.getHost())
            && configured.getPort() == row.getPort() && Objects.equals(configured.getUsername(), row.getUsername())
            && configured.getGpuIndex() == row.getGpuIndex() && Objects.equals(configured.getPythonPath(), row.getPythonPath())
            && Objects.equals(configured.getWorkDir(), row.getWorkDir()) && ComputeSharedQueueService.MODE.equals(row.getSchedulingMode())
            && "FREE".equals(row.getBillingType()) && row.getPublicInstanceKey() == null
            && (row.getHostKey() == null || Objects.equals(configured.getHostKey(), row.getHostKey()));
    }
    public synchronized void requireMutable(Long id) {
        if (contains(id)) throw new ServiceException("平台自有算力由服务配置管理，页面仅可使用或移除旧接入记录", 403);
        requireRemovableAlias(id);
    }
    public synchronized boolean recordProbe(ComputeNode snapshot, String state, String details, String hostKey) {
        ComputeNode node = resources.get(canonicalId(snapshot.getId()));
        if (node == null || !fingerprint(node).equals(fingerprint(snapshot)) || !Objects.equals(node.getHostKey(), hostKey)) return false;
        synchronized (monitor(node.getId())) {
            if(official(node)&&(!"RUNNING".equals(node.getPowerState())||node.getPowerOperationId()!=null))return false;
            node.setProbeState(state); node.setProbeJson(details); node.setProbeTime(new Date());
            if ("READY".equals(state)) { node.setPowerState("RUNNING"); node.setPowerCheckedAt(new Date()); }
        }
        return true;
    }
    public synchronized Map<String, Object> snapshot(ComputeNode node) {
        Map<String, Object> value = new LinkedHashMap<>(); value.put("code", node.getConfigurationCode());
        value.put("nodeId", String.valueOf(canonicalId(node.getId()))); value.put("targetFingerprint", fingerprint(node));
        if (node.isPlatformCatalogManaged()) {
            value.put("executionRevision", node.getConfigurationRevision());
            if (catalogProperties != null) value.put("executionEnvironment", catalogProperties.getEnvironmentId());
        }
        return value;
    }
    public synchronized void verifyJob(CloudTrainingJob job, ComputeNode node) throws IOException {
        if (!node.isConfigurationManaged()) return;
        String expected = node.getWorkDir() + "/" + ModelArtifactObjectStore.hash(job.getTenantId()).substring(0, 16) + "/" + job.getId();
        JsonNode options = json.readTree(job.getOptionsJson());
        JsonNode identity = options.path("configuredResource");
        boolean shared=ComputeSharedQueueService.MODE.equals(node.getSchedulingMode());
        if (!Objects.equals(expected, job.getRunDir()) || shared&&(!ComputeSharedQueueService.MODE.equals(options.path("sharedQueue").path("mode").asText())
            || node.getGpuIndex() != options.path("sharedQueue").path("gpuIndex").asInt(-1))
            || !shared&&identity.isMissingNode()
            || (!identity.isMissingNode() && (!node.getConfigurationCode().equals(identity.path("code").asText())
            || !String.valueOf(node.getId()).equals(identity.path("nodeId").asText()) || !fingerprint(node).equals(identity.path("targetFingerprint").asText()))))
            throw new IOException("配置算力与本轮冻结执行目标不匹配，保留任务等待管理员核对");
    }
    static String placeholders(int count) { return String.join(",", Collections.nCopies(count, "?")); }
    private String fingerprint(ComputeNode node) {
        String target=node.getConfigurationCode() + "\n" + node.getTenantId() + "\n" + node.getHost() + "\n" + node.getPort()
            + "\n" + node.getUsername() + "\n" + node.getHostKey() + "\n" + node.getPythonPath() + "\n" + node.getWorkDir() + "\n" + node.getGpuIndex();
        if(official(node)){ConfiguredComputeProperties.Resource r=definition(node);target+="\n"+node.getProviderInstanceId()+"\n"+node.getPublicInstanceKey()+"\n"+r.getPrivateKeyFile()+"\n"+r.getPublicKeyFingerprint()+"\n"+ModelArtifactObjectStore.hash(String.valueOf(r.getProviderApiToken()));}
        return ModelArtifactObjectStore.hash(target);
    }
}
