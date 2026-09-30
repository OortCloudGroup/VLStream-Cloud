package com.ruoyi.vlstream.test.vlstream.compute;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.vlstream.test.vlstream.data.*;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationImage;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AnnotationInstance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/** A cloud node consumes a verified, frozen MinIO package instead of rebuilding live sample data. */
@Service
@RequiredArgsConstructor
public class CloudTrainingDatasetWriter {
    private final TrainingDatasetArtifactService artifacts;
    private final TrainingDatasetRuntimeService runtime;
    private final DataManagementService data;

    public static List<AnnotationImage> members(DatasetSnapshot snapshot) {
        new TrainingDatasetLayout(snapshot.getAnnotationType(), snapshot.getLabels());
        Set<Long> annotated = snapshot.getInstances().stream().map(AnnotationInstance::getImageId).collect(Collectors.toSet());
        List<AnnotationImage> result = snapshot.getSamples().stream().filter(DatasetPartitioner::usable)
            .filter(image -> annotated.contains(image.getId())).collect(Collectors.toList());
        if (result.stream().anyMatch(image -> !Arrays.asList("train", "val").contains(image.getDatasetSplit()))
            || result.stream().noneMatch(image -> "train".equals(image.getDatasetSplit()))
            || result.stream().noneMatch(image -> "val".equals(image.getDatasetSplit())))
            throw new ServiceException("请先完成标注和训练/验证划分，两个集合均需有效样本");
        Map<String, String> hashes = new HashMap<>();
        for (AnnotationImage image : result) {
            if (org.springframework.util.StringUtils.hasText(image.getContentSha256())) {
                String previous = hashes.putIfAbsent(image.getContentSha256(), image.getDatasetSplit());
                if (previous != null && !previous.equals(image.getDatasetSplit())) throw new ServiceException("相同图片内容出现在训练和验证两个集合中");
            }
        }
        return result;
    }

    public String ensureArtifact(CloudTrainingJob job) throws IOException {
        com.fasterxml.jackson.databind.JsonNode options = new ObjectMapper().readTree(job.getOptionsJson());
        TrainingDatasetArtifact artifact;
        if (options.has("datasetArtifactRef")) {
            artifact = artifacts.require(job.getDatasetId(), options.path("datasetArtifactRef").asText());
        } else {
            artifact = artifacts.ensure(job.getDatasetId(), data.version(job.getDatasetId(), job.getVersionId()));
        }
        if (!job.getVersionId().equals(artifact.getVersionId()) || !job.getAnnotationType().equals(artifact.getAnnotationType()))
            throw new ServiceException("线上训练数据包与本轮冻结版本不一致");
        return artifact.getReference();
    }

    public String upload(CloudTrainingJob job, DatasetSnapshot ignored, ComputeSsh.Connection connection) throws IOException {
        TrainingDatasetArtifact artifact = artifacts.require(job.getDatasetId(), ensureArtifact(job));
        runtime.upload(artifact, job.getRunDir() + "/dataset", connection);
        TrainingDatasetRuntimeService.copyScripts(connection, job.getRunDir(), "four_task_runtime.py",
            "validate_detection_dataset.py", "validate_typed_dataset.py", "cloud_training_runner.py", "check_cloud_environment.py");
        return artifact.getReference();
    }
}
