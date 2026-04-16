package com.vlstream.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vlstream.entity.Algorithm;

import java.util.List;
import java.util.Map;

/**
 * Algorithm Service Interface
 * 
 * @author VLStream Team
 * @since 1.0.0
 */
public interface AlgorithmService extends IService<Algorithm> {

    /**
     * Query algorithm list with pagination
     * 
     * @param page Pagination parameter
     * @param repositoryId Repository ID
     * @param name Algorithm name (fuzzy query)
     * @param category Algorithm category
     * @param deployStatus Deployment status
     * @return Pagination result
     */
    IPage<Algorithm> selectAlgorithmPage(Page<Algorithm> page, 
                                       Long repositoryId, 
                                       String name, 
                                       String category, 
                                       String deployStatus);

    /**
     * Query algorithm list by repository ID
     * 
     * @param repositoryId Repository ID
     * @return Algorithm list
     */
    List<Algorithm> getByRepositoryId(Long repositoryId);

    /**
     * Query algorithm list by category
     * 
     * @param category Algorithm category
     * @return Algorithm list
     */
    List<Algorithm> getByCategory(String category);

    /**
     * Create algorithm
     * 
     * @param algorithm Algorithm information
     * @return Whether successful
     */
    boolean createAlgorithm(Algorithm algorithm);

    /**
     * Update algorithm
     * 
     * @param algorithm Algorithm information
     * @return Whether successful
     */
    boolean updateAlgorithm(Algorithm algorithm);

    /**
     * Delete algorithm
     * 
     * @param id Algorithm ID
     * @return Whether successful
     */
    boolean deleteAlgorithm(Long id);

    /**
     * Batch delete algorithms
     * 
     * @param ids Algorithm ID list
     * @return Whether successful
     */
    boolean batchDeleteAlgorithms(List<Long> ids);

    /**
     * Update deployment status
     * 
     * @param id Algorithm ID
     * @param deployStatus New deployment status
     * @return Whether successful
     */
    boolean updateDeployStatus(Long id, String deployStatus);

    /**
     * Batch update deployment status
     * 
     * @param ids Algorithm ID list
     * @param deployStatus New deployment status
     * @return Whether successful
     */
    boolean batchUpdateDeployStatus(List<Long> ids, String deployStatus);

    /**
     * Deploy algorithm to devices
     * 
     * @param algorithmId Algorithm ID
     * @param deviceIds Device ID list
     * @return Whether successful
     */
    boolean deployAlgorithmToDevices(Long algorithmId, List<Long> deviceIds);

    /**
     * Count algorithms under a repository
     * 
     * @param repositoryId Repository ID
     * @return Algorithm count
     */
    Long countByRepositoryId(Long repositoryId);

    /**
     * Get algorithm category statistics
     * 
     * @return Category statistics
     */
    List<Map<String, Object>> getCategoryStatistics();

    /**
     * Get algorithm type statistics
     * 
     * @return Type statistics
     */
    List<Map<String, Object>> getTypeStatistics();

    /**
     * Get deployment status statistics
     * 
     * @return Deployment status statistics
     */
    List<Map<String, Object>> getDeployStatusStatistics();

    /**
     * Algorithm evaluation
     * 
     * @param algorithmId Algorithm ID
     * @return Evaluation result
     */
    Map<String, Object> evaluateAlgorithm(Long algorithmId);
} 