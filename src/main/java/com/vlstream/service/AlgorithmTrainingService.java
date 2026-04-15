package com.vlstream.service;

import com.vlstream.entity.AlgorithmTraining;

import java.util.List;

/**
 * 算法训练任务Service接口
 */
public interface AlgorithmTrainingService {
    
    /**
     * 查询算法训练任务
     * 
     * @param id 算法训练任务主键
     * @return 算法训练任务
     */
    public AlgorithmTraining selectAlgorithmTrainingById(Long id);
    
    /**
     * 查询算法训练任务列表
     * 
     * @param algorithmTraining 算法训练任务
     * @return 算法训练任务集合
     */
    public List<AlgorithmTraining> selectAlgorithmTrainingList(AlgorithmTraining algorithmTraining);
    
    /**
     * 新增算法训练任务
     * 
     * @param algorithmTraining 算法训练任务
     * @return 结果
     */
    public int insertAlgorithmTraining(AlgorithmTraining algorithmTraining);
    
    /**
     * 修改算法训练任务
     * 
     * @param algorithmTraining 算法训练任务
     * @return 结果
     */
    public int updateAlgorithmTraining(AlgorithmTraining algorithmTraining);
    
    /**
     * 批量删除算法训练任务
     * 
     * @param ids 需要删除的算法训练任务主键集合
     * @return 结果
     */
    public int deleteAlgorithmTrainingByIds(Long[] ids);
    
    /**
     * 删除算法训练任务信息
     * 
     * @param id 算法训练任务主键
     * @return 结果
     */
    public int deleteAlgorithmTrainingById(Long id);
} 