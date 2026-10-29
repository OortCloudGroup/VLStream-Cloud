package com.ruoyi.vlstream.test.vlstream.compute;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.*;
import cn.dev33.satoken.servlet.model.*;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.*;
import com.ruoyi.vlstream.test.vlstream.controller.*;
import com.ruoyi.vlstream.test.vlstream.data.*;
import com.ruoyi.vlstream.test.vlstream.mapper.*;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import com.ruoyi.vlstream.test.vlstream.service.*;
import com.ruoyi.vlstream.test.vlstream.service.impl.VlsAlgorithmTrainingServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Synthetic A/B users only; no SQL, SSH, Docker or business process I/O. */
@Tag("dev") @Execution(ExecutionMode.SAME_THREAD)
class LegacyStopNoCloudOwnerTest {
    private SaTokenContext previous;
    private ComputePublicTaskAccess access;
    private JdbcTemplate jdbc;
    private final Map<String,Object> trainingOwner=new HashMap<>();
    private final Map<String,Object> containerOwner=new HashMap<>();
    @BeforeEach void setup() {
        previous=SaManager.getSaTokenContext(); SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        MockHttpServletRequest request=new MockHttpServletRequest();
        SaTokenContextForThreadLocalStorage.setBox(new SaRequestForServlet(request),new SaResponseForServlet(new MockHttpServletResponse()),new SaStorageForServlet(request));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        SaHolder.getStorage().set(LoginHelper.USER_KEY,"102"); TenantContextHolder.setTenantId("000000");
        ComputePublicInventoryService inventory=mock(ComputePublicInventoryService.class); when(inventory.viewer()).thenReturn("");
        // These legacy rows have no cloud job or linked training metadata.
        jdbc=mock(JdbcTemplate.class);
        trainingOwner.put("tenant_id","000000"); trainingOwner.put("create_user",101L);
        containerOwner.put("tenant_id","000000"); containerOwner.put("create_user",101L);
        when(jdbc.queryForList(anyString(),anyLong())).thenAnswer(call->{
            String sql=call.getArgument(0);
            if(sql.startsWith("SELECT tenant_id,create_user FROM vls_algorithm_training WHERE"))return Collections.singletonList(trainingOwner);
            if(sql.startsWith("SELECT tenant_id,create_user,instance_type,training_task_id FROM vls_container_instance WHERE"))return Collections.singletonList(containerOwner);
            return Collections.emptyList();
        });
        access=new ComputePublicTaskAccess(jdbc,inventory);
    }
    @AfterEach void cleanup() { RequestContextHolder.resetRequestAttributes();TenantContextHolder.clear();SaTokenContextForThreadLocalStorage.clearBox();SaManager.setSaTokenContext(previous); }
    private void actor(String user,String tenant) { SaHolder.getStorage().set(LoginHelper.USER_KEY,user);TenantContextHolder.setTenantId(tenant); }
    private void creator(Object row,String user) throws Exception {
        Method setter=java.util.Arrays.stream(row.getClass().getMethods()).filter(m->m.getName().equals("setCreateUser") && m.getParameterCount()==1).findFirst().get();
        setter.invoke(row,new Object[]{user==null?null:setter.getParameterTypes()[0]==String.class?user:Long.valueOf(user)});
    }
    @Test void legacyOwnershipDenialIsExplicitForOrdinaryAndAdminActors() {
        for (String user : Arrays.asList("102", "1")) {
            actor(user, "000000");
            assertEquals(Integer.valueOf(403), assertThrows(ServiceException.class, () -> access.training(1L)).getCode());
        }
        actor("101", "000000");
        assertDoesNotThrow(() -> access.training(1L));
    }
    @Test void foreignPublicationReadAndRetryNeverReachPublicationService() {
        TrainingPublicationService publication=mock(TrainingPublicationService.class);
        VlsAlgorithmTrainingController controller=new VlsAlgorithmTrainingController();
        ReflectionTestUtils.setField(controller,"publicAccess",access);
        ReflectionTestUtils.setField(controller,"trainingPublicationService",publication);
        for(String user:Arrays.asList("102","1")) {
            actor(user,"000000");
            assertEquals(Integer.valueOf(403),assertThrows(ServiceException.class,()->controller.publicationStatus(1L)).getCode());
            assertEquals(Integer.valueOf(403),assertThrows(ServiceException.class,()->controller.retryPublication(1L)).getCode());
        }
        verifyNoInteractions(publication);
        actor("101","000000");
        assertEquals(200,controller.publicationStatus(1L).getCode());
        assertEquals(200,controller.retryPublication(1L).getCode());
        verify(publication).status(1L);
        verify(publication).retry(1L);
    }
    @Test void foreignPlainLegacyTrainingNeverReachesSshStop() {
        AlgorithmTraining foreign=new AlgorithmTraining();foreign.setId(1L);foreign.setTenantId("000000");foreign.setCreateUser("101");
        VlsAlgorithmTrainingMapper mapper=mock(VlsAlgorithmTrainingMapper.class);when(mapper.selectAlgorithmTrainingById(1L)).thenReturn(foreign);
        VlsAlgorithmTrainingServiceImpl records=new VlsAlgorithmTrainingServiceImpl();ReflectionTestUtils.setField(records,"algorithmTrainingMapper",mapper);ReflectionTestUtils.setField(records,"publicAccess",access);
        AtomicInteger signals=new AtomicInteger();AtomicReference<String> process=new AtomicReference<>("RUNNING");
        RemoteTrainingService legacy=mock(RemoteTrainingService.class);when(legacy.stopTraining(eq(1L),isNull())).thenAnswer(c->{signals.incrementAndGet();process.set("STOP_SIGNAL_SENT");return false;});
        VlsAlgorithmTrainingController controller=new VlsAlgorithmTrainingController();ReflectionTestUtils.setField(controller,"vlsAlgorithmTrainingService",records);ReflectionTestUtils.setField(controller,"remoteTrainingService",legacy);
        assertAll("A(102) must never stop B(101) legacy training without a cloud row",
            ()->assertThrows(ServiceException.class,()->controller.stopTraining(1L)),
            ()->assertEquals(0,signals.get(),"Foreign SSH transport received a simulated stop"),
            ()->assertEquals("RUNNING",process.get()),()->verifyNoInteractions(legacy));
    }
    @Test void foreignUnlinkedContainerCannotChangeStopState() throws Exception {
        ContainerInstance foreign=new ContainerInstance();foreign.setId(20L);foreign.setTenantId("000000");foreign.setInstanceType("training");foreign.setInstanceStatus("running");creator(foreign,"101");
        IVlsContainerInstanceService records=mock(IVlsContainerInstanceService.class);when(records.getById(20L)).thenReturn(foreign);
        AtomicReference<String> process=new AtomicReference<>("RUNNING");when(records.updateById(any(ContainerInstance.class))).thenAnswer(c->{process.set("STOPPING");return true;});
        VlsContainerInstanceController controller=new VlsContainerInstanceController(records,mock(GpuTrainingSchedulerService.class));ReflectionTestUtils.setField(controller,"publicAccess",access);
        ContainerInstance input=new ContainerInstance();input.setId(20L);input.setInstanceStatus("stopping");
        assertAll("A must not mutate B unlinked legacy container",
            ()->assertThrows(ServiceException.class,()->controller.update(input)),
            ()->assertEquals("RUNNING",process.get()),()->verify(records,never()).updateById(any(ContainerInstance.class)));
    }
    @Test void foreignRunningSmartAnnotationCannotBeCancelled() throws Exception {
        SmartAnnotationTask foreign=new SmartAnnotationTask();foreign.setId(30L);foreign.setDatasetId(40L);foreign.setTenantId("000000");foreign.setTaskState("RUNNING");creator(foreign,"101");
        DataManagementService data=mock(DataManagementService.class);when(data.tenant()).thenReturn("000000");when(data.project(40L)).thenReturn(new AlgorithmAnnotation());
        SmartAnnotationTaskMapper tasks=mock(SmartAnnotationTaskMapper.class);when(tasks.selectOne(any())).thenReturn(foreign);
        SmartAnnotationService service=new SmartAnnotationService(data,tasks,mock(SmartAnnotationRoundMapper.class),mock(SmartAnnotationCandidateMapper.class),mock(VlsAlgorithmModelMapper.class),mock(VlsAlgorithmMapper.class),mock(VlsAnnotationInstanceMapper.class),mock(IVlsAnnotationInstanceService.class),mock(GpuTrainingSchedulerService.class),mock(DataMediaStorage.class),new ObjectMapper());
        assertAll("A must not cancel B running smart annotation",
            ()->assertThrows(ServiceException.class,()->service.cancel(30L)),
            ()->assertEquals("RUNNING",foreign.getTaskState()),()->verify(tasks,never()).updateById(any(SmartAnnotationTask.class)));
    }
    @Test void knownCreatorMayReadButUnprovenLegacyProcessCannotBeStopped() {
        actor("101","000000");
        AlgorithmTraining mine=new AlgorithmTraining();mine.setId(1L);mine.setTenantId("000000");mine.setCreateUser("101");
        VlsAlgorithmTrainingMapper mapper=mock(VlsAlgorithmTrainingMapper.class);when(mapper.selectAlgorithmTrainingById(1L)).thenReturn(mine);
        VlsAlgorithmTrainingServiceImpl records=spy(new VlsAlgorithmTrainingServiceImpl());ReflectionTestUtils.setField(records,"algorithmTrainingMapper",mapper);ReflectionTestUtils.setField(records,"publicAccess",access);
        doReturn(1).when(records).updateAlgorithmTraining(any(AlgorithmTraining.class));
        RemoteTrainingService legacy=new RemoteTrainingService();
        SSHService ssh=mock(SSHService.class);VlsRemoteServersMapper servers=mock(VlsRemoteServersMapper.class);
        ReflectionTestUtils.setField(legacy,"sshService",ssh);
        VlsAlgorithmTrainingController controller=new VlsAlgorithmTrainingController();ReflectionTestUtils.setField(controller,"vlsAlgorithmTrainingService",records);ReflectionTestUtils.setField(controller,"remoteTrainingService",legacy);
        access.training(1L);
        assertTrue(assertThrows(ServiceException.class,()->controller.stopTraining(1L)).getMessage().contains("可信进程身份"));
        for(String path:Arrays.asList("logs/training_1.log","logs/training_999.log","'; kill -9 123 #",""))
            assertThrows(ServiceException.class,()->legacy.stopTraining(1L,path));
        verifyNoInteractions(ssh,servers);verify(records,never()).updateAlgorithmTraining(any(AlgorithmTraining.class));
    }
    @Test void unknownOrMismatchedPlainLegacyOwnerFailsBeforeTransport() {
        AlgorithmTraining row=new AlgorithmTraining();row.setId(1L);row.setTenantId("000000");row.setCreateUser("101");
        VlsAlgorithmTrainingMapper mapper=mock(VlsAlgorithmTrainingMapper.class);when(mapper.selectAlgorithmTrainingById(1L)).thenReturn(row);
        VlsAlgorithmTrainingServiceImpl records=new VlsAlgorithmTrainingServiceImpl();ReflectionTestUtils.setField(records,"algorithmTrainingMapper",mapper);ReflectionTestUtils.setField(records,"publicAccess",access);
        RemoteTrainingService legacy=mock(RemoteTrainingService.class);VlsAlgorithmTrainingController controller=new VlsAlgorithmTrainingController();ReflectionTestUtils.setField(controller,"vlsAlgorithmTrainingService",records);ReflectionTestUtils.setField(controller,"remoteTrainingService",legacy);
        for(String user:Arrays.asList("102","1","0101","101 ","")) {actor(user,"000000");assertThrows(ServiceException.class,()->controller.stopTraining(1L));}
        actor("101","000001");assertThrows(ServiceException.class,()->controller.stopTraining(1L));
        actor("101","000000");for(Object value:Arrays.asList(null,101d,new java.math.BigDecimal("101"))) {trainingOwner.put("create_user",value);assertThrows(ServiceException.class,()->controller.stopTraining(1L));}
        verifyNoInteractions(legacy);verify(mapper,never()).selectAlgorithmTrainingById(anyLong());
    }
    @Test void knownCreatorMayUpdateUnlinkedLegacyContainer() throws Exception {
        actor("101","000000");
        ContainerInstance mine=new ContainerInstance();mine.setId(20L);mine.setTenantId("000000");mine.setInstanceType("training");creator(mine,"101");
        IVlsContainerInstanceService records=mock(IVlsContainerInstanceService.class);when(records.getById(20L)).thenReturn(mine);when(records.updateById(any(ContainerInstance.class))).thenReturn(true);
        VlsContainerInstanceController controller=new VlsContainerInstanceController(records,mock(GpuTrainingSchedulerService.class));ReflectionTestUtils.setField(controller,"publicAccess",access);
        ContainerInstance input=new ContainerInstance();input.setId(20L);input.setErrorMessage("owned display metadata");assertEquals(200,controller.update(input).getCode());verify(records).updateById(input);
    }
    @Test void unlinkedContainerRequiresKnownCreatorAndExactTenant() {
        for(String user:Arrays.asList("102","1","0101","101 ","")) {actor(user,"000000");assertThrows(ServiceException.class,()->access.container(20L));}
        actor("101","000001");assertThrows(ServiceException.class,()->access.container(20L));
        actor("101","000000");for(Object owner:Arrays.asList(null,101d,new java.math.BigDecimal("101"))) {containerOwner.put("create_user",owner);assertThrows(ServiceException.class,()->access.container(20L));}
    }
    @Test void linkedLegacyContainerMayUseTrustedTrainingOwnerButNotForeignTenant() {
        actor("101","000000");containerOwner.put("create_user",null);
        when(jdbc.queryForList(anyString(),eq(Long.class),eq(20L),eq(20L))).thenReturn(Collections.singletonList(1L));
        access.container(20L);
        containerOwner.put("tenant_id","000001");assertThrows(ServiceException.class,()->access.container(20L));
        containerOwner.put("tenant_id","000000");trainingOwner.put("create_user",102L);assertThrows(ServiceException.class,()->access.container(20L));
    }
    private SmartAnnotationService annotation(SmartAnnotationTask task,DataManagementService data,SmartAnnotationTaskMapper tasks) {
        when(data.tenant()).thenReturn("000000");when(data.project(40L)).thenReturn(new AlgorithmAnnotation());when(tasks.selectOne(any())).thenReturn(task);
        return new SmartAnnotationService(data,tasks,mock(SmartAnnotationRoundMapper.class),mock(SmartAnnotationCandidateMapper.class),mock(VlsAlgorithmModelMapper.class),mock(VlsAlgorithmMapper.class),mock(VlsAnnotationInstanceMapper.class),mock(IVlsAnnotationInstanceService.class),mock(GpuTrainingSchedulerService.class),mock(DataMediaStorage.class),new ObjectMapper());
    }
    private SmartAnnotationTask annotationTask(String owner) throws Exception {
        SmartAnnotationTask task=new SmartAnnotationTask();task.setId(30L);task.setDatasetId(40L);task.setTenantId("000000");task.setTaskState("RUNNING");creator(task,owner);return task;
    }
    @Test void knownCreatorMayCancelOwnRunningSmartAnnotation() throws Exception {
        actor("101","000000");SmartAnnotationTask task=annotationTask("101");DataManagementService data=mock(DataManagementService.class);SmartAnnotationTaskMapper tasks=mock(SmartAnnotationTaskMapper.class);
        SmartAnnotationService service=annotation(task,data,tasks);service.cancel(30L);assertEquals("CANCEL_REQUESTED",task.getTaskState());verify(tasks).updateById(task);verify(data).lockDatasetForTask(40L);
    }
    @Test void smartAnnotationRejectsMissingOwnerIdentityTenantAndAdminWithoutWrites() throws Exception {
        SmartAnnotationTask task=annotationTask("101");DataManagementService data=mock(DataManagementService.class);SmartAnnotationTaskMapper tasks=mock(SmartAnnotationTaskMapper.class);SmartAnnotationService service=annotation(task,data,tasks);
        for(String user:Arrays.asList("102","1","0101","101 ","")){actor(user,"000000");assertThrows(ServiceException.class,()->service.cancel(30L));assertEquals("RUNNING",task.getTaskState());}
        actor("101","000000");task.setTenantId("000001");assertThrows(ServiceException.class,()->service.cancel(30L));task.setTenantId("000000");creator(task,null);assertThrows(ServiceException.class,()->service.cancel(30L));
        verify(tasks,never()).updateById(any(SmartAnnotationTask.class));verify(data,never()).lockDatasetForTask(anyLong());
    }
    @Test void smartAnnotationRechecksCreatorAfterDatasetLock() throws Exception {
        actor("101","000000");SmartAnnotationTask mine=annotationTask("101"),changed=annotationTask("102");DataManagementService data=mock(DataManagementService.class);SmartAnnotationTaskMapper tasks=mock(SmartAnnotationTaskMapper.class);SmartAnnotationService service=annotation(mine,data,tasks);when(tasks.selectOne(any())).thenReturn(mine,changed);
        assertThrows(ServiceException.class,()->service.cancel(30L));assertEquals("RUNNING",mine.getTaskState());assertEquals("RUNNING",changed.getTaskState());verify(tasks,never()).updateById(any(SmartAnnotationTask.class));
    }

    @Test void foreignAndUnknownCreatorCannotUseAnySmartAnnotationControl() throws Exception {
        SmartAnnotationTask task=annotationTask("101");DataManagementService data=mock(DataManagementService.class);SmartAnnotationTaskMapper tasks=mock(SmartAnnotationTaskMapper.class);SmartAnnotationService service=annotation(task,data,tasks);
        for(String state:Arrays.asList("RUNNING","REVIEW","FAILED","CONFIRMING")) {
            task.setTaskState(state);
            for(String user:Arrays.asList("102","1","0101","101 ","")) {
                actor(user,"000000");
                assertThrows(ServiceException.class,()->service.next(30L));assertThrows(ServiceException.class,()->service.finish(30L));
                assertThrows(ServiceException.class,()->service.retry(30L));assertThrows(ServiceException.class,()->service.confirmAll(30L));
                assertThrows(ServiceException.class,()->service.confirmBatch(30L,Collections.emptyList()));
                assertThrows(ServiceException.class,()->service.review(30L,50L,new SmartAnnotationRequests.Review()));
                assertEquals(state,task.getTaskState());
            }
        }
        actor("101","000000");creator(task,null);assertThrows(ServiceException.class,()->service.finish(30L));
        verify(data,never()).lockDatasetForTask(anyLong());verify(tasks,never()).updateById(any(SmartAnnotationTask.class));verify(tasks,never()).update(any(),any());
    }
    @Test void ownerMayFinishAndTrustedBackgroundWorkerMayConfirmWithoutHttpSession() throws Exception {
        actor("101","000000");SmartAnnotationTask task=annotationTask("101");task.setTaskState("REVIEW");task.setTaskName("fixture");
        DataManagementService data=mock(DataManagementService.class);SmartAnnotationTaskMapper tasks=mock(SmartAnnotationTaskMapper.class);SmartAnnotationService service=annotation(task,data,tasks);
        service.finish(30L);assertEquals("COMPLETED",task.getTaskState());verify(tasks).updateById(task);verify(data).saveVersion(eq(40L),any());
        RequestContextHolder.resetRequestAttributes();actor("","000000");task.setTaskState("CONFIRMING");
        // The existing worker calls review in a tenant context, with no browser session.
        SmartAnnotationCandidateMapper candidates=mock(SmartAnnotationCandidateMapper.class);SmartAnnotationRoundMapper rounds=mock(SmartAnnotationRoundMapper.class);
        SmartAnnotationRound round=new SmartAnnotationRound();round.setId(50L);when(rounds.selectOne(any())).thenReturn(round);
        SmartAnnotationCandidate candidate=new SmartAnnotationCandidate();candidate.setReviewState("PENDING");when(candidates.selectOne(any())).thenReturn(candidate);
        AlgorithmAnnotation project=new AlgorithmAnnotation();project.setAnnotationType("object_detection");when(data.project(40L)).thenReturn(project);task.setAnnotationType("object_detection");
        service=new SmartAnnotationService(data,tasks,rounds,candidates,mock(VlsAlgorithmModelMapper.class),mock(VlsAlgorithmMapper.class),mock(VlsAnnotationInstanceMapper.class),mock(IVlsAnnotationInstanceService.class),mock(GpuTrainingSchedulerService.class),mock(DataMediaStorage.class),new ObjectMapper());
        SmartAnnotationRequests.Review review=new SmartAnnotationRequests.Review();review.setSkip(true);service.review(30L,60L,review);
        assertEquals("SKIPPED",candidate.getReviewState());verify(candidates).updateById(candidate);
    }
    @Test void containerDtoCannotRebindOwnedRecordOrSpoofCreatorOnCreation() throws Exception {
        actor("101","000000");ContainerInstance stored=new ContainerInstance();stored.setId(20L);stored.setTenantId("000000");creator(stored,"101");stored.setContainerId("owned-container");stored.setLogsPath("owned.log");stored.setInstanceType("training");
        IVlsContainerInstanceService records=mock(IVlsContainerInstanceService.class);when(records.getById(20L)).thenReturn(stored);
        VlsContainerInstanceController controller=new VlsContainerInstanceController(records,mock(GpuTrainingSchedulerService.class));ReflectionTestUtils.setField(controller,"publicAccess",access);
        String[] fields={"tenantId","createUser","trainingTaskId","instanceType","containerId","instanceName","serverId","serverIp","gpuIndex","gpuUuid","logsPath"};
        Object[] values={"000001","102",999L,"custom","foreign-container","foreign-name",999L,"foreign-server",1,"foreign-gpu","foreign.log"};
        for(int i=0;i<fields.length;i++) {
            ContainerInstance input=new ContainerInstance();input.setId(20L);org.springframework.beans.BeanWrapper bean=new org.springframework.beans.BeanWrapperImpl(input);bean.setPropertyValue(fields[i],values[i]);
            assertThrows(ServiceException.class,()->controller.update(input),fields[i]);assertThrows(ServiceException.class,()->controller.submit(input),fields[i]);assertThrows(ServiceException.class,()->controller.save(input),fields[i]);
        }
        verify(records,never()).updateById(any(ContainerInstance.class));verify(records,never()).saveOrUpdate(any(ContainerInstance.class));verify(records,never()).save(any(ContainerInstance.class));
        ContainerInstance fresh=new ContainerInstance();fresh.setTenantId("000001");creator(fresh,"102");access.prepareContainerCreate(fresh);
        assertEquals("000000",fresh.getTenantId());assertEquals("101",String.valueOf(fresh.getCreateUser()));
    }
    @Test void directContainerServicesDenyForeignStopRestartAndMixedBatchBeforeAnyWrite() {
        com.ruoyi.vlstream.test.vlstream.service.impl.VlsContainerInstanceServiceImpl service=new com.ruoyi.vlstream.test.vlstream.service.impl.VlsContainerInstanceServiceImpl();
        VlsContainerInstanceMapper mapper=mock(VlsContainerInstanceMapper.class);ReflectionTestUtils.setField(service,"baseMapper",mapper);ReflectionTestUtils.setField(service,"publicAccess",access);
        assertThrows(ServiceException.class,()->service.stopContainer(20L));assertThrows(ServiceException.class,()->service.restartContainer(20L));
        assertThrows(ServiceException.class,()->service.startContainer(20L,"foreign-container"));
        ContainerInstance input=new ContainerInstance();input.setId(20L);input.setInstanceStatus("stopping");assertThrows(ServiceException.class,()->service.updateById(input));
        Map<String,Object> own=new HashMap<>();own.put("tenant_id","000000");own.put("create_user",102L);
        when(jdbc.queryForList(eq("SELECT tenant_id,create_user,instance_type,training_task_id FROM vls_container_instance WHERE id=?"),eq(21L))).thenReturn(Collections.singletonList(own));
        assertThrows(ServiceException.class,()->service.deleteContainerInstanceBatch(Arrays.asList(21L,20L)));verifyNoInteractions(mapper);
    }
    @Test void trainingDtoCannotAlterOwnerTenantOrLogPathEvenForOwnRecord() {
        actor("101","000000");AlgorithmTraining stored=new AlgorithmTraining();stored.setId(1L);stored.setTenantId("000000");stored.setCreateUser("101");stored.setLogPath("owned.log");
        IVlsAlgorithmTrainingService records=mock(IVlsAlgorithmTrainingService.class);when(records.selectAlgorithmTrainingById(1L)).thenReturn(stored);
        VlsAlgorithmTrainingController controller=new VlsAlgorithmTrainingController();ReflectionTestUtils.setField(controller,"vlsAlgorithmTrainingService",records);
        for(String field:Arrays.asList("tenantId","createUser","logPath")) {
            AlgorithmTraining input=new AlgorithmTraining();input.setId(1L);new org.springframework.beans.BeanWrapperImpl(input).setPropertyValue(field,field.equals("tenantId")?"000001":field.equals("createUser")?"102":"foreign.log");
            assertThrows(ServiceException.class,()->controller.update(input));assertThrows(ServiceException.class,()->controller.submit(input));
        }
        verify(records,never()).updateById(any(AlgorithmTraining.class));verify(records,never()).saveOrUpdate(any(AlgorithmTraining.class));
    }

}
