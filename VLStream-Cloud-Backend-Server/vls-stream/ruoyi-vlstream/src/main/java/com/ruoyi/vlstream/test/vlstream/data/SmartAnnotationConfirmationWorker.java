package com.ruoyi.vlstream.test.vlstream.data;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.mapper.SmartAnnotationCandidateMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.SmartAnnotationTaskMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationImage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Resumable CPU-only confirmation. Each image and the durable cursor commit in one transaction. */
@Service
@Slf4j
@RequiredArgsConstructor
public class SmartAnnotationConfirmationWorker {
    private final SmartAnnotationTaskMapper tasks;
    private final SmartAnnotationCandidateMapper candidates;
    private final SmartAnnotationService service;
    private final DataManagementService data;
    private final PlatformTransactionManager transactions;
    private ScheduledExecutorService executor;
    private Long frozenRoundId;
    private Map<Long, AnnotationImage> frozenSamples = Collections.emptyMap();

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void start() {
        if (executor != null) return;
        executor = Executors.newSingleThreadScheduledExecutor(operation -> { Thread thread = new Thread(operation, "smart-annotation-confirmation"); thread.setDaemon(true); return thread; });
        executor.scheduleWithFixedDelay(this::tick, 1, 1, TimeUnit.SECONDS);
    }

    @PreDestroy public void stop() { if (executor != null) executor.shutdownNow(); }

    public void tick() {
        try {
            List<SmartAnnotationTask> confirming = tasks.selectConfirmingForWorker();
            if (confirming.isEmpty()) clearFrozenSamples();
            for (SmartAnnotationTask task : confirming) {
                String previous = TenantContextHolder.getTenantId(); TenantContextHolder.setTenantId(task.getTenantId());
                try { for (int i = 0; i < 20 && advance(task.getId()); i++) { /* bounded work per dataset */ } }
                finally { TenantContextHolder.setTenantId(previous); }
            }
        } catch (Exception ex) { log.warn("Smart annotation confirmation iteration failed", ex); }
    }

    boolean advance(Long id) {
        TransactionTemplate transaction = new TransactionTemplate(transactions);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        Long[] selected = {null};
        try {
            return Boolean.TRUE.equals(transaction.execute(status -> {
                SmartAnnotationTask task = lockTask(id);
                if (!isConfirming(task)) return false;
                boolean automatic = "AUTO_CONFIRMING".equals(task.getTaskState());
                SmartAnnotationRound round = service.current(task);
                QueryWrapper<SmartAnnotationCandidate> query = service.<SmartAnnotationCandidate>scope().eq("round_id", round.getId())
                    .eq("review_state", "PENDING").gt("id", task.getBulkCursor() == null ? 0L : task.getBulkCursor());
                if (automatic) query.and(group -> group.isNull("review_error").or().eq("review_error", ""));
                if ("llm".equals(task.getMode())) service.llmAutoEligible(query);
                SmartAnnotationCandidate candidate = candidates.selectOne(query.orderByAsc("id").last("LIMIT 1"));
                if (candidate == null) {
                    tasks.update(null, taskUpdate(id).set("task_state", "REVIEW").set("bulk_processed", task.getBulkTotal()));
                    if (round.getId().equals(frozenRoundId)) clearFrozenSamples();
                    return false;
                }
                selected[0] = candidate.getId();
                if (automatic) {
                    List<SmartAnnotationRequests.Box> regions = service.confirmationBoxes(task, service.boxes(candidate.getBoxesJson()), true);
                    service.verifyLlmAutoLabels(task, regions);
                    service.saveReview(task, candidate, regions, frozenSample(task, round, candidate.getImageId()));
                } else {
                    SmartAnnotationRequests.Review review = new SmartAnnotationRequests.Review();
                    review.setBoxes(service.batchReviewBoxes(task, candidate));
                    service.review(id, candidate.getId(), review);
                }
                tasks.update(null, taskUpdate(id).set("bulk_cursor", candidate.getId()).set("bulk_processed", number(task.getBulkProcessed()) + 1)
                    .set("bulk_accepted", number(task.getBulkAccepted()) + 1));
                return true;
            }));
        } catch (ServiceException rejected) {
            if (selected[0] == null) { failed(id); return false; }
            transaction.execute(status -> {
                SmartAnnotationTask task = lockTask(id);
                if (!isConfirming(task) || (task.getBulkCursor() != null && task.getBulkCursor() >= selected[0])) return null;
                String message = rejected.getMessage(); if (message.length() > 1000) message = message.substring(0, 1000);
                candidates.update(null, new UpdateWrapper<SmartAnnotationCandidate>().eq("id", selected[0]).eq("tenant_id", data.tenant()).eq("review_state", "PENDING").set("review_error", message));
                tasks.update(null, taskUpdate(id).set("bulk_cursor", selected[0]).set("bulk_processed", number(task.getBulkProcessed()) + 1)
                    .set("bulk_conflicts", number(task.getBulkConflicts()) + 1)); return null;
            });
            return true;
        } catch (RuntimeException failure) {
            failed(id); log.warn("Smart annotation bulk confirmation failed for task {}", id, failure); return false;
        }
    }

    private SmartAnnotationTask lockTask(Long id) {
        data.lockDatasetForTask(service.task(id).getDatasetId());
        return service.taskForUpdate(id);
    }

    private AnnotationImage frozenSample(SmartAnnotationTask task, SmartAnnotationRound round, Long imageId) {
        if (!round.getId().equals(frozenRoundId)) {
            frozenSamples = service.snapshot(task, round).getSamples().stream()
                .collect(Collectors.toMap(AnnotationImage::getId, Function.identity()));
            frozenRoundId = round.getId();
        }
        return frozenSamples.get(imageId);
    }

    private void clearFrozenSamples() {
        frozenRoundId = null;
        frozenSamples = Collections.emptyMap();
    }

    private boolean isConfirming(SmartAnnotationTask task) {
        return "CONFIRMING".equals(task.getTaskState()) || "AUTO_CONFIRMING".equals(task.getTaskState());
    }

    private UpdateWrapper<SmartAnnotationTask> taskUpdate(Long id) {
        return new UpdateWrapper<SmartAnnotationTask>().eq("id", id).eq("tenant_id", data.tenant()).in("task_state", "CONFIRMING", "AUTO_CONFIRMING");
    }
    private void failed(Long id) { tasks.update(null, taskUpdate(id).set("task_state", "REVIEW").set("bulk_error", "批量确认中断，已确认结果保留；可重试剩余结果")); }
    private int number(Integer value) { return value == null ? 0 : value; }
}
