package org.springblade.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import org.springblade.vlstream.excel.VlsSceneGovernanceExcel;
import org.springblade.vlstream.pojo.entity.SceneGovernance;
import org.springblade.vlstream.pojo.vo.SceneGovernanceVO;

import java.util.List;

/**
 * Scene governance table service class
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsSceneGovernanceService extends BaseService<SceneGovernance> {
	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsSceneGovernance query parameters
	 * @return IPage<VlsSceneGovernanceVO>
	 */
	IPage<SceneGovernanceVO> selectVlsSceneGovernancePage(IPage<SceneGovernanceVO> page, SceneGovernanceVO vlsSceneGovernance);

	/**
	 * Export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsSceneGovernanceExcel>
	 */
	List<VlsSceneGovernanceExcel> exportVlsSceneGovernance(Wrapper<SceneGovernance> queryWrapper);

	/**
	 * Save scene governance and sync job.
	 *
	 * @param sceneGovernance Scene governance entity
	 * @return boolean
	 */
	boolean saveAndSchedule(SceneGovernance sceneGovernance);

	/**
	 * Update scene governance and sync job.
	 *
	 * @param sceneGovernance Scene governance entity
	 * @return boolean
	 */
	boolean updateAndSchedule(SceneGovernance sceneGovernance);

	/**
	 * Save or update scene governance and sync job.
	 *
	 * @param sceneGovernance Scene governance entity
	 * @return boolean
	 */
	boolean submitAndSchedule(SceneGovernance sceneGovernance);

}
