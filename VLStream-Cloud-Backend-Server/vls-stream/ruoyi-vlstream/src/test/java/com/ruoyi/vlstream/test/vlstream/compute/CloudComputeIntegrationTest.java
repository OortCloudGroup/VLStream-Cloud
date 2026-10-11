package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.data.*;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmCategoryEnum;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmTrainingStatusEnum;
import com.ruoyi.vlstream.test.vlstream.mapper.*;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import com.ruoyi.vlstream.test.vlstream.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@Tag("dev")
@EnabledIfEnvironmentVariable(named = "VLS_AUTODL_TEST_JDBC", matches = "jdbc:mysql://127\\.0\\.0\\.1:33329/autodl_contract_test.*")
class CloudComputeIntegrationTest {
    static JdbcTemplate jdbc;
    static DataSourceTransactionManager transactions;
    static Path root;
    ObjectMapper json = new ObjectMapper();
    ComputeSsh ssh;
    ComputeSsh.Connection connection;
    ComputeNodeService nodes;
    CloudTrainingService training;
    CloudTrainingWorker worker;
    ModelArtifactObjectStore storage;
    Long nodeId;
    AlgorithmTraining task;

    @BeforeAll static void database() throws Exception {
        DriverManagerDataSource source = new DriverManagerDataSource(System.getenv("VLS_AUTODL_TEST_JDBC"), "root", "autodl-test-only");
        jdbc = new JdbcTemplate(source); transactions = new DataSourceTransactionManager(source);
        root = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (!Files.exists(root.resolve("BUSINESS_PROCESSES.md"))) root = root.getParent();
        for (String table : Arrays.asList("vls_compute_provider_inventory","vls_compute_public_instance","vls_compute_charge","vls_compute_usage","vls_compute_billing_remainder","vls_cloud_training_job", "vls_compute_node", "vls_training_publication", "vls_algorithm_training", "vls_container_instance", "vls_dataset_conversion_guard", "sys_menu")) jdbc.execute("DROP TABLE IF EXISTS " + table);
        jdbc.execute("CREATE TABLE sys_menu(menu_id BIGINT PRIMARY KEY,menu_name VARCHAR(100),parent_id BIGINT,order_num INT,path VARCHAR(200),component VARCHAR(200),query_param VARCHAR(200),is_frame INT,is_cache INT,menu_type CHAR(1),visible CHAR(1),status CHAR(1),perms VARCHAR(100),icon VARCHAR(100),create_by VARCHAR(100),create_time DATETIME,update_by VARCHAR(100),update_time DATETIME,remark VARCHAR(500))");
        jdbc.execute("CREATE TABLE vls_algorithm_training(id BIGINT PRIMARY KEY,tenant_id VARCHAR(64),create_user VARCHAR(64),task_name VARCHAR(100),create_time DATETIME DEFAULT CURRENT_TIMESTAMP,is_deleted INT DEFAULT 0,dataset_id BIGINT,train_status VARCHAR(20),progress INT,epoch_total INT,epoch_current INT,config_params TEXT,log_path TEXT,error_message TEXT,model_output_path TEXT,onnx_model_output_path TEXT,om_model_output_path TEXT,rknn_model_output_path TEXT,int8_rknn_model_output_path TEXT,onnx_conversion_status VARCHAR(20),om_conversion_status VARCHAR(20),start_time DATETIME,end_time DATETIME,update_time DATETIME)");
        jdbc.execute("CREATE TABLE vls_container_instance(id BIGINT PRIMARY KEY,tenant_id VARCHAR(64),training_task_id BIGINT,instance_status VARCHAR(20),error_message TEXT,stop_time DATETIME)");
        jdbc.execute("CREATE TABLE vls_dataset_conversion_guard(tenant_id VARCHAR(64),training_id BIGINT)");
        for (String file : Arrays.asList("V1_2_0_018__training_auto_publish.sql", "V1_2_0_025__autodl_ssh_compute.sql", "V1_2_0_030__compute_market.sql", "V1_2_0_031__compute_operator_permissions.sql", "V1_2_0_032__training_owner_query_index.sql", "V1_2_0_033__compute_provider_control.sql", "V1_2_0_034__compute_lifecycle_revision.sql", "V1_2_0_035__compute_usage_accounting_contract.sql", "V1_2_0_036__compute_cost_observation.sql", "V1_2_0_037__compute_health_refresh_throttle.sql", "V1_2_0_038__compute_usage_opening_user.sql", "V1_2_0_039__public_compute_instance_claim.sql", "V1_2_0_040__compute_provider_inventory_drafts.sql")) {
            String sql = new String(Files.readAllBytes(root.resolve("VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/db/migration/" + file)), StandardCharsets.UTF_8);
            for (String statement : sql.split(";")) if (!statement.trim().isEmpty()) jdbc.execute(statement);
        }
    }
    @BeforeEach void setup() throws Exception {
        for (String table : Arrays.asList("vls_compute_provider_inventory","vls_compute_public_instance","vls_compute_charge","vls_compute_usage","vls_compute_billing_remainder","vls_cloud_training_job", "vls_compute_node", "vls_training_publication", "vls_algorithm_training", "vls_container_instance")) jdbc.update("DELETE FROM " + table);
        jdbc.update("INSERT INTO vls_algorithm_training(id,tenant_id,train_status) VALUES(1,'tenant-a','pending')");
        TenantContextHolder.setTenantId("tenant-a");
        ComputeCredentialCipher cipher = mock(ComputeCredentialCipher.class); when(cipher.encrypt(anyString())).thenReturn("encrypted-test-secret");
        ssh = mock(ComputeSsh.class); connection = mock(ComputeSsh.Connection.class); when(ssh.open(any())).thenReturn(connection); when(connection.hostKey()).thenReturn("test-key");
        nodes = new ComputeNodeService(jdbc, cipher, ssh, json);
        nodeId = nodes.save(null, request()).getId();
        jdbc.update("UPDATE vls_compute_node SET probe_state='READY',probe_time=NOW(),power_state='RUNNING',probe_json='{\"contractVersion\":1,\"ready\":true}',host_key='test-key' WHERE id=?", nodeId);
        task = new AlgorithmTraining(); task.setId(1L); task.setTenantId("tenant-a"); task.setAlgorithmId(2L); task.setTaskName("cloud-test"); task.setTrainStatus(AlgorithmTrainingStatusEnum.pending); task.setEpochTotal(1);
        IVlsAlgorithmTrainingService trainings = mock(IVlsAlgorithmTrainingService.class); when(trainings.selectAlgorithmTrainingById(1L)).thenReturn(task);
        VlsAlgorithmMapper algorithms = mock(VlsAlgorithmMapper.class); Algorithm algorithm = new Algorithm(); algorithm.setCategory(AlgorithmCategoryEnum.detect); algorithm.setTenantId("tenant-a"); when(algorithms.selectById(2L)).thenReturn(algorithm);
        DataManagementService data = mock(DataManagementService.class);
        AlgorithmAnnotation project = new AlgorithmAnnotation(); project.setId(3L); project.setAnnotationType("object_detection"); when(data.project(3L)).thenReturn(project);
        DatasetSnapshot snapshot = CloudComputeContractTest.snapshot(); when(data.snapshot(3L)).thenReturn(snapshot); when(data.readSnapshot(anyString())).thenReturn(snapshot);
        DatasetVersion version = new DatasetVersion(); version.setId(4L); version.setSnapshotJson(json.writeValueAsString(snapshot)); when(data.saveVersion(eq(3L), any(DataRequests.Version.class))).thenReturn(version);
        TrainingPublicationService publication = new TrainingPublicationService(jdbc, transactions, mock(VlsAlgorithmTrainingMapper.class), mock(VlsAlgorithmModelMapper.class), mock(IVlsAlgorithmModelService.class));
        IVlsContainerInstanceService containers = mock(IVlsContainerInstanceService.class);
        when(containers.save(any())).thenAnswer(call -> { ContainerInstance instance = call.getArgument(0); instance.setId(com.baomidou.mybatisplus.core.toolkit.IdWorker.getId()); jdbc.update("INSERT INTO vls_container_instance(id,tenant_id,training_task_id,instance_status) VALUES(?,?,?,?)", instance.getId(), instance.getTenantId(), instance.getTrainingTaskId(), instance.getInstanceStatus()); return true; });
        training = new CloudTrainingService(jdbc, nodes, data, publication, trainings, containers, algorithms, json);
        storage = mock(ModelArtifactObjectStore.class);
        CloudTrainingDatasetWriter writer = mock(CloudTrainingDatasetWriter.class);
        when(writer.ensureArtifact(any())).thenReturn("vls-dataset://11111111-1111-1111-1111-111111111111");
        worker = new CloudTrainingWorker(jdbc, transactions, nodes, ssh, writer, data, storage, mock(RemoteModelArtifactService.class), json);
        ReflectionTestUtils.setField(worker, "tempDir", root.resolve("codex/autodl/tmp").toString());
    }
    @AfterEach void cleanup() { worker.shutdown(); TenantContextHolder.clear(); }
    void publicFixture() {
        ComputePublicInventoryService inventory=new ComputePublicInventoryService(jdbc);
        ComputePlatformAuthorization auth=mock(ComputePlatformAuthorization.class);
        when(auth.requireIdentity()).thenAnswer(call->{ComputePlatformAuthorization.Context p=new ComputePlatformAuthorization.Context();p.setAuthorized(true);p.setPlatformUserId("test-platform-user");p.setVlsUserId("test-vls-user");p.setVlsTenantId(ComputeNodeService.tenant());return p;});
        ReflectionTestUtils.setField(inventory,"authorization",auth); ReflectionTestUtils.setField(nodes,"publicInventory",inventory);
        ComputeNode physical=nodes.get(nodeId);
        jdbc.update("UPDATE vls_compute_node SET provider_instance_id='test-instance',resource_scope='PLATFORM',billing_type='FREE' WHERE (host=? AND port=?) OR id=?",physical.getHost(),physical.getPort(),nodeId);
        ComputeRequests.PublicIdentity identity=new ComputeRequests.PublicIdentity();identity.setAccountScope("test-account");identity.setInstanceId("test-instance");
        new TransactionTemplate(transactions).execute(tx->{ComputeNode node=nodes.lockServer(nodeId);inventory.bind(node,identity);inventory.claim(nodes.get(nodeId));return null;});
    }
    @Test void myTasksCountAllNodesAndOwnedDefaultGpuHistoryWithTheSameFiltersAsPagination() {
        start(); jdbc.update("UPDATE vls_algorithm_training SET create_user='alice',task_name='cloud one' WHERE id=1");
        ComputeRequests.Node second = request(); second.setGpuIndex(1); second.setName("second node");
        Long secondNode = nodes.save(null, second).getId();
        jdbc.update("INSERT INTO vls_algorithm_training(id,tenant_id,create_user,task_name,train_status,config_params) VALUES(2,'tenant-a','alice','old P100','completed','{}'),(3,'tenant-a','bob','bob task','pending','{}'),(4,'tenant-a',NULL,'owner unknown','completed','{}'),(5,'tenant-b','alice','other tenant','pending','{}'),(6,'tenant-a','alice','cloud two','pending','{\"cloudJobId\":\"second-job\"}')");
        jdbc.update("INSERT INTO vls_cloud_training_job(id,tenant_id,training_id,node_id,container_record_id,dataset_id,version_id,annotation_type,snapshot_json,base_model,options_json,run_dir) SELECT 'second-job',tenant_id,6,?,container_record_id,dataset_id,version_id,annotation_type,snapshot_json,base_model,options_json,run_dir FROM vls_cloud_training_job LIMIT 1",secondNode);
        MyTrainingTasksService alice = new MyTrainingTasksService(jdbc) { @Override protected String authenticatedUser() { return "alice"; } };
        MyTrainingTasksService.Page page = alice.page(1,1,null,null,null,null);
        assertEquals(3,page.getTotal()); assertEquals(1,page.getRecords().size());
        assertTrue(page.getRecords().get(0).get("createTime") instanceof String);
        assertEquals(2L,page.getStatistics().get("QUEUED")); assertEquals(1L,page.getStatistics().get("COMPLETED"));
        assertEquals(1,alice.page(1,20,"P100",null,null,null).getTotal());
        assertEquals(2,alice.page(1,20,null,"QUEUED",null,null).getTotal());
        assertEquals(0,alice.page(1,20,null,null,"2099-01-01 00:00:00",null).getTotal());
        MyTrainingTasksService bob = new MyTrainingTasksService(jdbc) { @Override protected String authenticatedUser() { return "bob"; } };
        assertEquals(1,bob.page(1,20,null,null,null,null).getTotal());
        TenantContextHolder.setTenantId("tenant-b"); assertEquals(1,alice.page(1,20,null,null,null,null).getTotal());
    }
    @Test void operatorPermissionCatalogueIsRegisteredWithoutChangingResourceOwnership() {
        assertEquals(4, jdbc.queryForObject("SELECT COUNT(*) FROM sys_menu WHERE perms LIKE 'vls:compute:%'", Integer.class));
        assertEquals("PRIVATE", nodes.get(nodeId).getResourceScope());
    }
    @Test void multipleGpuQuotasShareOneServerWithoutDuplicatingTheSameCard() {
        ComputeRequests.Node second = request(); second.setGpuIndex(1); second.setServerGroup("one-server");
        assertNotEquals(nodeId, nodes.save(null, second).getId());
        assertThrows(RuntimeException.class, () -> nodes.save(null, second));
        assertEquals(2, nodes.list().size());
    }
    @Test void publishedSupplyIsVisibleAcrossTenantsButOwnerEditsAndStopsRemainProtected() {
        ComputeMarketService market = new ComputeMarketService(jdbc, nodes);
        ComputeRequests.Offer offer = new ComputeRequests.Offer(); offer.setPublished(true); offer.setBillingType("FREE"); market.publish(nodeId, offer);
        assertThrows(RuntimeException.class,this::start);
        TenantContextHolder.setTenantId("tenant-b");
        assertTrue(nodes.list().isEmpty());
        assertEquals(1, market.list(true).size()); assertNull(market.list(true).get(0).getTaskCount()); assertEquals("ISOLATED",market.list(true).get(0).getClaimState());
        assertThrows(RuntimeException.class, () -> market.power(nodeId, "stop"));
        assertThrows(RuntimeException.class, () -> market.publish(nodeId, offer));
        assertThrows(RuntimeException.class, () -> nodes.delete(nodeId));
        TenantContextHolder.setTenantId("tenant-a");
        assertThrows(RuntimeException.class, () -> market.power(nodeId, "stop"));
    }
    @Test void operatorNoStockObservationExpiresAndStoppedResourceRemainsSelectable() {
        ComputeMarketService market = new ComputeMarketService(jdbc, nodes);
        ComputeRequests.Observation observation = new ComputeRequests.Observation(); observation.setPowerState("STOPPED"); observation.setCapacityState("UNAVAILABLE"); observation.setNote("test console explicitly reported no stock");
        market.observe(nodeId, observation); assertTrue(market.list(true).isEmpty());
        jdbc.update("UPDATE vls_compute_node SET capacity_checked_at=DATE_SUB(NOW(), INTERVAL 6 MINUTE) WHERE id=?", nodeId);
        assertEquals("UNKNOWN", market.list(true).get(0).getCapacityState()); assertTrue(market.list(true).get(0).isStopped());
        assertThrows(RuntimeException.class, () -> nodes.requireReady(nodeId));
    }
    @Test void busySiblingGpuPreventsWholeServerShutdown() {
        ComputeRequests.Node second = request(); second.setGpuIndex(1);
        Long otherCard = nodes.save(null, second).getId();
        start();
        assertThrows(RuntimeException.class, () -> new ComputeMarketService(jdbc, nodes).power(otherCard, "stop"));
    }
    @Test void oneServerHasOnlyOnePowerOperationAcrossGpuCardsAndUnknownOutcomeIsReadOnlyReconciled() {
        ComputeRequests.Node second=request(); second.setGpuIndex(1); Long other=nodes.save(null,second).getId();
        ComputePowerProvider provider=mock(ComputePowerProvider.class); when(provider.supports(any())).thenReturn(true);
        ComputeMarketService market=new ComputeMarketService(jdbc,nodes); ReflectionTestUtils.setField(market,"powerProviders",Collections.singletonList(provider));
        jdbc.update("UPDATE vls_compute_node SET power_state='STOPPED',control_mode='API' WHERE id IN (?,?)",nodeId,other);
        new TransactionTemplate(transactions).execute(tx -> market.power(nodeId,"start"));
        String repeated=new TransactionTemplate(transactions).execute(tx -> market.power(other,"start")); assertTrue(repeated.contains("正在处理"));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_node WHERE power_operation_id IS NOT NULL",Integer.class));
        ComputePowerWorker power=new ComputePowerWorker(jdbc); ReflectionTestUtils.setField(power,"providers",Collections.singletonList(provider));
        doThrow(new ComputeProviderException(ComputeProviderException.Kind.UNKNOWN)).when(provider).submit(any(),eq("start"),anyString());
        power.scan(); assertTrue(nodes.get(nodeId).isPowerOperationSubmitted()); assertNotNull(nodes.get(nodeId).getPowerOperationId()); assertEquals("UNKNOWN",nodes.get(nodeId).getPowerState());
        when(provider.status(any())).thenReturn(new ComputePowerProvider.Snapshot("RUNNING","UNKNOWN","test vendor running"));
        power.scan(); verify(provider,times(1)).submit(any(),eq("start"),anyString());
        assertNull(nodes.get(nodeId).getPowerOperationId()); assertEquals("RUNNING",nodes.get(other).getPowerState());
    }
    @Test void ownerRecoveryRequiresFreshVendorTerminalStateAndNeverResubmits() {
        jdbc.update("UPDATE vls_compute_node SET control_mode='API',power_state='UNKNOWN',power_operation_id='test-op',power_operation_action='stop',power_operation_submitted=1,power_operation_started_at=DATE_SUB(NOW(),INTERVAL 11 MINUTE) WHERE id=?",nodeId);
        ComputePowerProvider provider=mock(ComputePowerProvider.class); when(provider.supports(any())).thenReturn(true);
        ComputeProviderStateService state=new ComputeProviderStateService(jdbc,nodes); ReflectionTestUtils.setField(state,"providers",Collections.singletonList(provider));
        when(provider.status(any())).thenReturn(new ComputePowerProvider.Snapshot("UNKNOWN","UNKNOWN","unknown"));
        assertThrows(RuntimeException.class,()->new TransactionTemplate(transactions).execute(tx->state.refresh(nodeId,true))); assertNotNull(nodes.get(nodeId).getPowerOperationId());
        when(provider.status(any())).thenReturn(new ComputePowerProvider.Snapshot("RUNNING","UNKNOWN","current terminal"));
        assertTrue(new TransactionTemplate(transactions).execute(tx->state.refresh(nodeId,true)).contains("未执行启停")); assertNull(nodes.get(nodeId).getPowerOperationId());
        verify(provider,never()).submit(any(),anyString(),anyString());
    }
    @Test void expiredSubmittedOperationContinuesToPollAndCompletesWithoutResending() {
        jdbc.update("UPDATE vls_compute_node SET control_mode='API',power_state='UNKNOWN',power_operation_id='test-op',power_operation_action='start',power_operation_submitted=1,power_operation_started_at=DATE_SUB(NOW(),INTERVAL 11 MINUTE) WHERE id=?",nodeId);
        ComputePowerProvider provider=mock(ComputePowerProvider.class); when(provider.supports(any())).thenReturn(true);
        when(provider.status(any())).thenReturn(new ComputePowerProvider.Snapshot("RUNNING","UNKNOWN","vendor terminal"));
        ComputePowerWorker power=new ComputePowerWorker(jdbc); ReflectionTestUtils.setField(power,"providers",Collections.singletonList(provider)); power.scan();
        assertNull(nodes.get(nodeId).getPowerOperationId()); assertEquals("RUNNING",nodes.get(nodeId).getPowerState()); verify(provider,never()).submit(any(),anyString(),anyString());
    }
    @Test void providerCredentialsUseOwnerEncryptionAndChangingIdentityRequiresNewCredentials() throws Exception {
        ComputeProviderSettingsService settings=new ComputeProviderSettingsService(jdbc,nodes,new ComputeCredentialCipher("0123456789abcdef"),json);
        ComputeRequests.ProviderSettings r=new ComputeRequests.ProviderSettings(); r.setControlMode("API"); r.setProviderProduct("AUTODL_PRO"); r.setProviderInstanceId("pro-test"); r.setToken("test-only-token");
        new TransactionTemplate(transactions).execute(tx->{ settings.save(nodeId,r); return null; });
        ComputeNode saved=nodes.get(nodeId); assertTrue(saved.isProviderCredentialConfigured()); assertFalse(saved.getProviderCredentialCipher().contains("test-only-token"));
        r.setToken(null); r.setProviderInstanceId("pro-test-new"); assertThrows(RuntimeException.class,()->new TransactionTemplate(transactions).execute(tx->{ settings.save(nodeId,r); return null; }));
        jdbc.update("UPDATE vls_compute_node SET resource_scope='PLATFORM' WHERE id=?",nodeId); TenantContextHolder.setTenantId("tenant-b");
        assertThrows(RuntimeException.class,()->new TransactionTemplate(transactions).execute(tx->{ settings.save(nodeId,r); return null; }));
    }
    @Test void aProbeStartedBeforePowerCompletionCannotOverwriteTheNewLifecycleRevision() throws Exception {
        when(connection.execute(anyString(),anyInt())).thenAnswer(call->{
            jdbc.update("UPDATE vls_compute_node SET power_state='STOPPED',probe_state='UNTESTED',probe_time=NULL,lifecycle_revision=lifecycle_revision+1 WHERE id=?",nodeId);
            return "VLS_PROBE={\"contractVersion\":1,\"ready\":true,\"gpuAvailable\":true}";
        });
        ComputeNode result=nodes.probe(nodeId); assertEquals("STOPPED",result.getPowerState()); assertEquals("UNTESTED",result.getProbeState());
    }
    @Test void publishedConsumerRefreshUsesFixedPinnedCheckWithoutLeakingOwnerDetailsAndCachesSuccess() throws Exception {
        publicFixture();
        jdbc.update("UPDATE vls_compute_node SET resource_scope='PLATFORM',probe_time=DATE_SUB(NOW(),INTERVAL 6 MINUTE) WHERE id=?",nodeId);
        TenantContextHolder.setTenantId("tenant-b");
        when(connection.execute(anyString(),eq(180))).thenReturn("VLS_PROBE={\"contractVersion\":1,\"ready\":true,\"gpuAvailable\":true}");
        ComputeHealthRefreshService health=new ComputeHealthRefreshService(jdbc,nodes,transactions);
        assertTrue(health.refresh(nodeId).contains("通过")); assertEquals("tenant-b",ComputeNodeService.tenant());
        assertTrue(health.refresh(nodeId).contains("五分钟")); verify(ssh,times(1)).open(any());
        String publicResult=json.writeValueAsString(new ComputeMarketService(jdbc,nodes).list(false));
        assertFalse(publicResult.contains("host")); assertFalse(publicResult.contains("password")); assertFalse(publicResult.contains("encrypted-test-secret"));
        assertEquals("READY",new ComputeMarketService(jdbc,nodes).list(false).get(0).getHealthState());
    }
    @Test void consumerRefreshRejectsPrivateStoppedUnknownUnpinnedAndPendingResourcesWithoutSsh() {
        ComputeHealthRefreshService health=new ComputeHealthRefreshService(jdbc,nodes,transactions);
        TenantContextHolder.setTenantId("tenant-b"); assertThrows(RuntimeException.class,()->health.refresh(nodeId));
        jdbc.update("UPDATE vls_compute_node SET resource_scope='PLATFORM' WHERE id=?",nodeId);
        for(String state:Arrays.asList("STOPPED","UNKNOWN","STARTING","STOPPING")) {
            jdbc.update("UPDATE vls_compute_node SET power_state=? WHERE id=?",state,nodeId);
            assertThrows(RuntimeException.class,()->health.refresh(nodeId));
        }
        jdbc.update("UPDATE vls_compute_node SET power_state='RUNNING',host_key=NULL WHERE id=?",nodeId);
        assertThrows(RuntimeException.class,()->health.refresh(nodeId));
        jdbc.update("UPDATE vls_compute_node SET host_key='test-key',power_operation_id='pending' WHERE id=?",nodeId);
        assertThrows(RuntimeException.class,()->health.refresh(nodeId)); verifyNoInteractions(ssh);
    }
    @Test void failedConsumerRefreshIsThrottledAcrossSiblingCardsAndUnpublishDiscardsItsLateResult() throws Exception {
        ComputeRequests.Node second=request(); second.setGpuIndex(1); Long other=nodes.save(null,second).getId();
        publicFixture();
        jdbc.update("UPDATE vls_compute_node SET resource_scope='PLATFORM',power_state='RUNNING',host_key='test-key',probe_time=NULL WHERE id IN (?,?)",nodeId,other);
        TenantContextHolder.setTenantId("tenant-b"); ComputeHealthRefreshService health=new ComputeHealthRefreshService(jdbc,nodes,transactions);
        when(connection.execute(anyString(),eq(180))).thenReturn("VLS_PROBE={\"contractVersion\":1,\"ready\":false}");
        assertTrue(health.refresh(nodeId).contains("未通过")); assertThrows(RuntimeException.class,()->health.refresh(nodeId)); assertThrows(RuntimeException.class,()->health.refresh(other));
        verify(ssh,times(1)).open(any());
        jdbc.update("UPDATE vls_compute_node SET probe_requested_at=DATE_SUB(NOW(),INTERVAL 2 MINUTE) WHERE id IN (?,?)",nodeId,other);
        when(connection.execute(anyString(),eq(180))).thenAnswer(call->{ jdbc.update("UPDATE vls_compute_node SET resource_scope='PRIVATE' WHERE id=?",nodeId); return "VLS_PROBE={\"contractVersion\":1,\"ready\":true}"; });
        assertTrue(health.refresh(nodeId).contains("丢弃")); assertNotEquals("READY",jdbc.queryForObject("SELECT probe_state FROM vls_compute_node WHERE id=?",String.class,nodeId));
    }
    @Test void consumerRefreshDoesNotProbeAnActiveTrainingServer() {
        start(); jdbc.update("UPDATE vls_compute_node SET resource_scope='PLATFORM',probe_time=DATE_SUB(NOW(),INTERVAL 6 MINUTE) WHERE id=?",nodeId);
        TenantContextHolder.setTenantId("tenant-b"); ComputeHealthRefreshService health=new ComputeHealthRefreshService(jdbc,nodes,transactions);
        assertThrows(RuntimeException.class,()->health.refresh(nodeId)); verifyNoInteractions(ssh);
    }
    @Test void powerWorkerCoordinatesOnlyPreboundUsageWithUnknownRunningAndStoppedObservationsWithoutDebit() {
        ComputePowerProvider provider=mock(ComputePowerProvider.class); when(provider.supports(any())).thenReturn(true);
        ComputeMarketService market=new ComputeMarketService(jdbc,nodes); ReflectionTestUtils.setField(market,"powerProviders",Collections.singletonList(provider));
        jdbc.update("UPDATE vls_compute_node SET power_state='STOPPED',control_mode='API' WHERE id=?",nodeId);
        new TransactionTemplate(transactions).execute(tx->market.power(nodeId,"start")); String operation=nodes.get(nodeId).getPowerOperationId();
        ComputeBillingLedgerService ledger=new ComputeBillingLedgerService(jdbc,json);
        String usage=new TransactionTemplate(transactions).execute(tx->ledger.reserve(ComputeBillingLedgerService.serverKey(nodeId),"tenant-a","test-vls-opener","test-only-payer",billingRate(),false,operation));
        ComputePowerWorker power=new ComputePowerWorker(jdbc); ReflectionTestUtils.setField(power,"providers",Collections.singletonList(provider)); ReflectionTestUtils.setField(power,"billing",ledger);
        when(provider.status(any())).thenReturn(new ComputePowerProvider.Snapshot("UNKNOWN","UNKNOWN","test ambiguous")); power.scan();
        assertEquals("RECONCILE",jdbc.queryForObject("SELECT state FROM vls_compute_usage WHERE id=?",String.class,usage));
        assertNull(jdbc.queryForObject("SELECT billing_start_at FROM vls_compute_usage WHERE id=?",java.sql.Timestamp.class,usage));
        when(provider.status(any())).thenReturn(new ComputePowerProvider.Snapshot("RUNNING","UNKNOWN","test running")); power.scan();
        assertEquals("RUNNING",jdbc.queryForObject("SELECT state FROM vls_compute_usage WHERE id=?",String.class,usage));
        jdbc.update("UPDATE vls_compute_node SET provider='aliyun',provider_product='ECS',requested_stopped_mode='StopCharging' WHERE id=?",nodeId);
        new TransactionTemplate(transactions).execute(tx->market.power(nodeId,"stop"));
        ComputePowerProvider.Snapshot stopped=new ComputePowerProvider.Snapshot("STOPPED","UNKNOWN","test stopped"); stopped.stoppedMode="StopCharging"; stopped.chargeType="PostPaid";
        when(provider.status(any())).thenReturn(new ComputePowerProvider.Snapshot("RUNNING","UNKNOWN","test preflight"),stopped); power.scan();
        assertEquals("STOPPED",jdbc.queryForObject("SELECT state FROM vls_compute_usage WHERE id=?",String.class,usage));
        assertEquals("OBSERVATION",jdbc.queryForObject("SELECT stop_evidence FROM vls_compute_usage WHERE id=?",String.class,usage));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_charge",Integer.class)); verify(provider,times(2)).submit(any(),anyString(),anyString());
    }
    @Test void billingUsesInstanceSecondsDoubleActualCostAndCarriesFractionalQuotaAcrossUsageWithoutTickRounding() {
        ComputeBillingLedgerService ledger=new ComputeBillingLedgerService(jdbc,json); ComputeBillingRate rate=billingRate();
        String first=closedUsage(ledger,rate,"start-one",90); String key=new TransactionTemplate(transactions).execute(tx->ledger.prepareSettlement(first));
        assertEquals("0.05",jdbc.queryForObject("SELECT unrounded_text FROM vls_compute_charge WHERE charge_key=?",String.class,key));
        assertEquals("0.05",jdbc.queryForObject("SELECT value_text FROM vls_compute_billing_remainder",String.class));
        assertEquals(key,new TransactionTemplate(transactions).execute(tx->ledger.prepareSettlement(first)));
        assertEquals("0.05",jdbc.queryForObject("SELECT value_text FROM vls_compute_billing_remainder",String.class));
        String next=closedUsage(ledger,rate,"start-two",3690); new TransactionTemplate(transactions).execute(tx->ledger.prepareSettlement(next));
        assertEquals("0.10",jdbc.queryForObject("SELECT value_text FROM vls_compute_billing_remainder",String.class));
        assertEquals("2",jdbc.queryForObject("SELECT amount_text FROM vls_compute_charge WHERE usage_id=?",String.class,next));
    }
    @Test void billingTimeoutSurvivesRestartAndLooksUpTheSameKeyInsteadOfDebitingTwice() {
        ComputeBillingLedgerService ledger=new ComputeBillingLedgerService(jdbc,json); String usage=closedUsage(ledger,billingRate(),"start-test",3600);
        String key=new TransactionTemplate(transactions).execute(tx->ledger.prepareSettlement(usage)); ComputeBillingGateway gateway=mock(ComputeBillingGateway.class);
        when(gateway.availableQuota(anyString(),anyString())).thenReturn(new java.math.BigDecimal("100")); when(gateway.debit(anyString(),anyString(),any(),anyString())).thenThrow(new IllegalStateException("test-only timeout"));
        ledger.dispatch(key,gateway); assertEquals("UNKNOWN",jdbc.queryForObject("SELECT state FROM vls_compute_charge WHERE charge_key=?",String.class,key));
        when(gateway.lookup("mock-newapi-user","compute-usage-"+usage)).thenReturn(ComputeBillingGateway.Outcome.SUCCEEDED);
        new ComputeBillingLedgerService(jdbc,json).dispatch(key,gateway); new ComputeBillingLedgerService(jdbc,json).dispatch(key,gateway);
        verify(gateway,times(1)).debit(eq("mock-newapi-user"),eq("mock-quota-unit"),eq(new java.math.BigDecimal("2")),eq(key)); verify(gateway,times(1)).lookup("mock-newapi-user",key);
        assertEquals("SETTLED",jdbc.queryForObject("SELECT state FROM vls_compute_charge WHERE charge_key=?",String.class,key));
    }
    @Test void freeGpuAndFailedStartupNeverDebitAndUnconfirmedCostCannotActivatePaidUsage() {
        ComputeBillingLedgerService ledger=new ComputeBillingLedgerService(jdbc,json); ComputeBillingRate rate=billingRate();
        assertNull(ledger.reserve("server","tenant-a",null,null,null,true,"free")); assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_usage",Integer.class));
        String usage=new TransactionTemplate(transactions).execute(tx->ledger.reserve("server","tenant-a","test-vls-opener","mock-newapi-user",rate,false,"failed-start")); ledger.startFailed(usage);
        assertThrows(RuntimeException.class,()->new TransactionTemplate(transactions).execute(tx->ledger.prepareSettlement(usage)));
        rate.setCostSource("REFERENCE_QUOTE"); assertThrows(RuntimeException.class,()->ledger.reserve("server","tenant-a","test-vls-opener","mock-newapi-user",rate,false,"quote-start"));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_charge",Integer.class));
    }
    @Test void paidServerCannotSilentlyChangePayerOrHaveTwoConcurrentExclusiveUsages() {
        ComputeBillingLedgerService ledger=new ComputeBillingLedgerService(jdbc,json); ComputeBillingRate rate=billingRate();
        new TransactionTemplate(transactions).execute(tx->ledger.reserve("server","tenant-a","test-vls-opener","mock-newapi-user",rate,false,"start-payer"));
        assertThrows(RuntimeException.class,()->new TransactionTemplate(transactions).execute(tx->ledger.reserve("server","tenant-b","another-vls-opener","other-user",rate,false,"start-payer")));
        assertThrows(RuntimeException.class,()->new TransactionTemplate(transactions).execute(tx->ledger.reserve("server","tenant-a","another-vls-opener","other-user",rate,false,"other-start")));
        assertThrows(RuntimeException.class,()->new TransactionTemplate(transactions).execute(tx->ledger.reserve("server","tenant-a","another-vls-opener","mock-newapi-user",rate,false,"start-payer")));
        assertThrows(RuntimeException.class,()->ledger.reserve("another-server","tenant-a",null,"mock-newapi-user",rate,false,"missing-opener"));
    }
    @Test void openingUserPaysPhysicalInstanceIdleTimeAndSettlementSurvivesLogoutWithoutBrowserCredentials() {
        ComputeBillingLedgerService ledger=new ComputeBillingLedgerService(jdbc,json);
        String usage=closedUsage(ledger,billingRate(),"idle-whole-instance",3600);
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_cloud_training_job",Integer.class));
        assertEquals("test-vls-opener",jdbc.queryForObject("SELECT opening_user_id FROM vls_compute_usage WHERE id=?",String.class,usage));
        TenantContextHolder.clear(); // no active browser/session, backend outbox uses only immutable trusted account binding
        String key=new TransactionTemplate(transactions).execute(tx->new ComputeBillingLedgerService(jdbc,json).prepareSettlement(usage));
        assertEquals("2",jdbc.queryForObject("SELECT amount_text FROM vls_compute_charge WHERE charge_key=?",String.class,key));
        ComputeBillingGateway gateway=mock(ComputeBillingGateway.class);
        when(gateway.availableQuota(anyString(),anyString())).thenReturn(new java.math.BigDecimal("100"));
        when(gateway.debit(anyString(),anyString(),any(),anyString())).thenReturn(ComputeBillingGateway.Outcome.SUCCEEDED);
        new ComputeBillingLedgerService(jdbc,json).dispatch(key,gateway);
        verify(gateway).debit(eq("mock-newapi-user"),eq("mock-quota-unit"),eq(new java.math.BigDecimal("2")),eq(key));
        assertEquals("SETTLED",jdbc.queryForObject("SELECT state FROM vls_compute_charge WHERE charge_key=?",String.class,key));
    }
    @Test void legacyUsageWithoutTrustedOpenerRequiresReconciliationAndCannotSettle() {
        ComputeBillingLedgerService ledger=new ComputeBillingLedgerService(jdbc,json);
        String usage=closedUsage(ledger,billingRate(),"legacy-no-opener",3600);
        jdbc.update("UPDATE vls_compute_usage SET opening_user_id=NULL WHERE id=?",usage);
        assertThrows(RuntimeException.class,()->new TransactionTemplate(transactions).execute(tx->ledger.prepareSettlement(usage)));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_charge",Integer.class));
    }
    @Test void paidStartRequiresVerifiedPlatformButVerifiedIdentityStillCannotActivateBillingAndOwnerStopWorksWithoutIt() {
        jdbc.update("UPDATE vls_compute_node SET resource_scope='PLATFORM',billing_type='POINTS',power_state='STOPPED' WHERE id=?",nodeId);
        ComputeMarketService market=new ComputeMarketService(jdbc,nodes);
        assertThrows(RuntimeException.class,()->new TransactionTemplate(transactions).execute(tx->market.power(nodeId,"start")));
        ComputePlatformAuthorization auth=mock(ComputePlatformAuthorization.class); ReflectionTestUtils.setField(nodes,"platformAuthorization",auth);
        ComputePlatformAuthorization.Context context=new ComputePlatformAuthorization.Context(); context.setAuthorized(true); when(auth.requireIdentity()).thenReturn(context);
        RuntimeException blocked=assertThrows(RuntimeException.class,()->new TransactionTemplate(transactions).execute(tx->market.power(nodeId,"start")));
        assertTrue(blocked.getMessage().contains("New API")); assertNull(nodes.get(nodeId).getPowerOperationId());
        clearInvocations(auth); jdbc.update("UPDATE vls_compute_node SET power_state='RUNNING' WHERE id=?",nodeId);
        assertThrows(RuntimeException.class,()->new TransactionTemplate(transactions).execute(tx->market.power(nodeId,"stop"))); verifyNoInteractions(auth);
    }
    private ComputeBillingRate billingRate() {
        ComputeBillingRate rate=new ComputeBillingRate(); rate.setVersion("test-rate-v1"); rate.setCostSource("VERIFIED_MANUAL"); rate.setCurrency("CNY"); rate.setCostScope("INSTANCE_COMPUTE"); rate.setCostUnit("PER_INSTANCE_HOUR"); rate.setCostHourly(java.math.BigDecimal.ONE); rate.setValidFrom("2026-01-01T00:00:00Z"); rate.setValidUntil("2027-01-01T00:00:00Z"); rate.setQuotaUnit("mock-quota-unit"); rate.setConversionVersion("test-conversion-v1"); rate.setQuotaPerCurrencyUnit(java.math.BigDecimal.ONE); rate.setQuotaScale(0); rate.setQuotaRounding(java.math.RoundingMode.DOWN);
        rate.setPricePolicy("LOCKED_FOR_RUN"); rate.setProviderPriceLockConfirmed(true); rate.setPriceEvidence("TEST_ONLY_VERIFIED_LOCK_CONTRACT"); return rate;
    }
    @Test void ordinaryEcsStoppedModeKeepsUsageUnresolvedAndActualEconomicalModeEndsOnlyComputePeriod() {
        ComputeBillingLedgerService ledger=new ComputeBillingLedgerService(jdbc,json);
        String usage=new TransactionTemplate(transactions).execute(tx->ledger.reserve(ComputeBillingLedgerService.serverKey(nodeId),"tenant-a","test-local","test-sso",billingRate(),false,"test-stop-mode"));
        java.time.Instant start=java.time.Instant.now().minusSeconds(90); ledger.confirmedRunning(usage,start,start,"OBSERVATION");
        jdbc.update("UPDATE vls_compute_node SET provider='aliyun',provider_product='ECS',power_operation_action='stop' WHERE id=?",nodeId);
        ComputePowerProvider.Snapshot stopped=new ComputePowerProvider.Snapshot("STOPPED","UNKNOWN","test stopped"); stopped.chargeType="PostPaid";
        for(String mode:new String[]{"KeepCharging",null}) {
            stopped.stoppedMode=mode; new TransactionTemplate(transactions).execute(tx->{ledger.observePower(nodes.get(nodeId),stopped,nodeId); return null;});
            assertEquals("RECONCILE",jdbc.queryForObject("SELECT state FROM vls_compute_usage WHERE id=?",String.class,usage));
            assertNull(jdbc.queryForObject("SELECT billing_end_at FROM vls_compute_usage WHERE id=?",java.sql.Timestamp.class,usage));
            assertNotNull(jdbc.queryForObject("SELECT active_server_key FROM vls_compute_usage WHERE id=?",String.class,usage));
            assertThrows(RuntimeException.class,()->new TransactionTemplate(transactions).execute(tx->ledger.prepareSettlement(usage)));
        }
        stopped.stoppedMode="StopCharging"; new TransactionTemplate(transactions).execute(tx->{ledger.observePower(nodes.get(nodeId),stopped,nodeId); return null;});
        assertEquals("STOPPED",jdbc.queryForObject("SELECT state FROM vls_compute_usage WHERE id=?",String.class,usage));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_charge",Integer.class));
    }
    @Test void persistedRunPriceSnapshotCannotBeRewrittenByAFuturePriceObservation() {
        ComputeBillingLedgerService ledger=new ComputeBillingLedgerService(jdbc,json); ComputeBillingRate firstRate=billingRate();
        String usage=closedUsage(ledger,firstRate,"test-frozen-run-price",90); firstRate.setCostHourly(new java.math.BigDecimal("9")); firstRate.setVersion("new-rate");
        String key=new TransactionTemplate(transactions).execute(tx->ledger.prepareSettlement(usage));
        assertEquals("0.05",jdbc.queryForObject("SELECT unrounded_text FROM vls_compute_charge WHERE charge_key=?",String.class,key));
        assertTrue(jdbc.queryForObject("SELECT rate_snapshot FROM vls_compute_usage WHERE id=?",String.class,usage).contains("test-rate-v1"));
    }
    private String closedUsage(ComputeBillingLedgerService ledger,ComputeBillingRate rate,String op,long seconds) {
        String usage=new TransactionTemplate(transactions).execute(tx->ledger.reserve("server","tenant-a","test-vls-opener","mock-newapi-user",rate,false,op));
        java.time.Instant start=java.time.Instant.parse("2026-10-10T00:00:00Z"), end=start.plusSeconds(seconds);
        ledger.confirmedRunning(usage,start,start,"OBSERVATION"); new TransactionTemplate(transactions).execute(tx->{ ledger.confirmedStopped(usage,end,end,"OBSERVATION"); return null; }); return usage;
    }
    @Test void powerStockFailureAppliesToEveryGpuOnTheServerAndDoesNotClaimItIsRunning() {
        ComputeRequests.Node second=request(); second.setGpuIndex(1); Long other=nodes.save(null,second).getId();
        jdbc.update("UPDATE vls_compute_node SET power_operation_id='test-op',power_operation_action='start',power_operation_started_at=NOW() WHERE id=?",nodeId);
        new ComputePowerWorker(jdbc).recordFailure(nodes.get(nodeId),new ComputeProviderException(ComputeProviderException.Kind.NO_STOCK));
        assertEquals("UNAVAILABLE",nodes.get(other).getCapacityState()); assertTrue(new ComputeMarketService(jdbc,nodes).list(true).isEmpty());
    }
    @Test void rrStopWaitingForAnotherCardSeesJustCommittedJobInsteadOfOldSnapshot() throws Exception {
        ComputeRequests.Node second=request(); second.setGpuIndex(1); Long other=nodes.save(null,second).getId();
        java.util.concurrent.CountDownLatch held=new java.util.concurrent.CountDownLatch(1), snapshot=new java.util.concurrent.CountDownLatch(1), inserted=new java.util.concurrent.CountDownLatch(1), waiting=new java.util.concurrent.CountDownLatch(1), commit=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.ExecutorService threads=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Future<?> starter=threads.submit(()-> { TenantContextHolder.setTenantId("tenant-a"); try { rr().execute(tx->{ nodes.lockServer(nodeId); held.countDown(); await(snapshot); ComputeRequests.Start r=new ComputeRequests.Start(); r.setNodeId(nodeId); r.setDatasetId(3L); training.start(1L,r); inserted.countDown(); await(commit); return null; }); } finally { TenantContextHolder.clear(); } });
            java.util.concurrent.Future<String> stopper=threads.submit(()-> { TenantContextHolder.setTenantId("tenant-a"); try { await(held); return rr().execute(tx->{ nodes.get(other); snapshot.countDown(); await(inserted); waiting.countDown(); try { return new ComputeMarketService(jdbc,nodes).power(other,"stop"); } catch (com.ruoyi.common.exception.ServiceException e) { return e.getMessage(); } }); } finally { TenantContextHolder.clear(); } });
            await(waiting); assertThrows(java.util.concurrent.TimeoutException.class,()->stopper.get(200,java.util.concurrent.TimeUnit.MILLISECONDS)); commit.countDown(); starter.get(10,java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(stopper.get(10,java.util.concurrent.TimeUnit.SECONDS).contains("任务")); assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_compute_node WHERE power_operation_id IS NOT NULL",Integer.class));
        } finally { commit.countDown(); threads.shutdownNow(); }
    }
    @Test void rrTrainingWaitingForPowerCommitSeesOperationAcrossCardsAndDoesNotEnqueue() throws Exception {
        ComputeRequests.Node second=request(); second.setGpuIndex(1); Long other=nodes.save(null,second).getId();
        jdbc.update("UPDATE vls_compute_node SET power_state='STOPPED',control_mode='API' WHERE id=?",nodeId);
        jdbc.update("UPDATE vls_compute_node SET probe_state='READY',probe_json=?,probe_time=NOW(),power_state='RUNNING' WHERE id=?","{\"contractVersion\":1,\"ready\":true}",other);
        ComputePowerProvider provider=mock(ComputePowerProvider.class); when(provider.supports(any())).thenReturn(true); ComputeMarketService market=new ComputeMarketService(jdbc,nodes); ReflectionTestUtils.setField(market,"powerProviders",Collections.singletonList(provider));
        java.util.concurrent.CountDownLatch held=new java.util.concurrent.CountDownLatch(1), snapshot=new java.util.concurrent.CountDownLatch(1), operated=new java.util.concurrent.CountDownLatch(1), waiting=new java.util.concurrent.CountDownLatch(1), commit=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.ExecutorService threads=java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Future<?> power=threads.submit(()-> { TenantContextHolder.setTenantId("tenant-a"); try { rr().execute(tx->{ nodes.lockServer(nodeId); held.countDown(); await(snapshot); market.power(nodeId,"start"); operated.countDown(); await(commit); return null; }); } finally { TenantContextHolder.clear(); } });
            java.util.concurrent.Future<String> train=threads.submit(()-> { TenantContextHolder.setTenantId("tenant-a"); try { await(held); return rr().execute(tx->{ nodes.get(other); snapshot.countDown(); await(operated); waiting.countDown(); ComputeRequests.Start r=new ComputeRequests.Start(); r.setNodeId(other); r.setDatasetId(3L); try { training.start(1L,r); return "wrongly-enqueued"; } catch(com.ruoyi.common.exception.ServiceException e) { return e.getMessage(); } }); } finally { TenantContextHolder.clear(); } });
            await(waiting); assertThrows(java.util.concurrent.TimeoutException.class,()->train.get(200,java.util.concurrent.TimeUnit.MILLISECONDS)); commit.countDown(); power.get(10,java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(train.get(10,java.util.concurrent.TimeUnit.SECONDS).contains("尚未确认")); assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_cloud_training_job",Integer.class));
        } finally { commit.countDown(); threads.shutdownNow(); }
    }
    private TransactionTemplate rr() { TransactionTemplate tx=new TransactionTemplate(transactions); tx.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ); return tx; }
    private static void await(java.util.concurrent.CountDownLatch latch) { try { assertTrue(latch.await(10,java.util.concurrent.TimeUnit.SECONDS),"concurrency checkpoint timed out"); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); } }
    ComputeRequests.Node request() {
        ComputeRequests.Node request = new ComputeRequests.Node(); request.setName("test-instance"); request.setHost("test.autodl.com"); request.setPassword("test-only"); return request;
    }
    String start() {
        ComputeRequests.Start request = new ComputeRequests.Start(); request.setNodeId(nodeId); request.setDatasetId(3L); request.setEpochs(1);
        new TransactionTemplate(transactions).execute(tx -> training.start(1L, request));
        task.setConfigParams(jdbc.queryForObject("SELECT config_params FROM vls_algorithm_training WHERE id=1", String.class));
        return training.current(task).getId();
    }
    @Test void migrationQueueFreezesSnapshotWithoutDefaultGpuPathAndRejectsDuplicateStart() {
        String job = start();
        assertEquals("QUEUED", training.current(task).getJobState());
        assertEquals("not_required", jdbc.queryForObject("SELECT onnx_conversion_status FROM vls_algorithm_training WHERE id=1", String.class));
        assertTrue(training.current(task).getRunDir().endsWith(job));
        assertThrows(RuntimeException.class, this::start);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM vls_cloud_training_job", Integer.class));
        verifyNoInteractions(ssh);
    }
    @Test void tenantCannotReadChangeProbeOrStartAnotherTenantsInstanceOrJob() {
        start(); TenantContextHolder.setTenantId("tenant-b");
        assertTrue(nodes.list().isEmpty());
        assertThrows(RuntimeException.class, () -> nodes.get(nodeId));
        assertThrows(RuntimeException.class, () -> nodes.probe(nodeId));
        assertThrows(RuntimeException.class, () -> nodes.save(nodeId, request()));
        assertThrows(RuntimeException.class, () -> nodes.delete(nodeId));
        assertThrows(RuntimeException.class, () -> training.current(task));
        assertThrows(RuntimeException.class, this::start);
        verifyNoInteractions(ssh);
    }
    @Test void legacyProbeRequiresARecheckAndStructuredProbeIsParsedWithoutDroppingTheJsonBrace() throws Exception {
        jdbc.update("UPDATE vls_compute_node SET probe_json='{}' WHERE id=?", nodeId);
        assertThrows(RuntimeException.class, () -> nodes.requireReady(nodeId));
        when(connection.hostKey()).thenReturn("pinned-key");
        when(connection.execute(anyString(), eq(180))).thenReturn("startup message\nVLS_PROBE={\"contractVersion\":1,\"ready\":true,\"checks\":[]}");
        assertEquals("READY", nodes.probe(nodeId).getProbeState());
        assertDoesNotThrow(() -> nodes.requireReady(nodeId));
        when(connection.execute(anyString(), eq(180))).thenReturn("VLS_PROBE={\"contractVersion\":1,\"ready\":false,\"message\":\"missing preset\"}");
        assertEquals("ERROR", nodes.probe(nodeId).getProbeState());
        assertThrows(RuntimeException.class, () -> nodes.requireReady(nodeId));
    }
    @Test void queuedCancellationDoesNotRequireAnOnlineInstanceAndActiveNodesCannotBeEdited() {
        start(); assertThrows(RuntimeException.class, () -> nodes.save(nodeId, request())); assertThrows(RuntimeException.class, () -> nodes.delete(nodeId));
        assertThrows(RuntimeException.class, () -> training.assertInactive(task));
        training.stop(task); worker.tick(nodeId);
        assertEquals("CANCELLED", training.current(task).getJobState()); verifyNoInteractions(ssh);
        assertDoesNotThrow(() -> nodes.delete(nodeId));
    }
    @Test void downloadedBytesMustMatchTheRemoteHashBeforeCompletion() throws Exception {
        start();
        when(connection.execute(startsWith("test -s "), anyInt())).thenReturn("READY");
        when(connection.execute(contains(" status "), anyInt())).thenReturn("VLS_CLOUD={\"state\":\"SUCCEEDED\",\"sha256\":\"" + ModelArtifactObjectStore.hash("data") + "\",\"size\":4}");
        doAnswer(call -> { Files.write(call.getArgument(1), "evil".getBytes(StandardCharsets.UTF_8)); return null; }).when(connection).download(anyString(), any(Path.class));
        Path tempRoot = root.resolve("codex/autodl/tmp"); Files.createDirectories(tempRoot);
        when(storage.archive(anyString(), anyString(), any())).thenAnswer(call -> {
            Path file = Files.createTempFile(tempRoot, "verify-", ".part");
            try { ((ModelArtifactObjectStore.Source) call.getArgument(2)).write(file); return true; } finally { Files.deleteIfExists(file); }
        });
        worker.tick(nodeId);
        assertEquals("ARCHIVING", training.current(task).getJobState());
        assertNull(jdbc.queryForObject("SELECT model_output_path FROM vls_algorithm_training WHERE id=1", String.class));
        doAnswer(call -> { Files.write(call.getArgument(1), "data".getBytes(StandardCharsets.UTF_8)); return null; }).when(connection).download(anyString(), any(Path.class));
        worker.tick(nodeId);
        assertEquals("COMPLETED", training.current(task).getJobState());
        AlgorithmTraining update = new AlgorithmTraining(); update.setConfigParams("{}");
        training.preserveExecutionConfig(task, update);
        assertTrue(CloudTrainingService.isCloud(update));
    }
    @Test void disconnectKeepsJobActiveRatherThanClaimingTrainingStopped() throws Exception {
        start(); when(ssh.open(any())).thenThrow(new IOException("offline")); worker.tick(nodeId);
        assertEquals("PREPARING", training.current(task).getJobState());
        assertNotNull(training.current(task).getMessage()); assertThrows(RuntimeException.class, () -> nodes.delete(nodeId));
    }
    @Test void completionWaitsForBothVerifiedArtifactsAndLogsRemainServerAuthoritative() throws Exception {
        start();
        when(connection.execute(startsWith("test -s "), anyInt())).thenReturn("READY");
        when(connection.execute(contains(" status "), anyInt())).thenReturn("VLS_CLOUD={\"state\":\"SUCCEEDED\",\"logs\":\"Training complete\",\"epoch\":1,\"sha256\":\"" + String.join("", Collections.nCopies(64, "a")) + "\",\"size\":4}");
        when(storage.archive(anyString(), anyString(), any())).thenReturn(false);
        worker.tick(nodeId); assertEquals("ARCHIVING", training.current(task).getJobState());
        assertFalse(training.logs(task).isCompleted()); assertTrue(training.logs(task).isServerManaged());
        assertNull(jdbc.queryForObject("SELECT model_output_path FROM vls_algorithm_training WHERE id=1", String.class));
        when(storage.archive(anyString(), anyString(), any())).thenReturn(true);
        worker.tick(nodeId); assertEquals("COMPLETED", training.current(task).getJobState());
        assertEquals(training.current(task).modelKey(), jdbc.queryForObject("SELECT model_output_path FROM vls_algorithm_training WHERE id=1", String.class));
        verify(storage, atLeastOnce()).archive(endsWith(".vls-classes.yaml"), eq("data.yaml"), any());
        verify(storage).archive(endsWith("/weights/best.pt"), eq("best.pt"), any());
    }
}
