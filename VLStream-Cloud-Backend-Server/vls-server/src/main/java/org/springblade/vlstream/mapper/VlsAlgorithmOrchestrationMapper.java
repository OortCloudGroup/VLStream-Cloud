package org.springblade.vlstream.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.springblade.vlstream.excel.VlsAlgorithmOrchestrationExcel;
import org.springblade.vlstream.pojo.entity.AlgorithmOrchestration;
import org.springblade.vlstream.pojo.vo.AlgorithmOrchestrationVO;

import java.util.List;

/**
 * Algorithm orchestration table Mapper interface
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface VlsAlgorithmOrchestrationMapper extends BaseMapper<AlgorithmOrchestration> {

	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsAlgorithmOrchestration query parameters
	 * @return List<VlsAlgorithmOrchestrationVO>
	 */
	List<AlgorithmOrchestrationVO> selectVlsAlgorithmOrchestrationPage(IPage page, AlgorithmOrchestrationVO vlsAlgorithmOrchestration);

	/**
	 * Get export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsAlgorithmOrchestrationExcel>
	 */
	List<VlsAlgorithmOrchestrationExcel> exportVlsAlgorithmOrchestration(@Param("ew") Wrapper<AlgorithmOrchestration> queryWrapper);

}
