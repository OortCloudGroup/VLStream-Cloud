/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 */

package com.ruoyi.vlstream.test.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import com.ruoyi.vlstream.test.common.enums.YesNoEnum;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmModelExcel;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmModelMapper;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmTrainingMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmModel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmModelVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAlgorithmModelService;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 算法模型表 服务实现类
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
			log.warn("获取模型详情时，ID为空");
			return null;
		}
		return baseMapper.selectById(id);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public AlgorithmModel createModel(AlgorithmModelVO createDTO) {
		log.info("创建算法模型：{}", createDTO.getModelName());

		// 验证模型名称和版本是否存在
		Integer version = createDTO.getVersion();
		if (version == null || version < 1) {
			version = 1;
		}
		while (checkModelNameAndVersion(createDTO.getModelName(), version, null)) {
			version++;
		}

		AlgorithmTraining training = trainingMapper.selectById(createDTO.getTrainingId());
		// 创建模型实体
		AlgorithmModel model = new AlgorithmModel();
		BeanUtils.copyProperties(createDTO, model);
		model.setVersion(version);
		// 设置默认值
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
	public boolean deleteModel(Long id) {
		log.info("删除算法模型：{}", id);

		// 检查模型是否存在
		AlgorithmModel existingModel = getModelById(id);
		if (existingModel == null) {
			throw new RuntimeException("模型不存在");
		}

		// 检查模型是否可以删除
		if (existingModel.getStatus().equals(2)) {
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
			if (model.getStatus().equals(2)) {
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
		if (existingModel.getStatus().equals(2)) {
			throw new RuntimeException("模型状态不允许发布");
		}

		// 验证模型文件是否存在
		if (!new File(existingModel.getModelPath()).exists()) {
			throw new RuntimeException("模型文件不存在，无法发布");
		}

		// 更新状态为已发布
		UpdateWrapper<AlgorithmModel> publishWrapper = new UpdateWrapper<>();
		publishWrapper.eq("id", id).set("status", "published");
		if (!update(new AlgorithmModel(), publishWrapper)) {
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
		UpdateWrapper<AlgorithmModel> draftWrapper = new UpdateWrapper<>();
		draftWrapper.eq("id", id).set("status", "draft");
		if (!update(new AlgorithmModel(), draftWrapper)) {
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
			if (model.getStatus().equals(2)) {
				throw new RuntimeException("模型 " + model.getModelName() + " 状态不允许发布");
			}
		}

		// 批量更新状态
		UpdateWrapper<AlgorithmModel> publishWrapper = new UpdateWrapper<>();
		publishWrapper.in("id", ids).set("status", "published");
		if (!update(new AlgorithmModel(), publishWrapper)) {
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
		if (existingModel.getStatus().equals(2)) {
			throw new RuntimeException("模型未发布，无法下载");
		}

		// 验证模型文件是否存在
		String filePath = existingModel.getModelPath();
		if (!new File(filePath).exists()) {
			throw new RuntimeException("模型文件不存在");
		}

		// 增加下载次数
		UpdateWrapper<AlgorithmModel> downloadWrapper = new UpdateWrapper<>();
		downloadWrapper.eq("id", id).setSql("download_count = download_count + 1");
		if (!update(new AlgorithmModel(), downloadWrapper)) {
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
		if (!existingModel.getStatus().equals(2)) {
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
		UpdateWrapper<AlgorithmModel> deployWrapper = new UpdateWrapper<>();
		deployWrapper.eq("id", id).setSql("deploy_count = deploy_count + 1");
		if (!update(new AlgorithmModel(), deployWrapper)) {
			log.error("更新部署次数失败：{}", id);
		}

		log.info("部署模型成功：{}", id);
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
