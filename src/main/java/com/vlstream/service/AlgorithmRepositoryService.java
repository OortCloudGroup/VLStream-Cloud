package com.vlstream.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vlstream.entity.AlgorithmRepository;

import java.util.List;

/**
 * Algorithm Repository Service Interface
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
public interface AlgorithmRepositoryService extends IService<AlgorithmRepository> {

    /**
     * Query algorithm repository list with pagination
     * 
     * @param page Pagination parameter
     * @param name Repository name (fuzzy query)
     * @param repositoryType Repository type
     * @param status Status
     * @return Pagination result
     */
    IPage<AlgorithmRepository> selectRepositoryPage(Page<AlgorithmRepository> page, 
                                                   String name, 
                                                   String repositoryType, 
                                                   String status);

    /**
     * Query all enabled algorithm repositories
     * 
     * @return Enabled algorithm repository list
     */
    List<AlgorithmRepository> getEnabledRepositories();

    /**
     * Query algorithm repositories by type
     * 
     * @param repositoryType Repository type
     * @return Algorithm repository list
     */
    List<AlgorithmRepository> getByRepositoryType(String repositoryType);

    /**
     * Create algorithm repository
     * 
     * @param repository Algorithm repository information
     * @return Whether successful
     */
    boolean createRepository(AlgorithmRepository repository);

    /**
     * Update algorithm repository
     * 
     * @param repository Algorithm repository information
     * @return Whether successful
     */
    boolean updateRepository(AlgorithmRepository repository);

    /**
     * Delete algorithm repository
     * 
     * @param id Repository ID
     * @return Whether successful
     */
    boolean deleteRepository(Long id);

    /**
     * Batch delete algorithm repositories
     * 
     * @param ids Repository ID list
     * @return Whether successful
     */
    boolean batchDeleteRepositories(List<Long> ids);

    /**
     * Update repository status
     * 
     * @param id Repository ID
     * @param status New status
     * @return Whether successful
     */
    boolean updateRepositoryStatus(Long id, String status);

    /**
     * Batch update repository status
     * 
     * @param ids Repository ID list
     * @param status New status
     * @return Whether successful
     */
    boolean batchUpdateRepositoryStatus(List<Long> ids, String status);

    /**
     * Count algorithm repositories
     * 
     * @return Total repository count
     */
    Long countRepositories();

    /**
     * Update algorithm count for repository
     * 
     * @param repositoryId Repository ID
     */
    void updateAlgorithmCount(Long repositoryId);
} 