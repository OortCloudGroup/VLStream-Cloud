/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.domain.Algorithm;

import java.util.List;
import java.util.Map;

public interface IVlsAlgorithmService {

    BladePage<Algorithm> getAlgorithmPage(Long current, Long size, Long repositoryId, String name,
                                          String category, String type, String deployStatus);

    List<Algorithm> getAlgorithmsByRepositoryId(Long repositoryId);

    List<Algorithm> getAlgorithmsByCategory(String category);

    Algorithm getAlgorithmById(Long id);

    Algorithm createAlgorithm(Algorithm algorithm);

    Algorithm updateAlgorithm(Long id, Algorithm algorithm);

    boolean deleteAlgorithm(Long id);

    boolean deleteAlgorithms(List<Long> ids);

    boolean updateDeployStatus(Long id, String deployStatus);

    boolean updateDeployStatus(List<Long> ids, String deployStatus);

    Map<String, Object> deployAlgorithmToDevices(Long algorithmId, List<Long> deviceIds);

    Map<String, Object> evaluateAlgorithm(Long algorithmId);

    List<Map<String, Object>> getCategoryStatistics();

    List<Map<String, Object>> getTypeStatistics();

    List<Map<String, Object>> getDeployStatusStatistics();

    Long countByRepositoryId(Long repositoryId);
}
