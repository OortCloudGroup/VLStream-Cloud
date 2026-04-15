package com.vlstream.service.impl;

import com.vlstream.entity.AlgorithmTraining;
import com.vlstream.mapper.AlgorithmTrainingMapper;
import com.vlstream.service.AlgorithmTrainingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 算法训练任务Service业务层处理
 */
@Service
public class AlgorithmTrainingServiceImpl implements AlgorithmTrainingService {
    
    @Autowired
    private AlgorithmTrainingMapper algorithmTrainingMapper;

    /**
     * 查询算法训练任务
     * 
     * @param id 算法训练任务主键
     * @return 算法训练任务
     */
    @Override
    public AlgorithmTraining selectAlgorithmTrainingById(Long id) {
        return algorithmTrainingMapper.selectAlgorithmTrainingById(id);
    }
    
    /**
     * 查询算法训练任务列表
     * 
     * @param algorithmTraining 算法训练任务
     * @return 算法训练任务
     */
    @Override
    public List<AlgorithmTraining> selectAlgorithmTrainingList(AlgorithmTraining algorithmTraining) {
        return algorithmTrainingMapper.selectAlgorithmTrainingList(algorithmTraining);
    }
    
    /**
     * 新增算法训练任务
     * 
     * @param algorithmTraining 算法训练任务
     * @return 结果
     */
    @Override
    public int insertAlgorithmTraining(AlgorithmTraining algorithmTraining) {
        return algorithmTrainingMapper.insertAlgorithmTraining(algorithmTraining);
    }
    
    /**
     * 修改算法训练任务
     * 
     * @param algorithmTraining 算法训练任务
     * @return 结果
     */
    @Override
    public int updateAlgorithmTraining(AlgorithmTraining algorithmTraining) {
        return algorithmTrainingMapper.updateAlgorithmTraining(algorithmTraining);
    }
    
    /**
     * 批量删除算法训练任务
     * 
     * @param ids 需要删除的算法训练任务主键
     * @return 结果
     */
    @Override
    public int deleteAlgorithmTrainingByIds(Long[] ids) {
        return algorithmTrainingMapper.deleteAlgorithmTrainingByIds(ids);
    }
    
    /**
     * 删除算法训练任务信息
     * 
     * @param id 算法训练任务主键
     * @return 结果
     */
    @Override
    public int deleteAlgorithmTrainingById(Long id) {
        return algorithmTrainingMapper.deleteAlgorithmTrainingById(id);
    }
} 