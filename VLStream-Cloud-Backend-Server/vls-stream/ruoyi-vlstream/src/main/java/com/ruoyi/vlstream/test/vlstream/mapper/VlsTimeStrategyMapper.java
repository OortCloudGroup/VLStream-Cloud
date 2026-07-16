package com.ruoyi.vlstream.test.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.vlstream.test.vlstream.excel.VlsTimeStrategyExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.TimeStrategy;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.TimeStrategyVO;

import java.util.List;

/**
 * 时间策略表 Mapper 接口
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsTimeStrategyMapper extends BaseMapper<TimeStrategy> {

	/**
	 * 自定义分页
	 *
	 * @param page 分页参数
	 * @param vlsTimeStrategy 查询参数
	 * @return List<VlsTimeStrategyVO>
	 */
	List<TimeStrategyVO> selectVlsTimeStrategyPage(IPage page, TimeStrategyVO vlsTimeStrategy);

	/**
	 * 获取导出数据
	 *
	 * @param queryWrapper 查询条件
	 * @return List<VlsTimeStrategyExcel>
	 */
	List<VlsTimeStrategyExcel> exportVlsTimeStrategy(@Param("ew") Wrapper<TimeStrategy> queryWrapper);

}
