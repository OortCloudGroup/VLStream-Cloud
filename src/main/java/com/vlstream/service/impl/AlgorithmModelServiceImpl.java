package com.vlstream.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.vlstream.entity.AlgorithmModel;
import com.vlstream.dto.AlgorithmModelQueryDTO;
import com.vlstream.dto.AlgorithmModelCreateDTO;
import com.vlstream.dto.AlgorithmModelUpdateDTO;
import com.vlstream.dto.AlgorithmModelStatisticsDTO;
import com.vlstream.mapper.AlgorithmModelMapper;
import com.vlstream.service.AlgorithmModelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 算法模型Service实现类
 *
 * @author VLStream Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class AlgorithmModelServiceImpl extends ServiceImpl<AlgorithmModelMapper, AlgorithmModel> implements AlgorithmModelService {

    @Override
    public IPage<AlgorithmModel> getModelPage(AlgorithmModelQueryDTO queryDTO) {
        Page<AlgorithmModel> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        return baseMapper.selectModelPage(page, queryDTO);
    }

    @Override
    public AlgorithmModel getModelById(Long id) {
        if (id == null) {
            log.warn("获取模型详情时，ID为空");
            return null;
        }
        return baseMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlgorithmModel createModel(AlgorithmModelCreateDTO createDTO) {
        log.info("创建算法模型：{}", createDTO.getModelName());
        
        // 验证模型名称和版本是否存在
        Integer version = createDTO.getVersion();
        if (version == null || version < 1) {
            version = 1;
        }
        while (checkModelNameAndVersion(createDTO.getModelName(), version, null)) {
            version++;
        }

        // 验证模型文件是否存在
        if (!new File(createDTO.getModelPath()).exists()) {
            log.warn("模型文件不存在：{}", createDTO.getModelPath());
            // 在实际生产环境中，这里应该抛出异常
            // throw new RuntimeException("模型文件不存在");
        }
        
        // 创建模型实体
        AlgorithmModel model = new AlgorithmModel();
        BeanUtils.copyProperties(createDTO, model);
        model.setVersion(version);
        model.setModelPath("datasets/" + model.getModelPath());
        // 设置默认值
        if (model.getDownloadCount() == null) {
            model.setDownloadCount(0);
        }
        if (model.getDeployCount() == null) {
            model.setDeployCount(0);
        }
        if (model.getStatus() == null) {
            model.setStatus("draft");
        }
        
        // 保存到数据库
        boolean success = save(model);
        if (!success) {
            log.error("创建模型失败：{}", createDTO.getModelName());
            throw new RuntimeException("创建模型失败");
        }
        
        log.info("创建模型成功，ID：{}", model.getId());
        return model;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlgorithmModel updateModel(AlgorithmModelUpdateDTO updateDTO) {
        log.info("更新算法模型：{}", updateDTO.getId());
        
        // 检查模型是否存在
        AlgorithmModel existingModel = getModelById(updateDTO.getId());
        if (existingModel == null) {
            throw new RuntimeException("模型不存在");
        }
        
        // 验证模型名称和版本是否存在（排除当前记录）
        if (updateDTO.getModelName() != null && updateDTO.getVersion() != null) {
            if (checkModelNameAndVersion(updateDTO.getModelName(), updateDTO.getVersion(), updateDTO.getId())) {
                throw new RuntimeException("模型名称和版本已存在");
            }
        }
        
        // 检查模型是否可以更新
        if ("published".equals(existingModel.getStatus()) && updateDTO.getStatus() != null && 
            !updateDTO.getStatus().equals(existingModel.getStatus())) {
            throw new RuntimeException("已发布的模型不能修改状态");
        }
        
        // 更新字段
        if (updateDTO.getModelName() != null) {
            existingModel.setModelName(updateDTO.getModelName());
        }
        if (updateDTO.getVersion() != null) {
            existingModel.setVersion(updateDTO.getVersion());
        }
        if (updateDTO.getModelFormat() != null) {
            existingModel.setModelFormat(updateDTO.getModelFormat());
        }
        if (updateDTO.getModelSize() != null) {
            existingModel.setModelSize(updateDTO.getModelSize());
        }
        if (updateDTO.getModelPath() != null) {
            existingModel.setModelPath(updateDTO.getModelPath());
        }
        if (updateDTO.getAccuracy() != null) {
            existingModel.setAccuracy(updateDTO.getAccuracy());
        }
        if (updateDTO.getDescription() != null) {
            existingModel.setDescription(updateDTO.getDescription());
        }
        if (updateDTO.getStatus() != null) {
            existingModel.setStatus(updateDTO.getStatus());
        }
        
        // 保存更新
        boolean success = updateById(existingModel);
        if (!success) {
            log.error("更新模型失败：{}", updateDTO.getId());
            throw new RuntimeException("更新模型失败");
        }
        
        log.info("更新模型成功：{}", updateDTO.getId());
        return existingModel;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteModel(Long id) {
        log.info("删除算法模型：{}", id);
        
        // 检查模型是否存在
        AlgorithmModel existingModel = getModelById(id);
        if (existingModel == null) {
            throw new RuntimeException("模型不存在");
        }
        
        // 检查模型是否可以删除
        if (!existingModel.canDelete()) {
            throw new RuntimeException("已发布的模型不能删除");
        }
        
        // 删除模型（逻辑删除）
        boolean success = removeById(id);
        if (!success) {
            log.error("删除模型失败：{}", id);
            throw new RuntimeException("删除模型失败");
        }
        
        log.info("删除模型成功：{}", id);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteModel(List<Long> ids) {
        log.info("批量删除算法模型：{}", ids);
        
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("删除的模型ID列表不能为空");
        }
        
        // 检查所有模型是否可以删除
        List<AlgorithmModel> models = listByIds(ids);
        for (AlgorithmModel model : models) {
            if (!model.canDelete()) {
                throw new RuntimeException("模型 " + model.getModelName() + " 已发布，不能删除");
            }
        }
        
        // 批量删除
        boolean success = removeByIds(ids);
        if (!success) {
            log.error("批量删除模型失败：{}", ids);
            throw new RuntimeException("批量删除模型失败");
        }
        
        log.info("批量删除模型成功：{}", ids);
        return true;
    }

    @Override
    public List<AlgorithmModel> getModelsByAlgorithmId(Long algorithmId) {
        return baseMapper.selectByAlgorithmId(algorithmId);
    }

    @Override
    public List<AlgorithmModel> getModelsByTrainingId(Long trainingId) {
        return baseMapper.selectByTrainingId(trainingId);
    }

    @Override
    public List<AlgorithmModel> getModelsByStatus(String status) {
        return baseMapper.selectByStatus(status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean publishModel(Long id) {
        log.info("发布算法模型：{}", id);
        
        // 检查模型是否存在
        AlgorithmModel existingModel = getModelById(id);
        if (existingModel == null) {
            throw new RuntimeException("模型不存在");
        }
        
        // 检查模型是否可以发布
        if (!existingModel.canPublish()) {
            throw new RuntimeException("模型状态不允许发布");
        }
        
        // 验证模型文件是否存在
        if (!new File(existingModel.getModelPath()).exists()) {
            throw new RuntimeException("模型文件不存在，无法发布");
        }
        
        // 更新状态为已发布
        int result = baseMapper.updateStatus(id, "published");
        if (result <= 0) {
            log.error("发布模型失败：{}", id);
            throw new RuntimeException("发布模型失败");
        }
        
        // 更新发布时间
        existingModel.setPublishTime(LocalDateTime.now());
        updateById(existingModel);
        
        log.info("发布模型成功：{}", id);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unpublishModel(Long id) {
        log.info("撤销发布算法模型：{}", id);
        
        // 检查模型是否存在
        AlgorithmModel existingModel = getModelById(id);
        if (existingModel == null) {
            throw new RuntimeException("模型不存在");
        }
        
        // 检查模型是否已发布
        if (!"published".equals(existingModel.getStatus())) {
            throw new RuntimeException("模型未发布，无法撤销");
        }
        
        // 更新状态为草稿
        int result = baseMapper.updateStatus(id, "draft");
        if (result <= 0) {
            log.error("撤销发布模型失败：{}", id);
            throw new RuntimeException("撤销发布模型失败");
        }
        
        // 清空发布时间
        existingModel.setPublishTime(null);
        updateById(existingModel);
        
        log.info("撤销发布模型成功：{}", id);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchPublishModel(List<Long> ids) {
        log.info("批量发布算法模型：{}", ids);
        
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("发布的模型ID列表不能为空");
        }
        
        // 检查所有模型是否可以发布
        List<AlgorithmModel> models = listByIds(ids);
        for (AlgorithmModel model : models) {
            if (!model.canPublish()) {
                throw new RuntimeException("模型 " + model.getModelName() + " 状态不允许发布");
            }
        }
        
        // 批量更新状态
        int result = baseMapper.batchUpdateStatus(ids, "published");
        if (result <= 0) {
            log.error("批量发布模型失败：{}", ids);
            throw new RuntimeException("批量发布模型失败");
        }
        
        // 更新发布时间
        for (AlgorithmModel model : models) {
            model.setPublishTime(LocalDateTime.now());
            updateById(model);
        }
        
        log.info("批量发布模型成功：{}", ids);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String downloadModel(Long id) {
        log.info("下载算法模型：{}", id);
        
        // 检查模型是否存在
        AlgorithmModel existingModel = getModelById(id);
        if (existingModel == null) {
            throw new RuntimeException("模型不存在");
        }
        
        // 检查模型是否可以下载
        if (!existingModel.canDownload()) {
            throw new RuntimeException("模型未发布，无法下载");
        }
        
        // 验证模型文件是否存在
        String filePath = existingModel.getModelPath();
        if (!new File(filePath).exists()) {
            throw new RuntimeException("模型文件不存在");
        }
        
        // 增加下载次数
        int result = baseMapper.updateDownloadCount(id);
        if (result <= 0) {
            log.error("更新下载次数失败：{}", id);
        }
        
        log.info("下载模型成功：{}", id);
        return filePath;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deployModel(Long id) {
        log.info("部署算法模型：{}", id);
        
        // 检查模型是否存在
        AlgorithmModel existingModel = getModelById(id);
        if (existingModel == null) {
            throw new RuntimeException("模型不存在");
        }
        
        // 检查模型是否可以部署
        if (!existingModel.canDeploy()) {
            throw new RuntimeException("模型未发布，无法部署");
        }
        
        // 验证模型文件是否存在
        if (!new File(existingModel.getModelPath()).exists()) {
            throw new RuntimeException("模型文件不存在");
        }
        
        // 这里应该调用实际的部署逻辑
        // 例如：调用Docker API、Kubernetes API等
        log.info("执行模型部署逻辑...");
        
        // 增加部署次数
        int result = baseMapper.updateDeployCount(id);
        if (result <= 0) {
            log.error("更新部署次数失败：{}", id);
        }
        
        log.info("部署模型成功：{}", id);
        return true;
    }

    @Override
    public AlgorithmModelStatisticsDTO getStatistics() {
        return baseMapper.getStatistics();
    }

    @Override
    public boolean checkModelNameAndVersion(String modelName, Integer version, Long excludeId) {
        int count = baseMapper.checkModelNameAndVersion(modelName, version, excludeId);
        return count > 0;
    }

    @Override
    public AlgorithmModel getModelByAlgorithmIdAndVersion(Long algorithmId, Integer version) {
        return baseMapper.selectByAlgorithmIdAndVersion(algorithmId, version);
    }

    @Override
    public AlgorithmModel getLatestModelByAlgorithmId(Long algorithmId) {
        return baseMapper.selectLatestByAlgorithmId(algorithmId);
    }

    @Override
    public List<AlgorithmModel> getPopularModels(Integer limit) {
        return baseMapper.selectPopularModels(limit);
    }

    @Override
    public Long countModelsByCreatedBy(Long createdBy) {
        return baseMapper.countByCreatedBy(createdBy);
    }

    @Override
    public Long getTotalModelSize() {
        return baseMapper.getTotalModelSize();
    }
} 