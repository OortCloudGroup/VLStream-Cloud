package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsSceneGovernanceExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.SceneGovernance;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.SceneGovernanceVO;

import java.util.List;

/**
 * 场景治理表 服务类
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsSceneGovernanceService extends BaseService<SceneGovernance> {
	/**
	 * 自定义分页
	 *
	 * @param page 分页参数
	 * @param vlsSceneGovernance 查询参数
	 * @return IPage<VlsSceneGovernanceVO>
	 */
	IPage<SceneGovernanceVO> selectVlsSceneGovernancePage(IPage<SceneGovernanceVO> page, SceneGovernanceVO vlsSceneGovernance);

	/**
	 * 导出数据
	 *
	 * @param queryWrapper 查询条件
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
