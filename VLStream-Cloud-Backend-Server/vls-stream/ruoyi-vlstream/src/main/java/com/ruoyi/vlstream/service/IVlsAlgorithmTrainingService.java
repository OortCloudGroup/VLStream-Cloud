/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.domain.AlgorithmTraining;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public interface IVlsAlgorithmTrainingService {

    BladePage<AlgorithmTraining> getTrainingPage(Long current, Long size, String taskName, String trainStatus,
                                                 String createdTimeBegin, String createdTimeEnd);

    AlgorithmTraining getTrainingById(Long id);

    AlgorithmTraining createTraining(AlgorithmTraining training);

    AlgorithmTraining updateTraining(Long id, AlgorithmTraining training);

    boolean deleteTraining(Long id);

    boolean deleteTrainings(List<Long> ids);

    Map<String, Object> startTraining(Long id, Integer epochs, Long datasetId, Integer batchSize,
                                      Integer imgSize, String extraParams);

    boolean stopTraining(Long id);

    Map<String, Object> getTrainingLogs(Long id, String logPath, Integer lines);

    Map<String, Object> getTrainingStatus(Long id, String logPath);

    Map<String, Object> convertModel(Long id);

    ResponseEntity<byte[]> downloadModel(String id, String type);
}
