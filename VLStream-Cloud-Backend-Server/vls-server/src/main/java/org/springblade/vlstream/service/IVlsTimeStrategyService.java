package org.springblade.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import org.springblade.vlstream.excel.VlsTimeStrategyExcel;
import org.springblade.vlstream.pojo.entity.TimeStrategy;
import org.springblade.vlstream.pojo.vo.TimeStrategyVO;

import java.util.List;

/**
 * Time strategy table service class
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsTimeStrategyService extends BaseService<TimeStrategy> {
	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsTimeStrategy query parameters
	 * @return IPage<VlsTimeStrategyVO>
	 */
	IPage<TimeStrategyVO> selectVlsTimeStrategyPage(IPage<TimeStrategyVO> page, TimeStrategyVO vlsTimeStrategy);

	/**
	 * Export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsTimeStrategyExcel>
	 */
	List<VlsTimeStrategyExcel> exportVlsTimeStrategy(Wrapper<TimeStrategy> queryWrapper);

	/**
	 * Get time strategy by device ID
	 * @param deviceId device ID
	 * @return time strategy
	 */
	TimeStrategy getByDeviceId(String deviceId);

	/**
	 * Save or update time strategy
	 * @param timeStrategy time strategy
	 * @return whether successful
	 */
	boolean saveOrUpdateStrategy(TimeStrategy timeStrategy);

	/**
	 * Delete time strategy by device ID
	 * @param deviceId device ID
	 * @return whether successful
	 */
	boolean deleteByDeviceId(String deviceId);

}
