package com.ruoyi.vlstream.test.vlstream.data;

import com.ruoyi.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.io.IOException;
import java.util.Arrays;

/** Publishes a portable frozen version to object storage without connecting to a compute host. */
@Service
@RequiredArgsConstructor
public class DataTrainingPublisher {
    private final DataManagementService data;
    private final TrainingDatasetArtifactService artifacts;
    private final PlatformTransactionManager transactions;

    public boolean publish(Long projectId) {
        DatasetVersion version = new TransactionTemplate(transactions).execute(tx -> {
            data.lockDatasetForTask(projectId);
            DatasetSnapshot current = data.snapshot(projectId);
            AnnotationTaskType taskType = AnnotationTaskType.of(current.getAnnotationType());
            if (current.getSamples().stream().noneMatch(sample -> Arrays.asList("train", "val").contains(sample.getDatasetSplit()))) {
                DataRequests.Split split = new DataRequests.Split();
                if (taskType == AnnotationTaskType.CLASSIFICATION) split.setMode("stratified");
                data.split(projectId, split);
            }
            DataRequests.Version request = new DataRequests.Version();
            request.setName("MinIO 训练快照");
            request.setDescription("完整图片、标注、类别和训练划分的固定版本");
            return data.saveVersion(projectId, request);
        });
        try {
            TrainingDatasetArtifact artifact = artifacts.ensure(projectId, version);
            data.recordPublishedDataset(projectId, version.getId(), artifact.getReference());
            return true;
        } catch (IOException e) {
            throw new ServiceException("训练数据包保存失败，请检查 MinIO 与样本内容；原有已生成版本未覆盖");
        }
    }
}
