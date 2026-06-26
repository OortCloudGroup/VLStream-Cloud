package org.springblade.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.vlstream.excel.VlsTimeStrategyExcel;
import org.springblade.vlstream.pojo.entity.TimeStrategy;
import org.springblade.vlstream.pojo.vo.TimeStrategyVO;

import java.util.List;

/**
 * Time strategy table Mapper interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsTimeStrategyMapper extends BaseMapper<TimeStrategy> {

	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsTimeStrategy query parameters
	 * @return List<VlsTimeStrategyVO>
	 */
	List<TimeStrategyVO> selectVlsTimeStrategyPage(IPage page, TimeStrategyVO vlsTimeStrategy);

	/**
	 * Get export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsTimeStrategyExcel>
	 */
	List<VlsTimeStrategyExcel> exportVlsTimeStrategy(@Param("ew") Wrapper<TimeStrategy> queryWrapper);

}
