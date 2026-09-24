package com.ruoyi.vlstream.test.vlstream.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.mapper.*;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import com.ruoyi.vlstream.test.vlstream.service.*;
import com.ruoyi.vlstream.test.vlstream.service.impl.VlsAnnotationInstanceServiceImpl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        String migration = new String(Files.readAllBytes(DataTestConfiguration.root().resolve("VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/db/migration/V1_2_0_022__smart_annotation.sql")), StandardCharsets.UTF_8);
        for (String statement : migration.split(";")) if (!statement.trim().isEmpty()) jdbc.execute(statement);
        applyFourTypeMigration(jdbc);
        TenantContextHolder.setTenantId("tenant-a");
        smart = context.getBean(SmartAnnotationService.class); data = context.getBean(DataManagementService.class); sql = context.getBean(SqlSessionTemplate.class);
        DataRequests.Project request = new DataRequests.Project(); request.setAnnotationName("Smart annotation test"); request.setProjectCode("SMART");
        dataset = data.saveProject(null, request).getId();
        AnnotationLabel category = new AnnotationLabel(); category.setTenantId("tenant-a"); category.setAnnotationId(dataset); category.setName("car"); category.setColor("#3377ff"); category.setIsDeleted(0); category.setStatus(1);
        sql.getMapper(VlsAnnotationLabelMapper.class).insert(category); label = category.getId();
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    static void applyFourTypeMigration(JdbcTemplate jdbc) throws Exception {
        String migration = new String(Files.readAllBytes(DataTestConfiguration.root().resolve("VLStream-Cloud-Backend-Server/vls-stream/ruoyi-admin/src/main/resources/db/migration/V1_2_0_023__four_type_annotation_payloads.sql")), StandardCharsets.UTF_8).replaceAll("(?m)^--.*$", "");
        for (String statement : migration.split(";")) if (!statement.trim().isEmpty()) jdbc.execute(statement);
    }

    Long image(int number) {
        AnnotationImage image = new AnnotationImage(); image.setTenantId("tenant-a"); image.setAnnotationId(dataset); image.setImageName("image-" + number + ".png"); image.setOriginalName(image.getImageName());
        image.setLocalPath(image.getImageName()); image.setMediaType("image"); image.setMediaWidth(100); image.setMediaHeight(80); image.setContentSha256("hash-" + number);
        image.setDatasetSplit("unassigned"); image.setQualityStatus("accepted"); image.setIsDeleted(0); image.setStatus(1);
        sql.getMapper(DataSampleMapper.class).insert(image); return image.getId();
    }

    SmartAnnotationTask create(String mode) {
        SmartAnnotationRequests.Create request = new SmartAnnotationRequests.Create(); request.setDatasetId(dataset); request.setTaskName("Integration round"); request.setMode(mode); request.setSourceType("model"); request.setSourceId(100L);
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

    @Test void batchConflictRollsBackEarlierImagesAndReviewState() {
        Long first = image(1), second = image(2); SmartAnnotationTask task = create("model");
        SmartAnnotationCandidate a = prediction(task, first, label), b = prediction(task, second, 99999L);
        assertThrows(ServiceException.class, () -> smart.confirmBatch(task.getId(), Arrays.asList(a.getId(), b.getId())));
        assertTrue(data.snapshot(dataset).getInstances().isEmpty());
        assertEquals("PENDING", sql.getMapper(SmartAnnotationCandidateMapper.class).selectById(a.getId()).getReviewState());
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
            storage, remote, classes, snapshots, context.getBean(ObjectMapper.class));
        ServiceException rejected = assertThrows(ServiceException.class, () -> cleanup.delete(dataset));
        assertTrue(rejected.getMessage().contains("智能标注")); verifyNoInteractions(storage, remote, classes, snapshots);
        assertNotNull(data.project(dataset));
    }

    @Test void queuedCancellationIsDurableAndReviewCancellationKeepsConfirmedData() {
        Long image = image(1); SmartAnnotationTask task = create("model");
        smart.cancel(task.getId()); assertEquals("CANCEL_REQUESTED", smart.task(task.getId()).getTaskState());
        jdbc.update("UPDATE vls_smart_annotation_task SET task_state='REVIEW' WHERE id=?", task.getId());
        SmartAnnotationCandidate result = prediction(task, image, label);
        SmartAnnotationRequests.Review request = new SmartAnnotationRequests.Review(); request.setBoxes(smart.boxes(result.getBoxesJson())); smart.review(task.getId(), result.getId(), request);
        smart.cancel(task.getId()); assertEquals("CANCELLED", smart.task(task.getId()).getTaskState()); assertEquals(1, data.snapshot(dataset).getInstances().size());
    }

    @Test void activeLearningFreezesOnlyConfirmedLabelsForTheNextRound() {
        for (int n = 0; n < 103; n++) image(n);
        List<AnnotationImage> images = data.snapshot(dataset).getSamples();
        for (int n = 0; n < 10; n++) {
            AnnotationInstance instance = new AnnotationInstance(); instance.setTenantId("tenant-a"); instance.setAnnotationId(dataset); instance.setImageId(images.get(n % 2).getId()); instance.setLabelId(label);
            instance.setAnnotationType(com.ruoyi.vlstream.test.vlstream.enums.AlgorithmAnnotationTypeEnum.rect); instance.setAnnotationData("{\"x\":10,\"y\":10,\"width\":20,\"height\":20}"); instance.setIsDeleted(0); instance.setStatus(1);
            sql.getMapper(VlsAnnotationInstanceMapper.class).insert(instance);
        }
        SmartAnnotationTask task = create("active"); SmartAnnotationCandidate confirmed = prediction(task, images.get(2).getId(), label);
        SmartAnnotationCandidate untouched = prediction(task, images.get(3).getId(), label);
        untouched.setHardExample(0); sql.getMapper(SmartAnnotationCandidateMapper.class).updateById(untouched);
        assertThrows(ServiceException.class, () -> smart.next(task.getId()));
        SmartAnnotationRequests.Review review = new SmartAnnotationRequests.Review(); review.setBoxes(smart.boxes(confirmed.getBoxesJson())); smart.review(task.getId(), confirmed.getId(), review);
        smart.next(task.getId()); SmartAnnotationTask next = smart.task(task.getId());
        assertEquals(2, next.getRoundNumber()); assertEquals("QUEUED", next.getTaskState());
        DatasetSnapshot snapshot = smart.snapshot(next, smart.current(next));
        assertEquals(11, snapshot.getInstances().size()); assertEquals(100, SmartAnnotationService.unannotated(snapshot).size());
        assertEquals(2, data.versions(dataset).size());
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
        SmartAnnotationRequests.Create request = new SmartAnnotationRequests.Create(); request.setDatasetId(dataset); request.setTaskName(type); request.setMode("model"); request.setSourceType("system"); request.setSourceId(0L);
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
        smart.confirmAll(task.getId()); smart.cancel(task.getId()); worker.tick();
        assertEquals("CANCELLED", smart.task(task.getId()).getTaskState()); assertEquals(1, data.snapshot(dataset).getInstances().size());
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
        @Bean SmartAnnotationService smart(SqlSessionTemplate sql, DataManagementService data, DataMediaStorage storage, IVlsAnnotationInstanceService annotations, ObjectMapper json) {
            VlsAlgorithmModelMapper models = mock(VlsAlgorithmModelMapper.class); AlgorithmModel model = new AlgorithmModel(); model.setId(100L); model.setVersion(1); model.setModelName("Test detector"); model.setModelPath("/data/work/model.pt");
            when(models.selectOne(any())).thenReturn(model);
            when(models.selectList(any())).thenReturn(Collections.singletonList(model));
            return new SmartAnnotationService(data, sql.getMapper(SmartAnnotationTaskMapper.class), sql.getMapper(SmartAnnotationRoundMapper.class), sql.getMapper(SmartAnnotationCandidateMapper.class),
                models, mock(VlsAlgorithmMapper.class), sql.getMapper(VlsAnnotationInstanceMapper.class), annotations, mock(GpuTrainingSchedulerService.class), storage, json);
        }
    }
}
