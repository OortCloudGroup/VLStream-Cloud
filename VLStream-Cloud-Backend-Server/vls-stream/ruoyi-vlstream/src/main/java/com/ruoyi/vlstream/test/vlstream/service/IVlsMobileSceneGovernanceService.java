package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.MobileSceneGovernance;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.MobileSceneGovernanceLoopVO;

/**
 * 移动端场景治理主任务表 服务类
 */
public interface IVlsMobileSceneGovernanceService extends BaseService<MobileSceneGovernance> {

	/**
	 * 新增即时治理
	 */
	boolean saveImmediate(MobileSceneGovernance mobileSceneGovernance);

	/**
	 * 新增循环治理，并生成子循环任务
	 */
	boolean saveLoop(MobileSceneGovernance mobileSceneGovernance);

	/**
	 * 查询即时治理列表
	 */
	IPage<MobileSceneGovernance> listImmediate(IPage<MobileSceneGovernance> page);

	/**
	 * 查询循环治理列表（包含子循环任务）
	 */
	IPage<MobileSceneGovernanceLoopVO> listLoop(IPage<MobileSceneGovernance> page);
}
