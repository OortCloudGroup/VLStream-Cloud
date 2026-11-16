package com.ruoyi.vlstream.test.vlstream.data;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.mapper.SmartAnnotationCandidateMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.SmartAnnotationTaskMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class SmartAnnotationConfirmationWorkerTest {
    @Test void oneClickConfirmationUsesSharedChecksAndLeavesConflictPending() {
        SmartAnnotationService service = mock(SmartAnnotationService.class);
        SmartAnnotationTaskMapper tasks = mock(SmartAnnotationTaskMapper.class);
        SmartAnnotationCandidateMapper candidates = mock(SmartAnnotationCandidateMapper.class);
        DataManagementService data = mock(DataManagementService.class);
        PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
        when(manager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        SmartAnnotationTask task = new SmartAnnotationTask(); task.setId(1L); task.setDatasetId(10L); task.setTaskState("CONFIRMING");
        SmartAnnotationRound round = new SmartAnnotationRound(); round.setId(2L);
        SmartAnnotationCandidate candidate = new SmartAnnotationCandidate(); candidate.setId(3L); candidate.setReviewState("PENDING");
        when(service.task(1L)).thenReturn(task); when(service.current(task)).thenReturn(round);
        when(service.scope()).thenReturn(new QueryWrapper<>()); when(data.tenant()).thenReturn("tenant-a");
        when(candidates.selectOne(any())).thenReturn(candidate);
        when(service.batchReviewBoxes(task, candidate)).thenThrow(new ServiceException("不同类别的目标框高度重叠，请逐图检查后确认"));
        SmartAnnotationConfirmationWorker worker = new SmartAnnotationConfirmationWorker(tasks, candidates, service, data, manager);
        org.junit.jupiter.api.Assertions.assertTrue(worker.advance(1L));
        verify(service, never()).review(anyLong(), anyLong(), any());
        verify(candidates).update(isNull(), argThat(update -> update.getSqlSet().contains("review_error")));
        verify(tasks).update(isNull(), argThat(update -> update.getSqlSet().contains("bulk_conflicts")));
        org.junit.jupiter.api.Assertions.assertEquals("PENDING", candidate.getReviewState());
    }
}
