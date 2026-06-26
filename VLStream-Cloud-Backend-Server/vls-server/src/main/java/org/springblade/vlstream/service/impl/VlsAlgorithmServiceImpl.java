package org.springblade.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.vlstream.excel.VlsAlgorithmExcel;
import org.springblade.vlstream.mapper.VlsAlgorithmMapper;
import org.springblade.vlstream.pojo.entity.Algorithm;
import org.springblade.vlstream.pojo.vo.AlgorithmVO;
import org.springblade.vlstream.service.IVlsAlgorithmRepositoryService;
import org.springblade.vlstream.service.IVlsAlgorithmService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Algorithm table service implementation class
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
		log.info("Paginated query for algorithm list, parameters: repositoryId={}, name={}, category={}, deployStatus={}", repositoryId, name, category, deployStatus);
		return algorithmMapper.selectAlgorithmPage(page, repositoryId, name, category, deployStatus);
	}

	@Override
	public List<Algorithm> getByRepositoryId(Long repositoryId) {
		log.info("Query algorithm list by repository ID: {}", repositoryId);
		return algorithmMapper.selectByRepositoryId(repositoryId);
	}

	@Override
	public List<Algorithm> getByCategory(String category) {
		log.info("Query algorithm list by category: {}", category);
		return algorithmMapper.selectByCategory(category);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean createAlgorithm(Algorithm algorithm) {
		log.info("Create algorithm: {}", algorithm.getName());

		// Check if name is duplicated under the same repository
		QueryWrapper<Algorithm> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("repository_id", algorithm.getRepositoryId()).eq("name", algorithm.getName()).eq("is_deleted", 0);
		if (count(queryWrapper) > 0) {
			log.warn("Algorithm name already exists in the same repository: {}", algorithm.getName());
			return false;
		}

		// Set default value
		if (algorithm.getGpuRequired() == null) {
			algorithm.setGpuRequired(0);
		}
		boolean result = save(algorithm);

		// Update algorithm count of repository
		if (result) {
			algorithmRepositoryService.updateAlgorithmCount(algorithm.getRepositoryId());
		}

		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean updateAlgorithm(Algorithm algorithm) {
		log.info("Update algorithm: ID={}, Name={}", algorithm.getId(), algorithm.getName());

		// Get original algorithm info
		Algorithm existing = getById(algorithm.getId());
		if (existing == null) {
			log.warn("Algorithm does not exist: ID={}", algorithm.getId());
			return false;
		}

		// If the repository changes, the algorithm counts of both repositories need to be updated
		Long oldRepositoryId = existing.getRepositoryId();
		Long newRepositoryId = algorithm.getRepositoryId();
		boolean result = updateById(algorithm);

		// Update algorithm count
		if (result && !oldRepositoryId.equals(newRepositoryId)) {
			algorithmRepositoryService.updateAlgorithmCount(oldRepositoryId);
			algorithmRepositoryService.updateAlgorithmCount(newRepositoryId);
		}

		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deleteAlgorithm(Long id) {
		log.info("Delete algorithm: ID={}", id);

		Algorithm algorithm = getById(id);
		if (algorithm == null) {
			log.warn("Algorithm does not exist: ID={}", id);
			return false;
		}

		boolean result = removeById(id);

		// Update algorithm count of repository
		if (result) {
			algorithmRepositoryService.updateAlgorithmCount(algorithm.getRepositoryId());
		}

		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean batchDeleteAlgorithms(List<Long> ids) {
		log.info("Batch delete algorithms: IDs={}", ids);

		// Get repository info of algorithm to be deleted
		List<Algorithm> algorithms = listByIds(ids);
		Map<Long, Boolean> repositoryMap = new HashMap<>();
		algorithms.forEach(algo -> repositoryMap.put(algo.getRepositoryId(), true));

		boolean result = removeByIds(ids);

		// Update algorithm count of related repositories
		if (result) {
			repositoryMap.keySet().forEach(algorithmRepositoryService::updateAlgorithmCount);
		}

		return result;
	}

	@Override
	public boolean updateDeployStatus(Long id, String deployStatus) {
		log.info("Update algorithm deployment status: ID={}, Status={}", id, deployStatus);

		UpdateWrapper<Algorithm> updateWrapper = new UpdateWrapper<>();
		updateWrapper.eq("id", id).set("deploy_status", deployStatus);

		// If deployment succeeds, increase deployment count and update deployment time
		if ("deployed".equals(deployStatus)) {
			updateWrapper.setSql("deploy_count = deploy_count + 1").set("last_deploy_time", LocalDateTime.now());
		}

		return update(updateWrapper);
	}

	@Override
	public boolean batchUpdateDeployStatus(List<Long> ids, String deployStatus) {
		log.info("Batch update algorithm deployment status: IDs={}, Status={}", ids, deployStatus);

		UpdateWrapper<Algorithm> updateWrapper = new UpdateWrapper<>();
		updateWrapper.in("id", ids).set("deploy_status", deployStatus);

		// If deployment succeeds, increase deployment count and update deployment time
		if ("deployed".equals(deployStatus)) {
			updateWrapper.setSql("deploy_count = deploy_count + 1").set("last_deploy_time", LocalDateTime.now());
		}

		return update(updateWrapper);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deployAlgorithmToDevices(Long algorithmId, List<Long> deviceIds) {
		log.info("Deploy algorithm to device: AlgorithmId={}, DeviceIds={}", algorithmId, deviceIds);

		// Update algorithm deployment status to deploying
		updateDeployStatus(algorithmId, "deploying");

		try {
			// Actual deployment service should be called here
			// Simulate deployment process
			Thread.sleep(1000);

			// Deployment successful, update status
			updateDeployStatus(algorithmId, "deployed");

			log.info("Algorithm deployment succeeded: AlgorithmId={}", algorithmId);
			return true;

		} catch (Exception e) {
			log.error("Algorithm deployment failed: AlgorithmId={}", algorithmId, e);

			// Deployment failed, update status
			updateDeployStatus(algorithmId, "failed");
			return false;
		}
	}

	@Override
	public Long countByRepositoryId(Long repositoryId) {
		log.info("Count algorithms under a repository: RepositoryId={}", repositoryId);
		return algorithmMapper.countByRepositoryId(repositoryId);
	}

	@Override
	public List<Map<String, Object>> getCategoryStatistics() {
		log.info("Get algorithm category statistics");
		return algorithmMapper.selectCategoryStatistics();
	}

	@Override
	public List<Map<String, Object>> getTypeStatistics() {
		log.info("Get algorithm type statistics");
		return algorithmMapper.selectTypeStatistics();
	}

	@Override
	public List<Map<String, Object>> getDeployStatusStatistics() {
		log.info("Get deployment status statistics");
		return algorithmMapper.selectDeployStatusStatistics();
	}

	@Override
	public Map<String, Object> evaluateAlgorithm(Long algorithmId) {
		log.info("Algorithm evaluation: AlgorithmId={}", algorithmId);

		Algorithm algorithm = getById(algorithmId);
		if (algorithm == null) {
			log.warn("Algorithm does not exist: ID={}", algorithmId);
			return null;
		}

		// Simulate algorithm evaluation process
		Map<String, Object> result = new HashMap<>();
		result.put("algorithmId", algorithmId);
		result.put("algorithmName", algorithm.getName());
		result.put("accuracy", 0.95);
		result.put("precision", 0.92);
		result.put("recall", 0.89);
		result.put("f1Score", 0.905);
		result.put("evaluationTime", LocalDateTime.now());
		result.put("status", "completed");

		log.info("Algorithm evaluation completed: AlgorithmId={}, Result={}", algorithmId, result);
		return result;
	}

}
