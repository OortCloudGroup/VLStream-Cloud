package com.ruoyi.vlstream.test.vlstream.compute;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.*;
import cn.dev33.satoken.servlet.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.*;
import com.ruoyi.vlstream.test.vlstream.config.*;
import com.ruoyi.vlstream.test.vlstream.controller.VlsContainerInstanceController;
import com.ruoyi.vlstream.test.vlstream.data.*;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmCategoryEnum;
import com.ruoyi.vlstream.test.vlstream.mapper.*;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import com.ruoyi.vlstream.test.vlstream.service.*;
import com.ruoyi.vlstream.test.vlstream.service.impl.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.parallel.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Real start/enqueue + real guarded container service; persistence/transport are isolated doubles. */
@Tag("dev") @Execution(ExecutionMode.SAME_THREAD)
class TaskRecordAdmissionTest {
    SaTokenContext previous;
    JdbcTemplate jdbc;
    ComputePublicTaskAccess access;
    VlsContainerInstanceServiceImpl containers;
    VlsContainerInstanceMapper mapper;
    VlsAlgorithmTrainingServiceImpl trainings;
    VlsAlgorithmTrainingMapper trainingMapper;
    GpuTrainingSchedulerService scheduler;
    SSHService ssh;
    AlgorithmTraining training;
    Map<String,Object> owner=new HashMap<>();
    Map<String,Object> roundOwner=new HashMap<>();
    AtomicReference<ContainerInstance> inserted=new AtomicReference<>();
    @BeforeEach void setup() {
        previous=SaManager.getSaTokenContext();SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        MockHttpServletRequest request=new MockHttpServletRequest();SaTokenContextForThreadLocalStorage.setBox(new SaRequestForServlet(request),new SaResponseForServlet(new MockHttpServletResponse()),new SaStorageForServlet(request));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));SaHolder.getStorage().set(LoginHelper.USER_KEY,"101");TenantContextHolder.setTenantId("000000");
        owner.put("tenant_id","000000");owner.put("create_user",101L);roundOwner.putAll(owner);
        jdbc=mock(JdbcTemplate.class);
        when(jdbc.queryForList(anyString(),anyLong())).thenAnswer(call->{
            String sql=call.getArgument(0);
            if(sql.startsWith("SELECT tenant_id,create_user FROM vls_algorithm_training WHERE"))return Collections.singletonList(owner);
            if(sql.startsWith("SELECT t.tenant_id,t.create_user,r.tenant_id AS round_tenant_id"))return Collections.singletonList(roundOwner);
            if(sql.startsWith("SELECT tenant_id,create_user,instance_type,training_task_id FROM vls_container_instance") && inserted.get()!=null) {
                ContainerInstance row=inserted.get();Map<String,Object> record=new HashMap<>();record.put("tenant_id",row.getTenantId());record.put("create_user",row.getCreateUser());record.put("instance_type",row.getInstanceType());record.put("training_task_id",row.getTrainingTaskId());return Collections.singletonList(record);
            }
            return Collections.emptyList();
        });
        when(jdbc.queryForList(anyString(),anyLong(),anyLong(),anyString())).thenAnswer(call->{
            ContainerInstance row=inserted.get();if(row==null)return Collections.emptyList();
            return Collections.singletonList(Collections.singletonMap("logs_path",row.getLogsPath()));
        });
        ComputePublicInventoryService inventory=mock(ComputePublicInventoryService.class);when(inventory.viewer()).thenReturn("");access=new ComputePublicTaskAccess(jdbc,inventory);
        mapper=mock(VlsContainerInstanceMapper.class);when(mapper.selectList(any())).thenReturn(Collections.emptyList());when(mapper.selectCount(any())).thenReturn(0L);
        when(mapper.insert(any(ContainerInstance.class))).thenAnswer(call->{ContainerInstance row=call.getArgument(0);row.setId(20L);inserted.set(row);return 1;});
        when(mapper.selectById(anyLong())).thenAnswer(call->inserted.get());
        when(mapper.updateById(any(ContainerInstance.class))).thenAnswer(call->{ContainerInstance change=call.getArgument(0);if(change.getInstanceStatus()!=null)inserted.get().setInstanceStatus(change.getInstanceStatus());return 1;});
        containers=new VlsContainerInstanceServiceImpl();ReflectionTestUtils.setField(containers,"baseMapper",mapper);ReflectionTestUtils.setField(containers,"publicAccess",access);
        training=new AlgorithmTraining();training.setId(1L);training.setTenantId("000000");training.setCreateUser("101");training.setTaskName("fixture");training.setAlgorithmId(2L);
        trainingMapper=mock(VlsAlgorithmTrainingMapper.class);when(trainingMapper.selectById(1L)).thenReturn(training);when(trainingMapper.selectAlgorithmTrainingById(1L)).thenReturn(training);when(trainingMapper.updateById(any(AlgorithmTraining.class))).thenAnswer(call->{AlgorithmTraining change=call.getArgument(0);if(change.getConfigParams()!=null)training.setConfigParams(change.getConfigParams());if(change.getLogPath()!=null)training.setLogPath(change.getLogPath());return 1;});
        trainings=new VlsAlgorithmTrainingServiceImpl();ReflectionTestUtils.setField(trainings,"baseMapper",trainingMapper);ReflectionTestUtils.setField(trainings,"algorithmTrainingMapper",trainingMapper);ReflectionTestUtils.setField(trainings,"publicAccess",access);
        scheduler=new GpuTrainingSchedulerService();ReflectionTestUtils.setField(scheduler,"containerInstanceService",containers);ReflectionTestUtils.setField(scheduler,"algorithmTrainingService",trainings);ReflectionTestUtils.setField(scheduler,"objectMapper",new ObjectMapper());ReflectionTestUtils.setField(scheduler,"properties",new VlsTrainingContainerProperties());
        VlsSshProperties properties=new VlsSshProperties();properties.setHost("203.0.113.10");ReflectionTestUtils.setField(scheduler,"sshProperties",properties);
        RemoteServers server=new RemoteServers();server.setId(3L);server.setServerIp("203.0.113.10");server.setWorkDir("/fixture");server.setCondaEnv("fixture");
        VlsRemoteServersMapper servers=mock(VlsRemoteServersMapper.class);when(servers.selectActiveServer()).thenReturn(server);ReflectionTestUtils.setField(scheduler,"remoteServersMapper",servers);
        ssh=mock(SSHService.class);ReflectionTestUtils.setField(scheduler,"sshService",ssh);
        ReflectionTestUtils.setField(scheduler,"containerInstanceMapper",mapper);
    }
    @AfterEach void cleanup() {RequestContextHolder.resetRequestAttributes();TenantContextHolder.clear();SaTokenContextForThreadLocalStorage.clearBox();SaManager.setSaTokenContext(previous);}
    private CloudTrainingService cloud() {
        ComputeNodeService nodes=mock(ComputeNodeService.class);ComputeNode node=new ComputeNode();node.setId(3L);node.setTenantId("000000");node.setHost("203.0.113.10");node.setWorkDir("/fixture");node.setGpuIndex(0);
        when(nodes.requireReady(3L)).thenReturn(node);when(nodes.trainingSubject(node,"101")).thenReturn("fixture-owner");when(nodes.isCompanyShared(node)).thenReturn(true);
        DataManagementService data=mock(DataManagementService.class);AlgorithmAnnotation dataset=new AlgorithmAnnotation();dataset.setAnnotationType("object_detection");when(data.project(4L)).thenReturn(dataset);
        DatasetSnapshot snapshot=new DatasetSnapshot();snapshot.setAnnotationType("object_detection");AnnotationLabel label=new AnnotationLabel();label.setId(5L);label.setName("fixture");snapshot.setLabels(Collections.singletonList(label));
        List<AnnotationImage> images=new ArrayList<>();List<AnnotationInstance> annotations=new ArrayList<>();for(int i=0;i<2;i++){AnnotationImage image=new AnnotationImage();image.setId(6L+i);image.setDatasetSplit(i==0?"train":"val");images.add(image);AnnotationInstance annotation=new AnnotationInstance();annotation.setImageId(image.getId());annotations.add(annotation);}snapshot.setSamples(images);snapshot.setInstances(annotations);when(data.snapshot(4L)).thenReturn(snapshot);
        DatasetVersion version=new DatasetVersion();version.setId(8L);version.setSnapshotJson("{}");when(data.saveVersion(eq(4L),any())).thenReturn(version);
        VlsAlgorithmMapper algorithms=mock(VlsAlgorithmMapper.class);Algorithm algorithm=new Algorithm();algorithm.setId(2L);algorithm.setTenantId("000000");algorithm.setCategory(AlgorithmCategoryEnum.detect);when(algorithms.selectById(2L)).thenReturn(algorithm);
        CloudTrainingService service=new CloudTrainingService(jdbc,nodes,data,mock(TrainingPublicationService.class),trainings,containers,algorithms,new ObjectMapper());ReflectionTestUtils.setField(service,"publicAccess",access);
        ComputeSharedQueueService queue=mock(ComputeSharedQueueService.class);when(queue.verifyBaseModel(anyString(),anyString(),anyString())).thenAnswer(call->call.getArgument(0));when(queue.configuration(any(),anyString(),anyString())).thenReturn(Collections.singletonMap("mode","SHARED_GPU_QUEUE"));ReflectionTestUtils.setField(service,"sharedQueue",queue);return service;
    }
    private ComputeRequests.Start request(){ComputeRequests.Start request=new ComputeRequests.Start();request.setNodeId(3L);request.setDatasetId(4L);request.setEpochs(1);request.setBatchSize(2);request.setImgSize(320);request.setModelSource("preset");return request;}
    private void admitted(String type,long reference) {
        ContainerInstance row=inserted.get();assertNotNull(row);assertEquals(type,row.getInstanceType());assertEquals(reference,row.getTrainingTaskId());assertEquals("000000",row.getTenantId());assertEquals("101",String.valueOf(row.getCreateUser()));verify(mapper).insert(row);verifyNoInteractions(ssh);assertNotNull(RequestContextHolder.getRequestAttributes());
    }
    @Test void sharedCloudStartPersistsTrustedRecordInsideActualHttpComposition() {cloud().start(1L,request());admitted("cloud_training",1L);}
    @Test void legacyTrainingEnqueuePersistsTrustedRecordInsideActualHttpComposition() {scheduler.enqueue("detect",1L,"/fixture/dataset.yaml","@preset/detect",1,2,320);admitted("training",1L);verify(trainingMapper).updateById(argThat(row->inserted.get().getLogsPath().equals(row.getLogPath())));}
    @Test void annotationEnqueuePersistsTrustedRoundRecordInsideActualHttpComposition() {scheduler.enqueueSmartAnnotation(30L);admitted("smart_annotation",30L);access.container(20L);}
    @Test void foreignTaskCannotUseAnyTrustedEnqueuePath() {
        SaHolder.getStorage().set(LoginHelper.USER_KEY,"102");assertThrows(ServiceException.class,()->cloud().start(1L,request()));assertThrows(ServiceException.class,()->scheduler.enqueue("detect",1L,"/fixture/data","@preset/detect",1,2,320));assertThrows(ServiceException.class,()->scheduler.enqueueSmartAnnotation(30L));verify(mapper,never()).insert(any(ContainerInstance.class));verify(trainingMapper,never()).updateById(any(AlgorithmTraining.class));verifyNoInteractions(ssh);
    }
    @Test void manualDtoCannotInvokeTrustedRecordCreationOrSpoofTypeAndLink() {
        ContainerInstance forged=new ContainerInstance();forged.setInstanceType("training");forged.setTrainingTaskId(1L);forged.setServerId(3L);forged.setLogsPath("foreign.log");
        VlsContainerInstanceController controller=new VlsContainerInstanceController(containers,scheduler);ReflectionTestUtils.setField(controller,"publicAccess",access);
        assertThrows(ServiceException.class,()->controller.save(forged));assertThrows(ServiceException.class,()->containers.save(forged));verify(mapper,never()).insert(any(ContainerInstance.class));
    }
    @Test void roundOwnerCannotBeBorrowedFromUnrelatedOwnedOrdinaryTrainingId() {
        scheduler.enqueueSmartAnnotation(30L);roundOwner.put("create_user",102L);assertThrows(ServiceException.class,()->access.container(20L));roundOwner.put("create_user",101L);roundOwner.put("tenant_id","000001");assertThrows(ServiceException.class,()->access.container(20L));roundOwner.put("tenant_id","000000");inserted.get().setCreateUser("102");assertThrows(ServiceException.class,()->access.container(20L));
    }
    @Test void trustedNonHttpWorkerDerivesKnownPersistedCreatorAndDeniesUnknownOwner() {
        RequestContextHolder.resetRequestAttributes();SaHolder.getStorage().set(LoginHelper.USER_KEY,"");scheduler.enqueueSmartAnnotation(30L);assertEquals("101",String.valueOf(inserted.get().getCreateUser()));roundOwner.put("create_user",null);assertThrows(ServiceException.class,()->scheduler.enqueueSmartAnnotation(30L));verify(mapper,times(1)).insert(any(ContainerInstance.class));verifyNoInteractions(ssh);
    }
    private void dispatch(ContainerInstance row) {
        RequestContextHolder.resetRequestAttributes();when(mapper.selectNextQueuedTrainingForScheduler()).thenReturn(row);
        ReflectionTestUtils.invokeMethod(scheduler,"dispatchNext");
    }
    private VlsContainerInstanceController manualController() {VlsContainerInstanceController controller=new VlsContainerInstanceController(containers,scheduler);ReflectionTestUtils.setField(controller,"publicAccess",access);return controller;}
    @Test void manualReservedCreateCannotReachEnabledLegacyDispatcherEvenAsOldPersistedRow() {
        ContainerInstance forged=new ContainerInstance();forged.setInstanceType("training");forged.setInstanceStatus("queued");forged.setInstanceName("foreign-container");forged.setEnvConfig("{\"launchCommand\":\"SYNTHETIC_STOP_OTHER_USER_TASK\"}");
        assertThrows(ServiceException.class,()->manualController().save(forged));verify(mapper,never()).insert(any(ContainerInstance.class));
        forged.setId(20L);forged.setTenantId("000000");forged.setCreateUser("101");dispatch(forged);verifyNoInteractions(ssh);verify(mapper,never()).updateById(any(ContainerInstance.class));
    }
    @Test void manualMetadataCreationAndEditingCannotRequeueOrProvideExecutableText() {
        VlsContainerInstanceController controller=manualController();ContainerInstance metadata=new ContainerInstance();metadata.setInstanceName("owned display name");metadata.setEnvConfig("{\"FOO\":\"hello\"}");assertEquals(200,controller.save(metadata).getCode());assertEquals("custom",metadata.getInstanceType());assertEquals("stopped",metadata.getInstanceStatus());
        for(String field:Arrays.asList("envConfig","instanceStatus","instanceType")) {
            ContainerInstance edit=new ContainerInstance();edit.setId(20L);new org.springframework.beans.BeanWrapperImpl(edit).setPropertyValue(field,field.equals("envConfig")?"{\"launchCommand\":\"SYNTHETIC_STOP_OTHER_USER_TASK\"}":field.equals("instanceStatus")?"queued":"training");
            assertThrows(ServiceException.class,()->controller.update(edit));assertEquals("stopped",metadata.getInstanceStatus());
        }
        ContainerInstance variables=new ContainerInstance();variables.setId(20L);variables.setEnvConfig("{\"FOO\":\"bounded value\"}");assertEquals(200,controller.update(variables).getCode());
        dispatch(metadata);verifyNoInteractions(ssh);assertEquals("stopped",metadata.getInstanceStatus());
    }
    @Test void allOwnedExecutionAndSchedulingDtoFieldsRemainImmutable() {
        scheduler.enqueue("detect",1L,"/fixture/dataset.yaml","@preset/detect",1,2,320);String original=inserted.get().getEnvConfig();clearInvocations(mapper);
        String[] fields={"envConfig","portConfig","volumeConfig","imageName","imageType","imageTag","cpuLimit","memoryLimit","gpuLimit","resourceTypeId","resourceSpecId","instanceCount","algorithmId","instanceStatus","healthStatus","queueTime","startTime","stopTime","restartCount"};
        Object[] values={"{\"launchCommand\":\"SYNTHETIC_STOP_OTHER_USER_TASK\"}","{}","{}","foreign-image","foreign-type","foreign-tag","1","1g","GPU1",9L,9L,2,9L,"running","healthy",new Date(0),new Date(0),new Date(0),1};
        for(int i=0;i<fields.length;i++) {ContainerInstance edit=new ContainerInstance();edit.setId(20L);new org.springframework.beans.BeanWrapperImpl(edit).setPropertyValue(fields[i],values[i]);assertThrows(ServiceException.class,()->manualController().update(edit),fields[i]);assertEquals(original,inserted.get().getEnvConfig());}
        verify(mapper,never()).updateById(any(ContainerInstance.class));verifyNoInteractions(ssh);assertEquals("queued",inserted.get().getInstanceStatus());
    }
    private List<String> simulatedTransport() {
        List<String> forwarded=new ArrayList<>();when(ssh.executeCommand(anyString(),anyInt(),anyString(),anyString(),anyString())).thenAnswer(call->{String command=call.getArgument(4);SSHService.SSHExecutionResult result=new SSHService.SSHExecutionResult();result.setSuccess(true);result.setOutput(command.contains("--query-compute-apps")?"0":"owned-container-id");if(!command.contains("--query-compute-apps"))forwarded.add(command);return result;});return forwarded;
    }
    @Test void ownedStructuredDefinitionDispatchesRebuiltCommandAndNeverRawLaunchText() throws Exception {
        scheduler.enqueue("detect",1L,"/fixture/dataset.yaml","@preset/detect",1,2,320);
        com.fasterxml.jackson.databind.node.ObjectNode env=(com.fasterxml.jackson.databind.node.ObjectNode)new ObjectMapper().readTree(inserted.get().getEnvConfig());env.put("launchCommand","SYNTHETIC_STOP_OTHER_USER_TASK");inserted.get().setEnvConfig(env.toString());
        List<String> forwarded=simulatedTransport();dispatch(inserted.get());assertEquals(1,forwarded.size());assertFalse(forwarded.get(0).contains("SYNTHETIC_STOP_OTHER_USER_TASK"));assertTrue(forwarded.get(0).contains("vls-training-1"));assertEquals("running",inserted.get().getInstanceStatus());
    }
    @Test void oldRawCommandWithKnownAssociationMustMatchTrustedReconstruction() throws Exception {
        scheduler.enqueue("detect",1L,"/fixture/dataset.yaml","@preset/detect",1,2,320);ContainerInstance row=inserted.get();
        com.fasterxml.jackson.databind.JsonNode def=new ObjectMapper().readTree(row.getEnvConfig()).path("launchDefinition");
        com.fasterxml.jackson.databind.node.ObjectNode config=(com.fasterxml.jackson.databind.node.ObjectNode)new ObjectMapper().readTree(training.getConfigParams());config.put("runtimeDatasetPath","/fixture/dataset.yaml");config.put("epochs",1);config.put("batchSize",2);config.put("imgSize",320);training.setConfigParams(config.toString());
        IVlsAlgorithmService algorithms=mock(IVlsAlgorithmService.class);Algorithm algorithm=new Algorithm();algorithm.setTenantId("000000");algorithm.setCategory(AlgorithmCategoryEnum.detect);when(algorithms.getById(2L)).thenReturn(algorithm);ReflectionTestUtils.setField(scheduler,"algorithmService",algorithms);
        RemoteServers server=ReflectionTestUtils.invokeMethod(scheduler,"requireServer");String rebuilt=ReflectionTestUtils.invokeMethod(scheduler,"buildDockerRunCommand",server,row.getInstanceName(),row.getLogsPath(),"detect","/fixture/dataset.yaml","@preset/detect",1,2,320,def.path("runName").asText());
        row.setEnvConfig("{\"launchCommand\":\"SYNTHETIC_STOP_OTHER_USER_TASK\"}");dispatch(row);verifyNoInteractions(ssh);assertEquals("queued",row.getInstanceStatus());
        row.setEnvConfig(new ObjectMapper().writeValueAsString(Collections.singletonMap("launchCommand",rebuilt)));List<String> forwarded=simulatedTransport();dispatch(row);assertEquals(Collections.singletonList(rebuilt),forwarded);assertEquals("running",row.getInstanceStatus());
    }
    @Test void linkedRecordWithForeignPersistedCreatorCannotProbeOrExecute() {
        scheduler.enqueue("detect",1L,"/fixture/dataset.yaml","@preset/detect",1,2,320);owner.put("create_user",102L);dispatch(inserted.get());verifyNoInteractions(ssh);assertEquals("queued",inserted.get().getInstanceStatus());
    }
    @Test void explicitOwnedLifecycleStopRemainsAllowedWithoutClientExecutionMutation() {
        scheduler.enqueue("detect",1L,"/fixture/dataset.yaml","@preset/detect",1,2,320);assertTrue(containers.stopContainer(20L));assertEquals("stopping",inserted.get().getInstanceStatus());verifyNoInteractions(ssh);
    }
}
