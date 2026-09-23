package com.ruoyi.vlstream.test.vlstream.data;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.config.VlsSshProperties;
import com.ruoyi.vlstream.test.vlstream.config.VlsTrainingContainerProperties;
import com.ruoyi.vlstream.test.vlstream.mapper.*;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/** Called only by the existing exclusive GPU scheduler, with the task tenant restored. */
@Service
@RequiredArgsConstructor
public class SmartAnnotationWorker {
    private final SmartAnnotationService service;
    private final SmartAnnotationTaskMapper tasks;
    private final SmartAnnotationRoundMapper rounds;
    private final SmartAnnotationCandidateMapper candidates;
    private final DataManagementService data;
    private final DataMediaStorage storage;
    private final SampleMediaInspector inspector;
    private final YoloDatasetWriter writer;
    private final VlsSshProperties ssh;
    private final VlsTrainingContainerProperties properties;
    private final VlsRemoteServersMapper servers;
    private final ObjectMapper json;

    public String prepare(ContainerInstance instance) {
        SmartAnnotationRound round = service.round(instance.getTrainingTaskId());
        SmartAnnotationTask task = service.task(round.getTaskId());
        if (!"QUEUED".equals(task.getTaskState())) throw new ServiceException("智能标注任务不在队列中");
        state(task, round, "PREPARING", null);
        RemoteServers server = servers.selectActiveServer();
        if (server == null) throw new ServiceException("未配置GPU训练服务器");
        String root = properties.getHostDataDir() + "/vls-smart-annotation/" + task.getId() + "/" + round.getId() + "_" + UUID.randomUUID().toString().replace("-", "");
        String preset = "@preset/" + AnnotationTaskType.of(task.getAnnotationType()).getModelTask();
        if (!preset.equals(task.getModelPath()) && !task.getModelPath().startsWith(properties.getHostDataDir() + "/")) throw new ServiceException("模型不在训练容器挂载的数据目录内");
        round.setWorkDirectory(root); rounds.updateById(round);
        DatasetSnapshot snapshot = service.snapshot(task, round);
        Map<Long, List<AnnotationInstance>> annotations = snapshot.getInstances().stream().collect(Collectors.groupingBy(AnnotationInstance::getImageId));
        List<AnnotationImage> seeds = snapshot.getSamples().stream().filter(DatasetPartitioner::usable).filter(s -> annotations.containsKey(s.getId())).collect(Collectors.toList());
        List<AnnotationImage> pending = SmartAnnotationService.unannotated(snapshot);
        Map<Long, String> split = "active".equals(task.getMode()) ? partition(seeds) : Collections.emptyMap();
        if ("active".equals(task.getMode()) && "image_classification".equals(task.getAnnotationType()) && seeds.stream().noneMatch(sample -> "val".equals(sample.getDatasetSplit()))) {
            DataRequests.Split request = new DataRequests.Split(); request.setMode("stratified");
            Map<Long, String> categories = new HashMap<>(); annotations.forEach((id, values) -> { if (!values.isEmpty()) categories.put(id, String.valueOf(values.get(0).getLabelId())); });
            split = DatasetPartitioner.partition(seeds, categories, request);
        }
        TrainingDatasetLayout layout = new TrainingDatasetLayout(snapshot.getAnnotationType(), snapshot.getLabels());
        List<Map<String, Object>> images = new ArrayList<>();
        List<Map<String, Object>> manifestSamples = new ArrayList<>();
        List<AnnotationImage> upload = new ArrayList<>(pending);
        if ("active".equals(task.getMode())) upload.addAll(seeds);
        try (Connection connection = connect()) {
            ChannelSftp sftp = connection.channel;
            for (String folder : layout.directories()) mkdir(sftp, root + "/" + folder);
            Set<String> trainHashes = new HashSet<>(); Set<String> valHashes = new HashSet<>();
            for (AnnotationImage sample : upload) {
                byte[] bytes;
                try (InputStream input = storage.read(sample.getLocalPath())) { bytes = limited(input, 25 * 1024 * 1024); }
                SampleMediaInspector.Inspection inspection = inspector.inspect(sample.getOriginalName(), bytes);
                if (DataManagementService.hasText(sample.getContentSha256()) && !sample.getContentSha256().equals(inspection.getSha256())) throw new ServiceException("图片内容与输入快照不一致：" + sample.getImageName());
                if ((sample.getMediaWidth() != null && sample.getMediaWidth() > 0 && !Objects.equals(sample.getMediaWidth(), inspection.getWidth())) || (sample.getMediaHeight() != null && sample.getMediaHeight() > 0 && !Objects.equals(sample.getMediaHeight(), inspection.getHeight()))) throw new ServiceException("图片尺寸已变化，请重新校验样本：" + sample.getImageName());
                String subset = split.getOrDefault(sample.getId(), "predict");
                if ("train".equals(subset)) trainHashes.add(inspection.getSha256());
                if ("val".equals(subset)) valHashes.add(inspection.getSha256());
                String extension = sample.getOriginalName().substring(sample.getOriginalName().lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
                if (!Arrays.asList("jpg", "jpeg", "png", "bmp").contains(extension)) throw new ServiceException("不支持的图片格式");
                String path = layout.imagePath(sample.getId(), extension, subset, annotations.get(sample.getId()));
                sftp.put(new ByteArrayInputStream(bytes), root + "/" + path);
                if ("predict".equals(subset)) images.add(DataManagementService.map("id", sample.getId().toString(), "path", path));
                else for (Map.Entry<String, byte[]> file : layout.annotations(sample.getId(), subset, annotations.get(sample.getId()), inspection.getWidth(), inspection.getHeight()).entrySet())
                    sftp.put(new ByteArrayInputStream(file.getValue()), root + "/" + file.getKey());
                manifestSamples.add(layout.sample(sample.getId(), path, subset, inspection.getWidth(), inspection.getHeight(), inspection.getSha256()));
            }
            if (!Collections.disjoint(trainHashes, valHashes)) throw new ServiceException("训练与验证图片内容重复，请先整理样本");
            put(sftp, root + "/dataset.yaml", layout.yaml(root));
            put(sftp, root + "/vls-dataset.json", layout.manifest(task.getDatasetId(), data.tenant(), manifestSamples));
            put(sftp, root + "/config.json", data.writeJson(DataManagementService.map("model", task.getModelPath(), "mode", task.getMode(), "epochs", task.getEpochs(),
                "annotationType", task.getAnnotationType(), "confidence", task.getConfidence(), "workers", properties.getWorkers(), "images", images,
                "modelCache", properties.getHostDataDir() + "/vls-model-cache")));
            try (InputStream runner = new ClassPathResource("smart-annotation/runner.py").getInputStream()) { sftp.put(runner, root + "/runner.py"); }
            try (InputStream runtime = new ClassPathResource("training/four_task_runtime.py").getInputStream()) { sftp.put(runtime, root + "/four_task_runtime.py"); }
        } catch (ServiceException ex) { throw ex; }
        catch (Exception ex) { throw new ServiceException("准备智能标注失败，请检查对象存储、图片和GPU服务器连接"); }
        String python = "/data/work/anaconda3/envs/" + server.getCondaEnv() + "/bin/python";
        String run = quote(python) + " " + quote(root + "/runner.py") + " " + quote(root + "/config.json") + " > " + quote(root + "/run.log") + " 2>&1";
        return "docker run -d --name " + quote(instance.getInstanceName()) + " --gpus device=" + properties.getGpuIndex()
            + " --cpus " + quote(properties.getCpuLimit()) + " --memory " + quote(properties.getMemoryLimit()) + " --shm-size " + quote(properties.getShmSize())
            + " --user $(id -u):$(id -g) -e HOME=/tmp -e YOLO_CONFIG_DIR=" + quote(properties.getUltralyticsConfigDir())
            + " -v " + quote(properties.getHostDataDir() + ":" + properties.getHostDataDir()) + " -w " + quote(root)
            + " " + quote(properties.getImage()) + " /bin/bash -lc " + quote(run);
    }

    /** Respect an existing holdout; otherwise create a deterministic local split without changing the dataset. */
    static Map<Long, String> partition(List<AnnotationImage> samples) {
        List<Long> validation = samples.stream().filter(s -> "val".equals(s.getDatasetSplit())).map(AnnotationImage::getId).collect(Collectors.toList());
        DataRequests.Split request = new DataRequests.Split();
        if (!validation.isEmpty()) { request.setMode("manual"); request.setValidationIds(validation); }
        return DatasetPartitioner.partition(samples, Collections.emptyMap(), request);
    }

    @Transactional(rollbackFor = Exception.class)
    public void started(ContainerInstance instance) {
        SmartAnnotationRound round = service.round(instance.getTrainingTaskId()); state(service.task(round.getTaskId()), round, "RUNNING", null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void complete(ContainerInstance instance) {
        SmartAnnotationRound round = service.round(instance.getTrainingTaskId()); SmartAnnotationTask task = service.task(round.getTaskId());
        data.lockDatasetForTask(task.getDatasetId());
        task = service.task(task.getId());
        if ("CANCEL_REQUESTED".equals(task.getTaskState())) throw new ServiceException("任务已请求取消");
        if ("REVIEW".equals(round.getRoundState())) return;
        JsonNode result;
        try (Connection connection = connect(); InputStream input = connection.channel.get(round.getWorkDirectory() + "/results.json")) {
            result = json.readTree(limited(input, 64 * 1024 * 1024));
        } catch (Exception ex) { throw new ServiceException("读取智能标注结果失败，请检查本轮日志后重试"); }
        String modelPath = result.path("modelPath").asText(); String sha = result.path("modelSha256").asText();
        String expected = round.getWorkDirectory() + ("active".equals(task.getMode()) ? "/training/weights/best.pt" : "/source.pt");
        if (!expected.equals(modelPath) || !sha.matches("[0-9a-f]{64}")) throw new ServiceException("智能标注模型产物校验失败");
        DatasetSnapshot snapshot = service.snapshot(task, round);
        Map<Long, AnnotationImage> pending = SmartAnnotationService.unannotated(snapshot).stream().collect(Collectors.toMap(AnnotationImage::getId, s -> s));
        Map<String, Long> labelNames = new HashMap<>();
        for (AnnotationLabel label : snapshot.getLabels()) {
            if (labelNames.containsKey(label.getName())) throw new ServiceException("数据集含重名标签，请修正后重试");
            labelNames.put(label.getName(), label.getId());
        }
        List<SmartAnnotationCandidate> parsed = new ArrayList<>(); Set<Long> seen = new HashSet<>();
        if (!result.path("predictions").isArray()) throw new ServiceException("缺少推理结果");
        for (JsonNode row : result.path("predictions")) {
            long imageId;
            try { imageId = Long.parseLong(row.path("imageId").asText()); } catch (Exception ex) { throw new ServiceException("推理结果图片ID无效"); }
            AnnotationImage image = pending.get(imageId);
            if (image == null || !seen.add(imageId)) throw new ServiceException("推理结果包含未知或重复图片");
            int width = row.path("width").asInt(0), height = row.path("height").asInt(0);
            if (width <= 0 || height <= 0 || (image.getMediaWidth() != null && image.getMediaWidth() > 0 && image.getMediaWidth() != width)
                || (image.getMediaHeight() != null && image.getMediaHeight() > 0 && image.getMediaHeight() != height)) throw new ServiceException("推理结果图片尺寸不一致");
            List<SmartAnnotationRequests.Box> boxes = service.boxes(row.path("boxes").toString());
            for (SmartAnnotationRequests.Box box : boxes) {
                // Unknown model classes stay unassigned and require a user choice, never an index-based guess.
                Long labelId = labelNames.get(box.getClassName()); box.setLabelId(labelId == null ? 1L : labelId);
                if (box.getConfidence() == null) throw new ServiceException("预测框缺少置信度");
            }
            if (!boxes.isEmpty() || AnnotationTaskType.of(task.getAnnotationType()) == AnnotationTaskType.CLASSIFICATION || AnnotationTaskType.of(task.getAnnotationType()) == AnnotationTaskType.SEMANTIC_SEGMENTATION)
                AnnotationPayloads.validate(task.getAnnotationType(), boxes, width, height);
            for (SmartAnnotationRequests.Box box : boxes) box.setLabelId(labelNames.get(box.getClassName()));
            if (boxes.size() > 1000) throw new ServiceException("单图预测框数超过限制");
            SmartAnnotationCandidate candidate = new SmartAnnotationCandidate(); candidate.setTenantId(data.tenant()); candidate.setTaskId(task.getId());
            candidate.setRoundId(round.getId()); candidate.setImageId(imageId); candidate.setBoxesJson(data.writeJson(boxes)); candidate.setReviewState("PENDING");
            candidate.setSummaryJson(data.writeJson(AnnotationPayloads.summary(boxes)));
            candidate.setImageWidth(width); candidate.setImageHeight(height);
            candidate.setUncertainty(boxes.isEmpty() ? 1 : 1 - boxes.stream().mapToDouble(b -> b.getConfidence()).average().orElse(0));
            candidate.setHardExample(0); parsed.add(candidate);
        }
        if (!seen.equals(pending.keySet())) throw new ServiceException("推理结果不完整，不能进入确认阶段");
        parsed.sort(Comparator.comparing(SmartAnnotationCandidate::getUncertainty).reversed().thenComparing(SmartAnnotationCandidate::getImageId));
        for (int i = 0; i < parsed.size(); i++) {
            if ("active".equals(task.getMode()) && i < task.getReviewSize()) parsed.get(i).setHardExample(1);
            candidates.insert(parsed.get(i));
        }
        round.setModelPath(modelPath); round.setModelSha256(sha); round.setPredictedCount(parsed.size()); rounds.updateById(round);
        state(task, round, "REVIEW", null);
    }

    public void refreshProgress(ContainerInstance instance) {
        SmartAnnotationRound round = service.round(instance.getTrainingTaskId());
        if (round.getWorkDirectory() == null) return;
        try (Connection connection = connect(); InputStream input = connection.channel.get(round.getWorkDirectory() + "/progress.json")) {
            JsonNode progress = json.readTree(limited(input, 4096));
            String stage = progress.path("stage").asText(); int current = progress.path("current").asInt(-1), total = progress.path("total").asInt(-1);
            if (!Arrays.asList("TRAINING", "PREDICTING").contains(stage) || current < 0 || total < 1 || current > total) return;
            rounds.update(null, new UpdateWrapper<SmartAnnotationRound>().eq("id", round.getId()).eq("tenant_id", data.tenant())
                .set("progress_stage", stage).set("progress_current", current).set("progress_total", total));
        } catch (Exception ignored) { /* The first progress file appears after the model has loaded. Keep the last known sample. */ }
    }

    @Transactional(rollbackFor = Exception.class)
    public void failed(ContainerInstance instance, String reason) {
        SmartAnnotationRound round = service.round(instance.getTrainingTaskId()); SmartAnnotationTask task = service.task(round.getTaskId());
        data.lockDatasetForTask(task.getDatasetId()); task = service.task(task.getId());
        state(task, round, "CANCEL_REQUESTED".equals(task.getTaskState()) ? "CANCELLED" : "FAILED", reason);
    }

    public boolean cancelRequested(ContainerInstance instance) {
        SmartAnnotationRound round = service.round(instance.getTrainingTaskId());
        return "CANCEL_REQUESTED".equals(service.task(round.getTaskId()).getTaskState());
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelled(ContainerInstance instance) {
        SmartAnnotationRound round = service.round(instance.getTrainingTaskId()); SmartAnnotationTask task = service.task(round.getTaskId());
        data.lockDatasetForTask(task.getDatasetId()); state(task, round, "CANCELLED", null);
    }

    private void state(SmartAnnotationTask task, SmartAnnotationRound round, String state, String error) {
        if (error != null && error.length() > 1000) error = error.substring(0, 1000);
        UpdateWrapper<SmartAnnotationTask> update = new UpdateWrapper<SmartAnnotationTask>().eq("id", task.getId()).eq("tenant_id", data.tenant());
        if (!"CANCELLED".equals(state)) update.ne("task_state", "CANCEL_REQUESTED");
        tasks.update(null, update.set("task_state", state).set("error_message", error));
        rounds.update(null, new UpdateWrapper<SmartAnnotationRound>().eq("id", round.getId()).eq("tenant_id", data.tenant()).set("round_state", state).set("error_message", error));
    }

    public String logs(Long taskId) {
        SmartAnnotationRound round = service.current(service.task(taskId));
        if (round.getWorkDirectory() == null) return "等待GPU资源，尚未准备本轮文件";
        try (Connection connection = connect(); InputStream input = connection.channel.get(round.getWorkDirectory() + "/run.log")) {
            byte[] content = limited(input, 2 * 1024 * 1024);
            String text = new String(content, StandardCharsets.UTF_8); return text.substring(Math.max(0, text.length() - 24000));
        } catch (Exception ex) { throw new ServiceException("本轮日志暂不可用"); }
    }

    static byte[] limited(InputStream input, int maximum) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(); byte[] buffer = new byte[32768]; int read;
        while ((read = input.read(buffer)) != -1) { if (output.size() + read > maximum) throw new IOException("文件超过处理上限"); output.write(buffer, 0, read); }
        return output.toByteArray();
    }

    private Connection connect() throws Exception {
        Session session = new JSch().getSession(ssh.getUsername(), ssh.getHost(), ssh.getPort());
        try {
            session.setPassword(ssh.getPassword()); session.setConfig("StrictHostKeyChecking", "no"); session.connect(30000);
            ChannelSftp channel = (ChannelSftp) session.openChannel("sftp"); channel.connect(30000); return new Connection(session, channel);
        } catch (Exception ex) { session.disconnect(); throw ex; }
    }

    private static final class Connection implements AutoCloseable {
        private final Session session; private final ChannelSftp channel;
        private Connection(Session session, ChannelSftp channel) { this.session = session; this.channel = channel; }
        public void close() { channel.disconnect(); session.disconnect(); }
    }

    private static void mkdir(ChannelSftp sftp, String directory) throws SftpException {
        String path = "";
        for (String part : directory.split("/")) { if (part.isEmpty()) continue; path += "/" + part;
            try { sftp.stat(path); } catch (SftpException ex) { if (ex.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) sftp.mkdir(path); else throw ex; }
        }
    }
    private static void put(ChannelSftp sftp, String path, String text) throws SftpException { sftp.put(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), path); }
    private static String quote(String value) { return "'" + value.replace("'", "'\"'\"'") + "'"; }
}
