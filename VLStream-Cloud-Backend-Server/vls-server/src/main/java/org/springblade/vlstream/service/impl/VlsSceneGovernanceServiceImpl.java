package org.springblade.vlstream.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springblade.core.mp.base.BaseServiceImpl;
import org.springblade.vlstream.excel.VlsSceneGovernanceExcel;
import org.springblade.vlstream.mapper.VlsSceneGovernanceMapper;
import org.springblade.vlstream.pojo.entity.SceneGovernance;
import org.springblade.vlstream.pojo.vo.SceneGovernanceVO;
import org.springblade.vlstream.service.IVlsSceneGovernanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Scene governance table service implementation class
 *
 * @author Oort
 * @since 2025-12-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VlsSceneGovernanceServiceImpl extends BaseServiceImpl<VlsSceneGovernanceMapper, SceneGovernance> implements IVlsSceneGovernanceService {

	@Override
	public IPage<SceneGovernanceVO> selectVlsSceneGovernancePage(IPage<SceneGovernanceVO> page, SceneGovernanceVO vlsSceneGovernance) {
		return page.setRecords(baseMapper.selectVlsSceneGovernancePage(page, vlsSceneGovernance));
	}

	@Override
	public List<VlsSceneGovernanceExcel> exportVlsSceneGovernance(Wrapper<SceneGovernance> queryWrapper) {
		List<VlsSceneGovernanceExcel> vlsSceneGovernanceList = baseMapper.exportVlsSceneGovernance(queryWrapper);
		//vlsSceneGovernanceList.forEach(vlsSceneGovernance -> {
		//	vlsSceneGovernance.setTypeName(DictCache.getValue(DictEnum.YES_NO, VlsSceneGovernanceEntity.getType()));
		//});
		return vlsSceneGovernanceList;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean saveAndSchedule(SceneGovernance sceneGovernance) {
		boolean saved = save(sceneGovernance);
		if (!saved) {
			return false;
		}
		return true;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean updateAndSchedule(SceneGovernance sceneGovernance) {
		boolean updated = updateById(sceneGovernance);
		if (!updated) {
			return false;
		}
		return true;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public boolean submitAndSchedule(SceneGovernance sceneGovernance) {
		boolean submitted = saveOrUpdate(sceneGovernance);
		if (!submitted) {
			return false;
		}
		return true;
	}

}
