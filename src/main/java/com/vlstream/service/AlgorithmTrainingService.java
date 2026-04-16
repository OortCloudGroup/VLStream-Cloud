package com.vlstream.service;

import com.vlstream.entity.AlgorithmTraining;

import java.util.List;

/**
 * Algorithm Training Task Service Interface
 */
public interface AlgorithmTrainingService {
    
    /**
     * Query algorithm training task
     * 
     * @param id Algorithm training task primary key
     * @return Algorithm training task
     */
    public AlgorithmTraining selectAlgorithmTrainingById(Long id);
    
    /**
     * Query algorithm training task list
     * 
     * @param algorithmTraining Algorithm training task
     * @return Algorithm training task collection
     */
    public List<AlgorithmTraining> selectAlgorithmTrainingList(AlgorithmTraining algorithmTraining);
    
    /**
     * Add algorithm training task
     * 
     * @param algorithmTraining Algorithm training task
     * @return Result
     */
    public int insertAlgorithmTraining(AlgorithmTraining algorithmTraining);
    
    /**
     * Update algorithm training task
     * 
     * @param algorithmTraining Algorithm training task
     * @return Result
     */
    public int updateAlgorithmTraining(AlgorithmTraining algorithmTraining);
    
    /**
     * Batch delete algorithm training tasks
     * 
     * @param ids Algorithm training task primary key collection to delete
     * @return Result
     */
    public int deleteAlgorithmTrainingByIds(Long[] ids);
    
    /**
     * Delete algorithm training task information
     * 
     * @param id Algorithm training task primary key
     * @return Result
     */
    public int deleteAlgorithmTrainingById(Long id);
} 