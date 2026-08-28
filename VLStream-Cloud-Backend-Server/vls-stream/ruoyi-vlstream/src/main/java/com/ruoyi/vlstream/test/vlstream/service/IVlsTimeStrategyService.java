/*
 * SPDX-FileCopyrightText: 2021 RuoYi-Flowable-Plus
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: RuoYi-Flowable-Plus
 * Updated by: ChaoQun Lei
 */

package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsTimeStrategyExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TimeStrategy;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.TimeStrategyVO;

import java.util.List;

/**
 * service
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsTimeStrategyService extends BaseService<TimeStrategy> {
	/**
	 * Custom
	 *
	 * @param page parameter
	 * @param vlsTimeStrategy Query parameter
	 * @return IPage<VlsTimeStrategyVO>
	 */
	IPage<TimeStrategyVO> selectVlsTimeStrategyPage(IPage<TimeStrategyVO> page, TimeStrategyVO vlsTimeStrategy);

	/**
	 * Export data
	 *
	 * @param queryWrapper Query
	 * @return List<VlsTimeStrategyExcel>
	 */
	List<VlsTimeStrategyExcel> exportVlsTimeStrategy(Wrapper<TimeStrategy> queryWrapper);

	/**
	 * deviceIDGet
	 * @param deviceId deviceID
	 * @return
	 */
	TimeStrategy getByDeviceId(String deviceId);

	/**
	 * update time
	 * @param timeStrategy
	 * @return whether successfully
	 */
	boolean saveOrUpdateStrategy(TimeStrategy timeStrategy);

	/**
	 * deviceIDDelete
	 * @param deviceId deviceID
	 * @return whether successfully
	 */
	boolean deleteByDeviceId(String deviceId);

}
