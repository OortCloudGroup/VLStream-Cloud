/*
 * SPDX-FileCopyrightText: 2026 奥尔特云（深圳）智慧科技有限公司
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.service;

import com.ruoyi.vlstream.compat.BladePage;
import com.ruoyi.vlstream.domain.AlgorithmRepository;

import java.util.List;

public interface IVlsAlgorithmRepositoryService {

    BladePage<AlgorithmRepository> getRepositoryPage(Long current, Long size, String name, String repositoryType, String status);

    List<AlgorithmRepository> getEnabledRepositories();

    List<AlgorithmRepository> getRepositoriesByType(String repositoryType);

    AlgorithmRepository getRepositoryById(Long id);

    AlgorithmRepository createRepository(AlgorithmRepository repository);

    AlgorithmRepository updateRepository(Long id, AlgorithmRepository repository);

    boolean deleteRepository(Long id);

    boolean deleteRepositories(List<Long> ids);

    boolean updateRepositoryStatus(Long id, String status);

    boolean updateRepositoryStatus(List<Long> ids, String status);

    Long countRepositories();

    boolean refreshAlgorithmCount(Long id);
}
