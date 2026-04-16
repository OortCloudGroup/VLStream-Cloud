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
 * Algorithm Model Service Implementation Class
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
            log.warn("Get model details, ID is null");
            return null;
        }
        return baseMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlgorithmModel createModel(AlgorithmModelCreateDTO createDTO) {
        log.info("Create algorithm model: {}", createDTO.getModelName());
        
        // Validate model name and version exist
        Integer version = createDTO.getVersion();
        if (version == null || version < 1) {
            version = 1;
        }
        while (checkModelNameAndVersion(createDTO.getModelName(), version, null)) {
            version++;
        }

        // Validate model file exists
        if (!new File(createDTO.getModelPath()).exists()) {
            log.warn("Model file does not exist: {}", createDTO.getModelPath());
            // In actual production environment, an exception should be thrown here
            // throw new RuntimeException("Model file does not exist");
        }
        
        // Create model entity
        AlgorithmModel model = new AlgorithmModel();
        BeanUtils.copyProperties(createDTO, model);
        model.setVersion(version);
        model.setModelPath("datasets/" + model.getModelPath());
        // Set default values
        if (model.getDownloadCount() == null) {
            model.setDownloadCount(0);
        }
        if (model.getDeployCount() == null) {
            model.setDeployCount(0);
        }
        if (model.getStatus() == null) {
            model.setStatus("draft");
        }
        
        // Save to database
        boolean success = save(model);
        if (!success) {
            log.error("Create model failed: {}", createDTO.getModelName());
            throw new RuntimeException("Create model failed");
        }
        
        log.info("Model created successfully, ID: {}", model.getId());
        return model;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlgorithmModel updateModel(AlgorithmModelUpdateDTO updateDTO) {
        log.info("Update algorithm model: {}", updateDTO.getId());
        
        // Check if model exists
        AlgorithmModel existingModel = getModelById(updateDTO.getId());
        if (existingModel == null) {
            throw new RuntimeException("Model does not exist");
        }
        
        // Validate model name and version exist (exclude current record)
        if (updateDTO.getModelName() != null && updateDTO.getVersion() != null) {
            if (checkModelNameAndVersion(updateDTO.getModelName(), updateDTO.getVersion(), updateDTO.getId())) {
                throw new RuntimeException("Model name and version already exist");
            }
        }
        
        // Check if model can be updated
        if ("published".equals(existingModel.getStatus()) && updateDTO.getStatus() != null && 
            !updateDTO.getStatus().equals(existingModel.getStatus())) {
            throw new RuntimeException("Published model cannot modify status");
        }
        
        // Update fields
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
        
        // Save update
        boolean success = updateById(existingModel);
        if (!success) {
            log.error("Update model failed: {}", updateDTO.getId());
            throw new RuntimeException("Update model failed");
        }
        
        log.info("Model updated successfully: {}", updateDTO.getId());
        return existingModel;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteModel(Long id) {
        log.info("Delete algorithm model: {}", id);
        
        // Check if model exists
        AlgorithmModel existingModel = getModelById(id);
        if (existingModel == null) {
            throw new RuntimeException("Model does not exist");
        }
        
        // Check if model can be deleted
        if (!existingModel.canDelete()) {
            throw new RuntimeException("Published model cannot be deleted");
        }
        
        // Delete model (logical delete)
        boolean success = removeById(id);
        if (!success) {
            log.error("Delete model failed: {}", id);
            throw new RuntimeException("Delete model failed");
        }
        
        log.info("Model deleted successfully: {}", id);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteModel(List<Long> ids) {
        log.info("Batch delete algorithm models: {}", ids);
        
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("Model ID list to delete cannot be null");
        }
        
        // Check if all models can be deleted
        List<AlgorithmModel> models = listByIds(ids);
        for (AlgorithmModel model : models) {
            if (!model.canDelete()) {
                throw new RuntimeException("Model " + model.getModelName() + " is published and cannot be deleted");
            }
        }
        
        // Batch delete
        boolean success = removeByIds(ids);
        if (!success) {
            log.error("Batch delete models failed: {}", ids);
            throw new RuntimeException("Batch delete models failed");
        }
        
        log.info("Models batch deleted successfully: {}", ids);
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
        log.info("Publish algorithm model: {}", id);
        
        // Check if model exists
        AlgorithmModel existingModel = getModelById(id);
        if (existingModel == null) {
            throw new RuntimeException("Model does not exist");
        }
        
        // Check if model can be published
        if (!existingModel.canPublish()) {
            throw new RuntimeException("Model status does not allow publishing");
        }
        
        // Validate model file exists
        if (!new File(existingModel.getModelPath()).exists()) {
            throw new RuntimeException("Model file does not exist, cannot publish");
        }
        
        // Update status to published
        int result = baseMapper.updateStatus(id, "published");
        if (result <= 0) {
            log.error("Publish model failed: {}", id);
            throw new RuntimeException("Publish model failed");
        }
        
        // Update publish time
        existingModel.setPublishTime(LocalDateTime.now());
        updateById(existingModel);
        
        log.info("Model published successfully: {}", id);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean unpublishModel(Long id) {
        log.info("Unpublish algorithm model: {}", id);
        
        // Check if model exists
        AlgorithmModel existingModel = getModelById(id);
        if (existingModel == null) {
            throw new RuntimeException("Model does not exist");
        }
        
        // Check if model is published
        if (!"published".equals(existingModel.getStatus())) {
            throw new RuntimeException("Model is not published, cannot unpublish");
        }
        
        // Update status to draft
        int result = baseMapper.updateStatus(id, "draft");
        if (result <= 0) {
            log.error("Unpublish model failed: {}", id);
            throw new RuntimeException("Unpublish model failed");
        }
        
        // Clear publish time
        existingModel.setPublishTime(null);
        updateById(existingModel);
        
        log.info("Model unpublished successfully: {}", id);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchPublishModel(List<Long> ids) {
        log.info("Batch publish algorithm models: {}", ids);
        
        if (ids == null || ids.isEmpty()) {
            throw new RuntimeException("Model ID list to publish cannot be null");
        }
        
        // Check if all models can be published
        List<AlgorithmModel> models = listByIds(ids);
        for (AlgorithmModel model : models) {
            if (!model.canPublish()) {
                throw new RuntimeException("Model " + model.getModelName() + " status does not allow publishing");
            }
        }
        
        // Batch update status
        int result = baseMapper.batchUpdateStatus(ids, "published");
        if (result <= 0) {
            log.error("Batch publish models failed: {}", ids);
            throw new RuntimeException("Batch publish models failed");
        }
        
        // Update publish time
        for (AlgorithmModel model : models) {
            model.setPublishTime(LocalDateTime.now());
            updateById(model);
        }
        
        log.info("Models batch published successfully: {}", ids);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String downloadModel(Long id) {
        log.info("Download algorithm model: {}", id);
        
        // Check if model exists
        AlgorithmModel existingModel = getModelById(id);
        if (existingModel == null) {
            throw new RuntimeException("Model does not exist");
        }
        
        // Check if model can be downloaded
        if (!existingModel.canDownload()) {
            throw new RuntimeException("Model is not published, cannot download");
        }
        
        // Validate model file exists
        String filePath = existingModel.getModelPath();
        if (!new File(filePath).exists()) {
            throw new RuntimeException("Model file does not exist");
        }
        
        // Increment download count
        int result = baseMapper.updateDownloadCount(id);
        if (result <= 0) {
            log.error("Update download count failed: {}", id);
        }
        
        log.info("Model downloaded successfully: {}", id);
        return filePath;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deployModel(Long id) {
        log.info("Deploy algorithm model: {}", id);
        
        // Check if model exists
        AlgorithmModel existingModel = getModelById(id);
        if (existingModel == null) {
            throw new RuntimeException("Model does not exist");
        }
        
        // Check if model can be deployed
        if (!existingModel.canDeploy()) {
            throw new RuntimeException("Model is not published, cannot deploy");
        }
        
        // Validate model file exists
        if (!new File(existingModel.getModelPath()).exists()) {
            throw new RuntimeException("Model file does not exist");
        }
        
        // Here should call actual deployment logic
        // e.g., call Docker API, Kubernetes API, etc.
        log.info("Execute model deployment logic...");
        
        // Increment deployment count
        int result = baseMapper.updateDeployCount(id);
        if (result <= 0) {
            log.error("Update deployment count failed: {}", id);
        }
        
        log.info("Model deployed successfully: {}", id);
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