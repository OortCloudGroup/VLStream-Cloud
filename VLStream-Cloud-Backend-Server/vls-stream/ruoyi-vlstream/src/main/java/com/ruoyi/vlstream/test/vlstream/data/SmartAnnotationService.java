package com.ruoyi.vlstream.test.vlstream.data;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmAnnotationTypeEnum;
import com.ruoyi.vlstream.test.vlstream.mapper.*;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import com.ruoyi.vlstream.test.vlstream.service.GpuTrainingSchedulerService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAnnotationInstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/** Predictions never enter the formal annotation table until an explicit review transaction. */
@Service
@RequiredArgsConstructor
public class SmartAnnotationService {
    private final DataManagementService data;
    private final SmartAnnotationTaskMapper tasks;
    private final SmartAnnotationRoundMapper rounds;
    private final SmartAnnotationCandidateMapper candidates;
    private final VlsAlgorithmModelMapper models;
    private final VlsAlgorithmMapper algorithms;
    private final VlsAnnotationInstanceMapper instances;
    private final IVlsAnnotationInstanceService annotationService;
    private final GpuTrainingSchedulerService scheduler;
    private final DataMediaStorage storage;
    private final ObjectMapper json;
    @Value("${vlstream.smart-annotation.min-boxes-per-label:10}") private int minBoxes = 10;
    @Value("${vlstream.smart-annotation.min-unlabeled-images:101}") private int minUnlabeled = 101;
    @Value("${vlstream.smart-annotation.max-rounds:4}") private int maxRounds = 4;

    public <T> QueryWrapper<T> scope() { return new QueryWrapper<T>().eq("tenant_id", data.tenant()); }

    public SmartAnnotationTask task(Long id) {
        SmartAnnotationTask task = tasks.selectOne(this.<SmartAnnotationTask>scope().eq("id", id));
        if (task == null) throw new ServiceException("智能标注任务不存在或无权访问");
        data.project(task.getDatasetId());
        return task;
    }

    public SmartAnnotationRound round(Long id) {
        SmartAnnotationRound round = rounds.selectOne(this.<SmartAnnotationRound>scope().eq("id", id));
        if (round == null) throw new ServiceException("智能标注轮次不存在或无权访问");
        task(round.getTaskId());
        return round;
    }

    public SmartAnnotationRound current(SmartAnnotationTask task) {
        SmartAnnotationRound round = rounds.selectOne(this.<SmartAnnotationRound>scope().eq("task_id", task.getId()).eq("round_number", task.getRoundNumber()));
        if (round == null) throw new ServiceException("智能标注轮次不存在");
        return round;
    }

    public IPage<SmartAnnotationTask> list(Long datasetId, int page) {
        QueryWrapper<SmartAnnotationTask> query = scope();
        if (datasetId != null) { data.project(datasetId); query.eq("dataset_id", datasetId); }
        return tasks.selectPage(new Page<>(Math.max(1, page), 20), query.orderByDesc("id"));
    }

    public Map<String, Object> options(Long datasetId) {
        DatasetSnapshot snapshot = data.snapshot(datasetId);
        requireType(snapshot);
        AnnotationTaskType type = AnnotationTaskType.of(snapshot.getAnnotationType());
        List<Map<String, Object>> sources = new ArrayList<>();
        sources.add(DataManagementService.map("id", "0", "type", "system", "name", "系统基础模型 · " + type.getLabel()));
        for (AlgorithmModel model : models.selectList(this.<AlgorithmModel>scope().isNotNull("model_path").orderByDesc("id"))) {
            Algorithm algorithm = model.getAlgorithmId() == null ? null : algorithms.selectOne(this.<Algorithm>scope().eq("id", model.getAlgorithmId()));
            if (pt(model.getModelPath()) && compatible(type, algorithm)) sources.add(DataManagementService.map("id", model.getId().toString(), "type", "model", "name", model.getModelName() + " · V" + model.getVersion()));
        }
        for (Algorithm algorithm : algorithms.selectList(this.<Algorithm>scope().isNotNull("pt_model_file_path").orderByDesc("id"))) {
            if (pt(algorithm.getPtModelFilePath()) && compatible(type, algorithm)) sources.add(DataManagementService.map("id", algorithm.getId().toString(), "type", "algorithm", "name", algorithm.getName() + " · 基础权重"));
        }
        for (SmartAnnotationRound round : rounds.selectList(this.<SmartAnnotationRound>scope().eq("round_state", "REVIEW").isNotNull("model_path").orderByDesc("id"))) {
            SmartAnnotationTask owner = tasks.selectOne(this.<SmartAnnotationTask>scope().eq("id", round.getTaskId()).eq("mode", "active"));
            if (owner != null && snapshot.getAnnotationType().equals(owner.getAnnotationType())) sources.add(DataManagementService.map("id", round.getId().toString(), "type", "round", "name", owner.getTaskName() + " · 第" + round.getRoundNumber() + "轮"));
        }
        Map<Long, Long> counts = seedCounts(snapshot);
        return DataManagementService.map("annotationType", type.getCode(), "countUnit", type == AnnotationTaskType.CLASSIFICATION || type == AnnotationTaskType.SEMANTIC_SEGMENTATION ? "张图片" : "个实例",
            "sources", sources, "labels", snapshot.getLabels(), "unlabeled", unannotated(snapshot).size(),
            "labelCounts", snapshot.getLabels().stream().map(label -> DataManagementService.map("name", label.getName(), "count", counts.getOrDefault(label.getId(), 0L))).collect(Collectors.toList()),
            "minBoxesPerLabel", minBoxes, "minUnlabeledImages", minUnlabeled, "maxRounds", maxRounds);
    }

    @Transactional(rollbackFor = Exception.class)
    public SmartAnnotationTask create(SmartAnnotationRequests.Create request) {
        data.lockDatasetForTask(request.getDatasetId());
        if (tasks.selectCount(this.<SmartAnnotationTask>scope().eq("dataset_id", request.getDatasetId())
            .in("task_state", "QUEUED", "PREPARING", "RUNNING", "REVIEW", "CONFIRMING", "CANCEL_REQUESTED")) > 0)
            throw new ServiceException("当前数据集已有进行中的智能标注任务，请先完成或取消");
        DatasetSnapshot snapshot = data.snapshot(request.getDatasetId());
        validateInput(snapshot, request.getMode(), true);
        SmartAnnotationTask task = new SmartAnnotationTask();
        task.setTenantId(data.tenant()); task.setDatasetId(request.getDatasetId()); task.setTaskName(request.getTaskName());
        task.setMode(request.getMode()); task.setSourceType(request.getSourceType()); task.setSourceId(request.getSourceId());
        task.setAnnotationType(snapshot.getAnnotationType());
        resolveSource(task);
        task.setConfidence(request.getConfidence()); task.setEpochs(request.getEpochs()); task.setReviewSize(request.getReviewSize());
        task.setRoundNumber(1); task.setTaskState("QUEUED"); tasks.insert(task);
        enqueueRound(task);
        return task;
    }

    private void resolveSource(SmartAnnotationTask task) {
        AnnotationTaskType type = AnnotationTaskType.of(task.getAnnotationType());
        if ("system".equals(task.getSourceType())) {
            if (!Long.valueOf(0).equals(task.getSourceId())) throw new ServiceException("系统模型标识无效");
            task.setModelName("系统基础模型 · " + type.getLabel()); task.setModelPath("@preset/" + type.getModelTask()); return;
        } else if ("algorithm".equals(task.getSourceType())) {
            if (!"active".equals(task.getMode())) throw new ServiceException("指定模型模式请选择已训练模型或主动学习模型");
            Algorithm source = algorithms.selectOne(this.<Algorithm>scope().eq("id", task.getSourceId()));
            if (source == null) throw new ServiceException("基础算法不存在或无权访问");
            if (!compatible(type, source)) throw new ServiceException("基础算法与数据集标注类型不匹配");
            task.setModelName(source.getName()); task.setModelPath(source.getPtModelFilePath());
        } else if ("model".equals(task.getSourceType())) {
            AlgorithmModel source = models.selectOne(this.<AlgorithmModel>scope().eq("id", task.getSourceId()));
            if (source == null) throw new ServiceException("模型不存在或无权访问");
            Algorithm algorithm = source.getAlgorithmId() == null ? null : algorithms.selectOne(this.<Algorithm>scope().eq("id", source.getAlgorithmId()));
            if (!compatible(type, algorithm)) throw new ServiceException("模型与数据集标注类型不匹配");
            task.setModelName(source.getModelName()); task.setModelPath(source.getModelPath());
        } else if ("round".equals(task.getSourceType())) {
            SmartAnnotationRound source = round(task.getSourceId());
            SmartAnnotationTask owner = task(source.getTaskId());
            if (!"active".equals(owner.getMode()) || !"REVIEW".equals(source.getRoundState())) throw new ServiceException("该轮主动学习模型尚不可用");
            if (!task.getAnnotationType().equals(owner.getAnnotationType())) throw new ServiceException("历史标注模型与当前数据集类型不匹配");
            task.setModelName(owner.getTaskName() + " 第" + source.getRoundNumber() + "轮"); task.setModelPath(source.getModelPath());
        } else throw new ServiceException("不支持的模型来源");
        if (!pt(task.getModelPath())) throw new ServiceException("请选择具有可用 PT 权重的匹配模型");
    }

    private boolean compatible(AnnotationTaskType type, Algorithm algorithm) {
        return algorithm == null || algorithm.getCategory() == null || type.getModelTask().equals(algorithm.getCategory().getCode());
    }

    private boolean pt(String path) { return path != null && path.startsWith("/") && path.endsWith(".pt") && !path.contains("/../"); }

    private void requireType(DatasetSnapshot snapshot) {
        AnnotationTaskType.of(snapshot.getAnnotationType());
        if (snapshot.getLabels().isEmpty()) throw new ServiceException("请先在标注页面创建需要识别的标签");
    }

    private void validateInput(DatasetSnapshot snapshot, String mode, boolean first) {
        requireType(snapshot);
        int count = unannotated(snapshot).size();
        if (count == 0) throw new ServiceException("没有可预标注的未标注图片");
        if (!"active".equals(mode)) return;
        if (first && count < minUnlabeled) throw new ServiceException("主动学习至少需要 " + minUnlabeled + " 张未标注图片");
        Map<Long, Long> counts = seedCounts(snapshot);
        for (AnnotationLabel label : snapshot.getLabels()) if (counts.getOrDefault(label.getId(), 0L) < minBoxes)
            throw new ServiceException("主动学习需要每个标签至少 " + minBoxes + " 个已确认样本/实例：" + label.getName());
        if (snapshot.getInstances().stream().map(AnnotationInstance::getImageId).distinct().count() < 2)
            throw new ServiceException("主动学习至少需要两张不同的已标注图片");
    }

    public static List<AnnotationImage> unannotated(DatasetSnapshot snapshot) {
        Set<Long> annotated = snapshot.getInstances().stream().map(AnnotationInstance::getImageId).collect(Collectors.toSet());
        return snapshot.getSamples().stream().filter(s -> s.getMediaType() == null || "image".equals(s.getMediaType()))
            .filter(s -> !annotated.contains(s.getId())).filter(DatasetPartitioner::usable)
            .collect(Collectors.toList());
    }

    private Map<Long, Long> seedCounts(DatasetSnapshot snapshot) {
        Set<Long> usable = snapshot.getSamples().stream().filter(DatasetPartitioner::usable).map(AnnotationImage::getId).collect(Collectors.toSet());
        List<AnnotationInstance> valid = snapshot.getInstances().stream().filter(instance -> usable.contains(instance.getImageId())).collect(Collectors.toList());
        AnnotationTaskType type = AnnotationTaskType.of(snapshot.getAnnotationType());
        if (type == AnnotationTaskType.CLASSIFICATION || type == AnnotationTaskType.SEMANTIC_SEGMENTATION) {
            Map<Long, Set<Long>> images = new HashMap<>();
            valid.forEach(instance -> images.computeIfAbsent(instance.getLabelId(), ignored -> new HashSet<>()).add(instance.getImageId()));
            return images.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> (long) entry.getValue().size()));
        }
        return valid.stream().collect(Collectors.groupingBy(AnnotationInstance::getLabelId, Collectors.counting()));
    }

    private void enqueueRound(SmartAnnotationTask task) {
        DataRequests.Version request = new DataRequests.Version();
        request.setName("智能标注 · " + shortName(task.getTaskName()) + " · 第" + task.getRoundNumber() + "轮");
        request.setDescription("本轮输入快照；预标注结果须人工确认后才进入正式标注");
        DatasetVersion version = data.saveVersion(task.getDatasetId(), request);
        SmartAnnotationRound round = new SmartAnnotationRound(); round.setTenantId(data.tenant());
        round.setTaskId(task.getId()); round.setRoundNumber(task.getRoundNumber()); round.setVersionId(version.getId());
        round.setRoundState("QUEUED"); round.setPredictedCount(0); rounds.insert(round);
        scheduler.enqueueSmartAnnotation(round.getId());
    }

    public Map<String, Object> detail(Long id) {
        SmartAnnotationTask task = task(id); SmartAnnotationRound round = current(task);
        List<SmartAnnotationCandidate> all = candidates.selectList(this.<SmartAnnotationCandidate>scope().eq("round_id", round.getId()).select("id", "review_state", "hard_example"));
        Map<String, Long> counts = all.stream().collect(Collectors.groupingBy(SmartAnnotationCandidate::getReviewState, Collectors.counting()));
        return DataManagementService.map("task", task, "round", round, "rounds", rounds.selectList(this.<SmartAnnotationRound>scope().eq("task_id", id).orderByAsc("round_number")),
            "counts", counts, "pendingHard", all.stream().filter(c -> c.getHardExample() == 1 && "PENDING".equals(c.getReviewState())).count(),
            "labels", data.snapshot(task.getDatasetId()).getLabels(), "maxRounds", maxRounds);
    }

    public Map<String, Object> predictions(Long id, int page, String state, boolean hardOnly) {
        SmartAnnotationTask task = task(id); SmartAnnotationRound round = current(task);
        QueryWrapper<SmartAnnotationCandidate> query = this.<SmartAnnotationCandidate>scope().eq("round_id", round.getId());
        query.select("id", "task_id", "round_id", "image_id", "image_width", "image_height", "summary_json AS boxes_json", "review_error", "uncertainty", "hard_example", "review_state");
        if (state != null && !state.isEmpty()) query.eq("review_state", state);
        if (hardOnly) query.eq("hard_example", 1);
        IPage<SmartAnnotationCandidate> result = candidates.selectPage(new Page<>(Math.max(1, page), 20), query.orderByDesc("hard_example", "uncertainty").orderByAsc("id"));
        DatasetSnapshot snapshot = snapshot(task, round);
        Map<Long, AnnotationImage> images = snapshot.getSamples().stream().collect(Collectors.toMap(AnnotationImage::getId, s -> s));
        List<Map<String, Object>> rows = result.getRecords().stream().map(c -> {
            AnnotationImage sample = images.get(c.getImageId());
            return DataManagementService.map("candidate", c, "imageName", sample.getImageName(), "width", c.getImageWidth(), "height", c.getImageHeight(), "previewUrl", storage.preview(sample.getLocalPath()));
        }).collect(Collectors.toList());
        return DataManagementService.map("records", rows, "total", result.getTotal());
    }

    public Map<String, Object> prediction(Long id, Long candidateId) {
        SmartAnnotationTask task = task(id); SmartAnnotationRound round = current(task);
        SmartAnnotationCandidate candidate = candidates.selectOne(this.<SmartAnnotationCandidate>scope().eq("id", candidateId).eq("task_id", id).eq("round_id", round.getId()));
        if (candidate == null) throw new ServiceException("预标注结果不存在或不属于当前轮次");
        AnnotationImage image = data.sample(task.getDatasetId(), candidate.getImageId());
        return DataManagementService.map("candidate", candidate, "imageName", image.getImageName(), "width", candidate.getImageWidth(), "height", candidate.getImageHeight(),
            "previewUrl", storage.preview(image.getLocalPath()), "annotationType", task.getAnnotationType());
    }

    public DatasetSnapshot snapshot(SmartAnnotationTask task, SmartAnnotationRound round) {
        return data.readSnapshot(data.version(task.getDatasetId(), round.getVersionId()).getSnapshotJson());
    }

    public List<SmartAnnotationRequests.Box> boxes(String value) {
        try { return json.readValue(value, new TypeReference<List<SmartAnnotationRequests.Box>>() { }); }
        catch (Exception ex) { throw new ServiceException("预标注结果无法解析"); }
    }

    @Transactional(rollbackFor = Exception.class)
    public void review(Long id, Long candidateId, SmartAnnotationRequests.Review request) {
        SmartAnnotationTask task = task(id); data.lockDatasetForTask(task.getDatasetId()); task = task(id);
        if (!Arrays.asList("REVIEW", "CONFIRMING").contains(task.getTaskState())) throw new ServiceException("当前任务不可确认标注");
        if (!task.getAnnotationType().equals(data.project(task.getDatasetId()).getAnnotationType())) throw new ServiceException("数据集类型已变更，请取消旧任务后重新发起");
        SmartAnnotationCandidate candidate = candidates.selectOne(this.<SmartAnnotationCandidate>scope().eq("id", candidateId)
            .eq("round_id", current(task).getId()).eq("task_id", id));
        if (candidate == null) throw new ServiceException("预标注结果不存在或不属于当前轮次");
        if (!"PENDING".equals(candidate.getReviewState())) return; // Safe retry after a lost HTTP response.
        if (request.isSkip()) { candidate.setReviewState("SKIPPED"); candidates.updateById(candidate); return; }
        AnnotationImage sample = data.sample(task.getDatasetId(), candidate.getImageId());
        AnnotationImage frozen = snapshot(task, current(task)).getSamples().stream().filter(s -> s.getId().equals(sample.getId())).findFirst().orElseThrow(() -> new ServiceException("图片不属于本轮输入"));
        if (!Objects.equals(frozen.getLocalPath(), sample.getLocalPath()) || !Objects.equals(frozen.getContentSha256(), sample.getContentSha256())
            || !Objects.equals(frozen.getMediaWidth(), sample.getMediaWidth()) || !Objects.equals(frozen.getMediaHeight(), sample.getMediaHeight()))
            throw new ServiceException("图片内容已变化，请跳过旧结果后重新发起任务");
        if (instances.selectCount(this.<AnnotationInstance>scope().eq("annotation_id", task.getDatasetId()).eq("image_id", sample.getId())) > 0)
            throw new ServiceException("图片已被人工标注，旧预标注不能覆盖；请跳过此结果");
        AnnotationPayloads.validate(task.getAnnotationType(), request.getBoxes(), candidate.getImageWidth() == null ? sample.getMediaWidth() : candidate.getImageWidth(), candidate.getImageHeight() == null ? sample.getMediaHeight() : candidate.getImageHeight());
        data.validateAnnotationOwner(task.getDatasetId(), sample.getId(), request.getBoxes().stream().map(SmartAnnotationRequests.Box::getLabelId).collect(Collectors.toList()));
        List<AnnotationInstance> confirmed = new ArrayList<>();
        for (SmartAnnotationRequests.Box box : request.getBoxes()) {
            AnnotationInstance instance = new AnnotationInstance(); instance.setTenantId(data.tenant()); instance.setAnnotationId(task.getDatasetId());
            instance.setImageId(sample.getId()); instance.setLabelId(box.getLabelId()); instance.setAnnotationType(AnnotationPayloads.geometry(task.getAnnotationType()));
            instance.setAnnotationData(data.writeJson(AnnotationPayloads.content(task.getAnnotationType(), box)));
            instance.setVerified(1); instance.setConfidence(BigDecimal.valueOf(box.getConfidence() == null ? 1 : box.getConfidence())); confirmed.add(instance);
        }
        annotationService.batchSaveAnnotations(task.getDatasetId(), sample.getId(), confirmed);
        candidate.setBoxesJson(data.writeJson(request.getBoxes())); candidate.setSummaryJson(data.writeJson(AnnotationPayloads.summary(request.getBoxes())));
        candidate.setReviewError(""); candidate.setReviewState("ACCEPTED"); candidates.updateById(candidate);
    }

    public static void validateBoxes(List<SmartAnnotationRequests.Box> boxes, Integer width, Integer height) {
        if (boxes == null || boxes.isEmpty() || boxes.size() > 1000) throw new ServiceException("请补充目标框；无目标图片可跳过，跳过不会计为已标注");
        if (width == null || height == null || width <= 0 || height <= 0) throw new ServiceException("图片尺寸无效");
        for (SmartAnnotationRequests.Box box : boxes) {
            if (box.getLabelId() == null) throw new ServiceException("请为所有目标框选择当前数据集的标签");
            Double[] values = {box.getX(), box.getY(), box.getWidth(), box.getHeight()};
            for (Double value : values) if (value == null || !Double.isFinite(value)) throw new ServiceException("目标框坐标必须为有限数值");
            if (box.getX() < 0 || box.getY() < 0 || box.getWidth() <= 0 || box.getHeight() <= 0
                || box.getX() + box.getWidth() > width + 0.01 || box.getY() + box.getHeight() > height + 0.01)
                throw new ServiceException("目标框必须位于图片范围内且宽高大于零");
            if (box.getConfidence() != null && (!Double.isFinite(box.getConfidence()) || box.getConfidence() < 0 || box.getConfidence() > 1))
                throw new ServiceException("置信度必须介于 0 和 1 之间");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmBatch(Long id, List<Long> ids) {
        SmartAnnotationTask task = task(id);
        for (Long candidateId : new LinkedHashSet<>(ids)) {
            SmartAnnotationCandidate candidate = candidates.selectOne(this.<SmartAnnotationCandidate>scope().eq("id", candidateId).eq("round_id", current(task).getId()));
            if (candidate == null) throw new ServiceException("选中的结果不属于当前轮次");
            SmartAnnotationRequests.Review request = new SmartAnnotationRequests.Review(); request.setBoxes(boxes(candidate.getBoxesJson()));
            if (request.getBoxes().stream().anyMatch(b -> b.getConfidence() == null || b.getConfidence() < task.getConfidence()))
                throw new ServiceException("选中结果包含低置信度框，请逐张检查");
            review(id, candidateId, request);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void next(Long id) {
        SmartAnnotationTask task = task(id); data.lockDatasetForTask(task.getDatasetId()); task = task(id);
        if (!"active".equals(task.getMode()) || !"REVIEW".equals(task.getTaskState()) || task.getRoundNumber() >= maxRounds)
            throw new ServiceException("当前任务不能启动下一轮");
        SmartAnnotationRound round = current(task);
        if (candidates.selectCount(this.<SmartAnnotationCandidate>scope().eq("round_id", round.getId()).eq("hard_example", 1).eq("review_state", "PENDING")) > 0)
            throw new ServiceException("请先完成本轮难例的确认或跳过");
        if (candidates.selectCount(this.<SmartAnnotationCandidate>scope().eq("round_id", round.getId()).eq("review_state", "ACCEPTED")) == 0)
            throw new ServiceException("本轮尚未增加已确认样本，请先确认标注后继续学习");
        validateInput(data.snapshot(task.getDatasetId()), task.getMode(), false);
        task.setModelPath(round.getModelPath()); task.setRoundNumber(task.getRoundNumber() + 1); task.setTaskState("QUEUED"); tasks.updateById(task);
        enqueueRound(task);
    }

    @Transactional(rollbackFor = Exception.class)
    public void finish(Long id) {
        SmartAnnotationTask task = task(id); data.lockDatasetForTask(task.getDatasetId()); task = task(id);
        if (!"REVIEW".equals(task.getTaskState())) throw new ServiceException("当前任务不可完成");
        DataRequests.Version version = new DataRequests.Version(); version.setName("智能标注结果 · " + shortName(task.getTaskName()));
        version.setDescription("仅包含已确认标注，未确认预标注不会进入训练"); data.saveVersion(task.getDatasetId(), version);
        task.setTaskState("COMPLETED"); tasks.updateById(task);
    }

    @Transactional(rollbackFor = Exception.class)
    public void retry(Long id) {
        SmartAnnotationTask task = task(id); data.lockDatasetForTask(task.getDatasetId()); task = task(id);
        if (!"FAILED".equals(task.getTaskState())) throw new ServiceException("只有失败任务可以重试");
        SmartAnnotationRound round = current(task);
        rounds.update(null, new UpdateWrapper<SmartAnnotationRound>().eq("id", round.getId()).eq("tenant_id", data.tenant()).set("round_state", "QUEUED").set("error_message", null));
        tasks.update(null, new UpdateWrapper<SmartAnnotationTask>().eq("id", id).eq("tenant_id", data.tenant()).set("task_state", "QUEUED").set("error_message", null));
        scheduler.enqueueSmartAnnotation(round.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        SmartAnnotationTask task = task(id); data.lockDatasetForTask(task.getDatasetId()); task = task(id);
        if (Arrays.asList("COMPLETED", "CANCELLED").contains(task.getTaskState())) return;
        String state = Arrays.asList("FAILED", "REVIEW", "CONFIRMING").contains(task.getTaskState()) ? "CANCELLED" : "CANCEL_REQUESTED";
        task.setTaskState(state); tasks.updateById(task);
    }

    private String shortName(String name) { return name.substring(0, Math.min(70, name.length())); }

    @Transactional(rollbackFor = Exception.class)
    public void confirmAll(Long id) {
        SmartAnnotationTask task = task(id); data.lockDatasetForTask(task.getDatasetId()); task = task(id);
        if ("CONFIRMING".equals(task.getTaskState())) return;
        if (!"REVIEW".equals(task.getTaskState())) throw new ServiceException("当前任务不可批量确认");
        int total = candidates.selectCount(this.<SmartAnnotationCandidate>scope().eq("round_id", current(task).getId()).eq("review_state", "PENDING")).intValue();
        tasks.update(null, new UpdateWrapper<SmartAnnotationTask>().eq("id", id).eq("tenant_id", data.tenant()).set("task_state", "CONFIRMING")
            .set("bulk_cursor", 0).set("bulk_total", total).set("bulk_processed", 0).set("bulk_accepted", 0).set("bulk_conflicts", 0).set("bulk_error", null));
    }
}
