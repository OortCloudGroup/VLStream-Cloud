package com.ruoyi.vlstream.test.vlstream.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.mapper.*;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import com.ruoyi.vlstream.test.vlstream.service.*;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletResponse;
import java.io.ByteArrayInputStream;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@Tag("dev")
class SmartAnnotationServiceTest {
    DataManagementService data;
    SmartAnnotationTaskMapper tasks;
    SmartAnnotationRoundMapper rounds;
    SmartAnnotationCandidateMapper candidates;
    VlsAnnotationInstanceMapper instances;
    IVlsAnnotationInstanceService annotations;
    DataMediaStorage storage;
    SmartAnnotationService service;
    SmartAnnotationTask task;
    SmartAnnotationRound round;
    SmartAnnotationCandidate candidate;
    AnnotationImage image;

    @BeforeEach void setup() {
        data = mock(DataManagementService.class); tasks = mock(SmartAnnotationTaskMapper.class); rounds = mock(SmartAnnotationRoundMapper.class);
        candidates = mock(SmartAnnotationCandidateMapper.class); instances = mock(VlsAnnotationInstanceMapper.class); annotations = mock(IVlsAnnotationInstanceService.class);
        storage = mock(DataMediaStorage.class);
        service = new SmartAnnotationService(data, tasks, rounds, candidates, mock(VlsAlgorithmModelMapper.class), mock(VlsAlgorithmMapper.class), instances,
            annotations, mock(GpuTrainingSchedulerService.class), storage, new ObjectMapper());
        when(data.tenant()).thenReturn("tenant-a");
        task = new SmartAnnotationTask(); task.setId(1L); task.setDatasetId(10L); task.setRoundNumber(1); task.setTaskState("REVIEW"); task.setConfidence(.25);
        round = new SmartAnnotationRound(); round.setId(2L); round.setVersionId(20L); round.setTaskId(1L);
        candidate = new SmartAnnotationCandidate(); candidate.setId(3L); candidate.setImageId(30L); candidate.setReviewState("PENDING");
        image = new AnnotationImage(); image.setId(30L); image.setLocalPath("image.png"); image.setContentSha256("hash"); image.setMediaWidth(100); image.setMediaHeight(80);
        when(tasks.selectOne(any())).thenReturn(task); when(rounds.selectOne(any())).thenReturn(round); when(candidates.selectOne(any())).thenReturn(candidate);
        when(data.sample(10L, 30L)).thenReturn(image);
        AlgorithmAnnotation project = new AlgorithmAnnotation(); project.setAnnotationType("object_detection"); when(data.project(10L)).thenReturn(project);
        DatasetSnapshot snapshot = new DatasetSnapshot(); snapshot.setSamples(Collections.singletonList(image));
        DatasetVersion version = new DatasetVersion(); version.setSnapshotJson("snapshot");
        when(data.version(10L, 20L)).thenReturn(version); when(data.readSnapshot("snapshot")).thenReturn(snapshot);
        when(data.writeJson(any())).thenAnswer(call -> new ObjectMapper().writeValueAsString(call.getArgument(0)));
        when(instances.selectCount(any())).thenReturn(0L);
    }

    static SmartAnnotationRequests.Box box() {
        SmartAnnotationRequests.Box box = new SmartAnnotationRequests.Box(); box.setLabelId(2000000000000000001L);
        box.setX(10d); box.setY(10d); box.setWidth(30d); box.setHeight(20d); box.setConfidence(.8); return box;
    }

    @Test void rejectsNonFiniteAndOutOfBoundsCoordinates() {
        SmartAnnotationRequests.Box box = box(); box.setX(Double.NaN);
        assertThrows(ServiceException.class, () -> SmartAnnotationService.validateBoxes(Collections.singletonList(box), 100, 80));
        box.setX(90d);
        assertThrows(ServiceException.class, () -> SmartAnnotationService.validateBoxes(Collections.singletonList(box), 100, 80));
        box.setX(10d); box.setConfidence(Double.POSITIVE_INFINITY);
        assertThrows(ServiceException.class, () -> SmartAnnotationService.validateBoxes(Collections.singletonList(box), 100, 80));
    }

    @Test void rejectsUnmappedClassAndEmptyPredictions() {
        SmartAnnotationRequests.Box box = box(); box.setLabelId(null);
        assertThrows(ServiceException.class, () -> SmartAnnotationService.validateBoxes(Collections.singletonList(box), 100, 80));
        assertThrows(ServiceException.class, () -> SmartAnnotationService.validateBoxes(Collections.emptyList(), 100, 80));
    }

    @Test void labelIdIsSerializedAsStringWithoutPrecisionLoss() throws Exception {
        assertTrue(new ObjectMapper().writeValueAsString(box()).contains("\"labelId\":\"2000000000000000001\""));
    }

    @Test void streamsCandidateImageThroughTheAuthorizedTask() throws Exception {
        image.setMediaType("image");
        byte[] bytes = { 1, 2, 3, 4 };
        when(storage.read("image.png")).thenReturn(new ByteArrayInputStream(bytes));
        MockHttpServletResponse response = new MockHttpServletResponse();
        new com.ruoyi.vlstream.test.vlstream.controller.VlsSmartAnnotationController(service, null).predictionImage(1L, 3L, response);
        assertArrayEquals(bytes, response.getContentAsByteArray());
        assertEquals("application/octet-stream", response.getContentType());
        assertEquals("private, no-store", response.getHeader("Cache-Control"));
        verify(candidates).selectOne(argThat(query -> {
            String sql = query.getSqlSegment();
            return sql.contains("tenant_id") && sql.contains("task_id") && sql.contains("round_id") && sql.contains("id =");
        }));
        verify(data).sample(10L, 30L);
    }

    @Test void cannotStreamImageOutsideTheCurrentCandidateScope() {
        when(candidates.selectOne(any())).thenReturn(null);
        assertThrows(ServiceException.class, () -> service.predictionImage(1L, 99L));
        verifyNoInteractions(storage);
    }

    @Test void onlyExplicitReviewWritesFormalAnnotations() {
        SmartAnnotationRequests.Review review = new SmartAnnotationRequests.Review(); review.setBoxes(Collections.singletonList(box()));
        service.review(1L, 3L, review);
        verify(annotations).batchSaveAnnotations(eq(10L), eq(30L), argThat(saved -> saved.size() == 1 && saved.get(0).getVerified() == 1
            && "tenant-a".equals(saved.get(0).getTenantId()) && saved.get(0).getImageId().equals(30L)));
        assertEquals("ACCEPTED", candidate.getReviewState());
    }

    @Test void repeatedConfirmationDoesNotDuplicateOrOverwriteInstances() {
        candidate.setReviewState("ACCEPTED");
        service.review(1L, 3L, new SmartAnnotationRequests.Review());
        verifyNoInteractions(annotations);
    }

    @Test void concurrentManualAnnotationsAreNeverOverwritten() {
        when(instances.selectCount(any())).thenReturn(1L);
        SmartAnnotationRequests.Review review = new SmartAnnotationRequests.Review(); review.setBoxes(Collections.singletonList(box()));
        assertThrows(ServiceException.class, () -> service.review(1L, 3L, review));
        verifyNoInteractions(annotations);
        assertEquals("PENDING", candidate.getReviewState());
    }

    @Test void foreignCandidateCannotBeConfirmed() {
        when(candidates.selectOne(any())).thenReturn(null);
        assertThrows(ServiceException.class, () -> service.review(1L, 99L, new SmartAnnotationRequests.Review()));
        verifyNoInteractions(annotations);
    }

    @Test void skippedImageDoesNotCreateEmptyFormalAnnotation() {
        SmartAnnotationRequests.Review review = new SmartAnnotationRequests.Review(); review.setSkip(true);
        service.review(1L, 3L, review);
        assertEquals("SKIPPED", candidate.getReviewState()); verifyNoInteractions(annotations);
    }

    private void predictionBoxes(SmartAnnotationRequests.Box... boxes) throws Exception {
        candidate.setBoxesJson(new ObjectMapper().writeValueAsString(Arrays.asList(boxes)));
        candidate.setImageWidth(100); candidate.setImageHeight(80);
    }

    @Test void pendingPredictionViewSuppressesDuplicatesWithoutWritingCandidates() throws Exception {
        SmartAnnotationRequests.Box lower = box(), higher = box(); higher.setConfidence(.9);
        predictionBoxes(lower, higher);
        Map<String, Object> view = service.prediction(1L, 3L);
        Map<?, ?> quality = (Map<?, ?>) view.get("predictionQuality");
        assertEquals(1, quality.get("suppressedCount"));
        assertEquals(false, quality.get("classConflict"));
        assertEquals(.9, service.boxes(candidate.getBoxesJson()).get(0).getConfidence());
        verify(candidates, never()).updateById(any());
        verifyNoInteractions(annotations);
    }

    @Test void acceptedPredictionsAreDisplayedExactlyAsConfirmed() throws Exception {
        predictionBoxes(box(), box()); candidate.setReviewState("ACCEPTED");
        String confirmed = candidate.getBoxesJson();
        Map<String, Object> view = service.prediction(1L, 3L);
        assertEquals(confirmed, candidate.getBoxesJson());
        assertTrue(((Map<?, ?>) view.get("predictionQuality")).isEmpty());
    }

    @Test void classificationAndMaskPredictionsKeepTheirOriginalPayloads() throws Exception {
        predictionBoxes(box(), box()); String original = candidate.getBoxesJson();
        for (String type : Arrays.asList("image_classification", "instance_segmentation", "semantic_segmentation")) {
            task.setAnnotationType(type);
            Map<String, Object> view = service.prediction(1L, 3L);
            assertEquals(original, candidate.getBoxesJson());
            assertTrue(((Map<?, ?>) view.get("predictionQuality")).isEmpty());
        }
    }

    @Test void pageBatchConfirmationUsesTheSameDeduplicationAsTheEditor() throws Exception {
        SmartAnnotationRequests.Box higher = box(), lower = box(); lower.setConfidence(.1);
        predictionBoxes(lower, higher);
        service.confirmBatch(1L, Collections.singletonList(3L));
        verify(annotations).batchSaveAnnotations(eq(10L), eq(30L), argThat(saved -> saved.size() == 1));
        assertEquals("ACCEPTED", candidate.getReviewState());
    }

    @Test void automaticConfirmationChecksWeakCandidatesBeforeDeduplicating() {
        SmartAnnotationRequests.Box higher = box(), lower = box();
        lower.setConfidence(.1);
        ServiceException error = assertThrows(ServiceException.class,
            () -> service.confirmationBoxes(task, Arrays.asList(higher, lower), true));
        assertTrue(error.getMessage().contains("低置信度"));
    }

    @Test void batchRejectsCrossClassOverlapWhileManualConfirmationCanRetainIt() throws Exception {
        SmartAnnotationRequests.Box first = box(), second = box(); second.setLabelId(2000000000000000002L);
        predictionBoxes(first, second);
        assertTrue(((Map<?, ?>) service.prediction(1L, 3L).get("predictionQuality")).get("classConflict").equals(true));
        ServiceException error = assertThrows(ServiceException.class, () -> service.confirmBatch(1L, Collections.singletonList(3L)));
        assertTrue(error.getMessage().contains("不同类别"));
        verifyNoInteractions(annotations);
        SmartAnnotationRequests.Review review = new SmartAnnotationRequests.Review(); review.setBoxes(Arrays.asList(first, second));
        service.review(1L, 3L, review);
        verify(annotations).batchSaveAnnotations(eq(10L), eq(30L), argThat(saved -> saved.size() == 2));
    }

    @Test void pendingPageSummaryUsesTheSameFilterAsFullPrediction() throws Exception {
        predictionBoxes(box(), box()); image.setImageName("example.png");
        when(candidates.selectPage(any(), any())).thenAnswer(call -> {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<SmartAnnotationCandidate> page = call.getArgument(0);
            page.setRecords(Collections.singletonList(candidate)); page.setTotal(1); return page;
        });
        Map<String, Object> view = service.predictions(1L, 1, "PENDING", false);
        Map<?, ?> row = (Map<?, ?>) ((List<?>) view.get("records")).get(0);
        assertEquals(1, ((Map<?, ?>) row.get("predictionQuality")).get("suppressedCount"));
        assertEquals(1, service.boxes(candidate.getBoxesJson()).size());
        verify(candidates, never()).updateById(any());
    }

    @Test void inputSelectionExcludesAnnotatedImagesAndVideos() {
        AnnotationImage video = new AnnotationImage(); video.setId(31L); video.setMediaType("video");
        AnnotationImage labeled = new AnnotationImage(); labeled.setId(32L); labeled.setMediaType("image");
        AnnotationInstance label = new AnnotationInstance(); label.setImageId(32L);
        DatasetSnapshot snapshot = new DatasetSnapshot(); snapshot.setSamples(Arrays.asList(image, video, labeled)); snapshot.setInstances(Collections.singletonList(label));
        assertEquals(Collections.singletonList(image), SmartAnnotationService.unannotated(snapshot));
    }
    @Test void macArchiveMetadataIsExcludedWithoutDiscardingOrdinaryHiddenImages() {
        AnnotationImage sidecar=new AnnotationImage();sidecar.setId(40L);sidecar.setOriginalName("._helmet.png");
        AnnotationImage macFolder=new AnnotationImage();macFolder.setId(41L);macFolder.setOriginalName("__MACOSX/helmet.png");
        AnnotationImage hidden=new AnnotationImage();hidden.setId(42L);hidden.setOriginalName(".helmet.png");
        DatasetSnapshot snapshot=new DatasetSnapshot();snapshot.setInstances(Collections.emptyList());
        snapshot.setSamples(Arrays.asList(sidecar,macFolder,hidden));
        assertEquals(Collections.singletonList(hidden),SmartAnnotationService.unannotated(snapshot));
    }

    @Test void holdoutIsPreservedAndDuplicateContentCannotCrossSplits() {
        AnnotationImage train = new AnnotationImage(); train.setId(1L); train.setContentSha256("a"); train.setDatasetSplit("train");
        AnnotationImage val = new AnnotationImage(); val.setId(2L); val.setContentSha256("b"); val.setDatasetSplit("val");
        assertEquals("val", SmartAnnotationWorker.partition(Arrays.asList(train, val)).get(2L));
        val.setContentSha256("a");
        assertThrows(ServiceException.class, () -> SmartAnnotationWorker.partition(Arrays.asList(train, val)));
    }

    private SmartAnnotationWorker computeImporter() {
        DatasetSnapshot snapshot=data.readSnapshot("snapshot");
        snapshot.setInstances(Collections.emptyList());
        AnnotationLabel label=new AnnotationLabel();label.setId(2000000000000000001L);label.setName("car");
        snapshot.setLabels(Collections.singletonList(label));task.setTaskState("RUNNING");
        return new SmartAnnotationWorker(service,tasks,rounds,candidates,data,mock(DataMediaStorage.class),new SampleMediaInspector(),
            new YoloDatasetWriter(new ObjectMapper()),new com.ruoyi.vlstream.test.vlstream.config.VlsSshProperties(),new com.ruoyi.vlstream.test.vlstream.config.VlsTrainingContainerProperties(),new ObjectMapper());
    }
    private com.fasterxml.jackson.databind.JsonNode computeResult(long imageId) throws Exception {
        return new ObjectMapper().readTree("{\"predictions\":[{\"imageId\":\""+imageId+"\",\"width\":100,\"height\":80,\"boxes\":[{\"className\":\"car\",\"x\":10,\"y\":10,\"width\":30,\"height\":20,\"confidence\":0.8}]}]}");
    }
    @Test void computeResultImportIsIdempotentAndDoesNotPublishReviewOrFormalLabels() throws Exception {
        SmartAnnotationWorker worker=computeImporter();
        worker.importComputeResults(2L,computeResult(30L),"cloud-training/10/job/weights/best.pt","digest",false);
        worker.importComputeResults(2L,computeResult(30L),"cloud-training/10/job/weights/best.pt","digest",false);
        verify(candidates,times(1)).insert(any(SmartAnnotationCandidate.class));
        assertEquals("RUNNING",task.getTaskState());assertEquals(1,round.getPredictedCount());verifyNoInteractions(annotations);
    }

    @Test void automaticImportPrechecksEntireImagesWithoutWritingFormalLabels() {
        SmartAnnotationWorker worker = computeImporter();
        task.setMode("active");
        task.setReviewSize(5);
        List<AnnotationImage> samples = new ArrayList<>(Collections.singletonList(image));
        for (long id = 31L; id <= 34L; id++) {
            AnnotationImage sample = new AnnotationImage();
            sample.setId(id);
            samples.add(sample);
        }
        data.readSnapshot("snapshot").setSamples(samples);
        ObjectNode result = new ObjectMapper().createObjectNode().put("autoConfirm", true);
        ArrayNode predictions = result.putArray("predictions");
        addPrediction(predictions, 30L, "car", .8, .1);
        addPrediction(predictions, 31L, "car");
        addPrediction(predictions, 32L, "car", .25);
        addPrediction(predictions, 33L, "car", .8);
        addPrediction(predictions, 34L, "unmapped", .8);

        worker.importComputeResults(2L, result, "cloud-training/10/job/weights/best.pt", "digest", true);

        ArgumentCaptor<SmartAnnotationCandidate> inserted = ArgumentCaptor.forClass(SmartAnnotationCandidate.class);
        verify(candidates, times(5)).insert(inserted.capture());
        Map<Long, SmartAnnotationCandidate> byImage = new HashMap<>();
        for (SmartAnnotationCandidate row : inserted.getAllValues()) {
            byImage.put(row.getImageId(), row);
            assertEquals("PENDING", row.getReviewState());
        }
        assertEquals(2, service.boxes(byImage.get(30L).getBoxesJson()).size());
        assertTrue(byImage.get(30L).getReviewError().contains("低置信度"));
        assertTrue(byImage.get(31L).getReviewError().contains("没有预测结果"));
        assertTrue(byImage.get(32L).getReviewError().contains("低置信度"));
        assertTrue(byImage.get(34L).getReviewError().contains("选择"));
        assertNull(byImage.get(33L).getReviewError());
        assertEquals(0, byImage.get(33L).getHardExample());
        assertEquals(1, byImage.get(30L).getHardExample());
        assertEquals("RUNNING", task.getTaskState());
        verifyNoInteractions(annotations);
    }

    private void addPrediction(ArrayNode predictions, long imageId, String className, double... scores) {
        ArrayNode boxes = predictions.addObject().put("imageId", Long.toString(imageId)).put("width", 100).put("height", 80).putArray("boxes");
        for (int i = 0; i < scores.length; i++) {
            boxes.addObject().put("className", className).put("x", 10 + i * 40).put("y", 10).put("width", 30).put("height", 20)
                .put("confidence", scores[i]);
        }
    }

    @Test void computeResultWithUnknownImageIsRejectedBeforeCandidateWrites() throws Exception {
        SmartAnnotationWorker worker=computeImporter();
        assertThrows(ServiceException.class,()->worker.importComputeResults(2L,computeResult(999L),"cloud-training/10/job/weights/best.pt","digest",true));
        verify(candidates,never()).insert(any(SmartAnnotationCandidate.class));
    }
    @Test void cancelledComputeTaskDoesNotAdoptLatePredictions() throws Exception {
        SmartAnnotationWorker worker=computeImporter();task.setTaskState("CANCEL_REQUESTED");
        worker.importComputeResults(2L,computeResult(30L),"cloud-training/10/job/weights/best.pt","digest",true);
        verify(candidates,never()).insert(any(SmartAnnotationCandidate.class));verifyNoInteractions(annotations);
    }
}
