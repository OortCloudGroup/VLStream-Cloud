package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmOrchestrationExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmOrchestration;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmOrchestrationVO;

import java.util.List;

/**
 * 算法编排表 服务类
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsAlgorithmOrchestrationService extends BaseService<AlgorithmOrchestration> {
	/**
	 * 自定义分页
	 *
	 * @param page 分页参数
	 * @param vlsAlgorithmOrchestration 查询参数
	 * @return IPage<VlsAlgorithmOrchestrationVO>
	 */
	IPage<AlgorithmOrchestrationVO> selectVlsAlgorithmOrchestrationPage(IPage<AlgorithmOrchestrationVO> page, AlgorithmOrchestrationVO vlsAlgorithmOrchestration);

	/**
	 * 导出数据
	 *
	 * @param queryWrapper 查询条件
	 * @return List<VlsAlgorithmOrchestrationExcel>
	 */
	List<VlsAlgorithmOrchestrationExcel> exportVlsAlgorithmOrchestration(Wrapper<AlgorithmOrchestration> queryWrapper);

}
