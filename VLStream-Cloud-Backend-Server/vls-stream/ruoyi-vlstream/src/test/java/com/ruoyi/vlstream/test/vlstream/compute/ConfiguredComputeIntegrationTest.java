package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.data.*;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmCategoryEnum;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmTrainingStatusEnum;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import com.ruoyi.vlstream.test.vlstream.service.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.datasource.*;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Real SQL in a random in-memory DB; actors, object storage and SSH are fixtures only. */
@Tag("dev")
public class ConfiguredComputeIntegrationTest {
    final ObjectMapper json=new ObjectMapper();
    JdbcTemplate jdbc; DataSourceTransactionManager transactions;
    ConfiguredComputeProperties properties; ComputeTrustedSelfAssetsProperties assets;
    ConfiguredComputeRegistry registry; ComputeNodeService nodes; ComputeSharedQueueService shared;
    ComputeMarketService market; ComputeSsh ssh; ComputeSsh.Connection connection;
    String actor="alice"; CloudTrainingService training; AlgorithmTraining task;
    IVlsAlgorithmTrainingService trainings; ModelArtifactObjectStore storage;

    @BeforeEach void setup() throws Exception {
        TenantContextHolder.setTenantId("tenant-a");
        DriverManagerDataSource ds=new DriverManagerDataSource("jdbc:h2:mem:configured_"+UUID.randomUUID()+";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","sa","");
        jdbc=new JdbcTemplate(ds); transactions=new DataSourceTransactionManager(ds);
        jdbc.execute("CREATE TABLE vls_compute_node(id BIGINT PRIMARY KEY,tenant_id VARCHAR(64),name VARCHAR(100),provider VARCHAR(32),host VARCHAR(253),port INT DEFAULT 22,username VARCHAR(64),python_path VARCHAR(500),work_dir VARCHAR(500),gpu_index INT DEFAULT 0,scheduling_mode VARCHAR(32),billing_type VARCHAR(32),resource_scope VARCHAR(32),public_instance_key VARCHAR(100),host_key TEXT,enabled TINYINT DEFAULT 1,management_owner_subject VARCHAR(100),create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        for(String column:Arrays.asList("gpu_model VARCHAR(100)","points_per_hour DECIMAL(20,4)","power_state VARCHAR(32)","power_checked_at TIMESTAMP","probe_state VARCHAR(32)","probe_time TIMESTAMP","capacity_state VARCHAR(32)","capacity_checked_at TIMESTAMP","provider_locked BOOLEAN","power_operation_id VARCHAR(100)","provider_gpu_amount INT","probe_json TEXT"))jdbc.execute("ALTER TABLE vls_compute_node ADD "+column);
        jdbc.execute("CREATE TABLE vls_compute_public_instance(instance_key VARCHAR(100),public_state VARCHAR(32),workspace_dirty BOOLEAN)");
        jdbc.execute("CREATE TABLE vls_cloud_training_job(id VARCHAR(36) PRIMARY KEY,tenant_id VARCHAR(64),training_id BIGINT,node_id BIGINT,container_record_id BIGINT,dataset_id BIGINT,version_id BIGINT,annotation_type VARCHAR(40),snapshot_json TEXT,base_model TEXT,options_json TEXT,run_dir TEXT,public_instance_key VARCHAR(100),public_claim_id VARCHAR(100),public_owner_subject VARCHAR(100),job_state VARCHAR(32) DEFAULT 'QUEUED',cancel_requested BOOLEAN DEFAULT FALSE,progress INT DEFAULT 0,epoch_current INT DEFAULT 0,message TEXT,log_tail TEXT,create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        jdbc.execute("CREATE TABLE vls_algorithm_training(id BIGINT PRIMARY KEY,tenant_id VARCHAR(64),dataset_id BIGINT,train_status VARCHAR(32),progress INT,epoch_total INT,epoch_current INT,config_params TEXT,log_path TEXT,error_message TEXT,onnx_conversion_status VARCHAR(32),om_conversion_status VARCHAR(32),model_output_path TEXT,start_time TIMESTAMP,end_time TIMESTAMP,update_time TIMESTAMP)");
        jdbc.execute("CREATE TABLE vls_container_instance(id BIGINT PRIMARY KEY,tenant_id VARCHAR(64),training_task_id BIGINT,instance_status VARCHAR(32),error_message TEXT,stop_time TIMESTAMP)");
        jdbc.execute("CREATE ALIAS GET_LOCK FOR 'com.ruoyi.vlstream.test.vlstream.compute.ConfiguredComputeIntegrationTest.acquireLock'");
        jdbc.execute("CREATE ALIAS RELEASE_LOCK FOR 'com.ruoyi.vlstream.test.vlstream.compute.ConfiguredComputeIntegrationTest.releaseLock'");
        properties=new ConfiguredComputeProperties();assets=new ComputeTrustedSelfAssetsProperties();
        addResource(-173L,173L,"173");addResource(-99L,99L,"99");
        registry=new ConfiguredComputeRegistry(properties,assets,json,jdbc);registry.initialize();
        ssh=mock(ComputeSsh.class);connection=mock(ComputeSsh.Connection.class);when(ssh.open(any())).thenReturn(connection);
        nodes=new ComputeNodeService(jdbc,mock(ComputeCredentialCipher.class),ssh,json);
        ReflectionTestUtils.setField(nodes,"configured",registry);
        ComputeNodeOwnershipService ownership=mock(ComputeNodeOwnershipService.class);when(ownership.administrator()).thenReturn(true);
        when(ownership.viewer()).thenReturn("owner-fixture");when(ownership.canManage(any())).thenReturn(true);
        ReflectionTestUtils.setField(nodes,"ownership",ownership);
        ComputeSharedQueueProperties queue=new ComputeSharedQueueProperties();queue.setEnvironmentId("localhost");
        shared=new ComputeSharedQueueService(queue,new ComputeTrustedSelfPolicy(assets),jdbc,json) {
            @Override protected String authenticatedUser(){return actor;}
        };
        ReflectionTestUtils.setField(nodes,"sharedQueue",shared);
        market=new ComputeMarketService(jdbc,nodes);ReflectionTestUtils.setField(market,"configured",registry);ReflectionTestUtils.setField(market,"sharedQueue",shared);
        for(ComputeNode node:registry.published()) {
            when(connection.hostKey()).thenReturn(node.getHostKey());
            jdbc.update("INSERT INTO vls_compute_node(id,tenant_id,name,provider,host,port,username,python_path,work_dir,gpu_index,scheduling_mode,billing_type,resource_scope,enabled,host_key) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                -node.getId(),node.getTenantId(),node.getName(),node.getProvider(),node.getHost(),node.getPort(),node.getUsername(),node.getPythonPath(),node.getWorkDir(),0,node.getSchedulingMode(),"FREE","PLATFORM",true,node.getHostKey());
        }
        // Both old management rows are actually removed before normal use, not hidden by a mock.
        jdbc.update("DELETE FROM vls_compute_node");
    }
    public static int acquireLock(String name,int wait){return 1;}
    public static int releaseLock(String name){return 1;}
    @AfterEach void cleanup(){TenantContextHolder.clear();}
    void addResource(Long id,Long legacy,String suffix) throws Exception {
        ComputeTrustedSelfAssetsProperties.Asset asset=new ComputeTrustedSelfAssetsProperties.Asset();asset.setAssetId("fixture-"+suffix);
        asset.setTenantId("company-tenant");asset.setHost("192.168.88."+suffix);asset.setPort(22);asset.setUsername("oort");asset.setCompanyPool(true);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream wire=new DataOutputStream(bytes);byte[] type="ssh-ed25519".getBytes(StandardCharsets.US_ASCII);
        wire.writeInt(type.length);wire.write(type);wire.writeInt(32);wire.write(new byte[32]);asset.setHostKey(Base64.getEncoder().encodeToString(bytes.toByteArray()));assets.getAssets().add(asset);
        ConfiguredComputeProperties.Resource value=new ConfiguredComputeProperties.Resource();value.setCode("company-"+suffix);value.setNodeId(id);value.setLegacyNodeIds(Collections.singletonList(legacy));value.setAssetId(asset.getAssetId());value.setName("Free GPU "+suffix);value.setGpuModel("P100");
        value.setPythonPath("173".equals(suffix)?"/data/work/vlstream-gpu/runtime-173/envs/vls-standard/bin/python":"/home/oort/vlstream/envs/vls-standard/bin/python");
        value.setWorkDir("173".equals(suffix)?"/data/work/vlstream-gpu/localhost":"/home/oort/vlstream/localhost");properties.getResources().add(value);
    }
    String proof(ComputeNode node) throws Exception {
        Map<String,Object> value=new LinkedHashMap<>();value.put("contractVersion",1);value.put("ready",true);value.put("pythonIsolated",true);
        value.put("pythonPrefix",node.getPythonPath().replace("/bin/python",""));value.put("gpuUuid","GPU-12345678-1234-1234-1234-123456789abc");return json.writeValueAsString(value);
    }
    void ready(Long id) throws Exception {ComputeNode node=registry.resolve(id);assertTrue(registry.recordProbe(node,"READY",proof(node),node.getHostKey()));}
    void startFixture(Long id) throws Exception {
        ready(id);task=new AlgorithmTraining();task.setId(1L);task.setTenantId("tenant-a");task.setCreateUser("alice");task.setAlgorithmId(2L);task.setTaskName("Fixture");task.setTrainStatus(AlgorithmTrainingStatusEnum.pending);
        jdbc.update("INSERT INTO vls_algorithm_training(id,tenant_id,train_status) VALUES(1,'tenant-a','pending')");
        trainings=mock(IVlsAlgorithmTrainingService.class);when(trainings.selectAlgorithmTrainingById(1L)).thenReturn(task);
        VlsAlgorithmMapper algorithms=mock(VlsAlgorithmMapper.class);Algorithm algorithm=new Algorithm();algorithm.setTenantId("tenant-a");algorithm.setCategory(AlgorithmCategoryEnum.detect);when(algorithms.selectById(2L)).thenReturn(algorithm);
        DataManagementService data=mock(DataManagementService.class);AlgorithmAnnotation project=new AlgorithmAnnotation();project.setAnnotationType("object_detection");when(data.project(3L)).thenReturn(project);
        DatasetSnapshot snapshot=CloudComputeContractTest.snapshot();when(data.snapshot(3L)).thenReturn(snapshot);
        DatasetVersion version=new DatasetVersion();version.setId(4L);version.setSnapshotJson(json.writeValueAsString(snapshot));when(data.saveVersion(eq(3L),any())).thenReturn(version);
        IVlsContainerInstanceService containers=mock(IVlsContainerInstanceService.class);when(containers.saveTaskRecord(any())).thenAnswer(call->{ContainerInstance c=call.getArgument(0);c.setId(5L);jdbc.update("INSERT INTO vls_container_instance VALUES(5,'tenant-a',1,'queued',NULL,NULL)");return true;});
        training=new CloudTrainingService(jdbc,nodes,data,mock(TrainingPublicationService.class),trainings,containers,algorithms,json);
        ReflectionTestUtils.setField(training,"sharedQueue",shared);ReflectionTestUtils.setField(training,"publicAccess",mock(ComputePublicTaskAccess.class));
        storage=mock(ModelArtifactObjectStore.class);ReflectionTestUtils.setField(training,"modelObjects",storage);
    }
    ComputeRequests.Start request(Long id){ComputeRequests.Start request=new ComputeRequests.Start();request.setNodeId(id);request.setDatasetId(3L);request.setModelSource("preset");request.setEpochs(1);return request;}
    CloudTrainingJob job(){return jdbc.queryForObject("SELECT * FROM vls_cloud_training_job",new BeanPropertyRowMapper<>(CloudTrainingJob.class));}

    @Test void deletedRowsLeaveTwoSelectableResourcesAndNoManagementCrudOrBackfill() throws Exception {
        ready(-173L);ready(-99L);
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_node",Integer.class));assertEquals(2,market.list(true).size());assertTrue(market.list(true).stream().allMatch(ComputeMarketService.Resource::isSchedulable));
        assertTrue(market.list(false).stream().noneMatch(ComputeMarketService.Resource::isCanManage));
        assertEquals(-173L,nodes.get(173L).getId());assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_node",Integer.class));
        verifyNoInteractions(ssh);
    }
    @Test void missingProbeNeverBecomesReadyFromYamlAndHealthEvidenceDoesNotWriteNodeRows() throws Exception {
        assertFalse(market.list(true).get(0).isSchedulable());assertThrows(ServiceException.class,()->nodes.requireReady(-173L));
        ready(-173L);assertTrue(shared.ready(nodes.requireReady(-173L)));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_node",Integer.class));
    }
    @Test void consumerHealthUsesRealProbeServiceWithMockSshAndCachesWithoutANodeRow() throws Exception {
        ComputeNode configured=nodes.get(-173L);
        when(connection.execute(anyString(),eq(180))).thenReturn("VLS_PROBE="+proof(configured));
        ComputeHealthRefreshService health=new ComputeHealthRefreshService(jdbc,nodes,transactions);ReflectionTestUtils.setField(health,"sharedQueue",shared);
        assertTrue(health.refresh(-173L).contains("通过"));assertTrue(shared.ready(nodes.get(-173L)));
        assertTrue(health.refresh(173L).contains("五分钟"));verify(ssh,times(1)).open(any());
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_node",Integer.class));
    }
    @Test void yamlBindsStableIdsLegacyAliasesRuntimeAndApprovedAssetReferences() throws Exception {
        java.nio.file.Path build=java.nio.file.Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while(build!=null && !java.nio.file.Files.exists(build.resolve("ruoyi-admin/src/main/resources/application.yml")))build=build.getParent();
        assertNotNull(build);
        org.springframework.core.env.StandardEnvironment environment=new org.springframework.core.env.StandardEnvironment();
        for(org.springframework.core.env.PropertySource<?> source:new org.springframework.boot.env.YamlPropertySourceLoader().load("company-yml",new org.springframework.core.io.FileSystemResource(build.resolve("ruoyi-admin/src/main/resources/application.yml").toFile())))environment.getPropertySources().addLast(source);
        ConfiguredComputeProperties bound=org.springframework.boot.context.properties.bind.Binder.get(environment).bind("vlstream.compute.company-resources",org.springframework.boot.context.properties.bind.Bindable.of(ConfiguredComputeProperties.class)).get();
        assertEquals(3,bound.getResources().size());assertEquals(-173L,bound.getResources().get(0).getNodeId());assertEquals(-99L,bound.getResources().get(1).getNodeId());
        assertEquals(-6000L,bound.getResources().get(2).getNodeId());assertEquals(0,new java.math.BigDecimal("13.00").compareTo(bound.getResources().get(2).getFixedPrice().getPerInstanceHour()));
        assertDoesNotThrow(()->new ConfiguredComputeRegistry(bound,new ComputeTrustedSelfAssetsProperties(),json,mock(JdbcTemplate.class)).initialize());
        assertEquals("192.168.88.173",bound.getResources().get(0).getAssetHost());assertFalse(bound.getResources().get(0).getLegacyNodeIds().isEmpty());
        assertTrue(bound.getResources().get(0).getWorkDir().endsWith("/localhost"));
    }
    @Test void approvedHostReferenceReusesExistingAssetAndMissingAssetCannotAdvertiseSupply() {
        ConfiguredComputeProperties.Resource first=properties.getResources().get(0);first.setAssetId("");first.setAssetHost("192.168.88.173");registry.initialize();assertEquals(2,registry.published().size());
        first.setAssetHost("missing.invalid");registry.initialize();assertEquals(2,registry.published().size());assertTrue(registry.published().get(0).getLifecycleMessage().contains("YAML"));assertThrows(ServiceException.class,()->nodes.requireReady(-173L));
    }
    @Test void unmappedRealRowCannotAdvertiseReadinessOrBeRemovedUntilTheAliasIsExplicit() throws Exception {
        ComputeNode configured=nodes.get(-173L);
        jdbc.update("INSERT INTO vls_compute_node(id,tenant_id,name,provider,host,port,username,python_path,work_dir,gpu_index,scheduling_mode,billing_type,resource_scope,enabled,host_key) VALUES(555,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            configured.getTenantId(),configured.getName(),configured.getProvider(),configured.getHost(),configured.getPort(),configured.getUsername(),configured.getPythonPath(),configured.getWorkDir(),0,configured.getSchedulingMode(),"FREE","PLATFORM",true,configured.getHostKey());
        ComputeNode legacy=jdbc.queryForObject("SELECT * FROM vls_compute_node WHERE id=555",new BeanPropertyRowMapper<>(ComputeNode.class));
        assertTrue(registry.migrationPending(legacy));assertFalse(nodes.canManage(legacy));
        assertThrows(ServiceException.class,()->nodes.delete(555L));assertThrows(ServiceException.class,()->nodes.probe(555L));
        assertThrows(ServiceException.class,()->registry.resolve(-173L));assertTrue(market.list(true).stream().anyMatch(row -> row.getId().equals("-173") && !row.isSchedulable() && row.getMessage().contains("映射")));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_node",Integer.class));verifyNoInteractions(ssh);
        properties.getResources().get(0).setLegacyNodeIds(Arrays.asList(173L,555L));registry.initialize();
        assertFalse(registry.migrationPending(legacy));assertTrue(registry.replaces(legacy));assertEquals(-173L,registry.resolve(555L).getId());
    }
    @Test void unknownHistoricalIdAfterRowRemovalMustBeConfiguredInsteadOfAutoInferred() {
        jdbc.update("INSERT INTO vls_cloud_training_job(id,node_id,run_dir,options_json,job_state) VALUES('old',555,'/data/work/vlstream-gpu/localhost/old-run','{\"sharedQueue\":{\"mode\":\"SHARED_GPU_QUEUE\",\"gpuIndex\":0}}','COMPLETED')");
        assertThrows(ServiceException.class,()->nodes.get(-173L));
        properties.getResources().get(0).setLegacyNodeIds(Arrays.asList(173L,555L));registry.initialize();assertEquals(-173L,nodes.get(555L).getId());
        assertEquals(555L,jdbc.queryForObject("SELECT node_id FROM vls_cloud_training_job",Long.class));
    }
    @Test void mvcAndBeanValidationAcceptConfiguredIdsAsNumbersAndExactStrings() throws Exception {
        CloudTrainingService calls=mock(CloudTrainingService.class);when(calls.start(anyLong(),any())).thenReturn(new RemoteTrainingService.StartResult());
        com.ruoyi.vlstream.test.vlstream.controller.VlsComputeController controller=new com.ruoyi.vlstream.test.vlstream.controller.VlsComputeController(nodes,calls,market,mock(MyTrainingTasksService.class),mock(ComputeProviderSettingsService.class),mock(ComputeProviderStateService.class),mock(ComputeCostObservationService.class),mock(ComputeHealthRefreshService.class),mock(ComputeProviderInventoryService.class));
        org.springframework.test.web.servlet.MockMvc mvc=org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(controller).build();
        for(String node:Arrays.asList("-173","\"-99\""))mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/vlsCompute/trainings/1/start").contentType("application/json").content("{\"nodeId\":"+node+",\"datasetId\":3,\"epochs\":1}")).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
        org.mockito.ArgumentCaptor<ComputeRequests.Start> captured=org.mockito.ArgumentCaptor.forClass(ComputeRequests.Start.class);verify(calls,times(2)).start(eq(1L),captured.capture());
        assertEquals(Arrays.asList(-173L,-99L),Arrays.asList(captured.getAllValues().get(0).getNodeId(),captured.getAllValues().get(1).getNodeId()));
        assertTrue(json.writeValueAsString(nodes.get(-173L)).contains("\"id\":\"-173\""));verifyNoInteractions(ssh);
    }
    @Test void internalContainerOwnershipCompositionPreservesNegativeExecutionResourceId() {
        JdbcTemplate reads=mock(JdbcTemplate.class);Map<String,Object> owner=new LinkedHashMap<>();owner.put("tenant_id","tenant-a");owner.put("create_user",101L);
        when(reads.queryForList(anyString(),eq(1L))).thenReturn(Collections.singletonList(owner));
        ComputePublicTaskAccess access=new ComputePublicTaskAccess(reads,mock(ComputePublicInventoryService.class));
        ContainerInstance record=new ContainerInstance();record.setInstanceType("cloud_training");record.setTrainingTaskId(1L);record.setServerId(-173L);
        access.prepareTaskRecord(record);assertEquals(-173L,record.getServerId());assertEquals("tenant-a",record.getTenantId());assertEquals("101",record.getCreateUser());
    }
    @Test void everyDatabaseSelfNodeIsHistoricalOnlyWithoutAnySourceSwitch() {
        jdbc.update("INSERT INTO vls_compute_node(id,tenant_id,name,provider,host,port,enabled,gpu_index,resource_scope) VALUES(888,'tenant-a','Old self','self','legacy.invalid',22,1,0,'PLATFORM')");
        assertThrows(ServiceException.class,()->nodes.get(888L));
        assertEquals(2,market.list(true).size());
        assertTrue(registry.migrationPending(jdbc.queryForObject("SELECT * FROM vls_compute_node WHERE id=888",new BeanPropertyRowMapper<>(ComputeNode.class))));
        ComputeRequests.Node request=new ComputeRequests.Node();request.setProvider("self");
        assertThrows(ServiceException.class,()->nodes.save(null,request));
        assertTrue(registry.contains(-173L));assertEquals(-173L,nodes.queueId(173L));verifyNoInteractions(ssh);
    }
    @Test void malformedCompanyMappingLeavesAutodlSupplyVisibleAndSelfWorkHeld() {
        jdbc.execute("ALTER TABLE vls_compute_node ADD server_group VARCHAR(100)");jdbc.execute("ALTER TABLE vls_compute_node ADD provider_instance_id VARCHAR(100)");
        properties.getResources().get(0).setAssetId("not-installed");registry.initialize();
        jdbc.update("INSERT INTO vls_compute_node(id,tenant_id,name,provider,host,port,enabled,gpu_index,resource_scope) VALUES(888,'tenant-a','AutoDL unaffected','autodl','legacy.invalid',22,1,0,'PLATFORM')");
        assertTrue(market.list(false).stream().anyMatch(row -> row.getId().equals("888")));
        assertThrows(ServiceException.class,()->nodes.get(-173L));
        assertEquals("autodl",nodes.get(888L).getProvider());verifyNoInteractions(ssh);
    }
    @Test void unmappedPrivateSelfWorkIsHeldWithoutMarkingTheRemoteProcessStopped() {
        jdbc.update("INSERT INTO vls_compute_node(id,tenant_id,name,provider,host,resource_scope) VALUES(888,'tenant-a','Old self','self','legacy.invalid','PRIVATE')");
        jdbc.update("INSERT INTO vls_cloud_training_job(id,tenant_id,node_id,run_dir,options_json,job_state,container_record_id) VALUES('old-private','tenant-a',888,'/old/run','{}','RUNNING',6)");
        CloudTrainingWorker worker=new CloudTrainingWorker(jdbc,transactions,nodes,ssh,mock(CloudTrainingDatasetWriter.class),mock(DataManagementService.class),storage,mock(RemoteModelArtifactService.class),json);
        try {
            worker.tick(888L);assertTrue(jdbc.queryForObject("SELECT message FROM vls_cloud_training_job WHERE id='old-private'",String.class).contains("YAML"));
            jdbc.update("DELETE FROM vls_compute_node WHERE id=888");worker.tick(888L);
        } finally {worker.shutdown();}
        assertEquals("RUNNING",jdbc.queryForObject("SELECT job_state FROM vls_cloud_training_job WHERE id='old-private'",String.class));
        assertTrue(jdbc.queryForObject("SELECT message FROM vls_cloud_training_job WHERE id='old-private'",String.class).contains("保留"));verifyNoInteractions(ssh);
    }
    @Test void invalidatedAliasesCannotReuseCachedReadyProofOrBypassPendingMaintenance() throws Exception {
        ready(-173L);ComputeNode node=nodes.get(-173L);
        jdbc.update("INSERT INTO vls_compute_node(id,tenant_id,name,provider,host,port,username,python_path,work_dir,gpu_index,scheduling_mode,billing_type,resource_scope,enabled,host_key,power_operation_id) VALUES(173,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            node.getTenantId(),node.getName(),node.getProvider(),node.getHost(),node.getPort(),node.getUsername(),node.getPythonPath(),node.getWorkDir(),0,node.getSchedulingMode(),"FREE","PLATFORM",true,node.getHostKey(),"pending-maintenance");
        assertThrows(ServiceException.class,()->nodes.get(-173L));assertThrows(ServiceException.class,()->nodes.delete(173L));
        ComputeNode unavailable=registry.published().get(0);assertFalse(shared.ready(unavailable));assertNull(unavailable.getProbeJson());
        assertTrue(unavailable.getLifecycleMessage().contains("维护"));verifyNoInteractions(ssh);
    }
    @Test void bothResourcesStartWithCanonicalIdsWithoutDatabaseNodes() throws Exception {
        startFixture(-173L);training.start(1L,request(173L));
        CloudTrainingJob job=job();assertEquals(-173L,job.getNodeId());assertTrue(job.getOptionsJson().contains("targetFingerprint"));
        registry.verifyJob(job,nodes.get(job.getNodeId()));assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_node",Integer.class));verifyNoInteractions(ssh);
    }
    @Test void secondResourceUsesItsOwnTargetAndStableArtifactNamespace() throws Exception {
        startFixture(-99L);training.start(1L,request(-99L));assertEquals(-99L,job().getNodeId());assertTrue(job().getRunDir().startsWith("/home/oort/vlstream/localhost/"));assertTrue(job().modelKey().startsWith("cloud-training/-99/"));
    }
    @Test void disabledOrUnpublishedResourcesCannotAcceptTraining() throws Exception {
        properties.getResources().get(0).setEnabled(false);properties.getResources().get(1).setPublished(false);registry.initialize();
        ready(-173L);ready(-99L);assertTrue(market.list(true).isEmpty());assertThrows(ServiceException.class,()->nodes.requireReady(-173L));assertThrows(ServiceException.class,()->nodes.requireReady(-99L));
    }
    @Test void duplicateIdsAliasesTargetsAndUnsupportedParallelismFailAtConfigurationTime() {
        ConfiguredComputeProperties.Resource first=properties.getResources().get(0),second=properties.getResources().get(1);
        second.setNodeId(first.getNodeId());assertThrows(IllegalStateException.class,registry::initialize);second.setNodeId(-99L);
        second.setLegacyNodeIds(first.getLegacyNodeIds());assertThrows(IllegalStateException.class,registry::initialize);second.setLegacyNodeIds(Collections.singletonList(99L));
        second.setAssetId(first.getAssetId());assertThrows(IllegalStateException.class,registry::initialize);second.setAssetId("fixture-99");
        first.setMaxConcurrentTasks(2);assertThrows(IllegalStateException.class,registry::initialize);
    }
    @Test void endpointOrReservedIdCollisionRefusesBeforeAnyConnection() {
        jdbc.update("INSERT INTO vls_compute_node(id,provider,host) VALUES(173,'autodl','fixture.invalid')");assertThrows(ServiceException.class,()->nodes.get(-173L));verifyNoInteractions(ssh);
    }
    @Test void nodeEditsProviderActionsAndDuplicateConnectionAreBlockedWithoutSsh() {
        assertThrows(ServiceException.class,()->nodes.probe(-173L));assertThrows(ServiceException.class,()->nodes.getManaged(-173L));
        assertThrows(ServiceException.class,()->nodes.delete(-173L));assertThrows(ServiceException.class,()->market.power(-173L,"start"));
        assertThrows(ServiceException.class,()->market.publish(-173L,new ComputeRequests.Offer()));
        assertThrows(ServiceException.class,()->registry.requireUnconfiguredEndpoint("192.168.88.173",22));
        assertThrows(ServiceException.class,()->new ConfiguredComputeManagementGuard(registry).requireMutable(173L));verifyNoInteractions(ssh);
    }
    @Test void targetRuntimeChangesCannotRedirectAnExistingJob() throws Exception {
        startFixture(-173L);training.start(1L,request(-173L));CloudTrainingJob frozen=job();
        properties.getResources().get(0).setWorkDir("/data/work/vlstream-gpu/vm");registry.initialize();
        assertThrows(IOException.class,()->registry.verifyJob(frozen,nodes.get(-173L)));verifyNoInteractions(ssh);
    }
    @Test void nativeSharedActorRemainsBoundToTenantAndCreatorAfterDeletingNodes() throws Exception {
        startFixture(-173L);actor="bob";assertThrows(ServiceException.class,()->training.start(1L,request(-173L)));actor="alice";
        String owner=shared.viewer();TenantContextHolder.setTenantId("tenant-b");assertNotEquals(owner,shared.viewer());
        assertThrows(ServiceException.class,()->shared.requireCreator("bob"));actor=null;assertFalse(shared.authenticated());assertFalse(market.list(true).get(0).isSchedulable());
    }
    @Test void publicProjectionContainsOnlySafeFieldsAndPaginatesAfterDeletingRows() throws Exception {
        ComputePublicMarketService service=new ComputePublicMarketService(jdbc);ReflectionTestUtils.setField(service,"configured",registry);
        ComputePublicMarketService.Page page=service.list(1,1);assertEquals(2,page.getTotal());assertEquals(1,page.getItems().size());
        String value=json.writeValueAsString(page);for(String secret:Arrays.asList("host","username","tenantId","probeJson","nodeId","assetId","password","taskCount"))assertFalse(value.contains("\""+secret+"\""));
        assertEquals(1,service.list(2,1).getItems().size());assertTrue(service.list(3,1).getItems().isEmpty());
    }
    @Test void legacyAndNewJobsShareOneQueueAndRemovalIsBlockedByEitherIdentity() throws Exception {
        assertEquals(Arrays.asList(-173L,173L),nodes.queueIds(173L));assertEquals(-173L,nodes.queueId(173L));assertSame(registry.monitor(173L),registry.monitor(-173L));
        jdbc.update("INSERT INTO vls_cloud_training_job(id,node_id,job_state) VALUES('legacy',173,'RUNNING')");assertThrows(ServiceException.class,()->nodes.assertNoActiveJobs(-173L));
        jdbc.update("UPDATE vls_cloud_training_job SET node_id=-173,job_state='ARCHIVING'");assertThrows(ServiceException.class,()->nodes.assertNoActiveJobs(173L));
        jdbc.update("UPDATE vls_cloud_training_job SET job_state='COMPLETED'");nodes.assertNoActiveJobs(173L);
    }
    @Test void workerCancelsLegacyTaskAfterRowRemovalUsingTheConfiguredTargetAndLeavesOtherTasksQueued() throws Exception {
        startFixture(-173L);training.start(1L,request(-173L));
        jdbc.update("UPDATE vls_cloud_training_job SET node_id=173,cancel_requested=TRUE");
        // A second tenant's waiting ticket shares the GPU but remains a different owned task.
        CloudTrainingJob first=job();
        String secondOptions=first.getOptionsJson().replace(first.getId(),"second-ticket").replace("tenant-a","tenant-b");
        jdbc.update("INSERT INTO vls_cloud_training_job(id,tenant_id,node_id,options_json,run_dir,public_owner_subject,job_state,container_record_id) VALUES('second-ticket','tenant-b',-173,?,?,?,'QUEUED',6)",secondOptions,
            "/data/work/vlstream-gpu/localhost/"+ModelArtifactObjectStore.hash("tenant-b").substring(0,16)+"/second-ticket",first.getPublicOwnerSubject());
        when(connection.execute(anyString(),anyInt())).thenReturn("VLS_SHARED={}");
        CloudTrainingWorker worker=new CloudTrainingWorker(jdbc,transactions,nodes,ssh,mock(CloudTrainingDatasetWriter.class),mock(DataManagementService.class),storage,mock(RemoteModelArtifactService.class),json);
        ReflectionTestUtils.setField(worker,"sharedQueue",shared);
        try {worker.tick(173L);} finally {worker.shutdown();}
        assertEquals("CANCELLED",jdbc.queryForObject("SELECT job_state FROM vls_cloud_training_job WHERE id=?",String.class,first.getId()));
        assertEquals("QUEUED",jdbc.queryForObject("SELECT job_state FROM vls_cloud_training_job WHERE id='second-ticket'",String.class));
        assertEquals("tenant-a",TenantContextHolder.getTenantId());
        verify(ssh,atLeastOnce()).open(argThat(node->node.getId().equals(-173L) && node.getHost().equals("192.168.88.173")));
    }
    @Test void historicalLogsAndStopKeepNativeOwnerAndTenantChecksWithoutANodeRow() throws Exception {
        startFixture(-173L);training.start(1L,request(-173L));CloudTrainingJob stored=job();
        task.setConfigParams(jdbc.queryForObject("SELECT config_params FROM vls_algorithm_training WHERE id=1",String.class));
        // H2 cannot parse MySQL's unary BINARY syntax. Keep the real stored job and
        // service authorization, supplying only that dialect-specific SELECT as a fixture.
        JdbcTemplate reads=mock(JdbcTemplate.class);
        when(reads.query(anyString(),any(RowMapper.class),any(),any(),any())).thenReturn(Collections.singletonList(stored));
        CloudTrainingService reader=new CloudTrainingService(reads,nodes,mock(DataManagementService.class),mock(TrainingPublicationService.class),trainings,mock(IVlsContainerInstanceService.class),mock(VlsAlgorithmMapper.class),json);
        ReflectionTestUtils.setField(reader,"sharedQueue",shared);ReflectionTestUtils.setField(reader,"publicAccess",mock(ComputePublicTaskAccess.class));
        assertEquals("",reader.logs(task).getLogContent());reader.stop(task);
        verify(reads).update(contains("cancel_requested=1"),eq("tenant-a"),eq(stored.getId()));
        actor="bob";assertThrows(ServiceException.class,()->reader.logs(task));assertThrows(ServiceException.class,()->reader.stop(task));
        actor="alice";TenantContextHolder.setTenantId("tenant-b");assertThrows(ServiceException.class,()->reader.current(task));verifyNoInteractions(ssh);
    }
}
