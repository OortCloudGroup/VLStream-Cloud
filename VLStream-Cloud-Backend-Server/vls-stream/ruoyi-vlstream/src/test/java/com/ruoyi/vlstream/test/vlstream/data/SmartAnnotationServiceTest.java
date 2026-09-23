package com.ruoyi.vlstream.test.vlstream.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.mapper.*;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import com.ruoyi.vlstream.test.vlstream.service.*;
import org.junit.jupiter.api.*;
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
    SmartAnnotationService service;
    SmartAnnotationTask task;
    SmartAnnotationRound round;
    SmartAnnotationCandidate candidate;
    AnnotationImage image;

    @BeforeEach void setup() {
        data = mock(DataManagementService.class); tasks = mock(SmartAnnotationTaskMapper.class); rounds = mock(SmartAnnotationRoundMapper.class);
        candidates = mock(SmartAnnotationCandidateMapper.class); instances = mock(VlsAnnotationInstanceMapper.class); annotations = mock(IVlsAnnotationInstanceService.class);
        service = new SmartAnnotationService(data, tasks, rounds, candidates, mock(VlsAlgorithmModelMapper.class), mock(VlsAlgorithmMapper.class), instances,
            annotations, mock(GpuTrainingSchedulerService.class), mock(DataMediaStorage.class), new ObjectMapper());
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

    @Test void inputSelectionExcludesAnnotatedImagesAndVideos() {
        AnnotationImage video = new AnnotationImage(); video.setId(31L); video.setMediaType("video");
        AnnotationImage labeled = new AnnotationImage(); labeled.setId(32L); labeled.setMediaType("image");
        AnnotationInstance label = new AnnotationInstance(); label.setImageId(32L);
        DatasetSnapshot snapshot = new DatasetSnapshot(); snapshot.setSamples(Arrays.asList(image, video, labeled)); snapshot.setInstances(Collections.singletonList(label));
        assertEquals(Collections.singletonList(image), SmartAnnotationService.unannotated(snapshot));
    }

    @Test void holdoutIsPreservedAndDuplicateContentCannotCrossSplits() {
        AnnotationImage train = new AnnotationImage(); train.setId(1L); train.setContentSha256("a"); train.setDatasetSplit("train");
        AnnotationImage val = new AnnotationImage(); val.setId(2L); val.setContentSha256("b"); val.setDatasetSplit("val");
        assertEquals("val", SmartAnnotationWorker.partition(Arrays.asList(train, val)).get(2L));
        val.setContentSha256("a");
        assertThrows(ServiceException.class, () -> SmartAnnotationWorker.partition(Arrays.asList(train, val)));
    }
}
