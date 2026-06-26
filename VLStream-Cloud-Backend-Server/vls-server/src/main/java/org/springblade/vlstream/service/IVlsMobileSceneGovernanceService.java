package org.springblade.vlstream.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import org.springblade.vlstream.pojo.entity.MobileSceneGovernance;
import org.springblade.vlstream.pojo.vo.MobileSceneGovernanceLoopVO;

/**
 * Mobile scene governance main task table Service class
 */
public interface IVlsMobileSceneGovernanceService extends BaseService<MobileSceneGovernance> {

	/**
	 * Add immediate governance
	 */
	boolean saveImmediate(MobileSceneGovernance mobileSceneGovernance);

	/**
	 * Add cyclic governance and generate sub-cyclic tasks
	 */
	boolean saveLoop(MobileSceneGovernance mobileSceneGovernance);

	/**
	 * Query Immediate Governance List
	 */
	IPage<MobileSceneGovernance> listImmediate(IPage<MobileSceneGovernance> page);

	/**
	 * Query Cycle Governance List (including sub-cycle tasks)
	 */
	IPage<MobileSceneGovernanceLoopVO> listLoop(IPage<MobileSceneGovernance> page);
}
