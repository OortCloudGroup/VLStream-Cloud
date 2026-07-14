/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.domain.AlgorithmModel;

import java.util.List;
import java.util.Map;

public interface IVlsAlgorithmModelService {

    BladePage<AlgorithmModel> getModelPage(Long current, Long size, String modelName, Long algorithmId,
                                           Long trainingId, String status, String createdTimeBegin,
                                           String createdTimeEnd);

    AlgorithmModel getModelById(Long id);

    AlgorithmModel createModel(AlgorithmModel model);

    AlgorithmModel updateModel(AlgorithmModel model);

    boolean deleteModel(Long id);

    boolean deleteModels(List<Long> ids);

    List<AlgorithmModel> getModelsByAlgorithmId(Long algorithmId);

    List<AlgorithmModel> getModelsByTrainingId(Long trainingId);

    List<AlgorithmModel> getModelsByStatus(String status);

    boolean publishModel(Long id);

    boolean unpublishModel(Long id);

    boolean publishModels(List<Long> ids);

    String downloadModel(Long id);

    boolean deployModel(Long id);

    Map<String, Object> getModelStatistics();

    boolean checkModelNameAndVersion(String modelName, Integer version, Long excludeId);

    AlgorithmModel getModelByAlgorithmIdAndVersion(Long algorithmId, Integer version);

    AlgorithmModel getLatestModelByAlgorithmId(Long algorithmId);

    List<AlgorithmModel> getPopularModels(Integer limit);

    Long countModelsByCreatedBy(Long createdBy);

    Long getTotalModelSize();
}
