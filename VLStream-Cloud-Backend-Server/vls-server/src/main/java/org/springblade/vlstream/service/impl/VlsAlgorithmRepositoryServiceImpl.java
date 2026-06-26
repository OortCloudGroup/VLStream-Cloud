package org.springblade.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.common.enums.YesNoEnum;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.vlstream.enums.AlgorithmRepositoryTypeEnum;
import org.springblade.vlstream.excel.VlsAlgorithmRepositoryExcel;
import org.springblade.vlstream.mapper.VlsAlgorithmMapper;
import org.springblade.vlstream.mapper.VlsAlgorithmRepositoryMapper;
import org.springblade.vlstream.pojo.entity.AlgorithmRepository;
import org.springblade.vlstream.pojo.vo.AlgorithmRepositoryVO;
import org.springblade.vlstream.service.IVlsAlgorithmRepositoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Algorithm repository table Service implementation class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VlsAlgorithmRepositoryServiceImpl extends BaseServiceImpl<VlsAlgorithmRepositoryMapper, AlgorithmRepository> implements IVlsAlgorithmRepositoryService {

	private final VlsAlgorithmRepositoryMapper algorithmRepositoryMapper;
	private final VlsAlgorithmMapper algorithmMapper;

	@Override
	public IPage<AlgorithmRepositoryVO> selectVlsAlgorithmRepositoryPage(IPage<AlgorithmRepositoryVO> page, AlgorithmRepositoryVO vlsAlgorithmRepository) {
		return page.setRecords(baseMapper.selectVlsAlgorithmRepositoryPage(page, vlsAlgorithmRepository));
	}

	@Override
	public List<VlsAlgorithmRepositoryExcel> exportVlsAlgorithmRepository(Wrapper<AlgorithmRepository> queryWrapper) {
		List<VlsAlgorithmRepositoryExcel> vlsAlgorithmRepositoryList = baseMapper.exportVlsAlgorithmRepository(queryWrapper);
		//vlsAlgorithmRepositoryList.forEach(vlsAlgorithmRepository -> {
		//	vlsAlgorithmRepository.setTypeName(DictCache.getValue(DictEnum.YES_NO, VlsAlgorithmRepositoryEntity.getType()));
		//});
		return vlsAlgorithmRepositoryList;
	}

	@Override
	public IPage<AlgorithmRepository> selectRepositoryPage(Page<AlgorithmRepository> page,
														   String name,
														   String repositoryType,
														   String status) {
		log.info("Paginated query for algorithm repository list, parameters: name={}, repositoryType={}, status={}", name, repositoryType, status);
		return algorithmRepositoryMapper.selectRepositoryPage(page, name, repositoryType, status);
	}

	@Override
	public List<AlgorithmRepository> getEnabledRepositories() {
		log.info("Query All Enabled Algorithm Repositories");
		return algorithmRepositoryMapper.selectEnabledRepositories();
	}

	@Override
	public List<AlgorithmRepository> getByRepositoryType(String repositoryType) {
		log.info("Query algorithm repository by type: {}", repositoryType);
		return algorithmRepositoryMapper.selectByRepositoryType(repositoryType);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean createRepository(AlgorithmRepository repository) {
		log.info("Create algorithm repository: {}", repository.getName());

		// Check if name is duplicated
		QueryWrapper<AlgorithmRepository> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("name", repository.getName());
		if (count(queryWrapper) > 0) {
			log.warn("Algorithm repository name already exists: {}", repository.getName());
			return false;
		}

		// Set default value
		if (repository.getAlgorithmCount() == null) {
			repository.setAlgorithmCount(0);
		}
		if (repository.getRepositoryType() == null) {
			repository.setRepositoryType(AlgorithmRepositoryTypeEnum.extended);
		}
		if (repository.getStatus() == null) {
			repository.setStatus(YesNoEnum.YES.getCode());
		}

		return save(repository);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean updateRepository(AlgorithmRepository repository) {
		log.info("Update algorithm repository: ID={}, Name={}", repository.getId(), repository.getName());

		// Check if it is a basic preset algorithm repository (modifying certain fields is not allowed)
		AlgorithmRepository existing = getById(repository.getId());
		if (existing != null && "basic".equals(existing.getRepositoryType())) {
			// Modification of basic preset algorithm library is only allowed for remarks and status
			repository.setName(existing.getName());
			repository.setRepositoryType(existing.getRepositoryType());
		}

		return updateById(repository);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean deleteRepository(Long id) {
		log.info("Delete algorithm repository: ID={}", id);

		// Check if it is a basic preset algorithm repository (deletion is not allowed)
		AlgorithmRepository repository = getById(id);
		if (repository != null && "basic".equals(repository.getRepositoryType())) {
			log.warn("Deleting basic preset algorithm library is not allowed: ID={}", id);
			return false;
		}

		return removeById(id);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean batchDeleteRepositories(List<Long> ids) {
		log.info("Batch delete algorithm repositories: IDs={}", ids);

		// Filter out basic preset algorithm library
		List<AlgorithmRepository> repositories = listByIds(ids);
		List<Long> allowedIds = repositories.stream()
			.filter(repo -> !"basic".equals(repo.getRepositoryType()))
			.map(AlgorithmRepository::getId)
			.collect(Collectors.toList());

		if (allowedIds.isEmpty()) {
			log.warn("No deletable algorithm repository");
			return false;
		}

		return removeByIds(allowedIds);
	}

	@Override
	public boolean updateRepositoryStatus(Long id, String status) {
		log.info("Update algorithm repository status: ID={}, Status={}", id, status);

		UpdateWrapper<AlgorithmRepository> updateWrapper = new UpdateWrapper<>();
		updateWrapper.eq("id", id)
			.set("status", status);

		return update(updateWrapper);
	}

	@Override
	public boolean batchUpdateRepositoryStatus(List<Long> ids, String status) {
		log.info("Batch update algorithm repository status: IDs={}, Status={}", ids, status);

		UpdateWrapper<AlgorithmRepository> updateWrapper = new UpdateWrapper<>();
		updateWrapper.in("id", ids)
			.set("status", status);

		return update(updateWrapper);
	}

	@Override
	public Long countRepositories() {
		log.info("Count algorithm repositories");
		return algorithmRepositoryMapper.countRepositories();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void updateAlgorithmCount(Long repositoryId) {
		log.info("Update algorithm count of algorithm repository: ID={}", repositoryId);

		Long count = algorithmMapper.countByRepositoryId(repositoryId);

		UpdateWrapper<AlgorithmRepository> updateWrapper = new UpdateWrapper<>();
		updateWrapper.eq("id", repositoryId)
			.set("algorithm_count", count);

		update(updateWrapper);
	}

}
