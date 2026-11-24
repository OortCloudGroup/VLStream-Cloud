package com.ruoyi.vlstream.test.vlstream.data;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.TenantContextHolder;
import com.ruoyi.vlstream.test.vlstream.mapper.SmartAnnotationCandidateMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.SmartAnnotationRoundMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.SmartAnnotationTaskMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAnnotationInstanceMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationImage;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationInstance;
import com.ruoyi.vlstream.test.vlstream.service.LlmAnnotationProviderService;
import com.ruoyi.vlstream.test.vlstream.service.OpenAiVisionClient;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PreDestroy;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Claims each image in the database before sending it. Uncertain calls require an explicit retry. */
@Service
@Slf4j
public class SmartAnnotationLlmWorker {
    private final SmartAnnotationTaskMapper tasks;
    private final SmartAnnotationRoundMapper rounds;
    private final SmartAnnotationCandidateMapper candidates;
    private final VlsAnnotationInstanceMapper instances;
    private final SmartAnnotationService service;
    private final DataManagementService data;
    private final DataMediaStorage storage;
    private final LlmAnnotationProviderService providers;
    private final LlmAnnotationAdapter adapter;
    private final TransactionTemplate transaction;
    private ScheduledExecutorService executor;
    private Long cachedRound;
    private String cachedTenant;
    private DatasetSnapshot frozen;
    private Map<Long, AnnotationImage> frozenImages = Collections.emptyMap();

    public SmartAnnotationLlmWorker(SmartAnnotationTaskMapper tasks, SmartAnnotationRoundMapper rounds,
                                    SmartAnnotationCandidateMapper candidates, VlsAnnotationInstanceMapper instances,
                                    SmartAnnotationService service, DataManagementService data, DataMediaStorage storage,
                                    LlmAnnotationProviderService providers, LlmAnnotationAdapter adapter,
                                    PlatformTransactionManager transactions) {
        this.tasks = tasks;
        this.rounds = rounds;
        this.candidates = candidates;
        this.instances = instances;
        this.service = service;
        this.data = data;
        this.storage = storage;
        this.providers = providers;
        this.adapter = adapter;
        this.transaction = new TransactionTemplate(transactions);
        this.transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void start() {
        if (executor != null) return;
        executor = Executors.newSingleThreadScheduledExecutor(operation -> {
            Thread thread = new Thread(operation, "smart-annotation-vision-api");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(this::tick, 2, 1, TimeUnit.SECONDS);
    }

    @PreDestroy public synchronized void stop() {
        if (executor != null) executor.shutdownNow();
    }

    void tick() {
        try {
            List<SmartAnnotationTask> queued = tasks.selectLlmForWorker();
            if (queued.isEmpty()) clearSnapshot();
            for (SmartAnnotationTask task : queued) {
                String previous = TenantContextHolder.getTenantId();
                TenantContextHolder.setTenantId(task.getTenantId());
                try {
                    advance(task.getId());
                } catch (RuntimeException failure) {
                    // Do not log provider content, media paths, or request details.
                    log.warn("Vision annotation iteration failed for task {} ({})", task.getId(), failure.getClass().getSimpleName());
                } finally {
                    TenantContextHolder.setTenantId(previous);
                }
            }
        } catch (RuntimeException failure) {
            log.warn("Vision annotation queue scan failed ({})", failure.getClass().getSimpleName());
        }
    }

    void advance(Long id) {
        Claim claim = transaction.execute(status -> claim(id));
        if (claim == null) return;
        Outcome outcome = infer(claim);
        transaction.execute(status -> {
            SmartAnnotationTask task = service.taskForUpdate(id);
            if (!"RUNNING".equals(task.getTaskState())) return null;
            SmartAnnotationCandidate image = candidates.selectOne(candidateScope(claim.round).eq("id", claim.image.getId()));
            if (image == null || !"RUNNING".equals(image.getReviewState()) || !claim.token.equals(image.getLlmClaimToken())) return null;
            record(task, service.current(task), image, outcome);
            return null;
        });
    }

    private Claim claim(Long id) {
        SmartAnnotationTask task = service.taskForUpdate(id);
        if (!"llm".equals(task.getMode()) || !("QUEUED".equals(task.getTaskState()) || "RUNNING".equals(task.getTaskState()))) return null;
        SmartAnnotationRound round = service.current(task);
        SmartAnnotationCandidate running = candidates.selectOne(candidateScope(round).eq("review_state", "RUNNING").last("LIMIT 1"));
        if (running != null) {
            if (running.getLlmClaimUntil() == null || running.getLlmClaimUntil().getTime() <= System.currentTimeMillis()) {
                record(task, round, running, Outcome.failed("上次调用中断，服务端可能已处理；请核对后手动重试"));
            }
            return null;
        }
        SmartAnnotationCandidate image = candidates.selectOne(candidateScope(round).eq("review_state", "QUEUED").orderByAsc("id").last("LIMIT 1"));
        if (image == null) {
            settle(task, round, null);
            return null;
        }
        String token = UUID.randomUUID().toString();
        candidates.update(null, candidateUpdate(image).set("review_state", "RUNNING").set("llm_claim_token", token)
            .set("llm_claim_until", new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(30))));
        tasks.update(null, taskUpdate(task).set("task_state", "RUNNING").set("update_time", new Date()));
        if (!"RUNNING".equals(round.getRoundState())) rounds.update(null, roundUpdate(round).set("round_state", "RUNNING"));
        return new Claim(task, round, image, token);
    }

    private Outcome infer(Claim claim) {
        SmartAnnotationTask task = claim.task;
        Outcome outcome = new Outcome();
        try {
            providers.verifyBinding(data.tenant(), task.getSourceId(), task.getModelName(), task.getLlmProviderFingerprint());
            if (!Integer.valueOf(LlmAnnotationAdapter.PROMPT_VERSION).equals(task.getLlmPromptVersion())) {
                outcome.stopRemaining = true;
                outcome.error = "任务提示词版本不可用，请重新创建任务";
                return outcome;
            }
            loadSnapshot(claim);
            AnnotationImage image = frozenImages.get(claim.image.getImageId());
            if (image == null) return Outcome.failed("图片不属于本轮输入快照");
            AnnotationImage current = data.sample(task.getDatasetId(), image.getId());
            if (!sameImage(image, current)) return Outcome.failed("图片内容已变化，请使用当前数据集重新发起任务");
            if (alreadyAnnotated(task, image.getId())) {
                outcome.state = "SKIPPED";
                outcome.error = "图片已有人工标注，已跳过模型调用";
                return outcome;
            }
            byte[] bytes = readImage(image);
            outcome.sha256 = DigestUtils.sha256Hex(bytes);
            if (org.apache.commons.lang3.StringUtils.isNotBlank(image.getContentSha256()) && !image.getContentSha256().equalsIgnoreCase(outcome.sha256))
                return Outcome.failed("对象存储图片与本轮快照不一致");
            LlmAnnotationImage.Dimensions dimensions = LlmAnnotationImage.inspect(bytes);
            outcome.width = dimensions.getWidth(); outcome.height = dimensions.getHeight();
            if (!matchesDimension(image.getMediaWidth(), outcome.width) || !matchesDimension(image.getMediaHeight(), outcome.height))
                return Outcome.failed("图片实际尺寸与本轮快照不一致");
            String prompt = adapter.prompt(frozen, task.getLlmPrompt(), outcome.width, outcome.height);
            if (!stillClaimed(claim)) return Outcome.failed("任务已取消或执行权已失效");
            outcome.raw = providers.annotate(data.tenant(), task.getSourceId(), task.getModelName(), task.getLlmProviderFingerprint(), prompt, bytes);
            providers.verifyBinding(data.tenant(), task.getSourceId(), task.getModelName(), task.getLlmProviderFingerprint());
            LlmAnnotationAdapter.Result result = adapter.parse(frozen, outcome.raw, outcome.width, outcome.height);
            outcome.boxes = result.getBoxes();
            outcome.error = reviewReason(task, result, outcome.width, outcome.height);
            outcome.state = "PENDING";
        } catch (ServiceException failure) {
            outcome.error = failure.getMessage();
            Integer code = failure.getCode();
            outcome.stopRemaining = code != null && (code == 401 || code == 402 || code == 403 || code == 409 || code == 429);
        } catch (IOException failure) {
            outcome.error = "读取或解码本轮图片失败";
        } catch (RuntimeException failure) {
            log.warn("Vision annotation image processing failed for task {} ({})", task.getId(), failure.getClass().getSimpleName());
            outcome.error = "无法处理本轮图片或模型结果，请检查后手动重试";
        }
        return outcome;
    }

    private boolean stillClaimed(Claim claim) {
        SmartAnnotationCandidate held = candidates.selectOne(candidateScope(claim.round).eq("id", claim.image.getId())
            .eq("review_state", "RUNNING").eq("llm_claim_token", claim.token).gt("llm_claim_until", new Date())
            .select("id"));
        return held != null;
    }

    private String reviewReason(SmartAnnotationTask task, LlmAnnotationAdapter.Result result, int width, int height) {
        if (result.getUnassignedPixels() > 0 || result.getConflictingPixels() > 0)
            return "语义分割仍有未归类或类别冲突区域，请编辑后确认";
        AnnotationPayloads.validate(task.getAnnotationType(), result.getBoxes(), width, height);
        try {
            service.confirmationBoxes(task, result.getBoxes(), true);
            return "";
        } catch (ServiceException manual) {
            return manual.getMessage();
        }
    }

    private void record(SmartAnnotationTask task, SmartAnnotationRound round, SmartAnnotationCandidate image, Outcome outcome) {
        String message = outcome.error == null ? "" : org.apache.commons.lang3.StringUtils.abbreviate(outcome.error, 1000);
        UpdateWrapper<SmartAnnotationCandidate> update = candidateUpdate(image).set("review_state", outcome.state)
            .set("review_error", message).set("llm_raw_response", outcome.raw).set("llm_input_sha256", outcome.sha256)
            .set("llm_claim_token", null).set("llm_claim_until", null);
        if (outcome.width > 0 && outcome.height > 0) update.set("image_width", outcome.width).set("image_height", outcome.height);
        boolean pending = "PENDING".equals(outcome.state);
        if (pending) {
            double confidence = outcome.boxes.stream().mapToDouble(box -> box.getConfidence() == null ? 0 : box.getConfidence()).min().orElse(0);
            update.set("boxes_json", data.writeJson(outcome.boxes)).set("summary_json", data.writeJson(AnnotationPayloads.summary(outcome.boxes)))
                .set("hard_example", message.isEmpty() ? 0 : 1).set("uncertainty", 1 - confidence);
        }
        candidates.update(null, update);
        int additionallyFailed = outcome.stopRemaining ? candidates.update(null, new UpdateWrapper<SmartAnnotationCandidate>()
            .eq("tenant_id", data.tenant()).eq("round_id", round.getId()).eq("review_state", "QUEUED")
            .set("review_state", "FAILED").set("review_error", "提供商暂不可用，本张图片未提交")) : 0;
        round.setProgressCurrent(round.getProgressCurrent() + 1 + additionallyFailed);
        round.setLlmFailedCount(round.getLlmFailedCount() + ("FAILED".equals(outcome.state) ? 1 : 0) + additionallyFailed);
        round.setPredictedCount(round.getPredictedCount() + (pending ? 1 : 0));
        rounds.update(null, roundUpdate(round).set("progress_current", round.getProgressCurrent())
            .set("llm_failed_count", round.getLlmFailedCount()).set("predicted_count", round.getPredictedCount()));
        if (round.getProgressCurrent() >= round.getProgressTotal()) settle(task, round, outcome.stopRemaining ? message : null);
    }

    private void settle(SmartAnnotationTask task, SmartAnnotationRound round, String providerError) {
        String state = round.getPredictedCount() == 0 && round.getLlmFailedCount() > 0 ? "FAILED" : "REVIEW";
        int eligible = 0;
        if ("REVIEW".equals(state)) {
            eligible = candidates.selectCount(candidateScope(round).eq("review_state", "PENDING").eq("hard_example", 0)
                .and(q -> q.isNull("review_error").or().eq("review_error", ""))).intValue();
        }
        rounds.update(null, roundUpdate(round).set("round_state", state).set("error_message", providerError));
        tasks.update(null, taskUpdate(task).set("task_state", eligible > 0 ? "AUTO_CONFIRMING" : state)
            .set("bulk_cursor", 0).set("bulk_total", eligible).set("bulk_processed", 0).set("bulk_accepted", 0)
            .set("bulk_conflicts", 0).set("bulk_error", null).set("error_message", providerError));
        clearSnapshot();
    }

    private void loadSnapshot(Claim claim) {
        if (claim.round.getId().equals(cachedRound) && data.tenant().equals(cachedTenant)) return;
        frozen = service.snapshot(claim.task, claim.round);
        frozenImages = frozen.getSamples().stream().collect(Collectors.toMap(AnnotationImage::getId, Function.identity()));
        cachedRound = claim.round.getId();
        cachedTenant = data.tenant();
    }

    private void clearSnapshot() {
        cachedRound = null;
        cachedTenant = null;
        frozen = null;
        frozenImages = Collections.emptyMap();
    }

    private boolean alreadyAnnotated(SmartAnnotationTask task, Long imageId) {
        return instances.selectCount(service.<AnnotationInstance>scope().eq("annotation_id", task.getDatasetId()).eq("image_id", imageId)) > 0;
    }

    private static boolean sameImage(AnnotationImage frozen, AnnotationImage current) {
        return Objects.equals(frozen.getLocalPath(), current.getLocalPath())
            && Objects.equals(frozen.getContentSha256(), current.getContentSha256())
            && Objects.equals(frozen.getMediaWidth(), current.getMediaWidth())
            && Objects.equals(frozen.getMediaHeight(), current.getMediaHeight());
    }

    private static boolean matchesDimension(Integer saved, int actual) {
        return saved == null || saved <= 0 || saved == actual;
    }

    private byte[] readImage(AnnotationImage image) throws IOException {
        try (InputStream input = storage.read(image.getLocalPath()); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (output.size() + read > OpenAiVisionClient.MAX_ANNOTATION_IMAGE_BYTES)
                    throw new ServiceException("大模型标注图片超过 8 MiB，请压缩后重试");
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private QueryWrapper<SmartAnnotationCandidate> candidateScope(SmartAnnotationRound round) {
        return service.<SmartAnnotationCandidate>scope().eq("round_id", round.getId());
    }

    private UpdateWrapper<SmartAnnotationCandidate> candidateUpdate(SmartAnnotationCandidate image) {
        return new UpdateWrapper<SmartAnnotationCandidate>().eq("tenant_id", data.tenant()).eq("id", image.getId());
    }

    private UpdateWrapper<SmartAnnotationTask> taskUpdate(SmartAnnotationTask task) {
        return new UpdateWrapper<SmartAnnotationTask>().eq("tenant_id", data.tenant()).eq("id", task.getId());
    }

    private UpdateWrapper<SmartAnnotationRound> roundUpdate(SmartAnnotationRound round) {
        return new UpdateWrapper<SmartAnnotationRound>().eq("tenant_id", data.tenant()).eq("id", round.getId());
    }

    @AllArgsConstructor
    private static class Claim {
        private final SmartAnnotationTask task;
        private final SmartAnnotationRound round;
        private final SmartAnnotationCandidate image;
        private final String token;
    }

    private static class Outcome {
        private String state = "FAILED";
        private String error;
        private String raw;
        private String sha256;
        private boolean stopRemaining;
        private int width;
        private int height;
        private List<SmartAnnotationRequests.Box> boxes;

        static Outcome failed(String message) {
            Outcome result = new Outcome();
            result.error = message;
            return result;
        }
    }
}
