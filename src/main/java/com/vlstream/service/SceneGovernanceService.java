package com.vlstream.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.vlstream.entity.SceneGovernance;

import java.util.List;
import java.util.Map;

/**
 * Scene governance service layer interface
 *
 * @author VLStream Team
 * @since 1.0.0
 */
public interface SceneGovernanceService extends IService<SceneGovernance> {

    /**
     * Query scene governance information with pagination
     *
     * @param page      Pagination object
     * @param name      Scene name
     * @param status    Scene status
     * @param startDate Start date
     * @param endDate   End date
     * @return Scene governance information pagination list
     */
    IPage<SceneGovernance> getSceneGovernancePage(Page<SceneGovernance> page,
                                                 String name,
                                                 String status,
                                                 String startDate,
                                                 String endDate);

    /**
     * Query scene governance information by name
     *
     * @param name Scene name
     * @return Scene governance information
     */
    SceneGovernance getByName(String name);

    /**
     * Add scene governance information
     *
     * @param sceneGovernance Scene governance information
     * @return Whether successful
     */
    boolean addSceneGovernance(SceneGovernance sceneGovernance);

    /**
     * Update scene governance information
     *
     * @param sceneGovernance Scene governance information
     * @return Whether successful
     */
    boolean updateSceneGovernance(SceneGovernance sceneGovernance);

    /**
     * Delete scene governance information
     *
     * @param id Scene ID
     * @return Whether successful
     */
    boolean deleteSceneGovernance(Long id);

    /**
     * Batch delete scene governance information
     *
     * @param ids Scene ID list
     * @return Whether successful
     */
    boolean deleteSceneGovernanceBatch(List<Long> ids);

    /**
     * Update scene governance status
     *
     * @param id     Scene ID
     * @param status Status
     * @return Whether successful
     */
    boolean updateSceneGovernanceStatus(Long id, String status);

    /**
     * Batch update scene governance status
     *
     * @param ids    Scene ID list
     * @param status Status
     * @return Whether successful
     */
    boolean updateSceneGovernanceStatusBatch(List<Long> ids, String status);

    /**
     * Get scene governance list by status
     *
     * @param status Scene status
     * @return Scene governance list
     */
    List<SceneGovernance> getSceneGovernancesByStatus(String status);

    /**
     * Get scene governance list by execution type
     *
     * @param executeType Execution type
     * @return Scene governance list
     */
    List<SceneGovernance> getSceneGovernancesByExecuteType(String executeType);

    /**
     * Check if scene name exists
     *
     * @param name Scene name
     * @param id   Scene ID (exclude self when editing)
     * @return Whether exists
     */
    boolean checkSceneNameExists(String name, Long id);

    /**
     * Get scene governance statistics
     *
     * @return Statistics
     */
    Map<String, Object> getSceneGovernanceStatistics();

    /**
     * Get all execution type list
     *
     * @return Execution type list
     */
    List<String> getAllExecuteTypes();

    /**
     * Enable scene governance
     *
     * @param id Scene ID
     * @return Whether successful
     */
    boolean enableSceneGovernance(Long id);

    /**
     * Disable scene governance
     *
     * @param id Scene ID
     * @return Whether successful
     */
    boolean disableSceneGovernance(Long id);

    /**
     * Batch enable scene governance
     *
     * @param ids Scene ID list
     * @return Whether successful
     */
    boolean enableSceneGovernanceBatch(List<Long> ids);

    /**
     * Batch disable scene governance
     *
     * @param ids Scene ID list
     * @return Whether successful
     */
    boolean disableSceneGovernanceBatch(List<Long> ids);

    /**
     * Execute scene governance
     *
     * @param id Scene ID
     * @return Execution result
     */
    Map<String, Object> executeSceneGovernance(Long id);

    /**
     * Validate scene governance configuration
     *
     * @param sceneGovernance Scene governance information
     * @return Validation result
     */
    Map<String, Object> validateSceneGovernance(SceneGovernance sceneGovernance);

    /**
     * Export scene governance information
     *
     * @param sceneIds Scene ID list, export all scenes when empty
     * @return Export data
     */
    List<SceneGovernance> exportSceneGovernances(List<Long> sceneIds);

    /**
     * Batch import scene governance
     *
     * @param sceneGovernanceList Scene governance list
     * @return Import result
     */
    Map<String, Object> batchImportSceneGovernances(List<SceneGovernance> sceneGovernanceList);

    /**
     * Get scene governance execution history
     *
     * @param id Scene ID
     * @return Execution history
     */
    List<Map<String, Object>> getSceneGovernanceExecuteHistory(Long id);

    /**
     * Copy scene governance
     *
     * @param id   Source scene ID
     * @param name New scene name
     * @return Whether successful
     */
    boolean copySceneGovernance(Long id, String name);
} 