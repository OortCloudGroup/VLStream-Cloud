package com.ruoyi.vlstream.test.vlstream.cloudai;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.servlet.model.SaRequestForServlet;
import cn.dev33.satoken.servlet.model.SaResponseForServlet;
import cn.dev33.satoken.servlet.model.SaStorageForServlet;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.ruoyi.vlstream.test.vlstream.controller.AlgorithmCatalogController;
import com.ruoyi.vlstream.test.vlstream.controller.VlsAlgorithmController;
import com.ruoyi.vlstream.test.vlstream.controller.VlsLlmReviewController;
import com.ruoyi.vlstream.test.vlstream.controller.VlsDataManagementController;
import com.ruoyi.vlstream.test.vlstream.controller.VlsSmartAnnotationController;
import com.ruoyi.vlstream.test.vlstream.controller.VlsDatasetAnnotationController;
import com.ruoyi.vlstream.test.vlstream.controller.VlsAlgorithmTrainingController;
import com.ruoyi.vlstream.test.vlstream.controller.VlsAlgorithmModelController;
import com.ruoyi.vlstream.test.vlstream.controller.VlsEventManagementController;
import com.ruoyi.vlstream.test.vlstream.controller.VlsModelDispatchController;
import com.ruoyi.vlstream.test.vlstream.controller.VlsComputeQueueController;
import com.ruoyi.vlstream.test.vlstream.controller.VlsAlgorithmAnnotationController;
import com.ruoyi.vlstream.test.vlstream.controller.VlsAnnotationLabelController;
import com.ruoyi.vlstream.test.vlstream.controller.VlsAnnotationImageController;
import com.ruoyi.vlstream.test.vlstream.controller.VlsAnnotationInstanceController;
import com.ruoyi.vlstream.test.vlstream.controller.CompanyComputeOfferController;
import com.ruoyi.vlstream.test.vlstream.controller.VlsDeviceModelController;
import com.ruoyi.vlstream.test.vlstream.controller.VlsAlgorithmOrchestrationController;
import com.ruoyi.vlstream.test.vlstream.controller.VlsAlgorithmRepositoryController;
import com.ruoyi.vlstream.test.vlstream.controller.VlsContainerInstanceController;
import com.ruoyi.vlstream.test.vlstream.controller.VlsDeviceInfoController;
import com.ruoyi.vlstream.test.vlstream.service.DeviceModelService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAlgorithmOrchestrationService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAlgorithmRepositoryService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsContainerInstanceService;
import com.ruoyi.vlstream.test.vlstream.service.GpuTrainingSchedulerService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsDeviceTagRelationService;
import org.springframework.beans.factory.ObjectProvider;
import com.ruoyi.vlstream.test.vlstream.compute.CompanyComputeOfferService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAnnotationLabelService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAnnotationImageService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAnnotationInstanceService;
import com.ruoyi.vlstream.test.vlstream.compute.ComputeQueueSnapshotService;
import com.ruoyi.vlstream.test.vlstream.data.DataTrainingPublisher;
import com.ruoyi.vlstream.test.vlstream.data.DatasetGenerationJobs;
import com.ruoyi.vlstream.test.vlstream.data.DataManagementService;
import com.ruoyi.vlstream.test.vlstream.data.DataTransferService;
import com.ruoyi.vlstream.test.vlstream.data.DatasetAnnotationEditor;
import com.ruoyi.vlstream.test.vlstream.data.SmartAnnotationService;
import com.ruoyi.vlstream.test.vlstream.data.SmartAnnotationWorker;
import com.ruoyi.vlstream.test.vlstream.service.AlgorithmCatalogService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAlgorithmService;
import com.ruoyi.vlstream.test.vlstream.service.LlmReviewManagementService;
import com.ruoyi.vlstream.test.vlstream.service.LlmReviewTaskService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAlgorithmTrainingService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAlgorithmAnnotationService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAlgorithmModelService;
import com.ruoyi.vlstream.test.vlstream.service.ModelFileDownloadService;
import com.ruoyi.vlstream.test.vlstream.service.ImportedAlgorithmModelService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsEventManagementService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsDeviceInfoService;
import com.ruoyi.vlstream.test.vlstream.service.VlsEventReportApplicationService;
import com.ruoyi.vlstream.test.vlstream.service.VlsDeviceEventReportService;
import com.ruoyi.vlstream.test.vlstream.service.SavedModelDispatchService;
import com.ruoyi.vlstream.test.vlstream.service.ModelDispatchTaskService;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exercise the real annotation interceptor, including shared page dependencies. */
@Tag("dev")
class CloudAiControllerPermissionTest {
    private SaTokenContext previousContext;
    private SaTokenDao previousDao;
    private StpInterface previousPermissions;
    private SaTokenDaoDefaultImpl memory;
    private Set<String> permissions;
    private CloudAiService service;
    private CloudAiSources sources;
    private CloudAiEvents events;
    private CloudAiModels models;
    private IVlsAlgorithmService algorithms;
    private AlgorithmCatalogService catalog;
    private LlmReviewManagementService llmManagement;
    private LlmReviewTaskService llmTasks;
    private DataManagementService data;
    private SmartAnnotationService smart;
    private DatasetAnnotationEditor editor;
    private IVlsAlgorithmTrainingService training;
    private IVlsAlgorithmModelService algorithmModels;
    private IVlsEventManagementService eventManagement;
    private SavedModelDispatchService savedDispatch;
    private ModelDispatchTaskService dispatchTasks;
    private ComputeQueueSnapshotService queues;
    private IVlsAlgorithmAnnotationService annotations;
    private IVlsAnnotationLabelService labels;
    private IVlsAnnotationImageService annotationImages;
    private IVlsAnnotationInstanceService annotationInstances;
    private CompanyComputeOfferService companyOffers;
    private ModelFileDownloadService modelDownloads;
    private DeviceModelService deviceModels;
    private IVlsAlgorithmOrchestrationService orchestrations;
    private IVlsAlgorithmRepositoryService repositories;
    private IVlsContainerInstanceService containers;
    private IVlsDeviceInfoService legacyDevices;
    private MockMvc mvc;
    private String token;

    @BeforeEach @SuppressWarnings("unchecked") void setup() {
        previousContext = SaManager.getSaTokenContext();
        previousDao = SaManager.getSaTokenDao();
        previousPermissions = SaManager.getStpInterface();
        memory = new SaTokenDaoDefaultImpl();
        SaManager.setSaTokenDao(memory);
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        permissions = new HashSet<>();
        SaManager.setStpInterface(new StpInterface() {
            public List<String> getPermissionList(Object id, String type) { return new ArrayList<>(permissions); }
            public List<String> getRoleList(Object id, String type) { return Collections.emptyList(); }
        });
        context(new MockHttpServletRequest(), new MockHttpServletResponse());
        StpUtil.login("cloud-ai-permission-fixture");
        token = StpUtil.getTokenValue();
        SaTokenContextForThreadLocalStorage.clearBox();
        service = mock(CloudAiService.class);
        sources = mock(CloudAiSources.class);
        events = mock(CloudAiEvents.class);
        models = mock(CloudAiModels.class);
        algorithms = mock(IVlsAlgorithmService.class);
        catalog = mock(AlgorithmCatalogService.class);
        llmManagement = mock(LlmReviewManagementService.class);
        llmTasks = mock(LlmReviewTaskService.class);
        data = mock(DataManagementService.class);
        smart = mock(SmartAnnotationService.class);
        editor = mock(DatasetAnnotationEditor.class);
        training = mock(IVlsAlgorithmTrainingService.class);
        algorithmModels = mock(IVlsAlgorithmModelService.class);
        eventManagement = mock(IVlsEventManagementService.class);
        savedDispatch = mock(SavedModelDispatchService.class);
        dispatchTasks = mock(ModelDispatchTaskService.class);
        queues = mock(ComputeQueueSnapshotService.class);
        annotations = mock(IVlsAlgorithmAnnotationService.class);
        labels = mock(IVlsAnnotationLabelService.class);
        annotationImages = mock(IVlsAnnotationImageService.class);
        annotationInstances = mock(IVlsAnnotationInstanceService.class);
        companyOffers = mock(CompanyComputeOfferService.class);
        modelDownloads = mock(ModelFileDownloadService.class);
        deviceModels = mock(DeviceModelService.class);
        orchestrations = mock(IVlsAlgorithmOrchestrationService.class);
        repositories = mock(IVlsAlgorithmRepositoryService.class);
        containers = mock(IVlsContainerInstanceService.class);
        legacyDevices = mock(IVlsDeviceInfoService.class);
        VlsAlgorithmTrainingController trainingController = new VlsAlgorithmTrainingController();
        ReflectionTestUtils.setField(trainingController, "vlsAlgorithmTrainingService", training);
        ReflectionTestUtils.setField(trainingController, "modelFileDownloadService", modelDownloads);
        VlsModelDispatchController dispatchController = new VlsModelDispatchController();
        ReflectionTestUtils.setField(dispatchController, "savedDispatch", savedDispatch);
        ReflectionTestUtils.setField(dispatchController, "taskService", dispatchTasks);
        SaInterceptor guard = new SaInterceptor();
        mvc = MockMvcBuilders.standaloneSetup(new CloudAiController(service, sources, events, models),
                new VlsAlgorithmController(algorithms), new AlgorithmCatalogController(catalog),
                new VlsLlmReviewController(llmManagement, llmTasks),
                new VlsDataManagementController(data, mock(DataTransferService.class)),
                new VlsSmartAnnotationController(smart, mock(SmartAnnotationWorker.class)),
                new VlsDatasetAnnotationController(editor), trainingController,
                new VlsAlgorithmModelController(algorithmModels, mock(ModelFileDownloadService.class), mock(ImportedAlgorithmModelService.class)),
                new VlsEventManagementController(eventManagement, mock(IVlsDeviceInfoService.class), mock(VlsEventReportApplicationService.class), mock(VlsDeviceEventReportService.class)),
                dispatchController, new VlsComputeQueueController(queues),
                new VlsAlgorithmAnnotationController(annotations, mock(DataTrainingPublisher.class), mock(DatasetGenerationJobs.class)),
                new VlsAnnotationLabelController(labels), new VlsAnnotationImageController(annotationImages),
                new VlsAnnotationInstanceController(annotationInstances), new CompanyComputeOfferController(companyOffers),
                new VlsDeviceModelController(deviceModels), new VlsAlgorithmOrchestrationController(orchestrations),
                new VlsAlgorithmRepositoryController(repositories),
                new VlsContainerInstanceController(containers, mock(GpuTrainingSchedulerService.class)),
                new VlsDeviceInfoController(legacyDevices, mock(IVlsDeviceTagRelationService.class), algorithms, training,
                    algorithmModels, mock(ObjectProvider.class), mock(ObjectProvider.class), mock(ObjectProvider.class),
                    mock(ObjectProvider.class), mock(ObjectProvider.class), mock(ObjectProvider.class), mock(ObjectProvider.class)))
            .setControllerAdvice(new Errors()).addInterceptors(new HandlerInterceptor() {
                public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                    context(request, response);
                    return guard.preHandle(request, response, handler);
                }
                public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception failure) {
                    SaTokenContextForThreadLocalStorage.clearBox();
                }
            }).build();
    }

    private static void context(HttpServletRequest request, HttpServletResponse response) {
        SaTokenContextForThreadLocalStorage.setBox(new SaRequestForServlet(request), new SaResponseForServlet(response), new SaStorageForServlet(request));
    }
    private MockHttpServletRequestBuilder logged(MockHttpServletRequestBuilder request) {
        return request.header(SaManager.getConfig().getTokenName(), token);
    }
    @AfterEach void cleanup() {
        SaTokenContextForThreadLocalStorage.clearBox();
        SaManager.setStpInterface(previousPermissions);
        SaManager.setSaTokenDao(previousDao);
        SaManager.setSaTokenContext(previousContext);
        memory.endRefreshThread();
    }

    @Test void sceneWritesRequireScenePageAndRejectOtherPagePermissions() throws Exception {
        permissions.add("vls:menu:algorithm-task");
        mvc.perform(logged(post("/cloud-ai/scenes").contentType(MediaType.APPLICATION_JSON).content("{}"))).andExpect(status().isForbidden());
        mvc.perform(logged(delete("/cloud-ai/scenes/1"))).andExpect(status().isForbidden());
        verifyNoInteractions(service);
        permissions.clear();
        permissions.add("vls:menu:scene-governance");
        mvc.perform(logged(post("/cloud-ai/scenes").contentType(MediaType.APPLICATION_JSON).content("{}"))).andExpect(status().isOk());
        mvc.perform(logged(delete("/cloud-ai/scenes/1"))).andExpect(status().isOk());
        verify(service).saveScene(isNull(), any());
        verify(service).deleteScene("1");
    }

    @Test void sceneListIsSharedByTaskStatusAndAnalysisWithoutGrantingSceneMutation() throws Exception {
        for (String permission : Arrays.asList("vls:menu:algorithm-task", "vls:menu:task-status", "vls:menu:intelligent-analysis-request")) {
            permissions.clear(); permissions.add(permission);
            mvc.perform(logged(get("/cloud-ai/scenes"))).andExpect(status().isOk());
            mvc.perform(logged(delete("/cloud-ai/scenes/1"))).andExpect(status().isForbidden());
        }
        permissions.clear();
        mvc.perform(logged(get("/cloud-ai/scenes"))).andExpect(status().isForbidden());
        verify(service, times(3)).page(eq("scene"), anyInt(), anyInt(), any(), any(), any());
        verify(service, never()).deleteScene(anyString());
    }

    @Test void taskStatusCanStopRunsButCannotEditTasks() throws Exception {
        permissions.add("vls:menu:task-status");
        mvc.perform(logged(post("/cloud-ai/runs/1/stop"))).andExpect(status().isOk());
        mvc.perform(logged(delete("/cloud-ai/tasks/1"))).andExpect(status().isForbidden());
        verify(service).stop("1");
        verify(service, never()).deleteTask(anyString());
    }

    @Test void eventDetailIsSharedWithActiveSafetyButListIsNot() throws Exception {
        permissions.add("active-safety:events:list");
        mvc.perform(logged(get("/cloud-ai/events/1"))).andExpect(status().isOk());
        mvc.perform(logged(get("/cloud-ai/events"))).andExpect(status().isForbidden());
        verify(events).detail(1L);
        permissions.clear();
        mvc.perform(logged(get("/cloud-ai/events/1"))).andExpect(status().isForbidden());
    }

    @Test void anonymousRequestsRemainDenied() throws Exception {
        mvc.perform(get("/cloud-ai/scenes")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service, sources, events, models);
    }

    @Test void algorithmCatalogueAndBothCurrentAndLegacyDeletesRequireAlgorithmPage() throws Exception {
        permissions.add("vls:menu:data-management");
        mvc.perform(logged(get("/vlsAlgorithmCatalog/categories"))).andExpect(status().isForbidden());
        mvc.perform(logged(delete("/vlsAlgorithm/1"))).andExpect(status().isForbidden());
        mvc.perform(logged(get("/vlsAlgorithm/remove").param("ids", "1"))).andExpect(status().isForbidden());
        verifyNoInteractions(algorithms, catalog);
        permissions.clear(); permissions.add("vls:menu:algorithm-management");
        mvc.perform(logged(get("/vlsAlgorithmCatalog/categories"))).andExpect(status().isOk());
        mvc.perform(logged(delete("/vlsAlgorithm/1"))).andExpect(status().isOk());
        mvc.perform(logged(get("/vlsAlgorithm/remove").param("ids", "1"))).andExpect(status().isOk());
        verify(catalog).categories();
        verify(algorithms).deleteAlgorithm(1L);
        verify(algorithms).deleteLogic(anyList());
    }

    @Test void sharedAlgorithmReadRevocationTakesEffectWithoutGrantingMutation() throws Exception {
        permissions.add("vls:menu:algorithm-training");
        mvc.perform(logged(get("/vlsAlgorithm/1"))).andExpect(status().isOk());
        mvc.perform(logged(delete("/vlsAlgorithm/1"))).andExpect(status().isForbidden());
        permissions.clear();
        mvc.perform(logged(get("/vlsAlgorithm/1"))).andExpect(status().isForbidden());
        verify(algorithms).getById(1L);
        verify(algorithms, never()).deleteAlgorithm(anyLong());
    }

    @Test void providerReadIsSharedWithAlgorithmPageButProviderMutationIsNot() throws Exception {
        permissions.add("vls:menu:algorithm-management");
        mvc.perform(logged(get("/vlsLlmReview/providers"))).andExpect(status().isOk());
        mvc.perform(logged(delete("/vlsLlmReview/providers/1"))).andExpect(status().isForbidden());
        permissions.clear(); permissions.add("vls:menu:llm-provider-management");
        mvc.perform(logged(delete("/vlsLlmReview/providers/1"))).andExpect(status().isOk());
        verify(llmManagement).listProviders();
        verify(llmManagement).deleteProvider(1L);
    }

    @Test void reviewDecisionRequiresReviewPage() throws Exception {
        permissions.add("vls:menu:algorithm-management");
        mvc.perform(logged(post("/vlsLlmReview/tasks/1/manual-decision").contentType(MediaType.APPLICATION_JSON)
            .content("{\"decision\":\"confirmed\"}"))).andExpect(status().isForbidden());
        permissions.clear(); permissions.add("vls:menu:llm-review-tasks");
        mvc.perform(logged(post("/vlsLlmReview/tasks/1/manual-decision").contentType(MediaType.APPLICATION_JSON)
            .content("{\"decision\":\"confirmed\"}"))).andExpect(status().isOk());
        verify(llmTasks).manualDecision(1L, "confirmed");
    }

    @Test void smartAnnotationCanListDatasetsButCannotEditThem() throws Exception {
        permissions.add("vls:menu:smart-annotation");
        mvc.perform(logged(get("/vlsData/projects"))).andExpect(status().isOk());
        mvc.perform(logged(get("/vlsData/smart-annotation/1"))).andExpect(status().isOk());
        mvc.perform(logged(put("/vlsData/projects/1").contentType(MediaType.APPLICATION_JSON).content("{}")))
            .andExpect(status().isForbidden());
        mvc.perform(logged(get("/vlsData/projects/1/samples/2/annotation"))).andExpect(status().isForbidden());
        verify(data).projects(isNull(), eq(1), eq(20));
        verify(smart).detail(1L);
        verify(data, never()).saveProject(any(), any());
        verifyNoInteractions(editor);
        permissions.clear(); permissions.add("vls:menu:data-management");
        mvc.perform(logged(get("/vlsData/projects/1/samples/2/annotation"))).andExpect(status().isOk());
        mvc.perform(logged(get("/vlsData/smart-annotation/1"))).andExpect(status().isForbidden());
        verify(editor).read(1L, 2L);
    }

    @Test void stoppingTrainingRequiresTrainingPageBeforeAnyTrainingLookup() throws Exception {
        permissions.add("vls:menu:algorithm-model");
        mvc.perform(logged(post("/vlsAlgorithmTraining/1/stop"))).andExpect(status().isForbidden());
        verifyNoInteractions(training);
        permissions.clear(); permissions.add("vls:menu:algorithm-training");
        mvc.perform(logged(post("/vlsAlgorithmTraining/1/stop"))).andExpect(status().isOk());
        verify(training).selectAlgorithmTrainingById(1L);
    }

    @Test void modelDeletionIsSharedWithTrainingButImportIsNot() throws Exception {
        permissions.add("vls:menu:algorithm-training");
        mvc.perform(logged(delete("/vlsAlgorithmModel/1"))).andExpect(status().isOk());
        mvc.perform(logged(post("/vlsAlgorithmModel/import").contentType(MediaType.MULTIPART_FORM_DATA)))
            .andExpect(status().isForbidden());
        verify(algorithmModels).deleteModel(1L);
        permissions.clear();
        mvc.perform(logged(delete("/vlsAlgorithmModel/1"))).andExpect(status().isForbidden());
    }

    @Test void modelDetailAndDownloadRespectPageGrants() throws Exception {
        permissions.add("vls:menu:algorithm-training");
        mvc.perform(logged(get("/vlsAlgorithmModel/1"))).andExpect(status().isOk());
        mvc.perform(logged(get("/vlsAlgorithmModel/1/download-file"))).andExpect(status().isForbidden());
        permissions.clear();
        mvc.perform(logged(get("/vlsAlgorithmModel/1"))).andExpect(status().isForbidden());
        verify(algorithmModels).getModelById(1L);
    }

    @Test void eventDetailsAndStatusRequireExistingEventPermission() throws Exception {
        permissions.add("vls:menu:intelligent-analysis-request");
        mvc.perform(logged(get("/vlsEventManagement/1"))).andExpect(status().isForbidden());
        mvc.perform(logged(patch("/vlsEventManagement/1/status").param("status", "completed"))).andExpect(status().isForbidden());
        verifyNoInteractions(eventManagement);
        permissions.clear(); permissions.add("active-safety:events:list");
        mvc.perform(logged(get("/vlsEventManagement/1"))).andExpect(status().isOk());
        mvc.perform(logged(patch("/vlsEventManagement/1/status").param("status", "completed"))).andExpect(status().isOk());
        verify(eventManagement).getEventById(1L);
        verify(eventManagement).updateEvent(any());
    }

    @Test void dispatchRequiresDeviceOrArrangementButSignedHardwarePathRemainsPublic() throws Exception {
        permissions.add("vls:menu:algorithm-model");
        mvc.perform(logged(post("/vlsModelDispatch/models/1/attempt").param("deviceId", "d1").param("operationId", "op1")))
            .andExpect(status().isForbidden());
        verifyNoInteractions(savedDispatch);
        permissions.clear(); permissions.add("vls:menu:algorithm-arrangement");
        mvc.perform(logged(post("/vlsModelDispatch/models/1/attempt").param("deviceId", "d1").param("operationId", "op1")))
            .andExpect(status().isOk());
        verify(savedDispatch).submit(1L, "d1", "op1");
        permissions.clear();
        mvc.perform(get("/vlsModelDispatch/public/missing/download").param("expires", "1").param("signature", "bad"))
            .andExpect(status().isNotFound());
        verify(dispatchTasks).getByRequestId("missing");
    }

    @Test void trainingQueueRejectsRevokedPageBeforeReadingSnapshot() throws Exception {
        permissions.add("vls:menu:algorithm-training");
        mvc.perform(logged(get("/vlsCompute/selection/1/queue"))).andExpect(status().isOk());
        permissions.clear();
        mvc.perform(logged(get("/vlsCompute/selection/1/queue"))).andExpect(status().isForbidden());
        verify(queues).snapshot(1L);
    }

    @Test void trainingCanReadAnnotationButCannotDeleteIt() throws Exception {
        permissions.add("vls:menu:algorithm-training");
        mvc.perform(logged(get("/vlsAlgorithmAnnotation/1"))).andExpect(status().isOk());
        mvc.perform(logged(delete("/vlsAlgorithmAnnotation/1"))).andExpect(status().isForbidden());
        permissions.clear(); permissions.add("vls:menu:algorithm-standard");
        mvc.perform(logged(delete("/vlsAlgorithmAnnotation/1"))).andExpect(status().isOk());
        verify(annotations).getById(1L);
        verify(annotations).deleteAnnotation(1L);
    }

    @Test void dataManagementSharesLabelCreationButNotLegacyAnnotationCrud() throws Exception {
        permissions.add("vls:menu:data-management");
        mvc.perform(logged(get("/vlsAnnotationLabel/1/labels"))).andExpect(status().isOk());
        mvc.perform(logged(post("/vlsAnnotationLabel/1/labels").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"fixture\",\"color\":\"blue\"}"))).andExpect(status().isOk());
        mvc.perform(logged(get("/vlsAnnotationLabel/remove").param("ids", "1"))).andExpect(status().isForbidden());
        mvc.perform(logged(get("/vlsAnnotationImage/remove").param("ids", "1"))).andExpect(status().isForbidden());
        mvc.perform(logged(get("/vlsAnnotationInstance/remove").param("ids", "1"))).andExpect(status().isForbidden());
        verify(labels).getByAnnotationIdWithUsageCount(1L);
        verify(labels).createLabel(1L, "fixture", "blue", null);
        verifyNoInteractions(annotationImages, annotationInstances);
        permissions.clear();
        mvc.perform(logged(get("/vlsAnnotationLabel/1/labels"))).andExpect(status().isForbidden());
    }

    @Test void legacyAnnotationRemovalNeedsStandardPage() throws Exception {
        permissions.add("vls:menu:algorithm-standard");
        mvc.perform(logged(delete("/vlsAnnotationInstance/instances/1"))).andExpect(status().isOk());
        verify(annotationInstances).deleteAnnotation(1L);
        permissions.clear();
        mvc.perform(logged(delete("/vlsAnnotationInstance/instances/1"))).andExpect(status().isForbidden());
        verifyNoMoreInteractions(annotationInstances);
    }

    @Test void companyOfferRequiresManagementPageAndAllExistingSensitivePermissions() throws Exception {
        permissions.addAll(Arrays.asList("vls:compute:company-publish", "vls:compute:publish", "vls:compute:configure"));
        mvc.perform(logged(get("/vlsCompute/company-offers"))).andExpect(status().isForbidden());
        permissions.add("vls:menu:compute-management");
        mvc.perform(logged(get("/vlsCompute/company-offers"))).andExpect(status().isOk());
        permissions.remove("vls:compute:publish");
        mvc.perform(logged(put("/vlsCompute/company-offers/fixture/publication").param("published", "true"))).andExpect(status().isForbidden());
        verify(companyOffers).managementList();
        verify(companyOffers, never()).publish(anyString(), anyBoolean());
    }

    @Test void legacyModelDownloadIsSharedOnlyByModelTrainingAndVlstreamDevicePages() throws Exception {
        for (String page : Arrays.asList("vls:menu:algorithm-model", "vls:menu:algorithm-training", "vls:menu:vlstream:device", "vls:menu:legacy-device-management")) {
            permissions.clear(); permissions.add(page);
            mvc.perform(logged(get("/vlsAlgorithmTraining/download-model").param("id", "1").param("type", "pt")))
                .andExpect(status().isOk());
        }
        permissions.clear(); permissions.add("vls:menu:data-management");
        mvc.perform(logged(get("/vlsAlgorithmTraining/download-model").param("id", "1"))).andExpect(status().isForbidden());
        permissions.clear();
        mvc.perform(logged(get("/vlsAlgorithmTraining/download-model").param("id", "1"))).andExpect(status().isForbidden());
        verify(modelDownloads, times(4)).downloadModel(eq(1L), eq("pt"), any());
    }

    @Test void deviceModelQueryAndDeleteRequireTheVlstreamDevicePage() throws Exception {
        permissions.add("vls:menu:algorithm-model");
        mvc.perform(logged(get("/vlsDeviceModels").param("deviceId", "CAM-1"))).andExpect(status().isForbidden());
        mvc.perform(logged(delete("/vlsDeviceModels").param("deviceId", "CAM-1").param("modelId", "m1"))).andExpect(status().isForbidden());
        verifyNoInteractions(deviceModels);
        permissions.clear(); permissions.add("vls:menu:vlstream:device");
        mvc.perform(logged(delete("/vlsDeviceModels").param("deviceId", "CAM-1").param("modelId", "m1"))).andExpect(status().isOk());
        verify(deviceModels).delete("CAM-1", "m1");
        permissions.clear();
        mvc.perform(logged(delete("/vlsDeviceModels").param("deviceId", "CAM-1").param("modelId", "m1"))).andExpect(status().isForbidden());
        verifyNoMoreInteractions(deviceModels);
    }

    @Test void legacyOrchestrationRepositoryAndContainerEndpointsRequireTheirOwnPages() throws Exception {
        permissions.add("vls:menu:algorithm-management");
        mvc.perform(logged(get("/vlsAlgorithmRepository/enabled"))).andExpect(status().isOk());
        mvc.perform(logged(get("/vlsAlgorithmOrchestration/remove").param("ids", "1"))).andExpect(status().isForbidden());
        mvc.perform(logged(get("/vlsContainerInstance/1"))).andExpect(status().isForbidden());
        verify(repositories).getEnabledRepositories();
        verifyNoInteractions(orchestrations, containers);
        permissions.clear(); permissions.add("vls:menu:algorithm-arrangement");
        mvc.perform(logged(get("/vlsAlgorithmOrchestration/remove").param("ids", "1"))).andExpect(status().isOk());
        mvc.perform(logged(get("/vlsAlgorithmRepository/enabled"))).andExpect(status().isForbidden());
        verify(orchestrations).deleteLogic(anyList());
        permissions.clear(); permissions.add("vls:menu:container-instances");
        mvc.perform(logged(get("/vlsContainerInstance/1"))).andExpect(status().isOk());
        verify(containers).getById(1L);
        permissions.clear();
        mvc.perform(logged(get("/vlsContainerInstance/1"))).andExpect(status().isForbidden());
    }

    @Test void legacyAndCurrentDeviceDispatchShareOnlyTheirAuthorizedPages() throws Exception {
        for (String page : Arrays.asList("vls:menu:algorithm-management", "vls:menu:algorithm-arrangement", "vls:menu:vlstream:device", "vls:menu:legacy-device-management")) {
            permissions.clear(); permissions.add(page);
            mvc.perform(logged(post("/vlsDeviceInfo/1/algorithms").param("deviceIds", "CAM-1")))
                .andExpect(status().isOk());
        }
        permissions.clear(); permissions.add("vls:menu:data-management");
        mvc.perform(logged(get("/vlsDeviceInfo/dispatchAlgorithms").param("algorithmId", "1").param("deviceIds", "CAM-1")))
            .andExpect(status().isForbidden());
        permissions.clear();
        mvc.perform(logged(post("/vlsDeviceInfo/1/algorithms").param("deviceIds", "CAM-1")))
            .andExpect(status().isForbidden());
        verify(legacyDevices, times(4)).dispatchAlgorithms(1L, "CAM-1", "om");
    }

    @Test void legacyDevicesRequireTheirOwnPageRatherThanAnyIndividualProtocolPage() throws Exception {
        for (String page : Arrays.asList("vls:menu:vlstream:device", "vls:menu:isup:isupDevice",
            "vls:menu:ehome:device", "vls:menu:rtsp:rtspDevice", "vls:menu:onvif:cameraManage",
            "vls:menu:gbmanger:device", "vls:menu:custom:device")) {
            permissions.clear(); permissions.add(page);
            mvc.perform(logged(get("/vlsDeviceInfo/1"))).andExpect(status().isForbidden());
            mvc.perform(logged(delete("/vlsDeviceInfo/1"))).andExpect(status().isForbidden());
        }
        verifyNoInteractions(legacyDevices);
        permissions.clear(); permissions.add("vls:menu:legacy-device-management");
        mvc.perform(logged(get("/vlsDeviceInfo/1"))).andExpect(status().isOk());
        mvc.perform(logged(delete("/vlsDeviceInfo/1"))).andExpect(status().isOk());
        verify(legacyDevices).getById(1L); verify(legacyDevices).deleteDevice(1L);
        permissions.clear();
        mvc.perform(logged(delete("/vlsDeviceInfo/1"))).andExpect(status().isForbidden());
        verifyNoMoreInteractions(legacyDevices);
    }

    @Test void legacyDeviceModelPickerIsSharedOnlyWithItsOwnPage() throws Exception {
        permissions.add("vls:menu:onvif:cameraManage");
        mvc.perform(logged(get("/vlsAlgorithmModel/page"))).andExpect(status().isForbidden());
        permissions.clear(); permissions.add("vls:menu:legacy-device-management");
        mvc.perform(logged(get("/vlsAlgorithmModel/page"))).andExpect(status().isOk());
        verify(algorithmModels).selectVlsAlgorithmModelPage(any(), any());
        permissions.clear();
        mvc.perform(logged(get("/vlsAlgorithmModel/page"))).andExpect(status().isForbidden());
    }

    @RestControllerAdvice static class Errors {
        @ExceptionHandler(NotPermissionException.class) ResponseEntity<String> permission() { return ResponseEntity.status(403).body("denied"); }
        @ExceptionHandler(NotLoginException.class) ResponseEntity<String> login() { return ResponseEntity.status(401).body("login required"); }
    }
}
