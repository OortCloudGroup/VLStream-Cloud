package com.vlstream.service.impl;

import com.vlstream.entity.AlgorithmTraining;
import com.vlstream.mapper.AlgorithmTrainingMapper;
import com.vlstream.service.AlgorithmTrainingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Algorithm Training Task Service Implementation Class
 */
@Service
public class AlgorithmTrainingServiceImpl implements AlgorithmTrainingService {
    
    @Autowired
    private AlgorithmTrainingMapper algorithmTrainingMapper;

    /**
     * Query algorithm training task
     * 
     * @param id Algorithm training task ID
     * @return Algorithm training task
     */
    @Override
    public AlgorithmTraining selectAlgorithmTrainingById(Long id) {
        return algorithmTrainingMapper.selectAlgorithmTrainingById(id);
    }
    
    /**
     * Query algorithm training task list
     * 
     * @param algorithmTraining Algorithm training task
     * @return Algorithm training task list
     */
    @Override
    public List<AlgorithmTraining> selectAlgorithmTrainingList(AlgorithmTraining algorithmTraining) {
        return algorithmTrainingMapper.selectAlgorithmTrainingList(algorithmTraining);
    }
    
    /**
     * Add algorithm training task
     * 
     * @param algorithmTraining Algorithm training task
     * @return Result
     */
    @Override
    public int insertAlgorithmTraining(AlgorithmTraining algorithmTraining) {
        return algorithmTrainingMapper.insertAlgorithmTraining(algorithmTraining);
    }
    
    /**
     * Update algorithm training task
     * 
     * @param algorithmTraining Algorithm training task
     * @return Result
     */
    @Override
    public int updateAlgorithmTraining(AlgorithmTraining algorithmTraining) {
        return algorithmTrainingMapper.updateAlgorithmTraining(algorithmTraining);
    }
    
    /**
     * Batch delete algorithm training tasks
     * 
     * @param ids Algorithm training task IDs to delete
     * @return Result
     */
    @Override
    public int deleteAlgorithmTrainingByIds(Long[] ids) {
        return algorithmTrainingMapper.deleteAlgorithmTrainingByIds(ids);
    }
    
    /**
     * Delete algorithm training task
     * 
     * @param id Algorithm training task ID
     * @return Result
     */
    @Override
    public int deleteAlgorithmTrainingById(Long id) {
        return algorithmTrainingMapper.deleteAlgorithmTrainingById(id);
    }
} 