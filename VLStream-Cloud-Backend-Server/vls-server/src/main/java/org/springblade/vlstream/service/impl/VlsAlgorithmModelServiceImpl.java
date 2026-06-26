package org.springblade.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springblade.common.enums.YesNoEnum;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.vlstream.excel.VlsAlgorithmModelExcel;
import org.springblade.vlstream.mapper.VlsAlgorithmModelMapper;
import org.springblade.vlstream.mapper.VlsAlgorithmTrainingMapper;
import org.springblade.vlstream.pojo.entity.AlgorithmModel;
import org.springblade.vlstream.pojo.entity.AlgorithmTraining;
import org.springblade.vlstream.pojo.vo.AlgorithmModelVO;
import org.springblade.vlstream.service.IVlsAlgorithmModelService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Algorithm model table service implementation class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@Service
public class VlsAlgorithmModelServiceImpl extends BaseServiceImpl<VlsAlgorithmModelMapper, AlgorithmModel> implements IVlsAlgorithmModelService {

	@Resource
	private VlsAlgorithmTrainingMapper trainingMapper;

	@Override
	public IPage<AlgorithmModelVO> selectVlsAlgorithmModelPage(IPage<AlgorithmModelVO> page, AlgorithmModelVO vlsAlgorithmModel) {
		return page.setRecords(baseMapper.selectVlsAlgorithmModelPage(page, vlsAlgorithmModel));
	}

	@Override
	public List<VlsAlgorithmModelExcel> exportVlsAlgorithmModel(Wrapper<AlgorithmModel> queryWrapper) {
		List<VlsAlgorithmModelExcel> vlsAlgorithmModelList = baseMapper.exportVlsAlgorithmModel(queryWrapper);
		//vlsAlgorithmModelList.forEach(vlsAlgorithmModel -> {
		//	vlsAlgorithmModel.setTypeName(DictCache.getValue(DictEnum.YES_NO, VlsAlgorithmModelEntity.getType()));
		//});
		return vlsAlgorithmModelList;
	}

	@Override
	public AlgorithmModel getModelById(Long id) {
		if (id == null) {
			log.warn("ID is empty when getting model details");
			return null;
		}
		return baseMapper.selectById(id);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public AlgorithmModel createModel(AlgorithmModelVO createDTO) {
		log.info("Create algorithm model: {}", createDTO.getModelName());

		// Verify if model name and version exist
		Integer version = createDTO.getVersion();
		if (version == null || version < 1) {
			version = 1;
		}
		while (checkModelNameAndVersion(createDTO.getModelName(), version, null)) {
			version++;
		}

		AlgorithmTraining training = trainingMapper.selectById(createDTO.getTrainingId());
		// Create model entity
		AlgorithmModel model = new AlgorithmModel();
		BeanUtils.copyProperties(createDTO, model);
		model.setVersion(version);
		// Set default value
		if (model.getDownloadCount() == null) {
			model.setDownloadCount(0);
		}
		if (model.getDeployCount() == null) {
			model.setDeployCount(0);
		}
		if (model.getStatus() == null) {
			model.setStatus(YesNoEnum.NO.getCode());
		}
		model.setModelPath(training.getModelOutputPath());
		model.setOnnxModelPath(training.getOnnxModelOutputPath());
		model.setRknnModelPath(training.getRknnModelOutputPath());
		model.setInt8RknnModelOutputPath(training.getInt8RknnModelOutputPath());
		// Save to database
		boolean success = save(model);
		if (!success) {
			log.error("Failed to create model: {}", createDTO.getModelName());
			throw new RuntimeException("Failed to create model");
		}

		log.info("Created model successfully, ID: {}", model.getId());
		return model;
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
		if (existingModel.getStatus().equals(2)) {
			throw new RuntimeException("Published models cannot be deleted");
		}

		// Delete model (logical deletion)
		boolean success = removeById(id);
		if (!success) {
			log.error("Failed to delete model: {}", id);
			throw new RuntimeException("Failed to delete model");
		}

		log.info("Deleted model successfully: {}", id);
		return true;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean batchDeleteModel(List<Long> ids) {
		log.info("Batch delete algorithm models: {}", ids);

		if (ids == null || ids.isEmpty()) {
			throw new RuntimeException("List of model IDs to delete cannot be empty");
		}

		// Check if all models can be deleted
		List<AlgorithmModel> models = listByIds(ids);
		for (AlgorithmModel model : models) {
			if (model.getStatus().equals(2)) {
				throw new RuntimeException("Model" + model.getModelName() + "Published, cannot be deleted");
			}
		}

		// Batch delete
		boolean success = removeByIds(ids);
		if (!success) {
			log.error("Failed to batch delete models: {}", ids);
			throw new RuntimeException("Failed to batch delete models");
		}

		log.info("Successfully batch deleted models: {}", ids);
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
		if (existingModel.getStatus().equals(2)) {
			throw new RuntimeException("Model status does not allow publishing");
		}

		// Verify if model file exists
		if (!new File(existingModel.getModelPath()).exists()) {
			throw new RuntimeException("Model file does not exist, cannot be published");
		}

		// Update status to published
		int result = baseMapper.updateStatus(id, "published");
		if (result <= 0) {
			log.error("Failed to publish model: {}", id);
			throw new RuntimeException("Failed to publish model");
		}

		// Update publish time
		existingModel.setPublishTime(LocalDateTime.now());
		updateById(existingModel);

		log.info("Published model successfully: {}", id);
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
			throw new RuntimeException("Model not published, cannot be revoked");
		}

		// Update status to draft
		int result = baseMapper.updateStatus(id, "draft");
		if (result <= 0) {
			log.error("Failed to unpublish model: {}", id);
			throw new RuntimeException("Failed to unpublish model");
		}

		// Clear publication time
		existingModel.setPublishTime(null);
		updateById(existingModel);

		log.info("Successfully unpublished model: {}", id);
		return true;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean batchPublishModel(List<Long> ids) {
		log.info("Batch publish algorithm models: {}", ids);

		if (ids == null || ids.isEmpty()) {
			throw new RuntimeException("List of published model IDs cannot be empty");
		}

		// Check if all models can be published
		List<AlgorithmModel> models = listByIds(ids);
		for (AlgorithmModel model : models) {
			if (model.getStatus().equals(2)) {
				throw new RuntimeException("Model" + model.getModelName() + "Status does not allow publishing");
			}
		}

		// Batch update status
		int result = baseMapper.batchUpdateStatus(ids, "published");
		if (result <= 0) {
			log.error("Failed to batch publish models: {}", ids);
			throw new RuntimeException("Failed to batch publish models");
		}

		// Update publish time
		for (AlgorithmModel model : models) {
			model.setPublishTime(LocalDateTime.now());
			updateById(model);
		}

		log.info("Successfully batch published models: {}", ids);
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
		if (existingModel.getStatus().equals(2)) {
			throw new RuntimeException("Model not published, cannot be downloaded");
		}

		// Verify if model file exists
		String filePath = existingModel.getModelPath();
		if (!new File(filePath).exists()) {
			throw new RuntimeException("Model file does not exist");
		}

		// Increase download count
		int result = baseMapper.updateDownloadCount(id);
		if (result <= 0) {
			log.error("Failed to update download count: {}", id);
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
		if (!existingModel.getStatus().equals(2)) {
			throw new RuntimeException("Model not published, cannot be deployed");
		}

		// Verify if model file exists
		if (!new File(existingModel.getModelPath()).exists()) {
			throw new RuntimeException("Model file does not exist");
		}

		// Actual deployment logic should be called here
		// Example: Call Docker API, Kubernetes API, etc.
		log.info("Executing model deployment logic...");

		// Increase deployment count
		int result = baseMapper.updateDeployCount(id);
		if (result <= 0) {
			log.error("Failed to update deployment count: {}", id);
		}

		log.info("Successfully deployed model: {}", id);
		return true;
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
