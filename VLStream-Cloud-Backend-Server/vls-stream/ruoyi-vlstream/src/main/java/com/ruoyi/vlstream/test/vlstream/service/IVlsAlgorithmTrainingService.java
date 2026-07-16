package com.ruoyi.vlstream.test.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import com.ruoyi.vlstream.test.vlstream.excel.VlsAlgorithmTrainingExcel;
import com.ruoyi.vlstream.test.vlstream.pojo.entity.AlgorithmTraining;
import com.ruoyi.vlstream.test.vlstream.pojo.vo.AlgorithmTrainingVO;

import java.util.List;

/**
 * 算法训练任务表 服务类
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsAlgorithmTrainingService extends BaseService<AlgorithmTraining> {
	/**
	 * 自定义分页
	 *
	 * @param page 分页参数
	 * @param vlsAlgorithmTraining 查询参数
	 * @return IPage<VlsAlgorithmTrainingVO>
	 */
	IPage<AlgorithmTrainingVO> selectVlsAlgorithmTrainingPage(IPage<AlgorithmTrainingVO> page, AlgorithmTrainingVO vlsAlgorithmTraining);

	/**
	 * 导出数据
	 *
	 * @param queryWrapper 查询条件
	 * @return List<VlsAlgorithmTrainingExcel>
	 */
	List<VlsAlgorithmTrainingExcel> exportVlsAlgorithmTraining(Wrapper<AlgorithmTraining> queryWrapper);

	/**
	 * 查询算法训练任务
	 *
	 * @param id 算法训练任务主键
	 * @return 算法训练任务
	 */
	public AlgorithmTraining selectAlgorithmTrainingById(Long id);

	/**
	 * 查询算法训练任务列表
	 *
	 * @param algorithmTraining 算法训练任务
	 * @return 算法训练任务集合
	 */
	public List<AlgorithmTraining> selectAlgorithmTrainingList(AlgorithmTraining algorithmTraining);

	/**
	 * 新增算法训练任务
	 *
	 * @param algorithmTraining 算法训练任务
	 * @return 结果
	 */
	public int insertAlgorithmTraining(AlgorithmTraining algorithmTraining);

	/**
	 * 修改算法训练任务
	 *
	 * @param algorithmTraining 算法训练任务
	 * @return 结果
	 */
	public int updateAlgorithmTraining(AlgorithmTraining algorithmTraining);

	/**
	 * 批量删除算法训练任务
	 *
	 * @param ids 需要删除的算法训练任务主键集合
	 * @return 结果
	 */
	public int deleteAlgorithmTrainingByIds(Long[] ids);

	/**
	 * 删除算法训练任务信息
	 *
	 * @param id 算法训练任务主键
	 * @return 结果
	 */
	public int deleteAlgorithmTrainingById(Long id);

}
