package com.ruoyi.vlstream.test.vlstream.data;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage;
import cn.dev33.satoken.servlet.model.SaRequestForServlet;
import cn.dev33.satoken.servlet.model.SaResponseForServlet;
import cn.dev33.satoken.servlet.model.SaStorageForServlet;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.controller.VlsDataManagementController;
import com.ruoyi.vlstream.test.vlstream.controller.VlsSmartAnnotationController;
import com.ruoyi.vlstream.test.vlstream.mapper.*;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import com.ruoyi.vlstream.test.vlstream.service.*;
import com.ruoyi.vlstream.test.vlstream.service.impl.VlsAnnotationInstanceServiceImpl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("dev")
@EnabledIfEnvironmentVariable(named = "VLS_DATA_TEST_JDBC", matches = "jdbc:mysql://127\\.0\\.0\\.1:33316/vls_data_test_smart\\?.*")
class SmartAnnotationIntegrationTest {
    static AnnotationConfigApplicationContext context;
    SmartAnnotationService smart;
    DataManagementService data;
    SqlSessionTemplate sql;
    JdbcTemplate jdbc;
    Long dataset;
    Long label;

    @BeforeAll static void open() throws Exception {
        ((ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)).setLevel(ch.qos.logback.classic.Level.WARN);
        context = new AnnotationConfigApplicationContext(Config.class);
        Path artifacts = DataTestConfiguration.root().resolve("codex/smart-annotation"); Files.createDirectories(artifacts);
        String classpath = System.getProperty("java.class.path");
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader instanceof java.net.URLClassLoader) classpath = Arrays.stream(((java.net.URLClassLoader) loader).getURLs()).map(url -> {
            try { return Paths.get(url.toURI()).toString(); } catch (Exception ex) { throw new IllegalStateException(ex); }
        }).collect(java.util.stream.Collectors.joining(java.io.File.pathSeparator));
        Files.write(artifacts.resolve("runtime-classpath.txt"), classpath.getBytes(StandardCharsets.UTF_8));
    }
    @AfterAll static void close() { if (context != null) context.close(); }
    @BeforeEach void setup() throws Exception {
        DataTestConfiguration.initialize(context.getBean(DataSource.class));
        jdbc = new JdbcTemplate(context.getBean(DataSource.class));
        for (String table : Arrays.asList("vls_smart_annotation_candidate", "vls_smart_annotation_round", "vls_smart_annotation_task")) jdbc.execute("DROP TABLE IF EXISTS " + table);
        String migration = new String(Files.readAllBytes(DataTestConfiguration.root().resolve("VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/db/migration/mainline/V1_2_0_021__smart_annotation.sql")), StandardCharsets.UTF_8);
        for (String statement : migration.split(";")) if (!statement.trim().isEmpty()) jdbc.execute(statement);
        applyDatasetMigrations(jdbc);
        TenantContextHolder.setTenantId("tenant-a");
        smart = context.getBean(SmartAnnotationService.class); data = context.getBean(DataManagementService.class); sql = context.getBean(SqlSessionTemplate.class);
        DataRequests.Project request = new DataRequests.Project(); request.setAnnotationName("Smart annotation test"); request.setProjectCode("SMART");
        dataset = data.saveProject(null, request).getId();
        AnnotationLabel category = new AnnotationLabel(); category.setTenantId("tenant-a"); category.setAnnotationId(dataset); category.setName("car"); category.setColor("#3377ff"); category.setIsDeleted(0); category.setStatus(1);
        sql.getMapper(VlsAnnotationLabelMapper.class).insert(category); label = category.getId();
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    static void applyDatasetMigrations(JdbcTemplate jdbc) throws Exception {
        jdbc.execute("DROP TABLE IF EXISTS vls_training_dataset_artifact");
        Path migrations = DataTestConfiguration.root().resolve("VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/db/migration");
        for (String file : Arrays.asList("V1_2_0_022__four_type_annotation_payloads.sql", "V1_2_0_026__training_dataset_object_storage.sql")) {
            String migration = new String(Files.readAllBytes(migrations.resolve(file)), StandardCharsets.UTF_8).replaceAll("(?m)^--.*$", "");
            for (String statement : migration.split(";")) if (!statement.trim().isEmpty()) jdbc.execute(statement);
        }
        jdbc.execute("ALTER TABLE vls_smart_annotation_task ADD COLUMN compute_node_id BIGINT NULL");
    }

    Long image(int number) {
        AnnotationImage image = new AnnotationImage(); image.setTenantId("tenant-a"); image.setAnnotationId(dataset); image.setImageName("image-" + number + ".png"); image.setOriginalName(image.getImageName());
        image.setLocalPath(image.getImageName()); image.setMediaType("image"); image.setMediaWidth(100); image.setMediaHeight(80); image.setContentSha256("hash-" + number);
        image.setDatasetSplit("unassigned"); image.setQualityStatus("accepted"); image.setIsDeleted(0); image.setStatus(1);
        sql.getMapper(DataSampleMapper.class).insert(image); return image.getId();
    }

    SmartAnnotationTask create(String mode) {
        SmartAnnotationRequests.Create request = new SmartAnnotationRequests.Create(); request.setDatasetId(dataset); request.setComputeNodeId(10L); request.setTaskName("Integration round"); request.setMode(mode); request.setSourceType("model"); request.setSourceId(100L);
        return smart.create(request);
    }

    SmartAnnotationCandidate prediction(SmartAnnotationTask task, Long image, Long targetLabel) {
        SmartAnnotationRound round = smart.current(task);
        jdbc.update("UPDATE vls_smart_annotation_task SET task_state='REVIEW' WHERE id=?", task.getId());
        jdbc.update("UPDATE vls_smart_annotation_round SET round_state='REVIEW', model_path='/data/work/best.pt' WHERE id=?", round.getId());
        SmartAnnotationRequests.Box box = SmartAnnotationServiceTest.box(); box.setLabelId(targetLabel); box.setClassName("car");
        SmartAnnotationCandidate result = new SmartAnnotationCandidate(); result.setTenantId("tenant-a"); result.setTaskId(task.getId()); result.setRoundId(round.getId()); result.setImageId(image); result.setImageWidth(100); result.setImageHeight(80);
        result.setBoxesJson(data.writeJson(Collections.singletonList(box))); result.setHardExample(1); result.setUncertainty(.2); result.setReviewState("PENDING"); result.setIsDeleted(0); result.setStatus(1);
        result.setSummaryJson(data.writeJson(AnnotationPayloads.summary(Collections.singletonList(box))));
        sql.getMapper(SmartAnnotationCandidateMapper.class).insert(result); return result;
    }

    @Test void migrationAndReviewPersistCorrectProgressAndInvalidatePublishedDataset() {
        Long image = image(1); SmartAnnotationTask task = create("model"); SmartAnnotationCandidate result = prediction(task, image, label);
        assertEquals(0, data.snapshot(dataset).getInstances().size());
        jdbc.update("UPDATE vls_algorithm_annotation SET dataset_path='/old/dataset.yaml' WHERE id=?", dataset);
        SmartAnnotationRequests.Review request = new SmartAnnotationRequests.Review(); request.setBoxes(smart.boxes(result.getBoxesJson()));
        smart.review(task.getId(), result.getId(), request);
        assertEquals(1, data.snapshot(dataset).getInstances().size());
        assertNull(data.project(dataset).getDatasetPath()); assertEquals(1, data.project(dataset).getAnnotatedCount());
        smart.review(task.getId(), result.getId(), request); assertEquals(1, data.snapshot(dataset).getInstances().size());
        smart.finish(task.getId()); assertEquals("COMPLETED", smart.task(task.getId()).getTaskState());
        assertEquals(2, data.versions(dataset).size());
    }

    @Test void optionsImportsAndRoundSnapshotsWorkBeyondTenThousandSamples() throws Exception {
        int sampleCount = 10_001;
        long firstId = 1_000L;
        long lastId = firstId + sampleCount - 1;
        List<Object[]> members = new ArrayList<>(sampleCount);
        for (int i = 0; i < sampleCount; i++) {
            String name = "bulk-" + i + ".png";
            members.add(new Object[]{firstId + i, dataset, name, i == 501 ? "__MACOSX/" + name : name, name,
                i == 500 ? "video" : "image", i < 500 ? "low" : i == 502 ? "excluded" : "accepted"});
        }
        jdbc.batchUpdate("INSERT INTO vls_annotation_image (id, tenant_id, annotation_id, image_name, original_name, local_path, "
            + "media_type, quality_status) VALUES (?, 'tenant-a', ?, ?, ?, ?, ?, ?)", members);
        jdbc.batchUpdate("INSERT INTO vls_annotation_instance (tenant_id, annotation_id, image_id, label_id, annotation_type, annotation_data, is_deleted) "
            + "VALUES (?, ?, ?, ?, 'rect', '{}', ?)", Arrays.asList(
                new Object[]{"tenant-a", dataset, lastId, label, 0},
                new Object[]{"tenant-a", dataset, lastId, label, 0},
                new Object[]{"tenant-a", dataset, firstId, label, 0},
                new Object[]{"tenant-a", dataset, lastId - 1, label, 1},
                new Object[]{"tenant-b", dataset, lastId - 2, label, 0}));

        MockMvcBuilders.standaloneSetup(new VlsSmartAnnotationController(smart, mock(SmartAnnotationWorker.class)))
            .setMessageConverters(new MappingJackson2HttpMessageConverter(context.getBean(ObjectMapper.class))).build()
            .perform(get("/vlsData/smart-annotation/options/{datasetId}", dataset))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.unlabeled").value(sampleCount - 504))
            .andExpect(jsonPath("$.data.labelCounts[0].name").value("car"))
            .andExpect(jsonPath("$.data.labelCounts[0].count").value("2"));

        SampleMediaInspector.Inspection single = new SampleMediaInspector.Inspection();
        single.setMediaType("image");
        single.setSha256("single-import");
        Map<String, Object> registered = data.register(dataset, "new.png", "new.png", 1, "test", single);
        assertEquals(false, registered.get("duplicate"));
        SampleMediaInspector.Inspection archived = new SampleMediaInspector.Inspection();
        archived.setMediaType("image");
        archived.setSha256("archive-import");
        Map<String, Object> result = data.registerArchiveFromSource(dataset, Arrays.asList(
            new PendingSampleImport("a.png", "a.png", "a.png", 1, archived),
            new PendingSampleImport("duplicate.png", "duplicate.png", "duplicate.png", 1, single)), null, "test");
        Map<?, ?> duplicate = (Map<?, ?>) ((List<?>) result.get("results")).get(1);
        assertEquals(true, duplicate.get("duplicate"));
        assertEquals(registered.get("id"), duplicate.get("id"));

        SmartAnnotationTask task = create("model");
        SmartAnnotationRound round = smart.current(task);
        assertEquals(sampleCount + 2, data.version(dataset, round.getVersionId()).getSampleCount());
        DatasetSnapshot frozen = smart.snapshot(task, round);
        assertEquals(sampleCount + 2, frozen.getSamples().size());
        assertEquals(sampleCount - 502, SmartAnnotationService.unannotated(frozen).size());
        assertEquals(2L, frozen.getInstances().stream().filter(instance -> instance.getImageId().equals(lastId)).count());
    }

    @Test void batchConflictRollsBackEarlierImagesAndReviewState() {
        Long first = image(1), second = image(2); SmartAnnotationTask task = create("model");
        SmartAnnotationCandidate a = prediction(task, first, label), b = prediction(task, second, 99999L);
        assertThrows(ServiceException.class, () -> smart.confirmBatch(task.getId(), Arrays.asList(a.getId(), b.getId())));
        assertTrue(data.snapshot(dataset).getInstances().isEmpty());
        assertEquals("PENDING", sql.getMapper(SmartAnnotationCandidateMapper.class).selectById(a.getId()).getReviewState());
    }

    @Test void automaticConfirmationPersistsEligibleImagesAndPreservesPendingAndExistingLabels() {
        Long eligibleImage = image(1), pendingImage = image(2), editedImage = image(3);
        SmartAnnotationTask task = create("model");
        SmartAnnotationCandidate eligible = prediction(task, eligibleImage, label);
        SmartAnnotationCandidate pending = prediction(task, pendingImage, label);
        SmartAnnotationCandidate edited = prediction(task, editedImage, label);
        jdbc.update("UPDATE vls_smart_annotation_candidate SET review_error='low confidence' WHERE id=?", pending.getId());
        jdbc.update("INSERT INTO vls_annotation_instance (tenant_id,annotation_id,image_id,label_id,annotation_type,annotation_data,is_deleted) "
            + "VALUES ('tenant-a',?,?,?,'rect','{\"x\":4,\"y\":4,\"width\":10,\"height\":10}',0)", dataset, editedImage, label);
        jdbc.update("UPDATE vls_smart_annotation_task SET task_state='AUTO_CONFIRMING',bulk_total=2 WHERE id=?", task.getId());
        SmartAnnotationCandidateMapper candidates = sql.getMapper(SmartAnnotationCandidateMapper.class);
        context.getBean(SmartAnnotationConfirmationWorker.class).tick();

        assertEquals("REVIEW", smart.task(task.getId()).getTaskState());
        assertEquals(1, smart.task(task.getId()).getBulkAccepted());
        assertEquals(1, smart.task(task.getId()).getBulkConflicts());
        assertEquals("ACCEPTED", candidates.selectById(eligible.getId()).getReviewState());
        assertEquals("PENDING", candidates.selectById(pending.getId()).getReviewState());
        assertEquals("PENDING", candidates.selectById(edited.getId()).getReviewState());
        List<AnnotationInstance> annotations = data.snapshot(dataset).getInstances();
        assertEquals(2, annotations.size());
        assertTrue(annotations.stream().anyMatch(instance -> editedImage.equals(instance.getImageId())
            && instance.getAnnotationData().contains("\"x\":4")));
    }

    @Test void tenantIsolationAndDuplicateDatasetTaskAreEnforced() {
        image(1); SmartAnnotationTask task = create("model");
        assertThrows(ServiceException.class, () -> create("model"));
        TenantContextHolder.setTenantId("tenant-b");
        assertThrows(ServiceException.class, () -> smart.task(task.getId()));
        assertEquals(0, smart.list(null, 1).getTotal());
    }

    @Test void activeTaskPreventsDestructiveDatasetCleanup() {
        image(1); create("model");
        DatasetStorageProvider storage = mock(DatasetStorageProvider.class); DatasetRemoteCleanup remote = mock(DatasetRemoteCleanup.class);
        ModelClassFileService classes = mock(ModelClassFileService.class); ModelClassSnapshotStore snapshots = mock(ModelClassSnapshotStore.class);
        DatasetCleanupService cleanup = new DatasetCleanupService(jdbc, context.getBean(org.springframework.transaction.PlatformTransactionManager.class),
            storage, remote, mock(TrainingDatasetArtifactService.class), classes, snapshots, context.getBean(ObjectMapper.class));
        ServiceException rejected = assertThrows(ServiceException.class, () -> cleanup.delete(dataset));
        assertTrue(rejected.getMessage().contains("智能标注")); verifyNoInteractions(storage, remote, classes, snapshots);
        assertNotNull(data.project(dataset));
    }

    @Test void queuedCancellationIsDurableAndReviewCancellationKeepsConfirmedData() {
        Long image = image(1); SmartAnnotationTask task = create("model");
        cancelAsCreator(task); assertEquals("CANCEL_REQUESTED", smart.task(task.getId()).getTaskState());
        jdbc.update("UPDATE vls_smart_annotation_task SET task_state='REVIEW' WHERE id=?", task.getId());
        SmartAnnotationCandidate result = prediction(task, image, label);
        SmartAnnotationRequests.Review request = new SmartAnnotationRequests.Review(); request.setBoxes(smart.boxes(result.getBoxesJson())); smart.review(task.getId(), result.getId(), request);
        cancelAsCreator(task); assertEquals("CANCELLED", smart.task(task.getId()).getTaskState()); assertEquals(1, data.snapshot(dataset).getInstances().size());
    }

    @Test void activeLearningRequiresGeneratedInputOnlyForTheFirstRound() {
        for (int n = 0; n < 103; n++) image(n);
        List<AnnotationImage> images = data.snapshot(dataset).getSamples();
        for (int n = 0; n < 10; n++) {
            AnnotationInstance instance = new AnnotationInstance(); instance.setTenantId("tenant-a"); instance.setAnnotationId(dataset); instance.setImageId(images.get(n % 2).getId()); instance.setLabelId(label);
            instance.setAnnotationType(com.ruoyi.vlstream.test.vlstream.enums.AlgorithmAnnotationTypeEnum.rect); instance.setAnnotationData("{\"x\":10,\"y\":10,\"width\":20,\"height\":20}"); instance.setIsDeleted(0); instance.setStatus(1);
            sql.getMapper(VlsAnnotationInstanceMapper.class).insert(instance);
        }
        assertEquals(false, smart.options(dataset, true).get("datasetGenerated"));
        assertEquals(0, data.projects("SMART", 1, 20, true, false).getTotal());
        assertEquals(1, data.projects("SMART", 1, 20, false, false).getTotal());
        assertTrue(assertThrows(ServiceException.class, () -> create("active")).getMessage().contains("请先到算法标注页面生成数据集"));

        DataRequests.Version generatedRequest = new DataRequests.Version();
        generatedRequest.setName("训练数据快照");
        DatasetVersion generated = data.saveVersion(dataset, generatedRequest);
        String artifactId = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO vls_training_dataset_artifact (id,tenant_id,dataset_id,version_id,annotation_type,snapshot_sha256,dataset_yaml,storage_state) "
            + "VALUES (?, 'tenant-b', ?, ?, 'object_detection', ?, '', 'READY')", artifactId, dataset, generated.getId(), String.join("", Collections.nCopies(64, "a")));
        jdbc.update("UPDATE vls_algorithm_annotation SET dataset_path=? WHERE id=?", TrainingDatasetArtifactService.reference(artifactId), dataset);
        assertFalse(data.isDatasetGenerated(dataset));
        jdbc.update("UPDATE vls_training_dataset_artifact SET tenant_id='tenant-a', storage_state='BUILDING' WHERE id=?", artifactId);
        assertFalse(data.isDatasetGenerated(dataset));
        jdbc.update("UPDATE vls_training_dataset_artifact SET storage_state='READY' WHERE id=?", artifactId);
        assertEquals(true, smart.options(dataset, true).get("datasetGenerated"));
        assertEquals(dataset, data.projects("SMART", 1, 20, true, false).getRecords().get(0).getId());

        SmartAnnotationTask task = create("active"); SmartAnnotationCandidate confirmed = prediction(task, images.get(2).getId(), label);
        SmartAnnotationCandidate untouched = prediction(task, images.get(3).getId(), label);
        untouched.setHardExample(0); sql.getMapper(SmartAnnotationCandidateMapper.class).updateById(untouched);
        assertThrows(ServiceException.class, () -> smart.next(task.getId()));
        SmartAnnotationRequests.Review review = new SmartAnnotationRequests.Review(); review.setBoxes(smart.boxes(confirmed.getBoxesJson())); smart.review(task.getId(), confirmed.getId(), review);
        assertEquals(0, data.projects("SMART", 1, 20, true, false).getTotal());
        assertEquals(false, data.projects("SMART", 1, 20, false, true).getRecords().get(0).getDatasetGenerated());
        smart.next(task.getId()); SmartAnnotationTask next = smart.task(task.getId());
        assertEquals(2, next.getRoundNumber()); assertEquals("QUEUED", next.getTaskState());
        DatasetSnapshot snapshot = smart.snapshot(next, smart.current(next));
        assertEquals(11, snapshot.getInstances().size()); assertEquals(100, SmartAnnotationService.unannotated(snapshot).size());
        assertEquals(3, data.versions(dataset).size());
    }

    @Test void datasetPickerReturnsGeneratedAndUngeneratedRowsWithTheirStatus() throws Exception {
        jdbc.update("UPDATE vls_algorithm_annotation SET dataset_path=? WHERE id=?", "/data/generated/dataset.yaml", dataset);
        DataRequests.Project ungenerated = new DataRequests.Project();
        ungenerated.setProjectCode("SMART-UNG");
        ungenerated.setAnnotationName("Ungenerated");
        Long ungeneratedId = data.saveProject(null, ungenerated).getId();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new VlsDataManagementController(data, mock(DataTransferService.class)),
            new VlsSmartAnnotationController(smart, mock(SmartAnnotationWorker.class))).build();
        mvc.perform(get("/vlsData/projects").param("keyword", "SMART").param("includeGeneratedStatus", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.records.length()").value(2))
            .andExpect(jsonPath("$.data.records[0].datasetGenerated").value(false))
            .andExpect(jsonPath("$.data.records[1].datasetGenerated").value(true));
        mvc.perform(get("/vlsData/projects").param("keyword", "SMART"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.records[0].datasetGenerated").doesNotExist());
        mvc.perform(get("/vlsData/smart-annotation/options/{datasetId}", ungeneratedId).param("forActiveLearning", "true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.datasetGenerated").value(false))
            .andExpect(jsonPath("$.data.datasetName").value("Ungenerated"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"image_classification", "object_detection", "instance_segmentation", "semantic_segmentation"})
    void allTypesRoundTripThroughManualEditorSmartConfirmationVersionAndTrainingFiles(String type) {
        jdbc.update("UPDATE vls_algorithm_annotation SET annotation_type=? WHERE id=?", type, dataset);
        Long first = image(1), second = image(2);
        DatasetAnnotationEditor editor = context.getBean(DatasetAnnotationEditor.class);
        SmartAnnotationRequests.Box region = SmartAnnotationServiceTest.box(); region.setLabelId(label);
        if ("image_classification".equals(type)) { region = new SmartAnnotationRequests.Box(); region.setLabelId(label); }
        if (type.endsWith("segmentation")) {
            region.setX(0d); region.setY(0d); region.setWidth(100d); region.setHeight(80d);
            BitSet pixels = new BitSet(8000); pixels.set(0, 8000);
            if ("instance_segmentation".equals(type)) pixels.clear(3500, 3600);
            region.setMaskData(AnnotationMask.encode(pixels, 100, 80));
        }
        DatasetAnnotationEditor.Edit edit = new DatasetAnnotationEditor.Edit(); edit.setRevision(String.valueOf(editor.read(dataset, first).get("revision"))); edit.setBoxes(Collections.singletonList(region)); editor.save(dataset, first, edit);
        assertThrows(ServiceException.class, () -> editor.save(dataset, first, edit));
        Map<String, Object> options = smart.options(dataset, false);
        assertEquals(1, options.get("unlabeled"));
        assertEquals(Collections.singletonList(DataManagementService.map("name", "car", "count", 1L)), options.get("labelCounts"));
        SmartAnnotationRequests.Create request = new SmartAnnotationRequests.Create(); request.setDatasetId(dataset); request.setComputeNodeId(10L); request.setTaskName(type); request.setMode("model"); request.setSourceType("system"); request.setSourceId(0L);
        SmartAnnotationTask task = smart.create(request);
        SmartAnnotationCandidate candidate = prediction(task, second, label); region.setConfidence(.9);
        candidate.setBoxesJson(data.writeJson(Collections.singletonList(region))); candidate.setSummaryJson(data.writeJson(AnnotationPayloads.summary(Collections.singletonList(region)))); sql.getMapper(SmartAnnotationCandidateMapper.class).updateById(candidate);
        smart.confirmAll(task.getId()); context.getBean(SmartAnnotationConfirmationWorker.class).tick();
        assertEquals("REVIEW", smart.task(task.getId()).getTaskState()); assertEquals(1, smart.task(task.getId()).getBulkAccepted());
        assertEquals(2, data.snapshot(dataset).getInstances().size());
        smart.finish(task.getId());
        DatasetSnapshot frozen = data.readSnapshot(data.version(dataset, data.versions(dataset).get(0).getId()).getSnapshotJson());
        assertEquals(2, frozen.getInstances().size());
        List<AnnotationInstance> own = frozen.getInstances().stream().filter(instance -> first.equals(instance.getImageId())).collect(java.util.stream.Collectors.toList());
        TrainingDatasetLayout layout = new TrainingDatasetLayout(type, frozen.getLabels());
        assertFalse(layout.annotations(first, "train", own, 100, 80).isEmpty());
        assertEquals(type, smart.task(task.getId()).getAnnotationType());
    }

    @Test void bulkConfirmationKeepsInvalidResultsPendingAndCanResume() {
        Long a = image(1), b = image(2); SmartAnnotationTask task = create("model");
        SmartAnnotationCandidate first = prediction(task, a, label), invalid = prediction(task, b, 999L);
        smart.confirmAll(task.getId()); SmartAnnotationConfirmationWorker worker = context.getBean(SmartAnnotationConfirmationWorker.class);
        worker.advance(task.getId()); worker.tick();
        assertEquals("ACCEPTED", sql.getMapper(SmartAnnotationCandidateMapper.class).selectById(first.getId()).getReviewState());
        assertEquals("PENDING", sql.getMapper(SmartAnnotationCandidateMapper.class).selectById(invalid.getId()).getReviewState());
        assertEquals(1, smart.task(task.getId()).getBulkConflicts());
        assertEquals("REVIEW", smart.task(task.getId()).getTaskState());
        smart.confirmAll(task.getId()); cancelAsCreator(task); worker.tick();
        assertEquals("CANCELLED", smart.task(task.getId()).getTaskState()); assertEquals(1, data.snapshot(dataset).getInstances().size());
    }

    private void cancelAsCreator(SmartAnnotationTask task) {
        long creator = 101L;
        jdbc.update("UPDATE vls_smart_annotation_task SET create_user=? WHERE id=?", creator, task.getId());
        SaTokenContext previous = SaManager.getSaTokenContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        try {
            SaTokenContextForThreadLocalStorage.setBox(new SaRequestForServlet(request),
                new SaResponseForServlet(new MockHttpServletResponse()), new SaStorageForServlet(request));
            SaHolder.getStorage().set(LoginHelper.USER_KEY, Long.toString(creator));
            smart.cancel(task.getId());
        } finally {
            SaTokenContextForThreadLocalStorage.clearBox();
            SaManager.setSaTokenContext(previous);
        }
    }

    @org.springframework.boot.test.context.TestConfiguration
    public static class Config extends DataTestConfiguration {
        @Bean DatasetAnnotationEditor editor(DataManagementService data, DataMediaStorage storage, IVlsAnnotationInstanceService annotations, ObjectMapper json, SampleMediaInspector inspector) {
            return new DatasetAnnotationEditor(data, storage, annotations, json, inspector);
        }
        @Bean SmartAnnotationConfirmationWorker confirmation(SqlSessionTemplate sql, SmartAnnotationService service, DataManagementService data, org.springframework.transaction.PlatformTransactionManager tx) {
            return new SmartAnnotationConfirmationWorker(sql.getMapper(SmartAnnotationTaskMapper.class), sql.getMapper(SmartAnnotationCandidateMapper.class), service, data, tx);
        }
        @Bean IVlsAnnotationLabelService annotationLabelService() { return mock(IVlsAnnotationLabelService.class); }
        @Bean IVlsAnnotationImageService annotationImageService() { return mock(IVlsAnnotationImageService.class); }
        @Bean VlsAlgorithmAnnotationMapper annotationMapper(SqlSessionTemplate sql) { return sql.getMapper(VlsAlgorithmAnnotationMapper.class); }
        @Bean VlsAnnotationInstanceMapper instanceMapper(SqlSessionTemplate sql) { return sql.getMapper(VlsAnnotationInstanceMapper.class); }
        @Bean VlsAnnotationImageMapper imageMapper(SqlSessionTemplate sql) { return sql.getMapper(VlsAnnotationImageMapper.class); }
        @Override @Bean SqlSessionFactory sqlSessionFactory(DataSource source) throws Exception {
            SqlSessionFactory factory = super.sqlSessionFactory(source);
            for (Class<?> mapper : Arrays.asList(SmartAnnotationTaskMapper.class, SmartAnnotationRoundMapper.class, SmartAnnotationCandidateMapper.class, VlsAnnotationImageMapper.class)) factory.getConfiguration().addMapper(mapper);
            return factory;
        }
        @Bean IVlsAnnotationInstanceService annotationService(SqlSessionTemplate sql, DataManagementService data) {
            VlsAnnotationInstanceServiceImpl result = new VlsAnnotationInstanceServiceImpl();
            ReflectionTestUtils.setField(result, "baseMapper", sql.getMapper(VlsAnnotationInstanceMapper.class));
            ReflectionTestUtils.setField(result, "dataManagementService", data);
            ReflectionTestUtils.setField(result, "algorithmAnnotationMapper", sql.getMapper(VlsAlgorithmAnnotationMapper.class));
            ReflectionTestUtils.setField(result, "annotationImageMapper", sql.getMapper(VlsAnnotationImageMapper.class));
            ReflectionTestUtils.setField(result, "annotationLabelService", mock(IVlsAnnotationLabelService.class));
            return result;
        }
        @Bean com.ruoyi.vlstream.test.vlstream.compute.SmartAnnotationComputeService compute() { return mock(com.ruoyi.vlstream.test.vlstream.compute.SmartAnnotationComputeService.class); }
        @Bean SmartAnnotationService smart(SqlSessionTemplate sql, DataManagementService data, DataMediaStorage storage, IVlsAnnotationInstanceService annotations, ObjectMapper json) {
            VlsAlgorithmModelMapper models = mock(VlsAlgorithmModelMapper.class); AlgorithmModel model = new AlgorithmModel(); model.setId(100L); model.setVersion(1); model.setModelName("Test detector"); model.setModelPath("/data/work/model.pt");
            when(models.selectOne(any())).thenReturn(model);
            when(models.selectList(any())).thenReturn(Collections.singletonList(model));
            return new SmartAnnotationService(data, sql.getMapper(SmartAnnotationTaskMapper.class), sql.getMapper(SmartAnnotationRoundMapper.class), sql.getMapper(SmartAnnotationCandidateMapper.class),
                models, mock(VlsAlgorithmMapper.class), sql.getMapper(VlsAnnotationInstanceMapper.class), annotations, mock(GpuTrainingSchedulerService.class), storage, json);
        }
    }
}
