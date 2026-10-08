package com.ruoyi.vlstream.test.vlstream.data;

import com.ruoyi.common.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.IOException;

/** Publishes a portable frozen version to object storage without connecting to a compute host. */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataTrainingPublisher {
    private final DataManagementService data;
    private final TrainingDatasetArtifactService artifacts;
    private final DatasetGenerationPreflight preflight;

    public boolean publish(Long projectId) {
        DatasetGenerationReport report = generate(projectId);
        if ("BLOCKED".equals(report.getStatus())) throw new ServiceException("数据集检查发现" + report.getErrors().size() + "项问题，请从算法标注页生成并查看问题清单");
        return "READY".equals(report.getStatus());
    }

    public DatasetGenerationReport generate(Long projectId) {
        DatasetGenerationReport report;
        try {
            report = preflight.check(projectId, data.snapshot(projectId));
            if ("BLOCKED".equals(report.getStatus())) return report;
            DatasetVersion version = data.savePreparedTrainingVersion(projectId, report);
            TrainingDatasetArtifact artifact = artifacts.ensure(projectId, version);
            data.recordPublishedDataset(projectId, version.getId(), artifact.getReference());
            report.setStatus("READY"); report.setReference(artifact.getReference());
            return report;
        } catch (IOException e) {
            log.error("训练数据包生成失败 datasetId={}", projectId, e);
            throw new ServiceException("训练数据包保存失败，请检查 MinIO 与样本内容；原有已生成版本未覆盖");
        }
    }
}
