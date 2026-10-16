package com.ruoyi.vlstream.test.vlstream.compute;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.servlet.model.SaRequestForServlet;
import cn.dev33.satoken.servlet.model.SaResponseForServlet;
import cn.dev33.satoken.servlet.model.SaStorageForServlet;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.service.ModelArtifactObjectStore;
import com.ruoyi.vlstream.test.vlstream.controller.VlsAlgorithmTrainingController;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmTrainingStatusEnum;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmTrainingMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmTrainingVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAlgorithmTrainingService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAlgorithmService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsContainerInstanceService;
import com.ruoyi.vlstream.test.vlstream.service.RemoteTrainingService;
import com.ruoyi.vlstream.test.vlstream.service.TrainingPublicationService;
import com.ruoyi.vlstream.test.vlstream.service.impl.VlsAlgorithmTrainingServiceImpl;
import com.ruoyi.vlstream.test.vlstream.data.DataManagementService;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyLong;

/** Actual MySQL permission queries in a separate synthetic schema, with no SSH or business data. */
@Tag("dev")
@Execution(ExecutionMode.SAME_THREAD)
@EnabledIfEnvironmentVariable(named = "VLS_SHARED_GPU_TEST_JDBC", matches = "jdbc:mysql://127\\.0\\.0\\.1:333(?:29|39)/shared_gpu_queue_contract_test\\?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC")
class ComputeSharedQueueIntegrationTest {
    // 33339 permits a separate owned fixture while another review owns 33329.
    private static final String JDBC = "jdbc:mysql://127.0.0.1:"
        +(String.valueOf(System.getenv("VLS_SHARED_GPU_TEST_JDBC")).contains(":33339/")?"33339":"33329")
        +"/shared_gpu_queue_contract_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String SCHEMA = "shared_gpu_queue_contract_test";
    private static final String FIXTURE = "ComputeSharedQueueIntegrationTest-v1";
    private static final String SHARED = "{\"sharedQueue\":{\"mode\":\"SHARED_GPU_QUEUE\"}}";
    private static final List<String> TABLES = Arrays.asList("vls_cloud_training_job", "vls_compute_node", "vls_algorithm_model", "vls_container_instance", "vls_algorithm_training", "vls_smart_annotation_round", "vls_smart_annotation_task");
    private static JdbcTemplate jdbc;
    private final AtomicReference<String> platformUser = new AtomicReference<>("platform-alice");
    private final ComputeSharedQueueProperties properties = new ComputeSharedQueueProperties();
    private ComputeSharedQueueService shared;
    private ComputePublicTaskAccess access;
    private MyTrainingTasksService mine;
    private ComputePlatformAuthorization authorization;
    private ComputePublicInventoryService inventory;
    private SaTokenContext previousContext;
    private SaTokenDao previousDao;
    private SaTokenDaoDefaultImpl memory;

    @BeforeAll static void database() throws Exception {
        assertEquals(JDBC, System.getenv("VLS_SHARED_GPU_TEST_JDBC"), "Only the exact disposable loopback fixture is allowed");
        DriverManagerDataSource adminSource = new DriverManagerDataSource(
            JDBC.replace("/shared_gpu_queue_contract_test?","/mysql?"), "root", "autodl-test-only");
        JdbcTemplate admin = new JdbcTemplate(adminSource);
        assertTrue(admin.queryForObject("SELECT VERSION()", String.class).startsWith("8.0."));
        boolean exists = admin.queryForObject("SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name=?", Integer.class, SCHEMA) != 0;
        if (exists) {
            assertEquals(1, admin.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=? AND table_name='fixture_guard'", Integer.class, SCHEMA), "Never reuse an unmarked existing schema");
            assertEquals(FIXTURE, admin.queryForObject("SELECT fixture_name FROM shared_gpu_queue_contract_test.fixture_guard", String.class));
        } else {
            admin.execute("CREATE DATABASE shared_gpu_queue_contract_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
        }
        jdbc = new JdbcTemplate(new DriverManagerDataSource(JDBC, "root", "autodl-test-only"));
        assertEquals(SCHEMA, jdbc.queryForObject("SELECT DATABASE()", String.class));
        jdbc.execute("CREATE TABLE IF NOT EXISTS fixture_guard(fixture_name VARCHAR(128) PRIMARY KEY)");
        jdbc.update("INSERT IGNORE INTO fixture_guard(fixture_name) VALUES(?)", FIXTURE);
        jdbc.execute("CREATE TABLE IF NOT EXISTS vls_compute_node(id BIGINT PRIMARY KEY,tenant_id VARCHAR(64),name VARCHAR(100),resource_scope VARCHAR(32),public_instance_key VARCHAR(64),scheduling_mode VARCHAR(32),username VARCHAR(64),password_cipher TEXT NOT NULL)");
        if(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=? AND table_name='vls_compute_node' AND column_name='auth_mode'",Integer.class,SCHEMA)==0) {
            Path root=Paths.get(System.getProperty("user.dir")).toAbsolutePath();
            String migrationPath="ruoyi-admin/src/main/resources/db/migration/V1_2_0_043__compute_deployment_ssh_identity.sql";
            while(root!=null && !Files.exists(root.resolve(migrationPath)))root=root.getParent();
            assertNotNull(root);
            byte[] migration=Files.readAllBytes(root.resolve(migrationPath));
            try(java.sql.Connection connection=jdbc.getDataSource().getConnection()) {
                org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,new org.springframework.core.io.support.EncodedResource(new org.springframework.core.io.ByteArrayResource(migration),StandardCharsets.UTF_8));
            }
        }
        jdbc.execute("CREATE TABLE IF NOT EXISTS vls_algorithm_training(id BIGINT PRIMARY KEY,tenant_id VARCHAR(64),create_user VARCHAR(64),task_name VARCHAR(100),progress INT DEFAULT 0,create_time DATETIME DEFAULT CURRENT_TIMESTAMP,is_deleted INT DEFAULT 0,train_status VARCHAR(20),config_params TEXT,algorithm_id BIGINT,dataset_id BIGINT,epoch_total INT DEFAULT 1,epoch_current INT DEFAULT 0,model_output_path TEXT)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS vls_cloud_training_job(id VARCHAR(64) PRIMARY KEY,tenant_id VARCHAR(64),training_id BIGINT,node_id BIGINT,container_record_id BIGINT,public_instance_key VARCHAR(64),public_owner_subject VARCHAR(64),options_json TEXT,job_state VARCHAR(32),annotation_type VARCHAR(32) DEFAULT 'object_detection',log_tail TEXT,run_dir TEXT,epoch_current INT DEFAULT 0,progress INT DEFAULT 0,cancel_requested TINYINT DEFAULT 0,message TEXT,update_time DATETIME)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS vls_container_instance(id BIGINT PRIMARY KEY,tenant_id VARCHAR(64),training_task_id BIGINT,instance_status VARCHAR(32))");
        if(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=? AND table_name='vls_container_instance' AND column_name='create_user'",Integer.class,SCHEMA)==0)
            jdbc.execute("ALTER TABLE vls_container_instance ADD COLUMN create_user VARCHAR(64) NULL");
        if(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=? AND table_name='vls_container_instance' AND column_name='instance_type'",Integer.class,SCHEMA)==0)
            jdbc.execute("ALTER TABLE vls_container_instance ADD COLUMN instance_type VARCHAR(64) NULL");
        if(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=? AND table_name='vls_container_instance' AND column_name='logs_path'",Integer.class,SCHEMA)==0)
            jdbc.execute("ALTER TABLE vls_container_instance ADD COLUMN logs_path TEXT NULL");
        if(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=? AND table_name='vls_container_instance' AND column_name='is_deleted'",Integer.class,SCHEMA)==0)
            jdbc.execute("ALTER TABLE vls_container_instance ADD COLUMN is_deleted INT DEFAULT 0");
        if(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=? AND table_name='vls_container_instance' AND column_name='queue_time'",Integer.class,SCHEMA)==0)
            jdbc.execute("ALTER TABLE vls_container_instance ADD COLUMN queue_time DATETIME NULL");
        jdbc.execute("CREATE TABLE IF NOT EXISTS vls_smart_annotation_task(id BIGINT PRIMARY KEY,tenant_id VARCHAR(64),create_user BIGINT,is_deleted INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS vls_smart_annotation_round(id BIGINT PRIMARY KEY,tenant_id VARCHAR(64),task_id BIGINT,is_deleted INT DEFAULT 0)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS vls_algorithm_model(id BIGINT PRIMARY KEY,tenant_id VARCHAR(64),training_id BIGINT)");
        System.out.println("Shared SQL fixture: " + SCHEMA + ", MySQL " + jdbc.queryForObject("SELECT VERSION()", String.class));
    }

    @BeforeEach void setup() {
        assertEquals(SCHEMA, jdbc.queryForObject("SELECT DATABASE()", String.class));
        assertEquals(FIXTURE, jdbc.queryForObject("SELECT fixture_name FROM fixture_guard", String.class));
        for (String table : TABLES) jdbc.update("DELETE FROM " + table);
        previousContext = SaManager.getSaTokenContext();
        previousDao = SaManager.getSaTokenDao();
        memory = new SaTokenDaoDefaultImpl();
        SaManager.setSaTokenDao(memory);
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        MockHttpServletRequest request = new MockHttpServletRequest();
        SaTokenContextForThreadLocalStorage.setBox(new SaRequestForServlet(request), new SaResponseForServlet(new MockHttpServletResponse()), new SaStorageForServlet(request));
        actor("alice", "tenant-a", "localhost");
        shared = new ComputeSharedQueueService(properties, mock(ComputeTrustedSelfPolicy.class), jdbc, new ObjectMapper());
        authorization = mock(ComputePlatformAuthorization.class);
        when(authorization.requireIdentity()).thenAnswer(call -> {
            if (platformUser.get() == null) throw new ServiceException("Synthetic SSO identity unavailable");
            ComputePlatformAuthorization.Context identity = new ComputePlatformAuthorization.Context();
            identity.setAuthorized(true);
            identity.setPlatformUserId(platformUser.get());
            identity.setVlsUserId(LoginHelper.getUserId());
            identity.setVlsTenantId(ComputeNodeService.tenant());
            return identity;
        });
        inventory = new ComputePublicInventoryService(jdbc);
        ReflectionTestUtils.setField(inventory, "authorization", authorization);
        access = new ComputePublicTaskAccess(jdbc, inventory);
        ReflectionTestUtils.setField(access, "sharedQueue", shared);
        mine = new MyTrainingTasksService(jdbc);
        ReflectionTestUtils.setField(mine, "publicAccess", access);
    }

    @AfterEach void cleanup() {
        org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
        TenantContextHolder.clear();
        SaTokenContextForThreadLocalStorage.clearBox();
        SaManager.setSaTokenDao(previousDao);
        SaManager.setSaTokenContext(previousContext);
        memory.endRefreshThread();
    }

    private void actor(String user, String tenant, String environment) {
        SaHolder.getStorage().set(LoginHelper.USER_KEY, user);
        TenantContextHolder.setTenantId(tenant);
        properties.setEnvironmentId(environment);
    }

    private String nativeOwner(String environment, String tenant, String user) {
        return ModelArtifactObjectStore.hash("company-shared-vls-user\n" + environment + "\n" + tenant + "\n" + user);
    }

    private String ssoOwner(String user) {
        return ModelArtifactObjectStore.hash("trusted-platform-user\n" + user);
    }

    private void job(long id, String scope, String mode, String tenant, String creator, String owner, String options, String state) {
        String instance = "PLATFORM".equals(scope) && ComputeSharedQueueService.EXCLUSIVE.equals(mode) ? ssoOwner("fixture-instance-" + id) : null;
        jdbc.update("INSERT INTO vls_compute_node(id,tenant_id,name,resource_scope,public_instance_key,scheduling_mode) VALUES(?,?,?,?,?,?)", id + 10000, "supplier", "synthetic node " + id, scope, instance, mode);
        jdbc.update("INSERT INTO vls_algorithm_training(id,tenant_id,create_user,task_name,train_status,config_params) VALUES(?,?,?,?,?,?)", id, tenant, creator, "synthetic task " + id, "COMPLETED".equals(state) ? "completed" : "pending", "{\"cloudJobId\":\"fixture-job-" + id + "\"}");
        jdbc.update("INSERT INTO vls_container_instance(id,tenant_id,training_task_id,instance_status) VALUES(?,?,?,?)", id + 20000, tenant, id, "queued");
        jdbc.update("INSERT INTO vls_algorithm_model(id,tenant_id,training_id) VALUES(?,?,?)", id + 30000, tenant, id);
        jdbc.update("INSERT INTO vls_cloud_training_job(id,tenant_id,training_id,node_id,container_record_id,public_instance_key,public_owner_subject,options_json,job_state) VALUES(?,?,?,?,?,?,?,?,?)", "fixture-job-" + id, tenant, id, id + 10000, id + 20000, instance, owner, options, state);
    }

    private void shared(long id, String tenant, String creator, String environment) {
        job(id, "PLATFORM", ComputeSharedQueueService.MODE, tenant, creator, nativeOwner(environment, tenant, creator), SHARED, "QUEUED");
    }

    private void allowed(long id) {
        access.training(id);
        access.model(id + 30000);
        access.container(id + 20000);
    }

    private void denied(long id) {
        assertAll(
            () -> assertThrows(ServiceException.class, () -> access.training(id), "training " + id),
            () -> assertThrows(ServiceException.class, () -> access.model(id + 30000), "model " + id),
            () -> assertThrows(ServiceException.class, () -> access.container(id + 20000), "container " + id));
    }

    private List<Long> filtered(String table, String trainingReference, boolean containers) {
        return filtered(table, trainingReference, containers, true);
    }

    private List<Long> filtered(String table, String trainingReference, boolean containers, boolean outerTenantFilter) {
        QueryWrapper<Object> query = new QueryWrapper<>();
        if (outerTenantFilter) query.apply("BINARY " + table + ".tenant_id=BINARY {0}", ComputeNodeService.tenant());
        if (containers) access.filterContainers(query); else access.filter(query, trainingReference);
        Matcher matcher = Pattern.compile("#\\{ew\\.paramNameValuePairs\\.([^}]+)\\}").matcher(query.getSqlSegment());
        StringBuffer sql = new StringBuffer();
        List<Object> parameters = new ArrayList<>();
        while (matcher.find()) {
            parameters.add(query.getParamNameValuePairs().get(matcher.group(1)));
            matcher.appendReplacement(sql, "?");
        }
        matcher.appendTail(sql);
        return jdbc.queryForList("SELECT " + table + ".id FROM " + table + " WHERE " + sql + " ORDER BY " + table + ".id", Long.class, parameters.toArray());
    }

    private void visible(long... ids) {
        List<Long> trainings = new ArrayList<>(), models = new ArrayList<>(), containers = new ArrayList<>();
        for (long id : ids) { trainings.add(id); models.add(id + 30000); containers.add(id + 20000); }
        assertAll(
            () -> assertEquals(trainings, filtered("vls_algorithm_training", "vls_algorithm_training.id", false), "training list"),
            () -> assertEquals(models, filtered("vls_algorithm_model", "vls_algorithm_model.training_id", false), "model list"),
            () -> assertEquals(containers, filtered("vls_container_instance", null, true), "container list"));
    }

    @Test void nativeSubjectUsesActualSessionUserAndSeparatesTenantAndEnvironment() {
        assertEquals(nativeOwner("localhost", "tenant-a", "alice"), shared.requireCreator("alice"));
        assertThrows(ServiceException.class, () -> shared.requireCreator("bob"));
        shared(1, "tenant-a", "alice", "localhost");
        allowed(1); visible(1);
        actor("bob", "tenant-a", "localhost"); denied(1); visible();
        actor("alice", "tenant-b", "localhost"); denied(1); visible();
        actor("alice", "tenant-a", "vm"); denied(1); visible();
        actor("", "tenant-a", "localhost"); assertEquals("", shared.viewer()); denied(1); visible();
        assertThrows(ServiceException.class, () -> mine.page(1, 20, null, null, null, null));
    }

    @Test void realMysqlSharedListsKeepOnlyThisEnvironmentTenantAndUser() {
        shared(1, "tenant-a", "alice", "localhost");
        shared(2, "tenant-a", "bob", "localhost");
        shared(3, "tenant-a", "alice", "vm");
        shared(4, "tenant-b", "alice", "localhost");
        allowed(1); denied(2); denied(3); denied(4); visible(1);
        actor("bob", "tenant-a", "localhost"); visible(2);
        actor("alice", "tenant-a", "vm"); visible(3);
        actor("alice", "tenant-b", "localhost"); visible(4);
    }

    @Test void existingPrivateAndPublicExclusiveJobsUseSsoWithoutNativeSubstitution() {
        job(1, "PRIVATE", ComputeSharedQueueService.EXCLUSIVE, "tenant-a", "alice", ssoOwner("platform-alice"), "{}", "QUEUED");
        job(2, "PLATFORM", ComputeSharedQueueService.EXCLUSIVE, "tenant-a", "alice", ssoOwner("platform-alice"), "{}", "QUEUED");
        allowed(1); allowed(2); visible(1, 2);
        platformUser.set("platform-bob"); denied(1); denied(2); visible();
        platformUser.set("platform-alice"); actor("bob", "tenant-a", "localhost");
        allowed(1); allowed(2); visible(1, 2);
        assertEquals(0, mine.page(1, 20, null, null, null, null).getTotal(), "My tasks still require the local creator");
    }

    @Test void nativeLoginMayReadOwnSharedJobWithoutOptionalSsoAuthorization() {
        shared(1, "tenant-a", "alice", "localhost");
        job(2, "PRIVATE", ComputeSharedQueueService.EXCLUSIVE, "tenant-a", "alice", ssoOwner("platform-alice"), "{}", "QUEUED");
        platformUser.set(null); allowed(1); denied(2); visible(1);
        assertEquals(1, mine.page(1, 20, null, null, null, null).getTotal());
    }

    @Test void sharedJobNeverFallsBackToMatchingSsoSubject() {
        job(1, "PLATFORM", ComputeSharedQueueService.MODE, "tenant-a", "alice", ssoOwner("platform-alice"), SHARED, "QUEUED");
        denied(1); visible();
        assertEquals(0, mine.page(1, 20, null, null, null, null).getTotal());
    }

    @Test void corruptMissingOrUnknownSharedOptionsDenyNativeOwnedRowsWithoutJsonSqlErrors() {
        shared(1, "tenant-a", "alice", "localhost");
        for (String options : Arrays.asList(null, "{broken", "null", "[]", "{}", "{\"sharedQueue\":{\"mode\":\"other\"}}", "{\"sharedQueue\":{\"mode\":\"shared_gpu_queue\"}}")) {
            jdbc.update("UPDATE vls_cloud_training_job SET options_json=? WHERE training_id=1", options);
            denied(1); visible();
            assertEquals(0, mine.page(1, 20, null, null, null, null).getTotal(), "corrupt snapshot " + options);
        }
    }

    @Test void malformedSharedSnapshotCannotBeReadThroughMatchingOrdinarySsoSubject() {
        job(1, "PLATFORM", ComputeSharedQueueService.MODE, "tenant-a", "alice", ssoOwner("platform-alice"), "{broken", "QUEUED");
        assertAll(() -> denied(1), () -> visible(), () -> assertEquals(0, mine.page(1, 20, null, null, null, null).getTotal()));
    }

    @Test void sharedNodeCannotDowngradeMissingOrExclusiveFrozenModeToSsoReads() {
        job(1, "PLATFORM", ComputeSharedQueueService.MODE, "tenant-a", "alice", ssoOwner("platform-alice"), "{}", "QUEUED");
        for (String options : Arrays.asList("{}", "{\"sharedQueue\":{}}", "{\"sharedQueue\":{\"mode\":\"INSTANCE_EXCLUSIVE\"}}")) {
            jdbc.update("UPDATE vls_cloud_training_job SET options_json=? WHERE training_id=1", options);
            assertAll(() -> denied(1), () -> visible(), () -> assertEquals(0, mine.page(1, 20, null, null, null, null).getTotal(), "mismatched snapshot " + options));
        }
    }

    @Test void nullOwnerAndUnsupportedEnvironmentNeverGrantSharedReads() {
        job(1, "PLATFORM", ComputeSharedQueueService.MODE, "tenant-a", "alice", null, SHARED, "QUEUED");
        shared(2, "tenant-a", "alice", "localhost");
        for (String owner : Arrays.asList(null, "", " ")) {
            jdbc.update("UPDATE vls_cloud_training_job SET public_owner_subject=? WHERE training_id=1", owner);
            denied(1); visible(2);
            assertEquals(1, mine.page(1, 20, null, null, null, null).getTotal());
        }
        jdbc.update("UPDATE vls_compute_node SET resource_scope='PRIVATE' WHERE id=10001");
        for (String owner : Arrays.asList(null, "")) {
            jdbc.update("UPDATE vls_cloud_training_job SET public_owner_subject=? WHERE training_id=1", owner);
            denied(1); visible(2);
            assertEquals(1, mine.page(1, 20, null, null, null, null).getTotal(), "Withdrawn shared node cannot become legacy-private data");
        }
        jdbc.update("DELETE FROM vls_compute_node WHERE id=10001");
        jdbc.update("UPDATE vls_cloud_training_job SET public_owner_subject=NULL WHERE training_id=1");
        denied(1); visible(2);
        assertEquals(1, mine.page(1, 20, null, null, null, null).getTotal(), "Frozen shared mode still protects ownerless data after node removal");
        properties.setEnvironmentId("unsupported"); assertEquals("", shared.viewer());
        denied(1); denied(2); visible();
    }

    @Test void sameSsoInAnotherTenantCannotReadExistingPrivateOrPublicTaskData() {
        job(1, "PRIVATE", ComputeSharedQueueService.EXCLUSIVE, "tenant-a", "alice", ssoOwner("platform-alice"), "{}", "QUEUED");
        job(2, "PLATFORM", ComputeSharedQueueService.EXCLUSIVE, "tenant-b", "alice", ssoOwner("platform-alice"), "{}", "QUEUED");
        allowed(1); denied(2); visible(1);
        assertAll(
            () -> assertEquals(Collections.singletonList(1L), filtered("vls_algorithm_training", "vls_algorithm_training.id", false, false)),
            () -> assertEquals(Collections.singletonList(30001L), filtered("vls_algorithm_model", "vls_algorithm_model.training_id", false, false)),
            () -> assertEquals(Collections.singletonList(20001L), filtered("vls_container_instance", null, true, false)));
        actor("alice", "tenant-b", "localhost"); denied(1); allowed(2); visible(2);
    }

    @Test void aContainerRecordCannotUseAnotherLinkedTrainingToBypassItsJobOwner() {
        shared(1, "tenant-a", "alice", "localhost"); shared(2, "tenant-a", "bob", "localhost");
        jdbc.update("UPDATE vls_container_instance SET training_task_id=1 WHERE id=20002");
        assertThrows(ServiceException.class, () -> access.container(20002L));
        assertEquals(Collections.singletonList(20001L), filtered("vls_container_instance", null, true));
    }

    @Test void removingNodeKeepsFrozenSharedOwnershipForListAndDirectReads() {
        shared(1, "tenant-a", "alice", "localhost");
        jdbc.update("DELETE FROM vls_compute_node WHERE id=10001"); allowed(1); visible(1);
        actor("bob", "tenant-a", "localhost"); denied(1); visible();
    }

    @Test void myTasksPaginationStatisticsAndFiltersUseTheSameMixedOwnershipMatrix() {
        shared(1, "tenant-a", "alice", "localhost"); shared(2, "tenant-a", "bob", "localhost");
        shared(3, "tenant-a", "alice", "vm"); shared(4, "tenant-b", "alice", "localhost");
        job(5, "PRIVATE", ComputeSharedQueueService.EXCLUSIVE, "tenant-a", "alice", ssoOwner("platform-alice"), "{}", "COMPLETED");
        job(6, "PLATFORM", ComputeSharedQueueService.EXCLUSIVE, "tenant-a", "alice", ssoOwner("platform-alice"), "{}", "ARCHIVING");
        job(7, "PRIVATE", ComputeSharedQueueService.EXCLUSIVE, "tenant-a", "alice", null, "{}", "QUEUED");
        job(8, "PLATFORM", ComputeSharedQueueService.MODE, "tenant-a", "alice", nativeOwner("localhost", "tenant-a", "bob"), SHARED, "RUNNING");
        job(9, "PLATFORM", ComputeSharedQueueService.MODE, "tenant-a", "alice", null, SHARED, "RUNNING");
        MyTrainingTasksService.Page first = mine.page(1, 2, null, null, null, null);
        assertEquals(4, first.getTotal()); assertEquals(2, first.getRecords().size());
        assertEquals(Long.valueOf(2), first.getStatistics().get("QUEUED"));
        assertEquals(Long.valueOf(1), first.getStatistics().get("COMPLETED"));
        assertEquals(Long.valueOf(1), first.getStatistics().get("ARCHIVING"));
        assertEquals(Long.valueOf(0), first.getStatistics().get("RUNNING"));
        assertEquals(2, mine.page(2, 2, null, null, null, null).getRecords().size());
        assertEquals(1, mine.page(1, 20, "task 1", null, null, null).getTotal());
        assertEquals(1, mine.page(1, 20, null, "ARCHIVING", null, null).getTotal());
        assertEquals(0, mine.page(1, 20, null, null, "2099-01-01 00:00:00", null).getTotal());
    }

    @Test void legacyPrivateJobsKeepOnlyKnownLocalAuthorWithoutAssigningSso() {
        job(1, "PRIVATE", ComputeSharedQueueService.EXCLUSIVE, "tenant-a", "alice", null, "{}", "COMPLETED");
        allowed(1); visible(1);
        actor("bob", "tenant-a", "localhost"); denied(1); visible();
        actor("alice", "tenant-a", "localhost");
        jdbc.update("UPDATE vls_algorithm_training SET create_user=NULL WHERE id=1"); denied(1); visible();
        assertNull(jdbc.queryForObject("SELECT public_owner_subject FROM vls_cloud_training_job WHERE training_id=1", String.class));
    }

    @Test void mysqlListAndDirectGuardsRequireExactNativeAndSsoSubjectBytes() {
        shared(1, "tenant-a", "alice", "localhost");
        job(2, "PRIVATE", ComputeSharedQueueService.EXCLUSIVE, "tenant-a", "alice", ssoOwner("platform-alice"), "{}", "QUEUED");
        jdbc.update("UPDATE vls_cloud_training_job SET public_owner_subject=UPPER(public_owner_subject)");
        assertAll(() -> denied(1), () -> denied(2), () -> visible(), () -> assertEquals(0, mine.page(1, 20, null, null, null, null).getTotal()));
    }

    @Test void mysqlMyTaskTenantAndCreatorFiltersRemainCaseSensitive() {
        shared(1, "tenant-a", "alice", "localhost");
        job(2, "PRIVATE", ComputeSharedQueueService.EXCLUSIVE, "Tenant-A", "alice", ssoOwner("platform-alice"), "{}", "QUEUED");
        job(3, "PRIVATE", ComputeSharedQueueService.EXCLUSIVE, "tenant-a", "Alice", ssoOwner("platform-alice"), "{}", "QUEUED");
        assertEquals(1, mine.page(1, 20, null, null, null, null).getTotal());
        denied(2);
    }

    private String modelReference(long id) {
        return "cloud-training/" + (id + 10000) + "/fixture-job-" + id + "/weights/best.pt";
    }

    @Test void mysqlOwnCompletedSharedModelRequiresExactTenantOwnerReferenceAndAnnotation() {
        job(1, "PLATFORM", ComputeSharedQueueService.MODE, "tenant-a", "alice", shared.viewer(), SHARED, "COMPLETED");
        String reference = modelReference(1);
        assertEquals(reference, shared.verifyBaseModel(reference, "object_detection", shared.viewer()));
        assertAll(
            () -> assertThrows(ServiceException.class, () -> shared.verifyBaseModel(reference, "image_classification", shared.viewer())),
            () -> assertThrows(ServiceException.class, () -> shared.verifyBaseModel(reference, "OBJECT_DETECTION", shared.viewer())),
            () -> assertThrows(ServiceException.class, () -> shared.verifyBaseModel(reference.toUpperCase(Locale.ROOT), "object_detection", shared.viewer())),
            () -> assertThrows(ServiceException.class, () -> shared.verifyBaseModel(reference.replace("best.pt", "BEST.pt"), "object_detection", shared.viewer())));
        String owner = shared.viewer();
        for (String altered : Arrays.asList(owner.toUpperCase(Locale.ROOT), nativeOwner("localhost", "tenant-a", "bob"))) {
            jdbc.update("UPDATE vls_cloud_training_job SET public_owner_subject=? WHERE training_id=1", altered);
            assertThrows(ServiceException.class, () -> shared.verifyBaseModel(reference, "object_detection", shared.viewer()));
        }
        jdbc.update("UPDATE vls_cloud_training_job SET public_owner_subject=? WHERE training_id=1", owner);
        for (String altered : Arrays.asList("Tenant-A", "tenant-b")) {
            jdbc.update("UPDATE vls_cloud_training_job SET tenant_id=? WHERE training_id=1", altered);
            assertThrows(ServiceException.class, () -> shared.verifyBaseModel(reference, "object_detection", shared.viewer()));
        }
        jdbc.update("UPDATE vls_cloud_training_job SET tenant_id='tenant-a' WHERE training_id=1");
        assertEquals(reference, shared.verifyBaseModel(reference, "object_detection", shared.viewer()));
        actor("bob", "tenant-a", "localhost");
        assertThrows(ServiceException.class, () -> shared.verifyBaseModel(reference, "object_detection", shared.viewer()));
        actor("alice", "tenant-a", "vm");
        assertThrows(ServiceException.class, () -> shared.verifyBaseModel(reference, "object_detection", shared.viewer()));
    }

    @Test void mysqlPrivateExclusiveUnfinishedAndInvalidSnapshotModelsNeverBecomeSharedBaseWeights() {
        job(1, "PRIVATE", ComputeSharedQueueService.EXCLUSIVE, "tenant-a", "alice", shared.viewer(), "{}", "COMPLETED");
        job(2, "PLATFORM", ComputeSharedQueueService.EXCLUSIVE, "tenant-a", "alice", shared.viewer(), "{\"sharedQueue\":{\"mode\":\"INSTANCE_EXCLUSIVE\"}}", "COMPLETED");
        job(3, "PLATFORM", ComputeSharedQueueService.MODE, "tenant-a", "alice", shared.viewer(), SHARED, "ARCHIVING");
        assertAll(
            () -> assertThrows(ServiceException.class, () -> shared.verifyBaseModel(modelReference(1), "object_detection", shared.viewer())),
            () -> assertThrows(ServiceException.class, () -> shared.verifyBaseModel(modelReference(2), "object_detection", shared.viewer())));
        for (String state : Arrays.asList("QUEUED", "PREPARING", "RUNNING", "ARCHIVING", "FAILED", "CANCELLED", "completed")) {
            jdbc.update("UPDATE vls_cloud_training_job SET job_state=? WHERE training_id=3", state);
            assertThrows(ServiceException.class, () -> shared.verifyBaseModel(modelReference(3), "object_detection", shared.viewer()), state);
        }
        jdbc.update("UPDATE vls_cloud_training_job SET job_state='COMPLETED' WHERE training_id=3");
        assertEquals(modelReference(3), shared.verifyBaseModel(modelReference(3), "object_detection", shared.viewer()));
        for (String options : Arrays.asList(null, "{broken", "{}", "{\"sharedQueue\":{\"mode\":\"shared_gpu_queue\"}}")) {
            jdbc.update("UPDATE vls_cloud_training_job SET options_json=? WHERE training_id=3", options);
            assertThrows(ServiceException.class, () -> shared.verifyBaseModel(modelReference(3), "object_detection", shared.viewer()), options);
        }
    }

    /** No DB query in this guard body: real asset policy, public RFC key fixture and no network. */
    @Test void invalidCompanyWorkDirOrTupleRejectsIdleCheckAndBrokerCommandBeforeSshSideEffects() throws Exception {
        ComputeTrustedSelfAssetsProperties assets = new ComputeTrustedSelfAssetsProperties();
        ComputeTrustedSelfAssetsProperties.Asset asset = new ComputeTrustedSelfAssetsProperties.Asset();
        asset.setAssetId("synthetic-p100"); asset.setTenantId("tenant-a"); asset.setHost("192.168.88.173");
        asset.setPort(22); asset.setUsername("oort"); asset.setCompanyPool(true);
        // RFC8032 public vector in SSH wire format; no private/login credential is generated.
        asset.setHostKey("AAAAC3NzaC1lZDI1NTE5AAAAINdamAGCsQq31Uv+08lkBzoO4XLz2qYjJa8CGmj3B1Ea");
        assets.setAssets(Collections.singletonList(asset));
        ComputeSharedQueueService guarded = new ComputeSharedQueueService(properties, new ComputeTrustedSelfPolicy(assets), jdbc, new ObjectMapper());
        ComputeNode node = new ComputeNode();
        node.setHost("192.168.88.173"); node.setPort(22); node.setUsername("oort"); node.setTenantId("tenant-a");
        node.setProvider("self"); node.setSchedulingMode(ComputeSharedQueueService.MODE);
        node.setPythonPath("/data/work/vlstream-gpu/runtime-173/envs/vls-standard/bin/python");
        node.setWorkDir("/data/work/vlstream-gpu/localhost");
        guarded.requireTarget(node);
        ComputeSsh ssh = mock(ComputeSsh.class);
        ComputeSsh.Connection connection = mock(ComputeSsh.Connection.class);
        CloudTrainingJob job = new CloudTrainingJob();
        job.setId("11111111-1111-4111-8111-111111111111"); job.setPublicOwnerSubject(shared.viewer());
        job.setOptionsJson("{\"sharedQueue\":{\"mode\":\"SHARED_GPU_QUEUE\",\"environment\":\"localhost\",\"jobId\":\"" + job.getId() + "\",\"owner\":\"" + job.getPublicOwnerSubject() + "\",\"gpuIndex\":0,\"gpuUuid\":\"GPU-24fb011a-dd51-d9e1-ed6d-59f9266869ac\"}}");
        com.fasterxml.jackson.databind.JsonNode failed = new ObjectMapper().readTree("{\"state\":\"FAILED\"}");
        node.setWorkDir("/data/work/ultralytics_yolov8-main/datasets");
        assertAll(
            () -> assertThrows(ServiceException.class, () -> guarded.assertHostIdle(ssh, node)),
            () -> assertThrows(ServiceException.class, () -> guarded.release(connection, node, job, failed, null)));
        node.setWorkDir("/data/work/vlstream-gpu/localhost"); node.setTenantId("other-tenant");
        assertAll(
            () -> assertThrows(ServiceException.class, () -> guarded.assertHostIdle(ssh, node)),
            () -> assertThrows(ServiceException.class, () -> guarded.release(connection, node, job, failed, null)));
        verifyNoInteractions(ssh, connection);
    }

    private AlgorithmTraining storedTraining(long id) {
        return jdbc.queryForObject("SELECT * FROM vls_algorithm_training WHERE id=?",new BeanPropertyRowMapper<>(AlgorithmTraining.class),id);
    }

    private CloudTrainingService consumer(boolean injectGuard) {
        ComputeNodeService nodes=mock(ComputeNodeService.class);
        when(nodes.publicInventory()).thenReturn(inventory);
        IVlsAlgorithmTrainingService records=mock(IVlsAlgorithmTrainingService.class);
        when(records.selectAlgorithmTrainingById(anyLong())).thenAnswer(call->storedTraining(call.getArgument(0)));
        CloudTrainingService consumer=new CloudTrainingService(jdbc,nodes,mock(DataManagementService.class),mock(TrainingPublicationService.class),records,mock(IVlsContainerInstanceService.class),mock(VlsAlgorithmMapper.class),new ObjectMapper());
        // Every manually built shared consumer explicitly receives its actual shared identity service.
        ReflectionTestUtils.setField(consumer,"sharedQueue",shared);
        if(injectGuard)ReflectionTestUtils.setField(consumer,"publicAccess",access);
        jdbc.update("UPDATE vls_cloud_training_job SET log_tail=CONCAT('synthetic-log-',training_id),run_dir=CONCAT('/fixture/',id),progress=17,message='synthetic queued'");
        return consumer;
    }

    private void consumerDenied(CloudTrainingService cloud,long id) {
        AlgorithmTraining training=storedTraining(id);
        Long before=jdbc.queryForObject("SELECT COALESCE(SUM(cancel_requested),0) FROM vls_cloud_training_job",Long.class);
        assertAll(
            ()->assertThrows(ServiceException.class,()->cloud.current(training)),
            ()->assertThrows(ServiceException.class,()->cloud.logs(training)),
            ()->assertThrows(ServiceException.class,()->cloud.progress(training)),
            ()->assertThrows(ServiceException.class,()->cloud.stop(training)),
            ()->assertThrows(ServiceException.class,()->cloud.assertInactive(training)),
            ()->assertThrows(ServiceException.class,()->cloud.containerLogs(id+20000)));
        assertEquals(before,jdbc.queryForObject("SELECT COALESCE(SUM(cancel_requested),0) FROM vls_cloud_training_job",Long.class),"Denied consumers cannot cancel any task");
    }

    @Test void actualNativeConsumerAndControllerLogsStatusStopWorkWithoutOptionalSso() {
        shared(1,"tenant-a","alice","localhost"); shared(2,"tenant-a","bob","localhost"); platformUser.set(null);
        CloudTrainingService cloud=consumer(true);
        AlgorithmTraining training=storedTraining(1);
        assertEquals("fixture-job-1",cloud.current(training).getId());
        assertEquals("synthetic-log-1",cloud.logs(training).getLogContent());
        assertEquals(Integer.valueOf(17),cloud.progress(training).getPercentage());
        assertEquals("synthetic-log-1",cloud.containerLogs(20001L));
        assertThrows(ServiceException.class,()->cloud.assertInactive(training));
        VlsAlgorithmTrainingController controller=new VlsAlgorithmTrainingController();
        IVlsAlgorithmTrainingService records=mock(IVlsAlgorithmTrainingService.class);
        when(records.selectAlgorithmTrainingById(1L)).thenReturn(training);
        RemoteTrainingService defaultGpu=mock(RemoteTrainingService.class);
        ReflectionTestUtils.setField(controller,"vlsAlgorithmTrainingService",records);
        ReflectionTestUtils.setField(controller,"cloudTrainingService",cloud);
        ReflectionTestUtils.setField(controller,"remoteTrainingService",defaultGpu);
        assertEquals("synthetic-log-1",controller.getTrainingLogs(1L,"/untrusted-other-path",20).getData().getLogContent());
        assertEquals(Integer.valueOf(17),controller.getTrainingStatus(1L,"/untrusted-other-path").getData().getPercentage());
        assertEquals("stop_requested",controller.stopTraining(1L).getData());
        assertEquals(1,jdbc.queryForObject("SELECT cancel_requested FROM vls_cloud_training_job WHERE training_id=1",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT cancel_requested FROM vls_cloud_training_job WHERE training_id=2",Integer.class));
        verifyNoInteractions(defaultGpu);
        jdbc.update("UPDATE vls_cloud_training_job SET job_state='COMPLETED' WHERE training_id=1");
        cloud.assertInactive(training);
        assertEquals("synthetic-log-1",consumer(false).containerLogs(20001L),"Explicit shared fallback also authorizes the native owner");
    }

    @Test void actualConsumersRejectOtherUserTenantAndAdministratorWithoutCancellation() {
        shared(1,"tenant-a","alice","localhost");
        CloudTrainingService cloud=consumer(true);
        actor("bob","tenant-a","localhost"); consumerDenied(cloud,1);
        actor("alice","tenant-b","localhost"); consumerDenied(cloud,1);
        actor("1","tenant-a","localhost"); consumerDenied(cloud,1);
        actor("alice","tenant-a","vm"); consumerDenied(cloud,1);
        actor("alice","tenant-a","localhost");
        AlgorithmTraining wrongCreator=storedTraining(1); wrongCreator.setCreateUser("bob");
        assertThrows(ServiceException.class,()->cloud.current(wrongCreator));
        ReflectionTestUtils.setField(cloud,"publicAccess",null);
        ReflectionTestUtils.setField(cloud,"sharedQueue",null);
        consumerDenied(cloud,1);
    }

    @Test void actualConsumersKeepFrozenModeAndOwnerProtectionAfterWithdrawAndRemoval() {
        shared(1,"tenant-a","alice","localhost");
        CloudTrainingService cloud=consumer(true);
        for(String options:Arrays.asList("{broken","{}","{\"sharedQueue\":{\"mode\":\"INSTANCE_EXCLUSIVE\"}}")) {
            jdbc.update("UPDATE vls_cloud_training_job SET options_json=? WHERE training_id=1",options); consumerDenied(cloud,1);
        }
        jdbc.update("UPDATE vls_cloud_training_job SET options_json=? WHERE training_id=1",SHARED);
        jdbc.update("UPDATE vls_compute_node SET resource_scope='PRIVATE' WHERE id=10001");
        assertEquals("fixture-job-1",cloud.current(storedTraining(1)).getId());
        for(String owner:Arrays.asList(null,"")) {
            jdbc.update("UPDATE vls_cloud_training_job SET public_owner_subject=? WHERE training_id=1",owner); consumerDenied(cloud,1);
        }
        jdbc.update("UPDATE vls_cloud_training_job SET public_owner_subject=? WHERE training_id=1",shared.viewer());
        jdbc.update("DELETE FROM vls_compute_node WHERE id=10001");
        assertEquals("synthetic-log-1",cloud.containerLogs(20001L));
        jdbc.update("UPDATE vls_cloud_training_job SET public_owner_subject=NULL WHERE training_id=1"); consumerDenied(cloud,1);
    }

    @Test void actualSsoAndKnownLegacyConsumersRetainCreatorChecksWithTheSameSsoSubject() {
        job(1,"PRIVATE",ComputeSharedQueueService.EXCLUSIVE,"tenant-a","alice",ssoOwner("platform-alice"),"{}","QUEUED");
        job(2,"PLATFORM",ComputeSharedQueueService.EXCLUSIVE,"tenant-a","alice",ssoOwner("platform-alice"),"{}","QUEUED");
        job(3,"PRIVATE",ComputeSharedQueueService.EXCLUSIVE,"tenant-a","alice",null,"{}","QUEUED");
        CloudTrainingService cloud=consumer(true);
        for(long id:new long[]{1,2,3})assertEquals("synthetic-log-"+id,cloud.containerLogs(id+20000));
        actor("bob","tenant-a","localhost"); for(long id:new long[]{1,2,3})consumerDenied(cloud,id);
        actor("1","tenant-a","localhost"); for(long id:new long[]{1,2,3})consumerDenied(cloud,id);
        actor("alice","tenant-a","localhost"); platformUser.set(null);
        consumerDenied(cloud,1); consumerDenied(cloud,2);
        assertEquals("synthetic-log-3",cloud.logs(storedTraining(3)).getLogContent());
    }

    private SqlSession pageSession() throws Exception {
        MybatisConfiguration configuration=new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setEnvironment(new Environment("shared-consumer-only",new JdbcTransactionFactory(),jdbc.getDataSource()));
        MybatisPlusInterceptor paging=new MybatisPlusInterceptor();
        paging.addInnerInterceptor(new PaginationInnerInterceptor(com.baomidou.mybatisplus.annotation.DbType.MYSQL));
        configuration.addInterceptor(paging);
        String resource=VlsAlgorithmTrainingMapper.class.getName().replace('.','/')+".xml";
        try(InputStream input=getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input); new XMLMapperBuilder(input,configuration,resource,configuration.getSqlFragments()).parse();
        }
        return new SqlSessionFactoryBuilder().build(configuration).openSession(true);
    }

    private VlsAlgorithmTrainingServiceImpl pageService(SqlSession session) {
        VlsAlgorithmTrainingServiceImpl service=new VlsAlgorithmTrainingServiceImpl();
        ReflectionTestUtils.setField(service,"baseMapper",session.getMapper(VlsAlgorithmTrainingMapper.class));
        ReflectionTestUtils.setField(service,"publicAccess",access);
        return service;
    }

    private List<Long> pageIds(IPage<AlgorithmTrainingVO> page) {
        List<Long> ids=new ArrayList<>(); for(AlgorithmTrainingVO row:page.getRecords())ids.add(row.getId()); return ids;
    }

    @Test void actualPrimaryMapperPageUsesUnifiedGuardCountPaginationAndBoundRequestFilters() throws Exception {
        shared(1,"tenant-a","alice","localhost"); shared(2,"tenant-a","bob","localhost");
        shared(3,"tenant-a","alice","vm"); shared(4,"tenant-b","alice","localhost");
        job(5,"PRIVATE",ComputeSharedQueueService.EXCLUSIVE,"tenant-a","alice",ssoOwner("platform-alice"),"{}","QUEUED");
        job(6,"PLATFORM",ComputeSharedQueueService.EXCLUSIVE,"tenant-a","alice",ssoOwner("platform-alice"),"{}","QUEUED");
        job(7,"PRIVATE",ComputeSharedQueueService.EXCLUSIVE,"tenant-a","alice",null,"{}","QUEUED");
        job(8,"PLATFORM",ComputeSharedQueueService.MODE,"tenant-a","alice",nativeOwner("localhost","tenant-a","bob"),SHARED,"QUEUED");
        job(9,"PLATFORM",ComputeSharedQueueService.MODE,"tenant-a","alice",null,SHARED,"QUEUED");
        job(10,"PRIVATE",ComputeSharedQueueService.EXCLUSIVE,"tenant-a","bob",ssoOwner("platform-alice"),"{}","QUEUED");
        try(SqlSession session=pageSession()) {
            VlsAlgorithmTrainingServiceImpl service=pageService(session);
            IPage<AlgorithmTrainingVO> first=service.selectVlsAlgorithmTrainingPage(new Page<>(1,2),new AlgorithmTrainingVO());
            assertEquals(4,first.getTotal()); assertEquals(Arrays.asList(7L,6L),pageIds(first));
            IPage<AlgorithmTrainingVO> second=service.selectVlsAlgorithmTrainingPage(new Page<>(2,2),new AlgorithmTrainingVO());
            assertEquals(4,second.getTotal()); assertEquals(Arrays.asList(5L,1L),pageIds(second));
            platformUser.set(null);
            AlgorithmTrainingVO request=new AlgorithmTrainingVO(); request.setTenantId("tenant-b"); request.setCreateUser("bob");
            assertEquals(Arrays.asList(7L,1L),pageIds(service.selectVlsAlgorithmTrainingPage(new Page<>(1,20),request)));
            request.setTaskName("task 1"); assertEquals(Collections.singletonList(1L),pageIds(service.selectVlsAlgorithmTrainingPage(new Page<>(1,20),request)));
            request.setTaskName("' OR 1=1 --"); assertEquals(0,service.selectVlsAlgorithmTrainingPage(new Page<>(1,20),request).getTotal());
            request.setTaskName(null); request.setAlgorithmId(21L); request.setDatasetId(22L); request.setTrainStatus(AlgorithmTrainingStatusEnum.pending);
            jdbc.update("UPDATE vls_algorithm_training SET algorithm_id=21,dataset_id=22 WHERE id=1");
            assertEquals(Collections.singletonList(1L),pageIds(service.selectVlsAlgorithmTrainingPage(new Page<>(1,20),request)));
            request.setTrainStatus(AlgorithmTrainingStatusEnum.completed); assertEquals(0,service.selectVlsAlgorithmTrainingPage(new Page<>(1,20),request).getTotal());
        }
    }

    @Test void actualPrimaryPageAndControllerRetainSharedOwnerAfterNodeWithdrawalAndDeletion() throws Exception {
        shared(1,"tenant-a","alice","localhost");
        job(2,"PLATFORM",ComputeSharedQueueService.MODE,"tenant-a","alice",null,SHARED,"QUEUED");
        job(3,"PLATFORM",ComputeSharedQueueService.MODE,"tenant-a","alice",shared.viewer(),"{}","QUEUED");
        job(4,"PLATFORM",ComputeSharedQueueService.MODE,"tenant-a","alice",ssoOwner("platform-alice"),"{}","QUEUED");
        CloudTrainingService cloud=consumer(true); platformUser.set(null);
        jdbc.update("UPDATE vls_compute_node SET resource_scope='PRIVATE' WHERE id IN (10001,10002)");
        try(SqlSession session=pageSession()) {
            VlsAlgorithmTrainingServiceImpl service=pageService(session);
            assertEquals(Collections.singletonList(1L),pageIds(service.selectVlsAlgorithmTrainingPage(new Page<>(1,20),new AlgorithmTrainingVO())));
            jdbc.update("DELETE FROM vls_compute_node WHERE id IN (10001,10002)");
            VlsAlgorithmTrainingController controller=new VlsAlgorithmTrainingController();
            ReflectionTestUtils.setField(controller,"vlsAlgorithmTrainingService",service);
            ReflectionTestUtils.setField(controller,"cloudTrainingService",cloud);
            ReflectionTestUtils.setField(controller,"algorithmService",mock(IVlsAlgorithmService.class));
            IPage<AlgorithmTrainingVO> result=controller.page(new AlgorithmTrainingVO(),new org.springblade.core.mp.support.Query()).getData();
            assertEquals(1,result.getTotal()); assertEquals(Collections.singletonList(1L),pageIds(result));
            assertEquals("QUEUED",result.getRecords().get(0).getExecutionStage());
            actor("1","tenant-a","localhost"); assertEquals(0,service.selectVlsAlgorithmTrainingPage(new Page<>(1,20),new AlgorithmTrainingVO()).getTotal());
            actor("alice","tenant-b","localhost"); assertEquals(0,service.selectVlsAlgorithmTrainingPage(new Page<>(1,20),new AlgorithmTrainingVO()).getTotal());
        }
    }

    @Test void actualJdbcBigintLegacyCreatorKeepsExactOwnerAndTenantGuards() {
        jdbc.execute("ALTER TABLE vls_algorithm_training MODIFY create_user BIGINT NULL");
        try {
            String owner="9007199254740993";
            job(1,"PRIVATE",ComputeSharedQueueService.EXCLUSIVE,"tenant-a",owner,null,"{}","QUEUED");
            Object actual=jdbc.queryForMap("SELECT create_user FROM vls_algorithm_training WHERE id=1").get("create_user");
            assertTrue(actual instanceof Long,"Exercise the actual JDBC BIGINT representation");
            assertEquals(owner,ComputePublicTaskAccess.databaseCreator(actual));
            actor(owner,"tenant-a","localhost"); allowed(1); visible(1);
            CloudTrainingService cloud=consumer(true);
            assertEquals("synthetic-log-1",cloud.logs(storedTraining(1)).getLogContent());
            actor("9007199254740992","tenant-a","localhost"); denied(1); consumerDenied(cloud,1);
            actor(owner,"tenant-b","localhost"); denied(1); consumerDenied(cloud,1);
            actor(owner,"Tenant-a","localhost"); denied(1);
            actor(owner+" ","tenant-a","localhost"); denied(1);
            actor("0"+owner,"tenant-a","localhost"); denied(1);
            actor(owner,"tenant-a","localhost");
            jdbc.update("UPDATE vls_algorithm_training SET create_user=NULL WHERE id=1"); denied(1);
        } finally {
            jdbc.execute("ALTER TABLE vls_algorithm_training MODIFY create_user VARCHAR(64) NULL");
        }
    }

    @Test void actualDetailMapperCarriesTenantAndBigintCreatorIntoSharedLogsAndStatus() throws Exception {
        // Only the guarded synthetic fixture receives the nullable legacy detail columns.
        assertEquals(SCHEMA,jdbc.queryForObject("SELECT DATABASE()",String.class));
        for(String column:Arrays.asList("accuracy","precision_value","recall_value","map_value","loss_value","gpu_usage",
            "start_time","end_time","estimated_time","onnx_model_output_path","onnx_conversion_status","onnx_conversion_error",
            "rknn_model_output_path","int8_rknn_model_output_path","om_model_output_path","om_conversion_status",
            "om_conversion_error","log_path","error_message","update_time")) {
            if(jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=? AND table_name='vls_algorithm_training' AND column_name=?",Integer.class,SCHEMA,column)==0)
                jdbc.execute("ALTER TABLE vls_algorithm_training ADD COLUMN "+column+" VARCHAR(255) NULL");
        }
        jdbc.execute("ALTER TABLE vls_algorithm_training MODIFY create_user BIGINT NULL");
        try {
            String owner="9007199254740993";
            actor(owner,"tenant-a","localhost"); shared(1,"tenant-a",owner,"localhost"); platformUser.set(null);
            CloudTrainingService cloud=consumer(true);
            try(SqlSession session=pageSession()) {
                AlgorithmTraining training=session.getMapper(VlsAlgorithmTrainingMapper.class).selectAlgorithmTrainingById(1L);
                assertEquals("tenant-a",training.getTenantId()); assertEquals(owner,training.getCreateUser());
                assertEquals("synthetic-log-1",cloud.logs(training).getLogContent());
                assertEquals(Integer.valueOf(17),cloud.progress(training).getPercentage());
                actor("9007199254740992","tenant-a","localhost");
                assertThrows(ServiceException.class,()->cloud.logs(training));
                actor(owner,"tenant-b","localhost");
                assertThrows(ServiceException.class,()->cloud.progress(training));
                actor(owner,"tenant-a","localhost");
                training.setTenantId(null); assertThrows(ServiceException.class,()->cloud.current(training));
            }
        } finally {
            jdbc.execute("ALTER TABLE vls_algorithm_training MODIFY create_user VARCHAR(64) NULL");
        }
    }

    @Test void plainLegacyWithoutCloudJobRequiresExactPersistedCreatorAndTenantInMysql() {
        jdbc.update("INSERT INTO vls_algorithm_training(id,tenant_id,create_user,task_name,train_status) VALUES(1,'tenant-a','alice','synthetic legacy','training')");
        platformUser.set(null);access.training(1L);assertEquals(Collections.singletonList(1L),filtered("vls_algorithm_training","vls_algorithm_training.id",false));
        for(String user:Arrays.asList("bob","Alice","alice ","1","")) { actor(user,"tenant-a","localhost");assertThrows(ServiceException.class,()->access.training(1L));assertTrue(filtered("vls_algorithm_training","vls_algorithm_training.id",false).isEmpty()); }
        actor("alice","tenant-b","localhost");assertThrows(ServiceException.class,()->access.training(1L));
        actor("alice","tenant-a","localhost");jdbc.update("UPDATE vls_algorithm_training SET create_user=NULL WHERE id=1");assertThrows(ServiceException.class,()->access.training(1L));
        assertThrows(ServiceException.class,()->access.training(999L));
        assertEquals("training",jdbc.queryForObject("SELECT train_status FROM vls_algorithm_training WHERE id=1",String.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM vls_cloud_training_job",Integer.class));
    }
    @Test void unlinkedLegacyContainerHasCreatorProofAndCannotGainAccessThroughForeignTraining() {
        jdbc.update("INSERT INTO vls_container_instance(id,tenant_id,create_user,instance_status) VALUES(20,'tenant-a','alice','running')");
        platformUser.set(null);access.container(20L);assertEquals(Collections.singletonList(20L),filtered("vls_container_instance",null,true));
        actor("bob","tenant-a","localhost");assertThrows(ServiceException.class,()->access.container(20L));
        assertTrue(filtered("vls_container_instance",null,true).isEmpty());
        actor("alice","tenant-b","localhost");assertThrows(ServiceException.class,()->access.container(20L));
        actor("alice","tenant-a","localhost");jdbc.update("UPDATE vls_container_instance SET create_user=NULL WHERE id=20");assertThrows(ServiceException.class,()->access.container(20L));
        jdbc.update("INSERT INTO vls_algorithm_training(id,tenant_id,create_user,task_name) VALUES(1,'tenant-a','bob','synthetic foreign')");
        jdbc.update("UPDATE vls_container_instance SET training_task_id=1 WHERE id=20");assertThrows(ServiceException.class,()->access.container(20L));
        jdbc.update("UPDATE vls_algorithm_training SET create_user='alice' WHERE id=1");access.container(20L);
        assertEquals("running",jdbc.queryForObject("SELECT instance_status FROM vls_container_instance WHERE id=20",String.class));
    }
    @Test void smartAnnotationContainerUsesRoundAndTaskOwnerWithoutOrdinaryTrainingIdCollision() {
        actor("101","tenant-a","localhost");platformUser.set(null);
        jdbc.update("INSERT INTO vls_smart_annotation_task(id,tenant_id,create_user) VALUES(10,'tenant-a',101)");
        jdbc.update("INSERT INTO vls_smart_annotation_round(id,tenant_id,task_id) VALUES(30,'tenant-a',10)");
        jdbc.update("INSERT INTO vls_container_instance(id,tenant_id,create_user,instance_type,training_task_id,instance_status) VALUES(20,'tenant-a','101','smart_annotation',30,'queued')");
        access.container(20L);assertEquals(Collections.singletonList(20L),filtered("vls_container_instance",null,true));
        actor("102","tenant-a","localhost");assertThrows(ServiceException.class,()->access.container(20L));assertTrue(filtered("vls_container_instance",null,true).isEmpty());
        actor("101","tenant-a","localhost");jdbc.update("UPDATE vls_smart_annotation_task SET create_user=102 WHERE id=10");
        jdbc.update("INSERT INTO vls_algorithm_training(id,tenant_id,create_user,task_name) VALUES(30,'tenant-a','101','unrelated own training')");
        assertThrows(ServiceException.class,()->access.container(20L));assertTrue(filtered("vls_container_instance",null,true).isEmpty());
        jdbc.update("UPDATE vls_smart_annotation_task SET create_user=101 WHERE id=10");jdbc.update("UPDATE vls_container_instance SET create_user='102' WHERE id=20");assertThrows(ServiceException.class,()->access.container(20L));assertTrue(filtered("vls_container_instance",null,true).isEmpty());
        jdbc.update("UPDATE vls_container_instance SET create_user='101' WHERE id=20");jdbc.update("UPDATE vls_smart_annotation_round SET tenant_id='tenant-b' WHERE id=30");assertThrows(ServiceException.class,()->access.container(20L));assertTrue(filtered("vls_container_instance",null,true).isEmpty());
        assertEquals("queued",jdbc.queryForObject("SELECT instance_status FROM vls_container_instance WHERE id=20",String.class));
    }
    @Test void wrongPersistedContainerTypeCannotBorrowAnAnnotationRoundOrCloudJob() {
        actor("101","tenant-a","localhost");platformUser.set(null);
        jdbc.update("INSERT INTO vls_smart_annotation_task(id,tenant_id,create_user) VALUES(10,'tenant-a',101)");jdbc.update("INSERT INTO vls_smart_annotation_round(id,tenant_id,task_id) VALUES(30,'tenant-a',10)");
        jdbc.update("INSERT INTO vls_container_instance(id,tenant_id,create_user,instance_type,training_task_id,instance_status) VALUES(20,'tenant-a','101','training',30,'queued')");
        assertThrows(ServiceException.class,()->access.container(20L));assertTrue(filtered("vls_container_instance",null,true).isEmpty());
        jdbc.update("UPDATE vls_container_instance SET instance_type='smart_annotation' WHERE id=20");access.container(20L);
        jdbc.update("INSERT INTO vls_algorithm_training(id,tenant_id,create_user,task_name) VALUES(30,'tenant-a','101','unrelated own ordinary row')");
        jdbc.update("UPDATE vls_container_instance SET instance_type='SMART_ANNOTATION' WHERE id=20");assertThrows(ServiceException.class,()->access.container(20L));assertTrue(filtered("vls_container_instance",null,true).isEmpty());
        jdbc.update("UPDATE vls_container_instance SET instance_type='smart_annotation' WHERE id=20");access.container(20L);
        jdbc.update("INSERT INTO vls_cloud_training_job(id,tenant_id,training_id,container_record_id,public_owner_subject,options_json) VALUES('wrong-type','tenant-a',999,20,'anything','{}')");
        assertThrows(ServiceException.class,()->access.container(20L));assertTrue(filtered("vls_container_instance",null,true).isEmpty());
        jdbc.update("DELETE FROM vls_cloud_training_job WHERE id='wrong-type'");jdbc.update("UPDATE vls_smart_annotation_round SET is_deleted=1 WHERE id=30");assertThrows(ServiceException.class,()->access.container(20L));assertTrue(filtered("vls_container_instance",null,true).isEmpty());
    }
    @Test void generatedQueuedLogMustMatchOwnedPersistedTrainingContainerAndTenant() {
        org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(new org.springframework.web.context.request.ServletRequestAttributes(new MockHttpServletRequest()));
        actor("101","tenant-a","localhost");platformUser.set(null);
        jdbc.update("INSERT INTO vls_algorithm_training(id,tenant_id,create_user,task_name) VALUES(1,'tenant-a','101','owned')");
        jdbc.update("INSERT INTO vls_container_instance(id,tenant_id,create_user,instance_type,training_task_id,logs_path) VALUES(20,'tenant-a','101','training',1,'owned-run.log')");
        assertEquals("owned-run.log",access.queuedTrainingLog(1L,20L));
        actor("102","tenant-a","localhost");assertThrows(ServiceException.class,()->access.queuedTrainingLog(1L,20L));
        actor("101","tenant-a","localhost");jdbc.update("UPDATE vls_container_instance SET create_user='102' WHERE id=20");assertThrows(ServiceException.class,()->access.queuedTrainingLog(1L,20L));
        jdbc.update("UPDATE vls_container_instance SET create_user='101',tenant_id='tenant-b' WHERE id=20");assertThrows(ServiceException.class,()->access.queuedTrainingLog(1L,20L));
        jdbc.update("UPDATE vls_container_instance SET tenant_id='tenant-a' WHERE id=20");assertThrows(ServiceException.class,()->access.queuedTrainingLog(2L,20L));
        assertEquals("owned-run.log",jdbc.queryForObject("SELECT logs_path FROM vls_container_instance WHERE id=20",String.class));
    }
    @Test void actualLegacySchedulerSqlExcludesUnlinkedAndMismatchedRecordsButKeepsOwnedQueues() throws Exception {
        jdbc.update("INSERT INTO vls_algorithm_training(id,tenant_id,create_user,task_name) VALUES(1,'tenant-a','101','owned')");
        jdbc.update("INSERT INTO vls_smart_annotation_task(id,tenant_id,create_user) VALUES(10,'tenant-a',101)");jdbc.update("INSERT INTO vls_smart_annotation_round(id,tenant_id,task_id) VALUES(30,'tenant-a',10)");
        jdbc.update("INSERT INTO vls_container_instance(id,tenant_id,create_user,instance_type,training_task_id,instance_status) VALUES(20,'tenant-a','101','training',NULL,'queued'),(21,'tenant-a','102','training',1,'queued'),(22,'tenant-b','101','training',1,'queued'),(23,'tenant-a',NULL,'training',1,'queued'),(24,'tenant-a','101','training',1,'queued'),(25,'tenant-a','101','smart_annotation',30,'queued')");
        String queued=String.join(" ",com.ruoyi.vlstream.test.vlstream.mapper.VlsContainerInstanceMapper.class.getMethod("selectNextQueuedTrainingForScheduler").getAnnotation(org.apache.ibatis.annotations.Select.class).value());
        assertEquals(Long.valueOf(24),((Number)jdbc.queryForList(queued).get(0).get("id")).longValue());
        jdbc.update("UPDATE vls_container_instance SET instance_status='running' WHERE id IN(20,21,22,23,24,25)");
        String active=String.join(" ",com.ruoyi.vlstream.test.vlstream.mapper.VlsContainerInstanceMapper.class.getMethod("selectActiveTrainingForScheduler").getAnnotation(org.apache.ibatis.annotations.Select.class).value());
        assertEquals(Arrays.asList(24L,25L),jdbc.queryForList(active).stream().map(row->((Number)row.get("id")).longValue()).collect(java.util.stream.Collectors.toList()));
        jdbc.update("UPDATE vls_algorithm_training SET is_deleted=1 WHERE id=1");jdbc.update("UPDATE vls_smart_annotation_round SET tenant_id='tenant-b' WHERE id=30");assertTrue(jdbc.queryForList(active).isEmpty());
    }
    @Test void creatorNormalizationRejectsLossyOrUnexpectedValues() {
        assertEquals("9007199254740993",ComputePublicTaskAccess.databaseCreator(9007199254740993L));
        assertEquals("18446744073709551615",ComputePublicTaskAccess.databaseCreator(new java.math.BigInteger("18446744073709551615")));
        assertEquals("Alice ",ComputePublicTaskAccess.databaseCreator("Alice "));
        assertNull(ComputePublicTaskAccess.databaseCreator(null));
        assertNull(ComputePublicTaskAccess.databaseCreator(1.0d));
        assertNull(ComputePublicTaskAccess.databaseCreator(new java.math.BigDecimal("1.0")));
    }
}
