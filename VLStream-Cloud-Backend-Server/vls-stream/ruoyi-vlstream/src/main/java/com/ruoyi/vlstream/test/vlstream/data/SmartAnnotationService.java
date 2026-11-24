package com.ruoyi.vlstream.test.vlstream.data;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.vlstream.test.vlstream.enums.AlgorithmAnnotationTypeEnum;
import com.ruoyi.vlstream.test.vlstream.mapper.*;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import com.ruoyi.vlstream.test.vlstream.service.GpuTrainingSchedulerService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAnnotationInstanceService;
import com.ruoyi.vlstream.test.vlstream.service.LlmAnnotationProviderService;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/** Predictions never enter the formal annotation table until an explicit review transaction. */
@Service
@RequiredArgsConstructor
public class SmartAnnotationService {
    private static final List<String> ACTIVE_TASK_STATES = Arrays.asList(
        "QUEUED", "PREPARING", "RUNNING", "REVIEW", "CONFIRMING", "AUTO_CONFIRMING", "CANCEL_REQUESTED");
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
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.ruoyi.vlstream.test.vlstream.compute.SmartAnnotationComputeService compute;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.ruoyi.vlstream.test.vlstream.compute.ComputePublicTaskAccess computeAccess;
    @org.springframework.beans.factory.annotation.Autowired
    private LlmAnnotationProviderService llmProvider;
    @org.springframework.beans.factory.annotation.Autowired
    private JdbcTemplate jdbc;
    @Value("${vlstream.smart-annotation.min-boxes-per-label:10}") private int minBoxes = 10;
    @Value("${vlstream.smart-annotation.min-unlabeled-images:101}") private int minUnlabeled = 101;
    @Value("${vlstream.smart-annotation.max-rounds:4}") private int maxRounds = 4;

    public <T> QueryWrapper<T> scope() {
        String tenant = data.tenant();
        return new QueryWrapper<T>().eq("tenant_id", tenant).apply("CAST(tenant_id AS BINARY)=CAST({0} AS BINARY)", tenant);
    }

    private AlgorithmAnnotation requireDataset(Long id) {
        AlgorithmAnnotation project = data.project(id);
        if (!Objects.equals(project.getTenantId(), data.tenant())) throw new ServiceException("数据集不存在或无权访问", 403);
        return project;
    }

    public SmartAnnotationTask task(Long id) {
        return requireTask(this.<SmartAnnotationTask>scope().eq("id", id));
    }

    SmartAnnotationTask taskForUpdate(Long id) {
        return requireTask(this.<SmartAnnotationTask>scope().eq("id", id).last("FOR UPDATE"));
    }

    private SmartAnnotationTask requireTask(QueryWrapper<SmartAnnotationTask> query) {
        SmartAnnotationTask task = tasks.selectOne(query);
        if (task == null || !Objects.equals(task.getTenantId(), data.tenant()))
            throw new ServiceException("智能标注任务不存在或无权访问");
        requireDataset(task.getDatasetId());
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
        return list(datasetId, page, null);
    }

    public IPage<SmartAnnotationTask> list(Long datasetId, int page, String mode) {
        QueryWrapper<SmartAnnotationTask> query = scope();
        if ("llm".equals(mode)) query.eq("mode", "llm");
        else if (mode == null || mode.isEmpty() || "compute".equals(mode)) query.in("mode", "active", "model");
        else throw new ServiceException("无效的智能标注方式筛选");
        if (datasetId != null) { requireDataset(datasetId); query.eq("dataset_id", datasetId); }
        return tasks.selectPage(new Page<>(Math.max(1, page), 20), query.orderByDesc("id"))
            .convert(task -> taskView(task, false));
    }

    public Map<String, Object> options(Long datasetId, boolean forActiveLearning) {
        AlgorithmAnnotation project = requireDataset(datasetId);
        boolean datasetGenerated = data.isDatasetGenerated(datasetId);
        if (forActiveLearning && !datasetGenerated) {
            return DataManagementService.map("datasetName", project.getAnnotationName(), "datasetGenerated", false);
        }
        String annotationType = project.getAnnotationType();
        List<AnnotationLabel> labels = data.datasetLabels(datasetId);
        AnnotationTaskType type = requireType(annotationType, labels);
        List<Map<String, Object>> sources = new ArrayList<>();
        sources.add(DataManagementService.map("id", "0", "type", "system", "name", "系统基础模型 · " + type.getLabel()));
        QueryWrapper<AlgorithmModel> modelSources = this.<AlgorithmModel>scope().isNotNull("model_path").orderByDesc("id");
        if (computeAccess != null) computeAccess.filterModels(modelSources);
        for (AlgorithmModel model : models.selectList(modelSources)) {
            Algorithm algorithm = model.getAlgorithmId() == null ? null : algorithms.selectOne(this.<Algorithm>scope().eq("id", model.getAlgorithmId()));
            if (pt(model.getModelPath()) && compatible(type, algorithm)) sources.add(DataManagementService.map("id", model.getId().toString(), "type", "model", "name", model.getModelName() + " · V" + model.getVersion()));
        }
        for (Algorithm algorithm : algorithms.selectList(this.<Algorithm>scope().isNotNull("pt_model_file_path").orderByDesc("id"))) {
            if (pt(algorithm.getPtModelFilePath()) && compatible(type, algorithm)) sources.add(DataManagementService.map("id", algorithm.getId().toString(), "type", "algorithm", "name", algorithm.getName() + " · 基础权重"));
        }
        for (SmartAnnotationRound round : rounds.selectList(this.<SmartAnnotationRound>scope().eq("round_state", "REVIEW").isNotNull("model_path").orderByDesc("id"))) {
            SmartAnnotationTask owner = tasks.selectOne(this.<SmartAnnotationTask>scope().eq("id", round.getTaskId()).eq("mode", "active"));
            if (owner != null && annotationType.equals(owner.getAnnotationType()) && canUseRoundModel(owner, round))
                sources.add(DataManagementService.map("id", round.getId().toString(), "type", "round", "name", owner.getTaskName() + " · 第" + round.getRoundNumber() + "轮"));
        }
        InputCounts counts = inputCounts(datasetId, type);
        return DataManagementService.map("annotationType", type.getCode(), "countUnit", countSeedsByImage(type) ? "张图片" : "个实例",
            "sources", sources, "labels", labels, "unlabeled", counts.unlabeled,
            "datasetName", project.getAnnotationName(), "datasetGenerated", datasetGenerated,
            "labelCounts", labels.stream().map(label -> DataManagementService.map("name", label.getName(), "count", counts.seeds.getOrDefault(label.getId(), 0L))).collect(Collectors.toList()),
            "minBoxesPerLabel", minBoxes, "minUnlabeledImages", minUnlabeled, "maxRounds", maxRounds);
    }

    private InputCounts inputCounts(Long datasetId, AnnotationTaskType type) {
        InputCounts counts = new InputCounts();
        boolean byImage = countSeedsByImage(type);
        Long afterId = null;
        while (true) {
            List<AnnotationImage> batch = data.sampleMetadataBatch(datasetId, afterId);
            if (batch.isEmpty()) return counts;
            afterId = batch.get(batch.size() - 1).getId();
            List<AnnotationImage> usable = batch.stream().filter(DatasetPartitioner::usable).collect(Collectors.toList());
            if (usable.isEmpty()) continue;
            List<Long> ids = usable.stream().map(AnnotationImage::getId).collect(Collectors.toList());
            List<Map<String, Object>> groups = instances.selectMaps(this.<AnnotationInstance>scope().eq("annotation_id", datasetId)
                .in("image_id", ids).select("image_id", "label_id", "COUNT(*) AS instance_count").groupBy("image_id", "label_id"));
            Set<Long> annotated = new HashSet<>();
            for (Map<String, Object> group : groups) {
                annotated.add(((Number) group.get("image_id")).longValue());
                long labelId = ((Number) group.get("label_id")).longValue();
                long count = byImage ? 1L : ((Number) group.get("instance_count")).longValue();
                counts.seeds.merge(labelId, count, Long::sum);
            }
            counts.unlabeled += (int) usable.stream().filter(SmartAnnotationService::imageForPrediction)
                .filter(sample -> !annotated.contains(sample.getId())).count();
        }
    }

    private static class InputCounts {
        private final Map<Long, Long> seeds = new HashMap<>();
        private int unlabeled;
    }

    public List<LlmAnnotationProviderService.ProviderOption> llmProviders() {
        return llmProvider.listProviders(data.tenant());
    }

    public List<String> llmModels(Long providerId) {
        return llmProvider.listModels(data.tenant(), providerId);
    }

    @Transactional(rollbackFor = Exception.class)
    public SmartAnnotationTask create(SmartAnnotationRequests.Create request) {
        data.lockDatasetForTask(request.getDatasetId());
        requireDataset(request.getDatasetId());
        requireNoOtherActiveTask(request.getDatasetId(), null);
        if ("active".equals(request.getMode()) && !data.isDatasetGenerated(request.getDatasetId())) {
            throw new ServiceException("主动学习需要已生成的数据集，请先到算法标注页面生成数据集");
        }
        DatasetSnapshot snapshot = data.snapshotForVersion(request.getDatasetId());
        validateInput(snapshot, request.getMode(), true);
        SmartAnnotationTask task = new SmartAnnotationTask();
        task.setTenantId(data.tenant()); task.setDatasetId(request.getDatasetId()); task.setTaskName(request.getTaskName());
        task.setMode(request.getMode()); task.setSourceType(request.getSourceType()); task.setSourceId(request.getSourceId());
        task.setAnnotationType(snapshot.getAnnotationType());
        boolean llm = "llm".equals(task.getMode());
        if (llm) {
            if (request.getProviderId() == null) throw new ServiceException("请选择已授权的视觉大模型提供商");
            if (request.getComputeNodeId() != null) throw new ServiceException("远程大模型标注无需选择 GPU 算力");
            LlmAnnotationProviderService.Binding binding = llmProvider.bind(data.tenant(), request.getProviderId(), request.getModelName());
            task.setSourceType("llm"); task.setSourceId(request.getProviderId());
            task.setModelName(binding.getModelName()); task.setModelPath("");
            task.setLlmProviderFingerprint(binding.getProviderFingerprint());
            task.setLlmPrompt(request.getPrompt() == null ? "" : request.getPrompt().trim());
            task.setLlmPromptVersion(LlmAnnotationAdapter.PROMPT_VERSION);
        } else {
            task.setComputeNodeId(request.getComputeNodeId());
            if (task.getComputeNodeId() == null) throw new ServiceException("请选择执行智能标注的 GPU 算力节点");
            if (task.getSourceType() == null || task.getSourceId() == null) throw new ServiceException("请选择模型来源");
            resolveSource(task);
        }
        task.setConfidence(request.getConfidence() == null ? (llm ? 0.50 : 0.25) : request.getConfidence());
        task.setEpochs(request.getEpochs()); task.setReviewSize(request.getReviewSize());
        task.setRoundNumber(1); task.setTaskState("QUEUED"); tasks.insert(task);
        enqueueRound(task, snapshot);
        return task;
    }

    private void requireNoOtherActiveTask(Long datasetId, Long currentId) {
        if (tasks.selectCount(this.<SmartAnnotationTask>scope().eq("dataset_id", datasetId)
            .ne(currentId != null, "id", currentId).in("task_state", ACTIVE_TASK_STATES)) > 0)
            throw new ServiceException("当前数据集已有进行中的智能标注任务，请先完成或取消");
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
            if (computeAccess!=null) computeAccess.model(task.getSourceId());
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
            requireRoundModelAccess(owner, source);
            task.setModelName(owner.getTaskName() + " 第" + source.getRoundNumber() + "轮"); task.setModelPath(source.getModelPath());
        } else throw new ServiceException("不支持的模型来源");
        if (!pt(task.getModelPath())) throw new ServiceException("请选择具有可用 PT 权重的匹配模型");
    }

    private boolean canUseRoundModel(SmartAnnotationTask task, SmartAnnotationRound round) {
        return task.getComputeNodeId() == null || (compute != null
            && Boolean.TRUE.equals(compute.execution(task, round.getId()).get("canReadDiagnostics")));
    }

    private void requireRoundModelAccess(SmartAnnotationTask task, SmartAnnotationRound round) {
        if (!canUseRoundModel(task, round)) throw new ServiceException("该轮算力模型仅本次执行成员可使用", 403);
    }

    private boolean compatible(AnnotationTaskType type, Algorithm algorithm) {
        return algorithm == null || algorithm.getCategory() == null || type.getModelTask().equals(algorithm.getCategory().getCode());
    }

    private boolean pt(String path) { return path != null && (path.startsWith("/") || path.startsWith("cloud-training/")) && path.endsWith(".pt") && !path.contains("/../"); }

    private AnnotationTaskType requireType(String annotationType, List<AnnotationLabel> labels) {
        AnnotationTaskType type = AnnotationTaskType.of(annotationType);
        if (labels.isEmpty()) throw new ServiceException("请先在标注页面创建需要识别的标签");
        return type;
    }

    private static boolean countSeedsByImage(AnnotationTaskType type) {
        return type == AnnotationTaskType.CLASSIFICATION || type == AnnotationTaskType.SEMANTIC_SEGMENTATION;
    }

    private void validateInput(DatasetSnapshot snapshot, String mode, boolean first) {
        AnnotationTaskType type = requireType(snapshot.getAnnotationType(), snapshot.getLabels());
        int count = unannotated(snapshot).size();
        if (count == 0) throw new ServiceException("没有可预标注的未标注图片");
        if (!"active".equals(mode)) return;
        if (first && count < minUnlabeled) throw new ServiceException("主动学习至少需要 " + minUnlabeled + " 张未标注图片");
        Map<Long, Long> counts = seedCounts(snapshot, type);
        for (AnnotationLabel label : snapshot.getLabels()) if (counts.getOrDefault(label.getId(), 0L) < minBoxes)
            throw new ServiceException("主动学习需要每个标签至少 " + minBoxes + " 个已确认样本/实例：" + label.getName());
        if (snapshot.getInstances().stream().map(AnnotationInstance::getImageId).distinct().count() < 2)
            throw new ServiceException("主动学习至少需要两张不同的已标注图片");
    }

    public static List<AnnotationImage> unannotated(DatasetSnapshot snapshot) {
        Set<Long> annotated = snapshot.getInstances().stream().map(AnnotationInstance::getImageId).collect(Collectors.toSet());
        return snapshot.getSamples().stream().filter(SmartAnnotationService::imageForPrediction)
            .filter(s -> !annotated.contains(s.getId())).filter(DatasetPartitioner::usable)
            .collect(Collectors.toList());
    }

    private static boolean imageForPrediction(AnnotationImage sample) {
        return sample.getMediaType() == null || "image".equals(sample.getMediaType());
    }

    private Map<Long, Long> seedCounts(DatasetSnapshot snapshot, AnnotationTaskType type) {
        Set<Long> usable = snapshot.getSamples().stream().filter(DatasetPartitioner::usable).map(AnnotationImage::getId).collect(Collectors.toSet());
        List<AnnotationInstance> valid = snapshot.getInstances().stream().filter(instance -> usable.contains(instance.getImageId())).collect(Collectors.toList());
        if (countSeedsByImage(type)) {
            Map<Long, Set<Long>> images = new HashMap<>();
            valid.forEach(instance -> images.computeIfAbsent(instance.getLabelId(), ignored -> new HashSet<>()).add(instance.getImageId()));
            return images.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> (long) entry.getValue().size()));
        }
        return valid.stream().collect(Collectors.groupingBy(AnnotationInstance::getLabelId, Collectors.counting()));
    }

    private void enqueueRound(SmartAnnotationTask task, DatasetSnapshot snapshot) {
        String name = "智能标注 · " + shortName(task.getTaskName()) + " · 第" + task.getRoundNumber() + "轮";
        DatasetVersion version = data.saveVersion(task.getDatasetId(), name, "本轮输入快照。预标注结果经自动或人工确认后进入正式标注", snapshot);
        SmartAnnotationRound round = new SmartAnnotationRound(); round.setTenantId(data.tenant());
        round.setTaskId(task.getId()); round.setRoundNumber(task.getRoundNumber()); round.setVersionId(version.getId());
        round.setRoundState("QUEUED"); round.setPredictedCount(0);
        if ("llm".equals(task.getMode())) {
            List<AnnotationImage> images = unannotated(snapshot);
            round.setProgressStage("llm"); round.setProgressCurrent(0); round.setProgressTotal(images.size()); round.setLlmFailedCount(0);
            rounds.insert(round);
            jdbc.batchUpdate("INSERT INTO vls_smart_annotation_candidate "
                    + "(id, tenant_id, task_id, round_id, image_id, image_width, image_height, boxes_json, summary_json, uncertainty, review_state) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, '[]', '[]', 1, 'QUEUED')",
                images, 200, (statement, image) -> {
                    statement.setLong(1, IdWorker.getId());
                    statement.setString(2, task.getTenantId());
                    statement.setLong(3, task.getId());
                    statement.setLong(4, round.getId());
                    statement.setLong(5, image.getId());
                    statement.setInt(6, image.getMediaWidth() == null ? 0 : image.getMediaWidth());
                    statement.setInt(7, image.getMediaHeight() == null ? 0 : image.getMediaHeight());
                });
        } else {
            rounds.insert(round);
            enqueue(task, round, snapshot);
        }
    }

    private void enqueue(SmartAnnotationTask task, SmartAnnotationRound round, DatasetSnapshot snapshot) {
        if (task.getComputeNodeId() == null) scheduler.enqueueSmartAnnotation(round.getId());
        else compute.enqueue(task, round, snapshot, LoginHelper.getUserId());
    }

    public Map<String, Object> detail(Long id) {
        SmartAnnotationTask task = task(id); SmartAnnotationRound round = current(task);
        Map<String, Long> counts = candidates.selectMaps(this.<SmartAnnotationCandidate>scope().eq("round_id", round.getId())
            .select("review_state", "COUNT(*) AS state_count").groupBy("review_state")).stream()
            .collect(Collectors.toMap(row -> (String) row.get("review_state"), row -> ((Number) row.get("state_count")).longValue()));
        long pendingHard = candidates.selectCount(this.<SmartAnnotationCandidate>scope().eq("round_id", round.getId())
            .eq("hard_example", 1).eq("review_state", "PENDING"));
        Map<String, Object> execution = task.getComputeNodeId() == null ? Collections.emptyMap() : compute.execution(task, round.getId());
        boolean canReadDiagnostics = task.getComputeNodeId() == null ? isCreator(task) : Boolean.TRUE.equals(execution.get("canReadDiagnostics"));
        List<SmartAnnotationRound> history = rounds.selectList(this.<SmartAnnotationRound>scope().eq("task_id", id).orderByAsc("round_number"))
            .stream().map(item -> roundView(task, item, canReadDiagnostics && Objects.equals(item.getId(), round.getId()))).collect(Collectors.toList());
        return DataManagementService.map("task", taskView(task, canReadDiagnostics), "round", roundView(task, round, canReadDiagnostics), "rounds", history,
            "counts", counts, "pendingHard", pendingHard,
            "labels", data.datasetLabels(task.getDatasetId()), "maxRounds", maxRounds,
            "canUseRoundModel", task.getComputeNodeId() == null || canReadDiagnostics,
            "execution", execution);
    }

    // GPU output can contain private execution paths and application diagnostics.
    private SmartAnnotationTask taskView(SmartAnnotationTask task, boolean canReadDiagnostics) {
        if (canReadDiagnostics || "llm".equals(task.getMode())) return task;
        SmartAnnotationTask view = new SmartAnnotationTask();
        BeanUtils.copyProperties(task, view);
        view.setComputeNodeId(null);
        if (task.getErrorMessage() != null) view.setErrorMessage("本轮算力执行异常，请联系执行成员核对");
        return view;
    }

    private SmartAnnotationRound roundView(SmartAnnotationTask task, SmartAnnotationRound round, boolean canReadDiagnostics) {
        if (canReadDiagnostics || "llm".equals(task.getMode()) || round.getErrorMessage() == null) return round;
        SmartAnnotationRound view = new SmartAnnotationRound();
        BeanUtils.copyProperties(round, view);
        view.setErrorMessage("本轮算力执行异常，请联系执行成员核对");
        return view;
    }

    boolean isCreator(SmartAnnotationTask task) {
        String userId = LoginHelper.getUserId();
        return userId != null && userId.equals(task.getCreateUser());
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
            Map<String, Object> quality = predictionQuality(task, c);
            return DataManagementService.map("candidate", c, "imageName", sample.getImageName(), "width", c.getImageWidth(), "height", c.getImageHeight(), "previewUrl", storage.preview(sample.getLocalPath()), "predictionQuality", quality);
        }).collect(Collectors.toList());
        return DataManagementService.map("records", rows, "total", result.getTotal());
    }

    public Map<String, Object> prediction(Long id, Long candidateId) {
        SmartAnnotationTask task = task(id);
        SmartAnnotationCandidate candidate = currentCandidate(task, candidateId);
        AnnotationImage image = data.sample(task.getDatasetId(), candidate.getImageId());
        Map<String, Object> quality = predictionQuality(task, candidate);
        return DataManagementService.map("candidate", candidate, "imageName", image.getImageName(), "width", candidate.getImageWidth(), "height", candidate.getImageHeight(),
            "previewUrl", storage.preview(image.getLocalPath()), "annotationType", task.getAnnotationType(), "predictionQuality", quality);
    }

    public InputStream predictionImage(Long id, Long candidateId) {
        SmartAnnotationTask task = task(id);
        SmartAnnotationCandidate candidate = currentCandidate(task, candidateId);
        AnnotationImage image = data.sample(task.getDatasetId(), candidate.getImageId());
        if (!"image".equals(image.getMediaType())) throw new ServiceException("图片读取失败");
        return storage.read(image.getLocalPath());
    }

    private SmartAnnotationCandidate currentCandidate(SmartAnnotationTask task, Long candidateId) {
        SmartAnnotationRound round = current(task);
        SmartAnnotationCandidate candidate = candidates.selectOne(this.<SmartAnnotationCandidate>scope()
            .eq("id", candidateId).eq("task_id", task.getId()).eq("round_id", round.getId()));
        if (candidate == null) throw new ServiceException("预标注结果不存在或不属于当前轮次");
        return candidate;
    }

    private Map<String, Object> predictionQuality(SmartAnnotationTask task, SmartAnnotationCandidate candidate) {
        if (!"PENDING".equals(candidate.getReviewState())) return Collections.emptyMap();
        Map<String, Object> quality = new LinkedHashMap<>();
        if ("llm".equals(task.getMode())) quality.put("manualReview", Integer.valueOf(1).equals(candidate.getHardExample())
            || (candidate.getReviewError() != null && !candidate.getReviewError().isEmpty()));
        if (!"object_detection".equals(task.getAnnotationType())) return quality;
        List<SmartAnnotationRequests.Box> original = boxes(candidate.getBoxesJson());
        List<SmartAnnotationRequests.Box> filtered = DetectionPredictionFilter.suppressDuplicates(original);
        candidate.setBoxesJson(data.writeJson(filtered));
        quality.put("suppressedCount", original.size() - filtered.size());
        quality.put("classConflict", DetectionPredictionFilter.hasClassConflict(filtered));
        return quality;
    }

    List<SmartAnnotationRequests.Box> batchReviewBoxes(SmartAnnotationTask task, SmartAnnotationCandidate candidate) {
        boolean llm = "llm".equals(task.getMode());
        if (llm && (Integer.valueOf(1).equals(candidate.getHardExample())
            || (candidate.getReviewError() != null && !candidate.getReviewError().isEmpty())))
            throw new ServiceException("此大模型候选需要逐图复核");
        return confirmationBoxes(task, boxes(candidate.getBoxesJson()), llm);
    }

    List<SmartAnnotationRequests.Box> confirmationBoxes(SmartAnnotationTask task, List<SmartAnnotationRequests.Box> regions, boolean automatic) {
        if (!automatic) regions = deduplicateConfirmationBoxes(task, regions);
        if (regions.isEmpty()) throw new ServiceException("没有预测结果，请逐图检查");
        double threshold = task.getConfidence();
        if (regions.stream().anyMatch(box -> box.getConfidence() == null || !Double.isFinite(box.getConfidence())
            || (automatic ? box.getConfidence() <= threshold : box.getConfidence() < threshold)))
            throw new ServiceException("包含低置信度结果，请逐图检查后确认");
        if (regions.stream().anyMatch(box -> box.getLabelId() == null))
            throw new ServiceException("请为所有目标框选择当前数据集的标签");
        return automatic ? deduplicateConfirmationBoxes(task, regions) : regions;
    }

    private List<SmartAnnotationRequests.Box> deduplicateConfirmationBoxes(SmartAnnotationTask task, List<SmartAnnotationRequests.Box> regions) {
        if (!"object_detection".equals(task.getAnnotationType())) return regions;
        regions = DetectionPredictionFilter.suppressDuplicates(regions);
        if (DetectionPredictionFilter.hasClassConflict(regions))
            throw new ServiceException("不同类别的目标框高度重叠，请逐图检查后确认");
        return regions;
    }

    public DatasetSnapshot snapshot(SmartAnnotationTask task, SmartAnnotationRound round) {
        return data.readSnapshot(data.version(task.getDatasetId(), round.getVersionId()).getSnapshotJson());
    }

    public List<SmartAnnotationRequests.Box> boxes(String value) {
        try { return json.readValue(value, new TypeReference<List<SmartAnnotationRequests.Box>>() { }); }
        catch (Exception ex) { throw new ServiceException("预标注结果无法解析"); }
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void review(Long id, Long candidateId, SmartAnnotationRequests.Review request) {
        SmartAnnotationTask task = task(id); data.lockDatasetForTask(task.getDatasetId()); task = task(id);
        if (!Arrays.asList("REVIEW", "CONFIRMING").contains(task.getTaskState())) throw new ServiceException("当前任务不可确认标注");
        if (!task.getAnnotationType().equals(data.project(task.getDatasetId()).getAnnotationType())) throw new ServiceException("数据集类型已变更，请取消旧任务后重新发起");
        SmartAnnotationCandidate candidate = candidates.selectOne(this.<SmartAnnotationCandidate>scope().eq("id", candidateId)
            .eq("round_id", current(task).getId()).eq("task_id", id));
        if (candidate == null) throw new ServiceException("预标注结果不存在或不属于当前轮次");
        if (!"PENDING".equals(candidate.getReviewState())) return; // Safe retry after a lost HTTP response.
        if (request.isSkip()) { candidate.setReviewState("SKIPPED"); candidates.updateById(candidate); return; }
        AnnotationImage frozen = snapshot(task, current(task)).getSamples().stream()
            .filter(s -> s.getId().equals(candidate.getImageId())).findFirst().orElse(null);
        saveReview(task, candidate, request.getBoxes(), frozen);
    }

    /** Called under the dataset edit lock and within the confirmation transaction. */
    void saveReview(SmartAnnotationTask task, SmartAnnotationCandidate candidate,
                    List<SmartAnnotationRequests.Box> boxes, AnnotationImage frozen) {
        if (!task.getAnnotationType().equals(data.project(task.getDatasetId()).getAnnotationType()))
            throw new ServiceException("数据集类型已变更，请取消旧任务后重新发起");
        AnnotationImage sample = data.sample(task.getDatasetId(), candidate.getImageId());
        if (frozen == null || !frozen.getId().equals(sample.getId())) throw new ServiceException("图片不属于本轮输入");
        if (!Objects.equals(frozen.getLocalPath(), sample.getLocalPath()) || !Objects.equals(frozen.getContentSha256(), sample.getContentSha256())
            || !Objects.equals(frozen.getMediaWidth(), sample.getMediaWidth()) || !Objects.equals(frozen.getMediaHeight(), sample.getMediaHeight()))
            throw new ServiceException("图片内容已变化，请跳过旧结果后重新发起任务");
        if ("llm".equals(task.getMode())) verifyLlmReviewImage(candidate, sample);
        if (instances.selectCount(this.<AnnotationInstance>scope().eq("annotation_id", task.getDatasetId()).eq("image_id", sample.getId())) > 0)
            throw new ServiceException("图片已被人工标注，旧预标注不能覆盖；请跳过此结果");
        AnnotationPayloads.validate(task.getAnnotationType(), boxes, candidate.getImageWidth() == null ? sample.getMediaWidth() : candidate.getImageWidth(), candidate.getImageHeight() == null ? sample.getMediaHeight() : candidate.getImageHeight());
        data.validateAnnotationOwner(task.getDatasetId(), sample.getId(), boxes.stream().map(SmartAnnotationRequests.Box::getLabelId).collect(Collectors.toList()));
        List<AnnotationInstance> confirmed = new ArrayList<>();
        for (SmartAnnotationRequests.Box box : boxes) {
            AnnotationInstance instance = new AnnotationInstance(); instance.setTenantId(data.tenant()); instance.setAnnotationId(task.getDatasetId());
            instance.setImageId(sample.getId()); instance.setLabelId(box.getLabelId()); instance.setAnnotationType(AnnotationPayloads.geometry(task.getAnnotationType()));
            instance.setAnnotationData(data.writeJson(AnnotationPayloads.content(task.getAnnotationType(), box)));
            instance.setVerified(1); instance.setConfidence(BigDecimal.valueOf(box.getConfidence() == null ? 1 : box.getConfidence())); confirmed.add(instance);
        }
        annotationService.batchSaveAnnotations(task.getDatasetId(), sample.getId(), confirmed);
        candidate.setBoxesJson(data.writeJson(boxes)); candidate.setSummaryJson(data.writeJson(AnnotationPayloads.summary(boxes)));
        candidate.setReviewError(""); candidate.setReviewState("ACCEPTED"); candidates.updateById(candidate);
    }

    private void verifyLlmReviewImage(SmartAnnotationCandidate candidate, AnnotationImage sample) {
        String expected = sample.getContentSha256();
        if (org.apache.commons.lang3.StringUtils.isBlank(expected)) {
            try (InputStream input = storage.read(sample.getLocalPath())) {
                expected = org.apache.commons.codec.digest.DigestUtils.sha256Hex(SmartAnnotationWorker.limited(
                    input, com.ruoyi.vlstream.test.vlstream.service.OpenAiVisionClient.MAX_ANNOTATION_IMAGE_BYTES));
            } catch (java.io.IOException | RuntimeException failure) {
                throw new ServiceException("无法核对旧图片内容，请跳过后重新发起任务");
            }
        }
        if (!expected.equalsIgnoreCase(candidate.getLlmInputSha256()))
            throw new ServiceException("图片内容与模型输入不一致，请跳过后重新发起任务");
    }

    void verifyLlmAutoLabels(SmartAnnotationTask task, List<SmartAnnotationRequests.Box> regions) {
        if (!"llm".equals(task.getMode())) return;
        Map<Long, String> current = data.datasetLabels(task.getDatasetId()).stream()
            .collect(Collectors.toMap(AnnotationLabel::getId, AnnotationLabel::getName));
        if (regions.stream().anyMatch(region -> !Objects.equals(region.getClassName(), current.get(region.getLabelId()))))
            throw new ServiceException("数据集标签已变更，请逐图确认当前标签");
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

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void confirmBatch(Long id, List<Long> ids) {
        SmartAnnotationTask task = task(id);
        data.lockDatasetForTask(task.getDatasetId());
        task = task(id);
        for (Long candidateId : new LinkedHashSet<>(ids)) {
            SmartAnnotationCandidate candidate = candidates.selectOne(this.<SmartAnnotationCandidate>scope().eq("id", candidateId).eq("round_id", current(task).getId()));
            if (candidate == null) throw new ServiceException("选中的结果不属于当前轮次");
            if (!"PENDING".equals(candidate.getReviewState())) continue;
            SmartAnnotationRequests.Review request = new SmartAnnotationRequests.Review(); request.setBoxes(batchReviewBoxes(task, candidate));
            review(id, candidateId, request);
        }
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void next(Long id) {
        SmartAnnotationTask task = task(id); data.lockDatasetForTask(task.getDatasetId()); task = task(id);
        if (!"active".equals(task.getMode()) || !"REVIEW".equals(task.getTaskState()) || task.getRoundNumber() >= maxRounds)
            throw new ServiceException("当前任务不能启动下一轮");
        SmartAnnotationRound round = current(task);
        requireRoundModelAccess(task, round);
        if (candidates.selectCount(this.<SmartAnnotationCandidate>scope().eq("round_id", round.getId()).eq("hard_example", 1).eq("review_state", "PENDING")) > 0)
            throw new ServiceException("请先完成本轮难例的确认或跳过");
        if (candidates.selectCount(this.<SmartAnnotationCandidate>scope().eq("round_id", round.getId()).eq("review_state", "ACCEPTED")) == 0)
            throw new ServiceException("本轮尚未增加已确认样本，请先确认标注后继续学习");
        DatasetSnapshot snapshot = data.snapshotForVersion(task.getDatasetId());
        validateInput(snapshot, task.getMode(), false);
        task.setModelPath(round.getModelPath()); task.setRoundNumber(task.getRoundNumber() + 1); task.setTaskState("QUEUED"); tasks.updateById(task);
        enqueueRound(task, snapshot);
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void finish(Long id) {
        SmartAnnotationTask task = task(id); data.lockDatasetForTask(task.getDatasetId()); task = task(id);
        if (!"REVIEW".equals(task.getTaskState())) throw new ServiceException("当前任务不可完成");
        DataRequests.Version version = new DataRequests.Version(); version.setName("智能标注结果 · " + shortName(task.getTaskName()));
        version.setDescription("仅包含已确认标注，未确认预标注不会进入训练"); data.saveVersion(task.getDatasetId(), version);
        task.setTaskState("COMPLETED"); tasks.updateById(task);
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void retry(Long id) {
        SmartAnnotationTask task = task(id); data.lockDatasetForTask(task.getDatasetId()); task = task(id);
        if ("llm".equals(task.getMode())) {
            retryLlm(taskForUpdate(id));
            return;
        }
        if (!"FAILED".equals(task.getTaskState())) throw new ServiceException("只有失败任务可以重试");
        requireNoOtherActiveTask(task.getDatasetId(), task.getId());
        requireRetryModelAccess(task);
        SmartAnnotationRound round = current(task);
        rounds.update(null, new UpdateWrapper<SmartAnnotationRound>().eq("id", round.getId()).eq("tenant_id", data.tenant()).set("round_state", "QUEUED").set("error_message", null));
        tasks.update(null, new UpdateWrapper<SmartAnnotationTask>().eq("id", id).eq("tenant_id", data.tenant()).set("task_state", "QUEUED").set("error_message", null));
        enqueue(task, round, task.getComputeNodeId() == null ? null : snapshot(task, round));
    }

    private void requireRetryModelAccess(SmartAnnotationTask task) {
        if (task.getRoundNumber() > 1) {
            SmartAnnotationRound previous = rounds.selectOne(this.<SmartAnnotationRound>scope()
                .eq("task_id", task.getId()).eq("round_number", task.getRoundNumber() - 1));
            if (previous == null || !Objects.equals(previous.getModelPath(), task.getModelPath()))
                throw new ServiceException("无法确认重试所用的上轮模型");
            requireRoundModelAccess(task, previous);
        } else if ("model".equals(task.getSourceType()) && computeAccess != null) {
            computeAccess.model(task.getSourceId());
        } else if ("round".equals(task.getSourceType())) {
            SmartAnnotationRound source = round(task.getSourceId());
            requireRoundModelAccess(task(source.getTaskId()), source);
        }
    }

    private void retryLlm(SmartAnnotationTask task) {
        if (!Arrays.asList("FAILED", "REVIEW").contains(task.getTaskState())) throw new ServiceException("当前任务不可重试");
        SmartAnnotationRound round = current(task);
        int failed = round.getLlmFailedCount() == null ? 0 : round.getLlmFailedCount();
        if (failed == 0) throw new ServiceException("没有失败图片可重试");
        requireNoOtherActiveTask(task.getDatasetId(), task.getId());
        llmProvider.verifyBinding(data.tenant(), task.getSourceId(), task.getModelName(), task.getLlmProviderFingerprint());
        candidates.update(null, new UpdateWrapper<SmartAnnotationCandidate>().eq("tenant_id", data.tenant()).eq("round_id", round.getId())
            .eq("review_state", "FAILED").set("review_state", "QUEUED").set("review_error", null));
        rounds.update(null, new UpdateWrapper<SmartAnnotationRound>().eq("id", round.getId()).eq("tenant_id", data.tenant())
            .set("round_state", "QUEUED").set("progress_current", round.getProgressCurrent() - failed)
            .set("llm_failed_count", 0).set("error_message", null));
        tasks.update(null, new UpdateWrapper<SmartAnnotationTask>().eq("id", task.getId()).eq("tenant_id", data.tenant())
            .set("task_state", "QUEUED").set("error_message", null).set("bulk_error", null));
    }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void cancel(Long id) {
        SmartAnnotationTask task = task(id);
        data.lockDatasetForTask(task.getDatasetId());
        task = requireTask(this.<SmartAnnotationTask>scope().eq("id", id).eq("dataset_id", task.getDatasetId()));
        if (Arrays.asList("COMPLETED", "CANCELLED", "CANCEL_REQUESTED").contains(task.getTaskState())) return;
        if ("llm".equals(task.getMode())) {
            task = taskForUpdate(id);
            if (Arrays.asList("COMPLETED", "CANCELLED").contains(task.getTaskState())) return;
            SmartAnnotationRound round = current(task);
            tasks.update(null, new UpdateWrapper<SmartAnnotationTask>().eq("id", id).eq("tenant_id", data.tenant()).set("task_state", "CANCELLED"));
            rounds.update(null, new UpdateWrapper<SmartAnnotationRound>().eq("id", round.getId()).eq("tenant_id", data.tenant()).set("round_state", "CANCELLED"));
            candidates.update(null, new UpdateWrapper<SmartAnnotationCandidate>().eq("tenant_id", data.tenant()).eq("round_id", round.getId())
                .in("review_state", "QUEUED", "RUNNING").set("review_state", "CANCELLED")
                .set("llm_claim_token", null).set("llm_claim_until", null));
            return;
        }
        List<String> localStates = Arrays.asList("FAILED", "REVIEW", "CONFIRMING", "AUTO_CONFIRMING");
        if (task.getComputeNodeId() != null && !localStates.contains(task.getTaskState())) {
            compute.cancel(task, current(task).getId());
            task = taskForUpdate(id); // Compute completion may have committed while waiting for the job row.
        }
        if (Arrays.asList("COMPLETED", "CANCELLED").contains(task.getTaskState())) return;
        String state = localStates.contains(task.getTaskState()) ? "CANCELLED" : "CANCEL_REQUESTED";
        task.setTaskState(state); tasks.updateById(task);
    }

    private String shortName(String name) { return name.substring(0, Math.min(70, name.length())); }

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void confirmAll(Long id) {
        SmartAnnotationTask task = task(id); data.lockDatasetForTask(task.getDatasetId()); task = task(id);
        if (Arrays.asList("CONFIRMING", "AUTO_CONFIRMING").contains(task.getTaskState())) return;
        if (!"REVIEW".equals(task.getTaskState())) throw new ServiceException("当前任务不可批量确认");
        QueryWrapper<SmartAnnotationCandidate> pending = this.<SmartAnnotationCandidate>scope().eq("round_id", current(task).getId()).eq("review_state", "PENDING");
        if ("llm".equals(task.getMode())) pending.eq("hard_example", 0).and(q -> q.isNull("review_error").or().eq("review_error", ""));
        int total = candidates.selectCount(pending).intValue();
        if ("llm".equals(task.getMode()) && total == 0) throw new ServiceException("剩余大模型候选需要逐图复核");
        tasks.update(null, new UpdateWrapper<SmartAnnotationTask>().eq("id", id).eq("tenant_id", data.tenant()).set("task_state", "CONFIRMING")
            .set("bulk_cursor", 0).set("bulk_total", total).set("bulk_processed", 0).set("bulk_accepted", 0).set("bulk_conflicts", 0).set("bulk_error", null));
    }
}
