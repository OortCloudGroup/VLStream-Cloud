package org.springblade.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import org.springblade.vlstream.excel.VlsAlgorithmOrchestrationExcel;
import org.springblade.vlstream.pojo.entity.AlgorithmOrchestration;
import org.springblade.vlstream.pojo.vo.AlgorithmOrchestrationVO;

import java.util.List;

/**
 * Algorithm orchestration table service class
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsAlgorithmOrchestrationService extends BaseService<AlgorithmOrchestration> {
	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsAlgorithmOrchestration query parameters
	 * @return IPage<VlsAlgorithmOrchestrationVO>
	 */
	IPage<AlgorithmOrchestrationVO> selectVlsAlgorithmOrchestrationPage(IPage<AlgorithmOrchestrationVO> page, AlgorithmOrchestrationVO vlsAlgorithmOrchestration);

	/**
	 * Export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsAlgorithmOrchestrationExcel>
	 */
	List<VlsAlgorithmOrchestrationExcel> exportVlsAlgorithmOrchestration(Wrapper<AlgorithmOrchestration> queryWrapper);

}
