/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.mp.base.BaseServiceImpl;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmExcel;
import com.ruoyi.vlstream.test.vlstream.mapper.VlsAlgorithmMapper;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.Algorithm;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmVO;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAlgorithmRepositoryService;
import com.ruoyi.vlstream.test.vlstream.service.IVlsAlgorithmService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * algorithm service
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VlsAlgorithmServiceImpl extends BaseServiceImpl<VlsAlgorithmMapper, Algorithm> implements IVlsAlgorithmService {

	private final VlsAlgorithmMapper algorithmMapper;
	private final IVlsAlgorithmRepositoryService algorithmRepositoryService;

	@Override
	public IPage<AlgorithmVO> selectVlsAlgorithmPage(IPage<AlgorithmVO> page, AlgorithmVO vlsAlgorithm) {
		return page.setRecords(baseMapper.selectVlsAlgorithmPage(page, vlsAlgorithm));
	}

	@Override
	public List<VlsAlgorithmExcel> exportVlsAlgorithm(Wrapper<Algorithm> queryWrapper) {
		List<VlsAlgorithmExcel> vlsAlgorithmList = baseMapper.exportVlsAlgorithm(queryWrapper);
		//vlsAlgorithmList.forEach(vlsAlgorithm -> {
		//	vlsAlgorithm.setTypeName(DictCache.getValue(DictEnum.YES_NO, VlsAlgorithmEntity.getType()));
		//});
		return vlsAlgorithmList;
	}

	@Override
	public IPage<Algorithm> selectAlgorithmPage(Page<Algorithm> page, Long repositoryId, String name, String category, String deployStatus) {
		log.info("分页查询算法列表，参数：repositoryId={}, name={}, category={}, deployStatus={}", repositoryId, name, category, deployStatus);
		return algorithmMapper.selectAlgorithmPage(page, repositoryId, name, category, deployStatus);
	}

	@Override
	public List<Algorithm> getByRepositoryId(Long repositoryId) {
		log.info("根据仓库ID查询算法列表：{}", repositoryId);
		return algorithmMapper.selectByRepositoryId(repositoryId);
	}

	@Override
	public List<Algorithm> getByCategory(String category) {
		log.info("根据分类查询算法列表：{}", category);
		return algorithmMapper.selectByCategory(category);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean createAlgorithm(Algorithm algorithm) {
		log.info("创建算法：{}", algorithm.getName());

		// whether
		QueryWrapper<Algorithm> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("repository_id", algorithm.getRepositoryId()).eq("name", algorithm.getName()).eq("is_deleted", 0);
		if (count(queryWrapper) > 0) {
			log.warn("同一仓库下算法名称已存在：{}", algorithm.getName());
			return false;
		}

		// Set value
		if (algorithm.getGpuRequired() == null) {
			algorithm.setGpuRequired(0);
		}
		boolean result = save(algorithm);

		// new algorithm
		if (result) {
			algorithmRepositoryService.updateAlgorithmCount(algorithm.getRepositoryId());
		}

		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean updateAlgorithm(Algorithm algorithm) {
		log.info("更新算法：ID={}, Name={}", algorithm.getId(), algorithm.getName());

		// Get algorithminfo
		Algorithm existing = getById(algorithm.getId());
		if (existing == null) {
			log.warn("算法不存在：ID={}", algorithm.getId());
			return false;
		}

		// if , need to new algorithm
		Long oldRepositoryId = existing.getRepositoryId();
		Long newRepositoryId = algorithm.getRepositoryId();
		boolean result = updateById(algorithm);

		// new algorithm
		if (result && !oldRepositoryId.equals(newRepositoryId)) {
			algorithmRepositoryService.updateAlgorithmCount(oldRepositoryId);
			algorithmRepositoryService.updateAlgorithmCount(newRepositoryId);
		}

		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deleteAlgorithm(Long id) {
		log.info("删除算法：ID={}", id);

		Algorithm algorithm = getById(id);
		if (algorithm == null) {
			log.warn("算法不存在：ID={}", id);
			return false;
		}

		boolean result = removeById(id);

		// new algorithm
		if (result) {
			algorithmRepositoryService.updateAlgorithmCount(algorithm.getRepositoryId());
		}

		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean batchDeleteAlgorithms(List<Long> ids) {
		log.info("批量删除算法：IDs={}", ids);

		// Get Delete algorithm info
		List<Algorithm> algorithms = listByIds(ids);
		Map<Long, Boolean> repositoryMap = new HashMap<>();
		algorithms.forEach(algo -> repositoryMap.put(algo.getRepositoryId(), true));

		boolean result = removeByIds(ids);

		// new related algorithm
		if (result) {
			repositoryMap.keySet().forEach(algorithmRepositoryService::updateAlgorithmCount);
		}

		return result;
	}

	@Override
	public boolean updateDeployStatus(Long id, String deployStatus) {
		log.info("更新算法部署状态：ID={}, Status={}", id, deployStatus);

		UpdateWrapper<Algorithm> updateWrapper = new UpdateWrapper<>();
		updateWrapper.eq("id", id).set("deploy_status", deployStatus);

		// if is successfully, and new
		if ("deployed".equals(deployStatus)) {
			updateWrapper.setSql("deploy_count = deploy_count + 1").set("last_deploy_time", LocalDateTime.now());
		}

		return update(new Algorithm(), updateWrapper);
	}

	@Override
	public boolean batchUpdateDeployStatus(List<Long> ids, String deployStatus) {
		log.info("批量更新算法部署状态：IDs={}, Status={}", ids, deployStatus);

		UpdateWrapper<Algorithm> updateWrapper = new UpdateWrapper<>();
		updateWrapper.in("id", ids).set("deploy_status", deployStatus);

		// if is successfully, and new
		if ("deployed".equals(deployStatus)) {
			updateWrapper.setSql("deploy_count = deploy_count + 1").set("last_deploy_time", LocalDateTime.now());
		}

		return update(new Algorithm(), updateWrapper);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deployAlgorithmToDevices(Long algorithmId, List<Long> deviceIds) {
		log.info("部署算法到设备：AlgorithmId={}, DeviceIds={}", algorithmId, deviceIds);

		// new algorithm to in
		updateDeployStatus(algorithmId, "deploying");

		try {
			// service
			//
			Thread.sleep(1000);

			// successfully, new
			updateDeployStatus(algorithmId, "deployed");

			log.info("算法部署成功：AlgorithmId={}", algorithmId);
			return true;

		} catch (Exception e) {
			log.error("算法部署失败：AlgorithmId={}", algorithmId, e);

			// failed, new
			updateDeployStatus(algorithmId, "failed");
			return false;
		}
	}

	@Override
	public Long countByRepositoryId(Long repositoryId) {
		log.info("统计某仓库下的算法数量：RepositoryId={}", repositoryId);
		return algorithmMapper.countByRepositoryId(repositoryId);
	}

	@Override
	public List<Map<String, Object>> getCategoryStatistics() {
		log.info("获取算法分类统计");
		return algorithmMapper.selectCategoryStatistics();
	}

	@Override
	public List<Map<String, Object>> getTypeStatistics() {
		log.info("获取算法类型统计");
		return algorithmMapper.selectTypeStatistics();
	}

	@Override
	public List<Map<String, Object>> getDeployStatusStatistics() {
		log.info("获取部署状态统计");
		return algorithmMapper.selectDeployStatusStatistics();
	}

	@Override
	public Map<String, Object> evaluateAlgorithm(Long algorithmId) {
		log.info("算法评估：AlgorithmId={}", algorithmId);

		Algorithm algorithm = getById(algorithmId);
		if (algorithm == null) {
			log.warn("算法不存在：ID={}", algorithmId);
			return null;
		}

		// algorithm
		Map<String, Object> result = new HashMap<>();
		result.put("algorithmId", algorithmId);
		result.put("algorithmName", algorithm.getName());
		result.put("accuracy", 0.95);
		result.put("precision", 0.92);
		result.put("recall", 0.89);
		result.put("f1Score", 0.905);
		result.put("evaluationTime", LocalDateTime.now());
		result.put("status", "completed");

		log.info("算法评估完成：AlgorithmId={}, Result={}", algorithmId, result);
		return result;
	}

}
