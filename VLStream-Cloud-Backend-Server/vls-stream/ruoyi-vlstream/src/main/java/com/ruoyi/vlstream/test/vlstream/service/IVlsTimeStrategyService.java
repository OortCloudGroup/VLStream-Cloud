package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsTimeStrategyExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TimeStrategy;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.TimeStrategyVO;

import java.util.List;

/**
 * 时间策略表 服务类
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsTimeStrategyService extends BaseService<TimeStrategy> {
	/**
	 * 自定义分页
	 *
	 * @param page 分页参数
	 * @param vlsTimeStrategy 查询参数
	 * @return IPage<VlsTimeStrategyVO>
	 */
	IPage<TimeStrategyVO> selectVlsTimeStrategyPage(IPage<TimeStrategyVO> page, TimeStrategyVO vlsTimeStrategy);

	/**
	 * 导出数据
	 *
	 * @param queryWrapper 查询条件
	 * @return List<VlsTimeStrategyExcel>
	 */
	List<VlsTimeStrategyExcel> exportVlsTimeStrategy(Wrapper<TimeStrategy> queryWrapper);

	/**
	 * 根据设备ID获取时间策略
	 * @param deviceId 设备ID
	 * @return 时间策略
	 */
	TimeStrategy getByDeviceId(String deviceId);

	/**
	 * 保存或更新时间策略
	 * @param timeStrategy 时间策略
	 * @return 是否成功
	 */
	boolean saveOrUpdateStrategy(TimeStrategy timeStrategy);

	/**
	 * 根据设备ID删除时间策略
	 * @param deviceId 设备ID
	 * @return 是否成功
	 */
	boolean deleteByDeviceId(String deviceId);

}
