package org.springblade.vlstream.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springblade.core.mp.base.BaseService;
import org.springblade.vlstream.excel.VlsAlgorithmTrainingExcel;
import org.springblade.vlstream.pojo.entity.AlgorithmTraining;
import org.springblade.vlstream.pojo.vo.AlgorithmTrainingVO;

import java.util.List;

/**
 * Algorithm training task table service class
 *
 * @author Oort
 * @since 2025-12-23
 */
public interface IVlsAlgorithmTrainingService extends BaseService<AlgorithmTraining> {
	/**
	 * Custom paging
	 *
	 * @param page pagination parameters
	 * @param vlsAlgorithmTraining query parameters
	 * @return IPage<VlsAlgorithmTrainingVO>
	 */
	IPage<AlgorithmTrainingVO> selectVlsAlgorithmTrainingPage(IPage<AlgorithmTrainingVO> page, AlgorithmTrainingVO vlsAlgorithmTraining);

	/**
	 * Export data
	 *
	 * @param queryWrapper query conditions
	 * @return List<VlsAlgorithmTrainingExcel>
	 */
	List<VlsAlgorithmTrainingExcel> exportVlsAlgorithmTraining(Wrapper<AlgorithmTraining> queryWrapper);

	/**
	 * Query Algorithm Training Tasks
	 *
	 * @param id algorithm training task primary key
	 * @return algorithm training task
	 */
	public AlgorithmTraining selectAlgorithmTrainingById(Long id);

	/**
	 * Query Algorithm Training Task List
	 *
	 * @param algorithmTraining algorithm training task
	 * @return algorithm training task collection
	 */
	public List<AlgorithmTraining> selectAlgorithmTrainingList(AlgorithmTraining algorithmTraining);

	/**
	 * Add algorithm training task
	 *
	 * @param algorithmTraining algorithm training task
	 * @return result
	 */
	public int insertAlgorithmTraining(AlgorithmTraining algorithmTraining);

	/**
	 * Modify algorithm training task
	 *
	 * @param algorithmTraining algorithm training task
	 * @return result
	 */
	public int updateAlgorithmTraining(AlgorithmTraining algorithmTraining);

	/**
	 * Batch delete algorithm training tasks
	 *
	 * @param ids set of primary keys of the algorithm training tasks to be deleted
	 * @return result
	 */
	public int deleteAlgorithmTrainingByIds(Long[] ids);

	/**
	 * Delete algorithm training task information
	 *
	 * @param id algorithm training task primary key
	 * @return result
	 */
	public int deleteAlgorithmTrainingById(Long id);

}
